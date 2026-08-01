package thechosenquest.desktop;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Cached authored border artwork used by responsive Swing controls.
 *
 * The button frame is rendered as a horizontal three-slice: the metalsmith-style
 * corner caps keep their original proportions while only the quiet center rails
 * stretch. Interaction color is a restrained overlay, so class themes can affect
 * the hardware without replacing its brass material and painterly wear.
 */
final class UiBorderAssets {
    private static final BufferedImage BUTTON_FRAME =
        load("/assets/ui/borders/button-frame-brass-v1.png");
    private static final BufferedImage PANEL_FRAME =
        load("/assets/ui/borders/panel-frame-brass-v1.png");
    private static final BufferedImage HERO_FRAME_FALLBACK =
        load("/assets/ui/borders/hero-frame-brass-v1.png");
    private static final BufferedImage HERO_FRAME_METAL =
        load("/assets/ui/borders/hero-frame-metal-overlay-v5.png");
    private static final BufferedImage HERO_FRAME_ENAMEL_MASK =
        load("/assets/ui/borders/hero-frame-enamel-mask-v5.png");
    private static final BufferedImage HERO_FRAME_GEM_MASK =
        load("/assets/ui/borders/hero-frame-gem-mask-v5.png");
    private static final int BUTTON_SOURCE_CAP = 170;
    private static final int PANEL_SOURCE_CAP = 175;
    private static final Map<String, SoftReference<BufferedImage>> HERO_FRAME_CACHE =
        new LinkedHashMap<String, SoftReference<BufferedImage>>(16, .75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<String, SoftReference<BufferedImage>> eldest) {
                return size() > 24;
            }
        };

    private UiBorderAssets() { }

    static boolean paintButtonFrame(Graphics2D graphics, int x, int y,
                                    int width, int height, float opacity,
                                    Color interactionTint, float tintStrength) {
        if (graphics == null || BUTTON_FRAME == null || width <= 0 || height <= 0) {
            return false;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
        g.setComposite(AlphaComposite.SrcOver.derive(clamp(opacity)));

        int sourceWidth = BUTTON_FRAME.getWidth();
        int sourceHeight = BUTTON_FRAME.getHeight();
        int sourceCap = Math.min(BUTTON_SOURCE_CAP, sourceWidth / 2);
        int destinationCap = Math.max(12,
            Math.min(width / 2, Math.round(height * sourceCap / (float) sourceHeight)));
        int destinationRight = x + width - destinationCap;
        int sourceRight = sourceWidth - sourceCap;

        drawSlice(g, x, y, x + destinationCap, y + height,
            0, 0, sourceCap, sourceHeight);
        if (destinationRight > x + destinationCap) {
            drawSlice(g, x + destinationCap, y, destinationRight, y + height,
                sourceCap, 0, sourceRight, sourceHeight);
        }
        drawSlice(g, destinationRight, y, x + width, y + height,
            sourceRight, 0, sourceWidth, sourceHeight);

        float tint = clamp(tintStrength);
        if (interactionTint != null && tint > 0f) {
            Composite originalComposite = g.getComposite();
            g.setComposite(AlphaComposite.SrcAtop.derive(tint * clamp(opacity)));
            g.setColor(interactionTint);
            g.fillRect(x, y, width, height);
            g.setComposite(originalComposite);
        }
        g.dispose();
        return true;
    }

    /**
     * Nine-slice panel hardware. Both axes preserve the stepped corners while
     * only the straight rail segments stretch with the component.
     */
    static boolean paintPanelFrame(Graphics2D graphics, int x, int y,
                                   int width, int height, float opacity) {
        if (graphics == null || PANEL_FRAME == null || width <= 0 || height <= 0) {
            return false;
        }
        Graphics2D g = prepared(graphics, opacity);
        int sourceWidth = PANEL_FRAME.getWidth();
        int sourceHeight = PANEL_FRAME.getHeight();
        int sourceCap = Math.min(PANEL_SOURCE_CAP,
            Math.min(sourceWidth, sourceHeight) / 2);
        int destinationCap = Math.max(7, Math.min(34,
            Math.round(Math.min(width, height) *
                sourceCap / (float) Math.min(sourceWidth, sourceHeight))));
        destinationCap = Math.min(destinationCap, Math.min(width, height) / 2);

        int[] dx = {x, x + destinationCap, x + width - destinationCap, x + width};
        int[] dy = {y, y + destinationCap, y + height - destinationCap, y + height};
        int[] sx = {0, sourceCap, sourceWidth - sourceCap, sourceWidth};
        int[] sy = {0, sourceCap, sourceHeight - sourceCap, sourceHeight};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                drawPanelSlice(g, dx[column], dy[row], dx[column + 1], dy[row + 1],
                    sx[column], sy[row], sx[column + 1], sy[row + 1]);
            }
        }
        g.dispose();
        return true;
    }

    /**
     * The selected hero holder always preserves 3:4, matching this authored
     * overlay. Scaling the complete overlay therefore preserves every crown,
     * side tier, socket, and lower mount without nine-slice seams.
     */
    static boolean paintHeroFrame(Graphics2D graphics, int x, int y,
                                  int width, int height, float opacity,
                                  Color tintColor) {
        if (graphics == null || width <= 0 || height <= 0 ||
                (HERO_FRAME_METAL == null && HERO_FRAME_FALLBACK == null)) {
            return false;
        }
        Graphics2D g = prepared(graphics, opacity);
        BufferedImage composed = composedHeroFrame(width, height, tintColor);
        if (composed != null) {
            g.drawImage(composed, x, y, null);
        } else {
            g.drawImage(HERO_FRAME_FALLBACK, x, y, width, height, null);
        }
        g.dispose();
        return true;
    }

    /**
     * Class color is painted into native-size enamel and gemstone underlays.
     * Both masks extend slightly beneath the authored brass, which is composited
     * last and remains the authoritative visible boundary. No runtime geometry
     * or feature positioning is involved.
     */
    private static BufferedImage composedHeroFrame(int width, int height,
                                                   Color tintColor) {
        if (HERO_FRAME_METAL == null || HERO_FRAME_ENAMEL_MASK == null ||
                HERO_FRAME_GEM_MASK == null) return null;
        Color color = tintColor == null ? UiTheme.GOLD : tintColor;
        String key = width + "x" + height + "|" + color.getRGB();
        synchronized (HERO_FRAME_CACHE) {
            SoftReference<BufferedImage> reference = HERO_FRAME_CACHE.get(key);
            BufferedImage cached = reference == null ? null : reference.get();
            if (cached != null) return cached;
        }

        BufferedImage result = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
        paintMaskedColor(g, HERO_FRAME_ENAMEL_MASK, softenedFrameColor(color),
            width, height);
        paintMaskedColor(g, HERO_FRAME_GEM_MASK, color.brighter(),
            width, height);
        g.drawImage(HERO_FRAME_METAL, 0, 0, width, height, null);
        g.dispose();

        synchronized (HERO_FRAME_CACHE) {
            HERO_FRAME_CACHE.put(key, new SoftReference<BufferedImage>(result));
        }
        return result;
    }

    private static void paintMaskedColor(Graphics2D destination,
                                         BufferedImage mask, Color color,
                                         int width, int height) {
        BufferedImage layer = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = layer.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(mask, 0, 0, width, height, null);
        graphics.setComposite(AlphaComposite.SrcIn);
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        destination.drawImage(layer, 0, 0, null);
    }

    private static Color softenedFrameColor(Color color) {
        return new Color(
            Math.round(color.getRed() * .72f),
            Math.round(color.getGreen() * .72f),
            Math.round(color.getBlue() * .72f));
    }

    private static void drawSlice(Graphics2D g,
                                  int dx1, int dy1, int dx2, int dy2,
                                  int sx1, int sy1, int sx2, int sy2) {
        g.drawImage(BUTTON_FRAME, dx1, dy1, dx2, dy2,
            sx1, sy1, sx2, sy2, null);
    }

    private static void drawPanelSlice(Graphics2D g,
                                       int dx1, int dy1, int dx2, int dy2,
                                       int sx1, int sy1, int sx2, int sy2) {
        if (dx2 <= dx1 || dy2 <= dy1) return;
        g.drawImage(PANEL_FRAME, dx1, dy1, dx2, dy2,
            sx1, sy1, sx2, sy2, null);
    }

    private static Graphics2D prepared(Graphics2D graphics, float opacity) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
        g.setComposite(AlphaComposite.SrcOver.derive(clamp(opacity)));
        return g;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static BufferedImage load(String resource) {
        InputStream stream = UiBorderAssets.class.getResourceAsStream(resource);
        if (stream == null) return null;
        try {
            return ImageIO.read(stream);
        } catch (Exception ignored) {
            return null;
        } finally {
            try { stream.close(); } catch (Exception ignored) { }
        }
    }
}
