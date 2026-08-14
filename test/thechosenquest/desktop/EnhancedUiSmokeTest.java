package thechosenquest.desktop;

import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
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
        if (!"0.7.0-beta.2".equals(AppVersion.VERSION) ||
                !"v0.7.0-beta.2".equals(AppVersion.TAG)) {
            throw new AssertionError("Public beta version and release tag must stay aligned");
        }
        if (EnhancedUiSmokeTest.class.getResource("/assets/fonts/Cinzel.ttf") == null ||
                EnhancedUiSmokeTest.class.getResource("/assets/fonts/CormorantGaramond.ttf") == null) {
            throw new AssertionError("Bundled UI fonts are missing");
        }
        if (IconAssets.emptySlotIcon(24) == null ||
                IconAssets.itemResource((GameEngine.Item) null) != null) {
            throw new AssertionError(
                "Empty equipment must use a neutral icon rather than a shield resource");
        }
        String[] frameResources = {
            "/assets/ui/borders/button-frame-brass-v1.png",
            "/assets/ui/borders/panel-frame-brass-v1.png",
            "/assets/ui/borders/hero-frame-metal-overlay-v5.png",
            "/assets/ui/borders/hero-frame-enamel-mask-v5.png",
            "/assets/ui/borders/hero-frame-gem-mask-v5.png"
        };
        for (String resource : frameResources) {
            BufferedImage frame = ImageIO.read(
                EnhancedUiSmokeTest.class.getResource(resource));
            if (frame == null || !frame.getColorModel().hasAlpha() ||
                    ((frame.getRGB(0, 0) >>> 24) & 255) != 0) {
                throw new AssertionError(
                    "Authored UI frames must be readable transparent overlays: " +
                    resource);
            }
        }
        BufferedImage fighterFrame = new BufferedImage(233, 310,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D fighterFrameGraphics = fighterFrame.createGraphics();
        UiBorderAssets.paintHeroFrame(fighterFrameGraphics, 0, 0, 233, 310,
            1f, Color.RED);
        fighterFrameGraphics.dispose();
        BufferedImage mageFrame = new BufferedImage(233, 310,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D mageFrameGraphics = mageFrame.createGraphics();
        UiBorderAssets.paintHeroFrame(mageFrameGraphics, 0, 0, 233, 310,
            1f, Color.BLUE);
        mageFrameGraphics.dispose();
        if (!imagesDiffer(fighterFrame, mageFrame)) {
            throw new AssertionError(
                "Hero frame inlays must accept the exact runtime tint mask");
        }
        for (String race : GameEngine.RACES) {
            for (String heroClass : GameEngine.CLASSES) {
                HeroVisualTheme theme = HeroVisualTheme.forBuild(race, heroClass);
                if (!race.equals(theme.race()) ||
                        !heroClass.equals(theme.heroClass()) ||
                        theme.classAccent() == null ||
                        theme.raceAccent() == null ||
                        theme.metalAccent() == null ||
                        theme.resourceColor() == null ||
                        theme.raceMotif() == null ||
                        theme.classOverlay(22).getAlpha() != 22 ||
                        theme.raceOverlay(18).getAlpha() != 18) {
                    throw new AssertionError(
                        "Every build must resolve complete modular creation-theme inputs");
                }
            }
        }
        AudioInputStream titleMusic = AudioSystem.getAudioInputStream(
            EnhancedUiSmokeTest.class.getResource("/assets/audio/title-theme.wav"));
        if (titleMusic.getFrameLength() <= 0) {
            throw new AssertionError("Bundled title music is not a readable audio stream");
        }
        titleMusic.close();
        for (SoundManager.Music music : SoundManager.Music.values()) {
            if (music == SoundManager.Music.NONE) continue;
            String resource = SoundManager.musicResource(music);
            AudioInputStream sceneMusic = AudioSystem.getAudioInputStream(
                EnhancedUiSmokeTest.class.getResource(resource));
            if (sceneMusic.getFrameLength() <= 0) {
                throw new AssertionError("Unreadable scene music: " + resource);
            }
            sceneMusic.close();
        }
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
        panel.setPlayerNameForTest("AelindraTheStormweaverXXYYZZ");
        if (panel.playerNameForTest().length() != GameEngine.MAX_PLAYER_NAME_LENGTH ||
                !panel.playerNameForTest().equals(panel.displayedProfileNameForTest())) {
            throw new AssertionError(
                "Player names must stop at the profile-safe limit and update the grand title");
        }
        if (countPortraitFrames(panel) != 1) {
            throw new AssertionError(
                "Character creation must show one framed hero and no miniature portraits");
        }
        for (String race : GameEngine.RACES) {
            String resource = "/assets/race-crests/" + race.toLowerCase() + ".png";
            if (EnhancedUiSmokeTest.class.getResource(resource) == null) {
                throw new AssertionError("Missing race crest: " + resource);
            }
            BufferedImage crest = ImageIO.read(
                EnhancedUiSmokeTest.class.getResource(resource));
            if (crest == null || !crest.getColorModel().hasAlpha() ||
                    ((crest.getRGB(0, 0) >>> 24) & 255) != 0) {
                throw new AssertionError(
                    "Race crests must have a transparent surrounding canvas: " + resource);
            }
        }
        for (int index = 0; index < 12; index++) {
            String race = GameEngine.RACES[index % GameEngine.RACES.length];
            String heroClass = GameEngine.CLASSES[index % GameEngine.CLASSES.length];
            panel.selectBuildForTest(race, heroClass,
                GameEngine.defaultStarterKit(heroClass));
        }
        panel.awaitArtworkForTest();
        if (!"/assets/avatars/full-body/halfling-hunter.png".equals(
                panel.displayedArtworkForTest())) {
            throw new AssertionError("Rapid build switching must settle on the final portrait");
        }
        panel.selectGenderForTest(CharacterArt.MALE);
        panel.awaitArtworkForTest();
        if (!"/assets/avatars/counterpart/full-body/halfling-hunter.png".equals(
                panel.displayedArtworkForTest())) {
            throw new AssertionError(
                "Gender choice must load the authored counterpart without changing the build");
        }
        for (String race : GameEngine.RACES) {
            for (String heroClass : GameEngine.CLASSES) {
                String counterpartGender = CharacterArt.MALE.equals(
                    CharacterArt.defaultGender(race, heroClass))
                    ? CharacterArt.FEMALE : CharacterArt.MALE;
                if (EnhancedUiSmokeTest.class.getResource(
                        CharacterArt.portrait(race, heroClass, counterpartGender)) == null ||
                    EnhancedUiSmokeTest.class.getResource(
                        CharacterArt.fullBody(race, heroClass, counterpartGender)) == null) {
                    throw new AssertionError(
                        "Missing gender counterpart art for " + race + " " + heroClass);
                }
            }
        }
        if (!CharacterArt.FEMALE.equals(
                CharacterArt.defaultGender("Dwarf", "Rogue")) ||
                !"/assets/avatars/full-body/dwarf-rogue.png".equals(
                    CharacterArt.fullBody("Dwarf", "Rogue", CharacterArt.FEMALE)) ||
                !"/assets/avatars/counterpart/full-body/dwarf-rogue.png".equals(
                    CharacterArt.fullBody("Dwarf", "Rogue", CharacterArt.MALE))) {
            throw new AssertionError(
                "Dwarf Rogue female and male artwork assignments must not be reversed");
        }
        BufferedImage dwarfRogueFemale = ImageIO.read(
            EnhancedUiSmokeTest.class.getResource(
                CharacterArt.fullBody("Dwarf", "Rogue", CharacterArt.FEMALE)));
        BufferedImage dwarfRogueMale = ImageIO.read(
            EnhancedUiSmokeTest.class.getResource(
                CharacterArt.fullBody("Dwarf", "Rogue", CharacterArt.MALE)));
        if (!imagesDiffer(dwarfRogueFemale, dwarfRogueMale)) {
            throw new AssertionError(
                "Dwarf Rogue gender choices must resolve distinct authored artwork");
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
        File largeCharacterOutput = render(panel, 1920, 1080, output.getParentFile(),
            "character-creation-large-preview.png", 100000L);
        File compactCharacterOutput = render(panel, 1280, 720, output.getParentFile(),
            "character-creation-1280x720-preview.png", 70000L);
        JPanel frameIdentifiers = new JPanel(new GridLayout(1, 4, 12, 0));
        frameIdentifiers.setBackground(UiTheme.BACKGROUND);
        for (String heroClass : GameEngine.CLASSES) {
            AssetImagePanel portrait = new AssetImagePanel(
                "/assets/avatars/elf-" + heroClass.toLowerCase() + ".png", true);
            portrait.setBorder(new FantasyPortraitBorder("Elf", heroClass, true));
            if (!portrait.usesMaskedFantasyViewportForTest()) {
                throw new AssertionError("Fantasy portraits must use a masked frame viewport");
            }
            frameIdentifiers.add(portrait);
        }
        File frameIdentifiersOutput = render(frameIdentifiers, 720, 180,
            output.getParentFile(), "frame-identifiers-preview.png", 20000L);
        JPanel heroFrameVariants = new JPanel(new GridLayout(1, 4, 16, 0));
        heroFrameVariants.setBackground(UiTheme.BACKGROUND);
        for (String heroClass : GameEngine.CLASSES) {
            AssetImagePanel portrait = new AssetImagePanel(
                "/assets/avatars/full-body/elf-" +
                    heroClass.toLowerCase() + ".png", true);
            portrait.setOpaque(false);
            portrait.setBorder(new FantasyPortraitBorder("Elf", heroClass));
            heroFrameVariants.add(portrait);
        }
        File heroFrameVariantsOutput = render(heroFrameVariants, 1000, 340,
            output.getParentFile(), "hero-frame-variants-preview.png", 50000L);

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
        if (scrollingMap.viewSizeForTest() != GameMapPanel.DETAIL_VIEW_SIZE) {
            throw new AssertionError("Map must open in the 7x7 detail view");
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
        if (scrollingMap.viewSizeForTest() != GameMapPanel.CLOSE_VIEW_SIZE ||
                scrollingMap.viewRowForTest() != 4 || scrollingMap.viewColForTest() != 4) {
            throw new AssertionError("5x5 close zoom must remain centered on the hero");
        }
        File mapDetailOutput = render(scrollingMap, 288, 280,
            output.getParentFile(), "map-detail-preview.png", 8000L);
        scrollingMap.pan(1, 1);
        scrollingMap.zoomOut();
        if (scrollingMap.viewSizeForTest() != GameMapPanel.DETAIL_VIEW_SIZE ||
                scrollingMap.viewRowForTest() != 4 || scrollingMap.viewColForTest() != 4) {
            throw new AssertionError("Zooming a panned map must preserve its inspected center");
        }
        scrollingMap.zoomOut();
        if (scrollingMap.viewSizeForTest() != GameMapPanel.OVERVIEW_VIEW_SIZE ||
                scrollingMap.viewRowForTest() != 3 || scrollingMap.viewColForTest() != 3) {
            throw new AssertionError("Second zoom-out step must enter the 9x9 overview");
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

        final boolean[] titleSettingsOpened = { false };
        JPanel titleScreen = MainWindow.buildTitleScreen(new ActionListener() {
            public void actionPerformed(ActionEvent event) { }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) { }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) { }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) { }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) { }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) { }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) { titleSettingsOpened[0] = true; }
        });
        JButton titleSettings = findAccessibleButton(titleScreen, "settings");
        if (titleSettings == null) {
            throw new AssertionError("Title screen must expose the shared game settings");
        }
        titleSettings.doClick();
        if (!titleSettingsOpened[0]) {
            throw new AssertionError("Title screen Settings control must invoke its action");
        }
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
        if (MainWindow.enemyBeatInterval(false) <= MainWindow.enemyBeatInterval(true) ||
                MainWindow.enemyBeatInitialDelay(false) <= MainWindow.enemyBeatInitialDelay(true) ||
                MainWindow.enemyBeatSettleDelay(false) <= MainWindow.enemyBeatSettleDelay(true)) {
            throw new AssertionError("Reduced motion must preserve sequencing with shorter waits");
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
        previewState.enemyBleedTurns = 2;
        previewState.enemyBleedDamage = 3;
        previewState.enemyArmorBreakTurns = 2;
        previewState.enemyArmorBreakValue = 2;
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
        if (!encounter.hasStatusChipForTest("BLEED 3") ||
                !encounter.hasStatusChipForTest("ARMOR −2")) {
            throw new AssertionError("Persistent combat effects must render as separate status chips");
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
        if (!shortcutPanel.abilityTooltipForTest(2).contains("UNLOCKS LEVEL 2")) {
            throw new AssertionError("Locked ability help must explain its unlock requirement");
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
        if (!shortcutPanel.resourceMeterTextForTest().startsWith("MANA") ||
                !shortcutPanel.resourceMeterHelpForTest().contains("Channel Ward")) {
            throw new AssertionError("Combat quick actions must expose the active class resource");
        }
        if (!"NORMAL".equals(shortcutPanel.attackSpeedLabelForTest()) ||
                !"NORMAL".equals(shortcutPanel.spellSpeedLabelForTest()) ||
                shortcutPanel.timelineForTest().length() == 0) {
            throw new AssertionError("Combat action tempo and turn timeline must be visible");
        }
        String abilityTip = shortcutPanel.abilityTooltipForTest(1);
        String accessibleAbility = shortcutPanel.abilityAccessibleHelpForTest(1);
        if (abilityTip == null || !abilityTip.contains("Cost: 7 MP") ||
                !abilityTip.contains("Tempo: NORMAL") || !abilityTip.contains("Estimated:") ||
                accessibleAbility == null || !accessibleAbility.contains("Effect:")) {
            throw new AssertionError("Ability help must expose cost, tempo, effect, and outcome");
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
        GameEngine fighterShortcutEngine = new GameEngine();
        fighterShortcutEngine.newGame("Shortcut Fighter", "Human", "Fighter", "BREAKER");
        fighterShortcutEngine.getState().level = 3;
        fighterShortcutEngine.getState().weaponProficiency.put("HEAVY BLADE", Integer.valueOf(3));
        shortcutPanel.setEncounter(new GameEngine.Enemy("Skeleton", 24, 9, 10), "",
            fighterShortcutEngine.getState());
        if (shortcutPanel.visibleAbilityCountForTest() != 2 ||
                shortcutPanel.triggerShortcut('5')) {
            throw new AssertionError(
                "Second Wind must remain an armed passive rather than a combat action button");
        }
        if (!shortcutPanel.hasStatusChipForTest("SECOND WIND · ARMED")) {
            throw new AssertionError("Armed fighter death-save status must be visible in combat");
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
        java.awt.Dimension gameplayPortrait = exploration.heroPortraitSizeForTest();
        if (gameplayPortrait.width * 4 != gameplayPortrait.height * 3) {
            throw new AssertionError("Gameplay hero framing must retain a 3:4 portrait ratio");
        }
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
        File largeExplorationOutput = render(exploration, 1920, 1080,
            output.getParentFile(), "exploration-large-preview.png");
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
        if (shell.mapForTest().viewSizeForTest() != GameMapPanel.CLOSE_VIEW_SIZE) {
            throw new AssertionError("Visible map zoom control must enter the 5x5 close view");
        }
        shell.triggerMapZoomShortcut(false);
        if (shell.mapForTest().viewSizeForTest() != GameMapPanel.DETAIL_VIEW_SIZE) {
            throw new AssertionError("Visible map zoom control must restore the 7x7 detail view");
        }
        shell.triggerMapZoomShortcut(false);
        if (shell.mapForTest().viewSizeForTest() != GameMapPanel.OVERVIEW_VIEW_SIZE) {
            throw new AssertionError("Visible map zoom control must reach the 9x9 overview");
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
        soundManager.setMusic(SoundManager.Music.EXPLORATION);
        if (soundManager.requestedMusicForTest() != SoundManager.Music.EXPLORATION) {
            throw new AssertionError("Exploration music state must be retained headlessly");
        }
        soundManager.setMusic(SoundManager.Music.BOSS);
        if (soundManager.requestedMusicForTest() != SoundManager.Music.BOSS) {
            throw new AssertionError("Boss music must replace the previous scene state");
        }
        soundManager.stopMusic();
        if (soundManager.cachedCueCount() != SoundManager.Cue.values().length) {
            throw new AssertionError("Every sound cue must be prepared in the cache");
        }
        if (soundManager.fileBackedCueCount() != 13) {
            throw new AssertionError("The authored RPG effects slice must contain 13 cues");
        }
        for (SoundManager.Cue cue : SoundManager.Cue.values()) {
            String resource = SoundManager.cueResource(cue);
            if (resource == null) continue;
            AudioInputStream authored = AudioSystem.getAudioInputStream(
                EnhancedUiSmokeTest.class.getResource(resource));
            long expectedBytes = authored.getFrameLength() * authored.getFormat().getFrameSize();
            if (expectedBytes <= 0 || soundManager.cachedSampleLengthForTest(cue) != expectedBytes) {
                throw new AssertionError("Authored cue did not replace its fallback: " + cue);
            }
            authored.close();
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
                public void onCredits() { }
                public void onReleaseNotes() { }
                public void onFeedback() { }
                public void onBugReport() { }
            });
        File settingsOutput = render(settings, 620, 620, output.getParentFile(),
            "settings-preview.png");
        if (CreditsCatalog.entries().size() < 9) {
            throw new AssertionError("Credits catalog must retain current music, VFX, and fonts");
        }
        CreditsPanel credits = new CreditsPanel(new Runnable() { public void run() { } });
        File creditsOutput = render(credits, 760, 650, output.getParentFile(),
            "credits-and-licenses-preview.png");
        GameSettingsOverlay creditsOverlay = new GameSettingsOverlay(soundManager,
            new GamePreferences(), null);
        creditsOverlay.showCredits(false);
        if (!creditsOverlay.creditsVisibleForTest()) {
            throw new AssertionError("Credits must open inside the game overlay");
        }
        creditsOverlay.hideSettings();
        ReleaseNotesPanel releaseNotes = new ReleaseNotesPanel(
            new Runnable() { public void run() { } },
            new Runnable() { public void run() { } },
            new Runnable() { public void run() { } });
        File releaseNotesOutput = render(releaseNotes, 780, 660, output.getParentFile(),
            "release-notes-preview.png");
        UpdateService.Release updateRelease = new UpdateService.Release(
            "0.7.0-beta.2", "v0.7.0-beta.2",
            "The Chosen Quest Enhanced — Beta 0.7.0 Update Test",
            "## Highlights\n- Safer updates\n- New interface refinements",
            ProjectLinks.RELEASES, ProjectLinks.RELEASES,
            "sha256:test", true);
        UpdateService.Result updateResult = UpdateService.Result.available(updateRelease);
        UpdatePanel updatePanel = new UpdatePanel(updateResult,
            new Runnable() { public void run() { } },
            new Runnable() { public void run() { } },
            new Runnable() { public void run() { } });
        File updateOutput = render(updatePanel, 720, 540, output.getParentFile(),
            "update-available-preview.png");
        GameEngine.Item previewLoot = new GameEngine.Item("Flanged Mace", "Weapon",
            9, 0, 30, "Fighter", false, "UNCOMMON", false);
        creditsOverlay.setSize(900, 700);
        creditsOverlay.showLootDiscovery(previewLoot, "Iron Mace", 6,
            new Runnable() { public void run() { } },
            new Runnable() { public void run() { } },
            new Runnable() { public void run() { } });
        if (findButtonByText(creditsOverlay, "EQUIP NOW") == null ||
                findButtonByText(creditsOverlay, "VIEW INVENTORY") == null) {
            throw new AssertionError(
                "Upgrade loot must offer quick equip and direct Inventory access");
        }
        File lootOutput = render(creditsOverlay, 900, 700, output.getParentFile(),
            "loot-discovery-preview.png", 18000L);
        creditsOverlay.hideSettings();
        GameEngine.Item previewRelicReward = new GameEngine.Item(
            "Moonsteel Blade", "Weapon", 11, 0, 0, "Fighter", true);
        creditsOverlay.showRelicAttuned(previewRelicReward, "Iron Mace", 6,
            new Runnable() { public void run() { } },
            new Runnable() { public void run() { } },
            new Runnable() { public void run() { } });
        if (findButtonByText(creditsOverlay, "EQUIP NOW") == null ||
                findButtonByText(creditsOverlay, "VIEW INVENTORY") == null) {
            throw new AssertionError(
                "Attuned relic rewards must wait for an explicit equip choice");
        }
        creditsOverlay.hideSettings();
        creditsOverlay.showReleaseNotes(false, null);
        if (!creditsOverlay.releaseNotesVisibleForTest()) {
            throw new AssertionError("Release notes must open inside the game overlay");
        }
        creditsOverlay.hideSettings();
        creditsOverlay.showUpdateResultForTest(updateResult);
        if (!creditsOverlay.updateVisibleForTest()) {
            throw new AssertionError("Updates must open inside the game overlay");
        }
        JButton viewChanges = findButtonByText(creditsOverlay, "VIEW CHANGES");
        if (viewChanges == null) {
            throw new AssertionError("Update results must offer an in-game change review");
        }
        viewChanges.doClick();
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() { }
        });
        if (!creditsOverlay.releaseNotesVisibleForTest()) {
            throw new AssertionError(
                "View Changes must remain in-game instead of opening a fragile system browser");
        }
        creditsOverlay.hideSettings();
        creditsOverlay.setSize(1440, 900);
        creditsOverlay.showWorldMap(mapEngine);
        if (!creditsOverlay.worldMapVisibleForTest() ||
                creditsOverlay.worldMapViewSizeForTest() != GameEngine.SIZE) {
            throw new AssertionError("World map overlay must display the complete logical map");
        }
        File worldMapOutput = render(creditsOverlay, 1440, 900,
            output.getParentFile(), "world-map-preview.png", 30000L);
        creditsOverlay.closeWorldMap();
        soundManager.shutdown();

        JPanel buttonStates = new JPanel(new GridLayout(5, 5, 12, 12));
        buttonStates.setBackground(UiTheme.BACKGROUND);
        buttonStates.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 24, 24, 24));
        for (int style = 0; style < 5; style++) {
            for (int state = 0; state < 5; state++) {
                JButton stateButton = UiTheme.button(buttonStateName(state), style == 0);
                if (style == 2) {
                    UiTheme.applyButtonStyle(stateButton, UiTheme.ButtonStyle.DANGER, 10, 20);
                } else if (style == 3) {
                    UiTheme.applyButtonStyle(stateButton,
                        UiTheme.ButtonStyle.DECO_PRIMARY, 10, 20);
                    stateButton.putClientProperty(
                        "thechosenquest.button.accent", UiTheme.GREEN);
                    stateButton.setFont(UiTheme.displayBold(15));
                } else if (style == 4) {
                    UiTheme.applyButtonStyle(stateButton,
                        UiTheme.ButtonStyle.DECO_SECONDARY, 10, 20);
                    stateButton.putClientProperty(
                        "thechosenquest.button.accent", UiTheme.GREEN);
                    stateButton.setFont(UiTheme.displayBold(15));
                }
                configureButtonState(stateButton, state);
                buttonStates.add(stateButton);
            }
        }
        File buttonStatesOutput = render(buttonStates, 900, 390, output.getParentFile(),
            "button-states-preview.png", 8000L);

        System.out.println("Enhanced UI smoke tests passed: " + titleOutput.getPath() + ", " +
            output.getPath() + ", " + largeCharacterOutput.getPath() + ", " +
            frameIdentifiersOutput.getPath() + ", " +
            heroFrameVariantsOutput.getPath() + ", " +
            mapViewportOutput.getPath() + ", " + mapDetailOutput.getPath() + ", " +
            explorationOutput.getPath() + ", " + largeExplorationOutput.getPath() + ", " +
            encounterOutput.getPath() + ", " +
            inventoryOutput.getPath() + " and " +
            locationOutput.getPath() + ", " + tavernOutput.getPath() + ", " +
            victoryOutput.getPath() + ", " + defeatOutput.getPath() + ", " +
            shellCombat.getPath() + ", " + shellInventoryOutput.getPath() + ", " +
            shellShop.getPath() + ", " + shellBlacksmith.getPath() + ", " +
            shellAlchemist.getPath() + ", " + shellTavern.getPath() + ", " +
            shellSpiderNest.getPath() + ", " +
            settingsOutput.getPath() + ", " + creditsOutput.getPath() + ", " +
            releaseNotesOutput.getPath() + ", " +
            updateOutput.getPath() + ", " +
            lootOutput.getPath() + ", " +
            worldMapOutput.getPath() + ", " +
            buttonStatesOutput.getPath());
    }

    private static boolean imagesDiffer(BufferedImage first, BufferedImage second) {
        if (first.getWidth() != second.getWidth() ||
                first.getHeight() != second.getHeight()) return true;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) return true;
            }
        }
        return false;
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

    private static JButton findButtonByText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton &&
                    text.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton nested = findButtonByText((Container) component, text);
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
