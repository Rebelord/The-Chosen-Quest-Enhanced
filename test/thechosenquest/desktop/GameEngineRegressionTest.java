package thechosenquest.desktop;

import java.io.File;
import java.util.List;
import java.util.Random;

public final class GameEngineRegressionTest {
    private static final HeroExpectation[] HEROES = {
        hero("Human", "Fighter", 70, 0, 13, 8, 20),
        hero("Human", "Mage", 55, 30, 11, 1, 20),
        hero("Human", "Rogue", 55, 0, 14, 3, 40),
        hero("Human", "Hunter", 63, 0, 15, 2, 20),
        hero("Dwarf", "Fighter", 80, 0, 12, 10, 20),
        hero("Dwarf", "Mage", 65, 30, 10, 3, 20),
        hero("Dwarf", "Rogue", 65, 0, 13, 5, 40),
        hero("Dwarf", "Hunter", 73, 0, 14, 4, 20),
        hero("Elf", "Fighter", 65, 8, 13, 8, 20),
        hero("Elf", "Mage", 50, 38, 11, 1, 20),
        hero("Elf", "Rogue", 50, 8, 14, 3, 40),
        hero("Elf", "Hunter", 58, 8, 15, 2, 20),
        hero("Halfling", "Fighter", 63, 0, 12, 9, 30),
        hero("Halfling", "Mage", 48, 30, 10, 2, 30),
        hero("Halfling", "Rogue", 48, 0, 13, 4, 50),
        hero("Halfling", "Hunter", 56, 0, 14, 3, 30)
    };

    public static void main(String[] args) throws Exception {
        testFantasyNameGenerator();
        testEveryHeroCombination();
        testStarterLoadoutsAndSelling();
        testWeaponTraits();
        testProceduralWorldGeneration();
        testFiniteSpiderNest();
        testMapDiscoveryAndRumors();
        testDeterministicCombat();
        testClassCombatActions();
        testProgressiveAbilities();
        testMovementKeyRepeatGuard();
        testSpeedInitiativeAndActionTempo();
        testPersistentCombatStatuses();
        testLeveling();
        testFleeingAndDefeat();
        testDragonVictory();
        testEquipmentRules();
        testVendorInventoryRules();
        testCombatLootDiscovery();
        testRelicProgression();
        testShopProgressionForEveryClass();
        testClassRestrictions();
        testSpellIdentityAndCombatCurve();
        testSaveRoundTripAndNormalization();
        System.out.println("Game engine regression tests passed (16 hero builds, combat, progression, " +
            "fleeing, defeat, victory, equipment, and saves).");
    }

    private static void testFantasyNameGenerator() {
        java.util.HashSet<String> generated = new java.util.HashSet<String>();
        for (String race : GameEngine.RACES) {
            for (String heroClass : GameEngine.CLASSES) {
                String name = FantasyNameGenerator.generate(race, heroClass,
                    new Random(31L + generated.size()));
                require(name.matches("[A-Za-z]+ [A-Za-z]+"),
                    race + " " + heroClass + " generated name is readable");
                generated.add(name);
            }
        }
        require(generated.size() >= 12, "hero builds produce varied offline names");
        String fighter = FantasyNameGenerator.generate("Elf", "Fighter", new Random(9L));
        String mage = FantasyNameGenerator.generate("Elf", "Mage", new Random(9L));
        require(fighter.split(" ")[0].equals(mage.split(" ")[0]),
            "race consistently controls the first-name style");
        require(!fighter.split(" ")[1].equals(mage.split(" ")[1]),
            "class quietly changes the surname style");
    }

    private static void testEveryHeroCombination() {
        GameEngine engine = new GameEngine();
        for (HeroExpectation expected : HEROES) {
            engine.newGame("Hero", expected.race, expected.heroClass);
            GameEngine.State state = engine.getState();
            String label = expected.race + " " + expected.heroClass;
            require(state.maxHealth == expected.health, label + " health");
            require(state.health == expected.health, label + " starts healed");
            require(state.maxMana == expected.mana, label + " mana");
            require(state.mana == expected.mana, label + " starts with full mana");
            require(engine.getAttack() == expected.attack, label + " attack");
            require(engine.getDefense() == expected.defense, label + " defense");
            require(state.gold == expected.gold, label + " gold");
            int expectedItems = "Fighter".equals(expected.heroClass) ? 3 : 2;
            require(state.inventory.size() == expectedItems, label + " starting equipment");
            require(state.equippedWeapon != null && state.equippedArmour != null,
                label + " equipped loadout");
            require(state.spells.isEmpty() == !"Mage".equals(expected.heroClass),
                label + " spell access");

            engine.configureHeroPreview(expected.race, expected.heroClass);
            GameEngine.State preview = engine.getState();
            require(preview.maxHealth == expected.health &&
                    preview.maxMana == expected.mana &&
                    engine.getAttack() == expected.attack &&
                    engine.getDefense() == expected.defense &&
                    preview.gold == expected.gold && preview.inventory.size() == expectedItems,
                label + " lightweight character-creation preview");
        }
        GameEngine masculine = new GameEngine();
        masculine.newGame("Art A", "Human", "Mage",
            GameEngine.defaultStarterKit("Mage"), CharacterArt.MALE);
        GameEngine feminine = new GameEngine();
        feminine.newGame("Art B", "Human", "Mage",
            GameEngine.defaultStarterKit("Mage"), CharacterArt.FEMALE);
        require(masculine.getState().maxHealth == feminine.getState().maxHealth &&
                masculine.getState().maxMana == feminine.getState().maxMana &&
                masculine.getAttack() == feminine.getAttack() &&
                masculine.getDefense() == feminine.getDefense(),
            "cosmetic gender choice does not alter gameplay statistics");
    }

    private static void testWeaponTraits() {
        require("CONCUSSIVE".equals(GameEngine.weaponTraitName(
            new GameEngine.Item("Warhammer", "Weapon", 9, 0, 24))),
            "hammers expose their stun trait");
        require("ARMOR PIERCING".equals(GameEngine.weaponTraitName(
            new GameEngine.Item("Crossbow", "Weapon", 9, 0, 35))),
            "crossbows expose armor penetration");
        require("BLEEDING EDGE".equals(GameEngine.weaponTraitName(
            new GameEngine.Item("Shadowsteel Dirk", "Weapon", 8, 0, 28))),
            "daggers expose wound damage");
        require("ARCANE FOCUS".equals(GameEngine.weaponTraitName(
            new GameEngine.Item("Runed Wand", "Weapon", 6, 0, 22))),
            "mage weapons expose spell amplification");
        require(GameEngine.weaponTraitDescription(
            new GameEngine.Item("Long Sword", "Weapon", 7, 0, 15)).contains("defensive"),
            "inventory explains sword guard behavior");
        require(GameEngine.enemyBattleCry("Orc Warlord").contains("crows"),
            "elite enemies have authored battle cries");
    }

