package thechosenquest.desktop;

import java.util.Random;

/**
 * Small offline name generator for heroes and future NPCs. Race controls the
 * phonetic character of the given name, while class quietly influences the
 * surname. Curated fragments keep output pronounceable without network calls
 * or a third-party runtime dependency.
 */
final class FantasyNameGenerator {
    private static final Random RANDOM = new Random();

    private static final String[] HUMAN_NAMES = {
        "Alden", "Aria", "Brenna", "Caelan", "Corin", "Elara", "Garrick", "Helena",
        "Ilyra", "Joren", "Kaia", "Lucan", "Mara", "Nerys", "Oren", "Rowan",
        "Seren", "Talia", "Valen", "Willa"
    };
    private static final String[] DWARF_NAMES = {
        "Baerdin", "Borin", "Brumir", "Dagna", "Dorgrin", "Eira", "Fargrim", "Gimra",
        "Hilda", "Kelda", "Korrin", "Magda", "Nori", "Ordrin", "Ragna", "Sigrun",
        "Thargrim", "Torra", "Ulfgar", "Vigra"
    };
    private static final String[] ELF_NAMES = {
        "Aelindra", "Aerion", "Caelwen", "Elaria", "Elowen", "Faelar", "Ilyrana",
        "Lethariel", "Liora", "Maelis", "Naivara", "Orist", "Saelith", "Sylvara",
        "Taelion", "Thalia", "Vaelora", "Varis", "Yllara", "Zephriel"
    };
    private static final String[] HALFLING_NAMES = {
        "Bram", "Cora", "Della", "Eldon", "Elsie", "Fenna", "Hob", "Lila", "Merric",
        "Milo", "Nella", "Perrin", "Pippa", "Rosie", "Samkin", "Tobin", "Tolly",
        "Wenna", "Wicket", "Willow"
    };

    private static final String[] FIGHTER_SURNAMES = {
        "Brightsteel", "Dawnblade", "Ironward", "Lionheart", "Oathkeeper",
        "Redcrest", "Stoneguard", "Strongshield"
    };
    private static final String[] MAGE_SURNAMES = {
        "Ashenveil", "Dreamscribe", "Emberglow", "Mistwalker", "Mooncipher",
        "Runeweaver", "Spellward", "Starwhisper"
    };
    private static final String[] ROGUE_SURNAMES = {
        "Duskmantle", "Greyfox", "Nightshade", "Quickstep", "Shadowfen",
        "Silentbrook", "Softfoot", "Whispercloak"
    };
    private static final String[] HUNTER_SURNAMES = {
        "Ashvale", "Farstride", "Greenbough", "Keeneye", "Oaktrail", "Stormfeather",
        "Thornwood", "Wolfsong"
    };

    private FantasyNameGenerator() {
    }

    static String generate(String race, String heroClass, String currentName) {
        synchronized (RANDOM) {
            String generated = null;
            for (int attempt = 0; attempt < 8; attempt++) {
                generated = generate(race, heroClass, RANDOM);
                if (!generated.equalsIgnoreCase(clean(currentName))) return generated;
            }
            return generated;
        }
    }

    static String generate(String race, String heroClass, Random random) {
        Random source = random == null ? RANDOM : random;
        String[] firstNames = firstNamesFor(race);
        String[] surnames = surnamesFor(heroClass);
        return firstNames[source.nextInt(firstNames.length)] + " " +
            surnames[source.nextInt(surnames.length)];
    }

    private static String[] firstNamesFor(String race) {
        if ("Dwarf".equals(race)) return DWARF_NAMES;
        if ("Elf".equals(race)) return ELF_NAMES;
        if ("Halfling".equals(race)) return HALFLING_NAMES;
        return HUMAN_NAMES;
    }

    private static String[] surnamesFor(String heroClass) {
        if ("Mage".equals(heroClass)) return MAGE_SURNAMES;
        if ("Rogue".equals(heroClass)) return ROGUE_SURNAMES;
        if ("Hunter".equals(heroClass)) return HUNTER_SURNAMES;
        return FIGHTER_SURNAMES;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
