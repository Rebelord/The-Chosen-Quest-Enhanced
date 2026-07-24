package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

/** Styled, offline-readable summary of the current tester release. */
final class ReleaseNotesPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color HEADER = new Color(20, 13, 9);
    private static final Color CARD = new Color(46, 34, 26);

    ReleaseNotesPanel(final Runnable onClose, final Runnable onFeedback,
                      final Runnable onBugReport) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(780, 660));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createLineBorder(UiTheme.GOLD, 2));
        add(buildHeader(onClose), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UiTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 26, 24));
        content.add(intro());
        content.add(Box.createVerticalStrut(18));
        content.add(section("COMBAT & CLASS BALANCE",
            "Fighters, Rogues, and Hunters now use Rage, Momentum, and Focus. " +
            "Second Wind is a once-per-encounter death save instead of repeatable healing. " +
            "Hunter's Mark now gates Volley, relics respect level requirements, and dragon " +
            "protection is consistent across attacks, spells, and abilities."));
        content.add(Box.createVerticalStrut(12));
        content.add(section("WORLD & EXPLORATION",
            "The full-world map is available with M, map zoom now includes a closer 5x5 " +
            "view, fog-aware objectives and rumors occupy the map rail, and movement keys " +
            "advance one deliberate tile per press. The Ashweb Nest now ends with the elite " +
            "Ashweb Matriarch."));
        content.add(Box.createVerticalStrut(12));
        content.add(section("PRESENTATION & ACCESSIBILITY",
            "Character artwork and landscape scenes scale more reliably on large screens. " +
            "Status effects, ability costs, action tempo, multi-hit turns, inventory, and " +
            "portrait frames provide clearer feedback with reduced-motion support."));
        content.add(Box.createVerticalStrut(12));
        content.add(section("AUDIO & CREDITS",
            "Exploration, safe locations, combat, and bosses now receive scene-aware music. " +
            "High-frequency UI and combat cues use attributed game-ready assets, with full " +
            "credits and licenses available in-game and in the download."));
        content.add(Box.createVerticalStrut(12));
        content.add(section("WHAT WE NEED FROM TESTERS",
            "Try different race, class, and loadout combinations. Tell us when balance feels " +
            "unfair, when an action is unclear, when audio becomes repetitive, or when a " +
            "second encounter behaves differently from the first. Please include your game " +
            "version and operating system with bug reports."));

        JScrollPane scroll = new JScrollPane(content,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        FantasyScrollBarUI.install(scroll.getVerticalScrollBar(), UiTheme.GOLD);
        add(scroll, BorderLayout.CENTER);
        add(buildFooter(onClose, onFeedback, onBugReport), BorderLayout.SOUTH);
    }

    private JPanel buildHeader(final Runnable onClose) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(17, 20, 17, 20));
        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);
        JLabel title = new JLabel("WHAT'S NEW");
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.title(Font.PLAIN, 22));
        JLabel subtitle = new JLabel(AppVersion.DISPLAY_NAME +
            "  ·  Private tester release");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.body(Font.PLAIN, 11));
        copy.add(title);
        copy.add(Box.createVerticalStrut(2));
        copy.add(subtitle);
        header.add(copy, BorderLayout.WEST);
        JButton close = new JButton(new SystemIcon(SystemIcon.Type.CLOSE, 18, UiTheme.GOLD));
        close.setPreferredSize(new Dimension(34, 34));
        close.setToolTipText("Close release notes");
        close.getAccessibleContext().setAccessibleName("Close release notes");
        UiTheme.applyButtonStyle(close, UiTheme.ButtonStyle.ICON, 7, 7);
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (onClose != null) onClose.run();
            }
        });
        header.add(close, BorderLayout.EAST);
        return header;
    }

    private JTextArea intro() {
        JTextArea copy = text("This build packages the balance, exploration, interface, " +
            "audio, and testing improvements completed since the first public beta. Human " +
            "playtesting is the purpose of this release, so feedback is part of the quest.",
            13, UiTheme.TEXT);
        copy.setRows(3);
        copy.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        return copy;
    }

    private JPanel section(String heading, String body) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(13, 15, 14, 15)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        JLabel label = new JLabel(heading);
        label.setForeground(UiTheme.GOLD_LIGHT);
        label.setFont(UiTheme.title(Font.PLAIN, 15));
        label.setAlignmentX(LEFT_ALIGNMENT);
        card.add(label);
        card.add(Box.createVerticalStrut(6));
        JTextArea copy = text(body, 12, UiTheme.TEXT);
        copy.setRows(4);
        card.add(copy);
        return card;
    }

    private JPanel buildFooter(final Runnable onClose, final Runnable onFeedback,
                               final Runnable onBugReport) {
        JPanel footer = new JPanel(new java.awt.FlowLayout(
            java.awt.FlowLayout.RIGHT, 12, 13));
        footer.setBackground(HEADER);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        JButton bug = UiTheme.button("REPORT A BUG", false);
        bug.setPreferredSize(new Dimension(152, 42));
        bug.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (onBugReport != null) onBugReport.run();
            }
        });
        JButton feedback = UiTheme.button("SEND FEEDBACK", false);
        feedback.setPreferredSize(new Dimension(160, 42));
        feedback.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (onFeedback != null) onFeedback.run();
            }
        });
        JButton close = UiTheme.button("CONTINUE", true);
        close.setPreferredSize(new Dimension(142, 42));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (onClose != null) onClose.run();
            }
        });
        footer.add(bug);
        footer.add(feedback);
        footer.add(close);
        return footer;
    }

    private static JTextArea text(String value, int size, Color color) {
        JTextArea area = new JTextArea(value == null ? "" : value);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setForeground(color);
        area.setFont(UiTheme.body(Font.PLAIN, size));
        area.setBorder(null);
        area.setAlignmentX(LEFT_ALIGNMENT);
        return area;
    }
}
