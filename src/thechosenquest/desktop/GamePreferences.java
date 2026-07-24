package thechosenquest.desktop;

import java.util.prefs.Preferences;

/** Persistent non-audio interface preferences. */
final class GamePreferences {
    private static final String REDUCED_MOTION_KEY = "interface.reducedMotion";
    private static final String RELEASE_NOTES_VERSION_KEY = "releaseNotes.lastSeenVersion";

    private final Preferences preferences;
    private boolean reducedMotion;

    GamePreferences() {
        Preferences loaded = null;
        boolean savedReducedMotion = false;
        try {
            loaded = Preferences.userNodeForPackage(GamePreferences.class);
            savedReducedMotion = loaded.getBoolean(REDUCED_MOTION_KEY, false);
        } catch (RuntimeException ignored) {
            loaded = null;
        }
        preferences = loaded;
        reducedMotion = savedReducedMotion;
    }

    boolean isReducedMotion() {
        return reducedMotion;
    }

    void setReducedMotion(boolean value) {
        reducedMotion = value;
        if (preferences != null) {
            try {
                preferences.putBoolean(REDUCED_MOTION_KEY, value);
            } catch (RuntimeException ignored) { }
        }
    }

    boolean shouldShowReleaseNotes(String version) {
        if (version == null || version.length() == 0 || preferences == null) return true;
        try {
            return !version.equals(preferences.get(RELEASE_NOTES_VERSION_KEY, ""));
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    void markReleaseNotesSeen(String version) {
        if (version == null || version.length() == 0 || preferences == null) return;
        try {
            preferences.put(RELEASE_NOTES_VERSION_KEY, version);
        } catch (RuntimeException ignored) { }
    }
}
