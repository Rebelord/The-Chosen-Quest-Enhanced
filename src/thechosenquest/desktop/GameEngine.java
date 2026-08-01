package thechosenquest.desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Owns all serializable quest state and combat rules. Presentation code should
 * consume State snapshots and EnemyTurnEvent records instead of duplicating
 * calculations, which keeps saves, tests, and animated combat deterministic.
 */
final class GameEngine {
    /** Logical world edge; the UI presents a smaller scrolling viewport. */
    static final int SIZE = 13;
    static final int MAX_PLAYER_NAME_LENGTH = 24;
    private static final int POTION_COST = 10;
    static final int REGIONAL_MAP_COST = 12;

    enum ActionTempo {
        FAST(70, "FAST"), NORMAL(100, "NORMAL"), SLOW(140, "SLOW");

        final int cost;
        final String label;

        ActionTempo(int cost, String label) {
            this.cost = cost;
            this.label = label;
        }
    }

    private static final class EnemyAction {
        final String name;
        final ActionTempo tempo;
        final int damageModifier;

        EnemyAction(String name, ActionTempo tempo, int damageModifier) {
            this.name = name;
            this.tempo = tempo;
            this.damageModifier = damageModifier;
        }
    }

    /** A resolved enemy strike that the interface can present in sequence. */
    static final class EnemyTurnEvent {
        final String enemyName;
        final String actionName;
        final String tempoLabel;
        final int healthBefore;
        final int healthAfter;
        final int damage;
        final boolean missed;
        final int historyStart;
        final int historyEnd;

        EnemyTurnEvent(String enemyName, String actionName, String tempoLabel,
                       int healthBefore, int healthAfter, int damage,
                       boolean missed, int historyStart, int historyEnd) {
            this.enemyName = enemyName;
            this.actionName = actionName;
            this.tempoLabel = tempoLabel;
            this.healthBefore = healthBefore;
            this.healthAfter = healthAfter;
            this.damage = damage;
            this.missed = missed;
            this.historyStart = historyStart;
            this.historyEnd = historyEnd;
        }
    }

    static final String[] RACES = {"Human", "Dwarf", "Elf", "Halfling"};
    static final String[] CLASSES = {"Fighter", "Mage", "Rogue", "Hunter"};

    enum TileType {
        FIELD("Field", '.'), LAKE("Lake", '~'), CRYPT("Crypt", 'C'),
        TAVERN("Tavern", 'T'), SHOP("Shop", 'S'), ENCAMPMENT("Encampment", 'E'),
        SPIDER_NEST("Ashweb Nest", 'N');

        final String label;
        final char symbol;

        TileType(String label, char symbol) {
            this.label = label;
            this.symbol = symbol;
        }
    }

    /** How much of a world tile the hero has learned through travel or clues. */
    enum DiscoveryState {
        UNKNOWN, SCOUTED, VISITED
    }

    /** Services that change what the hero knows about the world map. */
    enum MapService {
        RUMOR, REGIONAL_MAP
    }

    static final class Enemy implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;
        int health;
        final int maxHealth;
        final int attack;
        final int reward;
        int defense;
        int tier;
        int combatLevel;
        int speed;
        int homeRow = -1;
        int homeCol = -1;
        String sourceId;
        boolean reducedRewards;

        Enemy(String name, int health, int attack, int reward) {
            this(name, health, attack, reward, 0);
        }

