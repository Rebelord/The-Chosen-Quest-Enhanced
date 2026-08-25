package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Coordinates the rules engine, Swing views, sound, and presentation timing.
 * GameEngine resolves actions synchronously; this class sequences the resulting
 * events so animation and audio never change the underlying combat outcome.
 */
final class MainWindow {
    private static final Color INK = new Color(239, 229, 205);
    private static final Color MUTED = new Color(190, 181, 160);
    private static final Color PANEL = new Color(30, 32, 37);
    private static final Color GOLD = new Color(214, 169, 70);
    private static final Color RED = new Color(170, 62, 52);
    private static final String TITLE_CARD = "title";
    private static final String CREATE_CARD = "create";
    private static final String GAME_CARD = "game";
    private static final String STORY_CARD = "story";
    private static final String ENCOUNTER_CARD = "encounter";
    private static final String INVENTORY_CARD = "inventory";
    private static final String LOCATION_CARD = "location";
    private static final String OUTCOME_CARD = "outcome";
    private static final int ENEMY_BEAT_INTERVAL_MS = 680;
    private static final int ENEMY_BEAT_INITIAL_DELAY_MS = 280;
    private static final int ENEMY_BEAT_SETTLE_MS = 540;

    static int enemyBeatInterval(boolean reducedMotion) {
        return reducedMotion ? 360 : ENEMY_BEAT_INTERVAL_MS;
    }

    static int enemyBeatInitialDelay(boolean reducedMotion) {
        return reducedMotion ? 140 : ENEMY_BEAT_INITIAL_DELAY_MS;
    }

    static int enemyBeatSettleDelay(boolean reducedMotion) {
        return reducedMotion ? 220 : ENEMY_BEAT_SETTLE_MS;
    }

    private final GameEngine engine = new GameEngine();
    private final SoundManager soundManager = new SoundManager();
    private final GamePreferences preferences = new GamePreferences();
    private final JFrame frame = new JFrame("The Chosen Quest — Enhanced Edition");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final CardLayout stageLayout = new CardLayout();
    private final JPanel stage = new JPanel(stageLayout);
    private final JTextArea story = new JTextArea();
    private final EncounterPanel encounterPanel = new EncounterPanel(new EncounterPanel.Listener() {
        public void onAttack() {
            performPlayerAttack(null);
        }

        public void onDefend() {
            soundManager.play(SoundManager.Cue.DEFEND);
            engine.defend();
            encounterPanel.setCombatActionsEnabled(false);
            encounterPanel.playDefend(new Runnable() {
                public void run() { refresh(); }
            });
        }

        public void onAbility(int slot) { performAbility(slot); }

        public void onPotion() {
            int health = engine.getState().health;
            engine.usePotion();
            soundManager.play(engine.getState().health > health ? SoundManager.Cue.HEAL :
                SoundManager.Cue.ERROR);
            refresh();
        }

        public void onFlee() {
            soundManager.play(SoundManager.Cue.FLEE);
            engine.flee();
            refresh();
        }
    });
    private final InventoryPanel inventoryPanel = new InventoryPanel(new InventoryPanel.Listener() {
        public void onEquip(GameEngine.Item item) {
            String weapon = engine.getState().equippedWeapon;
            String armour = engine.getState().equippedArmour;
            String offhand = engine.getState().equippedOffhand;
            engine.equipItem(item);
            String styleNotice = engine.consumeCombatStyleNotice();
            GameEngine.State state = engine.getState();
            soundManager.play(equalsText(weapon, state.equippedWeapon) &&
                equalsText(armour, state.equippedArmour) &&
                equalsText(offhand, state.equippedOffhand) ? SoundManager.Cue.ERROR :
                (item.relicReward ? SoundManager.Cue.RELIC_EQUIPPED :
                    SoundManager.Cue.EQUIP));
            refresh();
            showCombatStyleNotice(styleNotice);
        }

        public void onBack() { showStory(); }
    });
    private final LocationPanel locationPanel = new LocationPanel(new LocationPanel.Listener() {
        public void onBuy(Object selection) {
            int gold = engine.getState().gold;
            if (selection instanceof GameEngine.Item) {
                final GameEngine.Item item = (GameEngine.Item) selection;
                GameEngine.State before = engine.getState();
                final String equippedName = "Weapon".equals(item.type)
                    ? before.equippedWeapon : ("Offhand".equals(item.type)
                        ? before.equippedOffhand : before.equippedArmour);
                final int equippedValue = equippedValue(before, item.type);
                engine.buyItem(item);
                boolean purchased = engine.getState().gold < gold;
                soundManager.play(purchased ? SoundManager.Cue.PURCHASE :
                    SoundManager.Cue.ERROR);
                refresh();
                if (purchased) {
                    settingsOverlay.showPurchaseDiscovery(item, equippedName,
                        equippedValue, quickEquipAction(item), new Runnable() {
                            public void run() { showInventory(); }
                        }, new Runnable() {
                            public void run() { refresh(); }
                        });
                }
                return;
            } else if (selection instanceof GameEngine.Relic) {
                GameEngine.Relic relic = (GameEngine.Relic) selection;
                GameEngine.State before = engine.getState();
                String previousWeapon = before.equippedWeapon;
                String previousArmour = before.equippedArmour;
                int previousWeaponValue = equippedValue(before, "Weapon");
                int previousArmourValue = equippedValue(before, "Armour");
                int identified = engine.identifiedRelicCount();
                engine.identifyRelic(relic);
                boolean succeeded = engine.identifiedRelicCount() > identified;
                soundManager.play(succeeded ? SoundManager.Cue.RELIC_IDENTIFIED : SoundManager.Cue.ERROR);
                refresh();
                if (succeeded) {
                    final GameEngine.Item reward = inventoryItem(relic.rewardName);
                    if (reward != null) {
                        settingsOverlay.showRelicAttuned(reward,
                            "Weapon".equals(reward.type) ? previousWeapon : previousArmour,
                            "Weapon".equals(reward.type) ? previousWeaponValue : previousArmourValue,
                            quickEquipAction(reward, SoundManager.Cue.RELIC_EQUIPPED),
                            new Runnable() {
                                public void run() { showInventory(); }
                            }, new Runnable() {
                                public void run() { refresh(); }
                            });
                    }
                }
                return;
            } else {
                engine.buyPotion();
            }
            soundManager.play(engine.getState().gold < gold ? SoundManager.Cue.PURCHASE :
                SoundManager.Cue.ERROR);
            refresh();
        }

        public void onSell(GameEngine.Item item) {
            int gold = engine.getState().gold;
            engine.sellItem(item);
            soundManager.play(engine.getState().gold > gold ? SoundManager.Cue.PURCHASE :
                SoundManager.Cue.ERROR);
            refresh();
        }

        public void onRest() {
            int health = engine.getState().health;
            int mana = engine.getState().mana;
            engine.locationAction();
            GameEngine.State state = engine.getState();
            soundManager.play(state.health > health || state.mana > mana ? SoundManager.Cue.REST :
                SoundManager.Cue.UI_CONFIRM);
            refresh();
        }

        public void onMapService(GameEngine.MapService service) {
            int gold = engine.getState().gold;
            String before = engine.getHistory();
            engine.useMapService(service);
            boolean purchased = engine.getState().gold < gold;
            String after = engine.getHistory();
            boolean changed = !before.equals(after);
            soundManager.play(purchased ? SoundManager.Cue.PURCHASE :
                (changed ? SoundManager.Cue.UI_CONFIRM : SoundManager.Cue.ERROR));
            refresh();
            if (service == GameEngine.MapService.RUMOR && changed &&
                    engine.currentEnemy() == null) {
                boolean tavern = engine.currentTile() == GameEngine.TileType.TAVERN;
                settingsOverlay.showDialogue(
                    tavern ? "/assets/encounters/npcs/innkeeper.png" :
                        "/assets/encounters/npcs/alchemist.png",
                    tavern ? "Bram" : "Sylara",
                    tavern ? "INNKEEPER · LOCAL RUMOR" : "ALCHEMIST · ARCANE ADVICE",
                    dialogueCopy(after.substring(Math.min(before.length(), after.length()))),
                    tavern ? new Color(173, 115, 65) : new Color(76, 139, 96),
                    new Runnable() { public void run() { refresh(); } });
            }
        }

        public void onBack() { showStory(); }
    });
    private final OutcomePanel outcomePanel = new OutcomePanel(new OutcomePanel.Listener() {
        public void onNewQuest() { showCharacterCreation(); }
        public void onTitleScreen() { showTitleScreen(); }
    });
    private final GameMapPanel map = new GameMapPanel(engine);
    private final JLabel stats = new JLabel();
    private final JLabel location = new JLabel();
    private final JLabel enemy = new JLabel();
    private final JButton locationAction = new JButton();
    private final List<JButton> movementButtons = new ArrayList<JButton>();
    private JButton attackAction;
    private JButton defendAction;
    private JButton fleeAction;
    private JButton potionAction;
    private JButton spellAction;
    private JButton inventoryAction;
    private final JProgressBar healthBar = meter(new Color(151, 54, 48));
    private final JProgressBar manaBar = meter(new Color(54, 91, 151));
    private final JProgressBar experienceBar = meter(new Color(153, 119, 47));
    private JScrollPane storyScroll;
    private EnhancedExplorationPanel enhancedExploration;
    private boolean gameVisible;
    private String stageMode = STORY_CARD;
    private GameEngine.Enemy previousFoe;
    private int previousEnemyHealth = -1;
    private int previousPlayerHealth = -1;
    private boolean combatSequencePlaying;
    private final KeyRepeatGuard movementKeyGuard = new KeyRepeatGuard();
    private final GameSettingsOverlay settingsOverlay;

