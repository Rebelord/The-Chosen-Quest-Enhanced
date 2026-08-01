package thechosenquest.desktop;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Paints replaceable, transparent character-creation decoration beneath ordinary
 * Swing content. No wording, spacing, controls, or portrait pixels live in this
 * layer, so visual themes can change without changing the screen structure.
 */
final class CreationThemePanel extends JPanel {
    enum Role {
        SHELL,
        DOSSIER,
        HERO_PROFILE
    }

    private static final long serialVersionUID = 1L;
    private final Role role;
    private HeroVisualTheme theme = HeroVisualTheme.forBuild("Human", "Fighter");

    CreationThemePanel(Role role) {
        this.role = role;
        setOpaque(false);
    }

    void setHeroVisualTheme(HeroVisualTheme theme) {
        if (theme == null) return;
        this.theme = theme;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        Color base = role == Role.SHELL ? UiTheme.BACKGROUND
            : role == Role.HERO_PROFILE ? UiTheme.SURFACE : UiTheme.SURFACE_DEEP;
        Color top = blend(base, theme.raceAccent(),
            role == Role.SHELL ? .035 : .055);
        Color bottom = blend(base, Color.BLACK,
            role == Role.SHELL ? .12 : .18);
        g.setPaint(new GradientPaint(0, 0, top, 0,
            Math.max(1, getHeight()), bottom));
        g.fillRect(0, 0, getWidth(), getHeight());
        UiMaterials.paintLacquer(g,
            new java.awt.Rectangle(0, 0, getWidth(), getHeight()),
            role == Role.SHELL ? .32f : .44f);
        paintLacquerTexture(g, base);

        // Class enamel is the readable, higher-intensity identity layer.
        g.setColor(theme.classOverlay(role == Role.SHELL ? 12 : 22));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Race heraldry stays quiet and transparent so it never fights the copy.
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(theme.raceOverlay(role == Role.SHELL ? 18 : 25));
        g.setStroke(new BasicStroke(role == Role.SHELL ? 2f : 1.5f));
        paintRaceMotif(g);

        if (role != Role.SHELL) paintDecoCorners(g);
        g.dispose();
        super.paintComponent(graphics);
    }

    /**
     * Deterministic low-contrast grain adds painterly material depth without
     * embedding a fixed raster background or causing animation shimmer.
     */
    private void paintLacquerTexture(Graphics2D g, Color base) {
        int alpha = role == Role.SHELL ? 11 : 15;
        g.setStroke(new BasicStroke(1f));
        for (int y = 9; y < getHeight(); y += 19) {
            int offset = (y * 7) % 43;
            g.setColor(new Color(238, 218, 166, alpha));
            for (int x = offset; x < getWidth(); x += 71) {
                int length = 9 + ((x + y) % 17);
                g.drawLine(x, y, Math.min(getWidth(), x + length), y);
            }
        }
        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(),
            role == Role.SHELL ? 32 : 46));
        for (int x = 13; x < getWidth(); x += 37) {
            int y = (x * 11) % Math.max(1, getHeight());
            g.fillOval(x, y, 2, 1);
        }
    }

    private void paintRaceMotif(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();
        int cx = role == Role.HERO_PROFILE ? w / 2 : Math.max(42, w - 74);
        int cy = role == Role.HERO_PROFILE ? Math.max(84, h / 3) : 66;
        int radius = role == Role.SHELL ? 92 : 64;

        switch (theme.raceMotif()) {
            case MOUNTAIN_RUNES:
                for (int i = 0; i < 3; i++) {
                    int inset = i * 18;
                    g.drawLine(cx - radius + inset, cy + radius / 2,
                        cx, cy - radius + inset);
                    g.drawLine(cx, cy - radius + inset,
                        cx + radius - inset, cy + radius / 2);
                }
                g.drawLine(cx - radius / 2, cy + radius / 2,
                    cx + radius / 2, cy + radius / 2);
                break;
            case LEAF_CRESCENT:
                g.drawArc(cx - radius, cy - radius, radius * 2, radius * 2,
                    70, 220);
                g.drawArc(cx - radius / 2, cy - radius, radius, radius * 2,
                    250, 220);
                for (int i = -2; i <= 2; i++) {
                    g.drawArc(cx - 10 + i * 10, cy - 30 + i * 12,
                        32, 18, 20, 160);
                }
                break;
            case WHEAT_ARCH:
                g.drawArc(cx - radius, cy - radius / 2, radius * 2, radius * 2,
                    15, 150);
                g.drawLine(cx, cy - radius / 2, cx, cy + radius);
                for (int i = 0; i < 5; i++) {
                    int y = cy - 34 + i * 17;
                    g.drawArc(cx - 25, y, 24, 13, 200, 150);
                    g.drawArc(cx + 1, y - 7, 24, 13, 25, 150);
                }
                break;
            case CROWN_SUNBURST:
            default:
                for (int i = 0; i < 9; i++) {
                    double angle = Math.toRadians(200 + i * 17);
                    g.drawLine(cx, cy,
                        cx + (int) (Math.cos(angle) * radius),
                        cy + (int) (Math.sin(angle) * radius));
                }
                Polygon crown = new Polygon(
                    new int[] {cx - 34, cx - 26, cx - 9, cx, cx + 12, cx + 30,
                        cx + 34, cx - 34},
                    new int[] {cy + 25, cy - 8, cy + 10, cy - 20, cy + 10,
                        cy - 8, cy + 25, cy + 25}, 8);
                g.drawPolygon(crown);
                break;
        }
    }

    private void paintDecoCorners(Graphics2D g) {
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        int step = role == Role.HERO_PROFILE ? 22 : 16;
        g.setColor(new Color(theme.metalAccent().getRed(),
            theme.metalAccent().getGreen(), theme.metalAccent().getBlue(), 110));
        g.drawLine(0, step, step, 0);
        g.drawLine(w - step, 0, w, step);
        g.drawLine(0, h - step, step, h);
        g.drawLine(w - step, h, w, h - step);
        g.drawRect(step / 2, step / 2, Math.max(1, w - step),
            Math.max(1, h - step));
    }

    private static Color blend(Color base, Color accent, double amount) {
        double weight = Math.max(0d, Math.min(1d, amount));
        return new Color(
            (int) Math.round(base.getRed() * (1d - weight) +
                accent.getRed() * weight),
            (int) Math.round(base.getGreen() * (1d - weight) +
                accent.getGreen() * weight),
            (int) Math.round(base.getBlue() * (1d - weight) +
                accent.getBlue() * weight));
    }
}
