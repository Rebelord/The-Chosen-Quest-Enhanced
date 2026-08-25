package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Figma-aligned standard, elite, and boss combat component. */
final class EncounterPanel extends JPanel {
    interface Listener {
        void onAttack();
        void onDefend();
        void onAbility(int slot);
        void onPotion();
        void onFlee();
    }

    /**
     * Visual vocabulary for player attacks. This deliberately mirrors the
     * Figma Combat / Motion Beat Style variants so gameplay and design use the
     * same names without leaking rendering details into MainWindow.
     */
    enum PlayerAttackStyle {
        FIGHTER, MAGE, ROGUE, HUNTER, SPELL, FIREBALL, ICE_SPIKE,
        SHIELD_BASH, HEAVY_STRIKE, OFFHAND_STRIKE, PINNING_SHOT,
        WEAPON_MASTERY, EXECUTE, HUNTERS_MARK;

        static PlayerAttackStyle forAction(String heroClass, boolean spell) {
            return forAction(heroClass, spell ? "Magic Missile" : null);
        }

        static PlayerAttackStyle forAction(String heroClass, String spellName) {
            if ("Fireball".equals(spellName)) return FIREBALL;
            if ("Ice Spike".equals(spellName)) return ICE_SPIKE;
            if (spellName != null) return SPELL;
            if ("Mage".equals(heroClass)) return MAGE;
            if ("Rogue".equals(heroClass)) return ROGUE;
            if ("Hunter".equals(heroClass)) return HUNTER;
            return FIGHTER;
        }

        static PlayerAttackStyle forAbility(String heroClass, String ability) {
            if ("Shield Bash".equals(ability)) return SHIELD_BASH;
            if ("Stunning Blow".equals(ability)) return SHIELD_BASH;
            if ("Heavy Strike".equals(ability) || "Power Strike".equals(ability))
                return HEAVY_STRIKE;
            if ("Armor Breaker".equals(ability) || "Piercing Bolt".equals(ability))
                return HEAVY_STRIKE;
            if ("Offhand Strike".equals(ability)) return OFFHAND_STRIKE;
            if ("Blade Flurry".equals(ability) || "Twin Strike".equals(ability))
                return OFFHAND_STRIKE;
            if ("Pinning Shot".equals(ability)) return PINNING_SHOT;
            if ("Volley".equals(ability)) return PINNING_SHOT;
            if ("Execute".equals(ability)) return EXECUTE;
            if ("Hunter's Mark".equals(ability)) return HUNTERS_MARK;
            if ("Mage".equals(heroClass)) return forAction(heroClass, ability);
            return WEAPON_MASTERY;
        }
    }

    private static final long serialVersionUID = 1L;
    private static final Color CONSOLE = new Color(22, 15, 12);
    private static final Color INFO = new Color(52, 36, 27);
    private static final int REDUCED_MOTION_DURATION_MS = 220;
    private static final int COMBAT_LOG_FONT_SIZE = 14;
    private static final float COMBAT_LOG_LINE_SPACING = 0.18f;

    private final CombatStage stage = new CombatStage();
    private final JLabel enemyName = new JLabel();
    private final JLabel subtitle = new JLabel();
    private final JLabel defense = new JLabel();
    private final JLabel threat = new JLabel();
    private final JLabel healthCopy = new JLabel();
    private final JPanel traitChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final JPanel statusChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final JLabel tactic = new JLabel();
    private final JLabel timeline = new JLabel();
    private final JLabel speedComparison = new JLabel();
    private final JLabel statusFeedback = new JLabel();
    private final JLabel enemyIntent = new JLabel();
    private final JLabel turnBeat = new JLabel();
    private final JProgressBar health = new JProgressBar();
    private final JPanel resourceHost = new JPanel(new BorderLayout());
    private JProgressBar resourceMeter;
    private final JTextPane battleLog = new JTextPane();
    private final JPanel enemyInformation;
    private final JPanel commandConsole;
    private final JButton attack;
    private final JButton defend;
    private final JButton spellOne;
    private final JButton spellTwo;
    private final JButton spellThree;
    private final JButton potion;
    private final JButton flee;
    private final JPanel actionGrid = new JPanel(new GridLayout(0, 2, 10, 8));
    private final javax.swing.border.Border normalBorder =
        BorderFactory.createEmptyBorder(0, 0, 0, 0);
    private boolean reducedMotion;
    private GameEngine.Enemy displayedEnemy;

    /** Timing values are shared with the Figma Combat / Threat Intensity spec. */
    static int animationDurationForTier(EncounterCatalog.Tier tier, boolean reducedMotion) {
        if (reducedMotion) return REDUCED_MOTION_DURATION_MS;
        if (tier == EncounterCatalog.Tier.BOSS) return 520;
        if (tier == EncounterCatalog.Tier.ELITE) return 430;
        return 400;
    }

    static int shakeStrengthForTier(EncounterCatalog.Tier tier) {
        if (tier == EncounterCatalog.Tier.BOSS) return 9;
        if (tier == EncounterCatalog.Tier.ELITE) return 6;
        return 4;
    }

    EncounterPanel() {
        this(new Listener() {
            public void onAttack() { }
            public void onDefend() { }
            public void onAbility(int slot) { }
            public void onPotion() { }
            public void onFlee() { }
        });
    }