    MainWindow() {
        settingsOverlay = new GameSettingsOverlay(soundManager, preferences, new Runnable() {
            public void run() {
                if (enhancedExploration != null) {
                    enhancedExploration.setMuted(soundManager.isMuted());
                }
                encounterPanel.setReducedMotion(preferences.isReducedMotion());
            }
        });
        encounterPanel.setReducedMotion(preferences.isReducedMotion());
        buildWindow();
        refresh();
    }

    private void performPlayerAttack(String spell) {
        boolean magical = spell != null;
        final GameEngine.Enemy target = engine.currentEnemy();
        if (target == null) {
            soundManager.play(SoundManager.Cue.ERROR);
            refresh();
            return;
        }
        int historyStart = engine.getHistory().length();
        int healthBefore = target.health;
        int manaBefore = engine.getState().mana;
        String preparedSpell = spell;
        if (magical) {
            engine.castSpell(spell);
        } else {
            engine.attack();
        }
        int healthAfter = target.health;
        String history = engine.getHistory();
        String result = history.substring(Math.min(historyStart, history.length()));
        if (healthAfter >= healthBefore) {
            soundManager.play(SoundManager.Cue.ERROR);
            refresh();
            return;
        }
        boolean critical = result.contains("Critical strike") ||
            result.contains("Precision shot");
        EncounterPanel.PlayerAttackStyle style =
            EncounterPanel.PlayerAttackStyle.forAction(
                engine.getState().heroClass, preparedSpell);
        soundManager.play(magical && engine.getState().mana < manaBefore
            ? SoundManager.Cue.SPELL : SoundManager.Cue.ATTACK);
        encounterPanel.setCombatActionsEnabled(false);
        // GameEngine resolves the action first, but the UI defers health and
        // impact feedback to the contact frame. This keeps rules deterministic
        // while preventing damage from appearing before the attack lands.
        encounterPanel.playPlayerAttack(healthBefore, healthAfter, style, critical,
            new Runnable() {
                public void run() { soundManager.play(SoundManager.Cue.ENEMY_HIT); }
            }, new Runnable() {
                public void run() { refresh(); }
            });
    }

    private void performAbility(int slot) {
        final GameEngine.Enemy target = engine.currentEnemy();
        if (target == null) {
            soundManager.play(SoundManager.Cue.ERROR);
            refresh();
            return;
        }
        GameEngine.State state = engine.getState();
        String ability = GameEngine.abilityName(state, slot);
        int healthBefore = target.health;
        int playerHealthBefore = state.health;
        int manaBefore = state.mana;
        engine.useAbility(slot);
        int healthAfter = target.health;
        if (healthAfter < healthBefore) {
            EncounterPanel.PlayerAttackStyle style =
                EncounterPanel.PlayerAttackStyle.forAbility(state.heroClass, ability);
            soundManager.play(cueForAbility(state.heroClass, ability,
                state.mana < manaBefore));
            encounterPanel.setCombatActionsEnabled(false);
            encounterPanel.playPlayerAttack(healthBefore, healthAfter, style, false,
                new Runnable() {
                    public void run() { soundManager.play(SoundManager.Cue.ENEMY_HIT); }
                }, new Runnable() {
                    public void run() { refresh(); }
                });
        } else {
            soundManager.play(state.health > playerHealthBefore ? SoundManager.Cue.HEAL :
                cueForAbility(state.heroClass, ability, state.mana < manaBefore));
            refresh();
        }
    }

    private SoundManager.Cue cueForAbility(String heroClass, String ability,
                                           boolean spentMana) {
        if ("Mage".equals(heroClass)) return spentMana
            ? ("Fireball".equals(ability) ? SoundManager.Cue.FIREBALL :
                ("Ice Spike".equals(ability) ? SoundManager.Cue.ICE_SPIKE :
                    SoundManager.Cue.MAGIC_MISSILE))
            : SoundManager.Cue.ERROR;
        if ("Shield Bash".equals(ability) || "Stunning Blow".equals(ability))
            return SoundManager.Cue.SHIELD_BASH;
        if ("Cleave".equals(ability) || "Power Strike".equals(ability) ||
                "Armor Breaker".equals(ability) || "Piercing Bolt".equals(ability))
            return SoundManager.Cue.HEAVY_STRIKE;
        if ("Offhand Strike".equals(ability) || "Blade Flurry".equals(ability))
            return SoundManager.Cue.ROGUE_STRIKE;
        if ("Pinning Shot".equals(ability) || "Volley".equals(ability))
            return SoundManager.Cue.PINNING_SHOT;
        if ("Execute".equals(ability)) return SoundManager.Cue.EXECUTE;
        if ("Hunter's Mark".equals(ability)) return SoundManager.Cue.HUNTERS_MARK;
        return SoundManager.Cue.WEAPON_MASTERY;
    }

