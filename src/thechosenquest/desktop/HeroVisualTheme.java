package thechosenquest.desktop;

import java.awt.Color;

/**
 * Modular visual identity for the player's hero throughout the game.
 *
 * The Art Deco shell, layout, typography, and controls stay fixed. Class supplies
 * the stronger enamel/resource color, while race supplies a deliberately subtle
 * transparent motif and material tint. Creation, the hero rail, inventory, combat,
 * dialogue, and outcome screens consume the same inputs without becoming separate
 * race/class layouts.
 */
final class HeroVisualTheme {
    enum RaceMotif {
        CROWN_SUNBURST,
        MOUNTAIN_RUNES,
        LEAF_CRESCENT,
        WHEAT_ARCH
    }

    private final String race;
    private final String heroClass;
    private final Color classAccent;
    private final Color classDeep;
    private final Color raceAccent;
    private final Color metalAccent;
    private final Color resourceColor;
    private final RaceMotif raceMotif;

    private HeroVisualTheme(String race, String heroClass, Color classAccent,
                            Color classDeep, Color raceAccent, Color metalAccent,
                            Color resourceColor, RaceMotif raceMotif) {
        this.race = race;
        this.heroClass = heroClass;
        this.classAccent = classAccent;
        this.classDeep = classDeep;
        this.raceAccent = raceAccent;
        this.metalAccent = metalAccent;
        this.resourceColor = resourceColor;
        this.raceMotif = raceMotif;
    }

    static HeroVisualTheme forBuild(String race, String heroClass) {
        String safeRace = normalizeRace(race);
        String safeClass = normalizeClass(heroClass);

        Color classAccent;
        Color classDeep;
        Color resource;
        if ("Mage".equals(safeClass)) {
            classAccent = new Color(81, 151, 211);
            classDeep = new Color(28, 55, 84);
            resource = UiTheme.BLUE;
        } else if ("Rogue".equals(safeClass)) {
            classAccent = new Color(157, 103, 191);
            classDeep = new Color(60, 35, 72);
            resource = new Color(139, 92, 183);
        } else if ("Hunter".equals(safeClass)) {
            classAccent = new Color(88, 159, 91);
            classDeep = new Color(31, 65, 39);
            resource = new Color(91, 164, 91);
        } else {
            classAccent = new Color(194, 67, 44);
            classDeep = new Color(79, 35, 27);
            resource = new Color(194, 67, 44);
        }

        Color raceAccent;
        Color metal;
        RaceMotif motif;
        if ("Dwarf".equals(safeRace)) {
            raceAccent = new Color(181, 126, 66);
            metal = new Color(171, 139, 92);
            motif = RaceMotif.MOUNTAIN_RUNES;
        } else if ("Elf".equals(safeRace)) {
            raceAccent = new Color(123, 173, 137);
            metal = new Color(190, 197, 178);
            motif = RaceMotif.LEAF_CRESCENT;
        } else if ("Halfling".equals(safeRace)) {
            raceAccent = new Color(201, 158, 79);
            metal = new Color(184, 126, 79);
            motif = RaceMotif.WHEAT_ARCH;
        } else {
            raceAccent = new Color(197, 190, 168);
            metal = new Color(196, 158, 86);
            motif = RaceMotif.CROWN_SUNBURST;
        }

        return new HeroVisualTheme(safeRace, safeClass, classAccent, classDeep,
            raceAccent, metal, resource, motif);
    }

    String race() { return race; }

    String heroClass() { return heroClass; }

    Color classAccent() { return classAccent; }

    Color classDeep() { return classDeep; }

    Color raceAccent() { return raceAccent; }

    Color metalAccent() { return metalAccent; }

    Color resourceColor() { return resourceColor; }

    RaceMotif raceMotif() { return raceMotif; }

    /** Transparent overlay colors are composited over the fixed shared shell. */
    Color classOverlay(int alpha) {
        return withAlpha(classAccent, alpha);
    }

    Color raceOverlay(int alpha) {
        return withAlpha(raceAccent, alpha);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
            Math.max(0, Math.min(255, alpha)));
    }

    private static String normalizeRace(String race) {
        if ("Dwarf".equals(race) || "Elf".equals(race) ||
                "Halfling".equals(race)) return race;
        return "Human";
    }

    private static String normalizeClass(String heroClass) {
        if ("Mage".equals(heroClass) || "Rogue".equals(heroClass) ||
                "Hunter".equals(heroClass)) return heroClass;
        return "Fighter";
    }
}
