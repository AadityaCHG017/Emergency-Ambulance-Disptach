package com.example.controller;

import com.example.model.DispatchState;
import com.example.model.Ambulance;
import com.example.model.FirstAidTip;
import com.example.model.Hospital;
import com.example.model.TriageResult;
import com.example.service.AmbulanceService;
import com.example.service.AmbulanceTracker;
import com.example.service.DispatchStateManager;
import com.example.service.ETAService;
import com.example.service.FirstAidService;
import com.example.service.HospitalService;
import com.example.service.MapService;
import com.example.service.RoutingService;
import com.example.service.TriageService;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Dispatch Screen — EMERGENCY FLOW core hub.
 *
 * Responsibilities:
 *   1. Shows ambulance info + live map (WebView + AmbulanceTracker).
 *   2. Drives DispatchStateManager transitions: DISPATCHED → EN_ROUTE → ARRIVED_AT_PATIENT.
 *   3. Shows dynamic state banner — "Arrived / Confirm Pickup" — in the left panel.
 *   4. Hosts the symptom form (logic unchanged).
 *   5. On "Analyze & Find Hospital": navigates to DispatchResultController WITHOUT
 *      stopping the tracker (it stays alive in DispatchStateManager).
 */
public class DispatchController {

    private static final double USER_LAT = 12.9352;
    private static final double USER_LON = 77.6245;

    private final Stage            stage;
    private final Ambulance        ambulance;
    private final AmbulanceService ambulanceService;
    private final int              initialETA;

    // Map services
    private final MapService mapService = new MapService();
    private final ETAService etaService = new ETAService();

    // Live-updated left-panel labels
    private Label       etaCountdownLabel;
    private Label       posLabel;
    private Label       trackStatusLabel;
    private ProgressBar progressBar;

    // Dynamic state banner (appears when ambulance arrives)
    private VBox  stateBannerBox;
    private Label stateBannerLabel;

    // State listener — kept as field so it can be removed on navigation
    private final Consumer<DispatchState> stateListener = this::handleStateChange;

    public DispatchController(Stage stage, Ambulance ambulance,
                               AmbulanceService ambulanceService) {
        this.stage            = stage;
        this.ambulance        = ambulance;
        this.ambulanceService = ambulanceService;
        this.initialETA       = ambulanceService.calculateETA(ambulance);
    }

    // ── Scene builder ─────────────────────────────────────────────────────────

