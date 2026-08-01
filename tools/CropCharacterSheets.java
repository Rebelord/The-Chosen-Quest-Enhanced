import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Deterministically splits the approved 4x4 counterpart sheets into game assets. */
public final class CropCharacterSheets {
    private static final String[] RACES = {"human", "dwarf", "elf", "halfling"};
    private static final String[] CLASSES = {"fighter", "mage", "rogue", "hunter"};

    private CropCharacterSheets() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                "Usage: CropCharacterSheets <full-body-sheet> <portrait-sheet> <asset-root>");
        }
        BufferedImage fullBody = ImageIO.read(new File(args[0]));
        BufferedImage portraits = ImageIO.read(new File(args[1]));
        File root = new File(args[2]);
        split(fullBody, new File(root, "full-body"), 768, 1024);
        split(portraits, new File(root, "portraits"), 256, 256);
    }

    private static void split(BufferedImage sheet, File output, int targetWidth,
                              int targetHeight) throws Exception {
        if (sheet == null) throw new IllegalArgumentException("Unreadable character sheet");
        output.mkdirs();
        int[] rows = gridBounds(sheet, true);
        int[] columns = gridBounds(sheet, false);
        for (int row = 0; row < 4; row++) {
            int top = rows[row];
            int bottom = rows[row + 1];
            for (int column = 0; column < 4; column++) {
                int left = columns[column];
                int right = columns[column + 1];
                int gutter = Math.max(2, Math.min(right - left, bottom - top) / 100);
                BufferedImage cell = sheet.getSubimage(
                    left + gutter, top + gutter,
                    Math.max(1, right - left - gutter * 2),
                    Math.max(1, bottom - top - gutter * 2));
                BufferedImage scaled = cover(cell, targetWidth, targetHeight);
                File file = new File(output,
                    RACES[row] + "-" + CLASSES[column] + ".png");
                if (!ImageIO.write(scaled, "png", file)) {
                    throw new IllegalStateException("No PNG writer available for " + file);
                }
                System.out.println(file.getAbsolutePath());
            }
        }
    }

    /**
     * Generated sheets sometimes vary cell height by a few percent. Find the
     * darkest full-width/full-height gutter near each expected quartile instead
     * of assuming mathematically equal cells.
     */
    private static int[] gridBounds(BufferedImage image, boolean horizontal) {
        int length = horizontal ? image.getHeight() : image.getWidth();
        int[] bounds = {0, 0, 0, 0, length};
        int searchRadius = Math.max(12, length / 10);
        for (int divider = 1; divider < 4; divider++) {
            int expected = divider * length / 4;
            int start = Math.max(1, expected - searchRadius);
            int end = Math.min(length - 2, expected + searchRadius);
            long darkest = Long.MAX_VALUE;
            int selected = expected;
            for (int position = start; position <= end; position++) {
                long score = lineBrightness(image, position, horizontal);
                if (score < darkest) {
                    darkest = score;
                    selected = position;
                }
            }
            bounds[divider] = selected;
        }
        return bounds;
    }

    private static long lineBrightness(BufferedImage image, int position,
                                       boolean horizontal) {
        int samples = horizontal ? image.getWidth() : image.getHeight();
        int stride = Math.max(1, samples / 300);
        long total = 0;
        for (int sample = 0; sample < samples; sample += stride) {
            int rgb = horizontal ? image.getRGB(sample, position)
                : image.getRGB(position, sample);
            total += ((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255);
        }
        return total;
    }

    private static BufferedImage cover(BufferedImage source, int width, int height) {
        double scale = Math.max((double) width / source.getWidth(),
            (double) height / source.getHeight());
        int scaledWidth = Math.max(width, (int) Math.round(source.getWidth() * scale));
        int scaledHeight = Math.max(height, (int) Math.round(source.getHeight() * scale));
        int x = (width - scaledWidth) / 2;
        int y = (height - scaledHeight) / 2;
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, x, y, scaledWidth, scaledHeight, null);
        graphics.dispose();
        return result;
    }
}
