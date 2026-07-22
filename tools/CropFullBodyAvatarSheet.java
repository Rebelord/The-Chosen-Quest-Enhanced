import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Splits the 4x4 full-body avatar sheet into named, 3:4 PNG assets. */
public final class CropFullBodyAvatarSheet {
    private static final String[] RACES = {"human", "dwarf", "elf", "halfling"};
    private static final String[] CLASSES = {"fighter", "mage", "rogue", "hunter"};

    private static final int COLUMNS = 4;
    private static final int ROWS = 4;
    private static final int INSET_X = 4;
    private static final int INSET_Y = 6;
    private static final int OUTPUT_WIDTH = 768;
    private static final int OUTPUT_HEIGHT = 1024;

    private CropFullBodyAvatarSheet() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: CropFullBodyAvatarSheet <sheet.png> <output-directory>");
        }

        BufferedImage sheet = ImageIO.read(new File(args[0]));
        if (sheet == null) {
            throw new IllegalArgumentException("Unable to read image: " + args[0]);
        }
        if (sheet.getWidth() % COLUMNS != 0 || sheet.getHeight() % ROWS != 0) {
            throw new IllegalArgumentException("Sheet dimensions must divide evenly into a 4x4 grid");
        }

        int cellWidth = sheet.getWidth() / COLUMNS;
        int cellHeight = sheet.getHeight() / ROWS;
        int cropWidth = cellWidth - (INSET_X * 2);
        int cropHeight = cellHeight - (INSET_Y * 2);
        if (cropWidth * 4 != cropHeight * 3) {
            throw new IllegalArgumentException("Trimmed cells must have a 3:4 aspect ratio");
        }

        File outputDirectory = new File(args[1]);
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create output directory: " + outputDirectory);
        }

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int x = column * cellWidth + INSET_X;
                int y = row * cellHeight + INSET_Y;
                BufferedImage crop = sheet.getSubimage(x, y, cropWidth, cropHeight);
                BufferedImage output = new BufferedImage(OUTPUT_WIDTH, OUTPUT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = output.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(crop, 0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT, null);
                graphics.dispose();

                File destination = new File(outputDirectory, RACES[row] + "-" + CLASSES[column] + ".png");
                ImageIO.write(output, "png", destination);
                System.out.println(destination.getPath());
            }
        }
    }
}
