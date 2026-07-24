package thechosenquest.desktop;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/** Presentation metadata for the encounter component variants defined in Figma. */
final class EncounterCatalog {
    enum Tier {
        STANDARD("COMMON"), ELITE("ELITE"), BOSS("LEGENDARY");

        final String label;

        Tier(String label) {
            this.label = label;
        }
    }

    static final class Profile {
        final String id;
        final String displayName;
        final String subtitle;
        final Tier tier;
        final String artwork;
        final int defense;
        final int level;
        final String tags;
        final String tactic;
        final Color accent;
        final boolean mirrorArtwork;

        Profile(String id, String displayName, String subtitle, Tier tier, String artwork,
                int defense, int level, String tags, String tactic, Color accent,
                boolean mirrorArtwork) {
            this.id = id;
            this.displayName = displayName;
            this.subtitle = subtitle;
            this.tier = tier;
            this.artwork = artwork;
            this.defense = defense;
            this.level = level;
            this.tags = tags;
            this.tactic = tactic;
            this.accent = accent;
            this.mirrorArtwork = mirrorArtwork;
        }
    }

    private static final Map<String, Profile> PROFILES = createProfiles();
    private static final Profile FALLBACK = PROFILES.get("Bandit Marauder");

    private EncounterCatalog() {
    }

    static Profile forEnemy(String name) {
        Profile profile = PROFILES.get(name);
        return profile == null ? FALLBACK : profile;
    }

    static Profile[] allProfiles() {
        return new Profile[] {
            PROFILES.get("Bandit Marauder"), PROFILES.get("Skeletal Guardian"),
            PROFILES.get("Cave Troll"), PROFILES.get("Cultist Mage"),
            PROFILES.get("Dire Wolf"), PROFILES.get("Giant Forest Spider"),
            PROFILES.get("Swamp Serpent"), PROFILES.get("Armored Boar"),
            PROFILES.get("Orc Warlord"), PROFILES.get("Necromancer"),
            PROFILES.get("Dark Elf Assassin"), PROFILES.get("Fallen Knight"),
            PROFILES.get("Ancient Red Dragon"), PROFILES.get("Ancient Frost Dragon"),
            PROFILES.get("Corrupted Shadow Dragon"), PROFILES.get("Skeletal Undead Dragon")
        };
    }

