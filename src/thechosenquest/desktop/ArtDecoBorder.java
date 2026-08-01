package thechosenquest.desktop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.border.AbstractBorder;

/**
 * Reusable stepped fantasy Art Deco frame. It is deliberately code-rendered so
 * panels can resize and recolor without stretching a raster border.
 */
final class ArtDecoBorder extends AbstractBorder {
    private static final long serialVersionUID = 1L;
    private final Color accent;
    private final int padding;
    private final boolean selected;
    private final boolean layeredShell;

    ArtDecoBorder(Color accent, int padding) {
        this(accent, padding, false, false);
    }

    ArtDecoBorder(Color accent, int padding, boolean selected) {
        this(accent, padding, selected, false);
    }

    private ArtDecoBorder(Color accent, int padding, boolean selected,
                          boolean layeredShell) {
        this.accent = accent == null ? UiTheme.GOLD : accent;
        this.padding = Math.max(4, padding);
        this.selected = selected;
        this.layeredShell = layeredShell;
    }

    static ArtDecoBorder shell(Color accent, int padding) {
        return new ArtDecoBorder(accent, Math.max(7, padding),
            false, true);
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(padding, padding, padding, padding);
    }

    @Override
    public Insets getBorderInsets(Component component, Insets insets) {
        insets.top = insets.left = insets.bottom = insets.right = padding;
        return insets;
    }

    @Override
    public boolean isBorderOpaque() { return false; }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y,
                            int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        int cut = Math.min(12, Math.max(5, Math.min(width, height) / 7));
        int right = x + width - 1;
        int bottom = y + height - 1;

        float opacity = selected ? .92f : layeredShell ? .72f : .62f;
        if (UiBorderAssets.paintPanelFrame(g, x, y, width, height, opacity)) {
            g.dispose();
            return;
        }

        // Deterministic fallback for incomplete/older installations that do not
        // contain the optional painterly frame assets.
        Path2D outer = clippedRect(x, y, right, bottom, cut);
        if (layeredShell) {
            g.setStroke(new BasicStroke(4f));
            g.setColor(new Color(0, 0, 0, 170));
            g.draw(clippedRect(x + 2, y + 2, right - 2, bottom - 2,
                Math.max(4, cut - 1)));
        }
        g.setStroke(new BasicStroke(selected ? 2f : 1.2f));
        g.setColor(selected ? accent : translucent(accent, 180));
        g.draw(outer);

        if (width > 30 && height > 22) {
            Path2D inner = clippedRect(x + 3, y + 3, right - 3, bottom - 3,
                Math.max(3, cut - 2));
            g.setStroke(new BasicStroke(1f));
            g.setColor(translucent(selected ? UiTheme.GOLD_LIGHT : accent,
                selected ? 185 : 75));
            g.draw(inner);
        }
        if (layeredShell && width > 40 && height > 30) {
            Path2D innerRail = clippedRect(x + 7, y + 7,
                right - 7, bottom - 7, Math.max(3, cut - 5));
            g.setStroke(new BasicStroke(1f));
            g.setColor(translucent(UiTheme.GOLD_LIGHT, 58));
            g.draw(innerRail);
        }

        paintCornerStep(g, x, y, 1, 1, cut);
        paintCornerStep(g, right, y, -1, 1, cut);
        paintCornerStep(g, x, bottom, 1, -1, cut);
        paintCornerStep(g, right, bottom, -1, -1, cut);
        g.dispose();
    }

    private void paintCornerStep(Graphics2D g, int x, int y, int dx, int dy,
                                 int cut) {
        Path2D step = new Path2D.Double();
        step.moveTo(x + dx * 2, y + dy * (cut + 6));
        step.lineTo(x + dx * 2, y + dy * (cut - 1));
        step.lineTo(x + dx * (cut - 1), y + dy * 2);
        step.lineTo(x + dx * (cut + 6), y + dy * 2);
        g.setStroke(new BasicStroke(selected ? 2.2f : 1.3f,
            BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.setColor(translucent(selected ? UiTheme.GOLD_LIGHT : accent,
            selected ? 230 : 155));
        g.draw(step);
    }

    private static Path2D clippedRect(int left, int top, int right, int bottom,
                                      int cut) {
        Path2D path = new Path2D.Double();
        path.moveTo(left + cut, top);
        path.lineTo(right - cut, top);
        path.lineTo(right, top + cut);
        path.lineTo(right, bottom - cut);
        path.lineTo(right - cut, bottom);
        path.lineTo(left + cut, bottom);
        path.lineTo(left, bottom - cut);
        path.lineTo(left, top + cut);
        path.closePath();
        return path;
    }

    private static Color translucent(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
            Math.max(0, Math.min(255, alpha)));
    }
}
