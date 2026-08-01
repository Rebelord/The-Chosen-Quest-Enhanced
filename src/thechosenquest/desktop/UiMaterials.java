package thechosenquest.desktop;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Cached painterly material layers shared by responsive UI components.
 *
 * Geometry, masks, tint, focus, and hit targets stay in code. These images provide
 * only the surface variation that vector fills cannot convincingly reproduce.
 */
final class UiMaterials {
    private static final int TILE_SIZE = 320;
    private static final TexturePaint LACQUER =
        load("/assets/ui/materials/lacquer-neutral.png");
    private static final TexturePaint PARCHMENT =
        load("/assets/ui/materials/parchment-warm.png");

    private UiMaterials() { }

    static void paintLacquer(Graphics2D graphics, Shape clip, float opacity) {
        paint(graphics, clip, LACQUER, opacity);
    }

    static void paintParchment(Graphics2D graphics, Shape clip, float opacity) {
        paint(graphics, clip, PARCHMENT, opacity);
    }

    private static void paint(Graphics2D graphics, Shape clip,
                              TexturePaint material, float opacity) {
        if (graphics == null || material == null || opacity <= 0f) return;
        Shape originalClip = graphics.getClip();
        Composite originalComposite = graphics.getComposite();
        Paint originalPaint = graphics.getPaint();
        if (clip != null) graphics.clip(clip);
        graphics.setComposite(AlphaComposite.SrcOver.derive(
            Math.max(0f, Math.min(1f, opacity))));
        graphics.setPaint(material);
        java.awt.Rectangle bounds = graphics.getClipBounds();
        if (bounds != null) {
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        graphics.setPaint(originalPaint);
        graphics.setComposite(originalComposite);
        graphics.setClip(originalClip);
    }

    private static TexturePaint load(String resource) {
        InputStream stream = UiMaterials.class.getResourceAsStream(resource);
        if (stream == null) return null;
        try {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) return null;
            return new TexturePaint(image,
                new Rectangle2D.Double(0, 0, TILE_SIZE, TILE_SIZE));
        } catch (Exception ignored) {
            return null;
        } finally {
            try { stream.close(); } catch (Exception ignored) { }
        }
    }
}
