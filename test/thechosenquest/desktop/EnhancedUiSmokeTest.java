package thechosenquest.desktop;

import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JPanel;

public final class EnhancedUiSmokeTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (!"0.5.0-beta.1".equals(AppVersion.VERSION) ||
                !"v0.5.0-beta.1".equals(AppVersion.TAG)) {
            throw new AssertionError("Public beta version and release tag must stay aligned");
        }
        if (EnhancedUiSmokeTest.class.getResource("/assets/fonts/Cinzel.ttf") == null ||
                EnhancedUiSmokeTest.class.getResource("/assets/fonts/CormorantGaramond.ttf") == null) {
            throw new AssertionError("Bundled UI fonts are missing");
        }
        AudioInputStream titleMusic = AudioSystem.getAudioInputStream(
            EnhancedUiSmokeTest.class.getResource("/assets/audio/title-theme.wav"));
        if (titleMusic.getFrameLength() <= 0) {
            throw new AssertionError("Bundled title music is not a readable audio stream");
        }
        titleMusic.close();
        final boolean[] randomizedName = {false};
        CharacterCreationPanel panel = new CharacterCreationPanel(new CharacterCreationPanel.Listener() {
            public void onBegin(String name, String race, String heroClass) {
            }

            public void onBack() {
            }

            public void onRandomizeName() {
                randomizedName[0] = true;
            }
        });
        panel.setSize(1440, 900);
        layoutTree(panel);
        JButton randomizeButton = findAccessibleButton(panel, "Randomize hero name");
        if (randomizeButton == null || randomizeButton.getIcon() == null) {
            throw new AssertionError("Name randomizer must use an accessible dice icon button");
        }
        randomizeButton.doClick();
        if (!randomizedName[0]) {
            throw new AssertionError("Name randomizer must request its dice-roll sound cue");
        }
        if (countPortraitFrames(panel) < 5) {
            throw new AssertionError("Race choices and the hero preview must use fantasy frames");
        }

        BufferedImage image = new BufferedImage(1440, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();

        File output = new File(args.length == 0 ? "build/character-creation-preview.png" : args[0]);
        ImageIO.write(image, "png", output);
        if (!output.isFile() || output.length() < 100000L) {
            throw new AssertionError("Enhanced UI render was not produced correctly");
        }
        JPanel frameIdentifiers = new JPanel(new GridLayout(1, 4, 12, 0));
        frameIdentifiers.setBackground(UiTheme.BACKGROUND);
        for (String heroClass : GameEngine.CLASSES) {
            AssetImagePanel portrait = new AssetImagePanel(
                "/assets/avatars/elf-" + heroClass.toLowerCase() + ".png", true);
            portrait.setBorder(new FantasyPortraitBorder("Elf", heroClass, true));
            frameIdentifiers.add(portrait);
        }
        File frameIdentifiersOutput = render(frameIdentifiers, 720, 180,
            output.getParentFile(), "frame-identifiers-preview.png", 20000L);

        // A newly requested encounter must never display the previous enemy's
        // portrait while its own artwork is loading.
        AssetImagePanel encounterSwap = new AssetImagePanel(
            "/assets/encounters/creatures/armored-boar.png", true);
        encounterSwap.setSize(420, 420);
        encounterSwap.setResourceAsync(
            "/assets/encounters/creatures/swamp-serpent.png");
        String displayed = encounterSwap.displayedResourceForTest();
        if (displayed != null && !displayed.endsWith("swamp-serpent.png")) {
            throw new AssertionError("Serpent encounter retained the prior boar portrait");
        }
        encounterSwap.setResourceAsync(
            "/assets/encounters/elites/necromancer.png");
        displayed = encounterSwap.displayedResourceForTest();
        if (displayed != null && !displayed.endsWith("necromancer.png")) {
            throw new AssertionError("Necromancer encounter retained the prior serpent portrait");
        }
        encounterSwap.awaitResource();
        if (!"/assets/encounters/elites/necromancer.png".equals(
                encounterSwap.displayedResourceForTest())) {
            throw new AssertionError("Rapid encounter switching did not settle on Necromancer art");
        }
        if (!"/assets/scenes/moonwater-crossing.png".equals(
                EnhancedExplorationPanel.sceneResourceFor(GameEngine.TileType.LAKE, null)) ||
                !"/assets/scenes/forgotten-crypt.png".equals(
                EnhancedExplorationPanel.sceneResourceFor(GameEngine.TileType.CRYPT, null)) ||
                !"/assets/scenes/blacksmith-forge-exterior.png".equals(
                EnhancedExplorationPanel.sceneResourceFor(GameEngine.TileType.SHOP, "Blacksmith")) ||
                !"/assets/scenes/frontier-market-exterior.png".equals(
                EnhancedExplorationPanel.sceneResourceFor(GameEngine.TileType.SHOP,
                    "General Merchant")) ||
                !"/assets/scenes/wanderers-rest-exterior.png".equals(
                EnhancedExplorationPanel.sceneResourceFor(GameEngine.TileType.TAVERN,
                    "Innkeeper")) ||
                !"/assets/scenes/alchemist-waycamp-exterior.png".equals(
                EnhancedExplorationPanel.sceneResourceFor(GameEngine.TileType.ENCAMPMENT,
                    "Alchemist")) ||
                !"/assets/scenes/ashweb-nest.png".equals(
                EnhancedExplorationPanel.sceneResourceFor(GameEngine.TileType.SPIDER_NEST,
                    null))) {
            throw new AssertionError(
                "Pre-entry safe locations must use location art rather than NPC portraits");
        }
        String[] exteriorScenes = {
            "/assets/scenes/blacksmith-forge-exterior.png",
            "/assets/scenes/frontier-market-exterior.png",
            "/assets/scenes/wanderers-rest-exterior.png",
            "/assets/scenes/alchemist-waycamp-exterior.png",
            "/assets/scenes/ashweb-nest.png"
        };
        for (String resource : exteriorScenes) {
            BufferedImage exterior = ImageIO.read(EnhancedUiSmokeTest.class.getResource(resource));
            if (exterior == null || exterior.getWidth() != 1536 || exterior.getHeight() != 672) {
                throw new AssertionError("Exterior scene must match the 1536x672 layout: " + resource);
            }
        }

        GameEngine mapEngine = new GameEngine();
        mapEngine.setRandomSeed(404L);
        mapEngine.newGame("Map Scout", "Human", "Hunter");
        GameEngine.State mapState = mapEngine.getState();
        mapState.discovery[mapState.dragonLairRow][mapState.dragonLairCol] =
            GameEngine.DiscoveryState.SCOUTED;
        mapState.threatKnowledge[mapState.dragonLairRow][mapState.dragonLairCol] = 2;
        GameMapPanel scrollingMap = new GameMapPanel(mapEngine);
        if (scrollingMap.viewSizeForTest() != GameMapPanel.OVERVIEW_VIEW_SIZE) {
            throw new AssertionError("Map must open in the 9x9 exploration view");
        }
        if (GameMapPanel.heroMarkerSize(25) > 15 ||
                GameMapPanel.enemyMarkerSize(25) >= GameMapPanel.heroMarkerSize(25) ||
                GameMapPanel.warningMarkerSize(25) >= GameMapPanel.heroMarkerSize(25) ||
                GameMapPanel.heroMarkerSize(16) > 10) {
            throw new AssertionError(
                "Map markers must scale proportionally and simplify at overview density");
        }
        File mapViewportOutput = render(scrollingMap, 288, 280,
            output.getParentFile(), "map-viewport-preview.png", 8000L);
        if (!scrollingMap.offscreenHintSummaryForTest().contains("Dragon") ||
                scrollingMap.viewRowForTest() != 0 || scrollingMap.viewColForTest() != 0) {
            throw new AssertionError(
                "Known off-screen boss must appear as a knowledge-aware edge hint");
        }
        scrollingMap.pan(2, 2);
        if (scrollingMap.viewRowForTest() != 2 || scrollingMap.viewColForTest() != 2) {
            throw new AssertionError("Map camera must pan independently from hero movement");
        }
        scrollingMap.centerOnHero();
        if (scrollingMap.viewRowForTest() != 0 || scrollingMap.viewColForTest() != 0) {
            throw new AssertionError("Map camera must return to the hero");
        }
        mapState.row = 6;
        mapState.col = 6;
        scrollingMap.centerOnHero();
        scrollingMap.zoomIn();
        if (scrollingMap.viewSizeForTest() != GameMapPanel.DETAIL_VIEW_SIZE ||
                scrollingMap.viewRowForTest() != 3 || scrollingMap.viewColForTest() != 3) {
            throw new AssertionError("7x7 detail zoom must remain centered on the hero");
        }
        File mapDetailOutput = render(scrollingMap, 288, 280,
            output.getParentFile(), "map-detail-preview.png", 8000L);
        scrollingMap.pan(1, 1);
        scrollingMap.zoomOut();
        if (scrollingMap.viewSizeForTest() != GameMapPanel.OVERVIEW_VIEW_SIZE ||
                scrollingMap.viewRowForTest() != 3 || scrollingMap.viewColForTest() != 3) {
            throw new AssertionError("Zooming a panned map must preserve its inspected center");
        }
        mapState.row = GameEngine.SIZE - 1;
        mapState.col = GameEngine.SIZE - 1;
        scrollingMap.centerOnHero();
        if (scrollingMap.viewRowForTest() !=
                GameEngine.SIZE - GameMapPanel.OVERVIEW_VIEW_SIZE ||
                scrollingMap.viewColForTest() !=
                    GameEngine.SIZE - GameMapPanel.OVERVIEW_VIEW_SIZE) {
            throw new AssertionError("Map viewport must clamp cleanly at world edges");
        }

        JPanel titleScreen = MainWindow.buildTitleScreen(new ActionListener() {
            public void actionPerformed(ActionEvent event) { }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) { }
        });
        File titleOutput = render(titleScreen, 1440, 900, output.getParentFile(),
            "title-screen-preview.png");

        EncounterPanel encounter = new EncounterPanel();
        if (!CombatVfx.prototypeAssetsAvailable()) {
            throw new AssertionError("Bundled prototype combat VFX are incomplete or unreadable");
        }
        if (EncounterPanel.PlayerAttackStyle.forAction("Fighter", false) !=
                EncounterPanel.PlayerAttackStyle.FIGHTER ||
                EncounterPanel.PlayerAttackStyle.forAction("Mage", false) !=
                EncounterPanel.PlayerAttackStyle.MAGE ||
                EncounterPanel.PlayerAttackStyle.forAction("Rogue", false) !=
                EncounterPanel.PlayerAttackStyle.ROGUE ||
                EncounterPanel.PlayerAttackStyle.forAction("Hunter", false) !=
                EncounterPanel.PlayerAttackStyle.HUNTER ||
                EncounterPanel.PlayerAttackStyle.forAction("Fighter", true) !=
                EncounterPanel.PlayerAttackStyle.SPELL ||
                EncounterPanel.PlayerAttackStyle.forAction("Mage", "Magic Missile") !=
                EncounterPanel.PlayerAttackStyle.SPELL ||
                EncounterPanel.PlayerAttackStyle.forAction("Mage", "Fireball") !=
                EncounterPanel.PlayerAttackStyle.FIREBALL ||
                EncounterPanel.PlayerAttackStyle.forAction("Mage", "Ice Spike") !=
                EncounterPanel.PlayerAttackStyle.ICE_SPIKE) {
            throw new AssertionError("Hero actions must map to their Figma combat motion styles");
        }
        if (EncounterPanel.animationDurationForTier(EncounterCatalog.Tier.STANDARD, false) != 400 ||
                EncounterPanel.animationDurationForTier(EncounterCatalog.Tier.ELITE, false) != 430 ||
                EncounterPanel.animationDurationForTier(EncounterCatalog.Tier.BOSS, false) != 520 ||
                EncounterPanel.animationDurationForTier(EncounterCatalog.Tier.BOSS, true) != 220 ||
                EncounterPanel.shakeStrengthForTier(EncounterCatalog.Tier.STANDARD) != 4 ||
                EncounterPanel.shakeStrengthForTier(EncounterCatalog.Tier.ELITE) != 6 ||
                EncounterPanel.shakeStrengthForTier(EncounterCatalog.Tier.BOSS) != 9) {
            throw new AssertionError("Combat threat timing must stay synchronized with Figma");
        }
        if (!"/assets/scenes/alshira-ruins.png".equals(
                EncounterPanel.sceneFor(GameEngine.TileType.FIELD)) ||
                !"/assets/scenes/moonwater-crossing.png".equals(
                EncounterPanel.sceneFor(GameEngine.TileType.LAKE)) ||
                !"/assets/scenes/forgotten-crypt.png".equals(
                EncounterPanel.sceneFor(GameEngine.TileType.CRYPT)) ||
                !"/assets/scenes/ashweb-nest.png".equals(
                EncounterPanel.sceneFor(GameEngine.TileType.SPIDER_NEST))) {
            throw new AssertionError("Combat tile scenes are not mapped correctly");
        }
        GameEngine.State previewState = new GameEngine().getState();
        encounter.setEncounter(new GameEngine.Enemy("Dragon", 48, 12, 1500),
            "• A Dragon confronts you!\n• Critical strike!\n" +
            "• You cast Fireball for 17 damage.\n" +
            "• The Dragon hits you for 8 damage.\n", previewState);
        Color criticalColor = encounter.logColorForTextForTest("Critical strike");
        Color spellColor = encounter.logColorForTextForTest("Fireball");
        Color incomingColor = encounter.logColorForTextForTest("hits you");
        if (criticalColor == null || criticalColor.equals(spellColor) ||
                spellColor.equals(incomingColor) ||
                !encounter.logIsBoldForTextForTest("Critical strike") ||
                encounter.logFontSizeForTextForTest("Critical strike") < 14 ||
                encounter.logLineSpacingForTextForTest("Critical strike") < 0.15f) {
            throw new AssertionError("Combat actions must use distinct emphasized log styles");
        }
        encounter.setSize(760, 720);
        layoutTree(encounter);
        encounter.awaitArtworkForSnapshot();
        BufferedImage encounterImage = new BufferedImage(760, 720, BufferedImage.TYPE_INT_ARGB);
        graphics = encounterImage.createGraphics();
        encounter.printAll(graphics);
        graphics.dispose();
        File encounterOutput = new File(output.getParentFile(), "encounter-preview.png");
        ImageIO.write(encounterImage, "png", encounterOutput);
        if (!encounterOutput.isFile() || encounterOutput.length() < 50000L) {
            throw new AssertionError("Encounter UI render was not produced correctly");
        }

        final int[] shortcuts = new int[7];
        EncounterPanel shortcutPanel = new EncounterPanel(new EncounterPanel.Listener() {
            public void onAttack() { shortcuts[0]++; }
            public void onDefend() { shortcuts[1]++; }
            public void onAbility(int slot) { shortcuts[slot + 1]++; }
            public void onPotion() { shortcuts[5]++; }
            public void onFlee() { shortcuts[6]++; }
        });
        GameEngine shortcutEngine = new GameEngine();
        shortcutEngine.newGame("Shortcut Mage", "Elf", "Mage");
        shortcutEngine.getState().health--;
        shortcutPanel.setEncounter(new GameEngine.Enemy("Skeleton", 24, 9, 10), "",
            shortcutEngine.getState());
        if (shortcutPanel.visibleAbilityCountForTest() != 1 ||
                shortcutPanel.triggerShortcut('4') || shortcutPanel.triggerShortcut('5')) {
            throw new AssertionError("Level one combat must reveal only the starter ability");
        }
        shortcutEngine.getState().level = 3;
        shortcutEngine.getState().spells.add("Fireball");
        shortcutEngine.getState().spells.add("Ice Spike");
        shortcutPanel.setEncounter(new GameEngine.Enemy("Skeleton", 24, 9, 10), "",
            shortcutEngine.getState());
        if (shortcutPanel.visibleAbilityCountForTest() != 3) {
            throw new AssertionError("Level three combat must reveal the complete ability set");
        }
        if (!"Channel Ward".equals(shortcutPanel.defenseActionLabelForTest())) {
            throw new AssertionError("Mage combat action must be labeled Channel Ward");
        }
        if (!"NORMAL".equals(shortcutPanel.attackSpeedLabelForTest()) ||
                !"NORMAL".equals(shortcutPanel.spellSpeedLabelForTest()) ||
                shortcutPanel.timelineForTest().length() == 0) {
            throw new AssertionError("Combat action tempo and turn timeline must be visible");
        }
        char[] keys = {'1', '2', '3', '4', '5', '6', '7'};
        for (char key : keys) {
            if (!shortcutPanel.triggerShortcut(key)) {
                throw new AssertionError("Combat shortcut was not routed: " + key);
            }
        }
        for (int count : shortcuts) {
            if (count != 1) throw new AssertionError("Combat shortcut routed incorrectly");
        }
        if (shortcutPanel.triggerShortcut('X')) {
            throw new AssertionError("Unknown combat shortcut should be ignored");
        }
        if (!shortcutPanel.statusFeedbackForTest().contains("ARCANE FOCUS")) {
            throw new AssertionError("Equipped weapon trait must be visible in combat status");
        }

        SoundManager dialogueSound = new SoundManager();
        GameSettingsOverlay dialogueOverlay = new GameSettingsOverlay(dialogueSound,
            new GamePreferences(), null);
        dialogueOverlay.showDialogue("/assets/encounters/npcs/innkeeper.png", "Bram",
            "INNKEEPER · LOCAL RUMOR", "A shadow waits beyond the eastern road.",
            UiTheme.GOLD, null);
        if (!dialogueOverlay.dialogueVisibleForTest()) {
            throw new AssertionError("NPC dialogue must use the custom in-game overlay");
        }
        dialogueOverlay.completeDialogueForTest();
        if (!dialogueOverlay.dialogueTextForTest().contains("eastern road")) {
            throw new AssertionError("Typewriter dialogue must reveal the complete message");
        }
        dialogueOverlay.setSize(1100, 720);
        layoutTree(dialogueOverlay);
        File dialogueOutput = render(dialogueOverlay, 1100, 720,
            output.getParentFile(), "dialogue-preview.png", 30000L);
        dialogueOverlay.hideSettings();
        dialogueOverlay.showProgression(new GameEngine.ProgressionNotice(
            2, "Cleave", "HEAVY BLADE", 2), null, null);
        dialogueOverlay.setSize(1100, 720);
        layoutTree(dialogueOverlay);
        File progressionOutput = render(dialogueOverlay, 1100, 720,
            output.getParentFile(), "progression-preview.png", 12000L);
        dialogueSound.shutdown();

        // A killing blow disables controls during its animation. The reused
        // combat panel must restore them when the next encounter is loaded.
        shortcutPanel.setCombatActionsEnabled(false);
        shortcutPanel.setEncounter(new GameEngine.Enemy("Bandit Marauder", 28, 8, 12),
            "A second enemy approaches.\n", shortcutEngine.getState());
        if (!shortcutPanel.triggerShortcut('1') || !shortcutPanel.triggerShortcut('2') ||
                !shortcutPanel.triggerShortcut('7') || shortcuts[0] != 2 ||
                shortcuts[1] != 2 || shortcuts[6] != 2) {
            throw new AssertionError("A later encounter did not restore combat controls");
        }

        if (UiTheme.HERO_RAIL_WIDTH + UiTheme.CENTER_WIDTH + UiTheme.MAP_RAIL_WIDTH
                != UiTheme.SHELL_WIDTH) {
            throw new AssertionError("Figma shell columns no longer total 1440px");
        }
        int renderedProfiles = 0;
        for (EncounterCatalog.Profile profile : EncounterCatalog.allProfiles()) {
            if (profile == null) throw new AssertionError("Encounter profile is missing");
            boolean shouldMirror = "corrupted-shadow-dragon".equals(profile.id) ||
                "skeletal-undead-dragon".equals(profile.id);
            if (profile.mirrorArtwork != shouldMirror) {
                throw new AssertionError("Dragon combat orientation is incorrect: " + profile.id);
            }
            if (EnhancedUiSmokeTest.class.getResource(profile.artwork) == null) {
                throw new AssertionError("Encounter artwork is missing: " + profile.artwork);
            }
            EncounterPanel variant = new EncounterPanel();
            int healthValue = profile.tier == EncounterCatalog.Tier.BOSS ? 4200 :
                (profile.tier == EncounterCatalog.Tier.ELITE ? 120 : 55);
            variant.setEncounter(new GameEngine.Enemy(profile.displayName, healthValue, 10, 25),
                "The encounter begins.\nChoose an action to continue.", previewState);
            int expectedTraits = profile.tags.trim().split("\\s{2,}").length;
            if (variant.traitChipCountForTest() != expectedTraits) {
                throw new AssertionError("Encounter traits must render as individual chips: " +
                    profile.displayName);
            }
            render(variant, UiTheme.CENTER_WIDTH, UiTheme.BODY_HEIGHT, output.getParentFile(),
                "encounter-" + profile.id + "-preview.png");
            renderedProfiles++;
        }
        if (renderedProfiles != 16) {
            throw new AssertionError("Expected all 16 Figma encounter variants");
        }

        GameEngine.TileType[] sceneTiles = {
            GameEngine.TileType.LAKE, GameEngine.TileType.CRYPT
        };
        String[] sceneNames = {"lake", "crypt"};
        for (int i = 0; i < sceneTiles.length; i++) {
            previewState.tiles[previewState.row][previewState.col] = sceneTiles[i];
            EncounterPanel tileVariant = new EncounterPanel();
            tileVariant.setEncounter(new GameEngine.Enemy("Bandit Marauder", 45, 8, 12),
                "The terrain changes the shape of the battlefield.", previewState);
            render(tileVariant, UiTheme.CENTER_WIDTH, UiTheme.BODY_HEIGHT,
                output.getParentFile(), "encounter-" + sceneNames[i] + "-preview.png");
        }
        previewState.tiles[previewState.row][previewState.col] = GameEngine.TileType.FIELD;

        final GameEngine engine = new GameEngine();
        engine.newGame("Aelindra", "Elf", "Mage");
        final int[] havenVisits = new int[] { 0 };
        EnhancedExplorationPanel exploration = new EnhancedExplorationPanel(engine,
            new EnhancedExplorationPanel.Listener() {
                public void onInventory() { }
                public void onSpellbook() { }
                public void onShop() { }
                public void onHaven() { havenVisits[0]++; }
                public void onSave() { }
                public void onLoad() { }
                public void onNewQuest() { }
                public void onSettings() { }
                public boolean onToggleMute() { return false; }
                public void onSound(SoundManager.Cue cue) { }
                public void onStateChanged() { }
            });
        moveToTile(engine.getState(), GameEngine.TileType.TAVERN, false, false);
        engine.getState().health = 7;
        exploration.refresh();
        if (!"ENTER TAVERN".equals(exploration.primaryActionLabelForTest())) {
            throw new AssertionError("Tavern primary action must be labeled ENTER TAVERN");
        }
        exploration.triggerPrimaryActionForTest();
        if (havenVisits[0] != 1 || engine.getState().health != 7) {
            throw new AssertionError("Entering a tavern must open the haven without resting");
        }
        exploration.awaitArtworkForSnapshot();
        File explorationOutput = render(exploration, 1440, 900, output.getParentFile(),
            "exploration-preview.png");
        InventoryPanel inventory = new InventoryPanel(new InventoryPanel.Listener() {
            public void onEquip(GameEngine.Item item) { }
            public void onBack() { }
        });
        GameEngine.Relic previewRelic = new GameEngine.Relic(
            "Sealed Soulglass", "Necromancer", "Alchemist");
        engine.getState().relics.add(previewRelic);
        inventory.setState(engine.getState(), engine.getAttack(), engine.getDefense());
        if (!inventory.relicSummaryForTest().contains("Sealed Soulglass") ||
                !inventory.relicSummaryForTest().contains("Alchemist")) {
            throw new AssertionError("Inventory must preserve relic identity and vendor hint");
        }
        String progression = inventory.abilitySummaryForTest();
        if (!progression.contains("Core Training:LEVEL 1 · READY") ||
                !progression.contains("UNLOCKS LEVEL 2") ||
                !progression.contains("UNLOCKS LEVEL 3")) {
            throw new AssertionError(
                "Character sheet must communicate ready and upcoming abilities");
        }
        File inventoryOutput = render(inventory, 760, 720, output.getParentFile(),
            "inventory-preview.png");
        engine.getState().relics.remove(previewRelic);

        LocationPanel location = new LocationPanel(new LocationPanel.Listener() {
            public void onBuy(Object selection) { }
            public void onRest() { }
            public void onMapService(GameEngine.MapService service) { }
            public void onBack() { }
        });
        engine.getState().gold = 100;
        location.showShop(engine.getState(), engine.shopItems());
        int allShopWares = location.visibleWareCount();
        location.selectTab("WEAPONS");
        int weaponWares = location.visibleWareCount();
        location.selectTab("SUPPLIES");
        if (location.visibleWareCount() != 1 || weaponWares < 1 ||
                allShopWares <= weaponWares) {
            throw new AssertionError("Shop category tabs must filter the visible wares");
        }
        location.selectTab("ALL");
        File locationOutput = render(location, 760, 720, output.getParentFile(),
            "shop-preview.png");
        engine.getState().health = 21;
        engine.getState().mana = 9;
        location.showHaven(engine.getState(), GameEngine.TileType.TAVERN);
        location.selectTab("RUMORS");
        if (location.visibleWareCount() != 1) {
            throw new AssertionError("Haven category tabs must filter services");
        }
        location.selectTab("ALL");
        File tavernOutput = render(location, 760, 720, output.getParentFile(),
            "tavern-preview.png");

        OutcomePanel outcome = new OutcomePanel(new OutcomePanel.Listener() {
            public void onNewQuest() { }
            public void onTitleScreen() { }
        });
        engine.getState().won = true;
        engine.getState().level = 4;
        engine.getState().gold = 1620;
        outcome.showVictory(engine.getState());
        File victoryOutput = render(outcome, 1440, 900, output.getParentFile(),
            "victory-preview.png");
        engine.getState().won = false;
        engine.getState().health = 0;
        OutcomePanel defeat = new OutcomePanel(new OutcomePanel.Listener() {
            public void onNewQuest() { }
            public void onTitleScreen() { }
        });
        defeat.showDefeat(engine.getState());
        File defeatOutput = render(defeat, 1440, 900, output.getParentFile(),
            "defeat-preview.png");

        final GameEngine shellEngine = new GameEngine();
        shellEngine.newGame("Aelindra", "Elf", "Mage");
        EnhancedExplorationPanel shell = new EnhancedExplorationPanel(shellEngine,
            new EnhancedExplorationPanel.Listener() {
                public void onInventory() { }
                public void onSpellbook() { }
                public void onShop() { }
                public void onHaven() { }
                public void onSave() { }
                public void onLoad() { }
                public void onNewQuest() { }
                public void onSettings() { }
                public boolean onToggleMute() { return false; }
                public void onSound(SoundManager.Cue cue) { }
                public void onStateChanged() { }
            });
        shell.triggerMapZoomShortcut(true);
        if (shell.mapForTest().viewSizeForTest() != GameMapPanel.DETAIL_VIEW_SIZE) {
            throw new AssertionError("Visible map zoom control must enter the 7x7 detail view");
        }
        shell.triggerMapZoomShortcut(false);
        if (shell.mapForTest().viewSizeForTest() != GameMapPanel.OVERVIEW_VIEW_SIZE) {
            throw new AssertionError("Visible map zoom control must restore the 9x9 view");
        }
        EncounterPanel shellEncounter = new EncounterPanel();
        InventoryPanel shellInventory = new InventoryPanel(new InventoryPanel.Listener() {
            public void onEquip(GameEngine.Item item) { }
            public void onBack() { }
        });
        LocationPanel shellLocation = new LocationPanel(new LocationPanel.Listener() {
            public void onBuy(Object selection) { }
            public void onRest() { }
            public void onMapService(GameEngine.MapService service) { }
            public void onBack() { }
        });
        OutcomePanel shellOutcome = new OutcomePanel(new OutcomePanel.Listener() {
            public void onNewQuest() { }
            public void onTitleScreen() { }
        });
        shell.setAuxiliaryPanels(shellEncounter, shellInventory, shellLocation, shellOutcome);

        GameEngine.State shellState = shellEngine.getState();
        moveToEnemy(shellState, 2);
        shell.refresh();
        shellEncounter.setEncounter(shellEngine.currentEnemy(), shellEngine.getHistory(), shellState);
        shell.showEncounter();
        File shellCombat = render(shell, 1440, 900, output.getParentFile(),
            "figma-combat-shell-preview.png");

        shellInventory.setState(shellState, shellEngine.getAttack(), shellEngine.getDefense());
        shell.showInventory();
        File shellInventoryOutput = render(shell, 1440, 900, output.getParentFile(),
            "figma-inventory-shell-preview.png");

        moveToTile(shellState, GameEngine.TileType.SHOP, false, false);
        shell.refresh();
        shellLocation.showShop(shellState, shellEngine.shopItems());
        shell.showLocation();
        File shellShop = render(shell, 1440, 900, output.getParentFile(),
            "figma-shop-shell-preview.png");

        moveToTile(shellState, GameEngine.TileType.SHOP, true, false);
        shell.refresh();
        shellLocation.showShop(shellState, shellEngine.shopItems());
        shell.showLocation();
        File shellBlacksmith = render(shell, 1440, 900, output.getParentFile(),
            "figma-blacksmith-shell-preview.png");

        moveToTile(shellState, GameEngine.TileType.ENCAMPMENT, false, true);
        shell.refresh();
        shellLocation.showHaven(shellState, GameEngine.TileType.ENCAMPMENT);
        shell.showLocation();
        File shellAlchemist = render(shell, 1440, 900, output.getParentFile(),
            "figma-alchemist-shell-preview.png");

        moveToTile(shellState, GameEngine.TileType.TAVERN, false, false);
        shell.refresh();
        shellLocation.showHaven(shellState, GameEngine.TileType.TAVERN);
        shell.showLocation();
        File shellTavern = render(shell, 1440, 900, output.getParentFile(),
            "figma-tavern-shell-preview.png");

        shellState.row = shellState.spiderNestRow;
        shellState.col = shellState.spiderNestCol;
        shellState.enemies[shellState.row][shellState.col] = null;
        shellState.spiderNestRemaining = 2;
        shellState.spiderNestCleared = false;
        shellState.discovery[shellState.row][shellState.col] =
            GameEngine.DiscoveryState.VISITED;
        shellEngine.locationAction();
        shellEngine.currentEnemy().health = 1;
        shellEngine.setRandomSeed(1L);
        shellEngine.attack();
        shell.refresh();
        shell.showStory();
        if (!"SEARCH NEST".equals(shell.primaryActionLabelForTest())) {
            throw new AssertionError("Uncleared source must expose its finite search action");
        }
        File shellSpiderNest = render(shell, 1440, 900, output.getParentFile(),
            "figma-spider-nest-shell-preview.png");

        final SoundManager soundManager = new SoundManager();
        if (soundManager.cachedCueCount() != SoundManager.Cue.values().length) {
            throw new AssertionError("Every sound cue must be prepared in the cache");
        }
        if (soundManager.cachedAmbienceCount() != SoundManager.Ambience.values().length - 1) {
            throw new AssertionError("Every non-silent ambience must be prepared in the cache");
        }
        java.util.HashSet<SoundManager.Cue> creationCues =
            new java.util.HashSet<SoundManager.Cue>();
        for (String race : GameEngine.RACES) creationCues.add(SoundManager.cueForRace(race));
        for (String heroClass : GameEngine.CLASSES) {
            creationCues.add(SoundManager.cueForClass(heroClass));
        }
        if (creationCues.size() != 8) {
            throw new AssertionError("Every race and class needs a distinct selection sound");
        }
        GameSettingsPanel settings = new GameSettingsPanel(soundManager, new GamePreferences(),
            new GameSettingsPanel.Listener() {
                public void onCancel() { }
                public void onApplied() { }
            });
        File settingsOutput = render(settings, 620, 620, output.getParentFile(),
            "settings-preview.png");
        soundManager.shutdown();

        JPanel buttonStates = new JPanel(new GridLayout(3, 5, 12, 12));
        buttonStates.setBackground(UiTheme.BACKGROUND);
        buttonStates.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 24, 24, 24));
        for (int style = 0; style < 3; style++) {
            for (int state = 0; state < 5; state++) {
                JButton stateButton = UiTheme.button(buttonStateName(state), style == 0);
                if (style == 2) {
                    UiTheme.applyButtonStyle(stateButton, UiTheme.ButtonStyle.DANGER, 10, 20);
                }
                configureButtonState(stateButton, state);
                buttonStates.add(stateButton);
            }
        }
        File buttonStatesOutput = render(buttonStates, 900, 250, output.getParentFile(),
            "button-states-preview.png", 5000L);

        System.out.println("Enhanced UI smoke tests passed: " + titleOutput.getPath() + ", " +
            output.getPath() + ", " + frameIdentifiersOutput.getPath() + ", " +
            mapViewportOutput.getPath() + ", " + mapDetailOutput.getPath() + ", " +
            explorationOutput.getPath() + ", " + encounterOutput.getPath() + ", " +
            inventoryOutput.getPath() + " and " +
            locationOutput.getPath() + ", " + tavernOutput.getPath() + ", " +
            victoryOutput.getPath() + ", " + defeatOutput.getPath() + ", " +
            shellCombat.getPath() + ", " + shellInventoryOutput.getPath() + ", " +
            shellShop.getPath() + ", " + shellBlacksmith.getPath() + ", " +
            shellAlchemist.getPath() + ", " + shellTavern.getPath() + ", " +
            shellSpiderNest.getPath() + ", " +
            settingsOutput.getPath() + ", " + buttonStatesOutput.getPath());
    }

    private static String buttonStateName(int state) {
        String[] names = {"DEFAULT", "HOVER", "PRESSED", "ACTIVE", "DISABLED"};
        return names[state];
    }

    private static void moveToEnemy(GameEngine.State state, int tier) {
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

    private static void moveToTile(GameEngine.State state, GameEngine.TileType tile,
                                   boolean blacksmith, boolean remoteCamp) {
        for (int row = 0; row < state.tiles.length; row++) {
            for (int col = 0; col < state.tiles[row].length; col++) {
                if (state.tiles[row][col] != tile) continue;
                if (tile == GameEngine.TileType.SHOP &&
                        state.blacksmithShops[row][col] != blacksmith) continue;
                if (remoteCamp && row == 0 && col == 0) continue;
                state.row = row;
                state.col = col;
                return;
            }
        }
        throw new AssertionError("Missing generated tile " + tile);
    }

    private static void configureButtonState(JButton button, int state) {
        if (state == 1) button.getModel().setRollover(true);
        if (state == 2) {
            button.getModel().setArmed(true);
            button.getModel().setPressed(true);
        }
        if (state == 3) UiTheme.setButtonActive(button, true);
        if (state == 4) button.setEnabled(false);
    }

    private static File render(Container component, int width, int height,
                               File directory, String name) throws Exception {
        return render(component, width, height, directory, name, 20000L);
    }

    private static File render(Container component, int width, int height,
                               File directory, String name, long minimumBytes) throws Exception {
        component.setSize(width, height);
        layoutTree(component);
        awaitAssetImages(component);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        component.printAll(graphics);
        graphics.dispose();
        File output = new File(directory, name);
        ImageIO.write(image, "png", output);
        if (!output.isFile() || output.length() < minimumBytes) {
            throw new AssertionError(name + " was not produced correctly");
        }
        return output;
    }

    /** Ensures asynchronous production artwork is present in deterministic UI snapshots. */
    private static void awaitAssetImages(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof AssetImagePanel) {
                ((AssetImagePanel) component).awaitResource();
            }
            if (component instanceof Container) {
                awaitAssetImages((Container) component);
            }
        }
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                layoutTree((Container) component);
            }
            if (component instanceof JComponent) {
                ((JComponent) component).revalidate();
            }
        }
    }

    private static JButton findAccessibleButton(Container container, String accessibleName) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton && accessibleName.equals(
                    ((JButton) component).getAccessibleContext().getAccessibleName())) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton nested = findAccessibleButton((Container) component, accessibleName);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static int countPortraitFrames(Container container) {
        int count = container instanceof JComponent &&
            ((JComponent) container).getBorder() instanceof FantasyPortraitBorder ? 1 : 0;
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                count += countPortraitFrames((Container) component);
            }
        }
        return count;
    }
}
