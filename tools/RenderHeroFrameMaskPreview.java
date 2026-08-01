import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Renders a non-production comparison of candidate hero-frame semantic masks.
 */
public final class RenderHeroFrameMaskPreview {
    private static final Color[] COLORS = {
        new Color(196, 72, 55),
        new Color(61, 147, 218),
        new Color(145, 76, 190),
        new Color(73, 160, 102)
    };
    private static final String[] PORTRAITS = {
        "assets/avatars/full-body/human-fighter.png",
        "assets/avatars/full-body/elf-mage.png",
        "assets/avatars/full-body/human-rogue.png",
        "assets/avatars/full-body/halfling-hunter.png"
    };

    private RenderHeroFrameMaskPreview() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                "Usage: RenderHeroFrameMaskPreview <overlay> <enamel-mask> " +
                "<gem-mask> <output>");
        }
        BufferedImage overlay = read(args[0]);
        BufferedImage enamel = read(args[1]);
        BufferedImage gems = read(args[2]);
        int frameWidth = 326;
        int frameHeight = Math.round(frameWidth * overlay.getHeight() /
            (float) overlay.getWidth());
        int gap = 24;
        int margin = 30;
        BufferedImage preview = new BufferedImage(
            margin * 2 + frameWidth * 4 + gap * 3,
            margin * 2 + frameHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = preview.createGraphics();
        canvas.setColor(new Color(20, 15, 12));
        canvas.fillRect(0, 0, preview.getWidth(), preview.getHeight());
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        for (int i = 0; i < COLORS.length; i++) {
            int x = margin + i * (frameWidth + gap);
            BufferedImage portrait = read(PORTRAITS[i]);
            canvas.drawImage(portrait, x + 44, margin + 48,
                frameWidth - 88, frameHeight - 96, null);
            BufferedImage frame = compose(overlay, enamel, gems, COLORS[i],
                frameWidth, frameHeight);
            canvas.drawImage(frame, x, margin, null);
        }
        canvas.dispose();
        if (!ImageIO.write(preview, "png", new File(args[3]))) {
            throw new IllegalStateException("PNG writer is unavailable");
        }
    }

    private static BufferedImage compose(BufferedImage overlay,
                                         BufferedImage enamel,
                                         BufferedImage gems,
                                         Color color,
                                         int width, int height) {
        BufferedImage result = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        paintMaskedColor(graphics, enamel, softened(color), width, height);
        paintMaskedColor(graphics, gems, color.brighter(), width, height);
        graphics.drawImage(overlay, 0, 0, width, height, null);
        graphics.dispose();
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

    private static Color softened(Color color) {
        return new Color(
            Math.round(color.getRed() * .72f),
            Math.round(color.getGreen() * .72f),
            Math.round(color.getBlue() * .72f));
    }

    private static BufferedImage read(String path) throws Exception {
        BufferedImage image = ImageIO.read(new File(path));
        if (image == null) throw new IllegalArgumentException("Unreadable " + path);
        return image;
    }
}