    private static Map<String, Profile> createProfiles() {
        Map<String, Profile> profiles = new HashMap<String, Profile>();
        add(profiles, standard("bandit-marauder", "Bandit Marauder", "COMMON HUMANOID",
            "/assets/encounters/enemies/bandit-marauder.png", 3, 15,
            "HUMANOID   MELEE", "AMBUSHER · WATCH FOR QUICK STRIKES"), "Hobgoblin");
        add(profiles, standard("skeletal-guardian", "Skeletal Guardian", "COMMON UNDEAD",
            "/assets/encounters/enemies/skeletal-guardian.png", 5, 8,
            "UNDEAD   ARMORED", "STEADY DEFENSE · VULNERABLE TO HOLY DAMAGE"), "Skeleton");
        add(profiles, standard("cave-troll", "Cave Troll", "COMMON BRUTE",
            "/assets/encounters/enemies/cave-troll.png", 4, 12,
            "GIANT   REGENERATING", "BREAK ITS GUARD BEFORE IT RECOVERS"), "Troll");
        add(profiles, standard("cultist-mage", "Cultist Mage", "DANGEROUS CASTER",
            "/assets/encounters/enemies/cultist-mage.png", 3, 15,
            "HUMANOID   ARCANE", "DARK RITUAL · INTERRUPT THE INCANTATION"));
        add(profiles, standard("dire-wolf", "Dire Wolf", "COMMON BEAST",
            "/assets/encounters/creatures/dire-wolf.png", 2, 6,
            "BEAST   SWIFT", "PACK HUNTER · DEFEND AGAINST ITS LUNGE"));
        add(profiles, standard("giant-forest-spider", "Giant Forest Spider", "VENOMOUS BEAST",
            "/assets/encounters/creatures/giant-forest-spider.png", 2, 7,
            "BEAST   VENOM", "WEBBING · KEEP MOVING"), "Giant Spider");
        add(profiles, elite("ashweb-matriarch", "Ashweb Matriarch",
            "ELITE NEST MATRIARCH · FINAL BROOD",
            "/assets/encounters/creatures/giant-forest-spider.png", 4, 2,
            "BEAST   VENOM   WEB QUEEN",
            "QUICK LUNGE · END THE BROOD WITHOUT LOSING TEMPO"));
        add(profiles, standard("swamp-serpent", "Swamp Serpent", "MARSH PREDATOR",
            "/assets/encounters/creatures/swamp-serpent.png", 3, 9,
            "BEAST   POISON", "COILED STRIKE · ANTICIPATE THE BITE"));
        add(profiles, standard("armored-boar", "Armored Boar", "ARMORED BEAST",
            "/assets/encounters/creatures/armored-boar.png", 6, 10,
            "BEAST   ARMORED", "CHARGING · SIDESTEP BEFORE ATTACKING"));

        add(profiles, elite("orc-warlord", "Orc Warlord", "ELITE WARLORD · HEAVY PLATE",
            "/assets/encounters/elites/orc-warlord.png", 8, 22,
            "ENRAGE 30%   POISON STRIKE   LETHAL WARD",
            "ENRAGED · BREAK ITS WARD BEFORE ATTACKING"), "Minotaur");
        add(profiles, elite("necromancer", "Necromancer", "ELITE DEATH MAGE",
            "/assets/encounters/elites/necromancer.png", 6, 24,
            "SOUL DRAIN   SUMMON   DEATH WARD", "DISRUPT THE RITUAL CIRCLE"), "Lich");
        add(profiles, elite("dark-elf-assassin", "Dark Elf Assassin", "ELITE SHADOWBLADE",
            "/assets/encounters/elites/dark-elf-assassin.png", 7, 20,
            "STEALTH   VENOM   RIPOSTE", "REVEAL HER BEFORE COMMITTING TO A STRIKE"), "Mimic");
        add(profiles, elite("fallen-knight", "Fallen Knight", "ELITE OATHBREAKER",
            "/assets/encounters/elites/fallen-knight.png", 9, 25,
            "CURSED BLADE   BULWARK   RETRIBUTION", "SHATTER THE BULWARK"));

        add(profiles, boss("ancient-red-dragon", "Ancient Red Dragon", "LEGENDARY FIRE DRAGON · PHASE 1",
            "/assets/encounters/dragons/ancient-red-dragon.png", 13, 40,
            "INFERNO   WING BUFFET   AWAKENED", "THE AIR IGNITES · SEEK COVER"), "Dragon");
        add(profiles, boss("ancient-frost-dragon", "Ancient Frost Dragon", "LEGENDARY FROST BOSS · PHASE 1",
            "/assets/encounters/dragons/ancient-frost-dragon.png", 14, 42,
            "FROST AURA   ICE ARMOR   AWAKENED", "BLIZZARD FORMING · SEEK SHELTER"));
        add(profiles, boss("corrupted-shadow-dragon", "Corrupted Shadow Dragon", "LEGENDARY VOID DRAGON",
            "/assets/encounters/dragons/corrupted-shadow-dragon.png", 15, 44,
            "VOID BREATH   CORRUPTION   ECLIPSE", "SHADOWS GATHER · HOLD YOUR WARD"));
        add(profiles, boss("skeletal-undead-dragon", "Skeletal Undead Dragon", "LEGENDARY UNDEAD DRAGON",
            "/assets/encounters/dragons/skeletal-undead-dragon.png", 16, 46,
            "SOULFIRE   BONE STORM   UNDYING", "BREAK THE SOUL ANCHOR"));
        return profiles;
    }

    private static Profile standard(String id, String name, String subtitle, String artwork,
                                    int defense, int level, String tags, String tactic) {
        return new Profile(id, name, subtitle, Tier.STANDARD, artwork, defense, level,
            tags, tactic, UiTheme.GOLD, false);
    }

    private static Profile elite(String id, String name, String subtitle, String artwork,
                                 int defense, int level, String tags, String tactic) {
        return new Profile(id, name, subtitle, Tier.ELITE, artwork, defense, level,
            tags, tactic, UiTheme.ELITE, false);
    }

    private static Profile boss(String id, String name, String subtitle, String artwork,
                                int defense, int level, String tags, String tactic) {
        return new Profile(id, name, subtitle, Tier.BOSS, artwork, defense, level,
            tags, tactic, UiTheme.BOSS,
            "corrupted-shadow-dragon".equals(id) || "skeletal-undead-dragon".equals(id));
    }

    private static void add(Map<String, Profile> profiles, Profile profile, String... aliases) {
        profiles.put(profile.displayName, profile);
        for (String alias : aliases) profiles.put(alias, profile);
    }
}
