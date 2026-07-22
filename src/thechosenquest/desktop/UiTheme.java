package thechosenquest.desktop;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.InputStream;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

final class UiTheme {
    static final String BUTTON_ACTIVE = "thechosenquest.button.active";

    enum ButtonStyle {
        PRIMARY,
        SECONDARY,
        DANGER,
        ICON,
        KEY,
        TITLE_PRIMARY,
        TITLE_SECONDARY
    }

    private static final Font DISPLAY_FONT = loadFont(
        "/assets/fonts/CormorantGaramond.ttf", Font.SERIF);
    private static final Font TITLE_FONT = loadFont(
        "/assets/fonts/Cinzel.ttf", Font.SERIF);
    static final int SHELL_WIDTH = 1440;
    static final int SHELL_HEIGHT = 900;
    static final int HEADER_HEIGHT = 64;
    static final int BODY_HEIGHT = 836;
    static final int HERO_RAIL_WIDTH = 280;
    static final int CENTER_WIDTH = 840;
    static final int MAP_RAIL_WIDTH = 320;
    static final int COMBAT_STAGE_HEIGHT = 350;
    static final int ENEMY_INFO_HEIGHT = 190;
    static final int COMMAND_CONSOLE_HEIGHT = 280;

    static final Color BACKGROUND = new Color(20, 13, 9);
    static final Color SURFACE = new Color(36, 26, 20);
    static final Color SURFACE_DEEP = new Color(26, 19, 15);
    static final Color BORDER = new Color(90, 74, 55);
    static final Color GOLD = new Color(212, 175, 55);
    static final Color GOLD_LIGHT = new Color(241, 229, 172);
    static final Color TEXT = new Color(255, 249, 229);
    static final Color MUTED = new Color(166, 153, 135);
    static final Color GREEN = new Color(45, 90, 39);
    static final Color HEALTH = new Color(76, 175, 80);
    static final Color BLUE = new Color(50, 104, 145);
    static final Color PURPLE = new Color(122, 92, 158);
    static final Color DANGER = new Color(178, 58, 48);
    static final Color ELITE = new Color(191, 62, 45);
    static final Color BOSS = new Color(191, 42, 42);
    static final Color QUALITY_COMMON = new Color(196, 187, 174);
    static final Color QUALITY_UNCOMMON = new Color(111, 206, 120);
    static final Color QUALITY_RARE = new Color(105, 156, 232);
    static final Color QUALITY_RELIC = new Color(199, 149, 255);
    static final Color RELIC_SURFACE = new Color(27, 18, 35);
    // Shared map-state colors keep the renderer and Figma handoff vocabulary aligned.
    static final Color MAP_FOG = new Color(13, 15, 20);
    static final Color MAP_SCOUTED_OVERLAY = new Color(12, 14, 20, 105);
    static final Color MAP_WARNING = new Color(224, 79, 60);

    private UiTheme() {
    }

    static Color qualityColor(String quality) {
        if ("RELIC".equals(quality)) return QUALITY_RELIC;
        if ("RARE".equals(quality)) return QUALITY_RARE;
        if ("UNCOMMON".equals(quality)) return QUALITY_UNCOMMON;
        return QUALITY_COMMON;
    }

    static Font display(int size) {
        return DISPLAY_FONT.deriveFont(Font.PLAIN, (float) size);
    }

    static Font displayBold(int size) {
        return DISPLAY_FONT.deriveFont(Font.BOLD, (float) size);
    }

    static Font title(int style, int size) {
        return TITLE_FONT.deriveFont(style, (float) size);
    }

