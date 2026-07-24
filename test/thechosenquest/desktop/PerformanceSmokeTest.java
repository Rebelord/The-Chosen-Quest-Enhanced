package thechosenquest.desktop;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;

/** Lightweight repeatable profiling for the UI paths most visible to testers. */
public final class PerformanceSmokeTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File report = new File(args.length == 0
            ? "build/reports/performance-report.md" : args[0]);
        report.getParentFile().mkdirs();

        GameEngine preview = new GameEngine();
        long previewStart = System.nanoTime();
        for (int pass = 0; pass < 250; pass++) {
            for (String race : GameEngine.RACES) {
                for (String heroClass : GameEngine.CLASSES) {
                    preview.configureHeroPreview(race, heroClass,
                        GameEngine.defaultStarterKit(heroClass));
                }
            }
        }
        long previewMs = elapsedMs(previewStart);

        String portrait = "/assets/avatars/full-body/halfling-hunter.png";
        long firstImageStart = System.nanoTime();
        AssetImagePanel first = new AssetImagePanel(portrait, false);
        first.setSize(270, 360);
        paint(first);
        long firstImageMs = elapsedMs(firstImageStart);
        long cachedImageStart = System.nanoTime();
        AssetImagePanel cached = new AssetImagePanel(portrait, false);
        cached.setSize(270, 360);
        paint(cached);
        long cachedImageMs = elapsedMs(cachedImageStart);

        long audioStart = System.nanoTime();
        SoundManager sound = new SoundManager();
        long audioMs = elapsedMs(audioStart);
        int cues = sound.cachedCueCount();
        int authoredCues = sound.fileBackedCueCount();
        int ambiences = sound.cachedAmbienceCount();
        sound.shutdown();

        File save = File.createTempFile("chosen-quest-profile", ".save");
        GameEngine persistence = new GameEngine();
        persistence.newGame("Profile", "Elf", "Hunter");
        long saveStart = System.nanoTime();
        for (int index = 0; index < 20; index++) persistence.save(save);
        long saveMs = elapsedMs(saveStart);
        long loadStart = System.nanoTime();
        for (int index = 0; index < 20; index++) persistence.load(save);
        long loadMs = elapsedMs(loadStart);
        save.delete();

        try (PrintWriter writer = new PrintWriter(report, "UTF-8")) {
            writer.println("# Performance Smoke Report");
            writer.println();
            writer.println("Repeatable development-machine measurements; use trends, not these " +
                "absolute values, when evaluating other hardware.");
            writer.println();
            writer.println("| Hot path | Work | Time |");
            writer.println("|---|---:|---:|");
            writer.println("| Character preview rules | 4,000 build changes | " + previewMs + " ms |");
            writer.println("| First portrait decode + render | 1 image | " + firstImageMs + " ms |");
            writer.println("| Cached portrait render | 1 image | " + cachedImageMs + " ms |");
            writer.println("| Audio sample initialization | " + cues + " cues, " + ambiences +
                " ambience loops (" + authoredCues + " authored cues) | " + audioMs + " ms |");
            writer.println("| Save serialization | 20 saves | " + saveMs + " ms |");
            writer.println("| Save migration/load | 20 loads | " + loadMs + " ms |");
            writer.println();
            writer.println("## Implemented safeguards");
            writer.println();
            writer.println("- Hero preview rules do not generate a world.");
            writer.println("- Large avatar renders warm in the background while the title is visible.");
            writer.println("- Canceled image requests are purged and only the latest artwork is applied.");
            writer.println("- ImageIO temporary-disk caching is disabled for bundled resources.");
            writer.println("- Combat VFX and audio samples are decoded before their first gameplay use.");
        }
        if (previewMs > 3000L || cachedImageMs > 1000L || saveMs > 3000L || loadMs > 3000L) {
            throw new AssertionError("A profiled hot path exceeded its generous regression ceiling");
        }
        System.out.println("Performance smoke test passed: " + report.getAbsolutePath());
    }

    private static void paint(AssetImagePanel panel) {
        BufferedImage image = new BufferedImage(270, 360, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
    }

    private static long elapsedMs(long started) {
        return Math.max(0L, (System.nanoTime() - started) / 1000000L);
    }
}
