package thechosenquest.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

/** Styled, offline-readable project credits and third-party license presentation. */
final class CreditsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color HEADER = new Color(20, 13, 9);
    private static final Color CARD = new Color(46, 34, 26);

    CreditsPanel(final Runnable onClose) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(760, 650));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createLineBorder(UiTheme.GOLD, 2));
        add(buildHeader(onClose), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UiTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 26, 24));
        addIntro(content);

        String section = "";
        for (CreditsCatalog.Entry entry : CreditsCatalog.entries()) {
            if (!entry.section.equals(section)) {
                section = entry.section;
                content.add(sectionHeading(section));
                content.add(Box.createVerticalStrut(8));
            }
            content.add(entryCard(entry));
            content.add(Box.createVerticalStrut(10));
        }
        addBetaTesters(content, CreditsCatalog.betaTesters());
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        FantasyScrollBarUI.install(scroll.getVerticalScrollBar(), UiTheme.GOLD);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 13));
        footer.setBackground(HEADER);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        JButton close = UiTheme.button("RETURN", true);
        close.setPreferredSize(new Dimension(150, 42));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (onClose != null) onClose.run();
            }
        });
        footer.add(close);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildHeader(final Runnable onClose) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(17, 20, 17, 20));
        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);
        JLabel title = new JLabel("CREDITS & LICENSES");
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.title(Font.PLAIN, 22));
        JLabel subtitle = new JLabel("With gratitude to the people behind the adventure");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.body(Font.PLAIN, 11));
        copy.add(title);
        copy.add(Box.createVerticalStrut(2));
        copy.add(subtitle);
        header.add(copy, BorderLayout.WEST);
        JButton close = new JButton(new SystemIcon(SystemIcon.Type.CLOSE, 18, UiTheme.GOLD));
        close.setPreferredSize(new Dimension(34, 34));
        close.setToolTipText("Close credits");
        close.getAccessibleContext().setAccessibleName("Close credits and licenses");
        UiTheme.applyButtonStyle(close, UiTheme.ButtonStyle.ICON, 7, 7);
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (onClose != null) onClose.run();
            }
        });
        header.add(close, BorderLayout.EAST);
        return header;
    }

    private void addIntro(JPanel content) {
        JTextArea intro = copy("We credit identifiable artists even when their license " +
            "does not require it. Full license files and this attribution record are " +
            "included with every release.", 13, UiTheme.TEXT);
        intro.setRows(2);
        intro.setPreferredSize(new Dimension(680, 48));
        intro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        content.add(intro);
        content.add(Box.createVerticalStrut(18));
    }

    private JLabel sectionHeading(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.GOLD);
        label.setFont(UiTheme.title(Font.PLAIN, 17));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JPanel entryCard(final CreditsCatalog.Entry entry) {
        JPanel card = new JPanel(new BorderLayout(16, 4));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        JLabel title = new JLabel(entry.title);
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.displayBold(16));
        JLabel creator = new JLabel(entry.creator + "  •  " + entry.license);
        creator.setForeground(UiTheme.GOLD_LIGHT);
        creator.setFont(UiTheme.body(Font.BOLD, 11));
        JTextArea notes = copy(entry.notes, 11, UiTheme.MUTED);
        notes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        details.add(title);
        details.add(Box.createVerticalStrut(3));
        details.add(creator);
        details.add(Box.createVerticalStrut(5));
        details.add(notes);
        card.add(details, BorderLayout.CENTER);

        if (entry.source.length() > 0) {
            JButton source = UiTheme.button("SOURCE", false);
            source.setPreferredSize(new Dimension(98, 36));
            source.setToolTipText(entry.source);
            source.getAccessibleContext().setAccessibleDescription(entry.source);
            source.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) { open(entry.source); }
            });
            card.add(source, BorderLayout.EAST);
        }
        return card;
    }

    private void addBetaTesters(JPanel content, List<String> testers) {
        content.add(Box.createVerticalStrut(8));
        content.add(sectionHeading("BETA TESTERS"));
        content.add(Box.createVerticalStrut(8));
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(13, 14, 13, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            testers.isEmpty() ? 76 : Math.max(76, testers.size() * 24 + 46)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        if (testers.isEmpty()) {
            JLabel anonymous = new JLabel("The anonymous beta-testing community");
            anonymous.setForeground(UiTheme.TEXT);
            anonymous.setFont(UiTheme.displayBold(15));
            card.add(anonymous);
            card.add(Box.createVerticalStrut(4));
            JLabel note = new JLabel("Named credits are added only with explicit permission.");
            note.setForeground(UiTheme.MUTED);
            note.setFont(UiTheme.body(Font.PLAIN, 11));
            card.add(note);
        } else {
            for (String tester : testers) {
                JLabel name = new JLabel("◆  " + tester);
                name.setForeground(UiTheme.TEXT);
                name.setFont(UiTheme.displayBold(14));
                card.add(name);
                card.add(Box.createVerticalStrut(4));
            }
        }
        content.add(card);
    }

    private static JTextArea copy(String text, int size, Color color) {
        JTextArea area = new JTextArea(text == null ? "" : text);
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

    private static void open(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) {
            // The complete source remains visible in the button tooltip and packaged file.
        }
    }
}
