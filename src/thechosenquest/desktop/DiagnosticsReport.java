package thechosenquest.desktop;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * Produces privacy-safe technical context for tester reports.
 *
 * User names, save paths, IP addresses, and save contents are deliberately excluded.
 * The resulting block can be pasted into the public bug form even when the game
 * itself has no network connection.
 */
final class DiagnosticsReport {
    private DiagnosticsReport() { }

    static String create() {
        StringBuilder report = new StringBuilder();
        report.append("The Chosen Quest Enhanced diagnostics\n");
        report.append("Version: ").append(AppVersion.VERSION).append('\n');
        report.append("Release tag: ").append(AppVersion.TAG).append('\n');
        report.append("Operating system: ")
            .append(System.getProperty("os.name", "Unknown")).append(' ')
            .append(System.getProperty("os.version", "")).append('\n');
        report.append("Architecture: ")
            .append(System.getProperty("os.arch", "Unknown")).append('\n');
        report.append("Java: ")
            .append(System.getProperty("java.version", "Unknown")).append('\n');
        report.append("Headless mode: ")
            .append(GraphicsEnvironment.isHeadless()).append('\n');
        if (!GraphicsEnvironment.isHeadless()) {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            report.append("Primary display: ")
                .append(screen.width).append('x').append(screen.height).append('\n');
        }
        report.append("Memory available: ")
            .append(Runtime.getRuntime().maxMemory() / (1024L * 1024L))
            .append(" MB\n");
        return report.toString();
    }

    static boolean copyToClipboard() {
        if (GraphicsEnvironment.isHeadless()) return false;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(create()), null);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
