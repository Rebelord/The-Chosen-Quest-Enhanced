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
 * Scalable portrait ornament. Race supplies the frame material while class
 * supplies its accent gems and lower emblem. Combining those two
 * layers gives every race/class build a recognizable frame without loading 16
 * more raster assets.
 */
final class FantasyPortraitBorder extends AbstractBorder {
    private static final long serialVersionUID = 1L;
    private final String race;
    private final String heroClass;
    private final boolean compact;

    FantasyPortraitBorder(String race, String heroClass) {
        this(race, heroClass, false);
    }

    FantasyPortraitBorder(String race, String heroClass, boolean compact) {
        this.race = race == null ? "Human" : race;
        this.heroClass = heroClass == null ? "Fighter" : heroClass;
        this.compact = compact;
    }

    @Override
    public Insets getBorderInsets(Component component) {
        int inset = compact ? 7 : 12;
        return new Insets(inset, inset, inset, inset);
    }

    @Override
    public Insets getBorderInsets(Component component, Insets insets) {
        insets.top = insets.left = insets.bottom = insets.right = compact ? 7 : 12;
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    /** Opening used by AssetImagePanel to mask square artwork beneath the moulding. */
    int viewportInset() { return 5; }

    int viewportArc() { return compact ? 11 : 15; }

    /**
     * The authored hero overlay reserves a larger architectural moulding than
     * compact choice portraits. Keeping these proportions with the frame means
     * the character artwork is rendered into—not cropped behind—the opening.
     */
    Insets viewportInsets(int width, int height) {
        if (compact) return new Insets(5, 5, 5, 5);
        int horizontal = Math.max(18, Math.round(width * .135f));
        int vertical = Math.max(22, Math.round(height * .128f));
        return new Insets(vertical, horizontal, vertical, horizontal);
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y,
                            int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        Color material = materialColor();
        Color highlight = highlightColor();
        Color accent = accentForClass(heroClass);
        int right = x + width - 1;
        int bottom = y + height - 1;

        if (!compact && UiBorderAssets.paintHeroFrame(g, x, y, width, height, 1f,
                accent)) {
            g.dispose();
            return;
        }

        if (!compact) {
            paintArtDecoSilhouette(g, x, y, right, bottom, material, highlight,
                accent);
        }

        // Deep outer moulding, metallic face, and inset engraved line.
        g.setStroke(new BasicStroke(compact ? 6f : 10f));
        g.setColor(new Color(12, 8, 6, 220));
        g.drawRoundRect(x + 5, y + 5, Math.max(1, width - 11),
            Math.max(1, height - 11), 14, 14);
        g.setStroke(new BasicStroke(compact ? 3.5f : 6f));
        g.setColor(material);
        g.drawRoundRect(x + 5, y + 5, Math.max(1, width - 11),
            Math.max(1, height - 11), 12, 12);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(highlight);
        g.drawRoundRect(x + 8, y + 8, Math.max(1, width - 17),
            Math.max(1, height - 17), 9, 9);
        g.setColor(new Color(18, 12, 9, 210));
        g.drawRoundRect(x + 12, y + 12, Math.max(1, width - 25),
            Math.max(1, height - 25), 6, 6);

        paintCornerBrackets(g, x, y, right, bottom, material, highlight);
        paintClassGems(g, x, y, right, bottom, accent, highlight);
        // Keep the complete class badge inside the component bounds. The old
        // baseline sat directly on the bottom edge and clipped the Hunter bow.
        paintClassEmblem(g, x + width / 2, bottom - (compact ? 11 : 14), accent);
        g.dispose();
    }

    private void paintArtDecoSilhouette(Graphics2D g, int x, int y,
                                        int right, int bottom, Color material,
                                        Color highlight, Color accent) {
        int center = (x + right) / 2;
        Path2D stepped = new Path2D.Double();
        stepped.moveTo(center - 34, y + 3);
        stepped.lineTo(center - 17, y + 9);
        stepped.lineTo(x + 22, y + 9);
        stepped.lineTo(x + 10, y + 21);
        stepped.lineTo(x + 10, bottom - 24);
        stepped.lineTo(x + 20, bottom - 14);
        stepped.lineTo(center - 22, bottom - 14);
        stepped.lineTo(center, bottom - 2);
        stepped.lineTo(center + 22, bottom - 14);
        stepped.lineTo(right - 20, bottom - 14);
        stepped.lineTo(right - 10, bottom - 24);
        stepped.lineTo(right - 10, y + 21);
        stepped.lineTo(right - 22, y + 9);
        stepped.lineTo(center + 17, y + 9);
        stepped.lineTo(center + 34, y + 3);

        g.setStroke(new BasicStroke(11f, BasicStroke.CAP_SQUARE,
            BasicStroke.JOIN_MITER));
        g.setColor(new Color(8, 6, 5, 225));
        g.draw(stepped);
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_SQUARE,
            BasicStroke.JOIN_MITER));
        g.setColor(material);
        g.draw(stepped);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(highlight);
        g.draw(stepped);

        // Crown fan echoes the approved frame without adding a raster overlay.
        Path2D crown = new Path2D.Double();
        crown.moveTo(center - 28, y + 10);
        crown.lineTo(center - 15, y + 4);
        crown.lineTo(center, y);
        crown.lineTo(center + 15, y + 4);
        crown.lineTo(center + 28, y + 10);
        crown.closePath();
        g.setColor(new Color(10, 8, 6, 235));
        g.fill(crown);
        g.setColor(material);
        g.draw(crown);
        for (int offset = -18; offset <= 18; offset += 9) {
            g.setColor(offset == 0 ? accent : highlight);
            g.drawLine(center, y + 1, center + offset, y + 9);
        }

