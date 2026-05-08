package com.example.controller;

import com.example.model.User;
import com.example.service.UserPersistenceService;
import com.example.service.UserSessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Login / Registration Screen — shown on first launch only.
 *
 * Returning users (detected via saved file) see their profile pre-filled and
 * can proceed with one click. New users fill in the form to register.
 *
 * On success the user is stored in {@link UserSessionManager} (and to disk)
 * then the app transitions to the Home screen.
 */
public class LoginController {

    private final Stage   stage;
    private final boolean returningUser;

    // Form fields kept as instance vars so handlers can access them
    private TextField nameFld;
    private TextField phoneFld;
    private TextField locationFld;
    private TextField xFld;
    private TextField yFld;
    private TextField emergencyFld;
    private ComboBox<String> bloodGroupBox;
    private Label statusLabel;

    public LoginController(Stage stage) {
        this.stage         = stage;
        this.returningUser = UserPersistenceService.userExists();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Scene
    // ══════════════════════════════════════════════════════════════════════════

    public Scene buildScene() {
        VBox root = new VBox(0);

        root.getChildren().add(buildHeader());

        ScrollPane scroll = new ScrollPane(buildForm());
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().add(scroll);
        return new Scene(root, 600, 640);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        Label icon  = new Label("🚑");

        Label title = new Label("AmbulanceNow");

        Label sub = new Label(returningUser ? "Welcome back!" : "Create your profile");

        VBox text = new VBox(2, title, sub);
        text.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14, icon, text, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 24, 18, 24));
        return header;
    }

    // ── Form ──────────────────────────────────────────────────────────────────

    private VBox buildForm() {
        VBox form = new VBox(16);
        form.setPadding(new Insets(24, 28, 32, 28));

        // Returning-user notice
        if (returningUser) {
            User saved = UserPersistenceService.load().orElse(null);
            if (saved != null) {
                Label notice = new Label(
                    "✅  Profile found for " + saved.getName() +
                    "  •  Tap 'Continue' to proceed, or fill in new details to update."
                );
                notice.setWrapText(true);
                notice.setMaxWidth(Double.MAX_VALUE);
                form.getChildren().add(notice);
            }
        }

        // ── Fields ────────────────────────────────────────────────────────────
        nameFld      = styledField("Full name *", "e.g. Aaditya Chandragupta");
        phoneFld     = styledField("Phone number *", "10-digit mobile number");
        locationFld  = styledField("Location / Address *", "e.g. Sector 17, Chandigarh");

        HBox coordRow = new HBox(12);
        xFld = styledField("Simulated X (0-100)", "e.g. 50");
        yFld = styledField("Simulated Y (0-100)", "e.g. 50");
        HBox.setHgrow(xFld, Priority.ALWAYS);
        HBox.setHgrow(yFld, Priority.ALWAYS);
        coordRow.getChildren().addAll(
            labelledField("X Coordinate", xFld),
            labelledField("Y Coordinate", yFld)
        );

        emergencyFld = styledField("Emergency contact (optional)", "Family member phone");

        bloodGroupBox = new ComboBox<>();
        bloodGroupBox.getItems().addAll("—", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        bloodGroupBox.setValue("—");
        bloodGroupBox.setMaxWidth(Double.MAX_VALUE);

        statusLabel = new Label("");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        // Pre-fill if returning user
        if (returningUser) {
            UserPersistenceService.load().ifPresent(this::prefill);
        }

        form.getChildren().addAll(
            labelledField("Full Name *", nameFld),
            labelledField("Phone Number *", phoneFld),
            labelledField("Location / Address *", locationFld),
            coordRow,
            labelledField("Emergency Contact", emergencyFld),
            labelledField("Blood Group", bloodGroupBox),
            statusLabel,
            buildButtonRow()
        );

        return form;
    }

    private HBox buildButtonRow() {
        Button continueBtn = new Button(returningUser ? "✅  Continue" : "✅  Register & Continue");
        continueBtn.setId("loginContinueBtn");
        continueBtn.setOnAction(e -> handleContinue());

        Button skipBtn = new Button("Skip for now");
        skipBtn.setId("loginSkipBtn");
        skipBtn.setOnAction(e -> loginAsGuest());

        HBox row = new HBox(16, continueBtn, skipBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 0, 0, 0));
        return row;
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleContinue() {
        // If returning user and fields are unchanged / empty → just re-load
        if (returningUser && nameFld.getText().isBlank()) {
            UserPersistenceService.load().ifPresentOrElse(u -> {
                UserSessionManager.getInstance().login(u);
                goHome();
            }, this::loginAsGuest);
            return;
        }

        if (!validate()) return;

        User user = buildUserFromFields();
        UserPersistenceService.save(user);
        UserSessionManager.getInstance().login(user);
        goHome();
    }

    private void loginAsGuest() {
        User guest = new User("Guest", "0000000000", "Sector 17, Chandigarh", 50.0, 50.0);
        UserSessionManager.getInstance().login(guest);
        goHome();
    }

    private void goHome() {
        stage.setScene(new HomeController(stage).buildScene());
    }

    // ── Validation & model builder ────────────────────────────────────────────

    private boolean validate() {
        statusLabel.setText("");
        String name  = nameFld.getText().trim();
        String phone = phoneFld.getText().trim();
        String loc   = locationFld.getText().trim();

        if (name.isEmpty()) {
            statusLabel.setText("⚠  Please enter your name.");
            return false;
        }
        if (phone.length() < 10) {
            statusLabel.setText("⚠  Enter a valid phone number (min 10 digits).");
            return false;
        }
        if (loc.isEmpty()) {
            statusLabel.setText("⚠  Please enter your location.");
            return false;
        }
        double x = parseCoord(xFld.getText(), 50.0);
        double y = parseCoord(yFld.getText(), 50.0);
        if (x < 0 || x > 100 || y < 0 || y > 100) {
            statusLabel.setText("⚠  Coordinates must be between 0 and 100.");
            return false;
        }
        return true;
    }

    private User buildUserFromFields() {
        User u = new User(
            nameFld.getText().trim(),
            phoneFld.getText().trim(),
            locationFld.getText().trim(),
            parseCoord(xFld.getText(), 50.0),
            parseCoord(yFld.getText(), 50.0)
        );
        String em = emergencyFld.getText().trim();
        if (!em.isEmpty()) u.setEmergencyContact(em);
        String bg = bloodGroupBox.getValue();
        if (bg != null && !bg.equals("—")) u.setBloodGroup(bg);
        return u;
    }

    private void prefill(User u) {
        nameFld.setText(u.getName());
        phoneFld.setText(u.getPhoneNumber());
        locationFld.setText(u.getLocation());
        xFld.setText(String.valueOf(u.getX()));
        yFld.setText(String.valueOf(u.getY()));
        if (u.getEmergencyContact() != null) emergencyFld.setText(u.getEmergencyContact());
        if (u.getBloodGroup() != null) bloodGroupBox.setValue(u.getBloodGroup());
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private TextField styledField(String prompt, String hint) {
        TextField tf = new TextField();
        tf.setPromptText(hint);
        return tf;
    }

    private VBox labelledField(String label, Control field) {
        Label lbl = new Label(label);
        VBox box = new VBox(5, lbl, field);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static double parseCoord(String text, double fallback) {
        try { return Double.parseDouble(text.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
