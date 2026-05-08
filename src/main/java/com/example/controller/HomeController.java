package com.example.controller;

import com.example.model.Ambulance;
import com.example.service.AmbulanceService;
import com.example.service.DispatchStateManager;
import com.example.service.DispatchStateService;
import com.example.service.UserSessionManager;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Home Screen — two-path entry point.
 *
 * EMERGENCY path : instant ambulance dispatch → DispatchController
 * NON-EMERGENCY  : symptom selection first    → SymptomController
 */
public class HomeController {

    private final Stage stage;

    public HomeController(Stage stage) {
        this.stage = stage;
    }

    /** Builds and returns the Home screen Scene. */
    public Scene buildScene() {
        VBox root = new VBox(0);

        // ── Top bar ──────────────────────────────────────────────────────────
        root.getChildren().add(buildTopBar());

        // ── Hero section ─────────────────────────────────────────────────────
        root.getChildren().add(buildHeroSection());

        // ── Two path cards ───────────────────────────────────────────────────
        root.getChildren().add(buildPathCards());

        // ── Footer note ──────────────────────────────────────────────────────
        root.getChildren().add(buildFooter());

        return new Scene(root, 620, 580);
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        Label appName = new Label("🚑  AmbulanceNow");

        // Pulsing live dot
        Circle dot = new Circle(5, Color.web("#00E676"));
        Label liveLabel = new Label("LIVE");
        HBox liveBadge = new HBox(5, dot, liveLabel);
        liveBadge.setAlignment(Pos.CENTER);
        liveBadge.setPadding(new Insets(3, 10, 3, 10));

        // Pulse animation on the dot
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO,     new KeyValue(dot.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.8), new KeyValue(dot.opacityProperty(), 0.2)),
            new KeyFrame(Duration.seconds(1.6), new KeyValue(dot.opacityProperty(), 1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        UserSessionManager session = UserSessionManager.getInstance();
        String userName = session.isLoggedIn() ? session.getCurrentUser().getName() : null;
        String locText  = session.locationDisplay();

        Label location = new Label("📍  " + locText);

        HBox right;
        if (userName != null) {
            Label userLbl = new Label("👤  " + userName);
            right = new HBox(12, userLbl, location);
        } else {
            right = new HBox(location);
        }
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox(12, appName, liveBadge, spacer, right);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 24, 14, 24));
        return bar;
    }

    // ── Hero section ──────────────────────────────────────────────────────────

    private VBox buildHeroSection() {
        Label headline = new Label("Emergency Dispatch\n& Triage System");
        headline.setAlignment(Pos.CENTER);

        Label sub = new Label("Instant ambulance dispatch · Live tracking · Smart hospital routing");
        sub.setAlignment(Pos.CENTER);

        VBox hero = new VBox(10, headline, sub);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(28, 24, 20, 24));
        return hero;
    }

    // ── Two path cards ────────────────────────────────────────────────────────

    private HBox buildPathCards() {
        VBox emergencyCard = buildEmergencyCard();
        VBox nonEmergencyCard = buildNonEmergencyCard();

        HBox cards = new HBox(16, emergencyCard, nonEmergencyCard);
        cards.setAlignment(Pos.CENTER);
        cards.setPadding(new Insets(0, 24, 20, 24));
        HBox.setHgrow(emergencyCard, Priority.ALWAYS);
        HBox.setHgrow(nonEmergencyCard, Priority.ALWAYS);
        return cards;
    }

    /** Red EMERGENCY card — instant dispatch. */
    private VBox buildEmergencyCard() {
        Label icon = new Label("🆘");

        Label title = new Label("EMERGENCY");

        Label desc = new Label("Ambulance dispatched\ninstantly. No waiting.\nTell us symptoms after.");
        desc.setAlignment(Pos.CENTER);

        Button dispatchBtn = new Button("DISPATCH NOW");
        dispatchBtn.setId("dispatchNowBtn");
        dispatchBtn.setMaxWidth(Double.MAX_VALUE);
        dispatchBtn.setOnAction(e -> handleEmergencyDispatch());

        VBox card = new VBox(12, icon, title, desc, dispatchBtn);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24, 20, 24, 20));
        return card;
    }

    /** Blue NON-EMERGENCY card — symptom assessment first. */
    private VBox buildNonEmergencyCard() {
        Label icon = new Label("🏥");

        Label title = new Label("NON-EMERGENCY");

        Label desc = new Label("Select symptoms first.\nGet triage result and\nrecommended hospitals.");
        desc.setAlignment(Pos.CENTER);

        Button assessBtn = new Button("START ASSESSMENT");
        assessBtn.setId("startAssessBtn");
        assessBtn.setMaxWidth(Double.MAX_VALUE);
        assessBtn.setOnAction(e -> stage.setScene(new SymptomController(stage).buildScene()));

        VBox card = new VBox(12, icon, title, desc, assessBtn);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24, 20, 24, 20));
        return card;
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private Label buildFooter() {
        UserSessionManager s = UserSessionManager.getInstance();
        String loc = s.locationDisplay();
        Label footer = new Label("User location: " + loc + "  •  5 ambulances in fleet");
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.setAlignment(Pos.CENTER);
        return footer;
    }

    // ── Emergency dispatch handler ─────────────────────────────────────────────

    /**
     * EMERGENCY FLOW:
     *   1. Guard: block if ambulance already active.
     *   2. Instantiate AmbulanceService.
     *   3. Dispatch nearest ambulance.
     *   4. Mark dispatch active in DispatchStateService.
     *   5. Navigate to DispatchController.
     */
    private void handleEmergencyDispatch() {
        // Guard: block if any active dispatch cycle is running
        if (DispatchStateManager.getInstance().getState().isActive() ||
            DispatchStateService.getInstance().isAmbulanceActive()) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Ambulance Already Active");
            warn.setHeaderText("⚠  An ambulance is already active.");
            warn.setContentText(
                "A dispatch is already in progress.\n" +
                "Please complete or cancel it before requesting another.");
            warn.showAndWait();
            return;
        }

        AmbulanceService ambulanceService = new AmbulanceService();
        Ambulance ambulance = ambulanceService.dispatch();

        if (ambulance == null) {
            showNoAmbulanceAlert();
            return;
        }

        // Mark both singletons active
        DispatchStateService.getInstance().markActive();
        // DispatchStateManager transitions are driven by DispatchController

        DispatchController dispatchController =
            new DispatchController(stage, ambulance, ambulanceService);
        stage.setScene(dispatchController.buildScene());
    }

    private void showNoAmbulanceAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("No Ambulances Available");
        alert.setHeaderText("All ambulances are currently deployed");
        alert.setContentText("Please call 112 directly for emergency assistance.");
        alert.showAndWait();
    }
}