    public Scene buildScene() {
        HBox header    = buildHeader();
        VBox leftPanel = buildLeftPanel();
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        mapService.setEngine(webEngine);
        HBox.setHgrow(webView, Priority.ALWAYS);

        URL mapUrl = getClass().getResource("/map.html");
        if (mapUrl != null) webEngine.load(mapUrl.toExternalForm());

        webEngine.getLoadWorker().stateProperty().addListener((obs, old, s) -> {
            if (s == javafx.concurrent.Worker.State.SUCCEEDED) {
                placeMarkers();
                fetchRouteAndStartTracker();
            }
        });

        HBox body = new HBox(0, new ScrollPane(leftPanel) {{
            setFitToWidth(true);
            setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            setPrefWidth(380); setMinWidth(320);
        }}, webView);
        HBox.setHgrow(webView, Priority.ALWAYS);
        VBox root = new VBox(0, header, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        Scene scene = new Scene(root, 980, 700);

        // Register state listener + kick off DISPATCHED state
        DispatchStateManager.getInstance().addStateListener(stateListener);
        DispatchStateManager.getInstance().transition(DispatchState.DISPATCHED);
        return scene;
    }

    // ── Map ───────────────────────────────────────────────────────────────────

    private void placeMarkers() {
        mapService.updateAmbulance(ambulance.getLat(), ambulance.getLon(),
            "<b>🚑 " + ambulance.getNumberPlate() + "</b><br/>ETA: ~" + initialETA + " min");
        mapService.updatePatient(USER_LAT, USER_LON,
            "<b>📍 Your Location</b><br/>Koramangala, Bangalore");
        mapService.updateInfoPanel("Calculating…", initialETA + " min", "🔄 Fetching route…");
    }

    private void fetchRouteAndStartTracker() {
        Task<RoutingService.RouteResult> task = new Task<>() {
            @Override protected RoutingService.RouteResult call() {
                return etaService.get(ambulance.getLat(), ambulance.getLon(),
                                      USER_LAT, USER_LON).orElse(null);
            }
        };
        task.setOnSucceeded(e -> {
            RoutingService.RouteResult result = task.getValue();
            String popup = "<b>🚑 " + ambulance.getNumberPlate() + "</b>";
            if (result != null) {
                mapService.drawRoute(result.coordsJson());
                mapService.updateInfoPanel(
                    String.format("%.1f km", result.distanceKm()),
                    result.etaMinutes() + " min", "🚑 Ambulance en route");
                startTracker(result.waypoints(), popup);
            } else {
                mapService.updateInfoPanel(initialETA + " min (est.)", initialETA + " min (est.)", "⚠ Offline");
                startTracker(new ArrayList<>(), popup);
            }
            mapService.fitAll();
        });
        task.setOnFailed(e -> startTracker(new ArrayList<>(), "🚑 " + ambulance.getNumberPlate()));
        Thread t = new Thread(task, "osrm-initial-route");
        t.setDaemon(true); t.start();
    }

    private void startTracker(List<double[]> waypoints, String popup) {
        AmbulanceTracker tracker = new AmbulanceTracker(mapService, etaService, USER_LAT, USER_LON);

        tracker.setProximityThresholdMeters(DispatchStateManager.ARRIVAL_THRESHOLD_METERS);
        tracker.setOnProximityReached(() ->
            DispatchStateManager.getInstance().transition(DispatchState.ARRIVED_AT_PATIENT));
        tracker.setOnArrived(() ->
            DispatchStateManager.getInstance().transition(DispatchState.ARRIVED_AT_PATIENT));
        tracker.setOnETAUpdate((dist, eta) -> {
            etaCountdownLabel.setText(eta);
            trackStatusLabel.setText("🚑  Ambulance is on the way… " + dist);
        });
        tracker.setOnPositionUpdate(pos ->
            posLabel.setText(String.format("📍  Ambulance: %.4f°N, %.4f°E", pos[0], pos[1])));

        // Store in manager so it survives scene navigation
        DispatchStateManager.getInstance().setActiveTracker(tracker);
        DispatchStateManager.getInstance().transition(DispatchState.EN_ROUTE);

        tracker.startTracking(waypoints, ambulance.getLat(), ambulance.getLon(), popup);
        trackStatusLabel.setText("🚑  Ambulance is on the way…");
    }

    // ── State change handler ──────────────────────────────────────────────────

    private void handleStateChange(DispatchState state) {
        if (stateBannerBox == null) return;
        stateBannerBox.getChildren().clear();

        switch (state) {
            case EN_ROUTE -> {
                stateBannerLabel.setText("🔄  Ambulance en route to your location…");
                stateBannerBox.getChildren().add(stateBannerLabel);
                stateBannerBox.setVisible(true);
                stateBannerBox.setManaged(true);
            }
            case ARRIVED_AT_PATIENT -> {
                Label arrivedLbl = new Label("🚑  Ambulance has arrived at your location!");
                Button confirmBtn = new Button("✅  Confirm Patient Pickup");
                confirmBtn.setId("confirmPickupBtn");
                confirmBtn.setOnAction(e ->
                    DispatchStateManager.getInstance().transition(DispatchState.PATIENT_PICKED_UP));
                stateBannerBox.getChildren().addAll(arrivedLbl, confirmBtn);
                stateBannerBox.setVisible(true);
                stateBannerBox.setManaged(true);
            }
            case PATIENT_PICKED_UP -> {
                stateBannerLabel.setText("✅  Pickup confirmed — analyze symptoms to select hospital.");
                stateBannerBox.getChildren().add(stateBannerLabel);
                stateBannerBox.setVisible(true);
                stateBannerBox.setManaged(true);
            }
            default -> {
                stateBannerBox.setVisible(false);
                stateBannerBox.setManaged(false);
            }
        }
    }

    // ── UI builders ───────────────────────────────────────────────────────────

    private VBox buildLeftPanel() {
        // State banner (dynamic, initially hidden)
        stateBannerLabel = new Label();
        stateBannerBox   = new VBox(8);
        stateBannerBox.setPadding(new Insets(10, 16, 10, 16));
        stateBannerBox.setVisible(false);
        stateBannerBox.setManaged(false);

        VBox dispatch = buildDispatchPanel();
        Label divider = new Label("────────  SYMPTOM ASSESSMENT  ────────");
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setAlignment(Pos.CENTER);
        VBox symptom = buildSymptomPanel();

        VBox panel = new VBox(0, stateBannerBox, dispatch, divider, symptom);
        panel.setPadding(new Insets(0));
        return panel;
    }

    private HBox buildHeader() {
        Label title = new Label("🚑  Ambulance Dispatched");
        Label badge = new Label("● LIVE");
        Region sp   = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button homeBtn = new Button("🏠");
        homeBtn.setOnAction(e -> leaveAndReset());
        HBox h = new HBox(12, title, badge, sp, homeBtn);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(18, 24, 14, 24));
        return h;
    }

