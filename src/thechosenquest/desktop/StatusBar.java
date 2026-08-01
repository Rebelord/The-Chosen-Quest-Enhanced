package thechosenquest.desktop;

import java.awt.BasicStroke;
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
    StatusBar(Color fill) {
        setForeground(fill);
        setOpaque(false);
        setStringPainted(true);
        setFont(UiTheme.title(java.awt.Font.BOLD, 10));
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
        int trackY = Math.max(16, getHeight() - 9);
        int trackHeight = Math.min(7, getHeight() - trackY - 1);
        int trackX = 2;
        int trackWidth = Math.max(1, width - 4);
        g.setColor(new Color(8, 6, 5, 205));
        g.fillRoundRect(trackX - 1, trackY - 1,
            trackWidth + 2, trackHeight + 2, 7, 7);
        g.setColor(new Color(56, 39, 29));
        g.fillRoundRect(trackX, trackY, trackWidth, trackHeight, 7, 7);
        double range = Math.max(1, getMaximum() - getMinimum());
        double progress = Math.max(0, Math.min(1,
            (getValue() - getMinimum()) / range));
        int filled = (int) Math.round(trackWidth * progress);
        if (filled > 0) {
            Color fill = getForeground();
            g.setColor(new Color(fill.getRed(), fill.getGreen(),
                fill.getBlue(), 52));
            g.fillRoundRect(trackX - 1, trackY - 2,
                Math.min(trackWidth + 2, filled + 2), trackHeight + 4, 8, 8);
            g.setColor(fill);
            g.fillRoundRect(trackX, trackY, filled, trackHeight, 7, 7);
            g.setColor(new Color(255, 255, 255, 48));
            g.drawLine(trackX + 2, trackY + 1,
                Math.max(trackX + 2, trackX + filled - 3), trackY + 1);
        }
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(UiTheme.GOLD.getRed(), UiTheme.GOLD.getGreen(),
            UiTheme.GOLD.getBlue(), 90));
        g.drawLine(trackX + 2, trackY + trackHeight + 1,
            Math.max(trackX + 2, width - trackX - 2),
            trackY + trackHeight + 1);
        g.drawLine(trackX, trackY - 1, trackX, trackY + trackHeight + 1);
        g.drawLine(width - trackX - 1, trackY - 1,
            width - trackX - 1, trackY + trackHeight + 1);
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
