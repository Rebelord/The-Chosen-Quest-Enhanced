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
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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
            performPlayerAttack(false);
        }

        public void onDefend() {
            soundManager.play(SoundManager.Cue.DEFEND);
            engine.defend();
            encounterPanel.setCombatActionsEnabled(false);
            encounterPanel.playDefend(new Runnable() {
                public void run() { refresh(); }
            });
        }

        public void onSpell() { showSpellMenu(); }

        public void onQuickSpell() {
            performPlayerAttack(true);
        }

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
            GameEngine.State state = engine.getState();
            soundManager.play(equalsText(weapon, state.equippedWeapon) &&
                equalsText(armour, state.equippedArmour) &&
                equalsText(offhand, state.equippedOffhand) ? SoundManager.Cue.ERROR :
                SoundManager.Cue.EQUIP);
            refresh();
        }

        public void onBack() { showStory(); }
    });
    private final LocationPanel locationPanel = new LocationPanel(new LocationPanel.Listener() {
        public void onBuy(Object selection) {
            int gold = engine.getState().gold;
            if (selection instanceof GameEngine.Item) {
                engine.buyItem((GameEngine.Item) selection);
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
                    GameEngine.State after = engine.getState();
                    boolean autoEquipped = reward != null &&
                        (("Weapon".equals(reward.type) && !equalsText(previousWeapon, after.equippedWeapon)) ||
                         ("Armour".equals(reward.type) && !equalsText(previousArmour, after.equippedArmour)));
                    if (autoEquipped) {
                        settingsOverlay.showAutoEquipped(reward,
                            "Weapon".equals(reward.type) ? previousWeapon : previousArmour,
                            "Weapon".equals(reward.type) ? previousWeaponValue : previousArmourValue,
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
            boolean changed = !before.equals(engine.getHistory());
            soundManager.play(purchased ? SoundManager.Cue.PURCHASE :
                (changed ? SoundManager.Cue.UI_CONFIRM : SoundManager.Cue.ERROR));
            refresh();
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

    private void performPlayerAttack(boolean magical) {
        final GameEngine.Enemy target = engine.currentEnemy();
        if (target == null) {
            soundManager.play(SoundManager.Cue.ERROR);
            refresh();
            return;
        }
        int historyStart = engine.getHistory().length();
        int healthBefore = target.health;
        int manaBefore = engine.getState().mana;
        String preparedSpell = magical ? engine.getState().selectedSpell : null;
        if (magical) {
            engine.castSelectedSpell();
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

    void show() {
        frame.setVisible(true);
        soundManager.playTitleMusic();
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1100, 720));
        frame.setSize(UiTheme.SHELL_WIDTH, UiTheme.SHELL_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.addWindowFocusListener(new WindowAdapter() {
            public void windowLostFocus(WindowEvent event) {
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
                soundManager.play(SoundManager.Cue.ADVENTURE_BEGIN);
                engine.newGame(name, race, heroClass, starterKit);
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
        }), CREATE_CARD);
        enhancedExploration = new EnhancedExplorationPanel(engine,
            new EnhancedExplorationPanel.Listener() {
                public void onInventory() { showInventory(); }
                public void onSpellbook() { showSpellMenu(); }
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
        });
    }

    static JPanel buildTitleScreen(ActionListener beginAction, ActionListener loadAction) {
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
        bottomSpacer.setPreferredSize(new Dimension(1, 96));
        root.add(bottomSpacer, c);
        return root;
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
        bindMovementKey("UP", "moveNorth", -1, 0);
        bindMovementKey("DOWN", "moveSouth", 1, 0);
        bindMovementKey("LEFT", "moveWest", 0, -1);
        bindMovementKey("RIGHT", "moveEast", 0, 1);
        bindCombatKey('A');
        bindCombatKey('D');
        bindCombatKey('S');
        bindCombatKey('P');
        bindCombatKey('Q');
        bindCombatKey('F');
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
                if (gameVisible && state.health > 0 && !state.won &&
                        engine.currentEnemy() != null) {
                    encounterPanel.triggerShortcut(key);
                }
            }
        });
    }

    private void bindMovementKey(String keyStroke, String actionName,
                                 final int rowDelta, final int colDelta) {
        JComponent root = frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(keyStroke), actionName);
        root.getActionMap().put(actionName, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                GameEngine.State state = engine.getState();
                if (gameVisible && state.health > 0 && !state.won && engine.currentEnemy() == null) {
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
    }

    private JPanel buildMovementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 1; c.gridy = 0;
        panel.add(movementButton("↑", "North", new Runnable() { public void run() { engine.move(-1, 0); } }), c);
        c.gridx = 0; c.gridy = 1;
        panel.add(movementButton("←", "West", new Runnable() { public void run() { engine.move(0, -1); } }), c);
        c.gridx = 1;
        panel.add(movementButton("↓", "South", new Runnable() { public void run() { engine.move(1, 0); } }), c);
        c.gridx = 2;
        panel.add(movementButton("→", "East", new Runnable() { public void run() { engine.move(0, 1); } }), c);
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
        spellAction = styledButton("Select Spell", false);
        spellAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { showSpellMenu(); }
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
        GameEngine.State state = engine.getState();
        if (state.spells.isEmpty()) {
            soundManager.play(SoundManager.Cue.ERROR);
            JOptionPane.showMessageDialog(frame,
                "Your class does not know any spells.", "Spellbook",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String current = state.selectedSpell != null && state.spells.contains(state.selectedSpell)
            ? state.selectedSpell : state.spells.get(0);
        Object spell = JOptionPane.showInputDialog(frame,
            "Choose the spell assigned to Quick Cast:\n" +
            "Magic Missile: 7 mana\nFireball: 12 mana\nIce Spike: 15 mana",
            "Select Spell — " + state.mana + "/" + state.maxMana + " mana",
            JOptionPane.PLAIN_MESSAGE, null, state.spells.toArray(), current);
        if (spell != null) {
            engine.selectSpell(spell.toString());
            soundManager.play(SoundManager.Cue.UI_CONFIRM);
            refresh();
        }
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
            "Explore with the arrow keys or compass buttons.\n\n" +
            "Enemies block movement. Attack, defend, or flee to camp.\n" +
            "Potions restore 20 health. Shops sell them for 10 gold.\n" +
            "Equip weapons and armour from the inventory.\n" +
            "Mage spells consume mana. Resting restores health and mana.\n" +
            "Defeating enemies earns experience and levels.\n\n" +
            "Defeat the dragon in the southeast corner to win.",
            "How to play", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String title, Exception error) {
        JOptionPane.showMessageDialog(frame, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private void showGame() {
        settingsOverlay.hideSettings();
        soundManager.stopMusic();
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
        soundManager.stopMusic();
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
                final String label = foe.tier == 2 ? "BOSS ENCOUNTER" :
                    (foe.tier == 1 ? "ELITE ENCOUNTER" : "COMBAT");
                final Color tint = foe.tier == 2 ? new Color(78, 8, 18) :
                    (foe.tier == 1 ? new Color(72, 42, 8) : new Color(58, 12, 12));
                transition(label, tint, foe.tier == 2 ? 780 : 420,
                    new Runnable() { public void run() { refreshNow(); } });
                return;
            }
            if (foe == null && previousFoe != null) {
                transition("ENEMY DEFEATED", new Color(24, 58, 30), 360,
                    new Runnable() {
                        public void run() {
                            refreshNow();
                            if (discoveredRelic != null) {
                                Timer reveal = new Timer(220, new ActionListener() {
                                    public void actionPerformed(ActionEvent ignored) {
                                        showRelicDiscovery(discoveredRelic);
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
        soundManager.play(SoundManager.Cue.RELIC_DISCOVERED);
        settingsOverlay.showRelicDiscovery(relic, new Runnable() {
            public void run() { showInventory(); }
        }, new Runnable() {
            public void run() { refresh(); }
        });
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
        Timer timer = new Timer(ENEMY_BEAT_INTERVAL_MS, null);
        timer.setInitialDelay(ENEMY_BEAT_INITIAL_DELAY_MS);
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
                    Timer settle = new Timer(ENEMY_BEAT_SETTLE_MS, new ActionListener() {
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
        setMeter(manaBar, state.mana, Math.max(1, state.maxMana), "MANA");
        manaBar.setVisible(state.maxMana > 0);
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
