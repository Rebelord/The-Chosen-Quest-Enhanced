import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Produces compact, deterministic square UI assets from generated source art. */
public final class ResizeUiIcon {
    private ResizeUiIcon() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                "Usage: ResizeUiIcon <size> <input> <output> [<input> <output> ...]");
        }
        int size = Integer.parseInt(args[0]);
        for (int index = 1; index + 1 < args.length; index += 2) {
            BufferedImage source = ImageIO.read(new File(args[index]));
            if (source == null) {
                throw new IllegalArgumentException("Unreadable image: " + args[index]);
            }
            String outputName = args[index + 1].toLowerCase();
            boolean transparent = outputName.endsWith(".png");
            BufferedImage result = new BufferedImage(size, size,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = result.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, size, size, null);
            graphics.dispose();
            String format = transparent ? "png" : "jpg";
            if (!ImageIO.write(result, format, new File(args[index + 1]))) {
                throw new IllegalStateException("No " + format + " writer available");
            }
        }
    }
}