    private static void testStarterLoadoutsAndSelling() {
        for (String heroClass : GameEngine.CLASSES) {
            for (String path : GameEngine.starterKitsFor(heroClass)) {
                require(!GameEngine.combatPathFantasy(path).isEmpty() &&
                        !GameEngine.combatPathResourceLoop(heroClass, path).isEmpty() &&
                        GameEngine.combatPathTrack(path).contains("→") &&
                        !GameEngine.combatPathTradeoff(path).isEmpty(),
                    "every Combat Path exposes fantasy, resource, progression, and tradeoff copy");
            }
        }
        GameEngine engine = new GameEngine();
        engine.newGame("Berserker", "Human", "Fighter", "BERSERKER");
        GameEngine.State state = engine.getState();
        require("Greatsword".equals(state.equippedWeapon) &&
            "Leather Armour".equals(state.equippedArmour),
            "fighter alternate starter loadout is applied");
        require("SLOW".equals(GameEngine.attackTempoLabel(state)),
            "heavy starter weapon communicates its slower tempo");
        require(state.inventory.get(0).starterItem &&
            "COMMON".equals(GameEngine.itemQuality(state.inventory.get(0))),
            "starter equipment is protected common gear");
        String breakerPath = GameEngine.abilityProgressionSummary(state);
        require(breakerPath.contains("L2 Heavy Strike") &&
                breakerPath.contains("L3 Armor Breaker") &&
                breakerPath.contains("Second Wind"),
            "starter loadout previews its loadout-aware ability path");
        moveToShop(engine, true);
        int startingGold = state.gold;
        engine.sellItem(state.inventory.get(0));
        require(state.gold == startingGold && state.inventory.size() == 2,
            "starter gear cannot be sold");

        GameEngine.Item dagger = engine.shopItems().get(0);
        state.gold = 20;
        engine.buyItem(dagger);
        int afterPurchase = state.gold;
        int expectedSale = GameEngine.salePrice(dagger, true);
        engine.sellItem(dagger);
        require(state.gold == afterPurchase + expectedSale && !state.inventory.contains(dagger),
            "purchased gear can be sold using vendor pricing");

        engine.newGame("Arcanist", "Elf", "Mage", "ARCANIST");
        require("Apprentice Wand".equals(engine.getState().equippedWeapon) &&
            "Apprentice Tome".equals(engine.getState().equippedOffhand),
            "mage alternate starter loadout is applied");
        engine.newGame("Skirmisher", "Halfling", "Rogue", "SKIRMISHER");
        require("FAST".equals(GameEngine.attackTempoLabel(engine.getState())) &&
            "Offhand Dagger".equals(engine.getState().equippedOffhand),
            "quick-knives starter loadout attacks quickly");

        engine.newGame("Shield", "Human", "Fighter");
        state = engine.getState();
        GameEngine.Item shield = inventoryItem(state, "Iron Shield");
        int defense = engine.getDefense();
        require("Iron Shield".equals(state.equippedOffhand) &&
            engine.getDefense() == defense,
            "fighter shields occupy the offhand and improve defense");
        GameEngine.Item greatsword = new GameEngine.Item("Greatsword", "Weapon", 9, 0, 15,
            "Fighter", false);
        state.inventory.add(greatsword);
        engine.equipItem(greatsword);
        require(state.equippedOffhand == null &&
                "HEAVY ASSAULT".equals(GameEngine.currentCombatStyle(state)) &&
                engine.consumeCombatStyleNotice().contains("Heavy Strike"),
            "equipping a two-handed weapon clears the offhand slot");
        GameEngine.Item longSword = inventoryItem(state, "Long Sword");
        engine.equipItem(longSword);
        engine.consumeCombatStyleNotice();
        engine.equipItem(shield);
        String restoredNotice = engine.consumeCombatStyleNotice();
        require("SHIELD GUARD".equals(GameEngine.currentCombatStyle(state)) &&
                "KNIGHT".equals(GameEngine.originPath(state)) &&
                restoredNotice != null && restoredNotice.contains("Shield Counter"),
            "gear can reverse Current Style without rewriting Origin Path");
        moveToEnemy(engine, 0);
        int rageBeforeDefend = state.rage;
        engine.defend();
        require(state.rage == rageBeforeDefend &&
                engine.getHistory().contains("no defensive setup action"),
            "fighters cannot passively build Rage through a defense action");
        engine.newGame("Legacy", "Human", "Fighter", "VANGUARD");
        require("KNIGHT".equals(GameEngine.originPath(engine.getState())),
            "legacy Fighter path labels migrate to the canonical origin");
    }

    private static void testProceduralWorldGeneration() {
        GameEngine first = new GameEngine();
        first.setRandomSeed(101L);
        first.newGame("Seed One", "Human", "Fighter");
        GameEngine second = new GameEngine();
        second.setRandomSeed(202L);
        second.newGame("Seed Two", "Human", "Fighter");
        require(!worldSignature(first.getState()).equals(worldSignature(second.getState())),
            "different seeds create different worlds");
        validateGeneratedWorld(first.getState());
        validateGeneratedWorld(second.getState());
        java.util.HashSet<String> dragons = new java.util.HashSet<String>();
        for (long seed = 0; seed < 64; seed++) {
            GameEngine generated = new GameEngine();
            generated.setRandomSeed(seed);
            generated.newGame("Generated", "Human", "Fighter");
            validateGeneratedWorld(generated.getState());
            moveToEnemy(generated, 2);
            dragons.add(generated.currentEnemy().name);
        }
        require(dragons.size() == 4, "all four dragon variants can be selected");
    }

    private static void testMapDiscoveryAndRumors() {
        GameEngine explorer = new GameEngine();
        explorer.setRandomSeed(77L);
        explorer.newGame("Cartographer", "Human", "Fighter");
        require(explorer.discoveryAt(0, 0) == GameEngine.DiscoveryState.VISITED,
            "starting tile is visited");
        require(explorer.discoveryAt(0, 1) == GameEngine.DiscoveryState.SCOUTED,
            "travel scouts an adjacent tile");
        require(explorer.discoveryAt(GameEngine.SIZE - 1, GameEngine.SIZE - 1) ==
                GameEngine.DiscoveryState.UNKNOWN,
            "distant terrain begins under fog");

        moveToVendor(explorer, "General Merchant");
        explorer.getState().gold = 20;
        explorer.useMapService(GameEngine.MapService.REGIONAL_MAP);
        require(explorer.getState().regionalMapOwned && explorer.getState().gold == 8,
            "regional map is purchased once at the merchant");
        require(explorer.latestMapClue() != null &&
                explorer.latestMapClue().contains("threats remain unknown"),
            "regional map records its discovery limit in the map journal");
        for (int row = 0; row < GameEngine.SIZE; row++) {
            for (int col = 0; col < GameEngine.SIZE; col++) {
                require(explorer.discoveryAt(row, col) != GameEngine.DiscoveryState.UNKNOWN,
                    "regional map scouts the full board");
            }
        }

        GameEngine rumor = new GameEngine();
        rumor.setRandomSeed(91L);
        rumor.newGame("Listener", "Elf", "Hunter");
        moveToVendor(rumor, "Innkeeper");
        rumor.useMapService(GameEngine.MapService.RUMOR);
        require(!rumor.rumorServiceAvailableHere(),
            "a tavern rumor can only be requested once per game");
        boolean markedThreat = false;
        boolean markedRelicRegion = false;
        for (int row = 0; row < GameEngine.SIZE; row++) {
            for (int col = 0; col < GameEngine.SIZE; col++) {
                markedThreat |= rumor.threatKnowledgeAt(row, col) == 1;
                markedRelicRegion |= rumor.hasRelicClueAt(row, col);
            }
        }
        require(markedThreat, "a rumor marks an unknown threat");
        require(markedRelicRegion, "an elite rumor marks a relic search region");
        require(!rumor.mapJournal().isEmpty() && rumor.latestMapClue().contains("RELIC RUMOR"),
            "rumor direction and relic context persist in the map journal");
        String relicClue = null;
        for (int row = 0; row < GameEngine.SIZE && relicClue == null; row++) {
            for (int col = 0; col < GameEngine.SIZE && relicClue == null; col++) {
                if (rumor.hasRelicClueAt(row, col)) relicClue = rumor.relicClueAt(row, col);
            }
        }
        require(relicClue != null && relicClue.contains("may"),
            "relic regions explain which specialist may understand the eventual find");
        int knownAfterFirstRumor = knownThreatCount(rumor);
        rumor.useMapService(GameEngine.MapService.RUMOR);
        require(knownThreatCount(rumor) == knownAfterFirstRumor,
            "repeated tavern requests do not reveal another threat");
        require(rumor.getHistory().contains("already heard the useful rumors"),
            "repeated tavern requests explain the one-use rule");

        moveToVendor(rumor, "Alchemist");
        require(rumor.rumorServiceAvailableHere(),
            "the alchemist consultation remains independent from the tavern");
        rumor.useMapService(GameEngine.MapService.RUMOR);
        require(!rumor.rumorServiceAvailableHere(),
            "arcane advice can only be requested once per game");
    }

