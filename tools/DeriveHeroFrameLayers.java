import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import javax.imageio.ImageIO;

/**
 * Derives native-size semantic layers from the approved hero-frame artwork.
 *
 * No authored feature is located by a coordinate. Instead, the source pixels
 * define enclosed enamel and gemstone regions. Each color underlay is expanded
 * a few pixels beneath the opaque brass so scaling cannot expose a hairline gap;
 * the original frame remains the final visible boundary.
 */
public final class DeriveHeroFrameLayers {
    private static final float ENAMEL_REVEAL = .42f;
    private static final float GEM_REVEAL = .82f;
    private static final int UNDERLAP_PIXELS = 4;

    private DeriveHeroFrameLayers() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 4 && args.length != 6) {
            throw new IllegalArgumentException(
                "Usage: DeriveHeroFrameLayers <source> <overlay-out> " +
                "<enamel-underlay-out> <gem-underlay-out> " +
                "[<enamel-visible-mask-out> <gem-visible-mask-out>]");
        }
        BufferedImage source = ImageIO.read(new File(args[0]));
        if (source == null) throw new IllegalArgumentException("Unreadable source");

        int width = source.getWidth();
        int height = source.getHeight();
        boolean[] enamelInterior = classifyEnamel(source);
        boolean[] gemInterior = classifyGemComponents(source, enamelInterior);
        boolean[] enamelUnderlay = dilateInsideArtwork(
            source, enamelInterior, UNDERLAP_PIXELS);
        boolean[] gemUnderlay = dilateInsideArtwork(
            source, gemInterior, UNDERLAP_PIXELS);

        BufferedImage overlay = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        BufferedImage enamelMask = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        BufferedImage gemMask = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 255;
                float reveal = gemInterior[index] ? GEM_REVEAL
                    : enamelInterior[index] ? ENAMEL_REVEAL : 0f;
                int overlayAlpha = Math.round(alpha * (1f - reveal));
                overlay.setRGB(x, y,
                    (overlayAlpha << 24) | (argb & 0x00ffffff));
                if (enamelUnderlay[index]) {
                    enamelMask.setRGB(x, y, 0xffffffff);
                }
                if (gemUnderlay[index]) {
                    gemMask.setRGB(x, y, 0xffffffff);
                }
            }
        }

        write(overlay, args[1]);
        write(enamelMask, args[2]);
        write(gemMask, args[3]);
        if (args.length == 6) {
            write(asMaskImage(enamelInterior, width, height), args[4]);
            write(asMaskImage(gemInterior, width, height), args[5]);
        }
        System.out.println("Derived " + width + "x" + height +
            " frame layers from artwork: enamel=" + count(enamelInterior) +
            " px, gems=" + count(gemInterior) + " px");
    }

    private static boolean[] classifyEnamel(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        boolean[] result = new boolean[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 255;
                int red = (argb >>> 16) & 255;
                int green = (argb >>> 8) & 255;
                int blue = argb & 255;
                // The authored enamel is cool green. Brass remains warm even in
                // shadow, so channel relationships follow the painted material.
                result[y * width + x] = alpha >= 80 && green >= 20 &&
                    green > red * 1.08f && green > blue * 1.02f &&
                    red < 125 && blue < 120;
            }
        }
        return retainSubstantialComponents(result, width, height,
            70, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private static boolean[] classifyGemComponents(BufferedImage source,
                                                    boolean[] enamel) {
        int width = source.getWidth();
        int height = source.getHeight();
        boolean[] candidates = new boolean[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (enamel[index]) continue;
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 255;
                int red = (argb >>> 16) & 255;
                int green = (argb >>> 8) & 255;
                int blue = argb & 255;
                int luminance = (red * 3 + green * 5 + blue * 2) / 10;
                // Gem glass is the cool, near-black material enclosed by brass.
                candidates[index] = alpha >= 110 && luminance <= 69 &&
                    red <= 82 && green <= 91 && blue <= 96 &&
                    blue >= red * .72f;
            }
        }
        // Five compact, substantial dark-glass regions are enclosed by the
        // frame's authored brass sockets. Narrow rail shadows and crown seams
        // are rejected by area and aspect ratio, without locating any socket.
        return retainSubstantialComponents(candidates, width, height,
            400, 80, 80);
    }

    /**
     * Retains semantic regions by their connected painted area, never by screen
     * position. Long rail shadows fail the bounds test; enclosed gemstone
     * interiors remain compact.
     */
    private static boolean[] retainSubstantialComponents(boolean[] candidates,
                                                          int width, int height,
                                                          int minimumArea,
                                                          int maximumWidth,
                                                          int maximumHeight) {
        boolean[] result = new boolean[candidates.length];
        boolean[] visited = new boolean[candidates.length];
        for (int start = 0; start < candidates.length; start++) {
            if (!candidates[start] || visited[start]) continue;
            List<Integer> component = new ArrayList<Integer>();
            Queue<Integer> queue = new ArrayDeque<Integer>();
            queue.add(Integer.valueOf(start));
            int minX = width;
            int minY = height;
            int maxX = 0;
            int maxY = 0;
            while (!queue.isEmpty()) {
                int index = queue.remove().intValue();
                if (index < 0 || index >= candidates.length ||
                        visited[index] || !candidates[index]) continue;
                visited[index] = true;
                component.add(Integer.valueOf(index));
                int x = index % width;
                int y = index / width;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                if (x > 0) queue.add(Integer.valueOf(index - 1));
                if (x + 1 < width) queue.add(Integer.valueOf(index + 1));
                if (y > 0) queue.add(Integer.valueOf(index - width));
                if (y + 1 < height) queue.add(Integer.valueOf(index + width));
            }
            int componentWidth = maxX - minX + 1;
            int componentHeight = maxY - minY + 1;
            float aspect = componentWidth / (float) componentHeight;
            if (component.size() >= minimumArea &&
                    componentWidth <= maximumWidth &&
                    componentHeight <= maximumHeight &&
                    (maximumWidth == Integer.MAX_VALUE ||
                        (aspect >= .55f && aspect <= 1.82f))) {
                for (Integer index : component) result[index.intValue()] = true;
            }
        }
        return result;
    }

    private static boolean[] dilateInsideArtwork(BufferedImage source,
                                                  boolean[] interior,
                                                  int radius) {
        int width = source.getWidth();
        int height = source.getHeight();
        boolean[] result = interior.clone();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!interior[y * width + x]) continue;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        if (dx * dx + dy * dy > radius * radius) continue;
                        int targetX = x + dx;
                        int targetY = y + dy;
                        if (targetX < 0 || targetX >= width ||
                                targetY < 0 || targetY >= height) continue;
                        int alpha = (source.getRGB(targetX, targetY) >>> 24) & 255;
                        if (alpha > 0) result[targetY * width + targetX] = true;
                    }
                }
            }
        }
        return result;
    }

    private static int count(boolean[] pixels) {
        int count = 0;
        for (boolean pixel : pixels) if (pixel) count++;
        return count;
    }

    private static BufferedImage asMaskImage(boolean[] pixels,
                                             int width, int height) {
        BufferedImage image = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        for (int index = 0; index < pixels.length; index++) {
            if (pixels[index]) {
                image.setRGB(index % width, index / width, 0xffffffff);
            }
        }
        return image;
    }

    private static void write(BufferedImage image, String path) throws Exception {
        if (!ImageIO.write(image, "png", new File(path))) {
            throw new IllegalStateException("PNG writer is unavailable");
        }
    }
}
