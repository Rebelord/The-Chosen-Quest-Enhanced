import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Splits a 2x2 encounter-art sheet into four named square PNG assets. */
public final class CropEncounterSheet {
    private static final int GRID_SIZE = 2;
    private static final int INSET = 4;
    private static final int OUTPUT_SIZE = 1024;

    private CropEncounterSheet() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            throw new IllegalArgumentException(
                "Usage: CropEncounterSheet <sheet.png> <output-directory> <top-left> <top-right> <bottom-left> <bottom-right>"
            );
        }

        BufferedImage sheet = ImageIO.read(new File(args[0]));
        if (sheet == null) {
            throw new IllegalArgumentException("Unable to read image: " + args[0]);
        }
        if (sheet.getWidth() != sheet.getHeight() || sheet.getWidth() % GRID_SIZE != 0) {
            throw new IllegalArgumentException("Sheet must be square and divide evenly into a 2x2 grid");
        }

        File outputDirectory = new File(args[1]);
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create output directory: " + outputDirectory);
        }

        int cellSize = sheet.getWidth() / GRID_SIZE;
        int cropSize = cellSize - (INSET * 2);
        String[] names = {args[2], args[3], args[4], args[5]};

        for (int index = 0; index < names.length; index++) {
            int column = index % GRID_SIZE;
            int row = index / GRID_SIZE;
            int x = column * cellSize + INSET;
            int y = row * cellSize + INSET;
            BufferedImage crop = sheet.getSubimage(x, y, cropSize, cropSize);
            BufferedImage output = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(crop, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, null);
            graphics.dispose();

            File destination = new File(outputDirectory, names[index] + ".png");
            ImageIO.write(output, "png", destination);
            System.out.println(destination.getPath());
        }
    }
}