    private VBox buildDispatchPanel() {
        VBox idCard      = labelCard("AMBULANCE",    ambulance.getNumberPlate());
        VBox driverCard  = labelCard("DRIVER",       ambulance.getDriverName());
        VBox phoneCard   = labelCard("DRIVER PHONE", "📞 " + ambulance.getDriverPhone());
        HBox driverRow   = new HBox(12, driverCard, phoneCard);
        HBox.setHgrow(driverCard, Priority.ALWAYS);
        HBox.setHgrow(phoneCard,  Priority.ALWAYS);

        etaCountdownLabel = new Label(initialETA + " min");
        VBox etaCard = new VBox(4, new Label("ESTIMATED ARRIVAL (road-based, refreshes 5 s)"),
                                etaCountdownLabel);

        trackStatusLabel = new Label("🔄  Calculating route…");
        posLabel         = new Label("📍  Locating ambulance…");
        progressBar      = new ProgressBar(0.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        VBox trackCard = new VBox(8, new Label("LIVE TRACKING"),
                                  trackStatusLabel, progressBar, posLabel);

        VBox panel = new VBox(12, idCard, driverRow, etaCard, trackCard);
        panel.setPadding(new Insets(20, 16, 16, 20));
        return panel;
    }

    private static final String[] SYMPTOMS = {
        "chest pain","breathing difficulty","unconscious",
        "heavy bleeding","fracture","high fever","headache","cold"
    };

    private VBox buildSymptomPanel() {
        Label hint = new Label("Select symptoms so we can prepare the hospital:");
        List<CheckBox> cbs  = new ArrayList<>();
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(10);
        for (int i = 0; i < SYMPTOMS.length; i++) {
            CheckBox cb = new CheckBox(cap(SYMPTOMS[i]));
            cb.setId("dispCb_" + SYMPTOMS[i].replace(" ", "_"));
            cbs.add(cb); grid.add(cb, i % 2, i / 2);
        }
        Label statusLabel = new Label("");
        Button analyzeBtn = new Button("Analyze & Find Hospital  →");
        analyzeBtn.setId("dispatchAnalyzeBtn");
        analyzeBtn.setOnAction(e -> {
            List<String> sel = getSelected(cbs);
            if (sel.isEmpty()) { statusLabel.setText("⚠  Please select at least one symptom."); return; }
            statusLabel.setText("");
            runTriageAndNavigate(sel);
        });
        Button skipBtn = new Button("Skip");
        skipBtn.setId("skipSymptomsBtn");
        skipBtn.setOnAction(e -> leaveAndReset());
        HBox btnRow = new HBox(16, analyzeBtn, skipBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(12, hint, grid, statusLabel, btnRow);
        panel.setPadding(new Insets(8, 16, 28, 20));
        return panel;
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void runTriageAndNavigate(List<String> selected) {
        // Remove OUR listener — tracker stays alive in DispatchStateManager
        DispatchStateManager.getInstance().removeStateListener(stateListener);

        TriageResult result = new TriageService().analyze(selected, new HospitalService().getAllHospitals());
        Optional<FirstAidTip> tip = new FirstAidService().getTipsForSymptoms(selected);
        boolean showFA = new FirstAidService().shouldShowFirstAid(initialETA);

        stage.setScene(new DispatchResultController(
            stage, result, selected, ambulance, initialETA, tip, showFA).buildScene());
    }

    private void leaveAndReset() {
        DispatchStateManager.getInstance().removeStateListener(stateListener);
        DispatchStateManager.getInstance().reset();
        com.example.service.DispatchStateService.getInstance().clear();
        stage.setScene(new HomeController(stage).buildScene());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox labelCard(String t, String v) {
        VBox c = new VBox(4, new Label(t), new Label(v));
        c.setMaxWidth(Double.MAX_VALUE); return c;
    }

    private List<String> getSelected(List<CheckBox> cbs) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < cbs.size(); i++)
            if (cbs.get(i).isSelected()) out.add(SYMPTOMS[i]);
        return out;
    }

    private String cap(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
