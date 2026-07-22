import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Splits the 4x4 Chosen Quest avatar sheet into clean 256px PNG assets. */
public final class CropAvatarSheet {
    private static final String[] RACES = {"human", "dwarf", "elf", "halfling"};
    private static final String[] CLASSES = {"fighter", "mage", "rogue", "hunter"};

    private CropAvatarSheet() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: CropAvatarSheet <sheet.png> <output-directory>");
        }

        BufferedImage sheet = ImageIO.read(new File(args[0]));
        if (sheet == null || sheet.getWidth() != 1024 || sheet.getHeight() != 1024) {
            throw new IllegalArgumentException("Expected a readable 1024x1024 PNG avatar sheet");
        }

        File outputDirectory = new File(args[1]);
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create " + outputDirectory);
        }

        for (int row = 0; row < RACES.length; row++) {
            for (int column = 0; column < CLASSES.length; column++) {
                boolean tighterRogueCrop = column == 2 && (row == 1 || row == 3);
                int insetX = tighterRogueCrop ? 24 : 3;
                int insetY = tighterRogueCrop ? 4 : 3;
                int cropSize = tighterRogueCrop ? 208 : 250;
                int sourceX = column * 256 + insetX;
                int sourceY = row * 256 + insetY;

                BufferedImage avatar = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = avatar.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(sheet,
                    0, 0, 256, 256,
                    sourceX, sourceY, sourceX + cropSize, sourceY + cropSize,
                    null);
                graphics.dispose();

                File output = new File(outputDirectory,
                    RACES[row] + "-" + CLASSES[column] + ".png");
                ImageIO.write(avatar, "png", output);
            }
        }
    }
}