        Enemy(String name, int health, int attack, int reward, int tier) {
            this.name = name;
            this.health = health;
            this.maxHealth = health;
            this.attack = attack;
            this.reward = reward;
            this.tier = tier;
            this.defense = defenseForEnemy(name);
            this.combatLevel = combatLevelForEnemy(name, tier);
            this.speed = speedForEnemy(name);
        }
    }

    static final class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;
        final String type;
        final int attack;
        final int defense;
        final int cost;
        final String allowedClass;
        final boolean relicReward;
        final String quality;
        final boolean starterItem;

        Item(String name, String type, int attack, int defense, int cost) {
            this(name, type, attack, defense, cost, null, false);
        }

        Item(String name, String type, int attack, int defense, int cost,
             String allowedClass, boolean relicReward) {
            this(name, type, attack, defense, cost, allowedClass, relicReward, null, false);
        }

        Item(String name, String type, int attack, int defense, int cost,
             String allowedClass, boolean relicReward, String quality, boolean starterItem) {
            this.name = name;
            this.type = type;
            this.attack = attack;
            this.defense = defense;
            this.cost = cost;
            this.allowedClass = allowedClass;
            this.relicReward = relicReward;
            this.quality = quality;
            this.starterItem = starterItem;
        }

        @Override
        public String toString() {
            String bonus = attack > 0 && defense > 0 ? "+" + attack + " attack, +" + defense + " defense" :
                (attack > 0 ? "+" + attack + " attack" : "+" + defense + " defense");
            return name + " (" + bonus + ") — " + cost + " gold";
        }
    }

    static String itemQuality(Item item) {
        if (item == null) return "COMMON";
        if (item.relicReward) return "RELIC";
        if (item.quality != null) return item.quality;
        if (item.allowedClass != null) return "RARE";
        int value = item.attack + item.defense;
        return value >= ("Weapon".equals(item.type) ? 7 : 3) || item.cost >= 20
            ? "UNCOMMON" : "COMMON";
    }

    static int itemQualityRank(Item item) {
        String quality = itemQuality(item);
        if ("RELIC".equals(quality)) return 3;
        if ("RARE".equals(quality)) return 2;
        if ("UNCOMMON".equals(quality)) return 1;
        return 0;
    }

    static int itemStat(Item item) {
        return item == null ? 0 : item.attack + item.defense;
    }

    static final class Relic implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;
        final String source;
        final String vendor;
        boolean identified;
        String rewardName;

        Relic(String name, String source, String vendor) {
            this.name = name;
            this.source = source;
            this.vendor = vendor;
        }

        @Override
        public String toString() {
            return identified ? name + " — identified as " + rewardName :
                name + " — seek the " + vendor;
        }
    }

    static final class ProgressionNotice {
        final int level;
        final String abilitySummary;
        final String weaponFamily;
        final int proficiencyRank;

        ProgressionNotice(int level, String abilitySummary, String weaponFamily,
                          int proficiencyRank) {
            this.level = level;
            this.abilitySummary = abilitySummary;
            this.weaponFamily = weaponFamily;
            this.proficiencyRank = proficiencyRank;
        }
    }

    static final class State implements Serializable {
        private static final long serialVersionUID = 1L;
        String playerName;
        String race;
        String heroClass;
        String gender;
        String starterKit;
        int health = 50;
        int maxHealth = 50;
        int mana;
        int maxMana;
        int rage;
        int maxRage;
        int momentum;
        int maxMomentum;
        int focus;
        int maxFocus;
        int gold = 20;
        int potions = 2;
        int level = 1;
        int experience;
        int baseAttack = 5;
        int baseDefense;
        String equippedWeapon;
        String equippedArmour;
        String equippedOffhand;
        ArrayList<Item> inventory = new ArrayList<Item>();
        ArrayList<Relic> relics = new ArrayList<Relic>();
        ArrayList<String> spells = new ArrayList<String>();
        HashMap<String, Integer> weaponProficiency = new HashMap<String, Integer>();
        String selectedSpell;
        int row;
        int col;
        int previousRow;
        int previousCol;
        boolean defending;
        String combatPreparation;
        String combatStatus;
        int enemyStunTurns;
        int enemyBleedTurns;
        int enemyBleedDamage;
        int enemyArmorBreakTurns;
        int enemyArmorBreakValue;
        boolean enemyMarked;
        boolean enemyPinned;
        int heroCombatClock;
        int enemyCombatClock;
        int consecutiveHeroActions;
        boolean secondWindUsed;
        int enemyActionSequence;
        int combatTimelineRow = -1;
        int combatTimelineCol = -1;
        int heroSpeed;
        int enemySpeed;
        String enemyIntent;
        String combatTimeline;
        boolean won;
        TileType[][] tiles = new TileType[SIZE][SIZE];
        Enemy[][] enemies = new Enemy[SIZE][SIZE];
        boolean[][] blacksmithShops = new boolean[SIZE][SIZE];
        DiscoveryState[][] discovery = new DiscoveryState[SIZE][SIZE];
        byte[][] threatKnowledge = new byte[SIZE][SIZE];
        boolean[][] relicSearch = new boolean[SIZE][SIZE];
        boolean[][] rumorServicesUsed = new boolean[SIZE][SIZE];
        boolean regionalMapOwned;
        ArrayList<String> mapJournal = new ArrayList<String>();
        int dragonLairRow = -1;
        int dragonLairCol = -1;
        int spiderNestRow = -1;
        int spiderNestCol = -1;
        int spiderNestRemaining;
        boolean spiderNestCleared;
        int generationVersion = 10;
    }

    private State state;
    private Random random = new Random();
    private final StringBuilder history = new StringBuilder();
    private final ArrayList<EnemyTurnEvent> pendingEnemyTurnEvents =
        new ArrayList<EnemyTurnEvent>();
    private Relic pendingRelicDiscovery;
    private Item pendingLootDiscovery;
    private ProgressionNotice pendingProgressionNotice;

    GameEngine() {
        newGame("Chosen One", "Human", "Fighter");
    }

    void newGame(String playerName) {
        newGame(playerName, "Human", "Fighter");
    }

    void newGame(String playerName, String race, String heroClass) {
        newGame(playerName, race, heroClass, defaultStarterKit(heroClass));
    }

    void newGame(String playerName, String race, String heroClass, String starterKit) {
        newGame(playerName, race, heroClass, starterKit,
            CharacterArt.defaultGender(race, heroClass));
    }

    void newGame(String playerName, String race, String heroClass, String starterKit,
                 String gender) {
        state = new State();
        pendingRelicDiscovery = null;
        pendingLootDiscovery = null;
        pendingProgressionNotice = null;
        pendingEnemyTurnEvents.clear();
        String cleaned = playerName == null ? "" : playerName.trim();
        if (cleaned.length() > MAX_PLAYER_NAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_PLAYER_NAME_LENGTH).trim();
        }
        state.playerName = cleaned.isEmpty() ? "Chosen One" : cleaned;
        state.race = validChoice(race, RACES, "Human");
        state.heroClass = validChoice(heroClass, CLASSES, "Fighter");
        state.gender = CharacterArt.normalizeGender(gender, state.race, state.heroClass);
        state.starterKit = validStarterKit(state.heroClass, starterKit);
        configureHero();

        TileType[] terrain = {TileType.FIELD, TileType.FIELD, TileType.FIELD,
            TileType.LAKE, TileType.CRYPT};
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                state.tiles[row][col] = terrain[random.nextInt(terrain.length)];
            }
        }
        generateLandmarks();
        generateEnemies();
        initializeDiscovery();

        history.setLength(0);
        add("Welcome, " + state.playerName + " the " + state.race + " " + state.heroClass + ".");
        add("Find the dragon's distant lair and defeat it.");
        describeLocation();
    }

    /**
     * Configures only the hero-facing state needed by character creation.
     * Preview changes should not regenerate terrain, landmarks, and enemies on
     * the Swing event thread every time the player clicks a race or class.
     */
    void configureHeroPreview(String race, String heroClass) {
        configureHeroPreview(race, heroClass, defaultStarterKit(heroClass));
    }

    void configureHeroPreview(String race, String heroClass, String starterKit) {
        configureHeroPreview(race, heroClass, starterKit,
            CharacterArt.defaultGender(race, heroClass));
    }

    void configureHeroPreview(String race, String heroClass, String starterKit,
                              String gender) {
        state = new State();
        state.playerName = "Preview";
        state.race = validChoice(race, RACES, "Human");
        state.heroClass = validChoice(heroClass, CLASSES, "Fighter");
        state.gender = CharacterArt.normalizeGender(gender, state.race, state.heroClass);
        state.starterKit = validStarterKit(state.heroClass, starterKit);
        configureHero();
    }

    static String[] starterKitsFor(String heroClass) {
        if ("Mage".equals(heroClass)) return new String[] {"CHANNELER", "SPELLBLADE"};
        if ("Rogue".equals(heroClass)) return new String[] {"DUELIST", "QUICK KNIVES"};
        if ("Hunter".equals(heroClass)) return new String[] {"RANGER", "MARKSMAN"};
        return new String[] {"VANGUARD", "BREAKER"};
    }

    static String defaultStarterKit(String heroClass) { return starterKitsFor(heroClass)[0]; }

    static String starterKitDescription(String heroClass, String kit) {
        if ("SPELLBLADE".equals(kit)) return "Wand, tome, and leather · focused magic";
        if ("QUICK KNIVES".equals(kit)) return "Dual daggers and leather · fast setup";
        if ("MARKSMAN".equals(kit)) return "Crossbow and leather · heavy opening shot";
        if ("BREAKER".equals(kit)) return "Greatsword and leather · high-risk power";
        if ("Mage".equals(heroClass)) return "Staff and cloth · reliable spellcasting";
        if ("Rogue".equals(heroClass)) return "Short sword and leather · balanced skirmisher";
        if ("Hunter".equals(heroClass)) return "Long bow and leather · mobile ranged combat";
        return "Long sword and chain · durable frontline";
    }

    private static String validStarterKit(String heroClass, String kit) {
        for (String candidate : starterKitsFor(heroClass)) if (candidate.equals(kit)) return candidate;
        return defaultStarterKit(heroClass);
    }

    private void initializeDiscovery() {
        state.discovery = new DiscoveryState[SIZE][SIZE];
        state.threatKnowledge = new byte[SIZE][SIZE];
        state.relicSearch = new boolean[SIZE][SIZE];
        state.rumorServicesUsed = new boolean[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                state.discovery[row][col] = DiscoveryState.UNKNOWN;
            }
        }
        revealFromTravel();
    }

    /** Travel fully reveals the current tile and scouts the nearby route. */
    private void revealFromTravel() {
        ensureDiscoveryArrays();
        state.discovery[state.row][state.col] = DiscoveryState.VISITED;
        if (state.enemies[state.row][state.col] != null) {
            state.threatKnowledge[state.row][state.col] = 2;
        }
        int radius = "Hunter".equals(state.heroClass) ? 2 : 1;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int distance = Math.abs(row - state.row) + Math.abs(col - state.col);
                if (distance <= radius && state.discovery[row][col] == DiscoveryState.UNKNOWN) {
                    state.discovery[row][col] = DiscoveryState.SCOUTED;
                }
            }
        }
    }

    private void ensureDiscoveryArrays() {
        if (state.discovery == null || state.discovery.length != SIZE) {
            state.discovery = new DiscoveryState[SIZE][SIZE];
        }
        if (state.threatKnowledge == null || state.threatKnowledge.length != SIZE) {
            state.threatKnowledge = new byte[SIZE][SIZE];
        }
        if (state.relicSearch == null || state.relicSearch.length != SIZE) {
            state.relicSearch = new boolean[SIZE][SIZE];
        }
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (state.discovery[row][col] == null) {
                    state.discovery[row][col] = DiscoveryState.UNKNOWN;
                }
            }
        }
    }

    private void generateLandmarks() {
        boolean[][] reserved = new boolean[SIZE][SIZE];
        state.tiles[0][0] = TileType.ENCAMPMENT;
        reserved[0][0] = true;

        int[] merchant = placeLandmark(TileType.SHOP, 3, 6, reserved, TileType.FIELD);
        placeLandmark(TileType.TAVERN, 5, 9, reserved, TileType.FIELD);
        int[] blacksmith = placeLandmark(TileType.SHOP, 10, 16, reserved, TileType.FIELD);
        placeLandmark(TileType.ENCAMPMENT, 8, 14, reserved, TileType.FIELD);
        int[] nest = placeLandmark(TileType.SPIDER_NEST, 7, 13, reserved, TileType.FIELD);
        int[] lair = placeLandmark(TileType.CRYPT, 18, 24, reserved, TileType.CRYPT);
        state.dragonLairRow = lair[0];
        state.dragonLairCol = lair[1];
        state.spiderNestRow = nest[0];
        state.spiderNestCol = nest[1];
        state.spiderNestRemaining = 3;
        state.blacksmithShops[merchant[0]][merchant[1]] = false;
        state.blacksmithShops[blacksmith[0]][blacksmith[1]] = true;
        // The lair remains a crypt even if future terrain generation changes.
        state.tiles[lair[0]][lair[1]] = TileType.CRYPT;
    }

    private int[] placeLandmark(TileType tile, int minimumDistance, int maximumDistance,
                                boolean[][] reserved, TileType requiredTerrain) {
        ArrayList<int[]> candidates = new ArrayList<int[]>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int distance = row + col;
                if (!reserved[row][col] && distance >= minimumDistance &&
                        distance <= maximumDistance &&
                        (requiredTerrain == null || state.tiles[row][col] == requiredTerrain)) {
                    candidates.add(new int[] {row, col});
                }
            }
        }
        // The weighted terrain generator normally supplies many valid cells.
        // Retain a deterministic distance-only fallback so a rare seed can never
        // omit a required quest landmark.
        if (candidates.isEmpty()) {
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    int distance = row + col;
                    if (!reserved[row][col] && distance >= minimumDistance &&
                            distance <= maximumDistance) candidates.add(new int[] {row, col});
                }
            }
        }
        int[] selected = candidates.get(random.nextInt(candidates.size()));
        state.tiles[selected[0]][selected[1]] = tile;
        reserved[selected[0]][selected[1]] = true;
        return selected;
    }

    private void generateEnemies() {
        ArrayList<Enemy> standards = enemyPool(new Enemy[] {
            new Enemy("Bandit Marauder", 30, 9, 12, 0),
            new Enemy("Skeletal Guardian", 28, 9, 11, 0),
            new Enemy("Cave Troll", 42, 12, 22, 0),
            new Enemy("Cultist Mage", 32, 11, 18, 0),
            new Enemy("Dire Wolf", 22, 8, 7, 0),
            new Enemy("Giant Forest Spider", 24, 8, 9, 0),
            new Enemy("Swamp Serpent", 30, 10, 14, 0),
            new Enemy("Armored Boar", 38, 11, 18, 0)
        });
        ArrayList<Enemy> elites = enemyPool(new Enemy[] {
            new Enemy("Orc Warlord", 62, 14, 42, 1),
            new Enemy("Necromancer", 58, 15, 48, 1),
            new Enemy("Dark Elf Assassin", 52, 16, 44, 1),
            new Enemy("Fallen Knight", 72, 15, 55, 1)
        });
        ArrayList<Enemy> bosses = enemyPool(new Enemy[] {
            new Enemy("Ancient Red Dragon", 126, 17, 300, 2),
            new Enemy("Ancient Frost Dragon", 132, 18, 320, 2),
            new Enemy("Corrupted Shadow Dragon", 138, 19, 340, 2),
            new Enemy("Skeletal Undead Dragon", 146, 20, 360, 2)
        });
        Collections.shuffle(standards, random);
        Collections.shuffle(elites, random);
        Collections.shuffle(bosses, random);

        for (int i = 0; i < Math.min(5, standards.size()); i++) {
            placeBiomeEnemy(standards.get(i), 1, 14);
        }
        for (int i = 0; i < Math.min(3, elites.size()); i++) {
            placeBiomeEnemy(elites.get(i), 10, 21);
        }
        placeEnemy(state.dragonLairRow, state.dragonLairCol, bosses.get(0));
        placeEnemy(state.spiderNestRow, state.spiderNestCol, spiderBroodEnemy());
    }

    private void placeBiomeEnemy(Enemy enemy, int minimumDistance, int maximumDistance) {
        ArrayList<int[]> preferred = openTerrain(minimumDistance, maximumDistance);
        Collections.shuffle(preferred, random);
        for (int[] space : preferred) {
            if (state.enemies[space[0]][space[1]] == null &&
                    terrainSupports(enemy.name, state.tiles[space[0]][space[1]])) {
                placeEnemy(space[0], space[1], enemy);
                return;
            }
        }
        // Preserve encounter counts even if an unusually sparse biome cannot
        // host every shuffled profile in its preferred distance band.
        for (int[] space : preferred) {
            if (state.enemies[space[0]][space[1]] == null) {
                placeEnemy(space[0], space[1], enemy);
                return;
            }
        }
        throw new IllegalStateException("No open terrain for " + enemy.name);
    }

    static boolean terrainSupports(String enemyName, TileType terrain) {
        if (terrain == TileType.SPIDER_NEST) {
            return "Giant Forest Spider".equals(enemyName) ||
                "Giant Spider".equals(enemyName) ||
                "Ashweb Matriarch".equals(enemyName);
        }
        if ("Swamp Serpent".equals(enemyName)) return terrain == TileType.LAKE;
        if ("Skeletal Guardian".equals(enemyName) || "Cave Troll".equals(enemyName) ||
                "Cultist Mage".equals(enemyName) || "Necromancer".equals(enemyName) ||
                "Fallen Knight".equals(enemyName)) return terrain == TileType.CRYPT;
        if (enemyName != null && enemyName.contains("Dragon")) return terrain == TileType.CRYPT;
        return terrain == TileType.FIELD;
    }

    private Enemy spiderBroodEnemy() {
        if (state != null && state.spiderNestRemaining == 1) {
            Enemy matriarch = new Enemy("Ashweb Matriarch", 48, 11, 6, 1);
            // The finale uses elite presentation and initiative, but remains
            // a level-two source encounter rather than a relic-bearing world elite.
            matriarch.defense = 4;
            matriarch.combatLevel = 2;
            matriarch.speed = 115;
            matriarch.sourceId = "SPIDER_NEST";
            matriarch.reducedRewards = true;
            return matriarch;
        }
        Enemy brood = new Enemy("Giant Forest Spider", 24, 8, 3, 0);
        brood.sourceId = "SPIDER_NEST";
        brood.reducedRewards = true;
        return brood;
    }

    private ArrayList<Enemy> enemyPool(Enemy[] values) {
        ArrayList<Enemy> result = new ArrayList<Enemy>();
        Collections.addAll(result, values);
        return result;
    }

    private ArrayList<int[]> openTerrain(int minimumDistance, int maximumDistance) {
        ArrayList<int[]> result = new ArrayList<int[]>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int distance = row + col;
                TileType tile = state.tiles[row][col];
                if (distance >= minimumDistance && distance <= maximumDistance &&
                        (row != state.dragonLairRow || col != state.dragonLairCol) &&
                        tile != TileType.SHOP && tile != TileType.TAVERN &&
                        tile != TileType.ENCAMPMENT && tile != TileType.SPIDER_NEST) {
                    result.add(new int[] {row, col});
                }
            }
        }
        return result;
    }

    private String validChoice(String choice, String[] choices, String fallback) {
        for (String candidate : choices) {
            if (candidate.equals(choice)) {
                return candidate;
            }
        }
        return fallback;
    }

    private void configureHero() {
        if ("Dwarf".equals(state.race)) {
            state.maxHealth = 65;
            state.baseDefense = 2;
        } else if ("Elf".equals(state.race)) {
            state.maxHealth = 50;
            state.maxMana += 8;
            state.baseAttack = 6;
        } else if ("Halfling".equals(state.race)) {
            state.maxHealth = 48;
            state.baseDefense = 1;
            state.gold += 10;
        } else {
            state.maxHealth = 55;
            state.baseAttack = 6;
        }

        if ("Mage".equals(state.heroClass)) {
            state.maxMana += 30;
            state.baseAttack += 1;
            if ("SPELLBLADE".equals(state.starterKit)) {
                addStartingItem(new Item("Apprentice Wand", "Weapon", 3, 0, 8, "Mage", false), true);
                addStartingItem(new Item("Apprentice Tome", "Offhand", 1, 0, 8, "Mage", false), true);
                addStartingItem(new Item("Leather Armour", "Armour", 0, 2, 15), true);
            } else {
                addStartingItem(new Item("Oak Staff", "Weapon", 4, 0, 15), true);
                addStartingItem(new Item("Cloth Armour", "Armour", 0, 1, 2), true);
            }
            state.spells.add("Magic Missile");
            state.selectedSpell = "Magic Missile";
        } else if ("Rogue".equals(state.heroClass)) {
            state.baseAttack += 3;
            state.baseDefense += 1;
            state.gold += 20;
            state.maxMomentum = 100;
            addStartingItem(new Item("QUICK KNIVES".equals(state.starterKit) ? "Dagger" : "Short Sword",
                "Weapon", "QUICK KNIVES".equals(state.starterKit) ? 5 : 6, 0, 10), true);
            if ("QUICK KNIVES".equals(state.starterKit)) {
                addStartingItem(new Item("Offhand Dagger", "Offhand", 2, 0, 7, "Rogue", false), true);
            }
            addStartingItem(new Item("Leather Armour", "Armour", 0, 2, 15), true);
        } else if ("Hunter".equals(state.heroClass)) {
            state.baseAttack += 2;
            state.maxHealth += 8;
            state.maxFocus = maxFocusForLevel(state.level);
            addStartingItem(new Item("MARKSMAN".equals(state.starterKit) ? "Crossbow" : "Long Bow",
                "Weapon", "MARKSMAN".equals(state.starterKit) ? 8 : 7, 0, 30), true);
            addStartingItem(new Item("Leather Armour", "Armour", 0, 2, 15), true);
        } else {
            state.maxHealth += 15;
            state.baseDefense += 2;
            state.maxRage = 100;
            addStartingItem(new Item("BREAKER".equals(state.starterKit) ? "Greatsword" : "Long Sword",
                "Weapon", "BREAKER".equals(state.starterKit) ? 9 : 7, 0, 15,
                "Fighter", false), true);
            addStartingItem(new Item("BREAKER".equals(state.starterKit) ? "Leather Armour" : "Chain Armour",
                "Armour", 0, "BREAKER".equals(state.starterKit) ? 2 : 4, 35), true);
        }
        state.health = state.maxHealth;
        state.mana = state.maxMana;
        state.rage = 0;
        state.momentum = 0;
        state.focus = "Hunter".equals(state.heroClass) ? state.maxFocus / 3 : 0;
        trainEquippedWeapon(1, false);
    }

    private void addStartingItem(Item item, boolean equip) {
        item = new Item(item.name, item.type, item.attack, item.defense, item.cost,
            item.allowedClass, item.relicReward, "COMMON", true);
        state.inventory.add(item);
        if (equip) {
            if ("Weapon".equals(item.type)) {
                state.equippedWeapon = item.name;
            } else if ("Offhand".equals(item.type)) {
                state.equippedOffhand = item.name;
            } else {
                state.equippedArmour = item.name;
            }
        }
    }

    private void placeEnemy(int row, int col, Enemy enemy) {
        enemy.homeRow = row;
        enemy.homeCol = col;
        state.enemies[row][col] = enemy;
    }

    void move(int rowDelta, int colDelta) {
        if (!canAct()) {
            return;
        }
        if (currentEnemy() != null) {
            add("The " + currentEnemy().name + " blocks your path. Fight or flee.");
            return;
        }
        int nextRow = state.row + rowDelta;
        int nextCol = state.col + colDelta;
        if (nextRow < 0 || nextRow >= SIZE || nextCol < 0 || nextCol >= SIZE) {
            add("You cannot travel beyond the edge of the realm.");
            return;
        }
        state.previousRow = state.row;
        state.previousCol = state.col;
        state.row = nextRow;
        state.col = nextCol;
        state.defending = false;
        roamEnemies();
        activateSpiderNest();
        revealFromTravel();
        describeLocation();
    }

    private void roamEnemies() {
        Enemy[][] next = new Enemy[SIZE][SIZE];
        byte[][] nextKnowledge = new byte[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Enemy enemy = state.enemies[row][col];
                if (enemy == null) continue;
                int targetRow = row;
                int targetCol = col;
                int chance = enemy.sourceId != null ? 0 :
                    (enemy.tier == 0 ? 28 : (enemy.tier == 1 ? 12 : 0));
                if ((row != state.row || col != state.col) && random.nextInt(100) < chance) {
                    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                    int[] direction = directions[random.nextInt(directions.length)];
                    int candidateRow = row + direction[0];
                    int candidateCol = col + direction[1];
                    if (canEnemyOccupy(enemy, candidateRow, candidateCol, next)) {
                        targetRow = candidateRow;
                        targetCol = candidateCol;
                    }
                }
                if (next[targetRow][targetCol] == null) {
                    next[targetRow][targetCol] = enemy;
                    nextKnowledge[targetRow][targetCol] = state.threatKnowledge[row][col];
                } else {
                    next[row][col] = enemy;
                    nextKnowledge[row][col] = state.threatKnowledge[row][col];
                }
            }
        }
        state.enemies = next;
        state.threatKnowledge = nextKnowledge;
    }

    private boolean canEnemyOccupy(Enemy enemy, int row, int col, Enemy[][] moved) {
        if (!inside(row, col) || (row == state.row && col == state.col) ||
                state.enemies[row][col] != null || moved[row][col] != null) return false;
        TileType tile = state.tiles[row][col];
        if (tile == TileType.SHOP || tile == TileType.TAVERN ||
                tile == TileType.ENCAMPMENT) return false;
        if (!terrainSupports(enemy.name, tile)) return false;
        return enemy.tier != 1 ||
            Math.abs(row - enemy.homeRow) + Math.abs(col - enemy.homeCol) <= 1;
    }

    private boolean inside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    void attack() {
        if (!canAct()) {
            return;
        }
        Enemy enemy = currentEnemy();
        if (enemy == null) {
            add("There is nothing here to attack.");
            return;
        }
        Item weapon = findItem(state.equippedWeapon);
        String trait = weaponTraitName(weapon);
        state.combatStatus = trait + " · READY";
        int rawDamage = totalAttack() - 4 + random.nextInt(5);
        if ("Fighter".equals(state.heroClass)) rawDamage++;
        if ("Mage".equals(state.heroClass)) rawDamage -= 5;
        boolean aimed = "AIM".equals(state.combatPreparation);
        boolean stealthed = "STEALTH".equals(state.combatPreparation);
        boolean enraged = "RAGE".equals(state.combatPreparation);
        boolean marked = state.enemyMarked;
        state.combatPreparation = null;
        if (enraged) {
            rawDamage += 5 + state.level;
            add("Rage empowers your heavy strike!");
        }
        if ("Hunter".equals(state.heroClass) && (aimed || random.nextInt(5) == 0)) {
            rawDamage += aimed ? (isBoss(enemy) ? 9 : 5) : 3;
            add("Precision shot!");
        }
        if (marked) {
            rawDamage += 3;
            add("Hunter's Mark guides the shot.");
            state.combatStatus = "HUNTER'S MARK · TARGET TRACKED";
        }
        rawDamage = Math.max(2, rawDamage);
        if (isBoss(enemy)) rawDamage += bossRelicDamageBonus();
        int armour = effectiveEnemyDefense(enemy);
        if ("ARMOR PIERCING".equals(trait)) {
            int pierced = Math.min(3, armour);
            armour -= pierced;
            if (pierced > 0) {
                add("Armor Piercing ignores " + pierced + " defense.");
                state.combatStatus = "ARMOR PIERCING · " + pierced + " DEF IGNORED";
            }
        } else if ("SUNDERING".equals(trait)) {
            int sundered = Math.min(2, armour);
            armour -= sundered;
            if (sundered > 0) {
                add("Sundering force breaks through " + sundered + " defense.");
                state.combatStatus = "SUNDERING · " + sundered + " DEF BROKEN";
            }
            applyArmorBreak(2, 2);
        }
        int damage = Math.max(1, rawDamage - armour);
        int absorbed = Math.max(0, rawDamage - damage);
        if (absorbed > 0) {
            add("The " + enemy.name + "'s armor absorbs " + absorbed + " damage.");
        }
        if ("Rogue".equals(state.heroClass) && (stealthed || random.nextInt(4) == 0)) {
            damage = isBoss(enemy) ? Math.max(damage + 1, damage * 4 / 3) : damage * 2;
            add(stealthed ? "Stealth attack! Critical strike!" : "Critical strike!");
        }
        if ("BLEEDING EDGE".equals(trait) && random.nextInt(100) < traitChance(28)) {
            int bleed = isBoss(enemy) ? 2 : 3;
            damage += bleed;
            applyBleed(bleed, 2);
            add("Bleeding Edge opens a wound for " + bleed +
                " bonus damage and 2 turns of bleeding.");
            state.combatStatus = "BLEEDING EDGE · BLEED " + bleed + " · 2 TURNS";
        } else if ("CONCUSSIVE".equals(trait)) {
            int stunChance = isBoss(enemy) ? 8 : (enemy.tier == 1 ? 16 : 26);
            if (random.nextInt(100) < traitChance(stunChance)) {
                state.enemyStunTurns = 1;
                add("Concussive impact stuns the " + enemy.name + "!");
                state.combatStatus = "CONCUSSIVE · ENEMY STUNNED";
            }
        } else if ("BALANCED GUARD".equals(trait) && random.nextInt(100) < traitChance(18)) {
            state.defending = true;
            add("Balanced Guard flows into a defensive stance.");
            state.combatStatus = "BALANCED GUARD · DAMAGE REDUCED";
        } else if ("PRECISE".equals(trait) && random.nextInt(100) < traitChance(18)) {
            damage += 3;
            add("Precise aim finds a vulnerable point for 3 bonus damage.");
            state.combatStatus = "PRECISE · WEAK POINT";
        }
        enemy.health = Math.max(0, enemy.health - damage);
        add("You strike the " + enemy.name + " for " + damage + " damage.");
        if ("Fighter".equals(state.heroClass)) gainRage(14, "attack");
        if ("Rogue".equals(state.heroClass)) gainMomentum(18, "attack");
        if ("Hunter".equals(state.heroClass)) {
            gainFocus(15 + (aimed ? 10 : 0) + (marked ? 5 : 0),
                aimed ? "aimed shot" : "ranged hit");
        }
        if (enemy.health == 0) {
            defeatEnemy(enemy);
            return;
        }
        completePlayerAction(enemy, attackTempo(state));
    }

    private int effectiveEnemyDefense(Enemy enemy) {
        int reduction = state.enemyArmorBreakTurns > 0 ? state.enemyArmorBreakValue : 0;
        return Math.max(0, enemy.defense - reduction);
    }

    private void applyBleed(int damage, int turns) {
        state.enemyBleedDamage = Math.max(state.enemyBleedDamage, damage);
        state.enemyBleedTurns = Math.max(state.enemyBleedTurns, turns);
    }

    private void applyArmorBreak(int value, int turns) {
        state.enemyArmorBreakValue = Math.max(state.enemyArmorBreakValue, value);
        state.enemyArmorBreakTurns = Math.max(state.enemyArmorBreakTurns, turns);
    }

    void castSpell(String spell) {
        if (!canAct()) {
            return;
        }
        Enemy enemy = currentEnemy();
        if (enemy == null) {
            add("There is no target for a spell.");
            return;
        }
        if (!"Mage".equals(state.heroClass) || !state.spells.contains(spell)) {
            add("Your " + state.heroClass + " cannot cast " + spell + ".");
            return;
        }
        state.selectedSpell = spell;
        int cost;
        int damage;
        if ("Fireball".equals(spell)) {
            cost = 12;
            damage = 21 + random.nextInt(7);
        } else if ("Ice Spike".equals(spell)) {
            cost = 15;
            damage = 25 + random.nextInt(7);
        } else {
            cost = 7;
            damage = 13 + random.nextInt(5);
            spell = "Magic Missile";
        }
        if (state.mana < cost) {
            add("You need " + cost + " mana to cast " + spell + ".");
            return;
        }
        damage += (state.level - 1) * 2;
        Item weapon = findItem(state.equippedWeapon);
        if ("ARCANE FOCUS".equals(weaponTraitName(weapon))) {
            int rank = Math.max(1, equippedWeaponProficiency(state));
            int focusBonus = Math.max(1, weapon.attack * (rank + 1) / 6);
            damage += focusBonus;
            add("Arcane Focus amplifies the spell by " + focusBonus + " damage.");
            state.combatStatus = "ARCANE FOCUS · +" + focusBonus + " SPELL POWER";
        } else {
            state.combatStatus = spell.toUpperCase() + " · CAST";
        }
        Item focus = findItem(state.equippedOffhand);
        if (focus != null && "Mage".equals(state.heroClass)) damage += focus.attack * 2;
        if (isBoss(enemy)) damage += bossRelicDamageBonus();
        state.mana -= cost;
        enemy.health = Math.max(0, enemy.health - damage);
        add("You cast " + spell + " for " + damage + " damage.");
        if (enemy.health == 0) {
            defeatEnemy(enemy);
        } else {
            completePlayerAction(enemy, spellTempo(spell));
        }
    }

    void castSelectedSpell() {
        if (state.spells.isEmpty()) {
            add("Your class does not know any spells.");
            return;
        }
        String selected = state.selectedSpell;
        if (selected == null || !state.spells.contains(selected)) {
            selected = state.spells.get(0);
        }
        castSpell(selected);
    }

    void selectSpell(String spell) {
        if (!"Mage".equals(state.heroClass) || !state.spells.contains(spell)) {
            add("Your " + state.heroClass + " cannot prepare " + spell + ".");
            return;
        }
        state.selectedSpell = spell;
        add(spell + " is marked in your spellbook. Combat spells use direct action keys.");
    }

    static int spellCost(String spell) {
        if ("Fireball".equals(spell)) return 12;
        if ("Ice Spike".equals(spell)) return 15;
        return 7;
    }

    static int rageCost(int slot) {
        return slot == 2 ? 50 : 30;
    }

    static int momentumCost(int slot) {
        if (slot == 3) return 70;
        return slot == 2 ? 55 : 30;
    }

    static int hunterFocusCost(State state, int slot) {
        if (slot == 3) return 0;
        return slot == 2 ? ("Volley".equals(abilityName(state, slot)) ? 50 : 45) : 25;
    }

    static int maxFocusForLevel(int level) {
        return Math.min(120, 60 + Math.max(0, level - 1) * 20);
    }

    private void gainRage(int amount, String source) {
        if (!"Fighter".equals(state.heroClass) || state.maxRage < 1 || amount < 1) return;
        int gained = Math.min(amount, state.maxRage - state.rage);
        if (gained <= 0) return;
        state.rage += gained;
        add("Rage +" + gained + " from " + source + " (" +
            state.rage + "/" + state.maxRage + ").");
    }

    private void gainMomentum(int amount, String source) {
        if (!"Rogue".equals(state.heroClass) || state.maxMomentum < 1 || amount < 1) return;
        int gained = Math.min(amount, state.maxMomentum - state.momentum);
        if (gained <= 0) return;
        state.momentum += gained;
        add("Momentum +" + gained + " from " + source + " (" +
            state.momentum + "/" + state.maxMomentum + ").");
    }

    private void gainFocus(int amount, String source) {
        if (!"Hunter".equals(state.heroClass) || state.maxFocus < 1 || amount < 1) return;
        int gained = Math.min(amount, state.maxFocus - state.focus);
        if (gained <= 0) return;
        state.focus += gained;
        add("Focus +" + gained + " from " + source + " (" +
            state.focus + "/" + state.maxFocus + ").");
    }

    void defend() {
        if (!canAct()) {
            return;
        }
        Enemy enemy = currentEnemy();
        if (enemy == null) {
            add("You ready your shield, but no danger approaches.");
            return;
        }
        if ("Fighter".equals(state.heroClass)) {
            add("Fighters build Rage by attacking and enduring enemy blows; " +
                "they have no defensive setup action.");
            state.combatStatus = "FIGHTER · ATTACK TO BUILD RAGE";
            return;
        }
        Item offhand = findItem(state.equippedOffhand);
        state.defending = true;
        if ("Mage".equals(state.heroClass)) {
            int restored = Math.min(4 + state.level / 2, state.maxMana - state.mana);
            state.mana += restored;
            add(restored > 0 ? "You channel a ward and recover " + restored + " mana." :
                "You channel a ward around yourself.");
            state.combatStatus = "WARD · " + (restored > 0 ? "+" + restored + " MANA" : "ACTIVE");
        } else if ("Rogue".equals(state.heroClass)) {
            state.combatPreparation = "STEALTH";
            gainMomentum(10, "stealth setup");
            add("You slip into stealth and prepare a critical strike.");
            state.combatStatus = "STEALTH · CRITICAL READY";
        } else if ("Hunter".equals(state.heroClass)) {
            state.combatPreparation = "AIM";
            gainFocus(25, "Take Aim");
            add("You take aim, build Focus, and prepare a precision shot.");
            state.combatStatus = "AIM · FOCUS BUILDING";
        } else {
            add(offhand != null && offhand.name.contains("Shield")
                ? "You raise your shield against the " + enemy.name + "."
                : "You brace for the " + enemy.name + "'s attack.");
            state.combatStatus = offhand != null && offhand.name.contains("Shield")
                ? "SHIELD RAISED · DAMAGE REDUCED" : "BRACED · DAMAGE REDUCED";
        }
        completePlayerAction(enemy, ActionTempo.FAST);
    }

    void flee() {
        if (!canAct()) {
            return;
        }
        Enemy enemy = currentEnemy();
        if (enemy == null) {
            add("There is nothing to flee from.");
            return;
        }
        int[] retreat = retreatDestination();
        if (retreat == null) {
            add("The " + enemy.name + " has you surrounded. There is nowhere safe to flee.");
            return;
        }
        if (isBoss(enemy)) {
            enemy.health = enemy.maxHealth;
        } else {
            enemy.health = Math.min(enemy.maxHealth,
                enemy.health + Math.max(1, enemy.maxHealth / 5));
        }
        int originRow = state.row;
        int originCol = state.col;
        state.row = retreat[0];
        state.col = retreat[1];
        state.previousRow = originRow;
        state.previousCol = originCol;
        state.defending = false;
        state.combatPreparation = null;
        resetCombatTimeline();
        revealFromTravel();
        add("You escape from the " + enemy.name + " and retreat to " +
            coordinate(state.row, state.col) + ". The enemy regains its footing.");
        describeLocation();
    }

    private int[] retreatDestination() {
        if (isSafeRetreat(state.previousRow, state.previousCol)) {
            return new int[] {state.previousRow, state.previousCol};
        }
        for (int distance = 1; distance < SIZE * 2; distance++) {
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (Math.abs(row - state.row) + Math.abs(col - state.col) == distance &&
                            isSafeRetreat(row, col)) {
                        return new int[] {row, col};
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafeRetreat(int row, int col) {
        return inside(row, col) && (row != state.row || col != state.col) &&
            state.enemies[row][col] == null;
    }

    private String coordinate(int row, int col) {
        return Character.toString((char) ('A' + row)) + (col + 1);
    }

    private boolean isBoss(Enemy enemy) {
        return enemy != null && enemy.tier == 2;
    }

    private void defeatEnemy(Enemy enemy) {
        state.gold += enemy.reward;
        add("The " + enemy.name + " is defeated. You collect " + enemy.reward + " gold.");
        boolean sourceSpawn = "SPIDER_NEST".equals(enemy.sourceId);
        if (!sourceSpawn && enemy.tier == 1) awardRelic(enemy);
        else if (!sourceSpawn && enemy.tier == 0) awardStandardEquipmentDrop();
        state.enemies[state.row][state.col] = null;
        state.threatKnowledge[state.row][state.col] = 0;
        state.relicSearch[state.row][state.col] = false;
        resetCombatTimeline();
        gainExperience(sourceSpawn ? Math.max(1, enemy.maxHealth / 8) : enemy.maxHealth / 2);
        if (sourceSpawn) resolveSpiderNestDefeat();
        if (isBoss(enemy)) {
            state.won = true;
            add("The dragon has fallen. You have saved the realm!");
        }
    }

    /** Standard foes occasionally leave useful, class-compatible field loot. */
    private void awardStandardEquipmentDrop() {
        int roll = random.nextInt(100);
        if (roll >= 32) return;
        boolean uncommon = roll < 9;
        Item drop = randomEquipmentDrop(uncommon);
        state.inventory.add(drop);
        pendingLootDiscovery = drop;
        add("Loot found: " + drop.name + " [" + itemQuality(drop) + "].");
    }

    private Item randomEquipmentDrop(boolean uncommon) {
        String quality = uncommon ? "UNCOMMON" : "COMMON";
        ArrayList<Item> pool = new ArrayList<Item>();
        if ("Mage".equals(state.heroClass)) {
            if (uncommon) {
                pool.add(new Item("Runed Wand", "Weapon", 6, 0, 22, "Mage", false, quality, false));
                pool.add(new Item("Runed Tome", "Offhand", 2, 0, 24, "Mage", false, quality, false));
                pool.add(new Item("Mystic Robes", "Armour", 0, 3, 26, "Mage", false, quality, false));
            } else {
                pool.add(new Item("Apprentice Wand", "Weapon", 3, 0, 8, "Mage", false, quality, false));
                pool.add(new Item("Carved Totem", "Offhand", 1, 0, 8, "Mage", false, quality, false));
                pool.add(new Item("Cloth Armour", "Armour", 0, 1, 8, "Mage", false, quality, false));
            }
        } else if ("Rogue".equals(state.heroClass)) {
            if (uncommon) {
                pool.add(new Item("Serrated Dagger", "Weapon", 7, 0, 22, "Rogue", false, quality, false));
                pool.add(new Item("Balanced Offhand Dagger", "Offhand", 3, 0, 24, "Rogue", false, quality, false));
                pool.add(new Item("Reinforced Leather", "Armour", 0, 4, 32, "Rogue", false, quality, false));
            } else {
                pool.add(new Item("Dagger", "Weapon", 5, 0, 8, "Rogue", false, quality, false));
                pool.add(new Item("Offhand Dagger", "Offhand", 2, 0, 7, "Rogue", false, quality, false));
                pool.add(new Item("Leather Armour", "Armour", 0, 2, 15, "Rogue", false, quality, false));
            }
        } else if ("Hunter".equals(state.heroClass)) {
            if (uncommon) {
                pool.add(new Item("Hunting Crossbow", "Weapon", 9, 0, 25, "Hunter", false, quality, false));
                pool.add(new Item("Hunter's Quiver", "Offhand", 1, 1, 24, "Hunter", false, quality, false));
                pool.add(new Item("Reinforced Scale", "Armour", 0, 5, 40, "Hunter", false, quality, false));
            } else {
                pool.add(new Item("Long Bow", "Weapon", 6, 0, 10, "Hunter", false, quality, false));
                pool.add(new Item("Field Quiver", "Offhand", 1, 0, 9, "Hunter", false, quality, false));
                pool.add(new Item("Leather Armour", "Armour", 0, 2, 15, "Hunter", false, quality, false));
            }
        } else if (uncommon) {
            pool.add(new Item("Flanged Mace", "Weapon", 9, 0, 30, "Fighter", false, quality, false));
            pool.add(new Item("Soldier Shield", "Offhand", 0, 3, 28, "Fighter", false, quality, false));
            pool.add(new Item("Scale Armour", "Armour", 0, 3, 25, "Fighter", false, quality, false));
        } else {
            pool.add(new Item("Iron Mace", "Weapon", 6, 0, 10, "Fighter", false, quality, false));
            pool.add(new Item("Buckler", "Offhand", 0, 1, 9, "Fighter", false, quality, false));
            pool.add(new Item("Padded Armour", "Armour", 0, 2, 12, "Fighter", false, quality, false));
        }
        Collections.shuffle(pool, random);
        for (Item candidate : pool) if (!inventoryContains(candidate.name)) return candidate;
        return pool.get(0);
    }

    private boolean inventoryContains(String name) {
        if (state.inventory != null) for (Item item : state.inventory)
            if (item.name.equals(name)) return true;
        return false;
    }

    private void awardRelic(Enemy enemy) {
        Relic relic;
        if ("Orc Warlord".equals(enemy.name)) {
            relic = new Relic("Battered War Crest", enemy.name, "Blacksmith");
        } else if ("Necromancer".equals(enemy.name)) {
            relic = new Relic("Sealed Soulglass", enemy.name, "Alchemist");
        } else if ("Dark Elf Assassin".equals(enemy.name)) {
            relic = new Relic("Moonmarked Coffer", enemy.name, "General Merchant");
        } else {
            relic = new Relic("Tarnished Oath Signet", enemy.name, "Innkeeper");
        }
        state.relics.add(relic);
        pendingRelicDiscovery = relic;
        add("The elite carried an unidentified relic: " + relic.name + ".");
        add("Bring it to the " + relic.vendor + " for free identification.");
    }

    /** The nest has a finite brood budget; it never becomes an endless farm. */
    private void activateSpiderNest() {
        if (currentTile() == TileType.SPIDER_NEST && currentEnemy() == null &&
                !state.spiderNestCleared && state.spiderNestRemaining > 0) {
            spawnSpiderBrood();
        }
    }

    private void spawnSpiderBrood() {
        if (state.spiderNestCleared || state.spiderNestRemaining <= 0 ||
                currentEnemy() != null || currentTile() != TileType.SPIDER_NEST) return;
        placeEnemy(state.row, state.col, spiderBroodEnemy());
        state.threatKnowledge[state.row][state.col] = 2;
    }

    private void resolveSpiderNestDefeat() {
        state.spiderNestRemaining = Math.max(0, state.spiderNestRemaining - 1);
        if (state.spiderNestRemaining == 0) {
            state.spiderNestCleared = true;
            state.gold += 18;
            add("The final brood falls. The Ashweb Nest is permanently cleared!");
            add("You salvage 18 gold from abandoned packs—a one-time clearing reward.");
        } else {
            add(state.spiderNestRemaining == 1
                ? "The lesser broods are gone. The Ashweb Matriarch stirs deeper within."
                : state.spiderNestRemaining + " brood clutches remain. Search the nest " +
                    "when you are ready to continue.");
        }
    }

    void usePotion() {
        if (!canAct()) {
            return;
        }
        if (state.potions < 1) {
            add("You have no potions left.");
            return;
        }
        if (state.health == state.maxHealth) {
            add("You are already at full health.");
            return;
        }
        state.potions--;
        int restored = Math.min(20, state.maxHealth - state.health);
        state.health += restored;
        add("You drink a potion and recover " + restored + " health.");
        if (currentEnemy() != null) {
            completePlayerAction(currentEnemy(), ActionTempo.NORMAL);
        }
    }

    void locationAction() {
        if (!canAct() || currentEnemy() != null) {
            add(currentEnemy() == null ? "You cannot do that now." : "Defeat the enemy first.");
            return;
        }
        TileType tile = currentTile();
        if (tile == TileType.SPIDER_NEST) {
            if (state.spiderNestCleared || state.spiderNestRemaining <= 0) {
                add("The Ashweb Nest is permanently cleared. No fresh tracks disturb the dust.");
            } else {
                spawnSpiderBrood();
                add("You cut through another webbed clutch. A spider drops from the hollow!");
            }
        } else if (tile == TileType.TAVERN || tile == TileType.ENCAMPMENT) {
            int restored = state.maxHealth - state.health;
            state.health = state.maxHealth;
            state.mana = state.maxMana;
            add(restored == 0 ? "You are already well rested." :
                "You rest and recover " + restored + " health.");
        } else if (tile == TileType.SHOP) {
            buyPotion();
        } else {
            add("There is nothing special to do here.");
        }
    }

    /** Applies a vendor or rumor service to the hero's persistent map knowledge. */
    void useMapService(MapService service) {
        if (!canAct() || currentEnemy() != null) return;
        if (service == MapService.REGIONAL_MAP) {
            buyRegionalMap();
        } else {
            hearRumor();
        }
    }

    private void buyRegionalMap() {
        if (!"General Merchant".equals(currentVendor())) {
            add("Regional maps are sold by the General Merchant.");
            return;
        }
        if (state.regionalMapOwned) {
            add("Your regional map already shows every road and landmark.");
            return;
        }
        if (state.gold < REGIONAL_MAP_COST) {
            add("A regional map costs " + REGIONAL_MAP_COST + " gold.");
            return;
        }
        state.gold -= REGIONAL_MAP_COST;
        state.regionalMapOwned = true;
        ensureDiscoveryArrays();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (state.discovery[row][col] == DiscoveryState.UNKNOWN) {
                    state.discovery[row][col] = DiscoveryState.SCOUTED;
                }
            }
        }
        add("You study a regional map. Roads, terrain, and landmarks are now scouted.");
        add("The map does not reveal which dangers still roam those routes.");
        recordMapClue("REGIONAL MAP · All landmarks scouted; roaming threats remain unknown.");
    }

    private void hearRumor() {
        TileType tile = currentTile();
        if (tile != TileType.TAVERN && tile != TileType.ENCAMPMENT) {
            add("Seek rumors at the tavern or arcane advice at the alchemist's camp.");
            return;
        }
        ensureRumorServiceArray();
        if (state.rumorServicesUsed[state.row][state.col]) {
            add(tile == TileType.TAVERN
                ? "You have already heard the useful rumors at this tavern."
                : "The alchemist has no further arcane advice for this journey.");
            return;
        }
        // A consultation is a one-time location service even when every danger
        // is already known; this prevents repeatedly farming map information.
        state.rumorServicesUsed[state.row][state.col] = true;
        boolean seekBossFirst = tile == TileType.ENCAMPMENT;
        int[] target = unrevealedThreat(seekBossFirst ? 2 : 1);
        if (target == null) target = unrevealedThreat(seekBossFirst ? 1 : 2);
        if (target == null) target = unrevealedThreat(0);
        if (target == null) {
            add("No fresh rumors remain. Every known danger has already been marked.");
            return;
        }
        Enemy enemy = state.enemies[target[0]][target[1]];
        state.threatKnowledge[target[0]][target[1]] = 1;
        scoutRumorRegion(target[0], target[1], enemy.tier == 1);
        String direction = directionFrom(state.row, state.col, target[0], target[1]);
        String terrain = state.tiles[target[0]][target[1]].label.toLowerCase();
        if (enemy.tier == 1) {
            String tradition = relicTradition(enemy.name);
            String clue = "RELIC RUMOR · " + direction + " near " + terrain +
                " terrain · " + tradition;
            recordMapClue(clue);
            add("A rumor marks a relic-bearing elite " + direction.toLowerCase() +
                " near " + coordinate(target[0], target[1]) + ". " + tradition +
                " Search the violet-marked region.");
        } else if (enemy.tier == 2) {
            recordMapClue("ARCANE WARNING · Overwhelming threat " + direction +
                " near " + terrain + " terrain.");
            add("Arcane signs warn of an overwhelming threat near " +
                coordinate(target[0], target[1]) + ". Its identity remains unknown.");
        } else {
            recordMapClue("TRAVELER'S WARNING · Unknown danger " + direction +
                " near " + terrain + " terrain.");
            add("Travelers report danger near " + coordinate(target[0], target[1]) + ".");
        }
    }

    private void recordMapClue(String clue) {
        if (state.mapJournal == null) state.mapJournal = new ArrayList<String>();
        if (clue == null || clue.length() == 0) return;
        if (!state.mapJournal.contains(clue)) state.mapJournal.add(clue);
        while (state.mapJournal.size() > 8) state.mapJournal.remove(0);
    }

    String latestMapClue() {
        return state.mapJournal == null || state.mapJournal.isEmpty() ? null :
            state.mapJournal.get(state.mapJournal.size() - 1);
    }

    List<String> mapJournal() {
        return state.mapJournal == null ? Collections.<String>emptyList() :
            Collections.unmodifiableList(state.mapJournal);
    }

    String relicClueAt(int row, int col) {
        if (!inside(row, col) || !hasRelicClueAt(row, col)) return null;
        for (int candidateRow = 0; candidateRow < SIZE; candidateRow++) {
            for (int candidateCol = 0; candidateCol < SIZE; candidateCol++) {
                Enemy enemy = state.enemies[candidateRow][candidateCol];
                if (enemy != null && enemy.tier == 1 &&
                        Math.abs(row - candidateRow) + Math.abs(col - candidateCol) <= 1) {
                    return relicTradition(enemy.name);
                }
            }
        }
        return "Signs suggest an elite may carry an unidentified relic.";
    }

    private String relicTradition(String enemyName) {
        if ("Orc Warlord".equals(enemyName))
            return "Martial insignia may interest a Blacksmith.";
        if ("Necromancer".equals(enemyName))
            return "Spirit residue may be understood by an Alchemist.";
        if ("Dark Elf Assassin".equals(enemyName))
            return "Moonmarked valuables may be known to a General Merchant.";
        return "An old oath-token may be recognized by an Innkeeper.";
    }

    private String directionFrom(int fromRow, int fromCol, int toRow, int toCol) {
        int vertical = toRow - fromRow;
        int horizontal = toCol - fromCol;
        String northSouth = vertical < 0 ? "NORTH" : (vertical > 0 ? "SOUTH" : "");
        String eastWest = horizontal < 0 ? "WEST" : (horizontal > 0 ? "EAST" : "");
        String direction = northSouth + eastWest;
        int distance = Math.abs(vertical) + Math.abs(horizontal);
        String range = distance <= 4 ? "NEARBY" : (distance <= 8 ? "MID-DISTANCE" : "DISTANT");
        return range + (direction.length() == 0 ? "" : " " + direction);
    }

    private int[] unrevealedThreat(int tier) {
        ArrayList<int[]> candidates = new ArrayList<int[]>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Enemy enemy = state.enemies[row][col];
                if (enemy != null && enemy.tier == tier && state.threatKnowledge[row][col] == 0) {
                    candidates.add(new int[] {row, col});
                }
            }
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private void scoutRumorRegion(int centerRow, int centerCol, boolean relicClue) {
        ensureDiscoveryArrays();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int distance = Math.abs(row - centerRow) + Math.abs(col - centerCol);
                if (distance <= 1) {
                    if (state.discovery[row][col] == DiscoveryState.UNKNOWN) {
                        state.discovery[row][col] = DiscoveryState.SCOUTED;
                    }
                    if (relicClue) state.relicSearch[row][col] = true;
                }
            }
        }
    }

    private void ensureRumorServiceArray() {
        if (state.rumorServicesUsed == null || state.rumorServicesUsed.length != SIZE) {
            state.rumorServicesUsed = new boolean[SIZE][SIZE];
        }
    }

    boolean rumorServiceAvailableHere() {
        TileType tile = currentTile();
        if (tile != TileType.TAVERN && tile != TileType.ENCAMPMENT) return false;
        ensureRumorServiceArray();
        return !state.rumorServicesUsed[state.row][state.col];
    }

    private void completePlayerAction(Enemy enemy, ActionTempo tempo) {
        pendingEnemyTurnEvents.clear();
        ensureCombatTimeline(enemy);
        state.heroCombatClock += scaledDelay(tempo.cost, state.heroSpeed);
        state.consecutiveHeroActions++;

        int enemyActions = 0;
        boolean forceResponse = state.consecutiveHeroActions >= 2;
        while (state.health > 0 && enemy.health > 0 && enemyActions < 2 &&
                (state.enemyCombatClock <= state.heroCombatClock || forceResponse)) {
            EnemyAction action = nextEnemyAction(enemy, state.enemyActionSequence);
            boolean actingEarly = forceResponse && state.enemyCombatClock > state.heroCombatClock;
            int actedAt = actingEarly ? state.heroCombatClock : state.enemyCombatClock;
            boolean secondWindBefore = state.secondWindUsed;
            if (state.enemyPinned && isMeleeAction(action)) {
                int historyStart = history.length();
                state.enemyPinned = false;
                add("Pinning Shot prevents the " + enemy.name + " from reaching you with " +
                    action.name + ".");
                pendingEnemyTurnEvents.add(new EnemyTurnEvent(enemy.name,
                    "Pinned · " + action.name, action.tempo.label, state.health,
                    state.health, 0, true, historyStart, history.length()));
            } else if (state.enemyStunTurns > 0) {
                int historyStart = history.length();
                state.enemyStunTurns--;
                add("The " + enemy.name + " is stunned and loses its " + action.name + " turn.");
                pendingEnemyTurnEvents.add(new EnemyTurnEvent(enemy.name,
                    "Stunned · " + action.name, action.tempo.label, state.health,
                    state.health, 0, true, historyStart, history.length()));
            } else {
                enemyTurn(enemy, action);
            }
            tickEnemyEffects(enemy);
            enemyActions++;
            state.enemyActionSequence++;
            state.consecutiveHeroActions = 0;
            forceResponse = false;
            EnemyAction following = nextEnemyAction(enemy, state.enemyActionSequence);
            state.enemyCombatClock = actedAt + scaledDelay(following.tempo.cost, state.enemySpeed);
            // A death-save must hand control back to the player; otherwise a
            // fast enemy's second queued hit can erase the passive before the
            // player ever experiences the promised last stand.
            if (!secondWindBefore && state.secondWindUsed) break;
        }
        if (enemyActions == 2 && state.enemyCombatClock <= state.heroCombatClock) {
            state.heroCombatClock = Math.max(0, state.enemyCombatClock - 1);
        }
        updateCombatPreview(enemy);
    }

    /** Enemy-turn durations make setup readable and prevent fast heroes consuming
     * their own effects before the opponent has visibly reacted. */
    private void tickEnemyEffects(Enemy enemy) {
        if (enemy == null || enemy.health <= 0 || state.health <= 0) return;
        if (state.enemyBleedTurns > 0) {
            int bleed = Math.max(1, state.enemyBleedDamage);
            enemy.health = Math.max(0, enemy.health - bleed);
            state.enemyBleedTurns--;
            add("The " + enemy.name + " bleeds for " + bleed + " damage (" +
                state.enemyBleedTurns + " turns remaining).");
            if (state.enemyBleedTurns == 0) state.enemyBleedDamage = 0;
            if (enemy.health == 0) {
                defeatEnemy(enemy);
                return;
            }
        }
        if (state.enemyArmorBreakTurns > 0) {
            state.enemyArmorBreakTurns--;
            if (state.enemyArmorBreakTurns == 0) {
                state.enemyArmorBreakValue = 0;
                add("The " + enemy.name + " recovers its armor stance.");
            }
        }
    }

    private void enemyTurn(Enemy enemy, EnemyAction action) {
        int healthBefore = state.health;
        int historyStart = history.length();
        if ("STEALTH".equals(state.combatPreparation)) {
            int evadeChance = isBoss(enemy) ? 35 : 60;
            if (random.nextInt(100) < evadeChance) {
                state.defending = false;
                gainMomentum(24, "evasion");
                add("The " + enemy.name + " uses " + action.name +
                    ", strikes at shadows, and misses you.");
                pendingEnemyTurnEvents.add(new EnemyTurnEvent(enemy.name, action.name,
                    action.tempo.label, healthBefore, state.health, 0, true,
                    historyStart, history.length()));
                return;
            }
        }
        if ("Hunter".equals(state.heroClass)) {
            int speedEdge = Math.max(0, state.heroSpeed - state.enemySpeed);
            int missChance = Math.min(30, 10 + speedEdge * 3);
            if (random.nextInt(100) < missChance) {
                gainFocus(15, "enemy miss");
                add("The " + enemy.name + " uses " + action.name +
                    " but misses as you maintain your firing line.");
                pendingEnemyTurnEvents.add(new EnemyTurnEvent(enemy.name, action.name,
                    action.tempo.label, healthBefore, state.health, 0, true,
                    historyStart, history.length()));
                return;
            }
        }
        int damage = Math.max(1, enemy.attack + random.nextInt(5) - 2 +
            action.damageModifier);
        if (state.defending) {
            if ("Mage".equals(state.heroClass)) {
                damage = Math.max(1, damage * 7 / 10);
            } else if ("Hunter".equals(state.heroClass)) {
                damage = Math.max(1, damage * 4 / 5);
            } else if ("Fighter".equals(state.heroClass)) {
                damage = Math.max(1, damage / 2);
            }
        }
        damage = Math.max(1, damage - totalDefense());
        if (isBoss(enemy)) {
            damage = Math.max(1, damage - bossRelicGuard());
        }
        state.defending = false;
        int healthAfterHit = Math.max(0, state.health - damage);
        boolean secondWindTriggered = healthAfterHit == 0 &&
            "Fighter".equals(state.heroClass) && state.level >= 3 &&
            !state.secondWindUsed;
        state.health = secondWindTriggered ? 1 : healthAfterHit;
        int appliedDamage = Math.max(0, healthBefore - state.health);
        if ("AIM".equals(state.combatPreparation) && appliedDamage > 0) {
            state.combatPreparation = null;
            add("The hit disrupts your prepared Aim.");
        }
        add("The " + enemy.name + " uses " + action.name + " (" +
            action.tempo.label + ") and hits you for " + damage + " damage.");
        if (secondWindTriggered) {
            state.secondWindUsed = true;
            state.rage = state.maxRage;
            state.combatPreparation = "RAGE";
            state.combatStatus = "SECOND WIND · 1 HEALTH · FULL RAGE";
            add("Second Wind! Adrenaline keeps you at 1 health, fills Rage, and empowers your next attack.");
        } else {
            gainRage(Math.min(24, 8 + appliedDamage / 2), "damage");
        }
        pendingEnemyTurnEvents.add(new EnemyTurnEvent(enemy.name, action.name,
            action.tempo.label, healthBefore, state.health, appliedDamage, false,
            historyStart, history.length()));
        if (state.health == 0) {
            add("You have fallen. Start a new quest or load a saved game.");
        }
    }

    private void ensureCombatTimeline(Enemy enemy) {
        if (enemy == null) return;
        if (state.combatTimelineRow == state.row && state.combatTimelineCol == state.col &&
                state.heroSpeed > 0 && state.enemySpeed > 0) return;
        state.combatTimelineRow = state.row;
        state.combatTimelineCol = state.col;
        state.heroCombatClock = 0;
        state.consecutiveHeroActions = 0;
        state.enemyActionSequence = 0;
        state.heroSpeed = effectiveHeroSpeed(enemy);
        state.enemySpeed = effectiveEnemySpeed(enemy);
        EnemyAction opening = nextEnemyAction(enemy, 0);
        state.enemyCombatClock = scaledDelay(opening.tempo.cost, state.enemySpeed);
        updateCombatPreview(enemy);
    }

    private void resetCombatTimeline() {
        state.heroCombatClock = 0;
        state.enemyCombatClock = 0;
        state.consecutiveHeroActions = 0;
        state.secondWindUsed = false;
        state.rage = 0;
        state.momentum = 0;
        state.enemyActionSequence = 0;
        state.combatTimelineRow = -1;
        state.combatTimelineCol = -1;
        state.heroSpeed = 0;
        state.enemySpeed = 0;
        state.enemyIntent = null;
        state.combatTimeline = null;
        state.combatStatus = null;
        state.enemyStunTurns = 0;
        state.enemyBleedTurns = 0;
        state.enemyBleedDamage = 0;
        state.enemyArmorBreakTurns = 0;
        state.enemyArmorBreakValue = 0;
        state.enemyMarked = false;
        state.enemyPinned = false;
        if ("Hunter".equals(state.heroClass)) {
            state.maxFocus = maxFocusForLevel(state.level);
            state.focus = Math.max(state.focus, state.maxFocus / 3);
        } else {
            state.focus = 0;
        }
    }

    private void updateCombatPreview(Enemy enemy) {
        if (enemy == null) {
            state.enemyIntent = null;
            state.combatTimeline = null;
            return;
        }
        EnemyAction next = nextEnemyAction(enemy, state.enemyActionSequence);
        state.enemyIntent = next.name + " · " + next.tempo.label;
        int normalHeroReady = state.heroCombatClock + scaledDelay(
            ActionTempo.NORMAL.cost, state.heroSpeed);
        boolean bonusLikely = state.consecutiveHeroActions < 1 &&
            normalHeroReady < state.enemyCombatClock;
        state.combatTimeline = bonusLikely
            ? "YOU · READY  →  YOU · BONUS  →  " + enemy.name.toUpperCase()
            : "YOU · READY  →  " + enemy.name.toUpperCase() + " · " + next.tempo.label +
                "  →  YOU";
    }

    private int effectiveHeroSpeed(Enemy enemy) {
        int speed = baseHeroSpeed(state.heroClass) + raceSpeedModifier(state.race);
        int levelDifference = state.level - enemy.combatLevel;
        speed += Math.max(-10, Math.min(10, levelDifference * 2));
        if ("Chain Armour".equals(state.equippedArmour)) speed -= 5;
        if ("Scale Armour".equals(state.equippedArmour)) speed -= 4;
        if ("Plate Armour".equals(state.equippedArmour)) speed -= 8;
        if ("Reinforced Scale".equals(state.equippedArmour)) speed -= 4;
        if ("Knight Plate".equals(state.equippedArmour)) speed -= 8;
        return Math.max(55, speed);
    }

    private int effectiveEnemySpeed(Enemy enemy) {
        int tierModifier = enemy.tier == 2 ? 12 : (enemy.tier == 1 ? 8 : 0);
        return Math.max(55, enemy.speed + tierModifier);
    }

    private static int scaledDelay(int actionCost, int speed) {
        return Math.max(35, (actionCost * 100 + Math.max(1, speed) - 1) /
            Math.max(1, speed));
    }

    private static int baseHeroSpeed(String heroClass) {
        if ("Rogue".equals(heroClass)) return 120;
        if ("Hunter".equals(heroClass)) return 110;
        if ("Mage".equals(heroClass)) return 90;
        return 100;
    }

    private static int raceSpeedModifier(String race) {
        if ("Dwarf".equals(race)) return -8;
        if ("Elf".equals(race)) return 5;
        if ("Halfling".equals(race)) return 8;
        return 0;
    }

    private static ActionTempo attackTempo(State state) {
        String weapon = state == null ? null : state.equippedWeapon;
        if ("Dagger".equals(weapon) || "Serrated Dagger".equals(weapon) ||
                "Short Sword".equals(weapon) ||
                (weapon != null && weapon.endsWith("Dirk"))) {
            return ActionTempo.FAST;
        }
        if ("Axe".equals(weapon) || "Crossbow".equals(weapon) ||
                "Hunting Crossbow".equals(weapon) || "Greatsword".equals(weapon) ||
                "Tempered Greatsword".equals(weapon) ||
                "Iron Mace".equals(weapon) || "Flanged Mace".equals(weapon) ||
                "Warhammer".equals(weapon)) {
            return ActionTempo.SLOW;
        }
        return ActionTempo.NORMAL;
    }

    private static ActionTempo spellTempo(String spell) {
        if ("Fireball".equals(spell) || "Ice Spike".equals(spell)) {
            return ActionTempo.SLOW;
        }
        return ActionTempo.NORMAL;
    }

    static String attackTempoLabel(State state) {
        return attackTempo(state).label;
    }

    static String spellTempoLabel(String spell) {
        return spellTempo(spell).label;
    }

    private int traitChance(int baseChance) {
        int rank = Math.max(1, equippedWeaponProficiency(state));
        int percent = rank == 1 ? 70 : (rank == 2 ? 100 : 135);
        return Math.min(85, Math.max(1, baseChance * percent / 100));
    }

    /** Stable weapon-category traits shared by combat, inventory, and shops. */
    static String weaponTraitName(Item item) {
        if (item == null || !"Weapon".equals(item.type)) return "UNARMED";
        String name = item.name.toLowerCase();
        if (name.contains("crossbow")) return "ARMOR PIERCING";
        if (name.contains("mace") || name.contains("hammer")) return "CONCUSSIVE";
        if (name.contains("axe") || name.contains("greatsword")) return "SUNDERING";
        if (name.contains("dagger") || name.contains("dirk")) return "BLEEDING EDGE";
        if (name.contains("bow") || name.contains("recurve")) return "PRECISE";
        if (name.contains("staff") || name.contains("wand")) return "ARCANE FOCUS";
        if (name.contains("sword")) return "BALANCED GUARD";
        return "VERSATILE";
    }

    static String weaponTraitDescription(Item item) {
        String trait = weaponTraitName(item);
        if ("ARMOR PIERCING".equals(trait)) return "Ignores up to 3 enemy defense";
        if ("CONCUSSIVE".equals(trait)) return "Can stun and cancel an enemy action";
        if ("SUNDERING".equals(trait)) return "Breaks through up to 2 enemy defense";
        if ("BLEEDING EDGE".equals(trait)) return "Can inflict bonus wound damage";
        if ("PRECISE".equals(trait)) return "Can strike an exposed weak point";
        if ("ARCANE FOCUS".equals(trait)) return "Adds weapon focus to spell damage";
        if ("BALANCED GUARD".equals(trait)) return "Can flow into a defensive stance";
        if ("VERSATILE".equals(trait)) return "Reliable damage with normal action speed";
        return "No equipped weapon trait";
    }

    static String weaponFamily(Item item) {
        if (item == null || !"Weapon".equals(item.type)) return null;
        String name = item.name.toLowerCase();
        if (name.contains("mace") || name.contains("hammer")) return "MACE & HAMMER";
        if (name.contains("greatsword") || name.contains("axe")) return "HEAVY BLADE";
        if (name.contains("dagger") || name.contains("dirk")) return "DAGGER";
        if (name.contains("crossbow")) return "CROSSBOW";
        if (name.contains("bow") || name.contains("recurve")) return "BOW";
        if (name.contains("staff") || name.contains("wand")) return "ARCANE FOCUS";
        if (name.contains("sword") || name.contains("blade")) return "SWORD";
        return "MARTIAL";
    }

    static int weaponProficiencyRank(State state, String family) {
        if (state == null || family == null || state.weaponProficiency == null) return 0;
        Integer rank = state.weaponProficiency.get(family);
        return rank == null ? 0 : Math.max(0, Math.min(3, rank.intValue()));
    }

    static int equippedWeaponProficiency(State state) {
        Item weapon = equippedWeapon(state);
        return weaponProficiencyRank(state, weaponFamily(weapon));
    }

    static String proficiencyLabel(int rank) {
        if (rank >= 3) return "MASTERED";
        if (rank >= 2) return "PROFICIENT";
        if (rank >= 1) return "TRAINED";
        return "UNTRAINED";
    }

    static Item equippedWeapon(State state) {
        if (state == null || state.inventory == null || state.equippedWeapon == null) return null;
        for (Item item : state.inventory) {
            if (state.equippedWeapon.equals(item.name)) return item;
        }
        return null;
    }

    static boolean abilityUnlocked(State state, int slot) {
        if (state == null || slot < 1 || slot > 3) return false;
        if ("Mage".equals(state.heroClass)) return slot == 1 || state.level >= slot;
        if (slot == 1) return state.level >= 2;
        if (slot == 2) return state.level >= 3 && equippedWeaponProficiency(state) >= 3;
        return state.level >= 3;
    }

    /** Separates permanent level progression from encounter-specific readiness. */
    static boolean abilityAvailable(State state, int slot) {
        if (!abilityUnlocked(state, slot)) return false;
        if (state != null && slot == 3 && "Fighter".equals(state.heroClass)) {
            return !state.secondWindUsed;
        }
        if (state != null && "Fighter".equals(state.heroClass) && slot < 3) {
            return state.rage >= rageCost(slot);
        }
        if (state != null && "Rogue".equals(state.heroClass)) {
            return state.momentum >= momentumCost(slot);
        }
        if (state != null && "Hunter".equals(state.heroClass)) {
            return state.focus >= hunterFocusCost(state, slot) &&
                (!"Volley".equals(abilityName(state, slot)) || state.enemyMarked) &&
                (slot != 3 || !state.enemyMarked);
        }
        return true;
    }

    static boolean abilityPassive(State state, int slot) {
        return state != null && slot == 3 && "Fighter".equals(state.heroClass);
    }

    static String abilityName(State state, int slot) {
        if (state == null) return "Locked Ability";
        if ("Mage".equals(state.heroClass)) {
            return slot == 1 ? "Magic Missile" : (slot == 2 ? "Fireball" : "Ice Spike");
        }
        if (slot == 1) {
            if ("Fighter".equals(state.heroClass)) {
                if (state.equippedOffhand != null && state.equippedOffhand.contains("Shield")) {
                    return "Shield Bash";
                }
                return usesTwoHandedWeapon(state) ? "Cleave" : "Power Strike";
            }
            if ("Rogue".equals(state.heroClass)) return "Offhand Strike";
            return "Pinning Shot";
        }
        if (slot == 2) return weaponTechniqueName(state);
        if ("Fighter".equals(state.heroClass)) return "Second Wind";
        if ("Rogue".equals(state.heroClass)) return "Execute";
        return "Hunter's Mark";
    }

    static String abilityRequirement(State state, int slot) {
        if (abilityUnlocked(state, slot)) {
            if (state != null && slot == 3 && "Fighter".equals(state.heroClass)) {
                return state.secondWindUsed
                    ? "PASSIVE · SPENT THIS ENCOUNTER"
                    : "PASSIVE · ARMED";
            }
            if (state != null && "Fighter".equals(state.heroClass)) {
                int cost = rageCost(slot);
                return state.rage >= cost
                    ? "READY · " + cost + " RAGE"
                    : "NEEDS " + cost + " RAGE · " + state.rage + "/" + state.maxRage;
            }
            if (state != null && "Rogue".equals(state.heroClass)) {
                int cost = momentumCost(slot);
                return state.momentum >= cost
                    ? "READY · " + cost + " MOMENTUM"
                    : "NEEDS " + cost + " MOMENTUM · " +
                        state.momentum + "/" + state.maxMomentum;
            }
            if (state != null && "Hunter".equals(state.heroClass)) {
                int cost = hunterFocusCost(state, slot);
                if (slot == 3 && state.enemyMarked) return "TARGET ALREADY MARKED";
                if ("Volley".equals(abilityName(state, slot)) && !state.enemyMarked) {
                    return "REQUIRES HUNTER'S MARK · " + cost + " FOCUS";
                }
                return state.focus >= cost
                    ? "READY · " + cost + " FOCUS"
                    : "NEEDS " + cost + " FOCUS · " + state.focus + "/" + state.maxFocus;
            }
            return "READY";
        }
        if (state != null && "Mage".equals(state.heroClass)) {
            return "UNLOCKS LEVEL " + slot;
        }
        if (slot == 1) return "UNLOCKS LEVEL 2";
        if (slot == 2) return state != null && state.level < 3
            ? "UNLOCKS LEVEL 3" : "MASTER EQUIPPED WEAPON";
        return "UNLOCKS LEVEL 3";
    }

    /** Short, loadout-aware roadmap shared by creation and character screens. */
    static String abilityProgressionSummary(State state) {
        if (state == null) return "No progression available.";
        if ("Mage".equals(state.heroClass)) {
            return "L1 " + abilityName(state, 1) + "<br>L2 " + abilityName(state, 2) +
                "<br>L3 " + abilityName(state, 3);
        }
        return "L1 Core training<br>L2 " + abilityName(state, 1) +
            "<br>L3 " + abilityName(state, 2) + " + " + abilityName(state, 3) +
            ("Fighter".equals(state.heroClass) ? " (Passive)" : "");
    }

    static String abilityTempoLabel(State state, int slot) {
        String name = abilityName(state, slot);
        if ("Second Wind".equals(name)) return "PASSIVE";
        if ("Hunter's Mark".equals(name) ||
                "Offhand Strike".equals(name)) return ActionTempo.FAST.label;
        if ("Cleave".equals(name) || "Execute".equals(name) ||
                "Ice Spike".equals(name) || "Fireball".equals(name)) {
            return ActionTempo.SLOW.label;
        }
        return ActionTempo.NORMAL.label;
    }

    /**
     * Builds the authoritative ability help shown on both mouse hover and keyboard
     * focus. Keeping this beside the combat rules prevents UI copy from drifting
     * away from loadout-dependent names, costs, timing, and estimated outcomes.
     */
    static String abilityHelp(State state, Enemy enemy, int slot, boolean html) {
        String name = abilityName(state, slot);
        String requirement = abilityRequirement(state, slot);
        String cost = abilityPassive(state, slot) ? "Automatic" :
            ("Mage".equals(state == null ? null : state.heroClass)
                ? spellCost(name) + " MP" :
            ("Fighter".equals(state == null ? null : state.heroClass)
                ? rageCost(slot) + " Rage" :
            ("Rogue".equals(state == null ? null : state.heroClass)
                ? momentumCost(slot) + " Momentum" :
            ("Hunter".equals(state == null ? null : state.heroClass)
                ? (hunterFocusCost(state, slot) == 0
                    ? "Setup action" : hunterFocusCost(state, slot) + " Focus")
                : "No resource"))));
        String effect = abilityEffect(state, enemy, slot);
        String estimate = abilityOutcomeEstimate(state, enemy, slot);
        if (html) {
            return "<html><b>" + name + "</b><br>Cost: " + cost +
                " · Tempo: " + abilityTempoLabel(state, slot) +
                "<br>Effect: " + effect + "<br>Estimated: " + estimate +
                "<br>Status: " + requirement + "</html>";
        }
        return name + ". Cost: " + cost + ". Tempo: " +
            abilityTempoLabel(state, slot) + ". Effect: " + effect +
            ". Estimated: " + estimate + ". Status: " + requirement + ".";
    }

    static String abilityFocusSummary(State state, Enemy enemy, int slot) {
        return abilityName(state, slot).toUpperCase() + " · " +
            abilityTempoLabel(state, slot) + " · " + abilityOutcomeEstimate(state, enemy, slot);
    }

    /** Compact, duration-bearing labels consumed by the combat component. */
    static List<String> activeCombatStatuses(State state) {
        ArrayList<String> result = new ArrayList<String>();
        if (state == null) return result;
        if ("Fighter".equals(state.heroClass) && state.level >= 3) {
            result.add("HERO · SECOND WIND · " +
                (state.secondWindUsed ? "SPENT" : "ARMED"));
        }
        if ("Fighter".equals(state.heroClass) && state.maxRage > 0)
            result.add("HERO · RAGE " + state.rage + "/" + state.maxRage);
        if ("Rogue".equals(state.heroClass) && state.maxMomentum > 0)
            result.add("HERO · MOMENTUM " + state.momentum + "/" + state.maxMomentum);
        if ("Hunter".equals(state.heroClass) && state.maxFocus > 0)
            result.add("HERO · FOCUS " + state.focus + "/" + state.maxFocus);
        if (state.enemyMarked) result.add("ENEMY · MARKED · UNTIL CONSUMED");
        if (state.enemyPinned) result.add("ENEMY · PINNED · NEXT MELEE");
        if (state.defending) result.add("HERO · GUARD · NEXT HIT");
        if (state.combatPreparation != null) {
            String preparation = state.combatPreparation;
            String duration = "MARK".equals(preparation) || "AIM".equals(preparation) ||
                "RAGE".equals(preparation) || "STEALTH".equals(preparation)
                ? "NEXT ATTACK" : "ACTIVE";
            result.add("HERO · " + preparation + " · " + duration);
        }
        if (state.enemyStunTurns > 0)
            result.add("ENEMY · STUN · " + state.enemyStunTurns + " TURN");
        if (state.enemyBleedTurns > 0)
            result.add("ENEMY · BLEED " + state.enemyBleedDamage + " · " +
                state.enemyBleedTurns + " TURNS");
        if (state.enemyArmorBreakTurns > 0)
            result.add("ENEMY · ARMOR −" + state.enemyArmorBreakValue + " · " +
                state.enemyArmorBreakTurns + " TURNS");
        return result;
    }

    private static String abilityEffect(State state, Enemy enemy, int slot) {
        String name = abilityName(state, slot);
        if ("Shield Bash".equals(name)) return enemy != null && enemy.tier == 2
            ? "Strike and enter Guard; bosses resist the stun"
            : "Strike, enter Guard, and stun the target";
        if ("Cleave".equals(name)) return "Heavy strike that ignores 2 defense";
        if ("Power Strike".equals(name)) return "A strong direct weapon strike";
        if ("Offhand Strike".equals(name)) return "Fast strike amplified by your offhand";
        if ("Pinning Shot".equals(name))
            return "Ranged strike that prevents the target's next melee action";
        if ("Stunning Blow".equals(name)) return "Mastery strike that can stun non-bosses";
        if ("Armor Breaker".equals(name) || "Piercing Bolt".equals(name))
            return "Mastery strike that ignores 4 defense";
        if ("Blade Flurry".equals(name)) return "Fast mastery strike with bonus damage";
        if ("Volley".equals(name)) return "Precise mastery strike with bonus damage";
        if ("Riposte".equals(name)) return "Mastery strike that also enters Guard";
        if ("Second Wind".equals(name))
            return "Passive: survive one fatal hit at 1 health and enter Rage";
        if ("Hunter's Mark".equals(name))
            return "Mark the target; required and consumed by Volley";
        if ("Execute".equals(name)) return "Slow strike; much stronger below 35% enemy health";
        return "Magic Missile".equals(name) ? "Reliable arcane damage that bypasses defense" :
            ("Fireball".equals(name) ? "Heavy fire damage that bypasses defense" :
            ("Ice Spike".equals(name) ? "Devastating ice damage that bypasses defense" :
            "Loadout-aware combat technique"));
    }

    private static String abilityOutcomeEstimate(State state, Enemy enemy, int slot) {
        if (state == null) return "depends on current equipment";
        String name = abilityName(state, slot);
        if (!abilityUnlocked(state, slot)) return abilityRequirement(state, slot).toLowerCase();
        if ("Second Wind".equals(name)) {
            return state.secondWindUsed
                ? "death save already spent this encounter"
                : "next fatal hit leaves 1 health; next basic attack gains Rage";
        }
        if ("Hunter's Mark".equals(name)) {
            return "target becomes eligible for Volley and guided basic shots";
        }
        if ("Mage".equals(state.heroClass)) {
            int minimum = "Fireball".equals(name) ? 21 : ("Ice Spike".equals(name) ? 25 : 13);
            int maximum = minimum + ("Magic Missile".equals(name) ? 4 : 6);
            int fixed = (state.level - 1) * 2 + arcaneEquipmentBonus(state);
            if (enemy != null && enemy.tier == 2) fixed += relicDamageBonus(state);
            return (minimum + fixed) + "–" + (maximum + fixed) + " damage";
        }
        int raw = stateTotalAttack(state);
        int pierce = 0;
        if ("Cleave".equals(name)) { raw += 6; pierce = 2; }
        else if ("Power Strike".equals(name)) raw += 5;
        else if ("Offhand Strike".equals(name)) raw += 2 +
            Math.max(2, equippedItem(state, state.equippedOffhand) == null ? 0 :
                equippedItem(state, state.equippedOffhand).attack);
        else if ("Shield Bash".equals(name) || "Pinning Shot".equals(name)) raw += 2;
        else if (slot == 2) {
            raw += 4;
            String trait = weaponTraitName(equippedWeapon(state));
            if ("SUNDERING".equals(trait) || "ARMOR PIERCING".equals(trait)) pierce = 4;
            if ("BLEEDING EDGE".equals(trait) || "PRECISE".equals(trait)) raw += 3;
        } else if ("Execute".equals(name)) {
            boolean vulnerable = enemy != null && enemy.health <=
                Math.max(1, enemy.maxHealth * 35 / 100);
            raw += vulnerable ? 11 : 3;
            pierce = 1;
        }
        if (enemy != null && enemy.tier == 2) raw += relicDamageBonus(state);
        int armor = enemy == null ? 0 : Math.max(0, enemy.defense - pierce);
        return Math.max(1, raw - armor) + " damage";
    }

    private static int stateTotalAttack(State state) {
        Item weapon = equippedItem(state, state.equippedWeapon);
        Item offhand = equippedItem(state, state.equippedOffhand);
        return state.baseAttack + state.level - 1 + (weapon == null ? 0 : weapon.attack) +
            (offhand == null ? 0 : offhand.attack);
    }

    private static int arcaneEquipmentBonus(State state) {
        Item weapon = equippedWeapon(state);
        int bonus = 0;
        if ("ARCANE FOCUS".equals(weaponTraitName(weapon))) {
            int rank = Math.max(1, equippedWeaponProficiency(state));
            bonus += Math.max(1, weapon.attack * (rank + 1) / 6);
        }
        Item offhand = equippedItem(state, state.equippedOffhand);
        return bonus + (offhand == null ? 0 : offhand.attack * 2);
    }

    private static Item equippedItem(State state, String name) {
        if (state != null && name != null && state.inventory != null) {
            for (Item item : state.inventory) if (name.equals(item.name)) return item;
        }
        return null;
    }

    private static int relicDamageBonus(State state) {
        int count = 0;
        if (state.relics != null) for (Relic relic : state.relics) if (relic.identified) count++;
        int perRelic = ("Mage".equals(state.heroClass) || "Hunter".equals(state.heroClass))
            ? 6 : ("Fighter".equals(state.heroClass) ? 5 : 4);
        return count * perRelic;
    }

    private static String weaponTechniqueName(State state) {
        String trait = weaponTraitName(equippedWeapon(state));
        if ("CONCUSSIVE".equals(trait)) return "Stunning Blow";
        if ("SUNDERING".equals(trait)) return "Armor Breaker";
        if ("BLEEDING EDGE".equals(trait)) return "Blade Flurry";
        if ("PRECISE".equals(trait)) return "Volley";
        if ("ARMOR PIERCING".equals(trait)) return "Piercing Bolt";
        if ("BALANCED GUARD".equals(trait)) return "Riposte";
        return "Masterful Strike";
    }

    void useAbility(int slot) {
        if (!abilityUnlocked(state, slot)) {
            add(abilityRequirement(state, slot) + ".");
            return;
        }
        if (abilityPassive(state, slot)) {
            add("Second Wind is passive and triggers automatically on a fatal hit.");
            return;
        }
        if (!abilityAvailable(state, slot)) {
            add(abilityRequirement(state, slot) + ".");
            return;
        }
        if ("Mage".equals(state.heroClass)) {
            castSpell(abilityName(state, slot));
            return;
        }
        if (!canAct()) return;
        Enemy enemy = currentEnemy();
        if (enemy == null) {
            add("There is no target for " + abilityName(state, slot) + ".");
            return;
        }
        if ("Fighter".equals(state.heroClass)) {
            int cost = rageCost(slot);
            state.rage = Math.max(0, state.rage - cost);
            add("You spend " + cost + " Rage (" + state.rage + "/" + state.maxRage + ").");
        } else if ("Rogue".equals(state.heroClass)) {
            int cost = momentumCost(slot);
            state.momentum = Math.max(0, state.momentum - cost);
            add("You spend " + cost + " Momentum (" + state.momentum + "/" +
                state.maxMomentum + ").");
        } else if ("Hunter".equals(state.heroClass)) {
            int cost = hunterFocusCost(state, slot);
            state.focus = Math.max(0, state.focus - cost);
            if (cost > 0) {
                add("You spend " + cost + " Focus (" + state.focus + "/" +
                    state.maxFocus + ").");
            }
        }
        if (slot == 1) useTacticalAbility(enemy);
        else if (slot == 2) useWeaponTechnique(enemy);
        else useSignatureAbility(enemy);
    }

    private void useTacticalAbility(Enemy enemy) {
        String ability = abilityName(state, 1);
        int raw = totalAttack() + 2;
        int pierce = 0;
        ActionTempo tempo = ActionTempo.NORMAL;
        if ("Shield Bash".equals(ability)) {
            state.enemyStunTurns = isBoss(enemy) ? 0 : 1;
            state.defending = true;
            state.combatStatus = "SHIELD BASH · " +
                (state.enemyStunTurns > 0 ? "ENEMY STUNNED" : "GUARD ACTIVE");
        } else if ("Cleave".equals(ability)) {
            raw += 4;
            pierce = 2;
            tempo = ActionTempo.SLOW;
            state.combatStatus = "CLEAVE · ARMOR SUNDERED";
        } else if ("Power Strike".equals(ability)) {
            raw += 3;
            state.combatStatus = "POWER STRIKE · HEAVY HIT";
        } else if ("Offhand Strike".equals(ability)) {
            Item offhand = findItem(state.equippedOffhand);
            raw += offhand == null ? 1 : Math.max(2, offhand.attack);
            tempo = ActionTempo.FAST;
            state.combatStatus = "OFFHAND STRIKE · FAST";
        } else {
            raw += 2;
            state.enemyPinned = true;
            state.combatStatus = "PINNING SHOT · NEXT MELEE PREVENTED";
        }
        resolveAbilityDamage(enemy, ability, raw, pierce, tempo);
    }

    private void useWeaponTechnique(Enemy enemy) {
        String ability = abilityName(state, 2);
        String trait = weaponTraitName(findItem(state.equippedWeapon));
        int raw = totalAttack() + 4;
        int pierce = 0;
        ActionTempo tempo = ActionTempo.NORMAL;
        if ("CONCUSSIVE".equals(trait)) {
            if (!isBoss(enemy)) state.enemyStunTurns = 1;
        } else if ("SUNDERING".equals(trait) || "ARMOR PIERCING".equals(trait)) {
            pierce = 4;
            tempo = ActionTempo.SLOW;
            if ("SUNDERING".equals(trait)) applyArmorBreak(4, 2);
        } else if ("BLEEDING EDGE".equals(trait)) {
            raw += 3;
            tempo = ActionTempo.FAST;
            applyBleed(isBoss(enemy) ? 2 : 3, 2);
        } else if ("BALANCED GUARD".equals(trait)) {
            state.defending = true;
        } else if ("PRECISE".equals(trait)) {
            raw += 3;
        }
        if ("Volley".equals(ability)) {
            raw += 5 + state.level;
            state.enemyMarked = false;
            state.combatStatus = "VOLLEY · MARK CONSUMED";
        } else {
            state.combatStatus = ability.toUpperCase() + " · MASTERY";
        }
        resolveAbilityDamage(enemy, ability, raw, pierce, tempo);
    }

    private void useSignatureAbility(Enemy enemy) {
        String ability = abilityName(state, 3);
        if ("Second Wind".equals(ability)) {
            add("Second Wind is passive and triggers automatically on a fatal hit.");
            return;
        }
        if ("Hunter's Mark".equals(ability)) {
            state.enemyMarked = true;
            state.defending = true;
            state.combatStatus = "HUNTER'S MARK · VOLLEY ENABLED";
            add("You mark the " + enemy.name + "; Volley can now lock onto the target.");
            completePlayerAction(enemy, ActionTempo.FAST);
            return;
        }
        int threshold = Math.max(1, enemy.maxHealth * 35 / 100);
        int bonus = enemy.health <= threshold ? 11 : 3;
        state.combatStatus = enemy.health <= threshold
            ? "EXECUTE · VULNERABLE TARGET" : "EXECUTE · TARGET RESISTED";
        resolveAbilityDamage(enemy, ability, totalAttack() + bonus, 1, ActionTempo.SLOW);
    }

    private void resolveAbilityDamage(Enemy enemy, String ability, int rawDamage,
                                      int armorPierce, ActionTempo tempo) {
        if (isBoss(enemy)) rawDamage += bossRelicDamageBonus();
        int armour = Math.max(0, effectiveEnemyDefense(enemy) - Math.max(0, armorPierce));
        int damage = Math.max(1, rawDamage - armour);
        enemy.health = Math.max(0, enemy.health - damage);
        add("You use " + ability + " on the " + enemy.name + " for " + damage + " damage.");
        if (enemy.health == 0) defeatEnemy(enemy);
        else completePlayerAction(enemy, tempo);
    }

    static String enemyBattleCry(String enemyName) {
        if (enemyName == null) return "You should not have come this far.";
        if (enemyName.contains("Dragon")) return "Mortal, your courage will freeze to ash.";
        if ("Orc Warlord".equals(enemyName)) return "Kneel now—or feed the crows!";
        if ("Necromancer".equals(enemyName)) return "Your final breath already belongs to me.";
        if ("Dark Elf Assassin".equals(enemyName)) return "You noticed me one heartbeat too late.";
        if ("Fallen Knight".equals(enemyName)) return "No oath remains but your destruction.";
        if ("Cultist Mage".equals(enemyName)) return "The old flame hungers for your name.";
        if ("Ashweb Matriarch".equals(enemyName))
            return "A piercing shriek rolls through the webbed hollow.";
        if ("Bandit Marauder".equals(enemyName)) return "Drop your gold and I may leave you breathing.";
        if ("Skeletal Guardian".equals(enemyName))
            return "Ancient joints grind as the guardian lowers its rusted blade.";
        if ("Dire Wolf".equals(enemyName))
            return "The wolf lowers its head. A savage growl rolls through the trees.";
        if ("Giant Forest Spider".equals(enemyName) || "Giant Spider".equals(enemyName))
            return "The spider drums its legs against the web and darts into striking range.";
        if ("Swamp Serpent".equals(enemyName))
            return "The serpent coils above the black water, tongue tasting the air.";
        if ("Armored Boar".equals(enemyName))
            return "The boar paws the earth, then levels its plated tusks toward you.";
        if ("Cave Troll".equals(enemyName) || "Troll".equals(enemyName))
            return "The brute hunches forward and answers your approach with a guttural roar.";
        return "The enemy fixes its gaze on you and prepares to strike.";
    }

    /** Encounter copy distinguishes actual speech from readable creature behavior. */
    static boolean enemyUsesSpeech(String enemyName) {
        if (enemyName == null) return false;
        return enemyName.contains("Dragon") ||
            "Orc Warlord".equals(enemyName) ||
            "Necromancer".equals(enemyName) ||
            "Dark Elf Assassin".equals(enemyName) ||
            "Fallen Knight".equals(enemyName) ||
            "Cultist Mage".equals(enemyName) ||
            "Bandit Marauder".equals(enemyName);
    }

    static String enemyIntroductionRole(Enemy enemy) {
        if (enemy == null) return "ENCOUNTER";
        if (enemyUsesSpeech(enemy.name)) {
            return enemy.tier == 2 ? "BOSS THREAT" :
                (enemy.tier == 1 ? "ELITE CHALLENGE" : "BATTLE CRY");
        }
        return enemy.tier == 2 ? "ANCIENT PRESENCE" :
            (enemy.tier == 1 ? "ELITE CREATURE" : "CREATURE REACTION");
    }

    private static EnemyAction nextEnemyAction(Enemy enemy, int sequence) {
        String name = enemy.name == null ? "Enemy" : enemy.name;
        if (name.contains("Dragon")) {
            int phase = sequence % 3;
            if (phase == 2) return new EnemyAction("Breath Attack", ActionTempo.SLOW, 4);
            if (phase == 1) return new EnemyAction("Bite", ActionTempo.NORMAL, 1);
            return new EnemyAction("Claw", ActionTempo.FAST, -2);
        }
        if (name.contains("Mage") || "Necromancer".equals(name) || "Lich".equals(name)) {
            return sequence % 2 == 0
                ? new EnemyAction("Quick Hex", ActionTempo.FAST, -1)
                : new EnemyAction("Dark Ritual", ActionTempo.SLOW, 4);
        }
        if ("Dire Wolf".equals(name) || "Giant Forest Spider".equals(name) ||
                "Giant Spider".equals(name) || "Ashweb Matriarch".equals(name) ||
                "Dark Elf Assassin".equals(name) ||
                "Bandit Marauder".equals(name)) {
            return new EnemyAction("Quick Strike", ActionTempo.FAST, -1);
        }
        if ("Cave Troll".equals(name) || "Troll".equals(name) ||
                "Orc Warlord".equals(name) || "Minotaur".equals(name) ||
                "Fallen Knight".equals(name) || "Armored Boar".equals(name)) {
            return new EnemyAction("Heavy Strike", ActionTempo.SLOW, 3);
        }
        return new EnemyAction("Attack", ActionTempo.NORMAL, 0);
    }

    static boolean isMeleeAction(EnemyAction action) {
        return action != null && isMeleeActionName(action.name);
    }

    static boolean isMeleeActionName(String name) {
        if (name == null) return false;
        return !name.contains("Hex") && !name.contains("Ritual") &&
            !name.contains("Breath") && !name.contains("Shot") &&
            !name.contains("Bolt");
    }

    void buyPotion() {
        if (currentTile() == TileType.SHOP && isBlacksmithShop()) {
            add("The blacksmith does not stock potions.");
        } else if (state.gold < POTION_COST) {
            add("A potion costs " + POTION_COST + " gold. You cannot afford one.");
        } else {
            state.gold -= POTION_COST;
            state.potions++;
            add("You purchase a potion for " + POTION_COST + " gold.");
        }
    }

    List<Item> shopItems() {
        ArrayList<Item> items = new ArrayList<Item>();
        items.add(new Item("Dagger", "Weapon", 4, 0, 5));
        items.add(new Item("Long Sword", "Weapon", 7, 0, 15));
        items.add(new Item("Axe", "Weapon", 9, 0, 20));
        items.add(new Item("Long Bow", "Weapon", 8, 0, 30));
        items.add(new Item("Leather Armour", "Armour", 0, 2, 15));
        items.add(new Item("Scale Armour", "Armour", 0, 3, 25));
        items.add(new Item("Plate Armour", "Armour", 0, 6, 45));
        items.add(new Item("Short Sword", "Weapon", 6, 0, 12));
        items.add(new Item("Oak Staff", "Weapon", 4, 0, 15));
        items.add(new Item("Crossbow", "Weapon", 9, 0, 35));
        items.add(new Item("Cloth Armour", "Armour", 0, 1, 8));
        items.add(new Item("Steel Long Sword", "Weapon", 9, 0, 30, "Fighter", false,
            "UNCOMMON", false));
        items.add(new Item("Iron Mace", "Weapon", 6, 0, 12, "Fighter", false,
            "COMMON", false));
        items.add(new Item("Greatsword", "Weapon", 9, 0, 24, "Fighter", false,
            "UNCOMMON", false));
        if (state.level >= 2) {
            items.add(new Item("Flanged Mace", "Weapon", 9, 0, 30, "Fighter", false,
                "UNCOMMON", false));
            items.add(new Item("Tempered Greatsword", "Weapon", 10, 0, 38,
                "Fighter", false, "RARE", false));
        }
        if (state.level >= 3) {
            items.add(new Item("Warhammer", "Weapon", 11, 0, 46, "Fighter", false,
                "RARE", false));
        }
        items.add(new Item("Knight Plate", "Armour", 0, 7, 55, "Fighter", false,
            "RARE", false));
        items.add(new Item("Shadowsteel Dirk", "Weapon", 8, 0, 28, "Rogue", false,
            "UNCOMMON", false));
        items.add(new Item("Reinforced Leather", "Armour", 0, 4, 32,
            "Mage,Rogue,Hunter", false, "UNCOMMON", false));
        items.add(new Item("Ashwood Staff", "Weapon", 7, 0, 28, "Mage", false,
            "UNCOMMON", false));
        items.add(new Item("Mystic Robes", "Armour", 0, 3, 26, "Mage", false,
            "UNCOMMON", false));
        items.add(new Item("Ranger Bow", "Weapon", 10, 0, 38, "Hunter", false,
            "UNCOMMON", false));
        items.add(new Item("Reinforced Scale", "Armour", 0, 5, 40, "Hunter", false,
            "UNCOMMON", false));
        items.add(new Item("Iron Shield", "Offhand", 0, 2, 24, "Fighter", false,
            "UNCOMMON", false));
        items.add(new Item("Runed Tome", "Offhand", 2, 0, 24, "Mage", false,
            "UNCOMMON", false));
        items.add(new Item("Balanced Offhand Dagger", "Offhand", 3, 0, 24, "Rogue", false,
            "UNCOMMON", false));
        items.add(new Item("Hunter's Quiver", "Offhand", 1, 1, 24, "Hunter", false,
            "UNCOMMON", false));
        items.add(new Item("Runed Wand", "Weapon", 6, 0, 22, "Mage", false,
            "UNCOMMON", false));
        items.add(new Item("Serrated Dagger", "Weapon", 7, 0, 22, "Rogue", false,
            "UNCOMMON", false));
        items.add(new Item("Soldier Shield", "Offhand", 0, 3, 28, "Fighter", false,
            "UNCOMMON", false));
        if (state.level >= 2) {
            items.add(new Item("Hunting Crossbow", "Weapon", 9, 0, 28, "Hunter", false,
                "UNCOMMON", false));
            items.add(new Item("Carved Totem", "Offhand", 2, 0, 20, "Mage", false,
                "UNCOMMON", false));
        }
        return items;
    }

    void buyItem(Item item) {
        if (currentTile() != TileType.SHOP) {
            add("Equipment can only be purchased at a shop.");
        } else if (!vendorOffers(isBlacksmithShop(), item)) {
            add((isBlacksmithShop() ? "The blacksmith" : "The general merchant") +
                " does not stock " + item.name + ".");
        } else if (equipmentRestriction(state.heroClass, item) != null) {
            add(equipmentRestriction(state.heroClass, item));
        } else if (state.gold < item.cost) {
            add("You cannot afford the " + item.name + ".");
        } else {
            state.gold -= item.cost;
            state.inventory.add(item);
            add("You purchase the " + item.name + " for " + item.cost + " gold.");
        }
    }

    String sellRestriction(Item item) {
        if (item == null || state.inventory == null || !state.inventory.contains(item))
            return "NOT IN INVENTORY";
        if (item.starterItem) return "STARTER GEAR";
        if (item.relicReward) return "RELIC-BOUND";
        if (item.name.equals(state.equippedWeapon) || item.name.equals(state.equippedArmour) ||
                item.name.equals(state.equippedOffhand))
            return "EQUIPPED";
        return null;
    }

    int salePrice(Item item) {
        return salePrice(item, isBlacksmithShop());
    }

    static int salePrice(Item item, boolean blacksmith) {
        int percent = "RARE".equals(itemQuality(item)) ? 60 :
            ("UNCOMMON".equals(itemQuality(item)) ? 50 : 35);
        if (blacksmith && ("Weapon".equals(item.type) || item.defense >= 3)) percent += 10;
        return Math.max(1, item.cost * percent / 100);
    }

    void sellItem(Item item) {
        if (currentTile() != TileType.SHOP) {
            add("Equipment can only be sold at a shop.");
            return;
        }
        String restriction = sellRestriction(item);
        if (restriction != null) {
            add("The " + item.name + " cannot be sold: " + restriction.toLowerCase() + ".");
            return;
        }
        int price = salePrice(item);
        state.inventory.remove(item);
        state.gold += price;
        add("You sell the " + item.name + " for " + price + " gold.");
    }

    void identifyRelic(Relic relic) {
        if (relic == null || state.relics == null || !state.relics.contains(relic)) {
            add("That relic is not in your inventory.");
            return;
        }
        if (relic.identified) {
            add(relic.name + " has already been identified as " + relic.rewardName + ".");
            return;
        }
        String vendor = currentVendor();
        if (vendor == null) {
            add("A skilled vendor must examine the " + relic.name + ".");
            return;
        }
        if (!relic.vendor.equals(vendor)) {
            add("The " + vendor + " cannot identify the " + relic.name +
                ". Seek the " + relic.vendor + ".");
            return;
        }
        Item reward = relicReward(relic, state.heroClass);
        relic.identified = true;
        relic.rewardName = reward.name;
        state.inventory.add(reward);
        add("The " + vendor + " identifies the " + relic.name + " for free.");
        add("Its power becomes " + reward.name + ", attuned to your " +
            state.heroClass + " training.");
        equipIfUpgrade(reward);
        add("Identified relics grant class-attuned dragon damage and " +
            "reduce dragon damage taken.");
    }

    private Item relicReward(Relic relic, String heroClass) {
        boolean armour = "Alchemist".equals(relic.vendor) || "Innkeeper".equals(relic.vendor);
        if (armour) {
            if ("Fighter".equals(heroClass)) return new Item(
                "Innkeeper".equals(relic.vendor) ? "Oathbound Plate" : "Warded Chain",
                "Armour", 0, "Innkeeper".equals(relic.vendor) ? 8 : 7, 0, heroClass, true);
            if ("Mage".equals(heroClass)) return new Item(
                "Innkeeper".equals(relic.vendor) ? "Oathweave Robes" : "Soulglass Robes",
                "Armour", 0, 5, 0, heroClass, true);
            if ("Rogue".equals(heroClass)) return new Item(
                "Innkeeper".equals(relic.vendor) ? "Oathcloak" : "Veilweave Leather",
                "Armour", 0, 6, 0, heroClass, true);
            return new Item("Innkeeper".equals(relic.vendor) ? "Oathscale" : "Spirit Hide",
                "Armour", 0, "Innkeeper".equals(relic.vendor) ? 7 : 6, 0,
                heroClass, true);
        }
        boolean moon = "General Merchant".equals(relic.vendor);
        if ("Fighter".equals(heroClass)) return new Item(
            moon ? "Moonsteel Blade" : "Warlord Blade", "Weapon", moon ? 10 : 11,
            0, 0, heroClass, true);
        if ("Mage".equals(heroClass)) return new Item(
            moon ? "Moonwand" : "Runic Battlestaff", "Weapon", 8, 0, 0,
            heroClass, true);
        if ("Rogue".equals(heroClass)) return new Item(
            moon ? "Moonlit Dirk" : "Crest Dirk", "Weapon", moon ? 10 : 9,
            0, 0, heroClass, true);
        return new Item(moon ? "Moonbow" : "Warlord Recurve", "Weapon",
            moon ? 12 : 11, 0, 0, heroClass, true);
    }

    private void equipIfUpgrade(Item reward) {
        Item current = findItem("Weapon".equals(reward.type)
            ? state.equippedWeapon : state.equippedArmour);
        int currentValue = current == null ? 0 :
            ("Weapon".equals(reward.type) ? current.attack : current.defense);
        int rewardValue = "Weapon".equals(reward.type) ? reward.attack : reward.defense;
        if (rewardValue > currentValue) {
            if ("Weapon".equals(reward.type)) state.equippedWeapon = reward.name;
            else state.equippedArmour = reward.name;
            add("You equip the " + reward.name + ".");
        }
    }

    String currentVendor() {
        if (currentTile() == TileType.SHOP) {
            return isBlacksmithShop() ? "Blacksmith" : "General Merchant";
        }
        if (currentTile() == TileType.TAVERN) return "Innkeeper";
        if (currentTile() == TileType.ENCAMPMENT) return "Alchemist";
        return null;
    }

    boolean canIdentifyHere(Relic relic) {
        return relic != null && !relic.identified && relic.vendor.equals(currentVendor());
    }

    void equipItem(Item item) {
        if (!state.inventory.contains(item)) {
            add("The " + item.name + " is not in your inventory.");
            return;
        }
        String restriction = equipmentRestriction(state.heroClass, item);
        if (restriction != null) {
            add(restriction);
            return;
        }
        if ("Weapon".equals(item.type)) {
            state.equippedWeapon = item.name;
            if (isTwoHanded(item)) state.equippedOffhand = null;
            String family = weaponFamily(item);
            if (family != null && weaponProficiencyRank(state, family) == 0) {
                state.weaponProficiency.put(family, Integer.valueOf(1));
                add("You become Trained with " + family + " weapons.");
            }
        } else if ("Offhand".equals(item.type)) {
            Item weapon = findItem(state.equippedWeapon);
            if (isTwoHanded(weapon)) {
                add("You cannot equip " + item.name + " while wielding " + weapon.name + " with both hands.");
                return;
            }
            state.equippedOffhand = item.name;
        } else {
            state.equippedArmour = item.name;
        }
        add("You equip the " + item.name + ".");
    }

    static String equipmentRestriction(String heroClass, Item item) {
        if (item == null) return null;
        if (item.allowedClass != null) {
            boolean listed = ("," + item.allowedClass + ",").contains("," + heroClass + ",");
            return listed ? null :
                heroClass + " cannot equip " + item.name + ".";
        }
        boolean allowed;
        if (isRanged(item) && !"Hunter".equals(heroClass)) {
            allowed = false;
        } else if ("Fighter".equals(heroClass)) {
            allowed = true;
        } else if ("Mage".equals(heroClass)) {
            allowed = named(item, "Oak Staff", "Dagger", "Apprentice Wand", "Runed Wand",
                "Cloth Armour", "Leather Armour");
        } else if ("Rogue".equals(heroClass)) {
            allowed = named(item, "Dagger", "Serrated Dagger", "Short Sword", "Leather Armour");
        } else if ("Hunter".equals(heroClass)) {
            allowed = named(item, "Dagger", "Short Sword", "Long Bow", "Crossbow", "Hunting Crossbow",
                "Leather Armour", "Scale Armour");
        } else {
            allowed = false;
        }
        return allowed ? null : heroClass + " cannot equip " + item.name + ".";
    }

    static boolean vendorOffers(boolean blacksmith, Item item) {
        if (item == null) return false;
        if (blacksmith) {
            return named(item, "Dagger", "Short Sword", "Long Sword", "Axe",
                "Scale Armour", "Plate Armour", "Steel Long Sword", "Knight Plate",
                "Shadowsteel Dirk", "Reinforced Scale", "Iron Shield",
                "Balanced Offhand Dagger", "Iron Mace", "Flanged Mace", "Warhammer",
                "Greatsword", "Tempered Greatsword", "Serrated Dagger",
                "Soldier Shield");
        }
        return named(item, "Dagger", "Short Sword", "Long Bow", "Crossbow", "Oak Staff",
            "Cloth Armour", "Leather Armour", "Reinforced Leather", "Ashwood Staff",
            "Mystic Robes", "Ranger Bow", "Runed Tome", "Hunter's Quiver",
            "Runed Wand", "Hunting Crossbow", "Carved Totem");
    }

    static boolean isTwoHanded(Item item) {
        return item != null && named(item, "Oak Staff", "Ashwood Staff", "Runic Battlestaff",
            "Long Bow", "Ranger Bow", "Moonbow", "Warlord Recurve", "Crossbow",
            "Hunting Crossbow", "Greatsword", "Tempered Greatsword", "Axe", "Warhammer");
    }

    static boolean usesTwoHandedWeapon(State state) {
        if (state == null || state.equippedWeapon == null) return false;
        String name = state.equippedWeapon;
        return "Oak Staff".equals(name) || "Ashwood Staff".equals(name) ||
            "Runic Battlestaff".equals(name) || "Long Bow".equals(name) ||
            "Ranger Bow".equals(name) || "Moonbow".equals(name) ||
            "Warlord Recurve".equals(name) || "Crossbow".equals(name) ||
            "Hunting Crossbow".equals(name) || "Greatsword".equals(name) ||
            "Tempered Greatsword".equals(name) || "Axe".equals(name) ||
            "Warhammer".equals(name);
    }

    private static boolean isRanged(Item item) {
        return named(item, "Long Bow", "Crossbow", "Hunting Crossbow", "Ranger Bow");
    }

    private static boolean named(Item item, String... names) {
        for (String name : names) {
            if (name.equals(item.name)) return true;
        }
        return false;
    }

    private int totalAttack() {
        int total = state.baseAttack + state.level - 1;
        Item weapon = findItem(state.equippedWeapon);
        Item offhand = findItem(state.equippedOffhand);
        return total + (weapon == null ? 0 : weapon.attack) + (offhand == null ? 0 : offhand.attack);
    }

    private int totalDefense() {
        Item armour = findItem(state.equippedArmour);
        Item offhand = findItem(state.equippedOffhand);
        return state.baseDefense + (armour == null ? 0 : armour.defense) +
            (offhand == null ? 0 : offhand.defense);
    }

    int identifiedRelicCount() {
        int count = 0;
        if (state.relics != null) {
            for (Relic relic : state.relics) if (relic.identified) count++;
        }
        return count;
    }

    /**
     * Identified relics are the authored bridge from elite encounters to dragons.
     * Every offensive action, including class abilities, receives the same
     * class-attuned bonus so tactical play is never worse than basic attacks.
     */
    private int bossRelicDamageBonus() {
        return relicDamageBonus(state);
    }

    /** Bounded protection rewards preparation without making three relics invulnerable. */
    private int bossRelicGuard() {
        return Math.min(6, identifiedRelicCount() * 2);
    }

    private Item findItem(String name) {
        if (name != null && state.inventory != null) {
            for (Item item : state.inventory) {
                if (name.equals(item.name)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void gainExperience(int amount) {
        state.experience += amount;
        int needed = state.level * 30;
        if (state.experience >= needed) {
            state.experience -= needed;
            state.level++;
            state.maxHealth += 8;
            state.health = state.maxHealth;
            if ("Mage".equals(state.heroClass)) {
                state.maxMana += 6;
                state.mana = state.maxMana;
            }
            if ("Hunter".equals(state.heroClass)) {
                state.maxFocus = maxFocusForLevel(state.level);
                state.focus = Math.max(state.focus, state.maxFocus / 3);
            }
            add("You reached level " + state.level + "! " +
                ("Mage".equals(state.heroClass)
                    ? "Health and mana are restored." : "Health is restored."));
            unlockLevelAbilities();
            trainEquippedWeapon(Math.min(3, state.level), true);
            Item weapon = findItem(state.equippedWeapon);
            String family = weaponFamily(weapon);
            String abilities = "Mage".equals(state.heroClass)
                ? (state.level == 2 ? "Fireball" : "Ice Spike")
                : (state.level == 2 ? abilityName(state, 1) :
                    abilityName(state, 2) + " · " + abilityName(state, 3) +
                    ("Fighter".equals(state.heroClass) ? " (Passive)" : ""));
            pendingProgressionNotice = new ProgressionNotice(state.level, abilities,
                family, weaponProficiencyRank(state, family));
        }
    }

    private void unlockLevelAbilities() {
        if ("Mage".equals(state.heroClass)) {
            if (state.level >= 2 && !state.spells.contains("Fireball")) {
                state.spells.add("Fireball");
                add("New ability unlocked: Fireball.");
            }
            if (state.level >= 3 && !state.spells.contains("Ice Spike")) {
                state.spells.add("Ice Spike");
                add("New ability unlocked: Ice Spike.");
            }
        } else if (state.level == 2) {
            add("New tactical ability unlocked: " + abilityName(state, 1) + ".");
        } else if (state.level >= 3) {
            add("New mastery ability unlocked: " + abilityName(state, 2) + ".");
            add("New signature ability unlocked: " + abilityName(state, 3) + ".");
        }
    }

    private void trainEquippedWeapon(int targetRank, boolean announce) {
        if (state.weaponProficiency == null) {
            state.weaponProficiency = new HashMap<String, Integer>();
        }
        Item weapon = findItem(state.equippedWeapon);
        String family = weaponFamily(weapon);
        if (family == null) return;
        int current = weaponProficiencyRank(state, family);
        int rank = Math.max(current, Math.max(1, Math.min(3, targetRank)));
        state.weaponProficiency.put(family, Integer.valueOf(rank));
        if (announce && rank > current) {
            add(family + " proficiency advanced to " + proficiencyLabel(rank) + ".");
        }
    }

    String inventoryText() {
        StringBuilder text = new StringBuilder();
        text.append(state.race).append(' ').append(state.heroClass)
            .append(" — Level ").append(state.level).append('\n')
            .append("Attack ").append(totalAttack()).append("   Defense ").append(totalDefense()).append('\n')
            .append("Experience ").append(state.experience).append('/').append(state.level * 30).append("\n\n");
        if (state.inventory.isEmpty()) {
            text.append("No equipment");
        } else {
            for (Item item : state.inventory) {
                boolean equipped = item.name.equals(state.equippedWeapon) ||
                    item.name.equals(state.equippedArmour) || item.name.equals(state.equippedOffhand);
                text.append(equipped ? "✓ " : "  ").append(item).append('\n');
            }
        }
        if (!state.spells.isEmpty()) {
            text.append("\nSpells: ");
            for (int i = 0; i < state.spells.size(); i++) {
                if (i > 0) text.append(", ");
                text.append(state.spells.get(i));
            }
        }
        if (state.relics != null && !state.relics.isEmpty()) {
            text.append("\n\nQuest Relics:\n");
            for (Relic relic : state.relics) text.append("◆ ").append(relic).append('\n');
        }
        return text.toString();
    }

    private boolean canAct() {
        if (state.won) {
            add("Your quest is complete.");
            return false;
        }
        if (state.health == 0) {
            add("You cannot continue while defeated.");
            return false;
        }
        return true;
    }

    private void describeLocation() {
        add("You enter the " + currentTile().label.toLowerCase() + ".");
        Enemy enemy = currentEnemy();
        if (enemy != null) {
            ensureCombatTimeline(enemy);
            add("A " + enemy.name + " confronts you!");
        } else if (currentTile() == TileType.SPIDER_NEST) {
            if (state.spiderNestCleared) {
                add("The torn webs lie still. This threat source has been permanently cleared.");
            } else {
                add(state.spiderNestRemaining + " brood " +
                    (state.spiderNestRemaining == 1 ? "clutch remains" : "clutches remain") +
                    ". Clearing them yields only small field rewards but makes the route safer.");
            }
        } else if (currentTile() == TileType.SHOP) {
            add("The merchant offers potions for " + POTION_COST + " gold.");
        } else if (currentTile() == TileType.TAVERN || currentTile() == TileType.ENCAMPMENT) {
            add("This is a safe place to rest.");
        }
    }

    void save(File file) throws IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(state);
        }
        add("Quest saved to " + file.getName() + ".");
    }

    void load(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            Object loaded = input.readObject();
            if (!(loaded instanceof State)) {
                throw new IOException("That file is not a Desktop Edition save.");
            }
            state = (State) loaded;
        }
        normalizeLoadedState();
        pendingRelicDiscovery = null;
        pendingLootDiscovery = null;
        pendingProgressionNotice = null;
        pendingEnemyTurnEvents.clear();
        random = new Random();
        history.setLength(0);
        add("Quest loaded from " + file.getName() + ".");
        describeLocation();
    }

    private void normalizeLoadedState() {
        ensureWorldSize();
        if (state.race == null) state.race = "Human";
        if (state.heroClass == null) state.heroClass = "Fighter";
        state.gender = CharacterArt.normalizeGender(state.gender, state.race, state.heroClass);
        if (state.level < 1) state.level = 1;
        if (state.baseAttack < 1) state.baseAttack = 5;
        if ("Fighter".equals(state.heroClass)) {
            if (state.maxRage < 1) state.maxRage = 100;
            state.rage = Math.max(0, Math.min(state.maxRage, state.rage));
        } else {
            state.rage = 0;
            state.maxRage = 0;
        }
        if ("Rogue".equals(state.heroClass)) {
            if (state.maxMomentum < 1) state.maxMomentum = 100;
            state.momentum = Math.max(0, Math.min(state.maxMomentum, state.momentum));
        } else {
            state.momentum = 0;
            state.maxMomentum = 0;
        }
        if ("Hunter".equals(state.heroClass)) {
            state.maxFocus = maxFocusForLevel(state.level);
            state.focus = Math.max(0, Math.min(state.maxFocus, state.focus));
            if (state.focus == 0) state.focus = state.maxFocus / 3;
        } else {
            state.focus = 0;
            state.maxFocus = 0;
            state.enemyMarked = false;
            state.enemyPinned = false;
        }
        if (state.inventory == null) state.inventory = new ArrayList<Item>();
        if (state.relics == null) state.relics = new ArrayList<Relic>();
        if (state.mapJournal == null) state.mapJournal = new ArrayList<String>();
        if (state.spells == null) state.spells = new ArrayList<String>();
        if (state.blacksmithShops == null) {
            state.blacksmithShops = new boolean[SIZE][SIZE];
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    state.blacksmithShops[row][col] = state.tiles[row][col] == TileType.SHOP &&
                        row >= 3;
                }
            }
        }
        if (state.generationVersion < 3) {
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    Enemy enemy = state.enemies[row][col];
                    if (enemy != null) {
                        enemy.homeRow = row;
                        enemy.homeCol = col;
                        enemy.tier = legacyTier(enemy.name);
                        if (enemy.defense == 0) enemy.defense = defenseForEnemy(enemy.name);
                        if (enemy.tier == 2) {
                            state.dragonLairRow = row;
                            state.dragonLairCol = col;
                        }
                    }
                }
            }
            state.previousRow = 0;
            state.previousCol = 0;
            state.generationVersion = 3;
        }
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Enemy enemy = state.enemies[row][col];
                if (enemy == null) continue;
                if (enemy.combatLevel < 1) {
                    enemy.combatLevel = combatLevelForEnemy(enemy.name, enemy.tier);
                }
                if (enemy.speed < 1) enemy.speed = speedForEnemy(enemy.name);
            }
        }
        if (state.generationVersion < 4 || state.discovery == null ||
                state.threatKnowledge == null || state.relicSearch == null) {
            // Earlier editions exposed the whole board. Preserve that knowledge
            // when migrating instead of unexpectedly covering an existing save.
            state.discovery = new DiscoveryState[SIZE][SIZE];
            state.threatKnowledge = new byte[SIZE][SIZE];
            state.relicSearch = new boolean[SIZE][SIZE];
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    state.discovery[row][col] = DiscoveryState.SCOUTED;
                    if (state.enemies[row][col] != null) state.threatKnowledge[row][col] = 1;
                }
            }
            state.discovery[state.row][state.col] = DiscoveryState.VISITED;
            if (state.enemies[state.row][state.col] != null) {
                state.threatKnowledge[state.row][state.col] = 2;
            }
            state.generationVersion = 4;
        } else {
            ensureDiscoveryArrays();
            revealFromTravel();
        }
        if (state.generationVersion < 5 || state.rumorServicesUsed == null ||
                state.rumorServicesUsed.length != SIZE) {
            // Version 4 saves had no consultation history, so both locations
            // begin available rather than guessing what the player used.
            state.rumorServicesUsed = new boolean[SIZE][SIZE];
            state.generationVersion = 5;
        }
        if (state.generationVersion < 6 || state.starterKit == null) {
            // Older saves retain their original equipment and adopt the matching
            // default loadout label; no inventory replacement is performed.
            state.starterKit = defaultStarterKit(state.heroClass);
            state.generationVersion = 6;
        }
        if (state.generationVersion < 7) {
            // Offhand equipment is additive; older heroes simply begin with the
            // new slot empty and keep all existing statistics and inventory.
            state.equippedOffhand = null;
            state.generationVersion = 7;
        }
        if (state.generationVersion < 8 || state.weaponProficiency == null) {
            state.weaponProficiency = new HashMap<String, Integer>();
            trainEquippedWeapon(Math.min(3, state.level), false);
            state.generationVersion = 8;
        }
        if (state.generationVersion < 9) {
            // Version 9 expands the logical world. Existing tiles, enemies, and
            // discovery are copied in place; newly available territory begins
            // uncharted and contains deterministic terrain but no surprise foes.
            state.generationVersion = 9;
        }
        if (state.generationVersion < 10) {
            // Add the finite nest only on unvisited, empty territory. Existing
            // progress and known routes are never replaced with a surprise foe.
            installMigratedSpiderNest();
            state.generationVersion = 10;
        }
        if ("Mage".equals(state.heroClass)) {
            state.spells.clear();
            addSpellIfMissing("Magic Missile");
            if (state.level >= 2) addSpellIfMissing("Fireball");
            if (state.level >= 3) addSpellIfMissing("Ice Spike");
            if (state.selectedSpell == null || !state.spells.contains(state.selectedSpell)) {
                state.selectedSpell = "Magic Missile";
            }
        } else {
            state.spells.clear();
            state.selectedSpell = null;
        }
        Item weapon = findItem(state.equippedWeapon);
        Item armour = findItem(state.equippedArmour);
        Item offhand = findItem(state.equippedOffhand);
        if (weapon != null && equipmentRestriction(state.heroClass, weapon) != null) {
            state.equippedWeapon = null;
        }
        if (armour != null && equipmentRestriction(state.heroClass, armour) != null) {
            state.equippedArmour = null;
        }
        if (offhand != null && (equipmentRestriction(state.heroClass, offhand) != null ||
                isTwoHanded(weapon))) {
            state.equippedOffhand = null;
        }
    }

    /** Expands older serialized boards without discarding their quest progress. */
    private void ensureWorldSize() {
        if (state.tiles != null && state.tiles.length == SIZE &&
                state.enemies != null && state.enemies.length == SIZE &&
                state.blacksmithShops != null && state.blacksmithShops.length == SIZE &&
                state.discovery != null && state.discovery.length == SIZE &&
                state.threatKnowledge != null && state.threatKnowledge.length == SIZE &&
                state.relicSearch != null && state.relicSearch.length == SIZE &&
                state.rumorServicesUsed != null && state.rumorServicesUsed.length == SIZE) {
            return;
        }

        TileType[][] oldTiles = state.tiles;
        Enemy[][] oldEnemies = state.enemies;
        boolean[][] oldBlacksmiths = state.blacksmithShops;
        DiscoveryState[][] oldDiscovery = state.discovery;
        byte[][] oldThreats = state.threatKnowledge;
        boolean[][] oldRelics = state.relicSearch;
        boolean[][] oldRumors = state.rumorServicesUsed;

        state.tiles = new TileType[SIZE][SIZE];
        state.enemies = new Enemy[SIZE][SIZE];
        state.blacksmithShops = new boolean[SIZE][SIZE];
        state.discovery = new DiscoveryState[SIZE][SIZE];
        state.threatKnowledge = new byte[SIZE][SIZE];
        state.relicSearch = new boolean[SIZE][SIZE];
        state.rumorServicesUsed = new boolean[SIZE][SIZE];

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                state.tiles[row][col] = migratedTerrain(row, col);
                state.discovery[row][col] = DiscoveryState.UNKNOWN;
                if (hasCell(oldTiles, row, col) && oldTiles[row][col] != null) {
                    state.tiles[row][col] = oldTiles[row][col];
                }
                if (hasCell(oldEnemies, row, col)) state.enemies[row][col] = oldEnemies[row][col];
                if (hasCell(oldBlacksmiths, row, col)) {
                    state.blacksmithShops[row][col] = oldBlacksmiths[row][col];
                }
                if (hasCell(oldDiscovery, row, col) && oldDiscovery[row][col] != null) {
                    state.discovery[row][col] = oldDiscovery[row][col];
                }
                if (hasCell(oldThreats, row, col)) {
                    state.threatKnowledge[row][col] = oldThreats[row][col];
                }
                if (hasCell(oldRelics, row, col)) state.relicSearch[row][col] = oldRelics[row][col];
                if (hasCell(oldRumors, row, col)) {
                    state.rumorServicesUsed[row][col] = oldRumors[row][col];
                }
            }
        }
        if (oldBlacksmiths == null) {
            int bestRow = -1;
            int bestCol = -1;
            int bestDistance = -1;
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (state.tiles[row][col] == TileType.SHOP && row + col > bestDistance) {
                        bestRow = row;
                        bestCol = col;
                        bestDistance = row + col;
                    }
                }
            }
            if (bestRow >= 0) state.blacksmithShops[bestRow][bestCol] = true;
        }
        state.row = Math.max(0, Math.min(SIZE - 1, state.row));
        state.col = Math.max(0, Math.min(SIZE - 1, state.col));
        state.previousRow = Math.max(0, Math.min(SIZE - 1, state.previousRow));
        state.previousCol = Math.max(0, Math.min(SIZE - 1, state.previousCol));
    }

    private static TileType migratedTerrain(int row, int col) {
        int value = Math.abs(row * 31 + col * 17 + row * col * 3) % 10;
        if (value == 0) return TileType.LAKE;
        if (value == 1) return TileType.CRYPT;
        return TileType.FIELD;
    }

    private void installMigratedSpiderNest() {
        if (state.won) {
            state.spiderNestCleared = true;
            state.spiderNestRemaining = 0;
            return;
        }
        int bestRow = -1;
        int bestCol = -1;
        int bestScore = Integer.MAX_VALUE;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int distance = row + col;
                if (distance < 7 || distance > 13 || state.tiles[row][col] != TileType.FIELD ||
                        state.enemies[row][col] != null ||
                        state.discovery[row][col] != DiscoveryState.UNKNOWN) continue;
                int score = Math.abs((row * 47 + col * 29 + row * col * 7) % 101);
                if (score < bestScore) {
                    bestScore = score;
                    bestRow = row;
                    bestCol = col;
                }
            }
        }
        if (bestRow < 0) {
            // Fully explored legacy worlds remain valid and simply treat the
            // optional source as already resolved.
            state.spiderNestCleared = true;
            state.spiderNestRemaining = 0;
            return;
        }
        state.spiderNestRow = bestRow;
        state.spiderNestCol = bestCol;
        state.spiderNestRemaining = 3;
        state.spiderNestCleared = false;
        state.tiles[bestRow][bestCol] = TileType.SPIDER_NEST;
        placeEnemy(bestRow, bestCol, spiderBroodEnemy());
    }

    private static boolean hasCell(Object[][] values, int row, int col) {
        return values != null && row < values.length && values[row] != null &&
            col < values[row].length;
    }

    private static boolean hasCell(boolean[][] values, int row, int col) {
        return values != null && row < values.length && values[row] != null &&
            col < values[row].length;
    }

    private static boolean hasCell(byte[][] values, int row, int col) {
        return values != null && row < values.length && values[row] != null &&
            col < values[row].length;
    }

    private void addSpellIfMissing(String spell) {
        if (!state.spells.contains(spell)) state.spells.add(spell);
    }

    State getState() {
        return state;
    }

    ProgressionNotice consumeProgressionNotice() {
        ProgressionNotice notice = pendingProgressionNotice;
        pendingProgressionNotice = null;
        return notice;
    }

    DiscoveryState discoveryAt(int row, int col) {
        ensureDiscoveryArrays();
        return inside(row, col) ? state.discovery[row][col] : DiscoveryState.UNKNOWN;
    }

    int threatKnowledgeAt(int row, int col) {
        ensureDiscoveryArrays();
        return inside(row, col) ? state.threatKnowledge[row][col] : 0;
    }

    boolean hasRelicClueAt(int row, int col) {
        ensureDiscoveryArrays();
        return inside(row, col) && state.relicSearch[row][col];
    }

    Enemy currentEnemy() {
        return state.enemies[state.row][state.col];
    }

    boolean spiderNestCleared() {
        return state.spiderNestCleared;
    }

    int spiderNestRemaining() {
        return Math.max(0, state.spiderNestRemaining);
    }

    TileType currentTile() {
        return state.tiles[state.row][state.col];
    }

    boolean isBlacksmithShop() {
        return currentTile() == TileType.SHOP && state.blacksmithShops != null &&
            state.blacksmithShops[state.row][state.col];
    }

    private int legacyTier(String name) {
        if (name != null && name.contains("Dragon")) return 2;
        if ("Minotaur".equals(name) || "Lich".equals(name) || "Mimic".equals(name) ||
                "Orc Warlord".equals(name) || "Necromancer".equals(name) ||
                "Dark Elf Assassin".equals(name) || "Fallen Knight".equals(name)) return 1;
        return 0;
    }

    private static int defenseForEnemy(String name) {
        if ("Skeletal Guardian".equals(name) || "Skeleton".equals(name)) return 5;
        if ("Cave Troll".equals(name) || "Troll".equals(name)) return 4;
        if ("Cultist Mage".equals(name) || "Bandit Marauder".equals(name) ||
                "Hobgoblin".equals(name) || "Swamp Serpent".equals(name)) return 3;
        if ("Dire Wolf".equals(name) || "Giant Forest Spider".equals(name) ||
                "Giant Spider".equals(name)) return 2;
        if ("Ashweb Matriarch".equals(name)) return 4;
        if ("Armored Boar".equals(name) || "Necromancer".equals(name) ||
                "Lich".equals(name)) return 6;
        if ("Dark Elf Assassin".equals(name) || "Mimic".equals(name)) return 7;
        if ("Orc Warlord".equals(name) || "Minotaur".equals(name)) return 8;
        if ("Fallen Knight".equals(name)) return 9;
        if ("Ancient Frost Dragon".equals(name)) return 10;
        if ("Corrupted Shadow Dragon".equals(name)) return 10;
        if ("Skeletal Undead Dragon".equals(name)) return 11;
        if (name != null && name.contains("Dragon")) return 9;
        return 0;
    }

    private static int combatLevelForEnemy(String name, int tier) {
        if ("Skeletal Undead Dragon".equals(name)) return 7;
        if (name != null && name.contains("Dragon")) return 6;
        if (tier == 2) return 6;
        if ("Fallen Knight".equals(name) || "Necromancer".equals(name) ||
                "Lich".equals(name)) return 6;
        if (tier == 1) return 5;
        if ("Cave Troll".equals(name) || "Cultist Mage".equals(name) ||
                "Armored Boar".equals(name)) return 3;
        if ("Bandit Marauder".equals(name) || "Skeletal Guardian".equals(name) ||
                "Skeleton".equals(name) || "Swamp Serpent".equals(name)) return 2;
        return 1;
    }

    private static int speedForEnemy(String name) {
        if ("Dark Elf Assassin".equals(name)) return 125;
        if ("Dire Wolf".equals(name)) return 120;
        if ("Giant Forest Spider".equals(name) || "Giant Spider".equals(name) ||
                "Ashweb Matriarch".equals(name)) return 115;
        if ("Bandit Marauder".equals(name)) return 110;
        if (name != null && name.contains("Dragon")) return 105;
        if ("Cultist Mage".equals(name) || "Necromancer".equals(name) ||
                "Lich".equals(name)) return 95;
        if ("Orc Warlord".equals(name) || "Minotaur".equals(name)) return 90;
        if ("Armored Boar".equals(name) || "Fallen Knight".equals(name)) return 88;
        if ("Cave Troll".equals(name) || "Troll".equals(name)) return 82;
        return 100;
    }

    int getAttack() {
        return totalAttack();
    }

    int getDefense() {
        return totalDefense();
    }

    int getHeroSpeed() {
        Enemy enemy = currentEnemy();
        if (enemy != null) ensureCombatTimeline(enemy);
        return state.heroSpeed;
    }

    int getEnemySpeed() {
        Enemy enemy = currentEnemy();
        if (enemy != null) ensureCombatTimeline(enemy);
        return state.enemySpeed;
    }

    void setRandomSeed(long seed) {
        random = new Random(seed);
    }

    String getHistory() {
        return history.toString();
    }

    List<EnemyTurnEvent> consumeEnemyTurnEvents() {
        ArrayList<EnemyTurnEvent> events =
            new ArrayList<EnemyTurnEvent>(pendingEnemyTurnEvents);
        pendingEnemyTurnEvents.clear();
        return events;
    }

    Relic consumeRelicDiscovery() {
        Relic relic = pendingRelicDiscovery;
        pendingRelicDiscovery = null;
        return relic;
    }

    /** Exposes a combat equipment drop once to the presentation layer. */
    Item consumeLootDiscovery() {
        Item item = pendingLootDiscovery;
        pendingLootDiscovery = null;
        return item;
    }

    String getMapText() {
        StringBuilder map = new StringBuilder();
        map.append("    ");
        for (int col = 0; col < SIZE; col++) {
            map.append(String.format("%-4d", Integer.valueOf(col + 1)));
        }
        map.append('\n');
        for (int row = 0; row < SIZE; row++) {
            map.append((char) ('A' + row)).append("   ");
            for (int col = 0; col < SIZE; col++) {
                DiscoveryState discovery = discoveryAt(row, col);
                char symbol = discovery == DiscoveryState.UNKNOWN ? '?' :
                    state.tiles[row][col].symbol;
                if (row == state.row && col == state.col) {
                    symbol = '@';
                } else if (state.enemies[row][col] != null &&
                        threatKnowledgeAt(row, col) > 0) {
                    symbol = '!';
                }
                map.append(symbol).append("   ");
            }
            map.append('\n');
        }
        return map.toString();
    }

    private void add(String message) {
        history.append("• ").append(message).append('\n');
    }
}