        // Small side steps break the rectangular silhouette at eye level.
        int middle = (y + bottom) / 2;
        g.setColor(material);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(x + 4, middle - 24, x + 10, middle - 18);
        g.drawLine(x + 10, middle - 18, x + 4, middle - 12);
        g.drawLine(right - 4, middle - 24, right - 10, middle - 18);
        g.drawLine(right - 10, middle - 18, right - 4, middle - 12);
    }

    private void paintCornerBrackets(Graphics2D g, int x, int y, int right, int bottom,
                                     Color material, Color highlight) {
        int inset = 5;
        int length = compact ? 19 : 31;
        int[][] corners = {
            {x + inset, y + inset, 1, 1}, {right - inset, y + inset, -1, 1},
            {x + inset, bottom - inset, 1, -1}, {right - inset, bottom - inset, -1, -1}
        };
        for (int[] corner : corners) {
            int cx = corner[0];
            int cy = corner[1];
            int dx = corner[2];
            int dy = corner[3];
            Path2D bracket = new Path2D.Double();
            bracket.moveTo(cx, cy + dy * length);
            bracket.lineTo(cx, cy);
            bracket.lineTo(cx + dx * length, cy);
            g.setStroke(new BasicStroke(compact ? 4.5f : 8f,
                BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g.setColor(material);
            g.draw(bracket);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(highlight);
            g.draw(bracket);
        }
    }

    private void paintClassGems(Graphics2D g, int x, int y, int right, int bottom,
                                Color accent, Color highlight) {
        int offset = compact ? 8 : 11;
        int outerRadius = compact ? 4 : 6;
        int innerRadius = compact ? 2 : 4;
        int[][] points = {
            {x + offset, y + offset}, {right - offset, y + offset},
            {x + offset, bottom - offset}, {right - offset, bottom - offset}
        };
        for (int[] point : points) {
            g.setColor(new Color(12, 8, 7));
            g.fill(diamond(point[0], point[1], outerRadius));
            g.setColor(accent);
            Path2D jewel = diamond(point[0], point[1], innerRadius);
            g.fill(jewel);
            g.setColor(highlight);
            g.draw(jewel);
        }
    }

    private Path2D diamond(int centerX, int centerY, int radius) {
        Path2D shape = new Path2D.Double();
        shape.moveTo(centerX, centerY - radius);
        shape.lineTo(centerX + radius, centerY);
        shape.lineTo(centerX, centerY + radius);
        shape.lineTo(centerX - radius, centerY);
        shape.closePath();
        return shape;
    }

    private void paintClassEmblem(Graphics2D g, int centerX, int centerY, Color accent) {
        if (!compact) {
            Path2D mount = diamond(centerX, centerY, 9);
            g.setColor(new Color(10, 7, 6, 235));
            g.fill(mount);
            g.setColor(materialColor());
            g.draw(mount);
            Path2D jewel = diamond(centerX, centerY, 5);
            g.setColor(accent);
            g.fill(jewel);
            g.setColor(highlightColor());
            g.draw(jewel);
            return;
        }
        int plateWidth = 22;
        int plateHeight = 12;
        g.setColor(new Color(12, 8, 7, 225));
        g.fillOval(centerX - plateWidth / 2, centerY - plateHeight / 2,
            plateWidth, plateHeight);
        g.setColor(accent);
        g.setStroke(new BasicStroke(compact ? 1.6f : 2f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int radius = compact ? 4 : 5;
        if ("Mage".equals(heroClass)) {
            Path2D rune = new Path2D.Double();
            rune.moveTo(centerX, centerY - radius);
            rune.lineTo(centerX + radius, centerY);
            rune.lineTo(centerX, centerY + radius);
            rune.lineTo(centerX - radius, centerY);
            rune.closePath();
            g.draw(rune);
            g.fillOval(centerX - 1, centerY - 1, 3, 3);
        } else if ("Rogue".equals(heroClass)) {
            g.drawArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 70, 230);
            g.drawLine(centerX + 2, centerY - radius + 1,
                centerX - 2, centerY + radius);
        } else if ("Hunter".equals(heroClass)) {
            int bowRadius = compact ? 5 : 6;
            g.drawArc(centerX - bowRadius, centerY - bowRadius,
                bowRadius * 2, bowRadius * 2, -75, 150);
            g.drawLine(centerX - 2, centerY - bowRadius + 1,
                centerX + bowRadius - 1, centerY + bowRadius - 1);
        } else {
            g.drawLine(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);
            g.drawLine(centerX + radius, centerY - radius,
                centerX - radius, centerY + radius);
        }
    }

    private Color materialColor() {
        if ("Dwarf".equals(race)) return new Color(108, 104, 99);
        if ("Elf".equals(race)) return new Color(157, 173, 151);
        if ("Halfling".equals(race)) return new Color(139, 96, 52);
        return new Color(158, 117, 36);
    }

    private Color highlightColor() {
        if ("Dwarf".equals(race)) return new Color(213, 190, 137);
        if ("Elf".equals(race)) return new Color(220, 238, 210);
        if ("Halfling".equals(race)) return new Color(224, 177, 103);
        return new Color(244, 210, 126);
    }

    static Color accentForClass(String heroClass) {
        if ("Mage".equals(heroClass)) return new Color(57, 132, 188);
        if ("Rogue".equals(heroClass)) return new Color(139, 78, 184);
        if ("Hunter".equals(heroClass)) return new Color(74, 148, 84);
        return new Color(180, 58, 52);
    }
}