    static Font body(int style, int size) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(body(Font.BOLD, primary ? 15 : 13));
        applyButtonStyle(button, primary ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
            primary ? 13 : 10, primary ? 28 : 20);
        return button;
    }

    static void applyButtonStyle(JButton button, ButtonStyle style, int verticalPadding,
            int horizontalPadding) {
        button.setUI(new FantasyButtonUI(style));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(verticalPadding, horizontalPadding,
            verticalPadding, horizontalPadding));
        button.setForeground(style == ButtonStyle.PRIMARY ? new Color(26, 19, 15) : TEXT);
    }

    static void setButtonActive(JButton button, boolean active) {
        button.putClientProperty(BUTTON_ACTIVE, Boolean.valueOf(active));
        button.repaint();
    }

    static void panelBorder(JComponent component, int padding) {
        component.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
    }

    private static Font loadFont(String resource, String fallback) {
        InputStream stream = UiTheme.class.getResourceAsStream(resource);
        if (stream == null) return new Font(fallback, Font.PLAIN, 12);
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception error) {
            return new Font(fallback, Font.PLAIN, 12);
        } finally {
            try { stream.close(); } catch (Exception ignored) { }
        }
    }

    /** Platform-independent interaction states matching the Figma button variants. */
    private static final class FantasyButtonUI extends BasicButtonUI {
        private final ButtonStyle style;

        FantasyButtonUI(ButtonStyle style) {
            this.style = style;
        }

        @Override
        public void paint(Graphics graphics, JComponent component) {
            JButton button = (JButton) component;
            ButtonModel model = button.getModel();
            boolean active = model.isSelected()
                || Boolean.TRUE.equals(button.getClientProperty(BUTTON_ACTIVE));
            boolean pressed = model.isArmed() && model.isPressed();
            boolean hover = model.isRollover();
            boolean enabled = model.isEnabled();

            Color fill = fill(enabled, hover, pressed, active);
            Color stroke = stroke(enabled, hover, pressed, active);
            Color copy = copy(enabled, active);
            button.setForeground(copy);

            if (style == ButtonStyle.TITLE_PRIMARY ||
                    style == ButtonStyle.TITLE_SECONDARY) {
                paintTitleButton(graphics, button, enabled, hover, pressed, active);
                Graphics2D content = (Graphics2D) graphics.create();
                // Center copy within the raised face, excluding its lower
                // shadow. A press then returns the copy to the depressed plane.
                content.translate(0, pressed ? 0 : -3);
                super.paint(content, component);
                content.dispose();
                return;
            }
            if (style == ButtonStyle.KEY) {
                paintKeyButton(graphics, button, enabled, hover, pressed, active);
                Graphics2D content = (Graphics2D) graphics.create();
                if (pressed) content.translate(0, 2);
                super.paint(content, component);
                content.dispose();
                return;
            }

            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int width = Math.max(0, button.getWidth() - 1);
            int height = Math.max(0, button.getHeight() - 1);
            g.setColor(fill);
            int radius = 4;
            g.fillRoundRect(0, 0, width, height, radius, radius);
            g.setColor(stroke);
            g.drawRoundRect(0, 0, width, height, radius, radius);
            if ((active || button.isFocusOwner()) && enabled) {
                g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 105));
                g.drawRoundRect(2, 2, Math.max(0, width - 4), Math.max(0, height - 4), 3, 3);
            }
            g.dispose();
            super.paint(graphics, component);
        }

        /** Compact raised keycap that keeps its glyph centered at small sizes. */
        private void paintKeyButton(Graphics graphics, JButton button, boolean enabled,
                                    boolean hover, boolean pressed, boolean active) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int width = Math.max(1, button.getWidth() - 1);
            int height = Math.max(1, button.getHeight() - 1);
            int offset = pressed ? 3 : 0;
            int faceBottom = Math.max(1, height - 5);

            g.setColor(new Color(0, 0, 0, enabled ? 165 : 85));
            g.fillRoundRect(2, 5, Math.max(1, width - 4), Math.max(1, height - 5), 8, 8);

            Color rim = enabled ? (hover ? GOLD_LIGHT : GOLD) : new Color(82, 71, 57);
            Color face = !enabled ? new Color(42, 36, 31) :
                (pressed ? new Color(38, 25, 18) :
                    (hover ? new Color(78, 53, 35) : new Color(57, 39, 27)));
            g.setColor(rim);
            g.fillRoundRect(0, offset, width, faceBottom, 8, 8);
            g.setColor(face);
            g.fillRoundRect(2, offset + 2, Math.max(1, width - 4),
                Math.max(1, faceBottom - 4), 6, 6);

            if (enabled && !pressed) {
                g.setColor(new Color(255, 255, 255, 42));
                g.drawLine(7, offset + 4, Math.max(7, width - 7), offset + 4);
            }
            if ((active || button.isFocusOwner()) && enabled) {
                g.setColor(new Color(GOLD_LIGHT.getRed(), GOLD_LIGHT.getGreen(),
                    GOLD_LIGHT.getBlue(), 150));
                g.drawRoundRect(3, offset + 3, Math.max(0, width - 6),
                    Math.max(0, faceBottom - 6), 5, 5);
            }
            g.dispose();
        }

        private void paintTitleButton(Graphics graphics, JButton button, boolean enabled,
                boolean hover, boolean pressed, boolean active) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int width = Math.max(1, button.getWidth() - 1);
            int height = Math.max(1, button.getHeight() - 1);
            int pressOffset = pressed ? 3 : 0;
            int shadowDepth = pressed ? 1 : 6;

            g.setColor(new Color(0, 0, 0, enabled ? 145 : 85));
            g.fillRoundRect(3, shadowDepth + 2, Math.max(1, width - 6),
                Math.max(1, height - shadowDepth - 1), 14, 14);

            boolean primary = style == ButtonStyle.TITLE_PRIMARY;
            Color outer = enabled
                ? (primary ? new Color(198, 176, 141) : (hover ? GOLD_LIGHT : GOLD))
                : new Color(91, 78, 59);
            g.setColor(primary ? new Color(77, 10, 35) : new Color(59, 38, 20));
            g.fillRoundRect(0, pressOffset, width, Math.max(1, height - 6), 14, 14);
            g.setColor(outer);
            g.fillRoundRect(2, pressOffset + 2, Math.max(1, width - 4),
                Math.max(1, height - 10), 12, 12);

            Color bevel;
            Color face;
            if (!enabled) {
                bevel = new Color(53, 45, 40);
                face = new Color(45, 39, 35);
            } else if (primary) {
                bevel = pressed ? new Color(91, 7, 39) : new Color(112, 8, 44);
                face = hover ? new Color(224, 43, 91) : new Color(189, 24, 68);
            } else {
                bevel = pressed ? new Color(37, 25, 18) : new Color(50, 34, 24);
                face = hover ? new Color(75, 51, 34) : new Color(58, 39, 27);
            }
            g.setColor(bevel);
            g.fillRoundRect(6, pressOffset + 6, Math.max(1, width - 12),
                Math.max(1, height - 18), 9, 9);
            g.setColor(face);
            g.fillRoundRect(10, pressOffset + 7, Math.max(1, width - 20),
                Math.max(1, height - 24), 7, 7);

            if (primary && enabled) {
                int top = pressOffset + 8;
                int faceWidth = Math.max(1, width - 22);
                g.setColor(new Color(255, 85, 132, pressed ? 45 : 115));
                g.fillRoundRect(11, top, faceWidth, Math.max(4, (height - 25) / 3), 7, 7);
                g.setColor(new Color(84, 5, 34, 120));
                g.fillRoundRect(11, Math.max(top, height - 25 + pressOffset), faceWidth,
                    11, 6, 6);
            }

            if (enabled && !pressed) {
                g.setColor(new Color(255, 255, 255, primary ? 58 : 30));
                g.drawRoundRect(11, pressOffset + 8, Math.max(0, width - 22),
                    Math.max(0, height - 26), 7, 7);
                g.drawLine(17, pressOffset + 10, Math.max(17, width - 17), pressOffset + 10);
            }
            if ((active || button.isFocusOwner()) && enabled) {
                g.setColor(new Color(GOLD_LIGHT.getRed(), GOLD_LIGHT.getGreen(),
                    GOLD_LIGHT.getBlue(), 170));
                g.drawRoundRect(4, pressOffset + 4, Math.max(0, width - 8),
                    Math.max(0, height - 14), 10, 10);
            }
            g.dispose();
        }

        @Override
        protected void paintText(Graphics graphics, AbstractButton button,
                Rectangle textRect, String text) {
            if (style != ButtonStyle.TITLE_PRIMARY &&
                    style != ButtonStyle.TITLE_SECONDARY) {
                super.paintText(graphics, button, textRect, text);
                return;
            }
            Color original = button.getForeground();
            Graphics2D shadow = (Graphics2D) graphics.create();
            shadow.translate(1, 2);
            button.setForeground(new Color(45, 10, 18, 190));
            super.paintText(shadow, button, textRect, text);
            shadow.dispose();
            button.setForeground(original);
            super.paintText(graphics, button, textRect, text);
        }

        private Color fill(boolean enabled, boolean hover, boolean pressed, boolean active) {
            if (!enabled) return new Color(31, 26, 22);
            if (style == ButtonStyle.DANGER) {
                if (pressed) return new Color(125, 24, 29);
                if (hover) return new Color(207, 55, 60);
                if (active) return new Color(193, 43, 48);
                return new Color(170, 37, 42);
            }
            if (style == ButtonStyle.PRIMARY || style == ButtonStyle.TITLE_PRIMARY ||
                    style == ButtonStyle.TITLE_SECONDARY) {
                if (pressed) return new Color(211, 181, 107);
                if (hover) return new Color(255, 241, 202);
                if (active) return new Color(226, 197, 121);
                return new Color(247, 230, 184);
            }
            if (pressed) return new Color(23, 16, 12);
            if (hover) return new Color(58, 42, 31);
            if (active) return new Color(68, 48, 31);
            return style == ButtonStyle.ICON ? SURFACE_DEEP : SURFACE;
        }

        private Color stroke(boolean enabled, boolean hover, boolean pressed, boolean active) {
            if (!enabled) return new Color(72, 64, 55);
            if (style == ButtonStyle.DANGER) return hover || active
                ? new Color(239, 93, 91) : new Color(202, 62, 62);
            if (pressed) return new Color(174, 133, 35);
            if (hover || active) return GOLD_LIGHT;
            return style == ButtonStyle.PRIMARY || style == ButtonStyle.TITLE_PRIMARY ||
                style == ButtonStyle.TITLE_SECONDARY ? GOLD : BORDER;
        }

        private Color copy(boolean enabled, boolean active) {
            if (!enabled) return new Color(105, 96, 86);
            if (style == ButtonStyle.TITLE_PRIMARY) return enabled ? TEXT : MUTED;
            if (style == ButtonStyle.TITLE_SECONDARY) return enabled ? GOLD_LIGHT : MUTED;
            if (style == ButtonStyle.KEY) return enabled ? GOLD_LIGHT : MUTED;
            if (style == ButtonStyle.PRIMARY) return new Color(26, 19, 15);
            return active ? GOLD_LIGHT : TEXT;
        }
    }
}
