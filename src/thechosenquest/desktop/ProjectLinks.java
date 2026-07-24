package thechosenquest.desktop;

import java.awt.Desktop;
import java.net.URI;

/** Canonical public project links shared by title, settings, and release UI. */
final class ProjectLinks {
    static final String FEEDBACK = "https://forms.gle/GJLCPtzP7LN9PeBeA";
    static final String BUG_REPORT = "https://forms.gle/F8LtxPn5sKbwPAxX9";
    static final String RELEASES =
        "https://github.com/Rebelord/The-Chosen-Quest-Enhanced/releases";

    private ProjectLinks() { }

    static boolean open(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (Exception ignored) {
            // The URL remains available in tooltips, packaged instructions, and help text.
        }
        return false;
    }
}
