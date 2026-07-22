package thechosenquest.desktop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Illustrated world map renderer. World knowledge lives in GameEngine.State;
 * this class only translates Unknown, Scouted, and Visited tiles into visuals.
 */
final class GameMapPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int SIZE = 5;
    private static final int GAP = 4;
    private static final int TOP = 24;
    private static final int LEFT = 24;
    private static final int SHEET_CELL = 512;
    private static final int SHEET_INSET = 9;
    private static final Map<String, BufferedImage> IMAGES =
        new HashMap<String, BufferedImage>();
    private final GameEngine engine;
    private final BufferedImage terrainSheet;

    GameMapPanel(GameEngine engine) {
        this.engine = engine;
        terrainSheet = load("/assets/map/terrain-sheet.png");
        setOpaque(false);
        setPreferredSize(new Dimension(288, 300));
        setToolTipText("World map");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int tile = tileSize();
        drawCoordinates(g, tile);

        GameEngine.State state = engine.getState();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int x = LEFT + col * (tile + GAP);
                int y = TOP + row * (tile + GAP);
                drawTile(g, state, row, col, x, y, tile);
            }
        }
        g.dispose();
    }

    private void drawCoordinates(Graphics2D g, int tile) {
        g.setFont(UiTheme.body(Font.BOLD, 10));
        g.setColor(new Color(210, 199, 174));
        for (int i = 0; i < SIZE; i++) {
            drawCentered(g, Integer.toString(i + 1), LEFT + i * (tile + GAP), 3,
                tile, 17);
            drawCentered(g, Character.toString((char) ('A' + i)), 1,
                TOP + i * (tile + GAP), 20, tile);
        }
    }

    private void drawTile(Graphics2D g, GameEngine.State state, int row, int col,
                          int x, int y, int tile) {
        GameEngine.DiscoveryState discovery = engine.discoveryAt(row, col);
        Shape oldClip = g.getClip();
        g.clip(new RoundRectangle2D.Float(x, y, tile, tile, 9, 9));
        if (discovery == GameEngine.DiscoveryState.UNKNOWN) {
            drawFog(g, x, y, tile);
        } else {
            drawTerrain(g, state.tiles[row][col], x, y, tile);
            if (discovery == GameEngine.DiscoveryState.SCOUTED) {
                g.setColor(UiTheme.MAP_SCOUTED_OVERLAY);
                g.fillRect(x, y, tile, tile);
            }
            drawTerrainLabel(g, state.tiles[row][col], x, y, tile);
        }
        g.setClip(oldClip);

        g.setStroke(new BasicStroke(discovery == GameEngine.DiscoveryState.VISITED ?
            1.8f : 1.1f));
        g.setColor(discovery == GameEngine.DiscoveryState.VISITED ?
            new Color(214, 175, 55, 210) : new Color(187, 172, 143, 90));
        g.drawRoundRect(x, y, tile, tile, 9, 9);

        if (engine.hasRelicClueAt(row, col) &&
                discovery != GameEngine.DiscoveryState.UNKNOWN) {
            drawRelicClue(g, x, y, tile);
        }

        GameEngine.Enemy enemy = state.enemies[row][col];
        int threat = engine.threatKnowledgeAt(row, col);
        if (enemy != null && (threat >= 2 || discovery == GameEngine.DiscoveryState.VISITED)) {
            drawEnemyMini(g, enemy, x + tile - 21, y + 4, 18);
        } else if (enemy != null && threat == 1) {
            drawWarning(g, x + tile - 20, y + 4, 17);
        }

        if (row == state.row && col == state.col) {
            drawHeroMini(g, state, x + 5, y + 5, Math.max(21, tile / 2));
        }
    }

    private void drawTerrain(Graphics2D g, GameEngine.TileType type, int x, int y,
                             int tile) {
        if (terrainSheet == null) {
            g.setColor(tileFallback(type));
            g.fillRect(x, y, tile, tile);
            return;
        }
        int index = terrainIndex(type);
        int sx = (index % 3) * SHEET_CELL + SHEET_INSET;
        int sy = (index / 3) * SHEET_CELL + SHEET_INSET;
        int edge = SHEET_CELL - SHEET_INSET * 2;
        g.drawImage(terrainSheet, x, y, x + tile, y + tile,
            sx, sy, sx + edge, sy + edge, null);
    }

    private void drawTerrainLabel(Graphics2D g, GameEngine.TileType type, int x, int y,
                                  int tile) {
        String label = type == GameEngine.TileType.ENCAMPMENT ? "CAMP" :
            type.label.toUpperCase();
        g.setFont(UiTheme.body(Font.BOLD, Math.max(7, tile / 7)));
        int height = 13;
        g.setColor(new Color(8, 7, 6, 170));
        g.fillRect(x, y + tile - height, tile, height);
        g.setColor(new Color(250, 239, 208));
        drawCentered(g, label, x, y + tile - height, tile, height);
    }

    private void drawFog(Graphics2D g, int x, int y, int tile) {
        g.setColor(UiTheme.MAP_FOG);
        g.fillRect(x, y, tile, tile);
        for (int i = 0; i < 4; i++) {
            int inset = 4 + i * 5;
            g.setColor(new Color(73, 79, 91, 24 + i * 7));
            g.drawArc(x + inset, y + 7 + i * 3, Math.max(4, tile - inset * 2),
                Math.max(6, tile / 2), 15, 170);
        }
        g.setFont(UiTheme.displayBold(Math.max(17, tile / 2)));
        g.setColor(new Color(199, 190, 174, 125));
        drawCentered(g, "?", x, y, tile, tile);
    }

    private void drawHeroMini(Graphics2D g, GameEngine.State state, int x, int y,
                              int size) {
        String build = state.race.toLowerCase() + "-" +
            state.heroClass.toLowerCase();
        BufferedImage marker = load("/assets/map/markers/" + build + ".png");
        if (marker != null) {
            drawCharacterMarker(g, marker, x, y, size);
            return;
        }
        // Portraits remain a safe fallback for saves that contain a future build
        // whose dedicated map miniature has not been produced yet.
        drawPortrait(g, load("/assets/avatars/" + build + ".png"), x, y, size,
            UiTheme.GOLD);
    }

    private void drawCharacterMarker(Graphics2D g, BufferedImage image, int x, int y,
                                     int size) {
        // A small dark/gold token keeps the transparent figure readable against
        // bright water and forest tiles without clipping weapons or silhouettes.
        g.setColor(new Color(5, 5, 7, 205));
        g.fillOval(x - 1, y - 1, size + 2, size + 2);
        g.setColor(UiTheme.GOLD);
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(x - 1, y - 1, size + 2, size + 2);
        int artSize = size + 8;
        g.drawImage(image, x + (size - artSize) / 2, y + (size - artSize) / 2,
            artSize, artSize, null);
    }

    private void drawEnemyMini(Graphics2D g, GameEngine.Enemy enemy, int x, int y,
                               int size) {
        EncounterCatalog.Profile profile = EncounterCatalog.forEnemy(enemy.name);
        Color ring = enemy.tier == 2 ? UiTheme.BOSS :
            (enemy.tier == 1 ? UiTheme.ELITE : new Color(190, 85, 60));
        drawPortrait(g, load(profile.artwork), x, y, size, ring);
    }

    private void drawPortrait(Graphics2D g, BufferedImage image, int x, int y, int size,
                              Color ring) {
        g.setColor(new Color(5, 5, 7, 220));
        g.fillOval(x - 2, y - 2, size + 4, size + 4);
        Shape old = g.getClip();
        Ellipse2D clip = new Ellipse2D.Float(x, y, size, size);
        g.clip(clip);
        if (image != null) {
            double scale = Math.max((double) size / image.getWidth(),
                (double) size / image.getHeight());
            int width = (int) Math.ceil(image.getWidth() * scale);
            int height = (int) Math.ceil(image.getHeight() * scale);
            g.drawImage(image, x + (size - width) / 2, y + (size - height) / 3,
                width, height, null);
        } else {
            g.setColor(ring.darker());
            g.fillOval(x, y, size, size);
        }
        g.setClip(old);
        g.setColor(ring);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(x, y, size, size);
    }

    private void drawWarning(Graphics2D g, int x, int y, int size) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int[] xs = {cx, x + size, cx, x};
        int[] ys = {y, cy, y + size, cy};
        g.setColor(new Color(30, 11, 10, 220));
        g.fillPolygon(xs, ys, 4);
        g.setColor(UiTheme.MAP_WARNING);
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(xs, ys, 4);
        g.setColor(Color.WHITE);
        g.setFont(UiTheme.body(Font.BOLD, 11));
        drawCentered(g, "!", x, y - 1, size, size);
    }

    private void drawRelicClue(Graphics2D g, int x, int y, int tile) {
        g.setColor(new Color(199, 149, 255, 72));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(x + 3, y + 3, tile - 6, tile - 6, 7, 7);
        g.setColor(UiTheme.QUALITY_RELIC);
        g.fillOval(x + 5, y + tile - 10, 5, 5);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int tile = tileSize();
        int col = (event.getX() - LEFT) / Math.max(1, tile + GAP);
        int row = (event.getY() - TOP) / Math.max(1, tile + GAP);
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return null;
        int localX = event.getX() - (LEFT + col * (tile + GAP));
        int localY = event.getY() - (TOP + row * (tile + GAP));
        if (localX < 0 || localY < 0 || localX >= tile || localY >= tile) return null;

        GameEngine.DiscoveryState discovery = engine.discoveryAt(row, col);
        String coordinate = Character.toString((char) ('A' + row)) + (col + 1);
        if (discovery == GameEngine.DiscoveryState.UNKNOWN) {
            return coordinate + " · Uncharted — travel or seek a map to reveal it";
        }
        StringBuilder tip = new StringBuilder("<html><b>").append(coordinate)
            .append(" · ").append(engine.getState().tiles[row][col].label).append("</b>");
        GameEngine.Enemy enemy = engine.getState().enemies[row][col];
        int knowledge = engine.threatKnowledgeAt(row, col);
        if (enemy != null && knowledge >= 2) tip.append("<br>").append(enemy.name);
        else if (enemy != null && knowledge == 1) tip.append("<br>Rumored threat — identity unknown");
        if (engine.hasRelicClueAt(row, col)) tip.append("<br>Possible relic search region");
        if (discovery == GameEngine.DiscoveryState.SCOUTED) tip.append("<br>Scouted, not yet visited");
        return tip.append("</html>").toString();
    }

    private int tileSize() {
        return Math.max(16, Math.min((getWidth() - LEFT - 7 - GAP * 4) / SIZE,
            (getHeight() - TOP - 7 - GAP * 4) / SIZE));
    }

    private static BufferedImage load(String resource) {
        if (resource == null) return null;
        synchronized (IMAGES) {
            if (IMAGES.containsKey(resource)) return IMAGES.get(resource);
            BufferedImage image = null;
            try { image = ImageIO.read(GameMapPanel.class.getResource(resource)); }
            catch (Exception ignored) { }
            IMAGES.put(resource, image);
            return image;
        }
    }

    private int terrainIndex(GameEngine.TileType type) {
        switch (type) {
            case LAKE: return 1;
            case CRYPT: return 2;
            case TAVERN: return 3;
            case SHOP: return 4;
            case ENCAMPMENT: return 5;
            default: return 0;
        }
    }

    private Color tileFallback(GameEngine.TileType type) {
        switch (type) {
            case LAKE: return new Color(47, 91, 116);
            case CRYPT: return new Color(72, 68, 80);
            case TAVERN: return new Color(126, 76, 46);
            case SHOP: return new Color(119, 91, 42);
            case ENCAMPMENT: return new Color(91, 82, 53);
            default: return new Color(54, 92, 61);
        }
    }

    private void drawCentered(Graphics2D g, String text, int x, int y, int width,
                              int height) {
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + (width - metrics.stringWidth(text)) / 2;
        int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(text, textX, textY);
    }
}
