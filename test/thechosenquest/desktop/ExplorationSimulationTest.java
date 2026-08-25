package thechosenquest.desktop;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic route telemetry for the 13x13 world.
 *
 * Unlike the isolated combat harness, this test preserves generated placement,
 * roaming enemies, travel discovery, rests, relic errands, and accumulated
 * resources. It intentionally reports outcomes instead of enforcing a target win
 * rate; MAP-005 establishes a baseline for later world and progression tuning.
 */
public final class ExplorationSimulationTest {
    private static final int SEEDS_PER_BUILD = 6;
    private static final int MAX_TRAVEL_STEPS = 600;
    private static final int MAX_COMBAT_TURNS = 100;

    private enum Strategy { QUEST_RUSH, PREPARED_ROUTE, PREPARED_WITH_NEST }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        File output = new File(args.length == 0
            ? "build/reports/exploration-report.md" : args[0]);
        File parent = output.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create report directory: " + parent);
        }

        List<Trial> trials = simulateAll();
        verifyCoverage(trials);
        PrintWriter writer = new PrintWriter(output, "UTF-8");
        try {
            writer.print(renderReport(trials));
        } finally {
            writer.close();
        }
        if (!output.isFile() || output.length() < 2000L) {
            throw new AssertionError("Exploration report was not produced correctly");
        }
        System.out.println("Exploration simulation passed (32 builds x " +
            Strategy.values().length + " routes x " +
            SEEDS_PER_BUILD + " seeds): " + output.getAbsolutePath());
    }

    private static List<Trial> simulateAll() {
        ArrayList<Trial> trials = new ArrayList<Trial>();
        for (String race : GameEngine.RACES) {
            for (String heroClass : GameEngine.CLASSES) {
                for (String kit : GameEngine.starterKitsFor(heroClass)) {
                    for (Strategy strategy : Strategy.values()) {
                        for (int seed = 0; seed < SEEDS_PER_BUILD; seed++) {
                            trials.add(runTrial(race, heroClass, kit, strategy,
                                stableSeed(race, heroClass, kit, strategy.name(), seed)));
                        }
                    }
                }
            }
        }
        return trials;
    }

    private static Trial runTrial(String race, String heroClass, String kit,
                                  Strategy strategy, long seed) {
        GameEngine engine = new GameEngine();
        engine.setRandomSeed(seed);
        engine.newGame("Route Scout", race, heroClass, kit);
        Trial trial = new Trial(race, heroClass, kit, strategy);

        if (strategy != Strategy.QUEST_RUSH) {
            clearTier(engine, trial, 0);
            if (strategy == Strategy.PREPARED_WITH_NEST) clearSpiderNest(engine, trial);
            clearTier(engine, trial, 1);
            identifyRelics(engine, trial);
            restIfNeeded(engine, trial, true);
        }
        if (engine.getState().health > 0 && !trial.timeout) {
            travelTo(engine, trial, engine.getState().dragonLairRow,
                engine.getState().dragonLairCol, 2);
            if (engine.currentEnemy() != null && engine.currentEnemy().tier == 2 &&
                    engine.getState().health > 0) {
                captureBossReadiness(engine, trial);
                fightCurrent(engine, trial);
            }
        }
        captureDiscovery(engine, trial);
        trial.finalLevel = engine.getState().level;
        trial.relicsIdentified = engine.identifiedRelicCount();
        trial.won = engine.getState().won;
        trial.survived = engine.getState().health > 0;
        return trial;
    }

    private static void clearTier(GameEngine engine, Trial trial, int tier) {
        while (engine.getState().health > 0 && countEnemies(engine, tier) > 0 &&
                !trial.timeout) {
            GameEngine.Enemy current = engine.currentEnemy();
            if (current != null) {
                fightCurrent(engine, trial);
                if (engine.getState().health > 0) restIfNeeded(engine, trial, false);
                continue;
            }
            int[] step = nextStepToTier(engine, tier);
            if (step == null) {
                trial.pathFailures++;
                return;
            }
            int previousRow = engine.getState().row;
            int previousCol = engine.getState().col;
            move(engine, trial, step[0], step[1]);
            if (engine.getState().row == previousRow &&
                    engine.getState().col == previousCol) {
                trial.pathFailures++;
                return;
            }
        }
    }

    private static void identifyRelics(GameEngine engine, Trial trial) {
        ArrayList<GameEngine.Relic> relics =
            new ArrayList<GameEngine.Relic>(engine.getState().relics);
        for (GameEngine.Relic relic : relics) {
            if (relic.identified || engine.getState().health <= 0 || trial.timeout) continue;
            int[] vendor = findVendor(engine.getState(), relic.vendor);
            if (vendor == null) {
                trial.pathFailures++;
                continue;
            }
            travelTo(engine, trial, vendor[0], vendor[1], -1);
            if (engine.getState().row == vendor[0] && engine.getState().col == vendor[1]) {
                engine.identifyRelic(relic);
                if ((engine.currentTile() == GameEngine.TileType.TAVERN ||
                        engine.currentTile() == GameEngine.TileType.ENCAMPMENT) &&
                        needsRest(engine.getState())) {
                    engine.locationAction();
                    trial.rests++;
                }
                if ("General Merchant".equals(relic.vendor)) buyPotions(engine, trial);
            }
        }
    }

    private static void clearSpiderNest(GameEngine engine, Trial trial) {
        GameEngine.State state = engine.getState();
        while (state.health > 0 && !state.spiderNestCleared && !trial.timeout) {
            travelTo(engine, trial, state.spiderNestRow, state.spiderNestCol, 0);
            if (state.health <= 0 || trial.timeout) return;
            if (engine.currentEnemy() == null) engine.locationAction();
            if (engine.currentEnemy() != null) fightCurrent(engine, trial);
            if (state.health > 0 && !state.spiderNestCleared) restIfNeeded(engine, trial, false);
        }
    }

    private static void restIfNeeded(GameEngine engine, Trial trial, boolean beforeBoss) {
        GameEngine.State state = engine.getState();
        int healthThreshold = beforeBoss ? 100 : 65;
        int manaThreshold = beforeBoss ? 100 : 35;
        boolean pressured = state.health * 100 < state.maxHealth * healthThreshold ||
            ("Mage".equals(state.heroClass) && state.mana * 100 < state.maxMana * manaThreshold);
        if (!pressured || state.health <= 0) return;
        int[] haven = nearestHavenStepTarget(engine);
        if (haven == null) return;
        travelTo(engine, trial, haven[0], haven[1], -1);
        if (engine.getState().row == haven[0] && engine.getState().col == haven[1] &&
                engine.currentEnemy() == null) {
            engine.locationAction();
            trial.rests++;
        }
    }

    private static void buyPotions(GameEngine engine, Trial trial) {
        while (engine.getState().potions < 3 && engine.getState().gold >= 10) {
            int before = engine.getState().potions;
            engine.buyPotion();
            if (engine.getState().potions == before) break;
            trial.potionsBought++;
        }
    }

    private static void fightCurrent(GameEngine engine, Trial trial) {
        GameEngine.Enemy enemy = engine.currentEnemy();
        if (enemy == null) return;
        if (enemy.tier == 0) trial.standardEncounters++;
        else if (enemy.tier == 1) trial.eliteEncounters++;
        else trial.bossEncounters++;

        boolean secondWindUsed = false;
        int turn = 0;
        while (engine.getState().health > 0 && engine.currentEnemy() == enemy &&
                enemy.health > 0 && turn < MAX_COMBAT_TURNS) {
            GameEngine.State state = engine.getState();
            if (state.health < state.maxHealth && state.potions > 0 &&
                    state.health * 100 <= state.maxHealth * 32) {
                int before = state.potions;
                engine.usePotion();
                if (state.potions < before) trial.potionsUsed++;
            } else {
                secondWindUsed = takeTacticalAction(engine, turn, secondWindUsed);
            }
            engine.consumeEnemyTurnEvents();
            trial.combatTurns++;
            turn++;
        }
        if (turn >= MAX_COMBAT_TURNS && engine.currentEnemy() == enemy && enemy.health > 0) {
            trial.timeout = true;
        }
    }

    private static boolean takeTacticalAction(GameEngine engine, int turn,
                                              boolean secondWindUsed) {
        GameEngine.State state = engine.getState();
        GameEngine.Enemy enemy = engine.currentEnemy();
        if ("Mage".equals(state.heroClass)) {
            if (GameEngine.abilityUnlocked(state, 3) &&
                    state.mana >= GameEngine.spellCost("Ice Spike")) engine.useAbility(3);
            else if (GameEngine.abilityUnlocked(state, 2) &&
                    state.mana >= GameEngine.spellCost("Fireball")) engine.useAbility(2);
            else if (state.mana >= GameEngine.spellCost("Magic Missile")) engine.useAbility(1);
            else engine.defend();
            return secondWindUsed;
        }
        if ("Fighter".equals(state.heroClass)) {
            if (GameEngine.abilityAvailable(state, 2) && turn % 4 == 2) engine.useAbility(2);
            else if (GameEngine.abilityAvailable(state, 1) && turn % 3 == 1) engine.useAbility(1);
            else if (turn % 4 == 0) engine.defend();
            else engine.attack();
            return state.secondWindUsed;
        }
        if ("Rogue".equals(state.heroClass)) {
            if (GameEngine.abilityAvailable(state, 3) && enemy != null &&
                    enemy.health * 100 <= enemy.maxHealth * 35) engine.useAbility(3);
            else if (GameEngine.abilityAvailable(state, 2) && turn % 4 == 2) engine.useAbility(2);
            else if (GameEngine.abilityAvailable(state, 1) && turn % 3 == 1) engine.useAbility(1);
            else if (state.combatPreparation == null && turn % 3 == 0) engine.defend();
            else engine.attack();
            return secondWindUsed;
        }
        if (GameEngine.abilityAvailable(state, 2)) engine.useAbility(2);
        else if (!state.enemyMarked && GameEngine.abilityAvailable(state, 3)) engine.useAbility(3);
        else if (GameEngine.abilityAvailable(state, 1) && turn % 3 == 1) engine.useAbility(1);
        else if (state.combatPreparation == null &&
                (state.focus < 35 || turn % 3 == 0)) engine.defend();
        else engine.attack();
        return secondWindUsed;
    }

    private static void captureBossReadiness(GameEngine engine, Trial trial) {
        GameEngine.State state = engine.getState();
        trial.reachedBoss = true;
        trial.levelAtBoss = state.level;
        trial.healthAtBossPercent = percentValue(state.health, state.maxHealth);
        trial.manaAtBossPercent = percentValue(state.mana, state.maxMana);
        trial.relicsAtBoss = engine.identifiedRelicCount();
        trial.potionsAtBoss = state.potions;
    }

    private static void captureDiscovery(GameEngine engine, Trial trial) {
        for (int row = 0; row < GameEngine.SIZE; row++) {
            for (int col = 0; col < GameEngine.SIZE; col++) {
                GameEngine.DiscoveryState discovery = engine.discoveryAt(row, col);
                if (discovery == GameEngine.DiscoveryState.VISITED) trial.visitedTiles++;
                if (discovery != GameEngine.DiscoveryState.UNKNOWN) trial.knownTiles++;
            }
        }
    }

    private static int countEnemies(GameEngine engine, int tier) {
        int count = 0;
        for (GameEngine.Enemy[] row : engine.getState().enemies) {
            for (GameEngine.Enemy enemy : row) {
                if (enemy != null && enemy.tier == tier && enemy.sourceId == null) count++;
            }
        }
        return count;
    }

    private static void travelTo(GameEngine engine, Trial trial, int row, int col,
                                 int allowedTier) {
        while (engine.getState().health > 0 && !trial.timeout &&
                (engine.getState().row != row || engine.getState().col != col)) {
            int[] step = nextStep(engine, row, col, allowedTier, false);
            // Roaming enemies can temporarily seal the only approaches to a
            // corner landmark. A player can still proceed by fighting through;
            // use that route only when no enemy-free path exists.
            if (step == null) {
                step = breadthFirstStep(engine, row, col, allowedTier, false, true);
            }
            if (step == null) {
                trial.pathFailures++;
                return;
            }
            int previousRow = engine.getState().row;
            int previousCol = engine.getState().col;
            move(engine, trial, step[0], step[1]);
            if (engine.getState().row == previousRow &&
                    engine.getState().col == previousCol) {
                if (row == engine.getState().dragonLairRow &&
                        col == engine.getState().dragonLairCol &&
                        !engine.dragonSealReady()) {
                    trial.sealBlocked = true;
                } else {
                    trial.pathFailures++;
                }
                return;
            }
            // Let the caller capture boss-entry resources before combat begins.
            if (engine.getState().row == row && engine.getState().col == col) return;
            if (engine.currentEnemy() != null) {
                fightCurrent(engine, trial);
            }
        }
    }

    private static void move(GameEngine engine, Trial trial, int row, int col) {
        GameEngine.State state = engine.getState();
        int oldRow = state.row;
        int oldCol = state.col;
        engine.move(row - oldRow, col - oldCol);
        if (state.row != oldRow || state.col != oldCol) trial.moves++;
        if (trial.moves >= MAX_TRAVEL_STEPS) trial.timeout = true;
    }

    private static int[] nextStepToTier(GameEngine engine, int tier) {
        return breadthFirstStep(engine, -1, -1, tier, true, false);
    }

    private static int[] nextStep(GameEngine engine, int targetRow, int targetCol,
                                  int allowedTier, boolean anyTierTarget) {
        return breadthFirstStep(engine, targetRow, targetCol, allowedTier,
            anyTierTarget, false);
    }

    private static int[] nearestHavenStepTarget(GameEngine engine) {
        GameEngine.State state = engine.getState();
        int bestDistance = Integer.MAX_VALUE;
        int[] best = null;
        for (int row = 0; row < GameEngine.SIZE; row++) {
            for (int col = 0; col < GameEngine.SIZE; col++) {
                GameEngine.TileType tile = state.tiles[row][col];
                if (tile != GameEngine.TileType.TAVERN &&
                        tile != GameEngine.TileType.ENCAMPMENT) continue;
                int distance = Math.abs(row - state.row) + Math.abs(col - state.col);
                if (distance < bestDistance && nextStep(engine, row, col, -1, false) != null) {
                    bestDistance = distance;
                    best = new int[] {row, col};
                }
            }
        }
        return best;
    }

    private static int[] breadthFirstStep(GameEngine engine, int targetRow, int targetCol,
                                          int allowedTier, boolean anyTierTarget,
                                          boolean allowAnyEnemy) {
        GameEngine.State state = engine.getState();
        int start = state.row * GameEngine.SIZE + state.col;
        int[] parent = new int[GameEngine.SIZE * GameEngine.SIZE];
        for (int i = 0; i < parent.length; i++) parent[i] = -2;
        parent[start] = -1;
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        queue.add(Integer.valueOf(start));
        int found = -1;
        int[][] directions = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
        while (!queue.isEmpty()) {
            int cell = queue.remove().intValue();
            int row = cell / GameEngine.SIZE;
            int col = cell % GameEngine.SIZE;
            if ((anyTierTarget && state.enemies[row][col] != null &&
                    state.enemies[row][col].tier == allowedTier &&
                    state.enemies[row][col].sourceId == null) ||
                    (!anyTierTarget && row == targetRow && col == targetCol)) {
                found = cell;
                break;
            }
            for (int[] direction : directions) {
                int nextRow = row + direction[0];
                int nextCol = col + direction[1];
                if (nextRow < 0 || nextRow >= GameEngine.SIZE ||
                        nextCol < 0 || nextCol >= GameEngine.SIZE) continue;
                int next = nextRow * GameEngine.SIZE + nextCol;
                if (parent[next] != -2) continue;
                GameEngine.Enemy occupant = state.enemies[nextRow][nextCol];
                boolean target = (anyTierTarget && occupant != null &&
                    occupant.tier == allowedTier && occupant.sourceId == null) ||
                    (!anyTierTarget && nextRow == targetRow && nextCol == targetCol);
                if (occupant != null && !allowAnyEnemy &&
                        !(target && occupant.tier == allowedTier)) continue;
                parent[next] = cell;
                queue.add(Integer.valueOf(next));
            }
        }
        if (found < 0 || found == start) return null;
        int step = found;
        while (parent[step] != start && parent[step] >= 0) step = parent[step];
        return new int[] {step / GameEngine.SIZE, step % GameEngine.SIZE};
    }

    private static int[] findVendor(GameEngine.State state, String vendor) {
        for (int row = 0; row < GameEngine.SIZE; row++) {
            for (int col = 0; col < GameEngine.SIZE; col++) {
                if ("Blacksmith".equals(vendor) && state.tiles[row][col] ==
                        GameEngine.TileType.SHOP && state.blacksmithShops[row][col]) {
                    return new int[] {row, col};
                }
                if ("General Merchant".equals(vendor) && state.tiles[row][col] ==
                        GameEngine.TileType.SHOP && !state.blacksmithShops[row][col]) {
                    return new int[] {row, col};
                }
                if ("Innkeeper".equals(vendor) &&
                        state.tiles[row][col] == GameEngine.TileType.TAVERN) {
                    return new int[] {row, col};
                }
                if ("Alchemist".equals(vendor) &&
                        state.tiles[row][col] == GameEngine.TileType.ENCAMPMENT) {
                    return new int[] {row, col};
                }
            }
        }
        return null;
    }

    private static boolean needsRest(GameEngine.State state) {
        return state.health < state.maxHealth || state.mana < state.maxMana;
    }

    private static void verifyCoverage(List<Trial> trials) {
        int expected = GameEngine.RACES.length * GameEngine.CLASSES.length * 2 *
            Strategy.values().length * SEEDS_PER_BUILD;
        if (trials.size() != expected) {
            throw new AssertionError("Expected " + expected + " route trials, found " +
                trials.size());
        }
        for (Trial trial : trials) {
            if (trial.timeout) throw new AssertionError("Route did not terminate: " + trial.label());
            if (trial.pathFailures > 0) {
                throw new AssertionError("Route lost a valid path: " + trial.label());
            }
        }
    }

    private static String renderReport(List<Trial> trials) {
        StringBuilder out = new StringBuilder();
        out.append("# Deterministic Exploration Pacing Report\n\n");
        out.append("Generated by `ExplorationSimulationTest` from ").append(trials.size())
            .append(" complete route trials ")
            .append("across every race, class, loadout, and deterministic world seed.\n\n");
        out.append("## Route policies\n\n");
        out.append("- **Quest rush:** avoids optional enemies and follows the shortest safe path to the dragon.\n");
        out.append("- **Prepared route:** clears five standard enemies, clears three elites, rests when pressured, identifies each relic at its authored vendor, then approaches the dragon.\n");
        out.append("- **Prepared + nest:** follows the prepared policy and permanently clears all three finite Ashweb broods before the elite route.\n");
        out.append("- Both policies use the same class-aware tactical combat rules and potion threshold. Results are a reproducible baseline, not a substitute for human playtests.\n\n");
        out.append("## Overall pacing\n\n");
        out.append("| Route | Trials | Boss reached | Quest wins | Avg moves | Avg fights | Avg combat turns | Avg rests | Avg potions used | Avg known map |\n");
        out.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Strategy strategy : Strategy.values()) appendAggregateRow(out, trials, strategy, null, null);

        out.append("\n## Prepared-route readiness by class and loadout\n\n");
        out.append("| Build | Trials | Boss reached | Quest wins | Level at boss | HP at boss | Relics | Moves | Rests | Potions used |\n");
        out.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (String heroClass : GameEngine.CLASSES) {
            for (String kit : GameEngine.starterKitsFor(heroClass)) {
                appendPreparedBuildRow(out, trials, heroClass, kit);
            }
        }

        Aggregate rush = aggregate(trials, Strategy.QUEST_RUSH, null, null);
        Aggregate prepared = aggregate(trials, Strategy.PREPARED_ROUTE, null, null);
        Aggregate nest = aggregate(trials, Strategy.PREPARED_WITH_NEST, null, null);
        out.append("\n## Baseline findings\n\n");
        out.append("- The 13x13 world asks for **").append(decimal(prepared.moves, prepared.trials))
            .append(" movement steps** on the prepared route versus **")
            .append(decimal(rush.moves, rush.trials)).append("** on a quest rush.\n");
        out.append("- Prepared heroes reach the boss at average level **")
            .append(decimal(prepared.levelAtBoss, prepared.reachedBoss))
            .append("** with **").append(decimal(prepared.relicsAtBoss, prepared.reachedBoss))
            .append(" identified relics** and **")
            .append(decimal(prepared.healthAtBoss, prepared.reachedBoss)).append("% health**.\n");
        out.append("- Prepared-route map knowledge averages **")
            .append(decimal(prepared.knownPercent, prepared.trials))
            .append("%**, a useful baseline before a larger map or renewable enemy source is added.\n");
        out.append("- Clearing the finite nest adds **")
            .append(decimal(nest.moves * prepared.trials - prepared.moves * nest.trials,
                nest.trials * prepared.trials))
            .append(" movement steps** on average; its three reduced-reward broods cannot become an unlimited XP or loot farm.\n");
        out.append("- Compare the prepared routes during playtesting: the nest should feel like an optional safety objective, not a mandatory progression gate.\n");
        return out.toString();
    }

    private static void appendAggregateRow(StringBuilder out, List<Trial> trials,
                                           Strategy strategy, String heroClass, String kit) {
        Aggregate value = aggregate(trials, strategy, heroClass, kit);
        out.append("| ").append(strategyLabel(strategy))
            .append(" | ").append(value.trials)
            .append(" | ").append(percent(value.reachedBoss, value.trials))
            .append(" | ").append(percent(value.wins, value.trials))
            .append(" | ").append(decimal(value.moves, value.trials))
            .append(" | ").append(decimal(value.fights, value.trials))
            .append(" | ").append(decimal(value.combatTurns, value.trials))
            .append(" | ").append(decimal(value.rests, value.trials))
            .append(" | ").append(decimal(value.potionsUsed, value.trials))
            .append(" | ").append(decimal(value.knownPercent, value.trials)).append("% |\n");
    }

    private static String strategyLabel(Strategy strategy) {
        if (strategy == Strategy.QUEST_RUSH) return "Quest rush";
        if (strategy == Strategy.PREPARED_WITH_NEST) return "Prepared + nest";
        return "Prepared route";
    }

    private static void appendPreparedBuildRow(StringBuilder out, List<Trial> trials,
                                               String heroClass, String kit) {
        Aggregate value = aggregate(trials, Strategy.PREPARED_ROUTE, heroClass, kit);
        out.append("| ").append(heroClass).append(" · ").append(kit)
            .append(" | ").append(value.trials)
            .append(" | ").append(percent(value.reachedBoss, value.trials))
            .append(" | ").append(percent(value.wins, value.trials))
            .append(" | ").append(decimal(value.levelAtBoss, value.reachedBoss))
            .append(" | ").append(decimal(value.healthAtBoss, value.reachedBoss)).append("%")
            .append(" | ").append(decimal(value.relicsAtBoss, value.reachedBoss))
            .append(" | ").append(decimal(value.moves, value.trials))
            .append(" | ").append(decimal(value.rests, value.trials))
            .append(" | ").append(decimal(value.potionsUsed, value.trials)).append(" |\n");
    }

    private static Aggregate aggregate(List<Trial> trials, Strategy strategy,
                                       String heroClass, String kit) {
        Aggregate result = new Aggregate();
        for (Trial trial : trials) {
            if (trial.strategy != strategy ||
                    (heroClass != null && !heroClass.equals(trial.heroClass)) ||
                    (kit != null && !kit.equals(trial.kit))) continue;
            result.add(trial);
        }
        return result;
    }

    private static int percentValue(int value, int maximum) {
        return maximum <= 0 ? 0 : Math.max(0, value) * 100 / maximum;
    }

    private static String percent(long value, long total) {
        return String.format(Locale.US, "%.1f%%", total == 0 ? 0d : value * 100d / total);
    }

    private static String decimal(long value, long total) {
        return String.format(Locale.US, "%.1f", total == 0 ? 0d : value * 1d / total);
    }

    private static long stableSeed(String race, String heroClass, String kit,
                                   String route, int trial) {
        long value = 1469598103934665603L;
        String key = race + '|' + heroClass + '|' + kit + '|' + route + '|' + trial;
        for (int i = 0; i < key.length(); i++) {
            value ^= key.charAt(i);
            value *= 1099511628211L;
        }
        return value;
    }

    private static final class Trial {
        final String race;
        final String heroClass;
        final String kit;
        final Strategy strategy;
        int moves;
        int standardEncounters;
        int eliteEncounters;
        int bossEncounters;
        int combatTurns;
        int rests;
        int potionsUsed;
        int potionsBought;
        int levelAtBoss;
        int healthAtBossPercent;
        int manaAtBossPercent;
        int relicsAtBoss;
        int potionsAtBoss;
        int finalLevel;
        int relicsIdentified;
        int visitedTiles;
        int knownTiles;
        int pathFailures;
        boolean reachedBoss;
        boolean won;
        boolean survived;
        boolean timeout;
        boolean sealBlocked;

        Trial(String race, String heroClass, String kit, Strategy strategy) {
            this.race = race;
            this.heroClass = heroClass;
            this.kit = kit;
            this.strategy = strategy;
        }

        String label() { return race + ' ' + heroClass + " / " + kit + " / " + strategy; }
    }

    private static final class Aggregate {
        int trials;
        int reachedBoss;
        int wins;
        long moves;
        long fights;
        long combatTurns;
        long rests;
        long potionsUsed;
        long levelAtBoss;
        long healthAtBoss;
        long relicsAtBoss;
        long knownPercent;

        void add(Trial trial) {
            trials++;
            if (trial.reachedBoss) reachedBoss++;
            if (trial.won) wins++;
            moves += trial.moves;
            fights += trial.standardEncounters + trial.eliteEncounters + trial.bossEncounters;
            combatTurns += trial.combatTurns;
            rests += trial.rests;
            potionsUsed += trial.potionsUsed;
            if (trial.reachedBoss) {
                levelAtBoss += trial.levelAtBoss;
                healthAtBoss += trial.healthAtBossPercent;
                relicsAtBoss += trial.relicsAtBoss;
            }
            knownPercent += trial.knownTiles * 100 / (GameEngine.SIZE * GameEngine.SIZE);
        }
    }
}