    private static void testFiniteSpiderNest() throws Exception {
        GameEngine engine = new GameEngine();
        engine.setRandomSeed(515L);
        engine.newGame("Nest Warden", "Human", "Fighter");
        GameEngine.State state = engine.getState();
        require(state.spiderNestRow >= 0 && state.spiderNestCol >= 0 &&
                state.tiles[state.spiderNestRow][state.spiderNestCol] ==
                    GameEngine.TileType.SPIDER_NEST,
            "generated world contains one authored spider source");
        state.row = state.spiderNestRow;
        state.col = state.spiderNestCol;
        int inventoryBefore = state.inventory.size();
        int goldBefore = state.gold;
        for (int remaining = 3; remaining > 0; remaining--) {
            GameEngine.Enemy brood = engine.currentEnemy();
            require(brood != null && "SPIDER_NEST".equals(brood.sourceId) &&
                    brood.reducedRewards &&
                    (remaining == 1
                        ? "Ashweb Matriarch".equals(brood.name) && brood.tier == 1 &&
                            brood.reward == 6 && brood.combatLevel == 2
                        : "Giant Forest Spider".equals(brood.name) && brood.tier == 0 &&
                            brood.reward == 3),
                "nest brood is tagged for reduced source rewards");
            brood.health = 1;
            engine.setRandomSeed(remaining);
            engine.attack();
            require(engine.spiderNestRemaining() == remaining - 1,
                "each defeated brood consumes exactly one finite source charge");
            if (remaining == 3) {
                File save = File.createTempFile("chosen-quest-nest-", ".save");
                try {
                    engine.save(save);
                    engine.load(save);
                    state = engine.getState();
                    require(engine.spiderNestRemaining() == 2 &&
                            state.row == state.spiderNestRow && state.col == state.spiderNestCol,
                        "partially cleared source state survives save and load");
                } finally {
                    save.delete();
                }
            }
            if (remaining > 1) {
                require(engine.currentEnemy() == null,
                    "nest waits for an explicit search before the next brood");
                engine.locationAction();
            }
        }
        require(engine.spiderNestCleared() && engine.currentEnemy() == null,
            "third brood permanently clears the spider nest");
        require(state.inventory.size() == inventoryBefore,
            "source enemies cannot farm standard equipment drops");
        require(state.gold == goldBefore + 30,
            "two brood rewards, one Matriarch reward, and clearing gold are bounded");
        int clearedGold = state.gold;
        engine.locationAction();
        require(engine.currentEnemy() == null && state.gold == clearedGold,
            "cleared source cannot respawn enemies or repeat its reward");
    }

    private static int knownThreatCount(GameEngine engine) {
        int count = 0;
        for (int row = 0; row < GameEngine.SIZE; row++) {
            for (int col = 0; col < GameEngine.SIZE; col++) {
                if (engine.threatKnowledgeAt(row, col) > 0) count++;
            }
        }
        return count;
    }

    private static void validateGeneratedWorld(GameEngine.State state) {
        int shops = 0;
        int taverns = 0;
        int camps = 0;
        int blacksmiths = 0;
        int standards = 0;
        int elites = 0;
        int bosses = 0;
        int nests = 0;
        int sourceSpiders = 0;
        for (int row = 0; row < state.tiles.length; row++) {
            for (int col = 0; col < state.tiles[row].length; col++) {
                if (state.tiles[row][col] == GameEngine.TileType.SHOP) shops++;
                if (state.tiles[row][col] == GameEngine.TileType.TAVERN) taverns++;
                if (state.tiles[row][col] == GameEngine.TileType.ENCAMPMENT) camps++;
                if (state.tiles[row][col] == GameEngine.TileType.SPIDER_NEST) nests++;
                if (state.blacksmithShops[row][col]) blacksmiths++;
                GameEngine.Enemy enemy = state.enemies[row][col];
                if (enemy != null) {
                    require(state.tiles[row][col] != GameEngine.TileType.SHOP &&
                        state.tiles[row][col] != GameEngine.TileType.TAVERN &&
                        state.tiles[row][col] != GameEngine.TileType.ENCAMPMENT,
                        "enemies never occupy generated safe landmarks");
                    require(GameEngine.terrainSupports(enemy.name, state.tiles[row][col]),
                        "generated enemies occupy a compatible terrain biome");
                    if (enemy.sourceId != null) sourceSpiders++;
                    else if (enemy.tier == 0) standards++;
                    else if (enemy.tier == 1) elites++;
                    else if (enemy.tier == 2) bosses++;
                }
            }
        }
        require(state.tiles[0][0] == GameEngine.TileType.ENCAMPMENT,
            "starting tile remains a guaranteed safe camp");
        require(state.tiles.length == 13 && state.tiles[0].length == 13,
            "generated world uses the expanded 13x13 logical grid");
        require(state.dragonLairRow + state.dragonLairCol >= 18,
            "dragon lair remains a distant objective in the larger world");
        require(shops == 2 && taverns == 1 && camps == 2 && blacksmiths == 1,
            "generated world contains the complete landmark set");
        require(standards == 5 && elites == 3 && bosses == 1,
            "generated world contains tiered encounters");
        require(nests == 1 && sourceSpiders == 1 &&
                state.spiderNestRemaining == 3 && !state.spiderNestCleared,
            "generated world contains one finite three-brood spider source");
    }

