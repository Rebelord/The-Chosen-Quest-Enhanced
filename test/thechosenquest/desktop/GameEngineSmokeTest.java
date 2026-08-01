package thechosenquest.desktop;

import java.io.File;

public final class GameEngineSmokeTest {
    public static void main(String[] args) throws Exception {
        require(GameEngineSmokeTest.class.getResource("/assets/quest-background.png") != null,
            "bundled visual asset");
        require(GameEngineSmokeTest.class.getResource("/assets/title-screen.png") != null,
            "bundled enhanced title asset");
        require(GameEngineSmokeTest.class.getResource("/assets/title-logo.png") != null,
            "bundled transparent title logo");
        require(GameEngineSmokeTest.class.getResource("/assets/avatars/elf-mage.png") != null,
            "bundled square avatar asset");
        require(GameEngineSmokeTest.class.getResource("/assets/avatars/full-body/elf-mage.png") != null,
            "bundled full-body avatar asset");
        require(GameEngineSmokeTest.class.getResource("/assets/scenes/alshira-ruins.png") != null,
            "bundled exploration scene asset");
        require(GameEngineSmokeTest.class.getResource(
            "/assets/scenes/moonwater-crossing.png") != null,
            "bundled lake encounter scene");
        require(GameEngineSmokeTest.class.getResource(
            "/assets/scenes/forgotten-crypt.png") != null,
            "bundled crypt encounter scene");
        require(GameEngineSmokeTest.class.getResource(
            "/assets/encounters/dragons/ancient-red-dragon.png") != null,
            "bundled dragon encounter asset");
        require(GameEngineSmokeTest.class.getResource(
            "/assets/encounters/enemies/skeletal-guardian.png") != null,
            "bundled skeleton encounter asset");
        require(GameEngineSmokeTest.class.getResource(
            "/assets/encounters/npcs/general-merchant.png") != null,
            "bundled merchant asset");
        require(GameEngineSmokeTest.class.getResource(
            "/assets/encounters/npcs/blacksmith.png") != null,
            "bundled blacksmith asset");
        require(GameEngineSmokeTest.class.getResource(
            "/assets/encounters/npcs/alchemist.png") != null,
            "bundled alchemist asset");
        require(GameEngineSmokeTest.class.getResource(
            "/assets/encounters/npcs/innkeeper.png") != null,
            "bundled innkeeper asset");
        String[] iconResources = {
            IconAssets.WEAPON_SWORD, IconAssets.ARMOUR_SHIELD,
            IconAssets.POTION_HEALTH, IconAssets.INVENTORY_BACKPACK,
            IconAssets.SERVICE_REST, IconAssets.SERVICE_MEAL,
            IconAssets.QUEST_RUMOR, IconAssets.CURRENCY_COINS,
            IconAssets.MAGIC_ZAP, IconAssets.HEALTH_POTION,
            IconAssets.REGIONAL_MAP, IconAssets.UNIDENTIFIED_RELIC,
            IconAssets.IDENTIFIED_RELIC,
            IconAssets.itemResource("Oak Staff", "Weapon"),
            IconAssets.itemResource("Cloth Armour", "Armour"),
            IconAssets.itemResource("Long Bow", "Weapon"),
            IconAssets.itemResource("Plate Armour", "Armour")
        };
        for (String resource : iconResources) {
            require(GameEngineSmokeTest.class.getResource(resource) != null,
                "bundled icon " + resource);
        }
        require(IconAssets.itemResource((GameEngine.Item) null) == null,
            "empty equipment must not resolve to a shield resource");
        require(IconAssets.itemResource("Dagger", "Weapon").endsWith("weapon-dagger.png"),
            "named equipment artwork mapping");
        require(IconAssets.itemResource("Unknown Blade", "Weapon")
                .endsWith("weapon-arming-sword.png"),
            "unknown weapon artwork fallback");
        GameEngine engine = new GameEngine();
        engine.newGame("Test Hero");

        require(engine.getState().health == engine.getState().maxHealth, "new game health");
        require("Human".equals(engine.getState().race), "default race");
        require("Fighter".equals(engine.getState().heroClass), "default class");
        require(!engine.getState().inventory.isEmpty(), "starting equipment");
        require(engine.getState().row == 0 && engine.getState().col == 0, "start position");

        engine.move(0, 1);
        require(engine.getState().col == 1, "movement");

        engine.getState().health = 15;
        engine.getState().row = 0;
        engine.getState().col = 0;
        engine.locationAction();
        require(engine.getState().health == engine.getState().maxHealth, "resting");

        engine.newGame("Merlin", "Elf", "Mage");
        require(engine.getState().maxMana > 0, "mage mana");
        require(engine.getState().spells.size() == 1 &&
            engine.getState().spells.contains("Magic Missile"),
            "level one mage starts with a simple spellbook");
        moveToShop(engine, false);
        engine.getState().gold = 100;
        GameEngine.Item dagger = engine.shopItems().get(0);
        engine.buyItem(dagger);
        require(engine.getState().inventory.contains(dagger), "class-compatible equipment purchase");
        engine.equipItem(dagger);
        require("Dagger".equals(engine.getState().equippedWeapon), "equipment selection");

        moveToEnemy(engine, 0);
        engine.currentEnemy().health = 1;
        int oldMana = engine.getState().mana;
        engine.castSpell("Magic Missile");
        require(engine.currentEnemy() == null, "spell combat");
        require(engine.getState().mana < oldMana, "spell mana cost");

        File save = File.createTempFile("chosen-quest-", ".save");
        try {
            engine.getState().gold = 123;
            engine.save(save);
            engine.getState().gold = 0;
            engine.load(save);
            require(engine.getState().gold == 123, "save and load");
        } finally {
            save.delete();
        }

        System.out.println("Game engine smoke test passed.");
    }

    private static void require(boolean condition, String feature) {
        if (!condition) {
            throw new AssertionError("Failed: " + feature);
        }
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

    private static void moveToEnemy(GameEngine engine, int tier) {
        GameEngine.State state = engine.getState();
        for (int row = 0; row < state.enemies.length; row++) {
            for (int col = 0; col < state.enemies[row].length; col++) {
                GameEngine.Enemy enemy = state.enemies[row][col];
                if (enemy != null && enemy.tier == tier) {
                    state.row = row;
                    state.col = col;
                    return;
                }
            }
        }
        throw new AssertionError("Missing generated enemy tier " + tier);
    }
}
