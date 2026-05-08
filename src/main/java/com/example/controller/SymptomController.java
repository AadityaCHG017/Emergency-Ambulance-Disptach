package com.example.controller;

import com.example.model.Ambulance;
import com.example.model.Hospital;
import com.example.service.AmbulanceService;
import com.example.service.FirstAidService;
import com.example.service.HospitalService;
import com.example.service.TriageService;
import com.example.model.TriageResult;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Symptom Selection Screen — NON-EMERGENCY path.
 *
 * User selects symptoms → triage runs → ResultController shown.
 * Optional: user may still call an ambulance from the result.
 */
public class SymptomController {

    private final Stage stage;

    private static final String[] SYMPTOMS = {
        "chest pain", "breathing difficulty", "unconscious",
        "heavy bleeding", "fracture", "high fever", "headache", "cold"
    };

    public SymptomController(Stage stage) {
        this.stage = stage;
    }

    /** Builds and returns the Symptom Selection screen Scene. */
    public Scene buildScene() {
        VBox root = new VBox(0);

        // ── Header ────────────────────────────────────────────────────────────
        root.getChildren().add(buildHeader());

        // ── Content ───────────────────────────────────────────────────────────
        VBox content = new VBox(18);
        content.setPadding(new Insets(24, 24, 32, 24));

        // Intro card
        Label introLbl = new Label(
            "Check all symptoms that apply. The triage engine will assess your " +
            "condition and suggest the best hospitals near you."
        );
        introLbl.setMaxWidth(Double.MAX_VALUE);
        VBox introCard = styledCard(new VBox(introLbl));

        // Symptoms grid
        List<CheckBox> checkBoxes = new ArrayList<>();
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        for (int i = 0; i < SYMPTOMS.length; i++) {
            CheckBox cb = new CheckBox(capitalize(SYMPTOMS[i]));
            cb.setId("cb_" + SYMPTOMS[i].replace(" ", "_"));
            checkBoxes.add(cb);
            grid.add(cb, i % 2, i / 2);
        }

        // Status message
        Label statusLabel = new Label("");

        // Analyze button
        Button analyzeBtn = buildPrimaryBtn("Analyze Symptoms  →", "analyzeBtn",
            "#E53935", "#EF5350", "rgba(229,57,53,0.5)", "rgba(239,83,80,0.7)");
        analyzeBtn.setOnAction(e -> {
            List<String> selected = getSelected(checkBoxes);
            if (selected.isEmpty()) {
                statusLabel.setText("⚠  Please select at least one symptom.");
                return;
            }
            runTriage(selected);
        });

        // Bottom bar
        Button backBtn = buildGhostBtn("← Home", "backBtn");
        backBtn.setOnAction(e -> stage.setScene(new HomeController(stage).buildScene()));

        Button emergencyBtn = buildDangerOutlineBtn("🆘 Emergency Dispatch", "symEmergencyBtn");
        emergencyBtn.setOnAction(e -> handleEmergencyDispatch());

        HBox bottomBar = new HBox(10, backBtn, emergencyBtn);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomBarFull = new HBox(0, analyzeBtn, spacer, bottomBar);
        bottomBarFull.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(introCard, grid, statusLabel, bottomBarFull);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        root.getChildren().add(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return new Scene(root, 600, 580);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        Label title = new Label("Symptom Assessment");

        Label badge = new Label("NON-EMERGENCY");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button homeBtn = new Button("🏠");
        homeBtn.setOnAction(e -> stage.setScene(new HomeController(stage).buildScene()));

        HBox header = new HBox(12, title, badge, spacer, homeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 16, 24));
        return header;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> getSelected(List<CheckBox> checkBoxes) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                result.add(SYMPTOMS[i]);
            }
        }
        return result;
    }

    /** NON-EMERGENCY triage → ResultController */
    private void runTriage(List<String> selected) {
        HospitalService hospitalService = new HospitalService();
        List<Hospital> hospitals = hospitalService.getAllHospitals();
        TriageService triageService = new TriageService();
        TriageResult result = triageService.analyze(selected, hospitals);
        stage.setScene(new ResultController(stage, result, selected).buildScene());
    }

    /** Optional emergency dispatch from non-emergency screen */
    private void handleEmergencyDispatch() {
        AmbulanceService ambulanceService = new AmbulanceService();
        Ambulance ambulance = ambulanceService.dispatch();
        if (ambulance == null) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("No Ambulances"); a.setHeaderText("All units deployed");
            a.setContentText("Call 112 directly."); a.showAndWait();
            return;
        }
        stage.setScene(new DispatchController(stage, ambulance, ambulanceService).buildScene());
    }

    // ── Styled button helpers ─────────────────────────────────────────────────

    private Button buildPrimaryBtn(String text, String id,
                                   String bg, String bgHover,
                                   String shadow, String shadowHover) {
        Button btn = new Button(text);
        btn.setId(id);
        String base = String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-background-radius: 10px;
            -fx-padding: 12 30;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, %s, 12, 0, 0, 4);
        """, bg, shadow);
        String hover = String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-background-radius: 10px;
            -fx-padding: 12 30;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, %s, 16, 0, 0, 6);
        """, bgHover, shadowHover);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button buildGhostBtn(String text, String id) {
        Button btn = new Button(text);
        btn.setId(id);
        return btn;
    }

    private Button buildDangerOutlineBtn(String text, String id) {
        Button btn = new Button(text);
        btn.setId(id);
        return btn;
    }

    private VBox styledCard(VBox inner) {
        inner.setMaxWidth(Double.MAX_VALUE);
        return inner;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
