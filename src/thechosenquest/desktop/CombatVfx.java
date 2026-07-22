package thechosenquest.desktop;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Loads the temporary CC0 combat-effect frames into a small, strong cache.
 *
 * The renderer asks this class for already-decoded images during repainting;
 * disk or JAR reads must never happen inside the animation loop. Keeping the
 * mapping in one place also makes the prototype art straightforward to swap
 * for the project's final custom sprite sheets later.
 */
final class CombatVfx {
    enum Sequence {
        FIGHTER("fighter-gold/Classic_%02d.png", 25, 6),
        MAGE("mage-blue/Alternative_2_%02d.png", 1, 6),
        ROGUE("rogue-purple/Alternative_1_%02d.png", 25, 6),
        HUNTER("hunter-gold/Classic_%02d.png", 19, 6),
        SPELL("magic-missile-arcane/Arcane_Effect_%d.png", 1, 7),
        FIREBALL("fireball/Effects_Fire_0_%02d.png", 1, 28),
        ICE_SPIKE("ice-spike-water/Water__%02d.png", 1, 5),
        ENEMY("enemy-fire/Alternative_3_%02d.png", 1, 6),
        CRITICAL("critical-gold/Classic_%02d.png", 1, 6),
        DEFEND("defend-blue/AngelShieldEffect_%02d.png", 11, 5),
        BOSS("boss-wrath/Fire-Wrath__%02d.png", 16, 5);

        final String pathPattern;
        final int firstFrame;
        final int frameCount;

        Sequence(String pathPattern, int firstFrame, int frameCount) {
            this.pathPattern = pathPattern;
            this.firstFrame = firstFrame;
            this.frameCount = frameCount;
        }
    }

    private static final Map<Sequence, BufferedImage[]> CACHE =
        new EnumMap<Sequence, BufferedImage[]>(Sequence.class);
    private static boolean preloadStarted;

    private CombatVfx() { }

    /** Decode every tiny sequence before combat begins to prevent first-hit stutter. */
    static synchronized void preload() {
        for (Sequence sequence : Sequence.values()) frames(sequence);
    }

    /** Warm combat art without blocking Swing's event-dispatch thread. */
    static synchronized void preloadAsync() {
        if (preloadStarted) return;
        preloadStarted = true;
        Thread loader = new Thread(new Runnable() {
            public void run() {
                preload();
            }
        }, "chosen-quest-vfx-loader");
        loader.setDaemon(true);
        loader.start();
    }

    static synchronized BufferedImage frame(Sequence sequence, double normalizedProgress) {
        BufferedImage[] images = frames(sequence);
        if (images.length == 0) return null;
        double clamped = Math.max(0d, Math.min(.999999d, normalizedProgress));
        return images[(int) Math.floor(clamped * images.length)];
    }

    static synchronized boolean prototypeAssetsAvailable() {
        for (Sequence sequence : Sequence.values()) {
            if (frames(sequence).length != sequence.frameCount) return false;
        }
        return true;
    }

    private static BufferedImage[] frames(Sequence sequence) {
        BufferedImage[] cached = CACHE.get(sequence);
        if (cached != null) return cached;

        BufferedImage[] loaded = new BufferedImage[sequence.frameCount];
        for (int index = 0; index < sequence.frameCount; index++) {
            String relativePath = String.format(Locale.ROOT, sequence.pathPattern,
                sequence.firstFrame + index);
            String resource = "/assets/vfx/prototype/" + relativePath;
            try {
                loaded[index] = ImageIO.read(CombatVfx.class.getResource(resource));
            } catch (Exception ignored) {
                // An empty sequence activates EncounterPanel's procedural fallback.
                CACHE.put(sequence, new BufferedImage[0]);
                return CACHE.get(sequence);
            }
            if (loaded[index] == null) {
                CACHE.put(sequence, new BufferedImage[0]);
                return CACHE.get(sequence);
            }
        }
        CACHE.put(sequence, loaded);
        return loaded;
    }
}
