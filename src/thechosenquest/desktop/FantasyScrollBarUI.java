package thechosenquest.desktop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Slim, code-native scrollbar that does not crowd dense game panels. */
final class FantasyScrollBarUI extends BasicScrollBarUI {
    private static final Color TRACK = new Color(15, 17, 23);
    private final Color accent;

    FantasyScrollBarUI(Color accent) {
        this.accent = accent;
    }

    @Override
    protected void configureScrollBarColors() {
        trackColor = TRACK;
        thumbColor = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 165);
        thumbHighlightColor = accent;
        thumbDarkShadowColor = UiTheme.SURFACE_DEEP;
        thumbLightShadowColor = accent;
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
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
        graphics.setColor(TRACK);
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        if (!component.isEnabled() || bounds.isEmpty()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        int inset = 2;
        int width = Math.max(3, bounds.width - inset * 2);
        int height = Math.max(10, bounds.height - inset * 2);
        g.setColor(thumbColor);
        g.fillRoundRect(bounds.x + inset, bounds.y + inset, width, height, width, width);
        g.setColor(accent);
        g.drawRoundRect(bounds.x + inset, bounds.y + inset, width - 1, height - 1,
            width, width);
        g.dispose();
    }

    static void install(JScrollBar bar, Color accent) {
        bar.setUI(new FantasyScrollBarUI(accent));
        bar.setPreferredSize(new Dimension(10, 0));
        bar.setUnitIncrement(18);
        bar.setOpaque(false);
    }
}
