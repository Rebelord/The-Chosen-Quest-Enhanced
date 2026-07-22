package thechosenquest.desktop;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JToggleButton;

/** Accessible, look-and-feel-independent switch used by game preferences. */
final class ToggleSwitch extends JToggleButton {
    private static final long serialVersionUID = 1L;

    ToggleSwitch(String accessibleName) {
        setPreferredSize(new Dimension(42, 22));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setOpaque(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(accessibleName);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        int height = getHeight() - 1;
        int width = getWidth() - 1;
        Color track = isSelected() ? UiTheme.GOLD : new Color(62, 48, 37);
        if (!isEnabled()) track = new Color(48, 41, 35);
        g.setColor(track);
        g.fillRoundRect(0, 0, width, height, height, height);
        g.setColor(isFocusOwner() ? UiTheme.GOLD_LIGHT : UiTheme.BORDER);
        g.drawRoundRect(0, 0, width, height, height, height);
        int knob = height - 6;
        int x = isSelected() ? width - knob - 3 : 3;
        g.setColor(isEnabled() ? new Color(235, 220, 188) : UiTheme.MUTED);
        g.fillOval(x, 3, knob, knob);
        g.dispose();
    }
}
