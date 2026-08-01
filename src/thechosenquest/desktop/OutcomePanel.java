package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** Full-screen quest outcome card based on the Figma victory composition. */
final class OutcomePanel extends JPanel {
    interface Listener {
        void onNewQuest();
        void onTitleScreen();
    }

    private static final long serialVersionUID = 1L;
    private final AssetImagePanel artwork = new AssetImagePanel(null, true);
    private final JLabel eyebrow = new JLabel("", SwingConstants.CENTER);
    private final JLabel heading = new JLabel("", SwingConstants.CENTER);
    private final JLabel subtitle = new JLabel("", SwingConstants.CENTER);
    private final JLabel heroName = new JLabel("", SwingConstants.CENTER);
    private final JLabel identity = new JLabel("", SwingConstants.CENTER);
    private final JTextArea message = new JTextArea();
    private final JPanel card = new JPanel();

    OutcomePanel(final Listener listener) {
        setLayout(new BorderLayout());
        BackgroundPanel backdrop = new BackgroundPanel("/assets/quest-background.png", 0.58f);
        backdrop.setLayout(new GridBagLayout());
        add(backdrop, BorderLayout.CENTER);

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UiTheme.SURFACE_DEEP);
        card.setPreferredSize(new Dimension(900, 820));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.GOLD, 2),
            BorderFactory.createEmptyBorder(24, 48, 24, 48)));

        eyebrow.setForeground(UiTheme.GOLD);
        eyebrow.setFont(UiTheme.display(15));
        eyebrow.setAlignmentX(CENTER_ALIGNMENT);
        heading.setForeground(UiTheme.GOLD_LIGHT);
        heading.setFont(UiTheme.title(Font.PLAIN, 34));
        heading.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.display(14));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        card.add(eyebrow);
        card.add(Box.createVerticalStrut(8));
        card.add(heading);
        card.add(Box.createVerticalStrut(5));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(16));

        JPanel artWrap = new JPanel(new GridBagLayout());
        artWrap.setOpaque(false);
        artWrap.setAlignmentX(CENTER_ALIGNMENT);
        artWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        artwork.setPreferredSize(new Dimension(220, 220));
        artwork.setMinimumSize(new Dimension(220, 220));
        artwork.setBorder(BorderFactory.createLineBorder(UiTheme.GOLD, 2));
        artWrap.add(artwork);
        card.add(artWrap);
        card.add(Box.createVerticalStrut(10));

        heroName.setForeground(UiTheme.GOLD);
        heroName.setFont(UiTheme.display(25));
        heroName.setAlignmentX(CENTER_ALIGNMENT);
        identity.setForeground(UiTheme.TEXT);
        identity.setFont(UiTheme.body(Font.BOLD, 10));
        identity.setAlignmentX(CENTER_ALIGNMENT);
        card.add(heroName);
        card.add(identity);
        card.add(Box.createVerticalStrut(14));

        message.setEditable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setRows(6);
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        message.setBackground(new Color(245, 230, 190));
        message.setForeground(new Color(42, 27, 14));
        message.setFont(UiTheme.body(Font.PLAIN, 12));
        message.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.GOLD),
            BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        message.setAlignmentX(CENTER_ALIGNMENT);
        card.add(message);
        card.add(Box.createVerticalStrut(12));

        JPanel actions = new JPanel(new GridLayout(2, 1, 0, 10));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        actions.setAlignmentX(CENTER_ALIGNMENT);
        JButton title = UiTheme.button("RETURN TO TITLE", false);
        title.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onTitleScreen(); }
        });
        JButton again = UiTheme.button("BEGIN ANOTHER QUEST", true);
        again.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onNewQuest(); }
        });
        actions.add(title);
        actions.add(again);
        card.add(actions);
        backdrop.add(card);
    }

    void showVictory(GameEngine.State state) {
        eyebrow.setText("QUEST COMPLETE");
        heading.setText("THE REALM IS SAVED");
        heading.setForeground(UiTheme.GOLD_LIGHT);
        subtitle.setText("The dragon of the southeastern crypt has been vanquished.");
        setHero(state);
        message.setText("LEVEL REACHED                         " + state.level + "\n" +
            "GOLD EARNED                           " + state.gold + "\n" +
            "EQUIPMENT COLLECTED                   " + state.inventory.size() + "\n" +
            "SPELLS MASTERED                       " + state.spells.size() + "\n\n" +
            "The realm will remember " + state.playerName + " and the quest that broke the dragon's shadow.");
    }

    void showDefeat(GameEngine.State state) {
        eyebrow.setText("THE QUEST HAS ENDED");
        heading.setText("THE CHOSEN HAS FALLEN");
        heading.setForeground(new Color(196, 76, 64));
        subtitle.setText("Every legend may be told anew.");
        setHero(state);
        message.setText("LEVEL REACHED                         " + state.level + "\n" +
            "GOLD GATHERED                         " + state.gold + "\n" +
            "EQUIPMENT COLLECTED                   " + state.inventory.size() + "\n\n" +
            state.playerName + " fell before the darkness. Begin another quest or return to the title to load a saved journey.");
    }

    private void setHero(GameEngine.State state) {
        artwork.setCover(true);
        artwork.setResourceAsync(
            CharacterArt.portrait(state.race, state.heroClass, state.gender));
        heroName.setText(state.playerName);
        identity.setText("LEVEL " + state.level + " " + state.race.toUpperCase() + " " +
            state.heroClass.toUpperCase() + " · DRAGONSLAYER");
    }
}
