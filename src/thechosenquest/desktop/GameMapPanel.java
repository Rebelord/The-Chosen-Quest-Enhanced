package thechosenquest.desktop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Illustrated world map renderer. World knowledge lives in GameEngine.State;
 * this class only translates Unknown, Scouted, and Visited tiles into visuals.
 */
final class GameMapPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    /** The two authored densities keep map markers legible and controls simple. */
    static final int DETAIL_VIEW_SIZE = 7;
    static final int OVERVIEW_VIEW_SIZE = 9;
    private static final int GAP = 4;
    private static final int TOP = 24;
    private static final int LEFT = 24;
    private static final int SHEET_CELL = 512;
    private static final int SHEET_INSET = 9;
    private static final Map<String, BufferedImage> IMAGES =
        new HashMap<String, BufferedImage>();
    private final GameEngine engine;
    private final BufferedImage terrainSheet;
    private final List<MarkerHit> markerHits = new ArrayList<MarkerHit>();
    private int viewRow;
    private int viewCol;
    private int viewSize = OVERVIEW_VIEW_SIZE;
    private boolean trackingHero = true;

    GameMapPanel(GameEngine engine) {
        this.engine = engine;
        terrainSheet = load("/assets/map/terrain-sheet.png");
        setOpaque(false);
        setPreferredSize(new Dimension(288, 300));
        setToolTipText("World map");
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                for (MarkerHit hit : markerHits) {
                    if (hit.bounds.contains(event.getPoint())) {
                        focusOn(hit.marker.row, hit.marker.col);
                        return;
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        GameEngine.State state = engine.getState();
        updateTrackedOrigin(state);
        int tile = tileSize();
        drawCoordinates(g, tile);

        for (int localRow = 0; localRow < viewSize; localRow++) {
            for (int localCol = 0; localCol < viewSize; localCol++) {
                int row = viewRow + localRow;
                int col = viewCol + localCol;
                int x = LEFT + localCol * (tile + GAP);
                int y = TOP + localRow * (tile + GAP);
                drawTile(g, state, row, col, x, y, tile);
            }
        }
        drawOffscreenIndicators(g, state, tile);
        g.dispose();
    }

    private void drawCoordinates(Graphics2D g, int tile) {
        g.setFont(UiTheme.body(Font.BOLD, 10));
        g.setColor(new Color(210, 199, 174));
        for (int i = 0; i < viewSize; i++) {
            drawCentered(g, Integer.toString(viewCol + i + 1), LEFT + i * (tile + GAP), 3,
                tile, 17);
            drawCentered(g, Character.toString((char) ('A' + viewRow + i)), 1,
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
        }
        g.setClip(oldClip);

        g.setStroke(new BasicStroke(discovery == GameEngine.DiscoveryState.VISITED ?
            1.8f : 1.1f));
        g.setColor(discovery == GameEngine.DiscoveryState.VISITED ?
            new Color(214, 175, 55, 210) : new Color(187, 172, 143, 90));
        g.drawRoundRect(x, y, tile, tile, 9, 9);

        if (discovery == GameEngine.DiscoveryState.SCOUTED) {
            drawScoutedCue(g, x, y, tile);
        }
        if (discovery != GameEngine.DiscoveryState.UNKNOWN) {
            drawPointOfInterest(g, state, row, col, x, y, tile);
        }

        if (engine.hasRelicClueAt(row, col) &&
                discovery != GameEngine.DiscoveryState.UNKNOWN) {
            drawRelicClue(g, x, y, tile);
        }

        GameEngine.Enemy enemy = state.enemies[row][col];
        int threat = engine.threatKnowledgeAt(row, col);
        if (enemy != null && (threat >= 2 || discovery == GameEngine.DiscoveryState.VISITED)) {
            int enemySize = enemyMarkerSize(tile);
            drawEnemyMini(g, enemy, x + tile - enemySize - 3, y + 3, enemySize);
        } else if (enemy != null && threat == 1) {
            int warningSize = warningMarkerSize(tile);
            drawWarning(g, x + tile - warningSize - 3, y + 3, warningSize);
        }

        if (row == state.row && col == state.col) {
            int heroSize = heroMarkerSize(tile);
            drawHeroMini(g, state, x + (tile - heroSize) / 2,
                y + (tile - heroSize) / 2, heroSize);
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

    private void drawFog(Graphics2D g, int x, int y, int tile) {
        g.setColor(UiTheme.MAP_FOG);
        g.fillRect(x, y, tile, tile);
        for (int i = 0; i < 4; i++) {
            int inset = 4 + i * 5;
            g.setColor(new Color(73, 79, 91, 24 + i * 7));
            g.drawArc(x + inset, y + 7 + i * 3, Math.max(4, tile - inset * 2),
                Math.max(6, tile / 2), 15, 170);
        }
    }

    private void drawScoutedCue(Graphics2D g, int x, int y, int tile) {
        int size = Math.max(4, tile / 6);
        g.setColor(new Color(185, 207, 215, 210));
        g.fillArc(x + 3, y + 3, size, size, 90, 180);
        g.setColor(new Color(25, 31, 38, 210));
        g.drawArc(x + 3, y + 3, size, size, 90, 180);
    }

    /** Ordinary terrain remains artwork-only; only authored landmarks get glyphs. */
    private void drawPointOfInterest(Graphics2D g, GameEngine.State state,
                                     int row, int col, int x, int y, int tile) {
        int cx = x + tile / 2;
        int cy = y + tile / 2;
        int radius = Math.max(4, tile / 5);
        GameEngine.TileType type = state.tiles[row][col];
        g.setStroke(new BasicStroke(Math.max(1.2f, tile / 18f)));
        if (type == GameEngine.TileType.SHOP) {
            int[] xs = {cx, cx + radius, cx, cx - radius};
            int[] ys = {cy - radius, cy, cy + radius, cy};
            g.setColor(new Color(24, 16, 9, 220));
            g.fillPolygon(xs, ys, 4);
            g.setColor(state.blacksmithShops[row][col]
                ? new Color(221, 129, 69) : UiTheme.GOLD_LIGHT);
            g.drawPolygon(xs, ys, 4);
            if (state.blacksmithShops[row][col]) {
                g.drawLine(cx - 3, cy + 2, cx + 3, cy - 2);
                g.drawLine(cx - 2, cy - 3, cx + 3, cy + 3);
            } else {
                g.fillOval(cx - 2, cy - 2, 4, 4);
            }
        } else if (type == GameEngine.TileType.TAVERN) {
            g.setColor(new Color(30, 18, 10, 225));
            g.fillRect(cx - radius, cy - 1, radius * 2, radius + 1);
            int[] roofX = {cx - radius - 1, cx, cx + radius + 1};
            int[] roofY = {cy, cy - radius, cy};
            g.fillPolygon(roofX, roofY, 3);
            g.setColor(new Color(225, 153, 82));
            g.drawRect(cx - radius, cy - 1, radius * 2, radius + 1);
            g.drawPolygon(roofX, roofY, 3);
        } else if (type == GameEngine.TileType.ENCAMPMENT) {
            int[] tentX = {cx - radius, cx, cx + radius};
            int[] tentY = {cy + radius, cy - radius, cy + radius};
            g.setColor(new Color(18, 25, 17, 220));
            g.fillPolygon(tentX, tentY, 3);
            g.setColor(new Color(112, 184, 116));
            g.drawPolygon(tentX, tentY, 3);
            g.drawLine(cx, cy - radius, cx, cy + radius);
        } else if (type == GameEngine.TileType.SPIDER_NEST) {
            boolean cleared = state.spiderNestCleared;
            Color web = cleared ? new Color(111, 137, 119) : UiTheme.QUALITY_RELIC;
            g.setColor(new Color(12, 16, 14, 225));
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.setColor(web);
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.drawOval(cx - radius / 2, cy - radius / 2, radius, radius);
            g.drawLine(cx - radius, cy, cx + radius, cy);
            g.drawLine(cx, cy - radius, cx, cy + radius);
            g.drawLine(cx - radius + 2, cy - radius + 2,
                cx + radius - 2, cy + radius - 2);
            g.drawLine(cx + radius - 2, cy - radius + 2,
                cx - radius + 2, cy + radius - 2);
        } else if (row == state.dragonLairRow && col == state.dragonLairCol) {
            g.setColor(new Color(24, 8, 10, 220));
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.setColor(UiTheme.BOSS);
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.drawOval(cx - radius + 3, cy - radius + 3,
                Math.max(2, radius * 2 - 6), Math.max(2, radius * 2 - 6));
        }
    }

    private void drawHeroMini(Graphics2D g, GameEngine.State state, int x, int y,
                              int size) {
        if (size <= 10) {
            drawSimpleMarker(g, x, y, size, UiTheme.GOLD, true);
            return;
        }
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
        Shape old = g.getClip();
        g.clip(new Ellipse2D.Float(x, y, size, size));
        int artSize = size + Math.max(2, size / 5);
        g.drawImage(image, x + (size - artSize) / 2, y + (size - artSize) / 2,
            artSize, artSize, null);
        g.setClip(old);
        g.setColor(UiTheme.GOLD);
        g.drawOval(x - 1, y - 1, size + 2, size + 2);
    }

    private void drawEnemyMini(Graphics2D g, GameEngine.Enemy enemy, int x, int y,
                               int size) {
        EncounterCatalog.Profile profile = EncounterCatalog.forEnemy(enemy.name);
        Color ring = enemy.tier == 2 ? UiTheme.BOSS :
            (enemy.tier == 1 ? UiTheme.ELITE : new Color(190, 85, 60));
        if (size <= 9) {
            drawSimpleMarker(g, x, y, size, ring, false);
            return;
        }
        drawPortrait(g, load(profile.artwork), x, y, size, ring);
    }

    private void drawSimpleMarker(Graphics2D g, int x, int y, int size,
                                  Color ring, boolean hero) {
        g.setColor(new Color(7, 7, 9, 225));
        g.fillOval(x, y, size, size);
        g.setColor(ring);
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(x, y, size, size);
        int inset = Math.max(2, size / 3);
        if (hero) {
            int[] xs = {x + size / 2, x + size - inset, x + inset};
            int[] ys = {y + inset, y + size - inset, y + size - inset};
            g.fillPolygon(xs, ys, 3);
        } else {
            g.fillOval(x + inset, y + inset,
                Math.max(2, size - inset * 2), Math.max(2, size - inset * 2));
        }
    }

    static int heroMarkerSize(int tile) {
        return Math.max(8, Math.min(Math.max(8, tile - 6),
            Math.round(tile * 0.58f)));
    }

    static int enemyMarkerSize(int tile) {
        return Math.max(7, Math.min(Math.max(7, tile - 6),
            Math.round(tile * 0.46f)));
    }

    static int warningMarkerSize(int tile) {
        return Math.max(7, Math.min(Math.max(7, tile - 6),
            Math.round(tile * 0.40f)));
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

    void centerOnHero() {
        trackingHero = true;
        updateTrackedOrigin(engine.getState());
        repaint();
    }

    void pan(int rowDelta, int colDelta) {
        trackingHero = false;
        viewRow = clampOrigin(viewRow + rowDelta);
        viewCol = clampOrigin(viewCol + colDelta);
        repaint();
    }

    void followHero() {
        if (trackingHero) {
            updateTrackedOrigin(engine.getState());
            repaint();
        }
    }

    void zoomIn() {
        setViewSize(DETAIL_VIEW_SIZE);
    }

    void zoomOut() {
        setViewSize(OVERVIEW_VIEW_SIZE);
    }

    private void setViewSize(int requestedSize) {
        int nextSize = requestedSize <= DETAIL_VIEW_SIZE
            ? DETAIL_VIEW_SIZE : OVERVIEW_VIEW_SIZE;
        if (nextSize == viewSize) return;

        // Preserve the point the player was inspecting when manually panned.
        // Hero-tracking mode remains centered on the hero at either density.
        int focusRow = viewRow + viewSize / 2;
        int focusCol = viewCol + viewSize / 2;
        viewSize = nextSize;
        if (trackingHero) {
            updateTrackedOrigin(engine.getState());
        } else {
            viewRow = clampOrigin(focusRow - viewSize / 2);
            viewCol = clampOrigin(focusCol - viewSize / 2);
        }
        revalidate();
        repaint();
    }

    private void focusOn(int row, int col) {
        trackingHero = false;
        viewRow = clampOrigin(row - viewSize / 2);
        viewCol = clampOrigin(col - viewSize / 2);
        repaint();
    }

    private void updateTrackedOrigin(GameEngine.State state) {
        if (!trackingHero || state == null) return;
        viewRow = clampOrigin(state.row - viewSize / 2);
        viewCol = clampOrigin(state.col - viewSize / 2);
    }

    private int clampOrigin(int value) {
        return Math.max(0, Math.min(GameEngine.SIZE - viewSize, value));
    }

    private boolean visibleInViewport(int row, int col) {
        return row >= viewRow && row < viewRow + viewSize &&
            col >= viewCol && col < viewCol + viewSize;
    }

    private void drawOffscreenIndicators(Graphics2D g, GameEngine.State state, int tile) {
        markerHits.clear();
        OffscreenMarker[] best = new OffscreenMarker[Edge.values().length];
        int[] counts = new int[best.length];
        int centerRow = viewRow + viewSize / 2;
        int centerCol = viewCol + viewSize / 2;
        for (int row = 0; row < GameEngine.SIZE; row++) {
            for (int col = 0; col < GameEngine.SIZE; col++) {
                if (visibleInViewport(row, col)) continue;
                OffscreenMarker marker = markerFor(state, row, col, centerRow, centerCol);
                if (marker == null) continue;
                int edge = marker.edge.ordinal();
                counts[edge]++;
                if (best[edge] == null || marker.priority > best[edge].priority ||
                        (marker.priority == best[edge].priority &&
                            marker.distance < best[edge].distance)) {
                    best[edge] = marker;
                }
            }
        }

        int grid = viewSize * tile + (viewSize - 1) * GAP;
        for (Edge edge : Edge.values()) {
            OffscreenMarker marker = best[edge.ordinal()];
            if (marker == null) continue;
            int width = counts[edge.ordinal()] > 1 ? 40 : 30;
            int height = 22;
            int x = LEFT + grid / 2 - width / 2;
            int y = TOP + grid / 2 - height / 2;
            if (edge == Edge.NORTH) y = TOP + 2;
            else if (edge == Edge.SOUTH) y = TOP + grid - height - 2;
            else if (edge == Edge.WEST) x = LEFT + 2;
            else x = LEFT + grid - width - 2;
            Rectangle bounds = new Rectangle(x, y, width, height);
            markerHits.add(new MarkerHit(bounds, marker));
            g.setColor(new Color(9, 8, 8, 225));
            g.fillRoundRect(x, y, width, height, 9, 9);
            g.setColor(marker.color);
            g.setStroke(new BasicStroke(1.7f));
            g.drawRoundRect(x, y, width, height, 9, 9);
            g.setFont(UiTheme.body(Font.BOLD, 11));
            String copy = edge.arrow + marker.symbol +
                (counts[edge.ordinal()] > 1 ? Integer.toString(counts[edge.ordinal()]) : "");
            drawCentered(g, copy, x, y, width, height);
        }
    }

    private OffscreenMarker markerFor(GameEngine.State state, int row, int col,
                                      int centerRow, int centerCol) {
        GameEngine.DiscoveryState discovery = engine.discoveryAt(row, col);
        GameEngine.Enemy enemy = state.enemies[row][col];
        int threat = engine.threatKnowledgeAt(row, col);
        int priority = -1;
        String symbol = null;
        String label = null;
        Color color = UiTheme.MUTED;
        boolean vague = false;

        if (enemy != null && threat > 0) {
            priority = enemy.tier == 2 ? 100 : (enemy.tier == 1 ? 82 : 64);
            symbol = "!";
            label = threat >= 2 ? enemy.name : "Rumored threat";
            vague = threat < 2;
            color = enemy.tier == 2 ? UiTheme.BOSS :
                (enemy.tier == 1 ? UiTheme.ELITE : UiTheme.MAP_WARNING);
        }
        if (engine.hasRelicClueAt(row, col) &&
                discovery != GameEngine.DiscoveryState.UNKNOWN && priority < 76) {
            priority = 76;
            symbol = "◆";
            label = "Possible relic region";
            color = UiTheme.QUALITY_RELIC;
            vague = true;
        }
        GameEngine.TileType tile = state.tiles[row][col];
        boolean landmark = tile == GameEngine.TileType.SHOP ||
            tile == GameEngine.TileType.TAVERN || tile == GameEngine.TileType.ENCAMPMENT ||
            tile == GameEngine.TileType.SPIDER_NEST;
        if (landmark && discovery != GameEngine.DiscoveryState.UNKNOWN && priority < 50) {
            priority = 50;
            symbol = "⌖";
            label = tile == GameEngine.TileType.SPIDER_NEST && state.spiderNestCleared
                ? "Cleared Spider Nest" : tile.label;
            color = tile == GameEngine.TileType.SPIDER_NEST
                ? (state.spiderNestCleared ? new Color(111, 137, 119) : UiTheme.QUALITY_RELIC)
                : new Color(177, 136, 71);
        }
        if (priority < 0) return null;

        int rowDelta = row - centerRow;
        int colDelta = col - centerCol;
        Edge edge = Math.abs(colDelta) >= Math.abs(rowDelta)
            ? (colDelta < 0 ? Edge.WEST : Edge.EAST)
            : (rowDelta < 0 ? Edge.NORTH : Edge.SOUTH);
        int distance = Math.abs(row - state.row) + Math.abs(col - state.col);
        return new OffscreenMarker(row, col, priority, distance, edge,
            symbol, label, color, vague);
    }

    String offscreenHintSummaryForTest() {
        StringBuilder result = new StringBuilder();
        for (MarkerHit hit : markerHits) {
            if (result.length() > 0) result.append(" | ");
            result.append(hit.marker.edge).append(' ').append(hit.marker.label)
                .append(hit.marker.vague ? " · direction uncertain" :
                    " · " + hit.marker.distance + " tiles");
        }
        return result.toString();
    }

    int viewRowForTest() { return viewRow; }
    int viewColForTest() { return viewCol; }
    int viewSizeForTest() { return viewSize; }

    @Override
    public String getToolTipText(MouseEvent event) {
        for (MarkerHit hit : markerHits) {
            if (hit.bounds.contains(event.getPoint())) {
                return hit.marker.label + (hit.marker.vague ? " · approximate direction" :
                    " · " + hit.marker.distance + " tiles") + " — click to inspect";
            }
        }
        int tile = tileSize();
        int localCol = (event.getX() - LEFT) / Math.max(1, tile + GAP);
        int localRow = (event.getY() - TOP) / Math.max(1, tile + GAP);
        if (localRow < 0 || localRow >= viewSize ||
                localCol < 0 || localCol >= viewSize) return null;
        int localX = event.getX() - (LEFT + localCol * (tile + GAP));
        int localY = event.getY() - (TOP + localRow * (tile + GAP));
        if (localX < 0 || localY < 0 || localX >= tile || localY >= tile) return null;

        int row = viewRow + localRow;
        int col = viewCol + localCol;

        GameEngine.DiscoveryState discovery = engine.discoveryAt(row, col);
        String coordinate = Character.toString((char) ('A' + row)) + (col + 1);
        if (discovery == GameEngine.DiscoveryState.UNKNOWN) {
            return coordinate + " · Uncharted — travel or seek a map to reveal it";
        }
        StringBuilder tip = new StringBuilder("<html><b>").append(coordinate)
            .append(" · ").append(engine.getState().tiles[row][col].label).append("</b>");
        GameEngine.State state = engine.getState();
        if (row == state.dragonLairRow && col == state.dragonLairCol) {
            tip.append("<br>Dragon lair");
        } else if (state.tiles[row][col] == GameEngine.TileType.SHOP) {
            tip.append("<br>").append(state.blacksmithShops[row][col]
                ? "Blacksmith" : "General Merchant");
        } else if (state.tiles[row][col] == GameEngine.TileType.TAVERN) {
            tip.append("<br>Safe tavern");
        } else if (state.tiles[row][col] == GameEngine.TileType.ENCAMPMENT) {
            tip.append("<br>Safe encampment");
        } else if (state.tiles[row][col] == GameEngine.TileType.SPIDER_NEST) {
            tip.append(state.spiderNestCleared
                ? "<br>Permanently cleared threat source"
                : "<br>Finite threat source · " + state.spiderNestRemaining + " brood remaining");
        }
        GameEngine.Enemy enemy = state.enemies[row][col];
        int knowledge = engine.threatKnowledgeAt(row, col);
        if (enemy != null && knowledge >= 2) tip.append("<br>").append(enemy.name);
        else if (enemy != null && knowledge == 1) tip.append("<br>Rumored threat — identity unknown");
        if (engine.hasRelicClueAt(row, col)) tip.append("<br>Possible relic search region");
        if (discovery == GameEngine.DiscoveryState.SCOUTED) tip.append("<br>Scouted, not yet visited");
        return tip.append("</html>").toString();
    }

    private int tileSize() {
        return Math.max(16, Math.min((getWidth() - LEFT - 7 - GAP * (viewSize - 1)) /
                viewSize,
            (getHeight() - TOP - 7 - GAP * (viewSize - 1)) / viewSize));
    }

    private enum Edge {
        NORTH("↑"), EAST("→"), SOUTH("↓"), WEST("←");
        final String arrow;
        Edge(String arrow) { this.arrow = arrow; }
    }

    private static final class OffscreenMarker {
        final int row;
        final int col;
        final int priority;
        final int distance;
        final Edge edge;
        final String symbol;
        final String label;
        final Color color;
        final boolean vague;

        OffscreenMarker(int row, int col, int priority, int distance, Edge edge,
                        String symbol, String label, Color color, boolean vague) {
            this.row = row;
            this.col = col;
            this.priority = priority;
            this.distance = distance;
            this.edge = edge;
            this.symbol = symbol;
            this.label = label;
            this.color = color;
            this.vague = vague;
        }
    }

    private static final class MarkerHit {
        final Rectangle bounds;
        final OffscreenMarker marker;

        MarkerHit(Rectangle bounds, OffscreenMarker marker) {
            this.bounds = bounds;
            this.marker = marker;
        }
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
            case SPIDER_NEST: return 0;
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
            case SPIDER_NEST: return new Color(45, 72, 49);
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
