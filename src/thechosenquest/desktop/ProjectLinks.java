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
            /*
             * The macOS `open` command avoids initializing the legacy AWT
             * Desktop bridge. That bridge can crash inside libzip on old
             * browser-plugin Java 8 installations used by some playtesters.
             */
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac")) {
                new ProcessBuilder("/usr/bin/open", url).start();
                return true;
            }
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
