package com.example.controller;

import com.example.model.Ambulance;
import com.example.model.User;
import com.example.service.AmbulanceService;
import com.example.service.UserSessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Critical Alert Dialog — shown BEFORE navigating to the dispatch screen
 * when a Critical (Level 3) triage result is detected.
 *
 * Usage:
 * <pre>
 *     new CriticalAlertController(stage, ambulance, ambulanceService)
 *         .showAndProceed();   // blocks until user clicks OK
 * </pre>
 *
 * After the user dismisses the dialog the caller's scene is already replaced
 * with the DispatchController scene.
 */
public class CriticalAlertController {

    private final Stage          ownerStage;
    private final Ambulance      ambulance;
    private final AmbulanceService ambulanceService;

    public CriticalAlertController(Stage ownerStage,
                                   Ambulance ambulance,
                                   AmbulanceService ambulanceService) {
        this.ownerStage      = ownerStage;
        this.ambulance       = ambulance;
        this.ambulanceService = ambulanceService;
    }

    /**
     * Displays the modal critical alert.  Blocks until the user clicks OK,
     * then replaces the owner stage scene with the Dispatch screen.
     */
    public void showAndProceed() {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ownerStage);

        VBox root = buildContent(dialog);
        Scene scene = new Scene(root, 500, 360);
        dialog.setScene(scene);
        dialog.showAndWait();          // blocks here

        // After OK — navigate to dispatch
        ownerStage.setScene(new DispatchController(ownerStage, ambulance, ambulanceService).buildScene());
    }

    // ── Dialog content ────────────────────────────────────────────────────────

    private VBox buildContent(Stage dialog) {
        // ── Pulsing siren emoji header ────────────────────────────────────────
        Label siren = new Label("🚨");

        Label title = new Label("CRITICAL SITUATION DETECTED");
        title.setAlignment(Pos.CENTER);

        Separator sep1 = new Separator();

        // ── Body ──────────────────────────────────────────────────────────────
        Label bodyLbl = new Label(
            "Based on your symptoms, an ambulance is being dispatched\n" +
            "to your location immediately.\n\n" +
            "Please stay calm and follow the first-aid instructions\n" +
            "shown on the next screen."
        );
        bodyLbl.setWrapText(true);
        bodyLbl.setAlignment(Pos.CENTER);
        bodyLbl.setMaxWidth(440);

        // ── Info chips ────────────────────────────────────────────────────────
        UserSessionManager session = UserSessionManager.getInstance();
        String location = session.locationDisplay();
        int    eta      = ambulanceService.calculateETA(ambulance);

        HBox locChip = chip("📍 " + location,   "rgba(41,182,246,0.12)", "#29B6F6");
        HBox etaChip = chip("🚑 ETA: ~" + eta + " min", "rgba(255,215,64,0.12)", "#FFD740");
        HBox ambChip = chip("🆔 " + ambulance.getNumberPlate() + "  •  " + ambulance.getDriverName(),
                            "rgba(0,230,118,0.10)", "#00E676");

        VBox chips = new VBox(8, locChip, etaChip, ambChip);
        chips.setAlignment(Pos.CENTER);

        Separator sep2 = new Separator();

        // ── Emergency call note ───────────────────────────────────────────────
        Label callNote = new Label("Emergency services notified  •  Call 112 if needed");
        callNote.setAlignment(Pos.CENTER);

        // ── OK button ─────────────────────────────────────────────────────────
        Button okBtn = new Button("OK — Go to Dispatch Info");
        okBtn.setId("criticalAlertOkBtn");
        okBtn.setOnAction(e -> dialog.close());

        // ── Root ──────────────────────────────────────────────────────────────
        VBox root = new VBox(16,
            siren, title, sep1,
            bodyLbl, chips,
            sep2, callNote, okBtn
        );
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28, 32, 28, 32));
        return root;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HBox chip(String text, String bg, String fg) {
        Label lbl = new Label(text);

        HBox box = new HBox(lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(7, 18, 7, 18));
        return box;
    }
}
