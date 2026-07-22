package thechosenquest.desktop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JProgressBar;

/** Stable, look-and-feel-independent compact game status meter. */
final class StatusBar extends JProgressBar {
    private static final long serialVersionUID = 1L;
    private final Color fill;

    StatusBar(Color fill) {
        this.fill = fill;
        setOpaque(false);
        setStringPainted(true);
        setFont(UiTheme.body(java.awt.Font.BOLD, 10));
        setPreferredSize(new Dimension(220, 26));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        setMinimumSize(new Dimension(80, 26));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        int width = Math.max(1, getWidth());
        int trackY = Math.max(16, getHeight() - 8);
        int trackHeight = Math.min(8, getHeight() - trackY);
        g.setColor(new Color(46, 34, 26));
        g.fillRoundRect(0, trackY, width, trackHeight, 8, 8);
        double range = Math.max(1, getMaximum() - getMinimum());
        double progress = Math.max(0, Math.min(1,
            (getValue() - getMinimum()) / range));
        int filled = (int) Math.round(width * progress);
        if (filled > 0) {
            g.setColor(fill);
            g.fillRoundRect(0, trackY, filled, trackHeight, 8, 8);
        }
        String text = getString();
        if (text != null) {
            g.setFont(getFont());
            FontMetrics metrics = g.getFontMetrics();
            String[] parts = text.trim().split("\\s{2,}", 2);
            String label = parts[0];
            String value = parts.length > 1 ? parts[1] : "";
            int baseline = Math.min(12, metrics.getAscent());
            g.setColor(UiTheme.TEXT);
            g.drawString(label, 0, baseline);
            if (!value.isEmpty()) {
                g.drawString(value, Math.max(0, getWidth() - metrics.stringWidth(value)), baseline);
            }
        }
        g.dispose();
    }

    @Override
    protected void paintBorder(Graphics graphics) {
        // Border is painted with the meter for consistent cross-platform output.
    }
}
