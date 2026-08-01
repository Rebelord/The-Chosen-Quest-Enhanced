package thechosenquest.desktop;

/**
 * Public application version used by the UI, release archives, and Git tags.
 * This is intentionally independent from GameEngine's save-schema version.
 */
final class AppVersion {
    static final String VERSION = "0.7.0-beta.2";
    static final String TAG = "v" + VERSION;
    static final String DISPLAY_NAME = TAG;

    private AppVersion() { }
}
