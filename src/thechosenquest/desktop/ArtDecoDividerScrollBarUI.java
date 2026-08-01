package thechosenquest.desktop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Character creation's divider and scrollbar are one stable component.
 *
 * When scrolling is unnecessary it paints a centered ornamental gem. When content
 * overflows, the same gem becomes the moving scrollbar thumb. The component remains
 * the same width in both states, preventing the selection and hero columns from
 * shifting as window height changes.
 */
final class ArtDecoDividerScrollBarUI extends BasicScrollBarUI {
    private HeroVisualTheme theme =
        HeroVisualTheme.forBuild("Human", "Fighter");

    void setHeroVisualTheme(HeroVisualTheme theme) {
        if (theme == null) return;
        this.theme = theme;
        if (scrollbar != null) scrollbar.repaint();
    }

    static ArtDecoDividerScrollBarUI install(JScrollBar bar) {
        ArtDecoDividerScrollBarUI ui = new ArtDecoDividerScrollBarUI();
        bar.setUI(ui);
        bar.setOpaque(false);
        bar.setBackground(UiTheme.BACKGROUND);
        bar.setPreferredSize(new Dimension(28, 0));
        bar.setUnitIncrement(16);
        bar.setBlockIncrement(96);
        return ui;
    }

    @Override
    protected void configureScrollBarColors() {
        trackColor = new Color(0, 0, 0, 0);
        thumbColor = new Color(0, 0, 0, 0);
        thumbHighlightColor = new Color(0, 0, 0, 0);
        thumbDarkShadowColor = new Color(0, 0, 0, 0);
        thumbLightShadowColor = new Color(0, 0, 0, 0);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private JButton zeroButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        return button;
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        // The visible gem is smaller, but this provides a forgiving drag target.
        return new Dimension(26, 48);
    }

    @Override
    protected Dimension getMaximumThumbSize() {
        return new Dimension(26, 48);
    }

    @Override
    protected void paintTrack(Graphics graphics, JComponent component,
                              Rectangle bounds) {
        Graphics2D g = prepare(graphics);
        int centerX = bounds.x + bounds.width / 2;
        int top = bounds.y + 6;
        int bottom = bounds.y + bounds.height - 6;
        paintRail(g, centerX, top, bottom);
        if (!scrollingRequired()) {
            paintGem(g, centerX, bounds.y + bounds.height / 2, false, false);
        }
        g.dispose();
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent component,
                              Rectangle bounds) {
        if (!scrollingRequired() || bounds.isEmpty()) return;
        Graphics2D g = prepare(graphics);
        paintGem(g, bounds.x + bounds.width / 2,
            bounds.y + bounds.height / 2, isDragging, isThumbRollover());
        g.dispose();
    }

    private void paintRail(Graphics2D g, int x, int top, int bottom) {
        g.setColor(new Color(6, 5, 4, 210));
        g.setStroke(new BasicStroke(5f));
        g.drawLine(x, top, x, bottom);

        Color metal = theme.metalAccent();
        g.setColor(new Color(metal.getRed(), metal.getGreen(), metal.getBlue(), 205));
        g.setStroke(new BasicStroke(1.4f));
        g.drawLine(x, top, x, bottom);
        g.setColor(new Color(UiTheme.GOLD_LIGHT.getRed(),
            UiTheme.GOLD_LIGHT.getGreen(), UiTheme.GOLD_LIGHT.getBlue(), 68));
        g.drawLine(x + 2, top + 8, x + 2, Math.max(top + 8, bottom - 8));

        paintEndCap(g, x, top, 1);
        paintEndCap(g, x, bottom, -1);
    }

    private void paintEndCap(Graphics2D g, int x, int y, int direction) {
        Polygon cap = new Polygon(
            new int[] {x, x + 5, x, x - 5},
            new int[] {y, y + direction * 8, y + direction * 16,
                y + direction * 8}, 4);
        g.setColor(new Color(15, 11, 8));
        g.fillPolygon(cap);
        g.setColor(theme.metalAccent());
        g.drawPolygon(cap);
    }

    private void paintGem(Graphics2D g, int centerX, int centerY,
                          boolean dragging, boolean rollover) {
        int outerX = rollover || dragging ? 10 : 9;
        int outerY = rollover || dragging ? 23 : 21;
        Polygon mount = diamond(centerX, centerY, outerX, outerY);
        g.setColor(new Color(8, 6, 5, 235));
        g.fillPolygon(mount);
        g.setStroke(new BasicStroke(dragging ? 2.2f : 1.6f));
        g.setColor(rollover || dragging ? UiTheme.GOLD_LIGHT
            : theme.metalAccent());
        g.drawPolygon(mount);

        Polygon inset = diamond(centerX, centerY, 5, 13);
        Color accent = theme.classAccent();
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
            dragging ? 255 : 220));
        g.fillPolygon(inset);
        g.setColor(new Color(UiTheme.GOLD_LIGHT.getRed(),
            UiTheme.GOLD_LIGHT.getGreen(), UiTheme.GOLD_LIGHT.getBlue(),
            rollover || dragging ? 220 : 145));
        g.drawPolygon(inset);

        g.setColor(new Color(255, 255, 255, rollover ? 110 : 65));
        g.drawLine(centerX - 1, centerY - 8, centerX + 2, centerY - 2);
    }

    private static Polygon diamond(int centerX, int centerY,
                                   int radiusX, int radiusY) {
        return new Polygon(
            new int[] {centerX, centerX + radiusX, centerX, centerX - radiusX},
            new int[] {centerY - radiusY, centerY, centerY + radiusY, centerY}, 4);
    }

    private boolean scrollingRequired() {
        if (scrollbar == null) return false;
        return scrollbar.getModel().getMaximum() -
            scrollbar.getModel().getMinimum() >
            scrollbar.getModel().getExtent();
    }

    private static Graphics2D prepare(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE);
        return g;
    }
}
