package com.example.controller;

import com.example.model.Ambulance;
import com.example.model.DispatchState;
import com.example.model.FirstAidTip;
import com.example.model.Hospital;
import com.example.model.TriageResult;
import com.example.service.DispatchStateManager;
import com.example.service.DispatchStateService;
import com.example.util.DistanceCalculator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Shown after the EMERGENCY FLOW: ambulance dispatched → symptoms selected → triage run.
 *
 * <h3>State-aware hospital cards</h3>
 * <ul>
 *   <li>Hospital "Navigate" buttons are <b>disabled</b> until
 *       {@link DispatchState#PATIENT_PICKED_UP}.</li>
 *   <li>When {@link DispatchState#ARRIVED_AT_PATIENT} is detected, a banner
 *       appears with a "Confirm Pickup" button — no scene reload.</li>
 *   <li>When {@link DispatchState#PATIENT_PICKED_UP} is confirmed, all
 *       navigate buttons are enabled automatically.</li>
 * </ul>
 */
public class DispatchResultController {

    private final Stage               stage;
    private final TriageResult        result;
    private final List<String>        selectedSymptoms;
    private final Ambulance           ambulance;
    private final int                 ambulanceETA;
    private final Optional<FirstAidTip> firstAidTip;
    private final boolean             showFirstAid;

    // State-aware UI fields
    private final List<Button>             hospitalNavBtns = new ArrayList<>();
    private VBox                           arrivalBanner;
    private Label                          arrivalBannerLabel;
    private final Consumer<DispatchState>  stateListener = this::handleStateChange;

    public DispatchResultController(Stage stage, TriageResult result,
                                    List<String> selectedSymptoms, Ambulance ambulance,
                                    int ambulanceETA, Optional<FirstAidTip> firstAidTip,
                                    boolean showFirstAid) {
        this.stage            = stage;
        this.result           = result;
        this.selectedSymptoms = selectedSymptoms;
        this.ambulance        = ambulance;
        this.ambulanceETA     = ambulanceETA;
        this.firstAidTip      = firstAidTip;
        this.showFirstAid     = showFirstAid;
    }

    // ── Scene ─────────────────────────────────────────────────────────────────

    public Scene buildScene() {
        VBox root = new VBox(0);
        root.getChildren().add(buildHeader());

        VBox content = new VBox(14);
        content.setPadding(new Insets(20, 24, 32, 24));

        // Arrival banner (hidden until ARRIVED_AT_PATIENT)
        arrivalBanner = buildArrivalBanner();
        content.getChildren().add(arrivalBanner);

        // Ambulance strip
        content.getChildren().add(buildAmbulanceStrip());
        content.getChildren().add(buildLevelCard());
        content.getChildren().add(buildActionCard());

        if (showFirstAid && firstAidTip.isPresent())
            content.getChildren().add(buildFirstAidCard(firstAidTip.get()));

        content.getChildren().add(buildExplCard());
        content.getChildren().add(buildHospitalCard());
        content.getChildren().add(buildSymptomsCard());
        content.getChildren().add(buildButtonBar());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.getChildren().add(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Register state listener — fires immediately with current state
        DispatchStateManager.getInstance().addStateListener(stateListener);

        return new Scene(root, 600, 700);
    }

    // ── Arrival banner ────────────────────────────────────────────────────────

    private VBox buildArrivalBanner() {
        arrivalBannerLabel = new Label("⏳  Waiting for ambulance to arrive…");

        Button confirmBtn = new Button("✅  Confirm Patient Pickup");
        confirmBtn.setId("confirmPickupBtn");
        confirmBtn.setVisible(false);
        confirmBtn.setManaged(false);
        confirmBtn.setOnAction(e ->
            DispatchStateManager.getInstance().transition(DispatchState.PATIENT_PICKED_UP));

        VBox banner = new VBox(8, arrivalBannerLabel, confirmBtn);
        banner.setPadding(new Insets(12, 16, 12, 16));
        banner.setId("arrivalBanner");

        // Store confirm button reference so handleStateChange can show/hide it
        banner.setUserData(confirmBtn);
        return banner;
    }

    // ── State handler (runs on FX thread via DispatchStateManager) ────────────

    private void handleStateChange(DispatchState state) {
        if (arrivalBanner == null) return;
        Button confirmBtn = (Button) arrivalBanner.getUserData();

        switch (state) {
            case DISPATCHED, EN_ROUTE -> {
                arrivalBannerLabel.setText("⏳  Ambulance is en route — hospital navigation locked.");
                if (confirmBtn != null) { confirmBtn.setVisible(false); confirmBtn.setManaged(false); }
                arrivalBanner.setVisible(true);
                arrivalBanner.setManaged(true);
                hospitalNavBtns.forEach(b -> {
                    b.setDisable(true);
                    b.setText("🔒  Waiting for ambulance…");
                });
            }
            case ARRIVED_AT_PATIENT -> {
                arrivalBannerLabel.setText("🚑  Ambulance has arrived at your location!");
                if (confirmBtn != null) { confirmBtn.setVisible(true); confirmBtn.setManaged(true); }
                arrivalBanner.setVisible(true);
                arrivalBanner.setManaged(true);
            }
            case PATIENT_PICKED_UP, EN_ROUTE_TO_HOSPITAL, COMPLETED -> {
                arrivalBannerLabel.setText("✅  Pickup confirmed — select a hospital to navigate.");
                if (confirmBtn != null) { confirmBtn.setVisible(false); confirmBtn.setManaged(false); }
                arrivalBanner.setVisible(true);
                arrivalBanner.setManaged(true);
                // Unlock all hospital navigate buttons
                hospitalNavBtns.forEach(b -> {
                    b.setDisable(false);
                    b.setText("🗺  Navigate to This Hospital  →");
                });
            }
            case IDLE ->
                arrivalBanner.setVisible(false);
        }
    }

    // ── Helpers: clean up listener before leaving ─────────────────────────────

    private void leaveScreen() {
        DispatchStateManager.getInstance().removeStateListener(stateListener);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        Label title  = new Label("Assessment Result");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button homeBtn = new Button("🏠  Home");
        homeBtn.setOnAction(e -> {
            leaveScreen();
            DispatchStateManager.getInstance().reset();
            DispatchStateService.getInstance().clear();
            stage.setScene(new HomeController(stage).buildScene());
        });
        HBox h = new HBox(12, title, spacer, homeBtn);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(20, 24, 16, 24));
        return h;
    }

    // ── Ambulance strip ───────────────────────────────────────────────────────

    private HBox buildAmbulanceStrip() {
        Label msg = new Label("🚑  " + ambulance.getNumberPlate()
            + "  •  ETA " + ambulanceETA + " min  •  " + ambulance.getDriverPhone());
        HBox strip = new HBox(12, msg);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPadding(new Insets(12, 16, 12, 16));
        return strip;
    }

    // ── Triage cards ──────────────────────────────────────────────────────────

    private VBox buildLevelCard() {
        return card("TRIAGE LEVEL", "● " + result.levelLabel());
    }

    private VBox buildActionCard() {
        return card("RECOMMENDED ACTION", result.action());
    }

    private VBox buildExplCard() {
        Label l = new Label(result.explanation());
        l.setMaxWidth(Double.MAX_VALUE);
        return styledVBox(new VBox(6, new Label("EXPLANATION"), l));
    }

    private VBox buildFirstAidCard(FirstAidTip tip) {
        Label title = new Label("🩹  FIRST AID: " + tip.condition().toUpperCase());
        Label tips  = new Label(tip.tipsDisplay());
        tips.setMaxWidth(Double.MAX_VALUE);
        return styledVBox(new VBox(8,
            new Label("⏱  Ambulance ETA " + ambulanceETA + " min — do this now:"),
            title, tips));
    }

    // ── Hospital card (state-locked) ──────────────────────────────────────────

    private VBox buildHospitalCard() {
        Label title  = new Label("SUGGESTED HOSPITALS");
        Label hint   = new Label("Hospital navigation unlocks after patient pickup is confirmed.");
        VBox  list   = new VBox(10);
        List<Hospital> hospitals = result.suggestedHospitals();
        for (int i = 0; i < hospitals.size(); i++) {
            Hospital h   = hospitals.get(i);
            double dist  = DistanceCalculator.distanceFromUser(h.x(), h.y());
            int    eta   = DistanceCalculator.estimatedETA(dist);
            list.getChildren().add(buildHospCard(i + 1, h, dist, eta));
        }
        return styledVBox(new VBox(10, title, hint, list));
    }

    private VBox buildHospCard(int rank, Hospital h, double dist, int eta) {
        Label rankLbl = new Label("#" + rank);
        Label nameLbl = new Label(h.name());
        Label facilLbl= new Label(h.facilitiesDisplay());
        VBox  info    = new VBox(2, nameLbl, facilLbl);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label distLbl = new Label(String.format("%.1f km  •  %d min", dist, eta));
        HBox topRow   = new HBox(10, rankLbl, info, distLbl);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label ratingLbl = new Label("⭐ " + h.ratingDisplay());
        Label bedsLbl   = new Label("🛏 " + h.bedsAvailable() + " beds");
        Label addrLbl   = new Label("📍 " + h.address());
        HBox metaRow    = new HBox(14, ratingLbl, bedsLbl, addrLbl);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label phoneLbl  = new Label("📞 " + h.phone());

        // Button: initially locked
        Button navBtn = new Button("🔒  Waiting for ambulance…");
        navBtn.setId("dispNavHospital" + rank);
        navBtn.setMaxWidth(Double.MAX_VALUE);
        navBtn.setDisable(true);
        navBtn.setOnAction(e -> {
            leaveScreen();
            DispatchStateManager.getInstance().transition(DispatchState.EN_ROUTE_TO_HOSPITAL);
            stage.setScene(new HospitalTrackerController(stage, h).buildScene());
        });

        // Store reference for bulk enable/disable
        hospitalNavBtns.add(navBtn);

        return new VBox(8, topRow, metaRow, phoneLbl, navBtn);
    }

    // ── Symptoms card ─────────────────────────────────────────────────────────

    private VBox buildSymptomsCard() {
        return card("SYMPTOMS ASSESSED", String.join("  •  ", selectedSymptoms));
    }

    // ── Button bar ────────────────────────────────────────────────────────────

    private HBox buildButtonBar() {
        Button homeBtn = new Button("🏠  Home");
        homeBtn.setId("dispResHomeBtn");
        homeBtn.setOnAction(e -> {
            leaveScreen();
            DispatchStateManager.getInstance().reset();
            DispatchStateService.getInstance().clear();
            stage.setScene(new HomeController(stage).buildScene());
        });
        Button newBtn = new Button("New Assessment  →");
        newBtn.setId("dispResNewBtn");
        newBtn.setOnAction(e -> {
            leaveScreen();
            DispatchStateManager.getInstance().reset();
            DispatchStateService.getInstance().clear();
            stage.setScene(new SymptomController(stage).buildScene());
        });
        HBox bar = new HBox(12, homeBtn, newBtn);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(8, 0, 0, 0));
        return bar;
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private VBox card(String title, String value) {
        return styledVBox(new VBox(4, new Label(title), new Label(value)));
    }

    private VBox styledVBox(VBox v) {
        v.setMaxWidth(Double.MAX_VALUE); return v;
    }
}
