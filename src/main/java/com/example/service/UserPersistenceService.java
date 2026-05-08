package com.example.service;

import com.example.model.User;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;

/**
 * Saves and loads the current user profile to/from a local file using Java
 * serialization.  A backup copy is kept so corrupted data can be recovered.
 *
 * Storage location: {@code user_data/} directory next to the working directory.
 */
public final class UserPersistenceService {

    private static final String DIR         = "user_data";
    private static final String ACTIVE_FILE = DIR + "/current_user.dat";
    private static final String BACKUP_FILE = DIR + "/user_backup.dat";

    static {
        try {
            Files.createDirectories(Paths.get(DIR));
        } catch (IOException e) {
            System.err.println("[UserPersistence] Could not create storage dir: " + e.getMessage());
        }
    }

    private UserPersistenceService() {}   // utility class

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns {@code true} when a saved user profile exists on disk. */
    public static boolean userExists() {
        return Files.exists(Paths.get(ACTIVE_FILE));
    }

    /**
     * Serialises {@code user} to disk.
     * The previous active file is promoted to backup before writing.
     */
    public static void save(User user) {
        if (user == null) return;
        try {
            // Rotate active → backup
            Path active = Paths.get(ACTIVE_FILE);
            if (Files.exists(active)) {
                Files.copy(active, Paths.get(BACKUP_FILE),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(ACTIVE_FILE))) {
                oos.writeObject(user);
            }
            System.out.println("[UserPersistence] Saved: " + user.getName());
        } catch (IOException e) {
            System.err.println("[UserPersistence] Save failed: " + e.getMessage());
        }
    }

    /**
     * Deserialises the stored user.  Falls back to the backup if the active
     * file is corrupted.
     *
     * @return {@code Optional} with the user, or empty when nothing is stored.
     */
    public static Optional<User> load() {
        Optional<User> u = loadFrom(ACTIVE_FILE);
        if (u.isEmpty()) {
            System.out.println("[UserPersistence] Attempting backup restore…");
            u = loadFrom(BACKUP_FILE);
        }
        return u;
    }

    /** Deletes both the active and backup files. */
    public static void delete() {
        silentDelete(ACTIVE_FILE);
        silentDelete(BACKUP_FILE);
        System.out.println("[UserPersistence] User data deleted.");
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static Optional<User> loadFrom(String path) {
        if (!Files.exists(Paths.get(path))) return Optional.empty();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            User user = (User) ois.readObject();
            System.out.println("[UserPersistence] Loaded: " + user.getName() + " from " + path);
            return Optional.of(user);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[UserPersistence] Load failed (" + path + "): " + e.getMessage());
            return Optional.empty();
        }
    }

    private static void silentDelete(String path) {
        try { Files.deleteIfExists(Paths.get(path)); }
        catch (IOException ignored) {}
    }
}
