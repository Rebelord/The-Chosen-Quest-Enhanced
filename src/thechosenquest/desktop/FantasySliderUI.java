package thechosenquest.desktop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/** Code-native slider skin matching the shared Figma audio-control component. */
final class FantasySliderUI extends BasicSliderUI {
    private static final Color TRACK = new Color(56, 43, 33);

    FantasySliderUI(JSlider slider) {
        super(slider);
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(14, 14);
    }

    @Override
    public void paintTrack(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        int height = 8;
        int y = trackRect.y + (trackRect.height - height) / 2;
        g.setColor(TRACK);
        g.fillRoundRect(trackRect.x, y, trackRect.width, height, 8, 8);
        int fillWidth = Math.max(0, thumbRect.x + thumbRect.width / 2 - trackRect.x);
        g.setColor(UiTheme.GOLD);
        g.fillRoundRect(trackRect.x, y, fillWidth, height, 8, 8);
        g.dispose();
    }

    @Override
    public void paintThumb(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle bounds = thumbRect;
        if (slider.hasFocus()) {
            g.setColor(new Color(UiTheme.GOLD.getRed(), UiTheme.GOLD.getGreen(),
                UiTheme.GOLD.getBlue(), 90));
            g.fillOval(bounds.x - 3, bounds.y - 3, bounds.width + 6, bounds.height + 6);
        }
        g.setColor(UiTheme.GOLD);
        g.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(UiTheme.SURFACE_DEEP);
        g.drawOval(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
        g.dispose();
    }

    @Override
    public void paintFocus(Graphics graphics) {
        // The focused thumb supplies the visible focus indication.
    }
}
