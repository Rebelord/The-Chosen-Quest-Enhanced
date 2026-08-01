package thechosenquest.desktop;

/** Resolves cosmetic hero artwork without coupling gender presentation to gameplay. */
final class CharacterArt {
    static final String MALE = "Male";
    static final String FEMALE = "Female";

    private CharacterArt() { }

    static String normalizeGender(String gender, String race, String heroClass) {
        if (MALE.equals(gender) || FEMALE.equals(gender)) return gender;
        return defaultGender(race, heroClass);
    }

    /**
     * Existing art remains the canonical default for each authored build. The
     * counterpart sheet supplies only the missing gender, avoiding duplicate assets.
     */
    static String defaultGender(String race, String heroClass) {
        if ("Human".equals(race)) {
            return ("Fighter".equals(heroClass) || "Rogue".equals(heroClass))
                ? FEMALE : MALE;
        }
        if ("Dwarf".equals(race)) {
            return "Rogue".equals(heroClass) ? FEMALE : MALE;
        }
        if ("Elf".equals(race)) {
            return "Rogue".equals(heroClass) ? MALE : FEMALE;
        }
        if ("Halfling".equals(race)) {
            return ("Fighter".equals(heroClass) || "Rogue".equals(heroClass))
                ? MALE : FEMALE;
        }
        return MALE;
    }

    static String portrait(String race, String heroClass, String gender) {
        return resource(race, heroClass, gender, false);
    }

    static String fullBody(String race, String heroClass, String gender) {
        return resource(race, heroClass, gender, true);
    }

    static String mapMarker(String race, String heroClass, String gender) {
        String build = slug(race) + "-" + slug(heroClass) + ".png";
        if (usesCounterpart(race, heroClass, gender)) {
            // Counterpart portraits are deliberately composed for small circular crops.
            return "/assets/avatars/counterpart/portraits/" + build;
        }
        return "/assets/map/markers/" + build;
    }

    static boolean usesCounterpart(String race, String heroClass, String gender) {
        return !defaultGender(race, heroClass).equals(
            normalizeGender(gender, race, heroClass));
    }

    private static String resource(String race, String heroClass, String gender,
                                   boolean fullBody) {
        String build = slug(race) + "-" + slug(heroClass) + ".png";
        if (usesCounterpart(race, heroClass, gender)) {
            return "/assets/avatars/counterpart/" +
                (fullBody ? "full-body/" : "portraits/") + build;
        }
        return "/assets/avatars/" + (fullBody ? "full-body/" : "") + build;
    }

    private static String slug(String value) {
        return value == null ? "human" : value.toLowerCase();
    }
}