    void show() {
        frame.setVisible(true);
        soundManager.playTitleMusic();
        if (preferences.shouldShowReleaseNotes(AppVersion.VERSION)) {
            Timer notesDelay = new Timer(350, new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    settingsOverlay.showReleaseNotes(false, new Runnable() {
                        public void run() {
                            preferences.markReleaseNotesSeen(AppVersion.VERSION);
                            Timer updateDelay = new Timer(450,
                                new ActionListener() {
                                    public void actionPerformed(ActionEvent event) {
                                        settingsOverlay.checkForUpdatesQuietly();
                                    }
                                });
                            updateDelay.setRepeats(false);
                            updateDelay.start();
                        }
                    });
                }
            });
            notesDelay.setRepeats(false);
            notesDelay.start();
        } else {
            // A short delay keeps network setup out of the initial paint path.
            // The worker remains silent when offline or already current.
            Timer updateDelay = new Timer(900, new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    settingsOverlay.checkForUpdatesQuietly();
                }
            });
            updateDelay.setRepeats(false);
            updateDelay.start();
        }
    }

    private void buildWindow() {
        java.net.URL appIcon = MainWindow.class.getResource("/assets/app-icon.png");
        if (appIcon != null) frame.setIconImage(new ImageIcon(appIcon).getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1100, 720));
        frame.setSize(UiTheme.SHELL_WIDTH, UiTheme.SHELL_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.addWindowFocusListener(new WindowAdapter() {
            public void windowLostFocus(WindowEvent event) {
                movementKeyGuard.clear();
                soundManager.setFocusSuspended(true);
            }
            public void windowGainedFocus(WindowEvent event) {
                soundManager.setFocusSuspended(false);
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent event) { soundManager.shutdown(); }
        });
        installMovementKeys();

        cards.add(buildTitleScreen(), TITLE_CARD);
        cards.add(new CharacterCreationPanel(new CharacterCreationPanel.Listener() {
            public void onBegin(String name, String race, String heroClass) {
                onBegin(name, race, heroClass, GameEngine.defaultStarterKit(heroClass));
            }

            public void onBegin(String name, String race, String heroClass, String starterKit) {
                onBegin(name, race, heroClass, starterKit,
                    CharacterArt.defaultGender(race, heroClass));
            }

            public void onBegin(String name, String race, String heroClass, String starterKit,
                                String gender) {
                soundManager.play(SoundManager.Cue.ADVENTURE_BEGIN);
                engine.newGame(name, race, heroClass, starterKit, gender);
                showGame();
            }

            public void onBack() {
                showTitleScreen();
            }

            public void onRandomizeName() {
                soundManager.play(SoundManager.Cue.DICE_ROLL);
            }

            public void onRaceSelected(String race) {
                soundManager.playSelection(SoundManager.cueForRace(race));
            }

            public void onClassSelected(String heroClass) {
                soundManager.playSelection(SoundManager.cueForClass(heroClass));
            }

            public void onCombatPathSelected(String path) {
                soundManager.playSelection(SoundManager.Cue.PATH_SELECT);
            }

            public void onGenderSelected(String gender) {
                soundManager.playSelection(SoundManager.Cue.UI_CONFIRM);
            }
        }), CREATE_CARD);
        enhancedExploration = new EnhancedExplorationPanel(engine,
            new EnhancedExplorationPanel.Listener() {
                public void onInventory() { showInventory(); }
                public void onSpellbook() { cyclePreparedSpell(); }
                public void onShop() { showShop(); }
                public void onHaven() { showHaven(); }
                public void onSave() { chooseSave(); }
                public void onLoad() { chooseLoad(); }
                public void onNewQuest() { showCharacterCreation(); }
                public void onSettings() { settingsOverlay.showSettings(); }
                public boolean onToggleMute() {
                    boolean muted = soundManager.toggleMuted();
                    if (!muted) soundManager.play(SoundManager.Cue.UI_CONFIRM);
                    return muted;
                }
                public void onSound(SoundManager.Cue cue) { soundManager.play(cue); }
                public void onStateChanged() { refresh(); }
            });
        enhancedExploration.setMuted(soundManager.isMuted());
        enhancedExploration.setAuxiliaryPanels(encounterPanel, inventoryPanel,
            locationPanel, outcomePanel);
        cards.add(enhancedExploration, GAME_CARD);
        frame.setContentPane(cards);
        frame.setGlassPane(settingsOverlay);
        cardLayout.show(cards, TITLE_CARD);
    }

    private JPanel buildTitleScreen() {
        return buildTitleScreen(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                soundManager.play(SoundManager.Cue.UI_CONFIRM);
                showCharacterCreation();
            }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (chooseLoad()) showGame();
            }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                soundManager.play(SoundManager.Cue.UI_CONFIRM);
                settingsOverlay.showCredits(false);
            }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                soundManager.play(SoundManager.Cue.UI_CONFIRM);
                settingsOverlay.showReleaseNotes(false, null);
            }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                soundManager.play(SoundManager.Cue.UI_CONFIRM);
                ProjectLinks.open(ProjectLinks.FEEDBACK);
            }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                soundManager.play(SoundManager.Cue.UI_CONFIRM);
                ProjectLinks.open(ProjectLinks.BUG_REPORT);
            }
        }, new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                soundManager.play(SoundManager.Cue.UI_CONFIRM);
                settingsOverlay.showSettings();
            }
        });
    }

    static JPanel buildTitleScreen(ActionListener beginAction, ActionListener loadAction) {
        return buildTitleScreen(beginAction, loadAction, null);
    }

    static JPanel buildTitleScreen(ActionListener beginAction, ActionListener loadAction,
                                   ActionListener creditsAction) {
        return buildTitleScreen(beginAction, loadAction, creditsAction, null, null, null);
    }

    static JPanel buildTitleScreen(ActionListener beginAction, ActionListener loadAction,
                                   ActionListener creditsAction, ActionListener notesAction,
                                   ActionListener feedbackAction, ActionListener bugAction) {
        return buildTitleScreen(beginAction, loadAction, creditsAction, notesAction,
            feedbackAction, bugAction, null);
    }

    static JPanel buildTitleScreen(ActionListener beginAction, ActionListener loadAction,
                                   ActionListener creditsAction, ActionListener notesAction,
                                   ActionListener feedbackAction, ActionListener bugAction,
                                   ActionListener settingsAction) {
        BackgroundPanel root = new BackgroundPanel("/assets/title-screen.png", 0.0f, true);
        root.setLayout(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(0, 36, 0, 36));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new javax.swing.BoxLayout(titleBlock,
            javax.swing.BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.setPreferredSize(new Dimension(936, 186));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.56;
        c.fill = GridBagConstraints.BOTH;
        JPanel topSpacer = new JPanel();
        topSpacer.setOpaque(false);
        root.add(topSpacer, c);

        if (MainWindow.class.getResource("/assets/title-logo.png") != null) {
            AssetImagePanel logo = new AssetImagePanel("/assets/title-logo.png", false);
            logo.setPreferredSize(new Dimension(840, 127));
            logo.setMinimumSize(new Dimension(840, 127));
            logo.setMaximumSize(new Dimension(840, 127));
            logo.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            logo.setOpaque(false);
            titleBlock.add(logo);
        } else {
            JLabel title = new JLabel("THE CHOSEN QUEST");
            title.setForeground(UiTheme.GOLD_LIGHT);
            title.setFont(UiTheme.title(Font.PLAIN, 72));
            title.setAlignmentX(JComponent.CENTER_ALIGNMENT);
            titleBlock.add(title);
        }
        titleBlock.add(javax.swing.Box.createVerticalStrut(16));
        JLabel subtitle = new JLabel("A Retro Fantasy Adventure");
        subtitle.setForeground(UiTheme.GOLD_LIGHT);
        subtitle.setFont(UiTheme.displayBold(32));
        subtitle.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        titleBlock.add(subtitle);

        c.gridy = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        root.add(titleBlock, c);

        c.gridy = 2;
        c.weighty = 0.44;
        c.fill = GridBagConstraints.BOTH;
        JPanel middleSpacer = new JPanel();
        middleSpacer.setOpaque(false);
        root.add(middleSpacer, c);

        JPanel actions = new JPanel();
        actions.setLayout(new javax.swing.BoxLayout(actions, javax.swing.BoxLayout.Y_AXIS));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(360, 154));
        JButton begin = titleButton("BEGIN A NEW QUEST", true);
        begin.addActionListener(beginAction);
        actions.add(begin);
        actions.add(javax.swing.Box.createVerticalStrut(20));
        JButton load = titleButton("LOAD A SAVED QUEST", false);
        load.addActionListener(loadAction);
        actions.add(load);

        c.gridy = 3;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        root.add(actions, c);

        c.gridy = 4;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        JPanel bottomSpacer = new JPanel();
        bottomSpacer.setOpaque(false);
        bottomSpacer.setPreferredSize(new Dimension(1, 64));
        root.add(bottomSpacer, c);

        JPanel footer = new JPanel(new BorderLayout(18, 0));
        footer.setOpaque(false);
        footer.setPreferredSize(new Dimension(1368, 34));
        JPanel footerLinks = new JPanel(new java.awt.FlowLayout(
            java.awt.FlowLayout.LEFT, 16, 0));
        footerLinks.setOpaque(false);
        JButton settings = footerButton("SETTINGS", "Audio, display, and update settings",
            settingsAction);
        settings.setIcon(new SystemIcon(SystemIcon.Type.SETTINGS, 15,
            new Color(220, 205, 169)));
        settings.setIconTextGap(6);
        footerLinks.add(settings);
        footerLinks.add(footerButton("WHAT'S NEW", "View current release notes", notesAction));
        footerLinks.add(footerButton("FEEDBACK", ProjectLinks.FEEDBACK, feedbackAction));
        footerLinks.add(footerButton("REPORT A BUG", ProjectLinks.BUG_REPORT, bugAction));
        footerLinks.add(footerButton("CREDITS & LICENSES",
            "View contributors, asset sources, and licenses", creditsAction));
        footer.add(footerLinks, BorderLayout.WEST);

        JLabel version = new JLabel(AppVersion.DISPLAY_NAME + "  •  PLAYTEST BUILD");
        version.setForeground(new Color(190, 181, 160, 210));
        version.setFont(UiTheme.body(Font.BOLD, 13));
        version.getAccessibleContext().setAccessibleName(
            "The Chosen Quest Enhanced " + AppVersion.DISPLAY_NAME + " playtest build");
        footer.add(version, BorderLayout.EAST);
        c.gridy = 5;
        c.anchor = GridBagConstraints.SOUTH;
        c.fill = GridBagConstraints.NONE;
        root.add(footer, c);
        return root;
    }

    private static JButton footerButton(String text, String tooltip,
                                        ActionListener action) {
        JButton button = new JButton(text);
        button.setForeground(new Color(220, 205, 169));
        button.setFont(UiTheme.body(Font.BOLD, 11));
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(text.toLowerCase());
        if (action != null) button.addActionListener(action);
        else button.setEnabled(false);
        return button;
    }

    private static JButton titleButton(String text, final boolean primary) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(java.awt.Graphics graphics) {
                super.paintComponent(graphics);
                java.awt.FontMetrics metrics = graphics.getFontMetrics(getFont());
                int textWidth = metrics.stringWidth(getText());
                boolean pressed = getModel().isArmed() && getModel().isPressed();
                int faceOffset = pressed ? 0 : -3;
                if (primary) {
                    SystemIcon star = new SystemIcon(SystemIcon.Type.STAR, 28, UiTheme.GOLD);
                    int left = (getWidth() - textWidth) / 2 - 46;
                    int y = (getHeight() - 28) / 2 + faceOffset;
                    star.paintIcon(this, graphics, left, y);
                    star.paintIcon(this, graphics, getWidth() - left - 28, y);
                } else {
                    SystemIcon bookmark = new SystemIcon(SystemIcon.Type.BOOKMARK, 18,
                        UiTheme.GOLD);
                    int x = (getWidth() + textWidth) / 2 + 12;
                    bookmark.paintIcon(this, graphics, x,
                        (getHeight() - 18) / 2 + faceOffset);
                }
            }
        };
        button.setFont(UiTheme.body(Font.BOLD, primary ? 18 : 16));
        button.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(primary ? 332 : 264, primary ? 74 : 60));
        button.setMinimumSize(button.getPreferredSize());
        button.setMaximumSize(button.getPreferredSize());
        UiTheme.applyButtonStyle(button, primary ? UiTheme.ButtonStyle.TITLE_PRIMARY :
            UiTheme.ButtonStyle.TITLE_SECONDARY, primary ? 22 : 18, primary ? 34 : 28);
        return button;
    }

    private JPanel buildGameScreen() {
        BackgroundPanel root = new BackgroundPanel(0.68f);
        root.setLayout(new BorderLayout(14, 14));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        root.add(buildHeader(), BorderLayout.NORTH);

        story.setEditable(false);
        story.setLineWrap(true);
        story.setWrapStyleWord(true);
        story.setBackground(new Color(27, 29, 34));
        story.setForeground(INK);
        story.setCaretColor(INK);
        story.setSelectionColor(new Color(91, 73, 43));
        story.setFont(new Font(Font.SERIF, Font.PLAIN, 18));
        story.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        storyScroll = new JScrollPane(story);
        storyScroll.setBorder(BorderFactory.createLineBorder(new Color(111, 99, 73), 2));
        stage.setOpaque(false);
        stage.add(storyScroll, STORY_CARD);
        stage.add(encounterPanel, ENCOUNTER_CARD);
        stage.add(inventoryPanel, INVENTORY_CARD);
        stage.add(locationPanel, LOCATION_CARD);
        stage.add(outcomePanel, OUTCOME_CARD);
        root.add(stage, BorderLayout.CENTER);

        root.add(buildMapPanel(), BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(12, 8));
        bottom.setOpaque(false);
        bottom.add(buildMovementPanel(), BorderLayout.WEST);
        bottom.add(buildActionPanel(), BorderLayout.CENTER);
        bottom.add(buildFilePanel(), BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = translucentPanel(new BorderLayout(18, 4));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(214, 169, 70, 130)),
            BorderFactory.createEmptyBorder(8, 12, 10, 12)));

        JLabel title = new JLabel("THE CHOSEN QUEST");
        title.setForeground(GOLD);
        title.setFont(new Font(Font.SERIF, Font.BOLD, 26));
        header.add(title, BorderLayout.WEST);

        JPanel meters = new JPanel(new GridLayout(3, 1, 3, 3));
        meters.setOpaque(false);
        meters.setPreferredSize(new Dimension(300, 62));
        meters.add(healthBar);
        meters.add(manaBar);
        meters.add(experienceBar);
        header.add(meters, BorderLayout.CENTER);

        stats.setForeground(INK);
        stats.setHorizontalAlignment(SwingConstants.RIGHT);
        stats.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        header.add(stats, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMapPanel() {
        JPanel side = translucentPanel(new BorderLayout(8, 8));
        side.setPreferredSize(new Dimension(330, 0));
        side.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(111, 99, 73), 2),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        location.setForeground(GOLD);
        location.setFont(new Font(Font.SERIF, Font.BOLD, 18));
        side.add(location, BorderLayout.NORTH);

        JPanel mapContainer = new JPanel(new BorderLayout());
        mapContainer.setOpaque(false);
        mapContainer.add(map, BorderLayout.CENTER);
        JLabel key = new JLabel("<html><center><font color='#d6aa46'>●</font> You &nbsp;&nbsp; " +
            "<font color='#ad4036'>◆</font> Enemy<br>Fields · lakes · crypts · safe havens</center></html>",
            SwingConstants.CENTER);
        key.setForeground(MUTED);
        key.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        mapContainer.add(key, BorderLayout.SOUTH);
        side.add(mapContainer, BorderLayout.CENTER);

        enemy.setOpaque(true);
        enemy.setHorizontalAlignment(SwingConstants.CENTER);
        enemy.setBorder(BorderFactory.createEmptyBorder(9, 8, 9, 8));
        enemy.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        side.add(enemy, BorderLayout.SOUTH);
        return side;
    }

    private void installMovementKeys() {
        bindMovementKey("W", "moveNorthW", -1, 0);
        bindMovementKey("S", "moveSouthS", 1, 0);
        bindMovementKey("A", "moveWestA", 0, -1);
        bindMovementKey("D", "moveEastD", 0, 1);
        // Arrow keys remain as an accessibility fallback while the visible
        // map controls and primary bindings teach the standard WASD layout.
        bindMovementKey("UP", "moveNorthArrow", -1, 0);
        bindMovementKey("DOWN", "moveSouthArrow", 1, 0);
        bindMovementKey("LEFT", "moveWestArrow", 0, -1);
        bindMovementKey("RIGHT", "moveEastArrow", 0, 1);
        bindMapZoomKey(KeyStroke.getKeyStroke('+'), "mapZoomInPlus", true);
        bindMapZoomKey(KeyStroke.getKeyStroke('='), "mapZoomInEquals", true);
        bindMapZoomKey(KeyStroke.getKeyStroke('-'), "mapZoomOutMinus", false);
        bindMapZoomKey(KeyStroke.getKeyStroke("pressed ADD"), "mapZoomInNumpad", true);
        bindMapZoomKey(KeyStroke.getKeyStroke("pressed SUBTRACT"),
            "mapZoomOutNumpad", false);
        bindWorldMapKey();
        for (char key = '1'; key <= '7'; key++) bindCombatKey(key);
    }

    private void bindWorldMapKey() {
        JComponent root = frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("pressed M"), "toggleWorldMap");
        root.getActionMap().put("toggleWorldMap", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) {
                if (settingsOverlay.worldMapVisibleForTest()) {
                    settingsOverlay.closeWorldMap();
                } else if (gameVisible && !settingsOverlay.isVisible()) {
                    soundManager.play(SoundManager.Cue.UI_CONFIRM);
                    settingsOverlay.showWorldMap(engine);
                }
            }
        });
    }

    private void bindMapZoomKey(KeyStroke keyStroke, String actionName,
                                final boolean zoomIn) {
        if (keyStroke == null) return;
        JComponent root = frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(keyStroke, actionName);
        root.getActionMap().put(actionName, new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) {
                if (gameVisible && !settingsOverlay.isVisible() &&
                        enhancedExploration != null) {
                    enhancedExploration.triggerMapZoomShortcut(zoomIn);
                }
            }
        });
    }

    private void bindCombatKey(final char key) {
        String actionName = "combat" + key;
        JComponent root = frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("pressed " + key), actionName);
        root.getActionMap().put(actionName, new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) {
                GameEngine.State state = engine.getState();
                if (gameVisible && !settingsOverlay.isVisible() &&
                        state.health > 0 && !state.won &&
                        engine.currentEnemy() != null) {
                    encounterPanel.triggerShortcut(key);
                }
            }
        });
    }

    private void bindMovementKey(String keyStroke, String actionName,
                                 final int rowDelta, final int colDelta) {
        JComponent root = frame.getRootPane();
        final String heldKey = actionName;
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(keyStroke), actionName);
        root.getActionMap().put(actionName, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                if (!movementKeyGuard.press(heldKey)) return;
                GameEngine.State state = engine.getState();
                if (gameVisible && !settingsOverlay.isVisible() &&
                        state.health > 0 && !state.won && engine.currentEnemy() == null) {
                    stageMode = STORY_CARD;
                    if (enhancedExploration != null) {
                        enhancedExploration.triggerMovementShortcut(rowDelta, colDelta);
                    } else {
                        engine.move(rowDelta, colDelta);
                        refresh();
                    }
                }
            }
        });
        String releaseAction = actionName + "Released";
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("released " + keyStroke), releaseAction);
        root.getActionMap().put(releaseAction, new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent event) {
                movementKeyGuard.release(heldKey);
            }
        });
    }

    /** Turns operating-system key repeat into one deliberate tile step per press. */
    static final class KeyRepeatGuard {
        private final Set<String> held = new HashSet<String>();

        boolean press(String key) { return held.add(key); }
        void release(String key) { held.remove(key); }
        void clear() { held.clear(); }
    }

    private JPanel buildMovementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 1; c.gridy = 0;
        panel.add(movementButton("W", "North · W", new Runnable() { public void run() { engine.move(-1, 0); } }), c);
        c.gridx = 0; c.gridy = 1;
        panel.add(movementButton("A", "West · A", new Runnable() { public void run() { engine.move(0, -1); } }), c);
        c.gridx = 1;
        panel.add(movementButton("S", "South · S", new Runnable() { public void run() { engine.move(1, 0); } }), c);
        c.gridx = 2;
        panel.add(movementButton("D", "East · D", new Runnable() { public void run() { engine.move(0, 1); } }), c);
        return panel;
    }

    private JButton movementButton(String text, String tooltip, Runnable action) {
        JButton movement = button(text, tooltip, action);
        movementButtons.add(movement);
        return movement;
    }

    private JPanel buildActionPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 5, 5));
        panel.setOpaque(false);
        attackAction = button("Attack", null, new Runnable() { public void run() { engine.attack(); } });
        defendAction = button("Defend", null, new Runnable() { public void run() { engine.defend(); } });
        fleeAction = button("Flee", null, new Runnable() { public void run() { engine.flee(); } });
        potionAction = button("Potion", null, new Runnable() { public void run() { engine.usePotion(); } });
        panel.add(attackAction);
        panel.add(defendAction);
        panel.add(fleeAction);
        panel.add(potionAction);
        spellAction = styledButton("Cycle Spell", false);
        spellAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { cyclePreparedSpell(); }
        });
        panel.add(spellAction);
        inventoryAction = styledButton("Inventory", false);
        inventoryAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { showInventory(); }
        });
        panel.add(inventoryAction);
        styleButton(locationAction, true);
        locationAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (engine.currentTile() == GameEngine.TileType.SHOP && engine.currentEnemy() == null) {
                    showShop();
                } else if ((engine.currentTile() == GameEngine.TileType.TAVERN ||
                            engine.currentTile() == GameEngine.TileType.ENCAMPMENT) &&
                           engine.currentEnemy() == null) {
                    showHaven();
                } else {
                    engine.locationAction();
                    refresh();
                }
            }
        });
        panel.add(locationAction);
        JButton help = styledButton("Help", false);
        help.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { showHelp(); }
        });
        panel.add(help);
        return panel;
    }

    private JPanel buildFilePanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setOpaque(false);
        JButton newGame = styledButton("New Quest", false);
        newGame.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { showCharacterCreation(); }
        });
        JButton save = styledButton("Save", false);
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { chooseSave(); }
        });
        JButton load = styledButton("Load", false);
        load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { chooseLoad(); }
        });
        panel.add(newGame);
        panel.add(save);
        panel.add(load);
        return panel;
    }

    private JButton button(String text, String tooltip, final Runnable action) {
        JButton button = styledButton(text, false);
        if (tooltip != null) button.setToolTipText(tooltip);
        if (text.length() == 1) button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                stageMode = STORY_CARD;
                action.run();
                refresh();
            }
        });
        return button;
    }

    private JButton styledButton(String text, boolean accent) {
        JButton button = new JButton(text);
        styleButton(button, accent);
        return button;
    }

    private void styleButton(JButton button, boolean accent) {
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        UiTheme.applyButtonStyle(button,
            accent ? UiTheme.ButtonStyle.PRIMARY : UiTheme.ButtonStyle.SECONDARY, 7, 11);
    }

    private void showInventory() {
        transition("INVENTORY", new Color(32, 24, 16), 220, new Runnable() {
            public void run() {
                stageMode = INVENTORY_CARD;
                refreshNow();
            }
        });
    }

    private void showSpellMenu() {
        cyclePreparedSpell();
    }

    /** Cycles the prepared exploration shortcut without opening a system modal. */
    private void cyclePreparedSpell() {
        GameEngine.State state = engine.getState();
        if (state.spells.isEmpty()) {
            soundManager.play(SoundManager.Cue.ERROR);
            return;
        }
        int current = state.selectedSpell == null ? -1 : state.spells.indexOf(state.selectedSpell);
        String next = state.spells.get((current + 1) % state.spells.size());
        engine.selectSpell(next);
        soundManager.play(SoundManager.Cue.UI_CONFIRM);
        refresh();
    }

    private void showShop() {
        transition("ENTERING SHOP", new Color(54, 35, 12), 320, new Runnable() {
            public void run() {
                stageMode = LOCATION_CARD;
                refreshNow();
            }
        });
    }

    private void showHaven() {
        transition("ENTERING HAVEN", new Color(54, 35, 12), 320, new Runnable() {
            public void run() {
                stageMode = LOCATION_CARD;
                refreshNow();
            }
        });
    }

    private void showStory() {
        transition(null, Color.BLACK, 220, new Runnable() {
            public void run() {
                stageMode = STORY_CARD;
                refreshNow();
            }
        });
    }

    private void chooseSave() {
        JFileChooser chooser = saveChooser();
        chooser.setSelectedFile(new File("chosen-quest.save"));
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                engine.save(chooser.getSelectedFile());
                soundManager.play(SoundManager.Cue.UI_CONFIRM);
            } catch (Exception error) {
                soundManager.play(SoundManager.Cue.ERROR);
                showError("Could not save the quest", error);
            }
            refresh();
        }
    }

    private boolean chooseLoad() {
        JFileChooser chooser = saveChooser();
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                engine.load(chooser.getSelectedFile());
                soundManager.play(SoundManager.Cue.UI_CONFIRM);
                stageMode = STORY_CARD;
                refresh();
                return true;
            } catch (Exception error) {
                soundManager.play(SoundManager.Cue.ERROR);
                showError("Could not load the quest", error);
            }
        }
        return false;
    }

    private JFileChooser saveChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("The Chosen Quest save file");
        return chooser;
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(frame,
            "Explore with WASD or the map-rail keys (arrows also work).\n\n" +
            "In combat, use 1–7 for attack, class actions, abilities, potion, and flee.\n" +
            "Enemies block movement. Attack, use class tactics, or flee to safety.\n" +
            "Potions restore 20 health. Shops sell them for 10 gold.\n" +
            "Equip weapons and armour from the inventory.\n" +
            "Mage spells consume mana. Fighters build Rage only by attacking and taking damage.\n" +
            "Rogue techniques spend Momentum built through attacks, stealth, and evasion.\n" +
            "Hunter techniques spend Focus built through Aim, accurate shots, and enemy misses.\n" +
            "Resting restores health and Mage mana. Rage and Momentum reset between encounters;\n" +
            "Hunters retain earned Focus between fights.\n" +
            "Defeating enemies earns experience and levels.\n\n" +
            "Defeat the dragon in the southeast corner to win.",
            "How to play", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String title, Exception error) {
        JOptionPane.showMessageDialog(frame, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private void showGame() {
        settingsOverlay.hideSettings();
        transition("THE QUEST BEGINS", new Color(20, 14, 9), 480, new Runnable() {
            public void run() {
                gameVisible = true;
                stageMode = STORY_CARD;
                cardLayout.show(cards, GAME_CARD);
                refreshNow();
            }
        });
    }

    private void showCharacterCreation() {
        settingsOverlay.hideSettings();
        soundManager.setMusic(SoundManager.Music.EXPLORATION);
        soundManager.stopAmbience();
        transition("CHOOSE YOUR PATH", new Color(20, 14, 9), 360, new Runnable() {
            public void run() {
                gameVisible = false;
                cardLayout.show(cards, CREATE_CARD);
            }
        });
    }

    private void showTitleScreen() {
        settingsOverlay.hideSettings();
        soundManager.stopAmbience();
        transition(null, Color.BLACK, 300, new Runnable() {
            public void run() {
                gameVisible = false;
                stageMode = STORY_CARD;
                cardLayout.show(cards, TITLE_CARD);
                soundManager.playTitleMusic();
            }
        });
    }

    private void refresh() {
        List<GameEngine.EnemyTurnEvent> enemyTurns = engine.consumeEnemyTurnEvents();
        if (!combatSequencePlaying && gameVisible && frame.isShowing() &&
                !enemyTurns.isEmpty()) {
            playEnemyTurnSequence(enemyTurns);
            return;
        }
        final GameEngine.Relic discoveredRelic = engine.consumeRelicDiscovery();
        final GameEngine.Item discoveredLoot = engine.consumeLootDiscovery();
        final GameEngine.ProgressionNotice progression = engine.consumeProgressionNotice();
        GameEngine.State state = engine.getState();
        GameEngine.Enemy foe = engine.currentEnemy();
        if (gameVisible && frame.isShowing()) {
            if (state.won && previousFoe != null) {
                transition("VICTORY", new Color(71, 54, 15), 720,
                    new Runnable() { public void run() { refreshNow(); } });
                return;
            }
            if (state.health == 0 && previousPlayerHealth > 0) {
                transition("THE CHOSEN HAS FALLEN", new Color(72, 12, 12), 620,
                    new Runnable() { public void run() { refreshNow(); } });
                return;
            }
            if (foe != null && previousFoe == null) {
                final GameEngine.Enemy arrivingFoe = foe;
                final String label = foe.tier == 2 ? "BOSS ENCOUNTER" :
                    (foe.tier == 1 ? "ELITE ENCOUNTER" : "COMBAT");
                final Color tint = foe.tier == 2 ? new Color(78, 8, 18) :
                    (foe.tier == 1 ? new Color(72, 42, 8) : new Color(58, 12, 12));
                final int duration = foe.tier == 2 ? 780 : 420;
                transition(label, tint, duration, new Runnable() {
                    public void run() {
                        refreshNow();
                        scheduleEnemyThreat(arrivingFoe, duration / 2 + 90);
                    }
                });
                return;
            }
            if (foe == null && previousFoe != null) {
                transition("ENEMY DEFEATED", new Color(24, 58, 30), 360,
                    new Runnable() {
                        public void run() {
                            refreshNow();
                            if (progression != null || discoveredRelic != null ||
                                    discoveredLoot != null) {
                                Timer reveal = new Timer(220, new ActionListener() {
                                    public void actionPerformed(ActionEvent ignored) {
                                        if (progression != null) {
                                            showProgression(progression, discoveredRelic,
                                                discoveredLoot);
                                        } else if (discoveredRelic != null) {
                                            showRelicDiscovery(discoveredRelic, discoveredLoot);
                                        } else {
                                            showLootDiscovery(discoveredLoot);
                                        }
                                    }
                                });
                                reveal.setRepeats(false);
                                reveal.start();
                            }
                        }
                    });
                return;
            }
        }
        refreshNow();
    }

    private void showRelicDiscovery(GameEngine.Relic relic) {
        showRelicDiscovery(relic, null);
    }

    private void showRelicDiscovery(GameEngine.Relic relic,
                                    final GameEngine.Item chainedLoot) {
        soundManager.play(SoundManager.Cue.RELIC_DISCOVERED);
        settingsOverlay.showRelicDiscovery(relic, new Runnable() {
            public void run() { showInventory(); }
        }, new Runnable() {
            public void run() {
                if (chainedLoot != null) showLootDiscovery(chainedLoot);
                else refresh();
            }
        });
    }

    private void showLootDiscovery(final GameEngine.Item item) {
        String quality = GameEngine.itemQuality(item);
        soundManager.play("COMMON".equals(quality) ? SoundManager.Cue.LOOT_DISCOVERED :
            SoundManager.Cue.UNCOMMON_LOOT);
        GameEngine.State state = engine.getState();
        String equippedName = "Weapon".equals(item.type) ? state.equippedWeapon :
            ("Offhand".equals(item.type) ? state.equippedOffhand : state.equippedArmour);
        settingsOverlay.showLootDiscovery(item, equippedName,
            equippedValue(state, item.type), quickEquipAction(item), new Runnable() {
                public void run() { showInventory(); }
            }, new Runnable() {
                public void run() { refresh(); }
            });
    }

    private Runnable quickEquipAction(final GameEngine.Item item) {
        return quickEquipAction(item, SoundManager.Cue.EQUIP);
    }

    private Runnable quickEquipAction(final GameEngine.Item item,
                                      final SoundManager.Cue successCue) {
        return new Runnable() {
            public void run() {
                GameEngine.State before = engine.getState();
                String weapon = before.equippedWeapon;
                String armour = before.equippedArmour;
                String offhand = before.equippedOffhand;
                engine.equipItem(item);
                String styleNotice = engine.consumeCombatStyleNotice();
                GameEngine.State after = engine.getState();
                boolean changed = !equalsText(weapon, after.equippedWeapon) ||
                    !equalsText(armour, after.equippedArmour) ||
                    !equalsText(offhand, after.equippedOffhand);
                soundManager.play(changed ? successCue : SoundManager.Cue.ERROR);
                refresh();
                showCombatStyleNotice(styleNotice);
            }
        };
    }

    private void showCombatStyleNotice(String notice) {
        if (notice == null || notice.length() == 0) return;
        GameEngine.State state = engine.getState();
        HeroVisualTheme theme = HeroVisualTheme.forBuild(state.race, state.heroClass);
        settingsOverlay.showNotice(notice + " · ORIGIN " + GameEngine.originPath(state) +
            " PRESERVED", theme.classAccent(), preferences.isReducedMotion() ? 2200 : 2800);
    }

    private void showProgression(GameEngine.ProgressionNotice notice,
                                 final GameEngine.Relic chainedRelic,
                                 final GameEngine.Item chainedLoot) {
        soundManager.play(SoundManager.Cue.ADVENTURE_BEGIN);
        Runnable next = new Runnable() {
            public void run() {
                if (chainedRelic != null) showRelicDiscovery(chainedRelic, chainedLoot);
                else if (chainedLoot != null) showLootDiscovery(chainedLoot);
                else refresh();
            }
        };
        settingsOverlay.showProgression(notice, new Runnable() {
            public void run() {
                showInventory();
                if (chainedRelic != null || chainedLoot != null) {
                    Timer reveal = new Timer(260, new ActionListener() {
                        public void actionPerformed(ActionEvent ignored) {
                            if (chainedRelic != null) {
                                showRelicDiscovery(chainedRelic, chainedLoot);
                            } else {
                                showLootDiscovery(chainedLoot);
                            }
                        }
                    });
                    reveal.setRepeats(false);
                    reveal.start();
                }
            }
        }, next);
    }

    private void scheduleEnemyThreat(final GameEngine.Enemy foe, int delay) {
        Timer threat = new Timer(delay, new ActionListener() {
            public void actionPerformed(ActionEvent ignored) {
                if (!gameVisible || engine.currentEnemy() != foe || engine.getState().health <= 0) {
                    return;
                }
                if (foe.tier == 2) soundManager.play(SoundManager.Cue.DRAGON_ROAR);
                EncounterCatalog.Profile profile = EncounterCatalog.forEnemy(foe.name);
                settingsOverlay.showDialogue(EncounterPanel.assetFor(foe.name),
                    profile.displayName,
                    GameEngine.enemyIntroductionRole(foe),
                    GameEngine.enemyBattleCry(foe.name), profile.accent,
                    new Runnable() { public void run() { refresh(); } });
            }
        });
        threat.setRepeats(false);
        threat.start();
    }

    private static String dialogueCopy(String historyFragment) {
        if (historyFragment == null) return "";
        return historyFragment.replace("• ", "").replace('\n', ' ').trim();
    }

    private GameEngine.Item inventoryItem(String name) {
        if (name == null || engine.getState() == null) return null;
        for (GameEngine.Item item : engine.getState().inventory) {
            if (name.equals(item.name)) return item;
        }
        return null;
    }

    private int equippedValue(GameEngine.State state, String type) {
        if (state == null) return 0;
        String name = "Weapon".equals(type) ? state.equippedWeapon :
            ("Offhand".equals(type) ? state.equippedOffhand : state.equippedArmour);
        for (GameEngine.Item item : state.inventory) {
            if (item.name.equals(name)) return GameEngine.itemStat(item);
        }
        return 0;
    }

    /** Presents pre-resolved enemy events one at a time so multi-hits remain legible. */
    private void playEnemyTurnSequence(final List<GameEngine.EnemyTurnEvent> events) {
        combatSequencePlaying = true;
        encounterPanel.setCombatActionsEnabled(false);
        final GameEngine.State state = engine.getState();
        final GameEngine.Enemy foe = engine.currentEnemy();
        final String fullHistory = engine.getHistory();
        int prefixEnd = Math.max(0, Math.min(fullHistory.length(), events.get(0).historyStart));
        if (foe != null) {
            encounterPanel.setEncounter(foe, fullHistory.substring(0, prefixEnd), state);
            encounterPanel.setCombatActionsEnabled(false);
        }
        if (enhancedExploration != null) {
            enhancedExploration.showCombatHealth(events.get(0).healthBefore, state.maxHealth);
            enhancedExploration.showEncounter();
        }

        final int[] index = {0};
        final boolean reducedMotion = preferences.isReducedMotion();
        Timer timer = new Timer(enemyBeatInterval(reducedMotion), null);
        timer.setInitialDelay(enemyBeatInitialDelay(reducedMotion));
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                int position = index[0];
                GameEngine.EnemyTurnEvent turn = events.get(position);
                int historyEnd = Math.max(0, Math.min(fullHistory.length(), turn.historyEnd));
                encounterPanel.showEnemyTurnBeat(turn, position + 1, events.size(),
                    fullHistory.substring(0, historyEnd));
                encounterPanel.playEnemyAttack(turn, new Runnable() {
                    public void run() {
                        if (enhancedExploration != null) {
                            enhancedExploration.animateCombatHealth(turn.healthBefore,
                                turn.healthAfter, state.maxHealth,
                                preferences.isReducedMotion());
                        }
                        soundManager.play(turn.missed ? SoundManager.Cue.DEFEND :
                            SoundManager.Cue.PLAYER_HIT);
                    }
                }, null);
                index[0]++;
                if (index[0] >= events.size()) {
                    ((Timer) actionEvent.getSource()).stop();
                    Timer settle = new Timer(enemyBeatSettleDelay(reducedMotion), new ActionListener() {
                        public void actionPerformed(ActionEvent ignored) {
                            encounterPanel.clearEnemyTurnBeat();
                            encounterPanel.setCombatActionsEnabled(true);
                            combatSequencePlaying = false;
                            if (state.health > 0) {
                                previousPlayerHealth = state.health;
                            }
                            if (foe != null) previousEnemyHealth = foe.health;
                            refresh();
                        }
                    });
                    settle.setRepeats(false);
                    settle.start();
                }
            }
        });
        timer.start();
    }

    private void refreshNow() {
        updateAmbience(engine.getState(), engine.currentEnemy());
        updateMusic(engine.getState(), engine.currentEnemy());
        if (enhancedExploration != null) {
            enhancedExploration.refresh();
            GameEngine.State state = engine.getState();
            GameEngine.Enemy foe = engine.currentEnemy();
            playStateTransition(state, foe);
            if (state.won) {
                outcomePanel.showVictory(state);
                enhancedExploration.showOutcome();
            } else if (state.health == 0) {
                outcomePanel.showDefeat(state);
                enhancedExploration.showOutcome();
            } else if (foe != null) {
                encounterPanel.setEncounter(foe, engine.getHistory(), state);
                if (gameVisible && previousPlayerHealth >= 0 && state.health < previousPlayerHealth) {
                    encounterPanel.flashDamage(true);
                } else if (gameVisible && foe == previousFoe && previousEnemyHealth >= 0 &&
                           foe.health < previousEnemyHealth) {
                    encounterPanel.flashDamage(false);
                }
                enhancedExploration.showEncounter();
            } else if (INVENTORY_CARD.equals(stageMode)) {
                inventoryPanel.setState(state, engine.getAttack(), engine.getDefense());
                enhancedExploration.showInventory();
            } else if (LOCATION_CARD.equals(stageMode) &&
                       engine.currentTile() == GameEngine.TileType.SHOP) {
                locationPanel.showShop(state, engine.shopItems());
                enhancedExploration.showLocation();
            } else if (LOCATION_CARD.equals(stageMode) &&
                       (engine.currentTile() == GameEngine.TileType.TAVERN ||
                        engine.currentTile() == GameEngine.TileType.ENCAMPMENT)) {
                locationPanel.showHaven(state, engine.currentTile());
                enhancedExploration.showLocation();
            } else {
                stageMode = STORY_CARD;
                enhancedExploration.showStory();
            }
            previousFoe = foe;
            previousEnemyHealth = foe == null ? -1 : foe.health;
            previousPlayerHealth = state.health;
            return;
        }
        GameEngine.State state = engine.getState();
        GameEngine.Enemy foe = engine.currentEnemy();
        playStateTransition(state, foe);
        stats.setText("<html><div align='right'>" + state.playerName + " · Level " + state.level +
            "<br>ATK " + engine.getAttack() + " &nbsp; DEF " + engine.getDefense() +
            " &nbsp; Gold " + state.gold + " &nbsp; Potions " + state.potions + "</div></html>");
        setMeter(healthBar, state.health, state.maxHealth, "HEALTH");
        if ("Mage".equals(state.heroClass)) {
            manaBar.setForeground(new Color(54, 91, 151));
            setMeter(manaBar, state.mana, Math.max(1, state.maxMana), "MANA");
            manaBar.setVisible(true);
        } else if ("Fighter".equals(state.heroClass)) {
            manaBar.setForeground(new Color(194, 67, 44));
            setMeter(manaBar, state.rage, Math.max(1, state.maxRage), "RAGE");
            manaBar.setVisible(true);
        } else if ("Rogue".equals(state.heroClass)) {
            manaBar.setForeground(new Color(139, 92, 183));
            setMeter(manaBar, state.momentum, Math.max(1, state.maxMomentum), "MOMENTUM");
            manaBar.setVisible(true);
        } else if ("Hunter".equals(state.heroClass)) {
            manaBar.setForeground(new Color(91, 164, 91));
            setMeter(manaBar, state.focus, Math.max(1, state.maxFocus), "FOCUS");
            manaBar.setVisible(true);
        } else {
            manaBar.setVisible(false);
        }
        setMeter(experienceBar, state.experience, state.level * 30, "EXPERIENCE");

        location.setText("Location: " + engine.currentTile().label + "  (" +
            (char) ('A' + state.row) + (state.col + 1) + ")");
        boolean active = state.health > 0 && !state.won;
        boolean inCombat = foe != null && active;
        updateActionAvailability(state, inCombat, active);

        enemy.setIcon(null);
        if (state.won) {
            enemy.setText("Quest complete");
            enemy.setForeground(UiTheme.GOLD_LIGHT);
            enemy.setBackground(new Color(77, 61, 28));
            outcomePanel.showVictory(state);
            stageLayout.show(stage, OUTCOME_CARD);
        } else if (state.health == 0) {
            enemy.setText("The Chosen has fallen");
            enemy.setForeground(new Color(255, 219, 205));
            enemy.setBackground(new Color(91, 40, 38));
            outcomePanel.showDefeat(state);
            stageLayout.show(stage, OUTCOME_CARD);
        } else if (foe == null) {
            enemy.setText("No immediate danger");
            enemy.setForeground(new Color(182, 199, 177));
            enemy.setBackground(new Color(41, 65, 48));
            storyScroll.setBorder(BorderFactory.createLineBorder(new Color(111, 99, 73), 2));
            if (INVENTORY_CARD.equals(stageMode)) {
                inventoryPanel.setState(state, engine.getAttack(), engine.getDefense());
                stageLayout.show(stage, INVENTORY_CARD);
            } else if (LOCATION_CARD.equals(stageMode) &&
                       engine.currentTile() == GameEngine.TileType.SHOP) {
                locationPanel.showShop(state, engine.shopItems());
                stageLayout.show(stage, LOCATION_CARD);
            } else if (LOCATION_CARD.equals(stageMode) &&
                       (engine.currentTile() == GameEngine.TileType.TAVERN ||
                        engine.currentTile() == GameEngine.TileType.ENCAMPMENT)) {
                locationPanel.showHaven(state, engine.currentTile());
                stageLayout.show(stage, LOCATION_CARD);
            } else {
                stageMode = STORY_CARD;
                stageLayout.show(stage, STORY_CARD);
            }
        } else {
            enemy.setIcon(IconAssets.icon(IconAssets.WEAPON_SWORD, 18));
            enemy.setIconTextGap(8);
            enemy.setText(foe.name + "   HP " + foe.health + "/" + foe.maxHealth);
            enemy.setForeground(new Color(255, 219, 205));
            enemy.setBackground(new Color(91, 40, 38));
            storyScroll.setBorder(BorderFactory.createLineBorder(RED, 2));
            encounterPanel.setEncounter(foe, engine.getHistory(), state);
            if (gameVisible && previousPlayerHealth >= 0 && state.health < previousPlayerHealth) {
                encounterPanel.flashDamage(true);
            } else if (gameVisible && foe == previousFoe && previousEnemyHealth >= 0 &&
                       foe.health < previousEnemyHealth) {
                encounterPanel.flashDamage(false);
            }
            stageLayout.show(stage, ENCOUNTER_CARD);
        }
        previousFoe = foe;
        previousEnemyHealth = foe == null ? -1 : foe.health;
        previousPlayerHealth = state.health;
        map.repaint();
        story.setText(engine.getHistory());
        story.setCaretPosition(story.getDocument().getLength());

        if (engine.currentTile() == GameEngine.TileType.SHOP) {
            locationAction.setText("Shop");
        } else if (engine.currentTile() == GameEngine.TileType.TAVERN ||
                   engine.currentTile() == GameEngine.TileType.ENCAMPMENT) {
            locationAction.setText("Rest");
        } else {
            locationAction.setText("Explore");
        }
    }

    private void transition(String label, Color color, int duration, Runnable midpoint) {
        if (!frame.isShowing()) {
            if (midpoint != null) midpoint.run();
            return;
        }
        settingsOverlay.playTransition(label, color, duration, midpoint);
    }

    private void updateAmbience(GameEngine.State state, GameEngine.Enemy foe) {
        if (!gameVisible || state.won || state.health == 0) {
            soundManager.stopAmbience();
            return;
        }
        if (foe != null) {
            soundManager.setAmbience(foe.tier == 2 ?
                SoundManager.Ambience.DRAGON : SoundManager.Ambience.COMBAT);
            return;
        }
        SoundManager.Ambience ambience;
        switch (engine.currentTile()) {
            case LAKE:
                ambience = SoundManager.Ambience.LAKESHORE;
                break;
            case CRYPT:
                ambience = SoundManager.Ambience.CRYPT;
                break;
            case SPIDER_NEST:
                ambience = SoundManager.Ambience.CRYPT;
                break;
            case TAVERN:
                ambience = SoundManager.Ambience.TAVERN;
                break;
            case SHOP:
                ambience = engine.isBlacksmithShop() ? SoundManager.Ambience.FORGE :
                    SoundManager.Ambience.MARKET;
                break;
            case ENCAMPMENT:
                ambience = !(state.row == 0 && state.col == 0) ?
                    SoundManager.Ambience.ALCHEMIST : SoundManager.Ambience.CAMPFIRE;
                break;
            default:
                ambience = SoundManager.Ambience.FOREST;
                break;
        }
        soundManager.setAmbience(ambience);
    }

    /** Music communicates broad danger while ambience continues to identify place. */
    private void updateMusic(GameEngine.State state, GameEngine.Enemy foe) {
        if (!gameVisible || state.won || state.health == 0) {
            soundManager.stopMusic();
        } else if (foe != null) {
            soundManager.setMusic(foe.tier == 2 ? SoundManager.Music.BOSS :
                SoundManager.Music.COMBAT);
        } else if (engine.currentTile() == GameEngine.TileType.TAVERN ||
                   engine.currentTile() == GameEngine.TileType.SHOP ||
                   engine.currentTile() == GameEngine.TileType.ENCAMPMENT) {
            soundManager.setMusic(SoundManager.Music.TAVERN);
        } else {
            soundManager.setMusic(SoundManager.Music.EXPLORATION);
        }
    }

    private void updateActionAvailability(GameEngine.State state, boolean inCombat, boolean active) {
        for (JButton movement : movementButtons) movement.setEnabled(active && !inCombat);
        attackAction.setEnabled(inCombat);
        defendAction.setEnabled(inCombat);
        fleeAction.setEnabled(inCombat && !(state.row == 0 && state.col == 0));
        potionAction.setEnabled(active && state.potions > 0 && state.health < state.maxHealth);
        spellAction.setEnabled(inCombat && !state.spells.isEmpty() && state.mana >= 7);
        inventoryAction.setEnabled(active);
        locationAction.setEnabled(active && !inCombat);
    }

    private void playStateTransition(GameEngine.State state, GameEngine.Enemy foe) {
        if (!gameVisible) return;
        if (state.won && previousFoe != null) {
            soundManager.play(SoundManager.Cue.DRAGON_ROAR);
            soundManager.play(SoundManager.Cue.VICTORY);
        } else if (state.health == 0 && previousPlayerHealth > 0) {
            soundManager.play(SoundManager.Cue.DEFEAT);
        } else if (foe == null && previousFoe != null) {
            soundManager.play(SoundManager.Cue.ENEMY_DEFEATED);
        } else if (previousPlayerHealth >= 0 && state.health < previousPlayerHealth) {
            soundManager.play(SoundManager.Cue.PLAYER_HIT);
        }
    }

    private static boolean equalsText(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private static JProgressBar meter(Color color) {
        JProgressBar bar = new JProgressBar();
        bar.setStringPainted(true);
        bar.setForeground(color);
        bar.setBackground(new Color(18, 20, 23));
        bar.setBorder(BorderFactory.createLineBorder(new Color(94, 89, 77)));
        bar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        return bar;
    }

    private void setMeter(JProgressBar bar, int value, int maximum, String label) {
        bar.setMaximum(Math.max(1, maximum));
        bar.setValue(Math.max(0, value));
        bar.setString(label + "  " + value + "/" + maximum);
    }

    private JPanel translucentPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(new Color(PANEL.getRed(), PANEL.getGreen(), PANEL.getBlue(), 235));
        return panel;
    }
}
