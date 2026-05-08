package com.example.controller;

import com.example.model.Hospital;
import com.example.service.AmbulanceTracker;
import com.example.service.ETAService;
import com.example.service.MapService;
import com.example.service.RoutingService;
import com.example.util.DistanceCalculator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;

/**
 * Hospital Tracker Screen — NON-EMERGENCY navigation.
 *
 * Left panel  : ETA countdown, progress bar, hospital info (all preserved).
 * Right panel : Live Leaflet map — route from patient to hospital.
 *
 * AmbulanceTracker (repurposed as "vehicle tracker") moves a 📍 patient
 * marker along the real OSRM road route to the selected hospital.
 * ETA is refreshed every 5 seconds from OSRM.
 */
public class HospitalTrackerController {

    // Fixed patient (user) location — Koramangala, Bangalore
    private static final double USER_LAT = 12.9352;
    private static final double USER_LON = 77.6245;

    // ── Instance state ────────────────────────────────────────────────────────

    private final Stage    stage;
    private final Hospital hospital;
    private final double   distanceKm;
    private final int      etaMinutes;

    /** UI countdown (seconds) — runs in parallel with map tracker. */
    private int      secondsRemaining;
    private Timeline countdown;

    // Live-updated left-panel UI nodes
    private Label       etaLabel;
    private Label       statusLabel;
    private ProgressBar progressBar;
    private Label       progressPctLabel;

    // Map services
    private final MapService      mapService = new MapService();
    private final ETAService      etaService = new ETAService();
    private       AmbulanceTracker tracker;

    // ── Constructor ───────────────────────────────────────────────────────────

