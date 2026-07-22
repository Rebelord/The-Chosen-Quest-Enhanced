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
        String starterKit;
        int health = 50;
        int maxHealth = 50;
        int mana;
        int maxMana;
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
        int heroCombatClock;
        int enemyCombatClock;
        int consecutiveHeroActions;
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
        state = new State();
        pendingRelicDiscovery = null;
        pendingProgressionNotice = null;
        pendingEnemyTurnEvents.clear();
        String cleaned = playerName == null ? "" : playerName.trim();
        state.playerName = cleaned.isEmpty() ? "Chosen One" : cleaned;
        state.race = validChoice(race, RACES, "Human");
        state.heroClass = validChoice(heroClass, CLASSES, "Fighter");
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
        state = new State();
        state.playerName = "Preview";
        state.race = validChoice(race, RACES, "Human");
        state.heroClass = validChoice(heroClass, CLASSES, "Fighter");
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
            new Enemy("Ancient Red Dragon", 130, 18, 300, 2),
            new Enemy("Ancient Frost Dragon", 138, 19, 320, 2),
            new Enemy("Corrupted Shadow Dragon", 145, 20, 340, 2),
            new Enemy("Skeletal Undead Dragon", 152, 21, 360, 2)
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
            return "Giant Forest Spider".equals(enemyName) || "Giant Spider".equals(enemyName);
        }
        if ("Swamp Serpent".equals(enemyName)) return terrain == TileType.LAKE;
        if ("Skeletal Guardian".equals(enemyName) || "Cave Troll".equals(enemyName) ||
                "Cultist Mage".equals(enemyName) || "Necromancer".equals(enemyName) ||
                "Fallen Knight".equals(enemyName)) return terrain == TileType.CRYPT;
        if (enemyName != null && enemyName.contains("Dragon")) return terrain == TileType.CRYPT;
        return terrain == TileType.FIELD;
    }

    private Enemy spiderBroodEnemy() {
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
            addStartingItem(new Item("QUICK KNIVES".equals(state.starterKit) ? "Dagger" : "Short Sword",
                "Weapon", "QUICK KNIVES".equals(state.starterKit) ? 5 : 6, 0, 10), true);
            if ("QUICK KNIVES".equals(state.starterKit)) {
                addStartingItem(new Item("Offhand Dagger", "Offhand", 2, 0, 7, "Rogue", false), true);
            }
            addStartingItem(new Item("Leather Armour", "Armour", 0, 2, 15), true);
        } else if ("Hunter".equals(state.heroClass)) {
            state.baseAttack += 2;
            state.maxHealth += 8;
            addStartingItem(new Item("MARKSMAN".equals(state.starterKit) ? "Crossbow" : "Long Bow",
                "Weapon", "MARKSMAN".equals(state.starterKit) ? 8 : 7, 0, 30), true);
            addStartingItem(new Item("Leather Armour", "Armour", 0, 2, 15), true);
        } else {
            state.maxHealth += 15;
            state.baseDefense += 2;
            addStartingItem(new Item("BREAKER".equals(state.starterKit) ? "Greatsword" : "Long Sword",
                "Weapon", "BREAKER".equals(state.starterKit) ? 9 : 7, 0, 15,
                "Fighter", false), true);
            addStartingItem(new Item("BREAKER".equals(state.starterKit) ? "Leather Armour" : "Chain Armour",
                "Armour", 0, "BREAKER".equals(state.starterKit) ? 2 : 4, 35), true);
        }
        state.health = state.maxHealth;
        state.mana = state.maxMana;
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
        boolean marked = "MARK".equals(state.combatPreparation);
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
            rawDamage += 7 + state.level;
            add("Hunter's Mark empowers the shot!");
            state.combatStatus = "HUNTER'S MARK · CONSUMED";
        }
        rawDamage = Math.max(2, rawDamage);
        if (isBoss(enemy)) rawDamage += identifiedRelicCount() *
            ("Fighter".equals(state.heroClass) ? 3 : 2);
        int armour = enemy.defense;
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
            add("Bleeding Edge opens a wound for " + bleed + " bonus damage.");
            state.combatStatus = "BLEEDING EDGE · WOUND OPENED";
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
        if (enemy.health == 0) {
            defeatEnemy(enemy);
            return;
        }
        completePlayerAction(enemy, attackTempo(state));
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
        if (isBoss(enemy)) damage += identifiedRelicCount() *
            ("Mage".equals(state.heroClass) ? 5 : 2);
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

    void defend() {
        if (!canAct()) {
            return;
        }
        Enemy enemy = currentEnemy();
        if (enemy == null) {
            add("You ready your shield, but no danger approaches.");
            return;
        }
        Item weapon = findItem(state.equippedWeapon);
        Item offhand = findItem(state.equippedOffhand);
        state.defending = true;
        if ("Fighter".equals(state.heroClass) && isTwoHanded(weapon)) {
            state.defending = false;
            state.combatPreparation = "RAGE";
            add("You gather rage for a devastating heavy strike.");
            state.combatStatus = "RAGE · HEAVY STRIKE READY";
        } else if ("Mage".equals(state.heroClass)) {
            int restored = Math.min(4 + state.level / 2, state.maxMana - state.mana);
            state.mana += restored;
            add(restored > 0 ? "You channel a ward and recover " + restored + " mana." :
                "You channel a ward around yourself.");
            state.combatStatus = "WARD · " + (restored > 0 ? "+" + restored + " MANA" : "ACTIVE");
        } else if ("Rogue".equals(state.heroClass)) {
            state.combatPreparation = "STEALTH";
            add("You slip into stealth and prepare a critical strike.");
            state.combatStatus = "STEALTH · CRITICAL READY";
        } else if ("Hunter".equals(state.heroClass)) {
            state.combatPreparation = "AIM";
            add("You take aim and prepare a precision shot.");
            state.combatStatus = "AIM · PRECISION READY";
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
        add("Loot found: " + drop.name + " [" + itemQuality(drop) + "].");
    }

    private Item randomEquipmentDrop(boolean uncommon) {
        String quality = uncommon ? "UNCOMMON" : "COMMON";
        if ("Mage".equals(state.heroClass)) return uncommon
            ? new Item("Runed Wand", "Weapon", 6, 0, 22, "Mage", false, quality, false)
            : new Item("Apprentice Wand", "Weapon", 3, 0, 8, "Mage", false, quality, false);
        if ("Rogue".equals(state.heroClass)) return uncommon
            ? new Item("Serrated Dagger", "Weapon", 7, 0, 22, "Rogue", false, quality, false)
            : new Item("Dagger", "Weapon", 5, 0, 8, "Rogue", false, quality, false);
        if ("Hunter".equals(state.heroClass)) return uncommon
            ? new Item("Hunting Crossbow", "Weapon", 9, 0, 25, "Hunter", false, quality, false)
            : new Item("Long Bow", "Weapon", 6, 0, 10, "Hunter", false, quality, false);
        return uncommon
            ? new Item(random.nextBoolean() ? "Flanged Mace" : "Warhammer", "Weapon",
                9, 0, 24, "Fighter", false, quality, false)
            : new Item("Iron Mace", "Weapon", 6, 0, 10, "Fighter", false, quality, false);
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
            add(state.spiderNestRemaining + " brood " +
                (state.spiderNestRemaining == 1 ? "clutch remains" : "clutches remain") +
                ". Search the nest when you are ready to continue.");
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
        if (enemy.tier == 1) {
            add("A rumor marks a relic-bearing elite somewhere near " +
                coordinate(target[0], target[1]) + ". Search the violet-marked region.");
        } else if (enemy.tier == 2) {
            add("Arcane signs warn of an overwhelming threat near " +
                coordinate(target[0], target[1]) + ". Its identity remains unknown.");
        } else {
            add("Travelers report danger near " + coordinate(target[0], target[1]) + ".");
        }
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
            if (state.enemyStunTurns > 0) {
                state.enemyStunTurns--;
                add("The " + enemy.name + " is stunned and loses its " + action.name + " turn.");
            } else {
                enemyTurn(enemy, action);
            }
            enemyActions++;
            state.enemyActionSequence++;
            state.consecutiveHeroActions = 0;
            forceResponse = false;
            EnemyAction following = nextEnemyAction(enemy, state.enemyActionSequence);
            state.enemyCombatClock = actedAt + scaledDelay(following.tempo.cost, state.enemySpeed);
        }
        if (enemyActions == 2 && state.enemyCombatClock <= state.heroCombatClock) {
            state.heroCombatClock = Math.max(0, state.enemyCombatClock - 1);
        }
        updateCombatPreview(enemy);
    }

    private void enemyTurn(Enemy enemy, EnemyAction action) {
        int healthBefore = state.health;
        int historyStart = history.length();
        if ("STEALTH".equals(state.combatPreparation)) {
            int evadeChance = isBoss(enemy) ? 35 : 60;
            if (random.nextInt(100) < evadeChance) {
                state.defending = false;
                add("The " + enemy.name + " uses " + action.name +
                    ", strikes at shadows, and misses you.");
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
            int relicGuard = identifiedRelicCount();
            damage = Math.max(1, damage - Math.min(3, relicGuard));
        }
        state.defending = false;
        state.health = Math.max(0, state.health - damage);
        add("The " + enemy.name + " uses " + action.name + " (" +
            action.tempo.label + ") and hits you for " + damage + " damage.");
        pendingEnemyTurnEvents.add(new EnemyTurnEvent(enemy.name, action.name,
            action.tempo.label, healthBefore, state.health, damage, false,
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
        state.enemyActionSequence = 0;
        state.combatTimelineRow = -1;
        state.combatTimelineCol = -1;
        state.heroSpeed = 0;
        state.enemySpeed = 0;
        state.enemyIntent = null;
        state.combatTimeline = null;
        state.combatStatus = null;
        state.enemyStunTurns = 0;
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
        if (abilityUnlocked(state, slot)) return "READY";
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
            "<br>L3 " + abilityName(state, 2) + " + " + abilityName(state, 3);
    }

    static String abilityTempoLabel(State state, int slot) {
        String name = abilityName(state, slot);
        if ("Second Wind".equals(name) || "Hunter's Mark".equals(name) ||
                "Offhand Strike".equals(name)) return ActionTempo.FAST.label;
        if ("Cleave".equals(name) || "Execute".equals(name) ||
                "Ice Spike".equals(name) || "Fireball".equals(name)) {
            return ActionTempo.SLOW.label;
        }
        return ActionTempo.NORMAL.label;
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
            if (!isBoss(enemy)) state.enemyStunTurns = 1;
            state.combatStatus = "PINNING SHOT · ENEMY DELAYED";
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
        } else if ("BLEEDING EDGE".equals(trait)) {
            raw += 3;
            tempo = ActionTempo.FAST;
        } else if ("BALANCED GUARD".equals(trait)) {
            state.defending = true;
        } else if ("PRECISE".equals(trait)) {
            raw += 3;
        }
        state.combatStatus = ability.toUpperCase() + " · MASTERY";
        resolveAbilityDamage(enemy, ability, raw, pierce, tempo);
    }

    private void useSignatureAbility(Enemy enemy) {
        String ability = abilityName(state, 3);
        if ("Second Wind".equals(ability)) {
            int restored = Math.min(18 + state.level, state.maxHealth - state.health);
            state.health += restored;
            state.combatStatus = "SECOND WIND · +" + restored + " HEALTH";
            add("Second Wind restores " + restored + " health.");
            completePlayerAction(enemy, ActionTempo.FAST);
            return;
        }
        if ("Hunter's Mark".equals(ability)) {
            state.combatPreparation = "MARK";
            state.combatStatus = "HUNTER'S MARK · NEXT SHOT EMPOWERED";
            add("You mark the " + enemy.name + " for a devastating next shot.");
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
        int armour = Math.max(0, enemy.defense - Math.max(0, armorPierce));
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
        if ("Bandit Marauder".equals(enemyName)) return "Drop your gold and I may leave you breathing.";
        if ("Skeletal Guardian".equals(enemyName)) return "None shall pass the forgotten gate.";
        if ("Dire Wolf".equals(enemyName)) return "A savage growl rolls through the trees.";
        return "The creature fixes its gaze on you and prepares to strike.";
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
                "Giant Spider".equals(name) || "Dark Elf Assassin".equals(name) ||
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
        items.add(new Item("Steel Long Sword", "Weapon", 9, 0, 30, "Fighter", false));
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
        items.add(new Item("Knight Plate", "Armour", 0, 7, 55, "Fighter", false));
        items.add(new Item("Shadowsteel Dirk", "Weapon", 8, 0, 28, "Rogue", false));
        items.add(new Item("Reinforced Leather", "Armour", 0, 4, 32,
            "Mage,Rogue,Hunter", false));
        items.add(new Item("Ashwood Staff", "Weapon", 7, 0, 28, "Mage", false));
        items.add(new Item("Mystic Robes", "Armour", 0, 3, 26, "Mage", false));
        items.add(new Item("Ranger Bow", "Weapon", 10, 0, 38, "Hunter", false));
        items.add(new Item("Reinforced Scale", "Armour", 0, 5, 40, "Hunter", false));
        items.add(new Item("Iron Shield", "Offhand", 0, 2, 24, "Fighter", false,
            "UNCOMMON", false));
        items.add(new Item("Runed Tome", "Offhand", 2, 0, 24, "Mage", false,
            "UNCOMMON", false));
        items.add(new Item("Balanced Offhand Dagger", "Offhand", 3, 0, 24, "Rogue", false,
            "UNCOMMON", false));
        items.add(new Item("Hunter's Quiver", "Offhand", 1, 1, 24, "Hunter", false,
            "UNCOMMON", false));
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
        int percent = "RARE".equals(itemQuality(item)) ? 50 :
            ("UNCOMMON".equals(itemQuality(item)) ? 40 : 25);
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
                "Greatsword", "Tempered Greatsword");
        }
        return named(item, "Dagger", "Short Sword", "Long Bow", "Crossbow", "Oak Staff",
            "Cloth Armour", "Leather Armour", "Reinforced Leather", "Ashwood Staff",
            "Mystic Robes", "Ranger Bow", "Runed Tome", "Hunter's Quiver");
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
            state.maxMana += "Mage".equals(state.heroClass) ? 6 : 2;
            state.mana = state.maxMana;
            add("You reached level " + state.level + "! Health and mana are restored.");
            unlockLevelAbilities();
            trainEquippedWeapon(Math.min(3, state.level), true);
            Item weapon = findItem(state.equippedWeapon);
            String family = weaponFamily(weapon);
            String abilities = "Mage".equals(state.heroClass)
                ? (state.level == 2 ? "Fireball" : "Ice Spike")
                : (state.level == 2 ? abilityName(state, 1) :
                    abilityName(state, 2) + " · " + abilityName(state, 3));
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
        if (state.level < 1) state.level = 1;
        if (state.baseAttack < 1) state.baseAttack = 5;
        if (state.inventory == null) state.inventory = new ArrayList<Item>();
        if (state.relics == null) state.relics = new ArrayList<Relic>();
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
        if ("Giant Forest Spider".equals(name) || "Giant Spider".equals(name)) return 115;
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
