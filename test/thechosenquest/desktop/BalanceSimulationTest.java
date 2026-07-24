package thechosenquest.desktop;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic balance telemetry for every race, class, and starter loadout.
 *
 * This is intentionally a measurement harness, not a collection of fragile win-rate
 * assertions. It fails on missing coverage or non-terminating combat; fixed seeds keep
 * output comparable between revisions, while tuning decisions belong to BAL-002.
 */
public final class BalanceSimulationTest {
    private static final int SEEDS_PER_BUILD = 12;
    private static final int MAX_PLAYER_TURNS = 80;

    private static final Scenario[] SCENARIOS = {
        new Scenario("STANDARD", 1, "Skeletal Guardian", 28, 9, 11, 0, false),
        new Scenario("ELITE", 2, "Orc Warlord", 62, 14, 42, 1, false),
        new Scenario("BOSS", 3, "Corrupted Shadow Dragon", 138, 19, 340, 2, true)
    };

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        File output = new File(args.length == 0
            ? "build/reports/balance-report.md" : args[0]);
        File parent = output.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create report directory: " + parent);
        }

        List<Aggregate> results = simulateAll();
        verifyCoverage(results);
        String report = renderReport(results);
        PrintWriter writer = new PrintWriter(output, "UTF-8");
        try {
            writer.print(report);
        } finally {
            writer.close();
        }
        if (!output.isFile() || output.length() < 4000L) {
            throw new AssertionError("Balance report was not produced correctly");
        }
        System.out.println("Balance simulation passed (32 builds x 3 tiers x " +
            SEEDS_PER_BUILD + " seeds): " + output.getAbsolutePath());
    }

    private static List<Aggregate> simulateAll() {
        ArrayList<Aggregate> results = new ArrayList<Aggregate>();
        for (String race : GameEngine.RACES) {
            for (String heroClass : GameEngine.CLASSES) {
                for (String kit : GameEngine.starterKitsFor(heroClass)) {
                    for (Scenario scenario : SCENARIOS) {
                        Aggregate aggregate = new Aggregate(race, heroClass, kit, scenario);
                        for (int seed = 0; seed < SEEDS_PER_BUILD; seed++) {
                            long value = stableSeed(race, heroClass, kit, scenario.name, seed);
                            aggregate.add(runTrial(race, heroClass, kit, scenario, value));
                        }
                        results.add(aggregate);
                    }
                }
            }
        }
        return results;
    }

    private static Trial runTrial(String race, String heroClass, String kit,
                                  Scenario scenario, long seed) {
        GameEngine engine = new GameEngine();
        engine.setRandomSeed(seed);
        engine.newGame("Balance Hero", race, heroClass, kit);
        prepareCheckpoint(engine, scenario);
        GameEngine.State state = engine.getState();
        GameEngine.Enemy enemy = new GameEngine.Enemy(scenario.enemyName,
            scenario.health, scenario.attack, scenario.reward, scenario.tier);
        state.enemies[state.row][state.col] = enemy;
        state.threatKnowledge[state.row][state.col] = 2;

        int startingMana = state.mana;
        int startingPotions = state.potions;
        int dealt = 0;
        int incomingDamage = 0;
        int turns = 0;
        while (state.health > 0 && enemy.health > 0 && turns < MAX_PLAYER_TURNS) {
            int enemyBefore = enemy.health;
            takeTacticalAction(engine, scenario, turns, false);
            dealt += Math.max(0, enemyBefore - enemy.health);
            for (GameEngine.EnemyTurnEvent event : engine.consumeEnemyTurnEvents()) {
                incomingDamage += event.damage;
            }
            turns++;
        }
        return new Trial(enemy.health == 0, turns, incomingDamage,
            dealt, startingMana - state.mana, startingPotions - state.potions,
            state.health, state.maxHealth, turns >= MAX_PLAYER_TURNS && enemy.health > 0);
    }

    private static void prepareCheckpoint(GameEngine engine, Scenario scenario) {
        GameEngine.State state = engine.getState();
        int levels = scenario.level - 1;
        state.level = scenario.level;
        if ("Hunter".equals(state.heroClass)) {
            state.maxFocus = GameEngine.maxFocusForLevel(state.level);
            state.focus = state.maxFocus / 3;
        }
        state.maxHealth += levels * 8;
        state.health = state.maxHealth;
        state.maxMana += levels * ("Mage".equals(state.heroClass) ? 6 : 2);
        state.mana = state.maxMana;
        if ("Mage".equals(state.heroClass)) {
            if (scenario.level >= 2 && !state.spells.contains("Fireball")) {
                state.spells.add("Fireball");
            }
            if (scenario.level >= 3 && !state.spells.contains("Ice Spike")) {
                state.spells.add("Ice Spike");
            }
        }
        GameEngine.Item weapon = GameEngine.equippedWeapon(state);
        String family = GameEngine.weaponFamily(weapon);
        if (family != null) {
            state.weaponProficiency.put(family, Integer.valueOf(scenario.level));
        }
        if (scenario.identifiedRelic) addDefensiveRelic(state);
    }

    private static void addDefensiveRelic(GameEngine.State state) {
        GameEngine.Relic relic = new GameEngine.Relic(
            "Balance Warding Relic", "Simulation Elite", "Alchemist");
        relic.identified = true;
        relic.rewardName = "Balance Warding Armour";
        state.relics.add(relic);
        int defense = "Fighter".equals(state.heroClass) ? 7 :
            ("Mage".equals(state.heroClass) ? 5 : 6);
        GameEngine.Item armour = new GameEngine.Item("Balance Warding Armour", "Armour",
            0, defense, 0, state.heroClass, true, "RELIC", false);
        state.inventory.add(armour);
        state.equippedArmour = armour.name;
    }

    private static void takeTacticalAction(GameEngine engine, Scenario scenario, int turn,
                                           boolean useSecondWind) {
        GameEngine.State state = engine.getState();
        GameEngine.Enemy enemy = engine.currentEnemy();
        if (state.health < state.maxHealth && state.potions > 0 &&
                state.health * 100 <= state.maxHealth * 32) {
            engine.usePotion();
            return;
        }

        if ("Mage".equals(state.heroClass)) {
            if (scenario.level >= 3 && state.mana >= GameEngine.spellCost("Ice Spike")) {
                engine.useAbility(3);
            } else if (scenario.level >= 2 &&
                    state.mana >= GameEngine.spellCost("Fireball")) {
                engine.useAbility(2);
            } else if (state.mana >= GameEngine.spellCost("Magic Missile")) {
                engine.useAbility(1);
            } else {
                engine.defend();
            }
            return;
        }

        if (useSecondWind) {
            engine.useAbility(3);
            return;
        }
        if ("Rogue".equals(state.heroClass)) {
            if (scenario.level >= 3 && enemy != null &&
                    enemy.health * 100 <= enemy.maxHealth * 35 &&
                    GameEngine.abilityAvailable(state, 3)) {
                engine.useAbility(3);
            } else if (scenario.level >= 3 && turn % 4 == 2 &&
                    GameEngine.abilityAvailable(state, 2)) {
                engine.useAbility(2);
            } else if (scenario.level >= 2 && turn % 3 == 1 &&
                    GameEngine.abilityAvailable(state, 1)) {
                engine.useAbility(1);
            } else if (state.combatPreparation == null && turn % 3 == 0) {
                engine.defend();
            } else {
                engine.attack();
            }
            return;
        }
        if ("Hunter".equals(state.heroClass)) {
            if (scenario.level >= 3 && GameEngine.abilityAvailable(state, 2)) {
                engine.useAbility(2);
            } else if (scenario.level >= 3 && !state.enemyMarked &&
                    GameEngine.abilityAvailable(state, 3)) {
                engine.useAbility(3);
            } else if (scenario.level >= 2 && turn % 3 == 1 &&
                    GameEngine.abilityAvailable(state, 1)) {
                engine.useAbility(1);
            } else if (state.combatPreparation == null &&
                    (state.focus < 35 || turn % 3 == 0)) {
                engine.defend();
            } else {
                engine.attack();
            }
            return;
        }

        if (scenario.level >= 3 && turn % 4 == 2 &&
                GameEngine.abilityAvailable(state, 2)) {
            engine.useAbility(2);
        } else if (scenario.level >= 2 && turn % 3 == 1 &&
                GameEngine.abilityAvailable(state, 1)) {
            engine.useAbility(1);
        } else if (turn % 3 == 0) {
            engine.defend();
        } else {
            engine.attack();
        }
    }

    private static void verifyCoverage(List<Aggregate> results) {
        int expected = GameEngine.RACES.length * GameEngine.CLASSES.length * 2 *
            SCENARIOS.length;
        if (results.size() != expected) {
            throw new AssertionError("Expected " + expected + " aggregate rows, found " +
                results.size());
        }
        for (Aggregate result : results) {
            if (result.trials != SEEDS_PER_BUILD) {
                throw new AssertionError("Missing trials for " + result.label());
            }
            if (result.timeouts > 0) {
                throw new AssertionError("Combat did not terminate for " + result.label());
            }
        }
    }

    private static String renderReport(List<Aggregate> results) {
        StringBuilder out = new StringBuilder();
        out.append("# Deterministic Balance Report\n\n");
        out.append("Generated by `BalanceSimulationTest`. Each row represents ")
            .append(SEEDS_PER_BUILD).append(" deterministic trials. This report measures a ")
            .append("consistent tactical policy; it does not claim to model every player.\n\n");
        out.append("## Checkpoints\n\n");
        out.append("- **Standard:** level 1, Common starting loadout.\n");
        out.append("- **Elite:** level 2, starting loadout retained.\n");
        out.append("- **Boss:** level 3, mastered starting weapon and one identified defensive relic.\n\n");
        out.append("## Results\n\n");
        out.append("| Build | Loadout | Tier | Win rate | Avg turns | Avg HP left | Avg damage dealt | Avg damage taken | Avg mana used | Avg potions |\n");
        out.append("|---|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Aggregate result : results) {
            out.append("| ").append(result.race).append(' ').append(result.heroClass)
                .append(" | ").append(result.kit).append(" | ").append(result.scenario.name)
                .append(" | ").append(percent(result.wins, result.trials))
                .append(" | ").append(decimal(result.turns, result.trials))
                .append(" | ").append(decimal(result.healthRemainingPercent, result.trials))
                .append("% | ").append(decimal(result.damageDealt, result.trials))
                .append(" | ").append(decimal(result.damageTaken, result.trials))
                .append(" | ").append(decimal(result.manaUsed, result.trials))
                .append(" | ").append(decimal(result.potionsUsed, result.trials))
                .append(" |\n");
        }
        appendTierSummary(out, results);
        out.append("## Interpretation rules\n\n");
        out.append("- Compare builds within the same tier; checkpoint equipment differs by tier.\n");
        out.append("- A low result is a tuning signal for `BAL-002`, not automatically a defect.\n");
        out.append("- Human playtests remain authoritative for clarity, fun, and tactical agency.\n");
        return out.toString();
    }

    private static void appendTierSummary(StringBuilder out, List<Aggregate> results) {
        out.append("\n## Tier summary\n\n");
        out.append("| Tier | Trials | Win rate | Avg turns | Avg HP left |\n");
        out.append("|---|---:|---:|---:|---:|\n");
        for (Scenario scenario : SCENARIOS) {
            int trials = 0;
            int wins = 0;
            long turns = 0;
            long hp = 0;
            for (Aggregate result : results) {
                if (!scenario.name.equals(result.scenario.name)) continue;
                trials += result.trials;
                wins += result.wins;
                turns += result.turns;
                hp += result.healthRemainingPercent;
            }
            out.append("| ").append(scenario.name).append(" | ").append(trials)
                .append(" | ").append(percent(wins, trials))
                .append(" | ").append(decimal(turns, trials))
                .append(" | ").append(decimal(hp, trials)).append("% |\n");
        }
    }

    private static String percent(long value, long total) {
        return String.format(Locale.US, "%.1f%%", total == 0 ? 0d : value * 100d / total);
    }

    private static String decimal(long value, long total) {
        return String.format(Locale.US, "%.1f", total == 0 ? 0d : value * 1d / total);
    }

    private static long stableSeed(String race, String heroClass, String kit,
                                   String tier, int trial) {
        long value = 1469598103934665603L;
        String key = race + '|' + heroClass + '|' + kit + '|' + tier + '|' + trial;
        for (int i = 0; i < key.length(); i++) {
            value ^= key.charAt(i);
            value *= 1099511628211L;
        }
        return value;
    }

    private static final class Scenario {
        final String name;
        final int level;
        final String enemyName;
        final int health;
        final int attack;
        final int reward;
        final int tier;
        final boolean identifiedRelic;

        Scenario(String name, int level, String enemyName, int health, int attack,
                 int reward, int tier, boolean identifiedRelic) {
            this.name = name;
            this.level = level;
            this.enemyName = enemyName;
            this.health = health;
            this.attack = attack;
            this.reward = reward;
            this.tier = tier;
            this.identifiedRelic = identifiedRelic;
        }
    }

    private static final class Trial {
        final boolean won;
        final int turns;
        final int damageTaken;
        final int damageDealt;
        final int manaUsed;
        final int potionsUsed;
        final int healthRemaining;
        final int maxHealth;
        final boolean timeout;

        Trial(boolean won, int turns, int damageTaken, int damageDealt, int manaUsed,
              int potionsUsed, int healthRemaining, int maxHealth, boolean timeout) {
            this.won = won;
            this.turns = turns;
            this.damageTaken = damageTaken;
            this.damageDealt = damageDealt;
            this.manaUsed = manaUsed;
            this.potionsUsed = potionsUsed;
            this.healthRemaining = healthRemaining;
            this.maxHealth = maxHealth;
            this.timeout = timeout;
        }
    }

    private static final class Aggregate {
        final String race;
        final String heroClass;
        final String kit;
        final Scenario scenario;
        int trials;
        int wins;
        int timeouts;
        long turns;
        long damageTaken;
        long damageDealt;
        long manaUsed;
        long potionsUsed;
        long healthRemainingPercent;

        Aggregate(String race, String heroClass, String kit, Scenario scenario) {
            this.race = race;
            this.heroClass = heroClass;
            this.kit = kit;
            this.scenario = scenario;
        }

        void add(Trial trial) {
            trials++;
            if (trial.won) wins++;
            if (trial.timeout) timeouts++;
            turns += trial.turns;
            damageTaken += Math.max(0, trial.damageTaken);
            damageDealt += trial.damageDealt;
            manaUsed += Math.max(0, trial.manaUsed);
            potionsUsed += trial.potionsUsed;
            healthRemainingPercent += trial.maxHealth == 0 ? 0 :
                Math.max(0, trial.healthRemaining) * 100 / trial.maxHealth;
        }

        String label() {
            return race + ' ' + heroClass + " / " + kit + " / " + scenario.name;
        }
    }
}