    public HospitalTrackerController(Stage stage, Hospital hospital) {
        this.stage      = stage;
        this.hospital   = hospital;
        this.distanceKm = DistanceCalculator.distanceFromUser(hospital.x(), hospital.y());
        this.etaMinutes = Math.max(1, DistanceCalculator.estimatedETA(distanceKm));
        this.secondsRemaining = etaMinutes * 60;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Scene construction
    // ══════════════════════════════════════════════════════════════════════════

    public Scene buildScene() {
        HBox header = buildHeader();

        // Left info panel
        VBox leftContent = new VBox(20,
            buildHospitalInfoCard(),
            buildTrackerCard(),
            buildRouteInfoCard(),
            buildNavBar()
        );
        leftContent.setPadding(new Insets(20, 16, 24, 20));
        leftContent.setAlignment(Pos.TOP_CENTER);

        ScrollPane leftScroll = new ScrollPane(leftContent);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setPrefWidth(330);
        leftScroll.setMinWidth(280);

        // Right map panel
        WebView   webView   = new WebView();
        WebEngine webEngine = webView.getEngine();
        mapService.setEngine(webEngine);
        HBox.setHgrow(webView, Priority.ALWAYS);

        URL mapUrl = getClass().getResource("/map.html");
        if (mapUrl != null) webEngine.load(mapUrl.toExternalForm());

        webEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                placeInitialMarkers();
                fetchRouteAndStartTracker();
            }
        });

        HBox body = new HBox(0, leftScroll, webView);
        HBox.setHgrow(webView, Priority.ALWAYS);
        VBox root = new VBox(0, header, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        Scene scene = new Scene(root, 980, 680);
        javafx.application.Platform.runLater(this::startCountdown);
        return scene;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Map setup
    // ══════════════════════════════════════════════════════════════════════════

    private void placeInitialMarkers() {
        // Patient marker at user location
        mapService.updatePatient(
            USER_LAT, USER_LON,
            "<b>📍 Your Location</b><br/>Koramangala, Bangalore"
        );
        // Hospital marker
        mapService.updateHospital(
            hospital.lat(), hospital.lon(),
            "<b>🏥 " + hospital.name() + "</b><br/>" + hospital.address()
        );
        mapService.updateInfoPanel(
            String.format("%.1f km (est.)", distanceKm),
            etaMinutes + " min (est.)",
            "🗺  Loading real route…"
        );
    }

    private void fetchRouteAndStartTracker() {
        Task<RoutingService.RouteResult> task = new Task<>() {
            @Override
            protected RoutingService.RouteResult call() {
                return etaService
                    .get(USER_LAT, USER_LON, hospital.lat(), hospital.lon())
                    .orElse(null);
            }
        };

        task.setOnSucceeded(e -> {
            RoutingService.RouteResult result = task.getValue();
            String popup = "<b>📍 You</b>";

            if (result != null) {
                mapService.drawRoute(result.coordsJson());
                mapService.updateInfoPanel(
                    String.format("%.1f km", result.distanceKm()),
                    result.etaMinutes() + " min",
                    "🚗  En route to hospital"
                );
                startTracker(result.waypoints(), popup);
            } else {
                mapService.updateInfoPanel(
                    String.format("%.1f km (est.)", distanceKm),
                    etaMinutes + " min (est.)",
                    "⚠  Offline mode"
                );
                startTracker(new ArrayList<>(), popup);
            }
            mapService.fitAll();
        });

        task.setOnFailed(e -> {
            mapService.updateInfoPanel("—", "—", "⚠  Route unavailable");
            startTracker(new ArrayList<>(), "📍 You");
        });

        Thread t = new Thread(task, "osrm-hospital-route");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Start AmbulanceTracker — here it tracks the patient's vehicle
     * moving from their location to the hospital.
     */
    private void startTracker(java.util.List<double[]> waypoints, String popup) {
        tracker = new AmbulanceTracker(
            mapService, etaService, hospital.lat(), hospital.lon());

        // Update the left-panel ETA label from real OSRM data every 5 s
        tracker.setOnETAUpdate((dist, eta) -> {
            statusLabel.setText("🛣  " + dist + " remaining");
        });

        tracker.setOnArrived(() -> {
            statusLabel.setText("✅  You have arrived at " + hospital.name() + "!");
            mapService.updateInfoPanel("0 km", "0 min", "✅  Arrived!");
        });

        tracker.startTracking(waypoints, USER_LAT, USER_LON, popup);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Left-panel UI builders (unchanged logic)
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildHeader() {
        Label title     = new Label("🏥  Navigation Tracker");
        Label liveBadge = new Label("● LIVE");
        Region spacer   = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button("← Back");
        backBtn.setOnAction(e -> {
            stopAll();
            stage.setScene(new HomeController(stage).buildScene());
        });

        HBox h = new HBox(12, title, liveBadge, spacer, backBtn);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(18, 24, 14, 24));
        return h;
    }

    private VBox buildHospitalInfoCard() {
        Label cap      = new Label("DESTINATION");
        Label nameLbl  = new Label("🏥  " + hospital.name());
        Label addrLbl  = new Label("📍  " + hospital.address());
        Label facilLbl = new Label(hospital.facilitiesDisplay());
        Label ratingLbl= new Label("⭐  " + hospital.ratingDisplay());
        Label bedsLbl  = new Label("🛏  " + hospital.bedsAvailable() + " beds");
        Label distLbl  = new Label(String.format("%.1f km  •  %d min ETA",
                                                   distanceKm, etaMinutes));
        HBox metaRow   = new HBox(12, ratingLbl, bedsLbl, distLbl);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label phoneLbl = new Label("📞  " + hospital.phone());

        VBox card = new VBox(6, cap, nameLbl, addrLbl, facilLbl, metaRow, phoneLbl);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox buildTrackerCard() {
        Label cap = new Label("ETA TRACKER");

        etaLabel = new Label(formatTime(secondsRemaining));
        etaLabel.setAlignment(Pos.CENTER);
        etaLabel.setMaxWidth(Double.MAX_VALUE);

        Label unitLbl = new Label("remaining  (mm:ss)");
        unitLbl.setAlignment(Pos.CENTER);
        unitLbl.setMaxWidth(Double.MAX_VALUE);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(14);

        progressPctLabel = new Label("0% complete");
        progressPctLabel.setAlignment(Pos.CENTER_RIGHT);
        progressPctLabel.setMaxWidth(Double.MAX_VALUE);

        statusLabel = new Label("🚗  En Route to Hospital");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(10, cap, etaLabel, unitLbl,
                             progressBar, progressPctLabel, statusLabel);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox buildRouteInfoCard() {
        return new VBox(10,
            new Label("ROUTE DETAILS"),
            infoRow("🗺  Distance",      String.format("%.1f km", distanceKm)),
            infoRow("⏱  ETA",            etaMinutes + " min (OSRM real-time)"),
            infoRow("🚦  Route",         "Optimal driving route"),
            infoRow("📞  Emergency",     "Call 112 if condition worsens")
        );
    }

    private HBox buildNavBar() {
        Button homeBtn = new Button("🏠  Home");
        homeBtn.setOnAction(e -> { stopAll(); stage.setScene(new HomeController(stage).buildScene()); });

        Button newBtn = new Button("← New Assessment");
        newBtn.setOnAction(e -> { stopAll(); stage.setScene(new SymptomController(stage).buildScene()); });

        HBox nav = new HBox(12, homeBtn, newBtn);
        nav.setAlignment(Pos.CENTER_RIGHT);
        return nav;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ETA countdown (left panel — preserved exactly)
    // ══════════════════════════════════════════════════════════════════════════

    private void startCountdown() {
        if (secondsRemaining <= 0) { markArrived(); return; }
        int total = etaMinutes * 60;

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsRemaining--;
            double progress = 1.0 - ((double) secondsRemaining / total);
            progressBar.setProgress(progress);
            progressPctLabel.setText(String.format("%.0f%% complete", progress * 100));
            etaLabel.setText(formatTime(secondsRemaining));

            if (secondsRemaining <= 0) {
                countdown.stop();
                markArrived();
            } else if (progress >= 0.75) {
                statusLabel.setText("⚡  Almost There!");
            } else if (progress >= 0.4) {
                statusLabel.setText("🛣  Approaching Destination");
            }
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    private void markArrived() {
        secondsRemaining = 0;
        progressBar.setProgress(1.0);
        progressPctLabel.setText("100% — Arrived!");
        etaLabel.setText("ARRIVED");
        statusLabel.setText("✅  You have arrived at " + hospital.name() + "!");
        mapService.updateInfoPanel("0 km", "0 min", "✅  Arrived!");
    }

    private void stopAll() {
        if (countdown != null) countdown.stop();
        if (tracker  != null) tracker.stop();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private String formatTime(int s) {
        if (s <= 0) return "00:00";
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private HBox infoRow(String key, String value) {
        Label k = new Label(key);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        HBox row = new HBox(8, k, sp, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
