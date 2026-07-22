import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Normalizes generated scene art to the exploration panel's 1536x672 canvas. */
public final class NormalizeSceneArt {
    private static final int WIDTH = 1536;
    private static final int HEIGHT = 672;

    private NormalizeSceneArt() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("input output verticalAnchor(0..1)|fit");
        }
        BufferedImage source = ImageIO.read(new File(args[0]));
        if (source == null) throw new IllegalArgumentException("Unreadable input: " + args[0]);
        boolean resizeOnly = "fit".equalsIgnoreCase(args[2]);
        int scaledWidth = WIDTH;
        int scaledHeight = HEIGHT;
        int x = 0;
        int y = 0;
        if (!resizeOnly) {
            double anchor = Math.max(0d, Math.min(1d, Double.parseDouble(args[2])));
            double scale = Math.max((double) WIDTH / source.getWidth(),
                (double) HEIGHT / source.getHeight());
            scaledWidth = (int) Math.ceil(source.getWidth() * scale);
            scaledHeight = (int) Math.ceil(source.getHeight() * scale);
            x = (WIDTH - scaledWidth) / 2;
            int overflowY = Math.max(0, scaledHeight - HEIGHT);
            y = -(int) Math.round(overflowY * anchor);
        }

        BufferedImage output = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, x, y, scaledWidth, scaledHeight, null);
        graphics.dispose();

        File destination = new File(args[1]);
        File parent = destination.getParentFile();
        if (parent != null) parent.mkdirs();
        ImageIO.write(output, "png", destination);
        System.out.println(destination + "  " + WIDTH + "x" + HEIGHT);
    }
}
