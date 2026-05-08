package com.example.controller;

import com.example.model.Ambulance;
import com.example.model.FirstAidTip;
import com.example.model.Hospital;
import com.example.model.TriageResult;
import com.example.service.AmbulanceService;
import com.example.service.DispatchStateService;
import com.example.service.FirstAidService;
import com.example.util.DistanceCalculator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * Result Screen controller — NON-EMERGENCY path.
 *
 * Displays the full TriageResult:
 *   - Emergency level (color-coded)
 *   - Recommended action
 *   - Explanation
 *   - First-aid tips (Critical/Moderate)
 *   - Suggested hospitals (selectable — opens ETA tracker)
 *   - Conditional ambulance dispatch buttons
 *
 * Ambulance dispatch logic:
 *   Level 3 (Critical)  → auto-dispatches immediately
 *   Level 2 (Moderate)  → asks user via dialog ("Recommended")
 *   Level 1 (Low)       → asks user via dialog ("Available if needed")
 */
public class ResultController {

    private final Stage        stage;
    private final TriageResult result;
    private final List<String> selectedSymptoms;

    /** True once an ambulance has been dispatched to prevent double-dispatch. */
    private boolean dispatched = false;

    public ResultController(Stage stage, TriageResult result, List<String> selectedSymptoms) {
        this.stage            = stage;
        this.result           = result;
        this.selectedSymptoms = selectedSymptoms;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Scene construction
    // ══════════════════════════════════════════════════════════════════════════

    /** Builds and returns the Result screen Scene. */
    public Scene buildScene() {
        VBox root = new VBox(0);

        root.getChildren().add(buildHeader());

        // ── Scrollable content ────────────────────────────────────────────────
        VBox content = new VBox(14);
        content.setPadding(new Insets(20, 24, 32, 24));

        content.getChildren().add(buildLevelCard());
        content.getChildren().add(buildActionCard());
        content.getChildren().add(buildExplCard());

        // First-aid tips (Critical or Moderate only)
        FirstAidService fas = new FirstAidService();
        Optional<FirstAidTip> tip = fas.getTipsForSymptoms(selectedSymptoms);
        if (tip.isPresent() && result.level() >= 2) {
            content.getChildren().add(buildFirstAidCard(tip.get()));
        }

        content.getChildren().add(buildSymptomsCard());
        content.getChildren().add(buildHospitalCard());

        // Ambulance dispatch panel (auto or user-choice)
        content.getChildren().add(buildDispatchPanel());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        root.getChildren().add(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return new Scene(root, 600, 680);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Header
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildHeader() {
        Label title = new Label("Triage Result");

        Label badge = new Label("NON-EMERGENCY");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button homeBtn = new Button("🏠");
        homeBtn.setOnAction(e -> {
            DispatchStateService.getInstance().clear(); // allow fresh dispatch
            stage.setScene(new HomeController(stage).buildScene());
        });

        HBox header = new HBox(12, title, badge, spacer, homeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 16, 24));
        return header;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Triage cards
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildLevelCard() {
        Label titleLbl = cardTitle("TRIAGE LEVEL");
        Label badge = new Label("● " + result.levelLabel());
        return styledCard(new VBox(4, titleLbl, badge));
    }

    private VBox buildActionCard() {
        Label titleLbl = cardTitle("RECOMMENDED ACTION");
        Label actionLbl = new Label(result.action());
        return styledCard(new VBox(4, titleLbl, actionLbl));
    }

    private VBox buildExplCard() {
        Label titleLbl = cardTitle("EXPLANATION");
        Label explLbl = new Label(result.explanation());
        explLbl.setMaxWidth(Double.MAX_VALUE);
        return styledCard(new VBox(6, titleLbl, explLbl));
    }

    private VBox buildFirstAidCard(FirstAidTip tip) {
        Label titleLbl = new Label("🩹  FIRST AID: " + tip.condition().toUpperCase());

        Label tipsLbl = new Label(tip.tipsDisplay());
        tipsLbl.setMaxWidth(Double.MAX_VALUE);

        VBox card = styledCard(new VBox(8, titleLbl, tipsLbl));
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Symptoms + hospital cards
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildSymptomsCard() {
        Label titleLbl = cardTitle("SYMPTOMS ASSESSED");
        Label sympLbl = new Label(String.join("  •  ", selectedSymptoms));
        return styledCard(new VBox(4, titleLbl, sympLbl));
    }

    private VBox buildHospitalCard() {
        Label titleLbl = cardTitle("SUGGESTED HOSPITALS");

        Label hintLbl = new Label("Tap a hospital to start live navigation tracker");

        VBox hospList = new VBox(10);
        List<Hospital> hospitals = result.suggestedHospitals();
        for (int i = 0; i < hospitals.size(); i++) {
            Hospital h = hospitals.get(i);
            double dist = DistanceCalculator.distanceFromUser(h.x(), h.y());
            int eta     = DistanceCalculator.estimatedETA(dist);
            hospList.getChildren().add(buildHospitalCard(i + 1, h, dist, eta));
        }
        return styledCard(new VBox(10, titleLbl, hintLbl, hospList));
    }

    private VBox buildHospitalCard(int rank, Hospital h, double dist, int eta) {
        // ── Top row: rank + name/facilities + distance ────────────────────────
        Label rankLabel = new Label("#" + rank);

        Label nameLabel = new Label(h.name());

        Label facilLabel = new Label(h.facilitiesDisplay());

        VBox info = new VBox(2, nameLabel, facilLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label distLabel = new Label(String.format("%.1f km  •  %d min", dist, eta));
        distLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox topRow = new HBox(10, rankLabel, info, distLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // ── Navigate button ───────────────────────────────────────────────────
        Button navBtn = new Button("🗺  Navigate to This Hospital  →");
        navBtn.setId("navHospital" + rank);
        navBtn.setMaxWidth(Double.MAX_VALUE);
        navBtn.setOnAction(e ->
            stage.setScene(new HospitalTrackerController(stage, h).buildScene())
        );

        // ── Meta row (rating, beds, address, phone) ───────────────────────────
        Label ratingLbl = new Label("⭐ " + h.ratingDisplay());

        Label bedsLbl = new Label("🛏 " + h.bedsAvailable() + " beds");

        Label addrLbl = new Label("📍 " + h.address());

        HBox metaRow = new HBox(14, ratingLbl, bedsLbl, addrLbl);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label phoneLbl = new Label("📞 " + h.phone());

        VBox card = new VBox(8, topRow, metaRow, phoneLbl, navBtn);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Dispatch panel  (NEW — replaces old buildButtonBar)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Builds the bottom action panel.
     * <ul>
     *   <li>Critical (3): shows auto-dispatch note + "Already Dispatched" disabled button</li>
     *   <li>Moderate (2): shows "Ambulance Recommended" button + nav buttons</li>
     *   <li>Low     (1): shows "Ambulance Available" button + nav buttons</li>
     * </ul>
     * For Moderate/Low a dialog is triggered on first render via Platform.runLater so
     * the scene is fully visible before the dialog appears.
     */
    private VBox buildDispatchPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(8, 0, 0, 0));

        // ── Nav buttons (always present) ──────────────────────────────────────
        Button newAssessBtn = new Button("← New Assessment");
        newAssessBtn.setId("newAssessBtn");
        newAssessBtn.setOnAction(e -> {
            DispatchStateService.getInstance().clear();
            stage.setScene(new SymptomController(stage).buildScene());
        });

        Button homeBtn = new Button("🏠 Home");
        homeBtn.setId("homeBtn");
        homeBtn.setOnAction(e -> {
            DispatchStateService.getInstance().clear();
            stage.setScene(new HomeController(stage).buildScene());
        });

        if (result.level() == 3) {
            // ── Critical: auto-dispatch immediately ───────────────────────────
            Label autoLabel = new Label("🚨  Critical level detected — Ambulance auto-dispatched.");

            Button dispatchedBtn = new Button("🚑  Ambulance Dispatched");
            dispatchedBtn.setId("autoDispatchedBtn");
            dispatchedBtn.setDisable(true);

            HBox navBar = new HBox(12, homeBtn, newAssessBtn, dispatchedBtn);
            navBar.setAlignment(Pos.CENTER_RIGHT);

            panel.getChildren().addAll(autoLabel, navBar);

            // Trigger dispatch after scene is shown
            javafx.application.Platform.runLater(this::performDispatch);

        } else {
            // ── Moderate / Low: ask user ──────────────────────────────────────
            String btnLabel = (result.level() == 2)
                    ? "🚑  Ambulance Recommended — Dispatch?"
                    : "🚑  Ambulance Available — Do you need it?";

            Button askBtn = new Button(btnLabel);
            askBtn.setId("askDispatchBtn");

            String btnColor = (result.level() == 2) ? "#FB8C00" : "#43A047";
            String shadowColor = (result.level() == 2)
                    ? "rgba(251,140,0,0.5)" : "rgba(67,160,71,0.5)";

            askBtn.setOnAction(e -> {
                if (!dispatched) {
                    showAmbulanceDialog(result.level(), askBtn);
                }
            });

            HBox navBar = new HBox(12, homeBtn, newAssessBtn, askBtn);
            navBar.setAlignment(Pos.CENTER_RIGHT);
            panel.getChildren().add(navBar);

            // Show the dialog automatically after the scene renders
            javafx.application.Platform.runLater(() -> {
                if (!dispatched) {
                    showAmbulanceDialog(result.level(), askBtn);
                }
            });
        }

        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Ambulance dialog  (NEW)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Shows a styled confirmation dialog asking the user whether to dispatch.
     * Updates {@code askBtn} text and disables it on confirmation.
     *
     * @param level   triage severity (2 = Moderate, 1 = Low)
     * @param askBtn  the dispatch button to update after decision
     */
    private void showAmbulanceDialog(int level, Button askBtn) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ambulance Dispatch");

        if (level == 2) {
            alert.setHeaderText("⚠  Moderate Severity — Ambulance Recommended");
            alert.setContentText(
                "Your condition is MODERATE.\n\n" +
                "An ambulance is recommended to ensure safe transport to hospital " +
                "and monitoring en route.\n\n" +
                "Would you like to dispatch an ambulance?"
            );
        } else {
            alert.setHeaderText("ℹ  Low Severity — Ambulance Available");
            alert.setContentText(
                "Your condition is LOW severity.\n\n" +
                "An ambulance is not strictly necessary but is available if you " +
                "would prefer assisted transport.\n\n" +
                "Would you like to dispatch an ambulance?"
            );
        }

        ButtonType yesBtn = new ButtonType("Yes, Dispatch Now",  ButtonBar.ButtonData.YES);
        ButtonType noBtn  = new ButtonType("No, I'll Go Myself", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesBtn, noBtn);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isPresent() && choice.get() == yesBtn) {
            performDispatch();
            askBtn.setText("🚑  Ambulance Dispatched!");
            askBtn.setDisable(true);
        }
        // If NO → stay on results screen, no dispatch
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Dispatch logic
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Dispatches the nearest available ambulance.
     *
     * For Critical (level 3) the {@link CriticalAlertController} modal is shown
     * first and then the scene transitions to Dispatch.
     * For Moderate/Low (confirmed by the user) the transition is direct.
     *
     * Idempotent — will not dispatch twice.
     */
    private void performDispatch() {
        if (dispatched) return;
        dispatched = true;

        AmbulanceService ambulanceService = new AmbulanceService();
        Ambulance ambulance = ambulanceService.dispatch();

        if (ambulance == null) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("No Ambulances Available");
            err.setHeaderText("All ambulances are currently deployed");
            err.setContentText("Please call 112 directly for emergency assistance.");
            err.showAndWait();
            dispatched = false; // allow retry
            return;
        }

        // Mark global dispatch state active (prevents double-dispatch from Home)
        DispatchStateService.getInstance().markActive();

        if (result.level() == 3) {
            // Show the critical alert dialog; it navigates to dispatch on OK
            new CriticalAlertController(stage, ambulance, ambulanceService).showAndProceed();
        } else {
            // Moderate / Low: user already confirmed via dialog — go straight to dispatch
            stage.setScene(new DispatchController(stage, ambulance, ambulanceService).buildScene());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Shared helpers
    // ══════════════════════════════════════════════════════════════════════════

    private VBox styledCard(VBox inner) {
        inner.setMaxWidth(Double.MAX_VALUE);
        return inner;
    }

    private Label cardTitle(String text) {
        Label lbl = new Label(text);
        return lbl;
    }
}