    EncounterPanel(final Listener listener) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiTheme.SURFACE_DEEP);
        setBorder(normalBorder);
        setPreferredSize(new Dimension(UiTheme.CENTER_WIDTH, UiTheme.BODY_HEIGHT));

        attack = actionButton("1", "Attack", new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onAttack(); }
        });
        defend = actionButton("2", "Defend", new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onDefend(); }
        });
        spellOne = actionButton("3", "Magic Missile", new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onAbility(1); }
        });
        spellTwo = actionButton("4", "Fireball", new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onAbility(2); }
        });
        spellThree = actionButton("5", "Ice Spike", new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onAbility(3); }
        });
        potion = actionButton("6", "Use Potion", new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onPotion(); }
        });
        flee = actionButton("7", "Flee Battle", new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onFlee(); }
        });
        UiTheme.applyButtonStyle(flee, UiTheme.ButtonStyle.DANGER, 7, 10);

        installFocusedAbilityHelp(spellOne);
        installFocusedAbilityHelp(spellTwo);
        installFocusedAbilityHelp(spellThree);

        enemyInformation = buildEnemyInformation();
        commandConsole = buildCommandConsole();
        add(stage);
        add(enemyInformation);
        add(commandConsole);
    }

    private JPanel buildEnemyInformation() {
        JPanel panel = fixedPanel(UiTheme.ENEMY_INFO_HEIGHT, INFO);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, UiTheme.BORDER),
            BorderFactory.createEmptyBorder(14, 24, 12, 24)));

        JPanel identity = new JPanel(new BorderLayout(12, 0));
        identity.setOpaque(false);
        identity.setMaximumSize(new Dimension(Integer.MAX_VALUE, 47));
        JPanel names = new JPanel();
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.setOpaque(false);
        enemyName.setForeground(UiTheme.TEXT);
        enemyName.setFont(UiTheme.display(22));
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.body(Font.PLAIN, 10));
        names.add(enemyName);
        names.add(subtitle);
        identity.add(names, BorderLayout.CENTER);

        JPanel danger = new JPanel(new BorderLayout(10, 0));
        danger.setOpaque(false);
        defense.setForeground(UiTheme.TEXT);
        defense.setFont(UiTheme.body(Font.BOLD, 10));
        defense.setHorizontalAlignment(SwingConstants.RIGHT);
        threat.setOpaque(true);
        threat.setForeground(new Color(30, 22, 14));
        threat.setFont(UiTheme.body(Font.BOLD, 9));
        threat.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
        danger.add(defense, BorderLayout.CENTER);
        danger.add(threat, BorderLayout.EAST);
        identity.add(danger, BorderLayout.EAST);
        panel.add(identity);

        JPanel healthHeader = new JPanel(new BorderLayout());
        healthHeader.setOpaque(false);
        healthHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel healthLabel = new JLabel("ENEMY HEALTH");
        healthLabel.setForeground(UiTheme.MUTED);
        healthLabel.setFont(UiTheme.body(Font.BOLD, 9));
        healthCopy.setForeground(UiTheme.TEXT);
        healthCopy.setFont(UiTheme.body(Font.BOLD, 10));
        healthCopy.setHorizontalAlignment(SwingConstants.RIGHT);
        healthHeader.add(healthLabel, BorderLayout.WEST);
        healthHeader.add(healthCopy, BorderLayout.EAST);
        panel.add(healthHeader);

        health.setStringPainted(false);
        health.setForeground(new Color(72, 174, 78));
        health.setBackground(new Color(24, 20, 18));
        health.setBorder(BorderFactory.createEmptyBorder());
        health.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        health.setPreferredSize(new Dimension(UiTheme.CENTER_WIDTH - 48, 12));
        panel.add(health);
        panel.add(Box.createVerticalStrut(6));

        JPanel initiative = new JPanel(new BorderLayout(10, 0));
        initiative.setOpaque(true);
        initiative.setBackground(new Color(29, 22, 18));
        initiative.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(83, 67, 49)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        initiative.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel turnLabel = new JLabel("TURN ORDER");
        turnLabel.setForeground(UiTheme.GOLD);
        turnLabel.setFont(UiTheme.body(Font.BOLD, 8));
        timeline.setForeground(UiTheme.TEXT);
        timeline.setFont(UiTheme.body(Font.BOLD, 9));
        speedComparison.setForeground(UiTheme.MUTED);
        speedComparison.setFont(UiTheme.body(Font.BOLD, 8));
        speedComparison.setHorizontalAlignment(SwingConstants.RIGHT);
        statusFeedback.setForeground(new Color(105, 190, 119));
        statusFeedback.setFont(UiTheme.body(Font.BOLD, 8));
        statusFeedback.setHorizontalAlignment(SwingConstants.RIGHT);
        initiative.add(turnLabel, BorderLayout.WEST);
        initiative.add(timeline, BorderLayout.CENTER);
        JPanel heroReadiness = new JPanel(new GridLayout(2, 1, 0, 0));
        heroReadiness.setOpaque(false);
        heroReadiness.add(speedComparison);
        heroReadiness.add(statusFeedback);
        initiative.add(heroReadiness, BorderLayout.EAST);
        panel.add(initiative);
        panel.add(Box.createVerticalStrut(6));

        JPanel traits = new JPanel(new BorderLayout(14, 0));
        traits.setOpaque(false);
        traits.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        JPanel rowLabels = new JPanel(new GridLayout(2, 1));
        rowLabels.setOpaque(false);
        JLabel traitLabel = new JLabel("TRAITS");
        JLabel statusLabel = new JLabel("STATUS");
        for (JLabel label : new JLabel[] {traitLabel, statusLabel}) {
            label.setForeground(UiTheme.MUTED);
            label.setFont(UiTheme.body(Font.BOLD, 9));
            rowLabels.add(label);
        }
        traitChips.setOpaque(false);
        statusChips.setOpaque(false);
        tactic.setOpaque(true);
        tactic.setBackground(new Color(63, 47, 37));
        tactic.setForeground(UiTheme.MUTED);
        tactic.setFont(UiTheme.body(Font.PLAIN, 9));
        tactic.setHorizontalAlignment(SwingConstants.CENTER);
        tactic.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        enemyIntent.setForeground(UiTheme.GOLD_LIGHT);
        enemyIntent.setFont(UiTheme.body(Font.BOLD, 8));
        enemyIntent.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel chipRows = new JPanel(new GridLayout(2, 1));
        chipRows.setOpaque(false);
        chipRows.add(traitChips);
        chipRows.add(statusChips);
        traits.add(rowLabels, BorderLayout.WEST);
        traits.add(chipRows, BorderLayout.CENTER);
        JPanel intentStack = new JPanel(new GridLayout(2, 1, 0, 1));
        intentStack.setOpaque(false);
        intentStack.add(enemyIntent);
        intentStack.add(tactic);
        traits.add(intentStack, BorderLayout.EAST);
        panel.add(traits);
        return panel;
    }

    private JPanel buildCommandConsole() {
        JPanel console = fixedPanel(UiTheme.COMMAND_CONSOLE_HEIGHT, CONSOLE);
        console.setLayout(new BorderLayout());
        console.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(CONSOLE);
        actions.setPreferredSize(new Dimension(300, UiTheme.COMMAND_CONSOLE_HEIGHT));
        actions.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 16));
        JLabel actionHeading = sectionHeading("COMBAT ACTIONS");
        actionHeading.setAlignmentX(LEFT_ALIGNMENT);
        actions.add(actionHeading);
        actions.add(Box.createVerticalStrut(7));
        resourceHost.setOpaque(false);
        resourceHost.setAlignmentX(LEFT_ALIGNMENT);
        resourceHost.setPreferredSize(new Dimension(260, 38));
        resourceHost.setMaximumSize(new Dimension(260, 38));
        actions.add(resourceHost);
        actions.add(Box.createVerticalStrut(7));
        actionGrid.setOpaque(false);
        actionGrid.setAlignmentX(LEFT_ALIGNMENT);
        actionGrid.setPreferredSize(new Dimension(260, 150));
        actionGrid.setMaximumSize(new Dimension(260, 150));
        actions.add(actionGrid);
        actions.add(Box.createVerticalGlue());
        console.add(actions, BorderLayout.WEST);

        JPanel log = new JPanel(new BorderLayout(0, 9));
        log.setBackground(CONSOLE);
        log.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(74, 54, 42)),
            BorderFactory.createEmptyBorder(22, 18, 22, 24)));
        JPanel logHeader = new JPanel(new BorderLayout(12, 0));
        logHeader.setOpaque(false);
        JLabel logTitle = sectionHeading("COMBAT LOG");
        logTitle.setFont(UiTheme.body(Font.BOLD, 12));
        logHeader.add(logTitle, BorderLayout.WEST);
        turnBeat.setForeground(new Color(232, 92, 92));
        turnBeat.setBackground(new Color(66, 22, 22));
        turnBeat.setOpaque(false);
        turnBeat.setFont(UiTheme.body(Font.BOLD, 11));
        turnBeat.setHorizontalAlignment(SwingConstants.RIGHT);
        logHeader.add(turnBeat, BorderLayout.EAST);
        log.add(logHeader, BorderLayout.NORTH);
        battleLog.setEditable(false);
        battleLog.setBackground(CONSOLE);
        battleLog.setForeground(new Color(204, 192, 174));
        battleLog.setCaretColor(UiTheme.TEXT);
        battleLog.setFont(UiTheme.body(Font.PLAIN, COMBAT_LOG_FONT_SIZE));
        battleLog.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        JScrollPane scroll = new JScrollPane(battleLog);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CONSOLE);
        log.add(scroll, BorderLayout.CENTER);
        console.add(log, BorderLayout.CENTER);
        return console;
    }

    private JLabel sectionHeading(String value) {
        JLabel heading = new JLabel(value);
        heading.setForeground(UiTheme.GOLD);
        heading.setFont(UiTheme.body(Font.BOLD, 10));
        return heading;
    }

    private JPanel fixedPanel(int height, Color background) {
        JPanel panel = new JPanel();
        Dimension size = new Dimension(UiTheme.CENTER_WIDTH, height);
        panel.setPreferredSize(size);
        panel.setMinimumSize(size);
        panel.setMaximumSize(size);
        panel.setBackground(background);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private JButton actionButton(String shortcut, String label, ActionListener listener) {
        JButton button = new CombatActionButton(shortcut, label);
        button.setFont(UiTheme.body(Font.PLAIN, 11));
        UiTheme.applyButtonStyle(button, UiTheme.ButtonStyle.SECONDARY, 7, 10);
        button.addActionListener(listener);
        return button;
    }

    void setEncounter(GameEngine.Enemy enemy, String history) {
        setEncounter(enemy, history, null);
    }

    void setEncounter(GameEngine.Enemy enemy, String history, GameEngine.State state) {
        if (enemy == null) return;
        displayedEnemy = enemy;
        // The same panel instance is reused for every fight. A finishing attack
        // disables its controls while the impact animation plays, so a later
        // encounter must begin from an enabled baseline before class-specific
        // spell and potion availability is applied below.
        setCombatActionsEnabled(true);
        EncounterCatalog.Profile profile = EncounterCatalog.forEnemy(enemy.name);
        GameEngine.TileType tile = state == null ? GameEngine.TileType.FIELD :
            state.tiles[state.row][state.col];
        stage.setProfile(profile, sceneFor(tile));
        enemyName.setText(profile.displayName);
        subtitle.setText(profile.subtitle + " · LEVEL " + enemy.combatLevel);
        int armorBreak = state != null && state.enemyArmorBreakTurns > 0
            ? state.enemyArmorBreakValue : 0;
        defense.setText("DEF  " + Math.max(0, enemy.defense - armorBreak) +
            (armorBreak > 0 ? "  (−" + armorBreak + ")" : ""));
        threat.setText(profile.tier.label);
        threat.setBackground(profile.accent);
        health.setMaximum(Math.max(1, enemy.maxHealth));
        health.setValue(Math.max(0, enemy.health));
        health.setForeground(profile.tier == EncounterCatalog.Tier.BOSS
            ? new Color(194, 45, 45) : new Color(72, 174, 78));
        healthCopy.setText(enemy.health + " / " + enemy.maxHealth);
        updateTraitChips(profile, state);
        tactic.setText("  " + profile.tactic + "  ");
        enemyInformation.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, profile.accent),
            BorderFactory.createEmptyBorder(14, 24, 12, 24)));
        commandConsole.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, profile.accent));
        Color heroAccent = state == null ? UiTheme.GOLD :
            HeroVisualTheme.forBuild(state.race, state.heroClass).classAccent();
        for (JButton action : new JButton[] {attack, defend, spellOne, spellTwo, spellThree}) {
            action.setBorder(BorderFactory.createLineBorder(heroAccent));
            action.putClientProperty("thechosenquest.button.accent", heroAccent);
        }
        potion.setBorder(BorderFactory.createLineBorder(UiTheme.GREEN));
        potion.putClientProperty("thechosenquest.button.accent", UiTheme.GREEN);
        flee.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        flee.putClientProperty("thechosenquest.button.accent", null);

        if (state != null) {
            String defenseLabel = "Defend";
            String defenseTip = "Reduce incoming damage by 50%";
            if ("Mage".equals(state.heroClass)) {
                defenseLabel = "Channel Ward";
                defenseTip = "Restore a little mana and reduce incoming damage";
            } else if ("Rogue".equals(state.heroClass)) {
                defenseLabel = "Enter Stealth";
                defenseTip = "Evade the next attack and prepare a critical strike";
            } else if ("Hunter".equals(state.heroClass)) {
                defenseLabel = "Take Aim";
                defenseTip = "Prepare a stronger guaranteed precision shot";
            }
            ((CombatActionButton) defend).setActionLabel(defenseLabel);
            ((CombatActionButton) defend).setSpeedLabel("FAST");
            defend.setToolTipText(defenseTip);
            defend.setVisible(!"Fighter".equals(state.heroClass));
            ((CombatActionButton) potion).setActionLabel("Use Potion ×" + state.potions);
            ((CombatActionButton) potion).setSpeedLabel("NORMAL");
            potion.setEnabled(state.potions > 0 && state.health < state.maxHealth);
            configureAbilityButton(spellOne, state, 1);
            configureAbilityButton(spellTwo, state, 2);
            configureAbilityButton(spellThree, state, 3);
            updateResourceMeter(state);
            rebuildActionGrid(state);
            ((CombatActionButton) attack).setSpeedLabel(GameEngine.attackTempoLabel(state));
            ((CombatActionButton) flee).setSpeedLabel("NORMAL");
            timeline.setText(state.combatTimeline == null
                ? "YOU · READY  →  " + enemy.name.toUpperCase() + "  →  YOU"
                : state.combatTimeline);
            speedComparison.setText("SPD " + (state.heroSpeed > 0 ? state.heroSpeed : 100) +
                "  vs  " + (state.enemySpeed > 0 ? state.enemySpeed : enemy.speed));
            enemyIntent.setText("NEXT  " + (state.enemyIntent == null
                ? "Attack · NORMAL" : state.enemyIntent));
            statusFeedback.setText(state.combatStatus == null
                ? equippedTraitStatus(state) : state.combatStatus);
        }
        setBattleLog(history);
        battleLog.setCaretPosition(battleLog.getDocument().getLength());
    }

    private void configureAbilityButton(JButton button, GameEngine.State state, int slot) {
        String name = GameEngine.abilityName(state, slot);
        boolean unlocked = GameEngine.abilityUnlocked(state, slot);
        if ("Mage".equals(state.heroClass)) {
            int cost = GameEngine.spellCost(name);
            String compactName = "Magic Missile".equals(name) ? "Missile" : name;
            ((CombatActionButton) button).setActionLabel(compactName + " · " + cost + " MP");
            button.setEnabled(unlocked && state.mana >= cost);
        } else {
            ((CombatActionButton) button).setActionLabel(name);
            button.setEnabled(GameEngine.abilityAvailable(state, slot));
        }
        String help = GameEngine.abilityHelp(state, displayedEnemy, slot, true);
        String accessibleHelp = GameEngine.abilityHelp(state, displayedEnemy, slot, false);
        button.setToolTipText(help);
        button.putClientProperty("abilityHelp",
            GameEngine.abilityFocusSummary(state, displayedEnemy, slot));
        button.getAccessibleContext().setAccessibleDescription(accessibleHelp);
        ((CombatActionButton) button).setSpeedLabel(GameEngine.abilityTempoLabel(state, slot));
        button.setVisible(unlocked && !GameEngine.abilityPassive(state, slot));
    }

    private void installFocusedAbilityHelp(final JButton button) {
        button.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) {
                Object help = button.getClientProperty("abilityHelp");
                if (help != null) statusFeedback.setText(help.toString());
            }
        });
    }

    private void rebuildActionGrid(GameEngine.State state) {
        actionGrid.removeAll();
        actionGrid.add(attack);
        if (!"Fighter".equals(state.heroClass)) actionGrid.add(defend);
        if (GameEngine.abilityUnlocked(state, 1)) actionGrid.add(spellOne);
        if (GameEngine.abilityUnlocked(state, 2)) actionGrid.add(spellTwo);
        if (GameEngine.abilityUnlocked(state, 3) &&
                !GameEngine.abilityPassive(state, 3)) actionGrid.add(spellThree);
        actionGrid.add(potion);
        actionGrid.add(flee);
        int rows = Math.max(2, (actionGrid.getComponentCount() + 1) / 2);
        Dimension size = new Dimension(260, Math.min(174, rows * 40 + (rows - 1) * 8));
        actionGrid.setPreferredSize(size);
        actionGrid.setMaximumSize(size);
        actionGrid.revalidate();
        actionGrid.repaint();
    }

    /**
     * Keeps the class combat loop in the primary action area. Status chips remain
     * useful for duration-bearing effects, but resource readiness should never
     * require the player to scan a secondary row.
     */
    private void updateResourceMeter(GameEngine.State state) {
        String label;
        String hint;
        int value;
        int maximum;
        Color color = HeroVisualTheme.forBuild(state.race, state.heroClass).resourceColor();
        if ("Mage".equals(state.heroClass)) {
            label = "MANA";
            hint = "Channel Ward restores Mana";
            value = state.mana;
            maximum = state.maxMana;
        } else if ("Rogue".equals(state.heroClass)) {
            label = "MOMENTUM";
            hint = "Attack, evade, or enter Stealth";
            value = state.momentum;
            maximum = state.maxMomentum;
        } else if ("Hunter".equals(state.heroClass)) {
            label = "FOCUS";
            hint = "Aim, hit, or evade enemy attacks";
            value = state.focus;
            maximum = state.maxFocus;
        } else {
            label = "RAGE";
            hint = "Attack or endure damage";
            value = state.rage;
            maximum = state.maxRage;
        }
        resourceMeter = new StatusBar(color);
        resourceMeter.setMaximum(Math.max(1, maximum));
        resourceMeter.setValue(Math.max(0, value));
        resourceMeter.setString(label + "   " + value + " / " + maximum);
        resourceMeter.setToolTipText(hint);
        resourceMeter.getAccessibleContext().setAccessibleName(label + " resource");
        resourceMeter.getAccessibleContext().setAccessibleDescription(
            value + " of " + maximum + ". " + hint + ".");
        resourceHost.removeAll();
        resourceHost.add(resourceMeter, BorderLayout.CENTER);
        resourceHost.revalidate();
        resourceHost.repaint();
    }

    private String equippedTraitStatus(GameEngine.State state) {
        if (state == null || state.inventory == null) return "READY";
        for (GameEngine.Item item : state.inventory) {
            if (item.name.equals(state.equippedWeapon)) {
                return GameEngine.weaponTraitName(item) + " · " +
                    GameEngine.proficiencyLabel(GameEngine.equippedWeaponProficiency(state));
            }
        }
        return "UNARMED · READY";
    }

    private void setBattleLog(String history) {
        StyledDocument document = battleLog.getStyledDocument();
        try {
            document.remove(0, document.getLength());
            String[] lines = (history == null ? "" : history).split("(?<=\\n)");
            for (String line : lines) {
                document.insertString(document.getLength(), line, logStyle(line));
            }
        } catch (BadLocationException ignored) {
            battleLog.setText(history == null ? "" : history);
        }
    }

    private SimpleAttributeSet logStyle(String line) {
        String text = line == null ? "" : line.toLowerCase();
        SimpleAttributeSet style = new SimpleAttributeSet();
        Color color = new Color(204, 192, 174);
        boolean emphasized = false;
        if (text.contains("critical strike")) {
            color = UiTheme.GOLD_LIGHT;
            emphasized = true;
        } else if (text.contains("stun") || text.contains("concussive")) {
            color = new Color(255, 205, 92);
            emphasized = true;
        } else if (text.contains("bleeding edge") || text.contains("wound")) {
            color = new Color(225, 82, 82);
            emphasized = true;
        } else if (text.contains("armor piercing") || text.contains("sundering")) {
            color = new Color(239, 154, 79);
            emphasized = true;
        } else if (text.contains("balanced guard") || text.contains("arcane focus")) {
            color = new Color(120, 193, 231);
            emphasized = true;
        } else if (text.contains("precision shot")) {
            color = new Color(238, 205, 105);
            emphasized = true;
        } else if (text.contains("fireball")) {
            color = new Color(239, 116, 72);
            emphasized = true;
        } else if (text.contains("ice spike")) {
            color = new Color(113, 196, 244);
            emphasized = true;
        } else if (text.contains("magic missile") || text.contains("you cast")) {
            color = new Color(190, 139, 244);
            emphasized = true;
        } else if (text.contains("hits you") || text.contains("you have fallen")) {
            color = new Color(232, 92, 92);
            emphasized = true;
        } else if (text.contains("you strike") || text.contains("you attack")) {
            color = new Color(224, 177, 91);
            emphasized = true;
        } else if (text.contains("brace") || text.contains("defend") ||
                text.contains("shield") || text.contains("ward") ||
                text.contains("take aim") || text.contains("armor absorbs")) {
            color = new Color(105, 181, 219);
            emphasized = true;
        } else if (text.contains("stealth") || text.contains("shadows") ||
                text.contains("precision shot")) {
            color = new Color(177, 139, 222);
            emphasized = true;
        } else if (text.contains("recover") || text.contains("potion") ||
                text.contains("restored")) {
            color = new Color(101, 190, 119);
            emphasized = true;
        } else if (text.contains("escape") || text.contains("flee") ||
                text.contains("retreat")) {
            color = new Color(218, 151, 81);
            emphasized = true;
        } else if (text.contains("defeated") || text.contains("collect") ||
                text.contains("victory") || text.contains("saved the realm")) {
            color = UiTheme.GOLD_LIGHT;
            emphasized = true;
        }
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, emphasized);
        StyleConstants.setFontFamily(style,
            UiTheme.body(Font.PLAIN, COMBAT_LOG_FONT_SIZE).getFamily());
        StyleConstants.setFontSize(style, COMBAT_LOG_FONT_SIZE);
        StyleConstants.setLineSpacing(style, COMBAT_LOG_LINE_SPACING);
        StyleConstants.setSpaceBelow(style, 2f);
        return style;
    }

    /** Converts the catalog's compact trait string into Figma-style status chips. */
    private void updateTraitChips(EncounterCatalog.Profile profile, GameEngine.State state) {
        traitChips.removeAll();
        statusChips.removeAll();
        String[] values = profile.tags == null ? new String[0] :
            profile.tags.trim().split("\\s{2,}");
        Color fill = profile.tier == EncounterCatalog.Tier.BOSS
            ? new Color(67, 25, 25)
            : (profile.tier == EncounterCatalog.Tier.ELITE
                ? new Color(64, 39, 22) : new Color(58, 46, 24));
        Color copy = profile.tier == EncounterCatalog.Tier.BOSS
            ? new Color(255, 181, 162)
            : (profile.tier == EncounterCatalog.Tier.ELITE
                ? new Color(245, 189, 116) : new Color(242, 214, 117));
        for (String value : values) {
            if (!value.trim().isEmpty()) {
                traitChips.add(new TraitChip(value.trim(), fill, profile.accent, copy));
            }
        }
        for (String status : GameEngine.activeCombatStatuses(state)) {
            Color accent = status.contains("RAGE") ? new Color(225, 82, 62) :
                (status.contains("MOMENTUM") ? new Color(167, 112, 214) :
                (status.contains("FOCUS") || status.contains("MARKED") ||
                    status.contains("PINNED") ? new Color(111, 190, 111) :
                (status.contains("BLEED") ? new Color(225, 82, 82) :
                (status.contains("ARMOR") ? new Color(239, 154, 79) :
                (status.contains("STUN") ? new Color(255, 205, 92) :
                new Color(105, 181, 219))))));
            statusChips.add(new TraitChip(status, new Color(37, 29, 25), accent, accent));
        }
        traitChips.revalidate();
        traitChips.repaint();
        statusChips.revalidate();
        statusChips.repaint();
    }

    int traitChipCountForTest() {
        return traitChips.getComponentCount();
    }

    boolean hasStatusChipForTest(String value) {
        for (java.awt.Component component : statusChips.getComponents()) {
            if (component instanceof TraitChip &&
                    ((TraitChip) component).getText().contains(value)) return true;
        }
        return false;
    }

    Color logColorForTextForTest(String text) {
        int index = battleLog.getText().indexOf(text);
        if (index < 0) return null;
        return StyleConstants.getForeground(
            battleLog.getStyledDocument().getCharacterElement(index).getAttributes());
    }

    void awaitArtworkForSnapshot() {
        stage.awaitArtworkForSnapshot();
    }

    boolean logIsBoldForTextForTest(String text) {
        int index = battleLog.getText().indexOf(text);
        return index >= 0 && StyleConstants.isBold(
            battleLog.getStyledDocument().getCharacterElement(index).getAttributes());
    }

    int logFontSizeForTextForTest(String text) {
        int index = battleLog.getText().indexOf(text);
        return index < 0 ? 0 : StyleConstants.getFontSize(
            battleLog.getStyledDocument().getCharacterElement(index).getAttributes());
    }

    float logLineSpacingForTextForTest(String text) {
        int index = battleLog.getText().indexOf(text);
        return index < 0 ? 0f : StyleConstants.getLineSpacing(
            battleLog.getStyledDocument().getCharacterElement(index).getAttributes());
    }

    String defenseActionLabelForTest() {
        return ((CombatActionButton) defend).actionLabel;
    }

    boolean defenseActionVisibleForTest() { return defend.isVisible(); }

    String resourceMeterTextForTest() {
        return resourceMeter == null ? "" : resourceMeter.getString();
    }

    String resourceMeterHelpForTest() {
        return resourceMeter == null ? "" : resourceMeter.getToolTipText();
    }

    Color heroActionAccentForTest() {
        Object accent = attack.getClientProperty("thechosenquest.button.accent");
        return accent instanceof Color ? (Color) accent : null;
    }

    Color resourceColorForTest() {
        return resourceMeter == null ? null : resourceMeter.getForeground();
    }

    String attackSpeedLabelForTest() {
        return ((CombatActionButton) attack).speedLabel;
    }

    String spellSpeedLabelForTest() {
        return ((CombatActionButton) spellOne).speedLabel;
    }

    String statusFeedbackForTest() { return statusFeedback.getText(); }

    String abilityTooltipForTest(int slot) {
        JButton button = slot == 1 ? spellOne : (slot == 2 ? spellTwo : spellThree);
        return button.getToolTipText();
    }

    String abilityAccessibleHelpForTest(int slot) {
        JButton button = slot == 1 ? spellOne : (slot == 2 ? spellTwo : spellThree);
        return button.getAccessibleContext().getAccessibleDescription();
    }

    int visibleAbilityCountForTest() {
        int count = 0;
        if (spellOne.isVisible()) count++;
        if (spellTwo.isVisible()) count++;
        if (spellThree.isVisible()) count++;
        return count;
    }

    String timelineForTest() {
        return timeline.getText();
    }

    private static final class CombatActionButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final String shortcut;
        private String actionLabel;
        private String speedLabel = "NORMAL";

        CombatActionButton(String shortcut, String actionLabel) {
            super("");
            this.shortcut = shortcut;
            this.actionLabel = actionLabel;
        }

        void setActionLabel(String value) {
            actionLabel = value;
            repaint();
        }

        void setSpeedLabel(String value) {
            speedLabel = value == null ? "NORMAL" : value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font labelFont = UiTheme.body(Font.PLAIN, 10);
            Font keyFont = UiTheme.body(Font.BOLD, 10);
            Font speedFont = UiTheme.body(Font.BOLD, 7);
            FontMetrics metrics = g.getFontMetrics(labelFont);
            int baseline = Math.max(metrics.getAscent() + 5, getHeight() / 2);
            g.setFont(keyFont);
            g.setColor(isEnabled() ? UiTheme.GOLD_LIGHT : UiTheme.MUTED);
            g.drawString(shortcut, 11, baseline);
            g.setColor(isEnabled() ? new Color(103, 78, 58) : new Color(66, 57, 50));
            g.drawLine(28, 8, 28, Math.max(8, getHeight() - 9));
            g.setFont(labelFont);
            g.setColor(getForeground());
            g.drawString(actionLabel, 38, baseline);
            g.setFont(speedFont);
            g.setColor("SLOW".equals(speedLabel) ? new Color(224, 113, 78) :
                ("FAST".equals(speedLabel) ? new Color(105, 190, 119) : UiTheme.MUTED));
            g.drawString(speedLabel, 38, Math.min(getHeight() - 5, baseline + 11));
            g.dispose();
        }
    }

    /** Small rounded trait token used consistently by every encounter tier. */
    private static final class TraitChip extends JLabel {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        private final Color stroke;

        TraitChip(String text, Color fill, Color stroke, Color copy) {
            super(text);
            this.fill = fill;
            this.stroke = stroke;
            setOpaque(false);
            setForeground(copy);
            setFont(UiTheme.body(Font.BOLD, 9));
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            int width = Math.max(1, getWidth() - 1);
            int height = Math.max(1, getHeight() - 1);
            g.setColor(fill);
            g.fillRoundRect(0, 0, width, height, height, height);
            g.setColor(stroke);
            g.drawRoundRect(0, 0, width, height, height, height);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    void flashDamage(final boolean playerWasHit) {
        Color flash = playerWasHit ? new Color(255, 92, 73) : new Color(255, 218, 116);
        flashBorder(flash);
    }

    void setReducedMotion(boolean value) {
        reducedMotion = value;
        stage.setReducedMotion(value);
    }

    void playPlayerAttack(int healthBefore, int healthAfter, PlayerAttackStyle style,
                          boolean critical, Runnable impact, Runnable complete) {
        health.setMaximum(Math.max(1, Math.max(health.getMaximum(), healthBefore)));
        health.setValue(Math.max(0, healthBefore));
        healthCopy.setText(healthBefore + " / " + health.getMaximum());
        stage.play(CombatEffect.forPlayer(style),
            Math.max(0, healthBefore - healthAfter), critical, new Runnable() {
                public void run() {
                    animateEnemyHealth(healthBefore, healthAfter);
                    if (impact != null) impact.run();
                }
            }, complete);
    }

    void playDefend(Runnable complete) {
        stage.play(CombatEffect.DEFEND, 0, false, null, complete);
    }

    void playEnemyAttack(GameEngine.EnemyTurnEvent event, Runnable impact,
                         Runnable complete) {
        stage.play(event.missed ? CombatEffect.EVADE : CombatEffect.ENEMY,
            event.damage, false, impact, complete);
    }

    private void animateEnemyHealth(final int from, final int to) {
        if (reducedMotion || from == to) {
            health.setValue(Math.max(0, to));
            healthCopy.setText(Math.max(0, to) + " / " + health.getMaximum());
            return;
        }
        final long started = System.currentTimeMillis();
        Timer meterTimer = new Timer(16, null);
        meterTimer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                double progress = Math.min(1d,
                    (System.currentTimeMillis() - started) / 220d);
                double eased = 1d - Math.pow(1d - progress, 3d);
                int value = (int) Math.round(from + (to - from) * eased);
                health.setValue(Math.max(0, value));
                healthCopy.setText(Math.max(0, value) + " / " + health.getMaximum());
                if (progress >= 1d) ((Timer) event.getSource()).stop();
            }
        });
        meterTimer.start();
    }

    void flashEvade() {
        flashBorder(new Color(105, 181, 219));
    }

    private void flashBorder(Color flash) {
        setBorder(BorderFactory.createLineBorder(flash, 5));
        Timer timer = new Timer(180, new ActionListener() {
            public void actionPerformed(ActionEvent event) { setBorder(normalBorder); }
        });
        timer.setRepeats(false);
        timer.start();
    }

    void setCombatActionsEnabled(boolean enabled) {
        attack.setEnabled(enabled);
        if (defend.isVisible()) defend.setEnabled(enabled);
        flee.setEnabled(enabled);
        if (!enabled) {
            spellOne.setEnabled(false);
            spellTwo.setEnabled(false);
            spellThree.setEnabled(false);
            potion.setEnabled(false);
        }
    }

    void showEnemyTurnBeat(GameEngine.EnemyTurnEvent event, int number, int total,
                           String visibleHistory) {
        String count = total > 1 ? "ATTACK " + number + " OF " + total + "  ·  " : "";
        turnBeat.setText(count + event.actionName.toUpperCase() + "  ·  " +
            event.tempoLabel);
        turnBeat.setOpaque(true);
        turnBeat.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        turnBeat.getAccessibleContext().setAccessibleDescription(
            "Enemy " + event.actionName + ", " + event.tempoLabel.toLowerCase() +
            " tempo, attack " + number + " of " + total);
        setBattleLog(visibleHistory);
        battleLog.setCaretPosition(battleLog.getDocument().getLength());
    }

    void clearEnemyTurnBeat() {
        turnBeat.setText("");
        turnBeat.setOpaque(false);
        turnBeat.setBorder(BorderFactory.createEmptyBorder());
    }

    boolean triggerShortcut(char key) {
        JButton target = null;
        switch (key) {
            case '1': target = attack; break;
            case '2': target = defend; break;
            case '3': target = spellOne; break;
            case '4': target = spellTwo; break;
            case '5': target = spellThree; break;
            case '6': target = potion; break;
            case '7': target = flee; break;
            default: return false;
        }
        if (!target.isVisible() || !target.isEnabled()) return false;
        target.doClick();
        return true;
    }

    static String assetFor(String enemyName) {
        return EncounterCatalog.forEnemy(enemyName).artwork;
    }

    static String sceneFor(GameEngine.TileType tile) {
        if (tile == GameEngine.TileType.LAKE) {
            return "/assets/scenes/moonwater-crossing.png";
        }
        if (tile == GameEngine.TileType.CRYPT) {
            return "/assets/scenes/forgotten-crypt.png";
        }
        if (tile == GameEngine.TileType.SPIDER_NEST) {
            return "/assets/scenes/ashweb-nest.png";
        }
        return "/assets/scenes/alshira-ruins.png";
    }

    private enum CombatEffect {
        FIGHTER, MAGE, ROGUE, HUNTER, SPELL, FIREBALL, ICE_SPIKE,
        SHIELD_BASH, HEAVY_STRIKE, OFFHAND_STRIKE, PINNING_SHOT,
        WEAPON_MASTERY, EXECUTE, HUNTERS_MARK, ENEMY, DEFEND, EVADE;

        static CombatEffect forPlayer(PlayerAttackStyle style) {
            return CombatEffect.valueOf(style.name());
        }

        boolean isPlayerAttack() {
            return this == FIGHTER || this == MAGE || this == ROGUE ||
                this == HUNTER || this == SPELL || this == FIREBALL ||
                this == ICE_SPIKE || this == SHIELD_BASH ||
                this == HEAVY_STRIKE || this == OFFHAND_STRIKE ||
                this == PINNING_SHOT || this == WEAPON_MASTERY ||
                this == EXECUTE || this == HUNTERS_MARK;
        }
    }

    private static final class CombatStage extends JLayeredPane {
        private static final long serialVersionUID = 1L;
        private final AssetImagePanel backdrop = new AssetImagePanel(null, true);
        private final AssetImagePanel portrait = new AssetImagePanel(null, true);
        private final EffectOverlay effects = new EffectOverlay();
        private final JPanel veil = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                graphics.setColor(new Color(0, 0, 0, 125));
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        CombatStage() {
            Dimension size = new Dimension(UiTheme.CENTER_WIDTH, UiTheme.COMBAT_STAGE_HEIGHT);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setAlignmentX(LEFT_ALIGNMENT);
            setOpaque(true);
            setBackground(new Color(12, 12, 14));
            veil.setOpaque(false);
            add(backdrop, Integer.valueOf(DEFAULT_LAYER));
            add(veil, Integer.valueOf(PALETTE_LAYER));
            add(portrait, Integer.valueOf(MODAL_LAYER));
            add(effects, Integer.valueOf(DRAG_LAYER));
        }

        void setReducedMotion(boolean value) {
            effects.reducedMotion = value;
        }

        void play(CombatEffect effect, int amount, boolean critical,
                  Runnable impact, Runnable complete) {
            effects.play(effect, amount, critical, impact, complete);
        }

        void setProfile(EncounterCatalog.Profile profile, String tileScene) {
            effects.setTier(profile.tier);
            boolean layered = profile.tier != EncounterCatalog.Tier.BOSS;
            backdrop.setCover(true);
            // Dragon art is wider than the combat viewport and places horns near
            // the top edge. Bias boss crops upward so their faces remain intact.
            backdrop.setCropAnchorY(layered ? .5d : .08d);
            backdrop.setFlipHorizontal(profile.mirrorArtwork);
            portrait.setFlipHorizontal(profile.mirrorArtwork);
            backdrop.setResourceAsync(layered ? tileScene : profile.artwork);
            portrait.setResourceAsync(profile.artwork);
            portrait.setVisible(layered);
            veil.setVisible(layered);
            portrait.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(profile.accent, 2),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            setBorder(profile.tier == EncounterCatalog.Tier.STANDARD
                ? BorderFactory.createEmptyBorder()
                : BorderFactory.createLineBorder(profile.accent, 2));
            repaint();
        }

        void awaitArtworkForSnapshot() {
            backdrop.awaitResource();
            portrait.awaitResource();
        }

        @Override
        public void doLayout() {
            backdrop.setBounds(0, 0, getWidth(), getHeight());
            veil.setBounds(0, 0, getWidth(), getHeight());
            int targetOffset = portrait.isVisible() ? effects.targetOffset() : 0;
            portrait.setBounds((getWidth() - 288) / 2 + targetOffset,
                31 + effects.shakeOffset(), 288, 288);
            effects.setBounds(0, 0, getWidth(), getHeight());
        }

        private final class EffectOverlay extends JPanel {
            private static final long serialVersionUID = 1L;
            private Timer timer;
            private CombatEffect effect;
            private int amount;
            private boolean critical;
            private boolean reducedMotion;
            private boolean impacted;
            private double progress;
            private EncounterCatalog.Tier tier = EncounterCatalog.Tier.STANDARD;
            private Runnable impact;
            private Runnable complete;

            EffectOverlay() {
                setOpaque(false);
                setVisible(false);
                CombatVfx.preloadAsync();
            }

            void setTier(EncounterCatalog.Tier value) {
                tier = value == null ? EncounterCatalog.Tier.STANDARD : value;
            }

            void play(CombatEffect nextEffect, int nextAmount, boolean nextCritical,
                      Runnable nextImpact, Runnable nextComplete) {
                if (timer != null && timer.isRunning()) timer.stop();
                effect = nextEffect;
                amount = nextAmount;
                critical = nextCritical;
                impact = nextImpact;
                complete = nextComplete;
                impacted = false;
                progress = 0d;
                setVisible(true);
                final long started = System.currentTimeMillis();
                final int duration = animationDurationForTier(tier, reducedMotion);
                timer = new Timer(16, new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        double rawProgress = Math.min(1d,
                            (System.currentTimeMillis() - started) / (double) duration);
                        progress = visualProgress(rawProgress);
                        if (!impacted && rawProgress >= .48d) {
                            impacted = true;
                            if (impact != null) impact.run();
                        }
                        // The stage has fixed geometry. Updating its child bounds
                        // directly avoids an expensive full Swing layout pass on
                        // every 16 ms animation tick.
                        CombatStage.this.doLayout();
                        CombatStage.this.repaint();
                        if (rawProgress >= 1d) {
                            ((Timer) event.getSource()).stop();
                            setVisible(false);
                            CombatStage.this.doLayout();
                            CombatStage.this.repaint();
                            if (complete != null) complete.run();
                        }
                    }
                });
                timer.start();
            }

            /** Boss impacts hold briefly at contact to make their weight readable. */
            private double visualProgress(double raw) {
                if (tier != EncounterCatalog.Tier.BOSS || raw < .48d) return raw;
                if (raw < .60d) return .48d;
                return .48d + ((raw - .60d) / .40d) * .52d;
            }

            int targetOffset() {
                if (reducedMotion || effect == null || !isVisible()) return 0;
                if (effect == CombatEffect.ENEMY) {
                    int lunge = tier == EncounterCatalog.Tier.BOSS ? 36 :
                        (tier == EncounterCatalog.Tier.ELITE ? 28 : 22);
                    if (progress < .48d) return (int) Math.round(-lunge * ease(progress / .48d));
                    return (int) Math.round(-lunge * (1d - ease((progress - .48d) / .52d)));
                }
                if (effect.isPlayerAttack() && progress >= .48d && progress < .78d) {
                    int recoil = tier == EncounterCatalog.Tier.BOSS ? 20 :
                        (tier == EncounterCatalog.Tier.ELITE ? 16 : 13);
                    return (int) Math.round(recoil * (1d - (progress - .48d) / .30d));
                }
                return 0;
            }

            int shakeOffset() {
                if (reducedMotion || !impacted || progress >= .76d ||
                        effect == CombatEffect.EVADE || effect == CombatEffect.DEFEND) return 0;
                double strength = shakeStrengthForTier(tier);
                if (critical) strength = Math.max(strength, 6d);
                return (int) Math.round(Math.sin(progress * 130d) * strength *
                    (1d - (progress - .48d) / .28d));
            }

            private double ease(double value) {
                double clamped = Math.max(0d, Math.min(1d, value));
                return 1d - Math.pow(1d - clamped, 3d);
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                if (effect == null) return;
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                paintImpactTint(g);
                paintTierShockwave(g);
                switch (effect) {
                    case FIGHTER: paintFighterSlash(g); break;
                    case SHIELD_BASH:
                    case HEAVY_STRIKE:
                    case WEAPON_MASTERY:
                    case EXECUTE:
                        paintFighterSlash(g); break;
                    case MAGE: paintMagePulse(g); break;
                    case ROGUE: paintRogueSlashes(g); break;
                    case OFFHAND_STRIKE: paintRogueSlashes(g); break;
                    case HUNTER: paintHunterArrow(g); break;
                    case PINNING_SHOT:
                    case HUNTERS_MARK:
                        paintHunterArrow(g); break;
                    case SPELL:
                    case FIREBALL:
                    case ICE_SPIKE: paintSpellProjectile(g); break;
                    case ENEMY: paintEnemyArc(g); break;
                    case DEFEND: paintDefendWard(g); break;
                    default: break;
                }
                paintAbilitySignature(g);
                if (critical && effect.isPlayerAttack()) paintCriticalBurst(g);
                g.setComposite(AlphaComposite.SrcOver);

                if (progress >= .48d && amount > 0) {
                    double rise = ease((progress - .48d) / .52d);
                    String copy = effect == CombatEffect.EVADE ? "DODGE" : "-" + amount;
                    Font font = UiTheme.display(critical ? 34 : 28);
                    g.setFont(font);
                    FontMetrics metrics = g.getFontMetrics(font);
                    int x = effect == CombatEffect.ENEMY ? 78 :
                        (getWidth() - metrics.stringWidth(copy)) / 2;
                    int y = (int) Math.round(132 - rise * 38d);
                    g.setColor(new Color(18, 12, 10, 190));
                    g.drawString(copy, x + 2, y + 2);
                    g.setColor(resultColor());
                    g.drawString(copy, x, y);
                    if (critical) {
                        String label = "CRITICAL";
                        g.setFont(UiTheme.body(Font.BOLD, 10));
                        g.drawString(label, (getWidth() - g.getFontMetrics().stringWidth(label)) / 2,
                            y + 19);
                    }
                } else if (effect == CombatEffect.EVADE && progress >= .42d) {
                    g.setFont(UiTheme.display(26));
                    g.setColor(new Color(136, 207, 242));
                    g.drawString("DODGE", 52, 112);
                }
                g.dispose();
            }

            private void paintImpactTint(Graphics2D g) {
                double window = Math.max(0d, 1d - Math.abs(progress - .55d) / .20d);
                if (window <= 0d) return;
                float strength = tier == EncounterCatalog.Tier.BOSS ? .38f :
                    (tier == EncounterCatalog.Tier.ELITE ? .30f : .24f);
                g.setComposite(AlphaComposite.SrcOver.derive((float) (strength * window)));
                g.setColor(effectColor());
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setComposite(AlphaComposite.SrcOver);
            }

            private void paintTierShockwave(Graphics2D g) {
                if (!impacted || tier == EncounterCatalog.Tier.STANDARD || progress > .82d) return;
                double phase = Math.max(0d, (progress - .48d) / .34d);
                int radius = (int) Math.round(42d + ease(phase) *
                    (tier == EncounterCatalog.Tier.BOSS ? 100d : 66d));
                float alpha = (float) Math.max(0d, .70d * (1d - phase));
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g.setStroke(new BasicStroke(tier == EncounterCatalog.Tier.BOSS ? 8f : 5f));
                g.setColor(tier == EncounterCatalog.Tier.BOSS
                    ? new Color(220, 55, 47) : UiTheme.GOLD);
                int cx = getWidth() / 2;
                int cy = 162;
                g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
                g.setComposite(AlphaComposite.SrcOver);
            }

            private void paintFighterSlash(Graphics2D g) {
                if (paintSprite(g, CombatVfx.Sequence.FIGHTER, .18d, .46d, .76d,
                        getWidth() / 2, 163, critical ? 292 : 258,
                        critical ? 346 : 306, -.05d)) return;
                float alpha = effectAlpha(.28d, .50d, .74d);
                if (alpha <= 0f) return;
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g.setStroke(new BasicStroke(critical ? 10f : 7f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(critical ? new Color(255, 225, 116) : new Color(239, 236, 215));
                int cx = getWidth() / 2;
                g.drawLine(cx - 96, 230, cx + 82, 82);
                g.setStroke(new BasicStroke(2f));
                g.setColor(new Color(255, 248, 205));
                g.drawLine(cx - 74, 239, cx + 96, 98);
            }

            private void paintMagePulse(Graphics2D g) {
                if (paintSprite(g, CombatVfx.Sequence.MAGE, .14d, .48d, .80d,
                        getWidth() / 2, 163, critical ? 270 : 238,
                        critical ? 320 : 282, 0d)) return;
                float alpha = effectAlpha(.16d, .50d, .80d);
                if (alpha <= 0f) return;
                double pulse = reducedMotion ? 1d : ease((progress - .16d) / .48d);
                int radius = (int) Math.round(18d + pulse * (critical ? 82d : 64d));
                int cx = getWidth() / 2;
                int cy = 162;
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g.setColor(new Color(72, 145, 242, 58));
                g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
                g.setStroke(new BasicStroke(critical ? 7f : 4f));
                g.setColor(new Color(92, 166, 255));
                g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
            }

            private void paintRogueSlashes(Graphics2D g) {
                boolean first = paintSprite(g, CombatVfx.Sequence.ROGUE,
                    .14d, .38d, .62d, getWidth() / 2 - 18, 158,
                    critical ? 242 : 214, critical ? 288 : 254, -.10d);
                boolean second = paintSprite(g, CombatVfx.Sequence.ROGUE,
                    .30d, .52d, .76d, getWidth() / 2 + 22, 173,
                    critical ? 242 : 214, critical ? 288 : 254, .13d);
                if (first && second) return;
                int cx = getWidth() / 2;
                paintRogueSlash(g, cx - 92, 224, cx + 76, 92,
                    effectAlpha(.22d, .43d, .66d));
                paintRogueSlash(g, cx - 76, 242, cx + 92, 110,
                    effectAlpha(.34d, .55d, .76d));
            }

            private void paintRogueSlash(Graphics2D g, int x1, int y1, int x2, int y2,
                                         float alpha) {
                if (alpha <= 0f) return;
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g.setStroke(new BasicStroke(critical ? 7f : 4f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(critical ? new Color(255, 225, 116) : new Color(235, 226, 203));
                g.drawLine(x1, y1, x2, y2);
            }

            private void paintHunterArrow(Graphics2D g) {
                if (progress < .10d || progress > .76d) return;
                double travel = reducedMotion ? 1d : ease((progress - .10d) / .48d);
                int x = (int) Math.round(55d + (getWidth() / 2d - 55d) * travel);
                int y = 162;
                if (paintSprite(g, CombatVfx.Sequence.HUNTER, .10d, .48d, .76d,
                        x, y, critical ? 194 : 166, critical ? 230 : 198, 0d)) return;
                g.setComposite(AlphaComposite.SrcOver.derive(effectAlpha(.10d, .50d, .76d)));
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(239, 211, 111));
                g.drawLine(x - 92, y, x, y);
                g.drawLine(x - 17, y - 15, x, y);
                g.drawLine(x - 17, y + 15, x, y);
            }

            private void paintSpellProjectile(Graphics2D g) {
                if (progress > .74d) return;
                double travel = reducedMotion ? 1d : ease(Math.max(0d, (progress - .10d) / .48d));
                int x = (int) Math.round(55d + (getWidth() / 2d - 55d) * travel);
                int y = 176;
                CombatVfx.Sequence sequence = effect == CombatEffect.FIREBALL
                    ? CombatVfx.Sequence.FIREBALL :
                    (effect == CombatEffect.ICE_SPIKE
                        ? CombatVfx.Sequence.ICE_SPIKE : CombatVfx.Sequence.SPELL);
                int spriteWidth = effect == CombatEffect.FIREBALL ? 228 :
                    (effect == CombatEffect.ICE_SPIKE ? 196 : 206);
                int spriteHeight = effect == CombatEffect.FIREBALL ? 132 :
                    (effect == CombatEffect.ICE_SPIKE ? 196 : 244);
                if (paintSprite(g, sequence, .08d, .46d, .76d,
                        x, y, critical ? (int) (spriteWidth * 1.16d) : spriteWidth,
                        critical ? (int) (spriteHeight * 1.16d) : spriteHeight, 0d)) return;
                int radius = critical ? 29 : 22;
                g.setColor(new Color(119, 74, 214, 95));
                g.fillOval(x - radius - 10, y - radius - 10,
                    (radius + 10) * 2, (radius + 10) * 2);
                g.setColor(new Color(211, 179, 255));
                g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                g.setStroke(new BasicStroke(3f));
                g.setColor(new Color(143, 102, 236));
                g.drawOval(x - radius - 7, y - radius - 7,
                    (radius + 7) * 2, (radius + 7) * 2);
            }

            private void paintEnemyArc(Graphics2D g) {
                CombatVfx.Sequence sequence = tier == EncounterCatalog.Tier.BOSS
                    ? CombatVfx.Sequence.BOSS : CombatVfx.Sequence.ENEMY;
                int width = tier == EncounterCatalog.Tier.BOSS ? 320 :
                    (tier == EncounterCatalog.Tier.ELITE ? 258 : 226);
                int height = tier == EncounterCatalog.Tier.BOSS ? 380 :
                    (tier == EncounterCatalog.Tier.ELITE ? 306 : 268);
                if (paintSprite(g, sequence, .16d, .48d, .78d,
                        tier == EncounterCatalog.Tier.BOSS ? 138 : 112,
                        168, width, height, .08d)) return;
                float alpha = effectAlpha(.22d, .50d, .76d);
                if (alpha <= 0f) return;
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g.setStroke(new BasicStroke(tier == EncounterCatalog.Tier.BOSS ? 10f : 7f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(233, 78, 64));
                g.drawArc(38, 82, getWidth() / 2, 210, 225, 105);
            }

            private void paintCriticalBurst(Graphics2D g) {
                int size = tier == EncounterCatalog.Tier.BOSS ? 330 : 274;
                paintSprite(g, CombatVfx.Sequence.CRITICAL, .34d, .52d, .82d,
                    getWidth() / 2, 161, size, (int) Math.round(size * 1.18d), 0d);
            }

            /**
             * Paint one cached animation frame. Returning false only means the
             * prototype sequence is unavailable, allowing the old vector effect
             * to serve as a safe fallback instead of breaking an attack.
             */
            private boolean paintSprite(Graphics2D g, CombatVfx.Sequence sequence,
                                        double start, double peak, double end,
                                        int centerX, int centerY, int width, int height,
                                        double rotation) {
                double frameProgress = (progress - start) / Math.max(.001d, end - start);
                BufferedImage image = CombatVfx.frame(sequence, frameProgress);
                if (image == null) return false;
                float alpha = effectAlpha(start, peak, end);
                if (alpha <= 0f) return true;

                Graphics2D sprite = (Graphics2D) g.create();
                sprite.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                sprite.setComposite(AlphaComposite.SrcOver.derive(alpha));
                sprite.translate(centerX, centerY);
                if (!reducedMotion) sprite.rotate(rotation);
                sprite.drawImage(image, -width / 2, -height / 2, width, height, null);
                sprite.dispose();
                return true;
            }

            private void paintDefendWard(Graphics2D g) {
                if (paintSprite(g, CombatVfx.Sequence.DEFEND, 0d, .50d, 1d,
                        86, 174, 166, 196, 0d)) return;
                float alpha = effectAlpha(0d, .50d, 1d);
                if (alpha <= 0f) return;
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                int cx = 86;
                g.setColor(new Color(77, 156, 211, 65));
                g.fillOval(cx - 48, 116, 96, 116);
                g.setStroke(new BasicStroke(4f));
                g.setColor(new Color(130, 203, 242));
                g.drawOval(cx - 48, 116, 96, 116);
            }

            /** Lightweight authored overlays keep martial abilities visually distinct. */
            private void paintAbilitySignature(Graphics2D g) {
                if (progress < .24d || progress > .84d) return;
                float alpha = effectAlpha(.24d, .50d, .84d);
                int cx = getWidth() / 2;
                int cy = 162;
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                if (effect == CombatEffect.SHIELD_BASH) {
                    g.setColor(new Color(116, 196, 235));
                    g.setStroke(new BasicStroke(7f));
                    g.drawArc(cx - 54, cy - 62, 108, 124, 205, 130);
                    g.drawLine(cx, cy - 48, cx, cy + 42);
                } else if (effect == CombatEffect.HEAVY_STRIKE) {
                    g.setColor(new Color(255, 178, 74));
                    g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                    g.drawLine(cx - 116, cy - 70, cx + 102, cy + 72);
                } else if (effect == CombatEffect.OFFHAND_STRIKE) {
                    g.setColor(new Color(192, 137, 236));
                    g.setStroke(new BasicStroke(4f));
                    g.drawLine(cx - 104, cy + 70, cx + 80, cy - 68);
                    g.drawLine(cx - 80, cy + 78, cx + 104, cy - 60);
                } else if (effect == CombatEffect.PINNING_SHOT ||
                        effect == CombatEffect.HUNTERS_MARK) {
                    int radius = effect == CombatEffect.HUNTERS_MARK ? 62 : 42;
                    g.setColor(new Color(116, 207, 112));
                    g.setStroke(new BasicStroke(effect == CombatEffect.HUNTERS_MARK ? 6f : 3f));
                    g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
                    g.drawLine(cx - radius - 16, cy, cx + radius + 16, cy);
                    g.drawLine(cx, cy - radius - 16, cx, cy + radius + 16);
                } else if (effect == CombatEffect.EXECUTE) {
                    g.setColor(new Color(239, 72, 66));
                    g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                    g.drawLine(cx - 70, cy - 72, cx + 70, cy + 72);
                    g.drawLine(cx + 70, cy - 72, cx - 70, cy + 72);
                } else if (effect == CombatEffect.WEAPON_MASTERY) {
                    g.setColor(UiTheme.GOLD_LIGHT);
                    g.setStroke(new BasicStroke(5f));
                    g.drawOval(cx - 68, cy - 68, 136, 136);
                    g.drawOval(cx - 48, cy - 48, 96, 96);
                }
                g.setComposite(AlphaComposite.SrcOver);
            }

            private float effectAlpha(double start, double peak, double end) {
                if (progress < start || progress > end) return 0f;
                if (progress <= peak) return (float) Math.min(1d,
                    (progress - start) / Math.max(.001d, peak - start));
                return (float) Math.min(1d,
                    (end - progress) / Math.max(.001d, end - peak));
            }

            private Color effectColor() {
                if (effect == CombatEffect.ENEMY) return new Color(210, 54, 45);
                if (effect == CombatEffect.MAGE) return new Color(62, 132, 235);
                if (effect == CombatEffect.FIREBALL) return new Color(229, 86, 36);
                if (effect == CombatEffect.ICE_SPIKE) return new Color(73, 165, 235);
                if (effect == CombatEffect.SPELL) return new Color(151, 103, 238);
                if (effect == CombatEffect.EVADE) return new Color(91, 177, 224);
                if (effect == CombatEffect.EXECUTE) return new Color(225, 55, 49);
                if (effect == CombatEffect.SHIELD_BASH) return new Color(87, 164, 214);
                if (effect == CombatEffect.OFFHAND_STRIKE) return new Color(159, 98, 211);
                if (effect == CombatEffect.PINNING_SHOT ||
                        effect == CombatEffect.HUNTERS_MARK) return new Color(88, 177, 92);
                return new Color(255, 211, 105);
            }

            private Color resultColor() {
                if (critical) return new Color(255, 225, 116);
                if (effect == CombatEffect.ENEMY) return new Color(255, 115, 98);
                if (effect == CombatEffect.MAGE) return new Color(105, 176, 255);
                if (effect == CombatEffect.FIREBALL) return new Color(255, 151, 79);
                if (effect == CombatEffect.ICE_SPIKE) return new Color(146, 218, 255);
                if (effect == CombatEffect.SPELL) return new Color(211, 179, 255);
                return new Color(250, 237, 205);
            }
        }
    }
}
