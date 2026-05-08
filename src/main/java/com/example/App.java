package com.example;

import com.example.controller.HomeController;
import com.example.controller.LoginController;
import com.example.service.UserPersistenceService;
import com.example.service.UserSessionManager;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application entry point.
 *
 * Launch flow:
 *   1. Check {@link UserPersistenceService} for a saved user profile.
 *   2. If found   → auto-login via {@link UserSessionManager} → Home screen.
 *   3. If missing → show {@link LoginController} (register / skip) → Home screen.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("AmbulanceNow — Emergency Decision Support");
        stage.setResizable(false);

        // ── Auto-login if user data already exists ────────────────────────────
        UserPersistenceService.load().ifPresent(user ->
            UserSessionManager.getInstance().login(user)
        );

        if (UserSessionManager.getInstance().isLoggedIn()) {
            // Returning user — skip login screen, go straight to Home
            stage.setScene(new HomeController(stage).buildScene());
        } else {
            // First-time user — show registration / login screen
            stage.setScene(new LoginController(stage).buildScene());
        }

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}