    private static String worldSignature(GameEngine.State state) {
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < state.tiles.length; row++) {
            for (int col = 0; col < state.tiles[row].length; col++) {
                result.append(state.tiles[row][col].symbol);
                GameEngine.Enemy enemy = state.enemies[row][col];
                if (enemy != null) result.append(enemy.name);
                result.append('|');
            }
        }
        return result.toString();
    }

    private static void testDeterministicCombat() {
        GameEngine engine = new GameEngine();
        engine.newGame("Fighter", "Human", "Fighter");
        moveToEnemy(engine, 0);
        int attack = engine.getAttack();
        int enemyHealth = engine.currentEnemy().health;
        long seed = 8675309L;
        int rawDamage = attack - 3 + new Random(seed).nextInt(5);
        int expectedDamage = Math.max(1, rawDamage - engine.currentEnemy().defense);
        engine.setRandomSeed(seed);
        engine.attack();
        require(engine.currentEnemy().health == enemyHealth - expectedDamage,
            "seeded attack damage");
    }

    private static void testClassCombatActions() {
        GameEngine mage = new GameEngine();
        mage.setRandomSeed(44L);
        mage.newGame("Channeler", "Dwarf", "Mage");
        moveToEnemy(mage, 0);
        mage.getState().mana -= 10;
        int manaBefore = mage.getState().mana;
        mage.setRandomSeed(3L);
        mage.defend();
        require(mage.getState().mana > manaBefore,
            "mage channel ward restores mana in combat");
        require(mage.getHistory().contains("channel a ward"),
            "mage ward is explained in the combat log");

        GameEngine rogue = new GameEngine();
        rogue.setRandomSeed(45L);
        rogue.newGame("Shadow", "Halfling", "Rogue");
        moveToEnemy(rogue, 0);
        rogue.setRandomSeed(1L);
        rogue.defend();
        require("STEALTH".equals(rogue.getState().combatPreparation),
            "rogue defend enters stealth");
        rogue.setRandomSeed(9L);
        rogue.attack();
        require(rogue.getState().combatPreparation == null &&
                rogue.getHistory().contains("Stealth attack! Critical strike!"),
            "rogue stealth guarantees and consumes a prepared critical");

        GameEngine hunter = new GameEngine();
        hunter.setRandomSeed(46L);
        hunter.newGame("Archer", "Elf", "Hunter");
        moveToEnemy(hunter, 0);
        hunter.setRandomSeed(2L);
        hunter.defend();
        require("AIM".equals(hunter.getState().combatPreparation),
            "hunter defend prepares an aimed shot");
        hunter.setRandomSeed(11L);
        hunter.attack();
        require(hunter.getState().combatPreparation == null &&
                hunter.getHistory().contains("Precision shot!"),
            "hunter aim guarantees and consumes a precision shot");

        GameEngine fighter = new GameEngine();
        fighter.setRandomSeed(47L);
        fighter.newGame("Guardian", "Human", "Fighter");
        moveToEnemy(fighter, 0);
        fighter.setRandomSeed(4L);
        int fighterRage = fighter.getState().rage;
        fighter.defend();
        require(fighter.getState().combatPreparation == null &&
                fighter.getState().rage == fighterRage &&
                fighter.getHistory().contains("no defensive setup action"),
            "fighter has no passive defensive setup action");
    }

    private static void testProgressiveAbilities() {
        GameEngine fighter = new GameEngine();
        fighter.newGame("Learner", "Human", "Fighter", "BERSERKER");
        GameEngine.State fighterState = fighter.getState();
        require(!GameEngine.abilityUnlocked(fighterState, 1) &&
                "UNLOCKS LEVEL 2".equals(GameEngine.abilityRequirement(fighterState, 1)),
            "fighter combat begins with only core actions");
        fighterState.level = 2;
        require(GameEngine.abilityUnlocked(fighterState, 1) &&
                "Heavy Strike".equals(GameEngine.abilityName(fighterState, 1)),
            "level two fighter unlock follows the equipped two-handed style");
        GameEngine.Relic earlyRelic =
            new GameEngine.Relic("Early Test Relic", "Warlord", "Blacksmith");
        earlyRelic.identified = true;
        earlyRelic.rewardName = "Test Greatsword";
        fighterState.relics.add(earlyRelic);
        fighterState.weaponProficiency.put("HEAVY BLADE", Integer.valueOf(3));
        require(!GameEngine.abilityUnlocked(fighterState, 2) &&
                !GameEngine.abilityUnlocked(fighterState, 3),
            "identified relics and weapon mastery must not bypass level-three unlocks");
        moveToEnemy(fighter, 0);
        fighter.currentEnemy().health = 100;
        require(!GameEngine.abilityAvailable(fighterState, 1) &&
                GameEngine.abilityRequirement(fighterState, 1).contains("NEEDS 30 RAGE"),
            "fighter tactics require earned Rage");
        fighterState.rage = GameEngine.rageCost(1);
        int beforeCleave = fighter.currentEnemy().health;
        fighter.useAbility(1);
        require(fighter.currentEnemy().health < beforeCleave &&
                fighter.getHistory().contains("spend 30 Rage") &&
                fighter.getHistory().contains("You use Heavy Strike"),
            "fighter tactical ability spends Rage and resolves through combat rules");
        fighterState.level = 3;
        fighterState.weaponProficiency.put("HEAVY BLADE", Integer.valueOf(3));
        require(GameEngine.abilityUnlocked(fighterState, 2) &&
                "Armor Breaker".equals(GameEngine.abilityName(fighterState, 2)) &&
                "Second Wind".equals(GameEngine.abilityName(fighterState, 3)) &&
                GameEngine.abilityPassive(fighterState, 3),
            "level three fighter exposes mastery and signature abilities");
        fighterState.health = fighterState.maxHealth / 2;
        int healthBeforePassiveAttempt = fighterState.health;
        fighter.useAbility(3);
        require(fighterState.health == healthBeforePassiveAttempt &&
                !fighterState.secondWindUsed &&
                fighter.getHistory().contains("triggers automatically"),
            "Second Wind cannot be activated as a healing action");
        fighterState.health = 1;
        fighter.currentEnemy().health = 1000;
        for (int turn = 0; turn < 4 && !fighterState.secondWindUsed; turn++) {
            fighter.attack();
        }
        require(fighterState.secondWindUsed && fighterState.health == 1 &&
                fighterState.rage == fighterState.maxRage &&
                "RAGE".equals(fighterState.combatPreparation) &&
                GameEngine.abilityRequirement(fighterState, 3).contains("SPENT"),
            "a fatal hit triggers one last stand and returns control with Rage ready");
        fighter.attack();
        require(fighter.getHistory().contains("Rage empowers your heavy strike"),
            "Second Wind adrenaline empowers the fighter's next basic attack");

        GameEngine rogue = new GameEngine();
        rogue.newGame("Learner", "Halfling", "Rogue", "SKIRMISHER");
        rogue.getState().level = 2;
        require("Twin Strike".equals(GameEngine.abilityName(rogue.getState(), 1)),
            "rogue level two unlock uses the dual-wield identity");
        moveToEnemy(rogue, 0);
        require(!GameEngine.abilityAvailable(rogue.getState(), 1),
            "rogue advanced actions begin gated by Momentum");
        int momentumBefore = rogue.getState().momentum;
        rogue.setRandomSeed(9L);
        rogue.attack();
        require(rogue.getState().momentum > momentumBefore,
            "rogue basic attacks build Momentum");
        rogue.getState().momentum = GameEngine.momentumCost(1);
        rogue.useAbility(1);
        require(rogue.getHistory().contains("spend 30 Momentum"),
            "rogue advanced actions consume Momentum");

        GameEngine hunter = new GameEngine();
        hunter.newGame("Learner", "Elf", "Hunter");
        hunter.getState().level = 3;
        hunter.getState().maxFocus = GameEngine.maxFocusForLevel(3);
        hunter.getState().focus = hunter.getState().maxFocus;
        hunter.getState().weaponProficiency.put("BOW", Integer.valueOf(3));
        require("Pinning Shot".equals(GameEngine.abilityName(hunter.getState(), 1)) &&
                "Hunter's Mark".equals(GameEngine.abilityName(hunter.getState(), 3)),
            "hunter progression adds control and setup actions");
        moveToEnemy(hunter, 0);
        hunter.currentEnemy().health = 100;
        hunter.useAbility(3);
        require(hunter.getState().enemyMarked &&
                hunter.getState().focus == hunter.getState().maxFocus -
                    GameEngine.hunterFocusCost(hunter.getState(), 3),
            "Hunter's Mark spends Focus and establishes a persistent target requirement");
        hunter.getState().focus = GameEngine.hunterFocusCost(hunter.getState(), 2);
        require("Volley".equals(GameEngine.abilityName(hunter.getState(), 2)) &&
                GameEngine.abilityAvailable(hunter.getState(), 2),
            "Volley becomes available only with sufficient Focus and a marked target");
        hunter.useAbility(2);
        require(!hunter.getState().enemyMarked &&
                hunter.getHistory().contains("You use Volley"),
            "Volley consumes Hunter's Mark when it resolves");
        require(GameEngine.isMeleeActionName("Claw") &&
                GameEngine.isMeleeActionName("Heavy Strike") &&
                !GameEngine.isMeleeActionName("Breath Attack") &&
                !GameEngine.isMeleeActionName("Dark Ritual"),
            "Pinning Shot distinguishes melee intent from breath and magical attacks");
    }

    private static void testMovementKeyRepeatGuard() {
        MainWindow.KeyRepeatGuard guard = new MainWindow.KeyRepeatGuard();
        require(guard.press("moveNorth") && !guard.press("moveNorth"),
            "holding a movement key must produce only one accepted press");
        guard.release("moveNorth");
        require(guard.press("moveNorth"),
            "releasing a movement key must allow the next deliberate step");
        guard.clear();
        require(guard.press("moveNorth"),
            "losing window focus must clear held movement keys");
    }

    private static void testSpeedInitiativeAndActionTempo() {
        GameEngine rogue = new GameEngine();
        rogue.newGame("Swift", "Halfling", "Rogue");
        moveToEnemy(rogue, 0);
        GameEngine.State rogueState = rogue.getState();
        rogueState.enemies[rogueState.row][rogueState.col] =
            new GameEngine.Enemy("Cave Troll", 200, 8, 10, 0);
        int rogueHealth = rogueState.health;
        rogue.setRandomSeed(12L);
        rogue.defend();
        require(rogueState.health == rogueHealth,
            "a fast preparation can preserve a bonus player action");
        rogue.attack();
        require(rogueState.health < rogueHealth &&
                occurrences(rogue.getHistory(), "hits you") == 1,
            "two-action safeguard gives a slow enemy a response");

        GameEngine mage = new GameEngine();
        mage.newGame("Committed", "Elf", "Mage");
        mage.getState().level = 2;
        mage.getState().spells.add("Fireball");
        moveToEnemy(mage, 0);
        GameEngine.State mageState = mage.getState();
        mageState.enemies[mageState.row][mageState.col] =
            new GameEngine.Enemy("Dire Wolf", 200, 5, 10, 0);
        mage.setRandomSeed(21L);
        mage.castSpell("Fireball");
        require(occurrences(mage.getHistory(), "hits you") == 2,
            "a slow spell can expose the hero to two fast enemy attacks");
        List<GameEngine.EnemyTurnEvent> enemyTurns = mage.consumeEnemyTurnEvents();
        require(enemyTurns.size() == 2,
            "two resolved attacks are exposed as two presentation events");
        require(enemyTurns.get(0).healthBefore > enemyTurns.get(0).healthAfter &&
                enemyTurns.get(0).healthAfter == enemyTurns.get(1).healthBefore &&
                enemyTurns.get(1).historyEnd > enemyTurns.get(0).historyEnd,
            "enemy attack events preserve sequential health and combat-log states");
        require(mage.consumeEnemyTurnEvents().isEmpty(),
            "enemy attack presentation events are consumed only once");

        require("FAST".equals(GameEngine.attackTempoLabel(rogueState)),
            "short swords are fast attacks");
        require("NORMAL".equals(GameEngine.attackTempoLabel(mageState)),
            "staff attacks use normal recovery");
        require("SLOW".equals(GameEngine.spellTempoLabel("Fireball")) &&
                "NORMAL".equals(GameEngine.spellTempoLabel("Magic Missile")),
            "spell recovery communicates committed and normal casts");

        int lowerLevelSpeed = mage.getHeroSpeed();
        mageState.level += 5;
        mageState.combatTimelineRow = -1;
        int higherLevelSpeed = mage.getHeroSpeed();
        require(higherLevelSpeed > lowerLevelSpeed &&
                higherLevelSpeed - lowerLevelSpeed <= 10,
            "level advantage improves speed within the capped modifier");
        require(mage.getEnemySpeed() >= mage.currentEnemy().speed,
            "enemy tier modifiers never reduce base speed");
    }

    private static void testPersistentCombatStatuses() {
        GameEngine engine = new GameEngine();
        engine.newGame("Status", "Human", "Mage");
        moveToEnemy(engine, 0);
        GameEngine.State state = engine.getState();
        GameEngine.Enemy enemy = engine.currentEnemy();
        enemy.health = 100;
        // Establish the encounter clock before injecting deterministic effects;
        // first-time timeline setup intentionally clears stale combat counters.
        engine.defend();
        engine.consumeEnemyTurnEvents();
        state.enemyBleedDamage = 3;
        state.enemyBleedTurns = 2;
        state.enemyArmorBreakValue = 2;
        state.enemyArmorBreakTurns = 2;
        state.enemyStunTurns = 1;
        state.consecutiveHeroActions = 2;
        int enemyHealth = enemy.health;
        engine.defend();
        require(enemy.health == enemyHealth - 3 && state.enemyBleedTurns == 1 &&
                state.enemyArmorBreakTurns == 1,
            "bleed and armor break persist and age on perceptible enemy turns");
        List<GameEngine.EnemyTurnEvent> turns = engine.consumeEnemyTurnEvents();
        require(turns.size() == 1 && turns.get(0).missed && turns.get(0).damage == 0 &&
                turns.get(0).actionName.startsWith("Stunned"),
            "a stunned enemy turn remains a visible presentation beat");
        List<String> statuses = GameEngine.activeCombatStatuses(state);
        require(statuses.toString().contains("BLEED 3") &&
                statuses.toString().contains("ARMOR −2"),
            "active combat effects expose source, magnitude, and remaining duration");
    }

    private static void testLeveling() {
        GameEngine engine = new GameEngine();
        engine.newGame("Almost Ready", "Human", "Fighter");
        GameEngine.State state = engine.getState();
        state.experience = 29;
        state.health = 1;
        moveToEnemy(engine, 0);
        int expectedOverflow = engine.currentEnemy().maxHealth / 2 - 1;
        engine.currentEnemy().health = 1;
        int oldMaxHealth = state.maxHealth;
        engine.setRandomSeed(1L);
        engine.attack();
        require(state.level == 2, "level gained after crossing threshold");
        require(state.experience == expectedOverflow, "overflow experience retained");
        require(state.maxHealth == oldMaxHealth + 8, "level health increase");
        require(state.health == state.maxHealth, "level restores health");
        require(GameEngine.abilityUnlocked(state, 1),
            "level two reveals the first tactical class ability");
        require(GameEngine.equippedWeaponProficiency(state) == 2,
            "meaningful level-up advances the equipped weapon proficiency");
        GameEngine.ProgressionNotice fighterNotice = engine.consumeProgressionNotice();
        require(fighterNotice != null && fighterNotice.level == 2 &&
                fighterNotice.abilitySummary.length() > 0,
            "level-up creates a dedicated progression presentation event");

        GameEngine mage = new GameEngine();
        mage.newGame("Scholar", "Elf", "Mage");
        GameEngine.State mageState = mage.getState();
        require("UNLOCKS LEVEL 2".equals(GameEngine.abilityRequirement(mageState, 2)) &&
                "UNLOCKS LEVEL 3".equals(GameEngine.abilityRequirement(mageState, 3)),
            "mage spell lock labels match their actual unlock levels");
        require(mageState.spells.size() == 1 && mageState.spells.contains("Magic Missile"),
            "mage starts with only Magic Missile");
        mageState.experience = 29;
        moveToEnemy(mage, 0);
        mage.currentEnemy().health = 1;
        mage.castSpell("Magic Missile");
        require(mageState.level == 2 && mageState.spells.contains("Fireball") &&
                !mageState.spells.contains("Ice Spike"),
            "level two unlocks Fireball without revealing the final spell");
        require(mage.consumeProgressionNotice() != null,
            "mage level two unlock produces progression feedback");
        mageState.experience = 59;
        moveToEnemy(mage, 0);
        mage.currentEnemy().health = 1;
        mage.castSpell("Magic Missile");
        require(mageState.level == 3 && mageState.spells.contains("Ice Spike") &&
                GameEngine.equippedWeaponProficiency(mageState) == 3,
            "level three unlocks Ice Spike and weapon mastery");
    }

    private static void testFleeingAndDefeat() {
        GameEngine engine = new GameEngine();
        engine.newGame("Runner", "Human", "Fighter");
        GameEngine.State state = engine.getState();
        moveToEnemy(engine, 0);
        GameEngine.Enemy wolf = engine.currentEnemy();
        state.previousRow = 0;
        state.previousCol = 0;
        int enemyRow = state.row;
        int enemyCol = state.col;
        engine.flee();
        require(state.row == 0 && state.col == 0, "flee returns to previous safe tile");
        require(state.enemies[enemyRow][enemyCol] == wolf, "flee leaves enemy alive");

        state.row = enemyRow;
        state.col = enemyCol;
        state.health = 1;
        wolf.health = 999;
        wolf.speed = 200;
        wolf.combatLevel = 1;
        engine.setRandomSeed(2L);
        engine.attack();
        require(state.health == 0, "enemy can defeat player");
        engine.move(0, -1);
        require(state.row == enemyRow && state.col == enemyCol, "defeated player cannot move");
    }

    private static void testDragonVictory() {
        GameEngine sealed = new GameEngine();
        sealed.newGame("Unprepared", "Human", "Fighter");
        GameEngine.State sealedState = sealed.getState();
        int lairRow = sealedState.dragonLairRow;
        int lairCol = sealedState.dragonLairCol;
        int approachCol = lairCol > 0 ? lairCol - 1 : lairCol + 1;
        sealedState.row = lairRow;
        sealedState.col = approachCol;
        sealedState.enemies[lairRow][approachCol] = null;
        sealed.move(0, lairCol - approachCol);
        require(sealedState.col == approachCol &&
                sealed.getHistory().contains("three elite relics (0/3)"),
            "dragon seal blocks an unprepared quest rush with explicit progress");
        sealedState.level = 3;
        for (int index = 0; index < 3; index++) {
            GameEngine.Relic relic = new GameEngine.Relic(
                "Seal Relic " + index, "Elite", "Blacksmith");
            relic.identified = true;
            sealedState.relics.add(relic);
        }
        sealed.move(0, lairCol - approachCol);
        require(sealedState.col == lairCol && sealed.dragonSealReady(),
            "level three and three identified relics unseal the dragon encounter");

        GameEngine engine = new GameEngine();
        engine.newGame("Chosen", "Elf", "Mage");
        GameEngine.State state = engine.getState();
        moveToEnemy(engine, 2);
        engine.currentEnemy().health = 1;
        engine.setRandomSeed(3L);
        engine.castSpell("Magic Missile");
        require(state.won, "dragon defeat wins quest");
        require(engine.currentEnemy() == null, "dragon removed after victory");
        int row = state.row;
        engine.move(-1, 0);
        require(state.row == row, "victorious quest no longer accepts actions");
    }

    private static void testEquipmentRules() {
        GameEngine engine = new GameEngine();
        engine.newGame("Buyer", "Dwarf", "Fighter");
        GameEngine.State state = engine.getState();
        GameEngine.Item plate = engine.shopItems().get(6);
        int inventorySize = state.inventory.size();
        engine.buyItem(plate);
        require(state.inventory.size() == inventorySize, "cannot buy equipment away from shop");

        moveToShop(engine, true);
        state.gold = 100;
        engine.buyItem(plate);
        require(state.inventory.contains(plate), "shop equipment purchase");
        require(state.gold == 100 - plate.cost, "purchase deducts exact cost");
        int inventoryAfterPurchase = state.inventory.size();
        int goldAfterPurchase = state.gold;
        require(!containsShopItem(engine, "Plate Armour"),
            "purchased equipment leaves this shop's finite stock");
        engine.buyItem(plate);
        require(state.inventory.size() == inventoryAfterPurchase &&
                state.gold == goldAfterPurchase,
            "repeat purchase events cannot duplicate finite shop equipment");
        engine.equipItem(plate);
        require("Plate Armour".equals(state.equippedArmour), "purchased armour equips");
        require(engine.getDefense() == state.baseDefense + plate.defense + 2,
            "equipped armour changes defense");
    }

    private static void testClassRestrictions() {
        GameEngine engine = new GameEngine();
        String[][] allowed = {
            {"Fighter", "Axe", "Plate Armour"},
            {"Mage", "Dagger", "Cloth Armour"},
            {"Rogue", "Short Sword", "Leather Armour"},
            {"Hunter", "Crossbow", "Scale Armour"}
        };
        String[][] blocked = {
            {"Fighter", "Long Bow", "Crossbow"},
            {"Mage", "Axe", "Plate Armour"},
            {"Rogue", "Long Sword", "Long Bow", "Scale Armour"},
            {"Hunter", "Axe", "Plate Armour"}
        };
        for (String[] rule : allowed) {
            engine.newGame("Allowed", "Human", rule[0]);
            for (int i = 1; i < rule.length; i++) {
                GameEngine.Item item = item(engine, rule[i]);
                require(GameEngine.equipmentRestriction(rule[0], item) == null,
                    rule[0] + " may equip " + rule[i]);
            }
        }
        for (String[] rule : blocked) {
            engine.newGame("Blocked", "Human", rule[0]);
            for (int i = 1; i < rule.length; i++) {
                GameEngine.Item item = item(engine, rule[i]);
                require(GameEngine.equipmentRestriction(rule[0], item) != null,
                    rule[0] + " is blocked from " + rule[i]);
            }
        }

        engine.newGame("Mage", "Human", "Mage");
        GameEngine.State state = engine.getState();
        moveToShop(engine, true);
        state.gold = 100;
        GameEngine.Item axe = item(engine, "Axe");
        int inventorySize = state.inventory.size();
        engine.buyItem(axe);
        require(state.inventory.size() == inventorySize && state.gold == 100,
            "shop prevents incompatible purchases");
        state.inventory.add(axe);
        String weapon = state.equippedWeapon;
        engine.equipItem(axe);
        require(weapon.equals(state.equippedWeapon),
            "inventory prevents incompatible equipment");
        require(engine.getHistory().contains("Mage cannot equip Axe"),
            "class restriction gives clear feedback");
    }

    private static void testVendorInventoryRules() {
        GameEngine engine = new GameEngine();
        engine.newGame("Vendor Check", "Human", "Fighter");
        GameEngine.State state = engine.getState();
        GameEngine.Item plate = item(engine, "Plate Armour");
        GameEngine.Item cloth = item(engine, "Cloth Armour");

        moveToShop(engine, false);
        state.gold = 100;
        int inventorySize = state.inventory.size();
        engine.buyItem(plate);
        require(state.inventory.size() == inventorySize && state.gold == 100,
            "general merchant does not sell plate armour");

        moveToShop(engine, true);
        engine.buyItem(cloth);
        require(state.inventory.size() == inventorySize && state.gold == 100,
            "blacksmith does not sell cloth armour");
        engine.buyPotion();
        require(state.gold == 100 && state.potions == 2,
            "blacksmith does not sell potions");
        require(GameEngine.vendorOffers(true, plate) &&
                GameEngine.vendorOffers(false, cloth),
            "vendor inventory assigns heavy and light armour correctly");

        GameEngine.Item ironMace = item(engine, "Iron Mace");
        require(GameEngine.vendorOffers(true, ironMace) &&
                !GameEngine.vendorOffers(false, ironMace),
            "entry concussive weapons belong only to the blacksmith");
        require(!containsShopItem(engine, "Flanged Mace") &&
                !containsShopItem(engine, "Warhammer"),
            "advanced blacksmith stock is gated by hero level");
        state.level = 2;
        GameEngine.Item tempered = item(engine, "Tempered Greatsword");
        require(containsShopItem(engine, "Flanged Mace") &&
                GameEngine.isTwoHanded(tempered),
            "level two unlocks uncommon trait-focused blacksmith weapons");
        state.level = 3;
        GameEngine.Item warhammer = item(engine, "Warhammer");
        require(GameEngine.vendorOffers(true, warhammer) &&
                "CONCUSSIVE".equals(GameEngine.weaponTraitName(warhammer)),
            "level three unlocks the rare blacksmith warhammer");
    }

    private static void testRelicProgression() {
        String[][] drops = {
            {"Orc Warlord", "Battered War Crest", "Blacksmith"},
            {"Necromancer", "Sealed Soulglass", "Alchemist"},
            {"Dark Elf Assassin", "Moonmarked Coffer", "General Merchant"},
            {"Fallen Knight", "Tarnished Oath Signet", "Innkeeper"}
        };
        for (String[] drop : drops) {
            GameEngine engine = new GameEngine();
            engine.newGame("Relic Seeker", "Human", "Fighter");
            moveToEnemy(engine, 1);
            GameEngine.State state = engine.getState();
            state.enemies[state.row][state.col] =
                new GameEngine.Enemy(drop[0], 1, 1, 1, 1);
            engine.setRandomSeed(1L);
            engine.attack();
            require(state.relics.size() == 1, drop[0] + " drops one relic");
            GameEngine.Relic relic = state.relics.get(0);
            require(drop[1].equals(relic.name) && drop[2].equals(relic.vendor),
                drop[0] + " maps to its intended relic and vendor");
            require(engine.consumeRelicDiscovery() == relic,
                "new relic is exposed for the discovery presentation");

            moveToVendor(engine, "Blacksmith".equals(relic.vendor)
                ? "General Merchant" : "Blacksmith");
            engine.identifyRelic(relic);
            require(!relic.identified, "wrong vendor cannot identify " + relic.name);

            moveToVendor(engine, relic.vendor);
            int gold = state.gold;
            int equipment = state.inventory.size();
            String equippedWeapon = state.equippedWeapon;
            String equippedArmour = state.equippedArmour;
            engine.identifyRelic(relic);
            require(relic.identified && engine.identifiedRelicCount() == 1,
                "correct vendor identifies " + relic.name);
            require(state.gold == gold, "relic identification is free");
            require(state.inventory.size() == equipment + 1 && relic.rewardName != null,
                "identified relic grants class-compatible equipment");
            GameEngine.Item reward = state.inventory.get(state.inventory.size() - 1);
            require(GameEngine.equipmentRestriction(state.heroClass, reward) == null &&
                    reward.relicReward, "relic reward is usable and marked as special");
            require(equippedWeapon.equals(state.equippedWeapon) &&
                    equippedArmour.equals(state.equippedArmour),
                "identification leaves relic equipment for the player to equip");
            engine.equipItem(reward);
            require(("Weapon".equals(reward.type) && reward.name.equals(state.equippedWeapon)) ||
                    ("Armour".equals(reward.type) && reward.name.equals(state.equippedArmour)),
                "identified relic reward can be equipped explicitly");
        }
    }

    private static void testCombatLootDiscovery() {
        for (int seed = 0; seed < 500; seed++) {
            GameEngine engine = new GameEngine();
            engine.newGame("Field Scavenger", "Human", "Fighter");
            moveToEnemy(engine, 0);
            GameEngine.State state = engine.getState();
            state.enemies[state.row][state.col] =
                new GameEngine.Enemy("Loot Test Bandit", 1, 1, 1, 0);
            int inventoryBefore = state.inventory.size();
            engine.setRandomSeed(seed);
            engine.attack();
            GameEngine.Item loot = engine.consumeLootDiscovery();
            if (loot == null) continue;
            require(state.inventory.size() == inventoryBefore + 1 &&
                    state.inventory.contains(loot),
                "combat loot notice points to the item actually added to inventory");
            require("COMMON".equals(GameEngine.itemQuality(loot)) ||
                    "UNCOMMON".equals(GameEngine.itemQuality(loot)),
                "standard combat loot preserves its visible quality tier");
            require(engine.consumeLootDiscovery() == null,
                "combat loot presentation is consumed exactly once");
            return;
        }
        throw new AssertionError("A deterministic standard combat drop was not found");
    }

    private static void testShopProgressionForEveryClass() {
        String[][] upgrades = {
            {"Fighter", "Steel Long Sword"},
            {"Mage", "Ashwood Staff"},
            {"Rogue", "Shadowsteel Dirk"},
            {"Hunter", "Ranger Bow"}
        };
        GameEngine engine = new GameEngine();
        for (String[] upgrade : upgrades) {
            engine.newGame("Shopper", "Human", upgrade[0]);
            int startingAttack = engine.getAttack();
            GameEngine.Item candidate = item(engine, upgrade[1]);
            require(candidate.attack > 0 &&
                    GameEngine.equipmentRestriction(upgrade[0], candidate) == null,
                upgrade[0] + " has a class-compatible shop weapon upgrade");
            engine.getState().inventory.add(candidate);
            engine.equipItem(candidate);
            require(engine.getAttack() > startingAttack,
                upgrade[0] + " shop weapon is a meaningful minor upgrade");
        }
        engine.newGame("Loot", "Human", "Mage");
        require("UNCOMMON".equals(GameEngine.itemQuality(item(engine, "Runed Wand"))) &&
                "UNCOMMON".equals(GameEngine.itemQuality(item(engine, "Serrated Dagger"))) &&
                "UNCOMMON".equals(GameEngine.itemQuality(item(engine, "Soldier Shield"))),
            "shops identify class-specific minor upgrades as uncommon gear");
        require(!containsShopItem(engine, "Hunting Crossbow") &&
                !containsShopItem(engine, "Carved Totem"),
            "advanced field-loot variants do not enter shops before level two");
        engine.getState().level = 2;
        require(containsShopItem(engine, "Hunting Crossbow") &&
                containsShopItem(engine, "Carved Totem"),
            "level two expands specialized ranged and magical stock");
        GameEngine.Item resale = item(engine, "Runed Wand");
        require(GameEngine.salePrice(resale, false) == resale.cost / 2,
            "uncommon resale provides meaningful progress toward a replacement");
    }

    private static void testSpellIdentityAndCombatCurve() {
        GameEngine fighter = new GameEngine();
        fighter.newGame("Fighter", "Human", "Fighter");
        moveToEnemy(fighter, 0);
        int mana = fighter.getState().mana;
        int health = fighter.currentEnemy().health;
        fighter.castSpell("Magic Missile");
        require(fighter.getState().mana == mana && fighter.currentEnemy().health == health,
            "non-mage cannot invoke spells through the engine");

        GameEngine mage = new GameEngine();
        mage.newGame("Mage", "Human", "Mage");
        mage.getState().level = 3;
        mage.getState().spells.add("Fireball");
        mage.getState().spells.add("Ice Spike");
        moveToEnemy(mage, 0);
        mage.setRandomSeed(17L);
        int beforeSpell = mage.currentEnemy().health;
        mage.castSpell("Fireball");
        int spellDamage = beforeSpell -
            (mage.currentEnemy() == null ? 0 : mage.currentEnemy().health);

        GameEngine physicalMage = new GameEngine();
        physicalMage.newGame("Mage", "Human", "Mage");
        moveToEnemy(physicalMage, 0);
        physicalMage.setRandomSeed(17L);
        int beforeAttack = physicalMage.currentEnemy().health;
        physicalMage.attack();
        int attackDamage = beforeAttack - physicalMage.currentEnemy().health;
        require(spellDamage > attackDamage,
            "mage spells are meaningfully stronger than staff attacks");
        require(mage.getState().spells.contains("Ice Spike"),
            "level three mage receives the complete spell progression set");

        mage.getState().mana = mage.getState().maxMana;
        moveToEnemy(mage, 0);
        mage.selectSpell("Ice Spike");
        require("Ice Spike".equals(mage.getState().selectedSpell),
            "spellbook state can be updated without spending mana");
        require(mage.getState().mana == mage.getState().maxMana,
            "selecting a spell does not consume a combat turn or mana");
        mage.selectSpell("Fireball");
        int quickMana = mage.getState().mana;
        require("Fireball".equals(mage.getState().selectedSpell),
            "spell selection remembers the most recently cast spell");
        mage.castSelectedSpell();
        require(mage.getState().mana == quickMana - GameEngine.spellCost("Fireball"),
            "legacy selected-spell saves still cast the remembered spell");

        GameEngine difficulty = new GameEngine();
        difficulty.newGame("Difficulty", "Human", "Fighter");
        moveToEnemy(difficulty, 0);
        require(difficulty.currentEnemy().maxHealth >= 22 &&
            difficulty.currentEnemy().attack >= 8,
            "standard encounters use the raised difficulty curve");
        moveToEnemy(difficulty, 2);
        require(difficulty.currentEnemy().maxHealth >= 110 &&
                difficulty.currentEnemy().attack >= 16 &&
                difficulty.currentEnemy().defense <= 11 &&
                difficulty.currentEnemy().combatLevel <= 7,
            "boss curve targets prepared level three to four heroes");
    }

    private static GameEngine.Item item(GameEngine engine, String name) {
        for (GameEngine.Item item : engine.shopItems()) {
            if (name.equals(item.name)) return item;
        }
        throw new AssertionError("Missing shop item: " + name);
    }

    private static GameEngine.Item inventoryItem(GameEngine.State state, String name) {
        for (GameEngine.Item item : state.inventory) {
            if (name.equals(item.name)) return item;
        }
        throw new AssertionError("Missing inventory item: " + name);
    }

    private static boolean containsShopItem(GameEngine engine, String name) {
        for (GameEngine.Item item : engine.shopItems()) {
            if (name.equals(item.name)) return true;
        }
        return false;
    }

    private static void moveToEnemy(GameEngine engine, int tier) {
        GameEngine.State state = engine.getState();
        for (int row = 0; row < state.enemies.length; row++) {
            for (int col = 0; col < state.enemies[row].length; col++) {
                GameEngine.Enemy enemy = state.enemies[row][col];
                if (enemy != null && enemy.tier == tier && enemy.sourceId == null) {
                    state.row = row;
                    state.col = col;
                    return;
                }
            }
        }
        throw new AssertionError("Missing generated enemy tier " + tier);
    }

    private static void moveToShop(GameEngine engine, boolean blacksmith) {
        GameEngine.State state = engine.getState();
        for (int row = 0; row < state.tiles.length; row++) {
            for (int col = 0; col < state.tiles[row].length; col++) {
                if (state.tiles[row][col] == GameEngine.TileType.SHOP &&
                        state.blacksmithShops[row][col] == blacksmith) {
                    state.row = row;
                    state.col = col;
                    return;
                }
            }
        }
        throw new AssertionError("Missing generated shop");
    }

    private static void moveToVendor(GameEngine engine, String vendor) {
        if ("Blacksmith".equals(vendor)) {
            moveToShop(engine, true);
            return;
        }
        if ("General Merchant".equals(vendor)) {
            moveToShop(engine, false);
            return;
        }
        GameEngine.TileType wanted = "Innkeeper".equals(vendor)
            ? GameEngine.TileType.TAVERN : GameEngine.TileType.ENCAMPMENT;
        GameEngine.State state = engine.getState();
        for (int row = 0; row < state.tiles.length; row++) {
            for (int col = 0; col < state.tiles[row].length; col++) {
                if (state.tiles[row][col] == wanted) {
                    state.row = row;
                    state.col = col;
                    return;
                }
            }
        }
        throw new AssertionError("Missing vendor: " + vendor);
    }

    private static void testSaveRoundTripAndNormalization() throws Exception {
        GameEngine engine = new GameEngine();
        engine.newGame("Legacy Hero", "Halfling", "Rogue");
        GameEngine.State state = engine.getState();
        state.race = null;
        state.heroClass = null;
        state.level = 0;
        state.baseAttack = 0;
        state.inventory = null;
        state.relics = null;
        state.spells = null;
        state.weaponProficiency = null;
        state.gold = 77;
        // Simulate a schema-8 save from the original 5x5 logical world.
        state.tiles = new GameEngine.TileType[5][5];
        state.enemies = new GameEngine.Enemy[5][5];
        state.blacksmithShops = new boolean[5][5];
        state.discovery = new GameEngine.DiscoveryState[5][5];
        state.threatKnowledge = new byte[5][5];
        state.relicSearch = new boolean[5][5];
        state.rumorServicesUsed = new boolean[5][5];
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                state.tiles[row][col] = GameEngine.TileType.FIELD;
                state.discovery[row][col] = GameEngine.DiscoveryState.SCOUTED;
            }
        }
        state.tiles[0][0] = GameEngine.TileType.ENCAMPMENT;
        state.discovery[0][0] = GameEngine.DiscoveryState.VISITED;
        state.dragonLairRow = -1;
        state.dragonLairCol = -1;
        state.generationVersion = 8;
        File save = File.createTempFile("chosen-quest-regression-", ".save");
        try {
            engine.save(save);
            engine.load(save);
            state = engine.getState();
            require("Human".equals(state.race), "missing race normalized");
            require("Fighter".equals(state.heroClass), "missing class normalized");
            require(state.level == 1 && state.baseAttack == 5, "legacy stats normalized");
            require(state.inventory != null && state.spells != null && state.relics != null,
                "legacy collections normalized");
            require(state.weaponProficiency != null,
                "legacy saves receive weapon proficiency state");
            require(state.discovery != null && state.threatKnowledge != null &&
                state.relicSearch != null, "map discovery state survives save normalization");
            require(state.tiles.length == GameEngine.SIZE &&
                    state.tiles[0][0] == GameEngine.TileType.ENCAMPMENT &&
                    state.discovery[GameEngine.SIZE - 1][GameEngine.SIZE - 1] ==
                        GameEngine.DiscoveryState.UNKNOWN,
                "legacy 5x5 saves expand without losing known tiles");
            require(state.generationVersion == 11, "expanded saves advance to schema 11");
            require(state.spiderNestRow >= 0 && state.spiderNestRemaining == 3 &&
                    state.tiles[state.spiderNestRow][state.spiderNestCol] ==
                        GameEngine.TileType.SPIDER_NEST,
                "schema 10 adds the optional nest only in uncharted territory");
            require(state.gold == 77, "save preserves valid state");
        } finally {
            save.delete();
        }
    }

    private static HeroExpectation hero(String race, String heroClass, int health, int mana,
                                        int attack, int defense, int gold) {
        return new HeroExpectation(race, heroClass, health, mana, attack, defense, gold);
    }

    private static int occurrences(String text, String fragment) {
        int count = 0;
        int index = 0;
        while (text != null && (index = text.indexOf(fragment, index)) >= 0) {
            count++;
            index += fragment.length();
        }
        return count;
    }

    private static void require(boolean condition, String feature) {
        if (!condition) throw new AssertionError("Failed: " + feature);
    }

    private static final class HeroExpectation {
        final String race;
        final String heroClass;
        final int health;
        final int mana;
        final int attack;
        final int defense;
        final int gold;

        HeroExpectation(String race, String heroClass, int health, int mana,
                        int attack, int defense, int gold) {
            this.race = race;
            this.heroClass = heroClass;
            this.health = health;
            this.mana = mana;
            this.attack = attack;
            this.defense = defense;
            this.gold = gold;
        }
    }
}
