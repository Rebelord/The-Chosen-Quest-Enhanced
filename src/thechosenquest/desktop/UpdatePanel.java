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
import javax.swing.SwingConstants;

/** Game-styled presentation for checking and reviewing application updates. */
class UpdatePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color HEADER = new Color(20, 13, 9);
    private final UpdateService.Result result;

    UpdatePanel(UpdateService.Result result, final Runnable onClose,
                final Runnable onDownload, final Runnable onViewChanges) {
        this.result = result;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(720, 540));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createLineBorder(
            result.status == UpdateService.Status.AVAILABLE
                ? UiTheme.GOLD_LIGHT : UiTheme.GOLD, 2));
        add(buildHeader(onClose), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(onClose, onDownload, onViewChanges), BorderLayout.SOUTH);
    }

    static UpdatePanel checking() {
        return new UpdatePanel(UpdateService.Result.current(null), null, null, null) {
            private static final long serialVersionUID = 1L;
            @Override
            protected String heading() { return "CHECKING FOR UPDATES"; }
            @Override
            protected String subtitle() {
                return "Contacting the official GitHub release channel…";
            }
            @Override
            protected JPanel buildContent() {
                JPanel content = new JPanel(new BorderLayout());
                content.setBackground(UiTheme.SURFACE);
                content.setBorder(BorderFactory.createEmptyBorder(80, 42, 80, 42));
                JLabel message = new JLabel(
                    "<html><div style='text-align:center'>Searching the " +
                    "published release list.<br><br><font color='#a69987'>" +
                    "Offline play remains available if the service cannot be reached." +
                    "</font></div></html>", SwingConstants.CENTER);
                message.setForeground(UiTheme.TEXT);
                message.setFont(UiTheme.body(Font.PLAIN, 15));
                content.add(message, BorderLayout.CENTER);
                return content;
            }
            @Override
            protected JPanel buildFooter(Runnable onClose, Runnable onDownload,
                                         Runnable onViewChanges) {
                JPanel footer = footerPanel();
                JLabel status = new JLabel("PLEASE WAIT");
                status.setForeground(UiTheme.MUTED);
                status.setFont(UiTheme.body(Font.BOLD, 11));
                footer.add(status);
                return footer;
            }
        };
    }

    protected String heading() {
        if (result.status == UpdateService.Status.AVAILABLE) return "UPDATE AVAILABLE";
        if (result.status == UpdateService.Status.CURRENT) return "GAME IS UP TO DATE";
        return "UPDATE CHECK UNAVAILABLE";
    }

    protected String subtitle() {
        if (result.release != null) {
            return AppVersion.DISPLAY_NAME + " installed  ·  " +
                result.release.tag + " published";
        }
        return AppVersion.DISPLAY_NAME + " installed";
    }

    private JPanel buildHeader(final Runnable onClose) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(17, 20, 17, 20));
        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);
        JLabel title = new JLabel(heading());
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.title(Font.PLAIN, 22));
        JLabel subtitle = new JLabel(subtitle());
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.body(Font.PLAIN, 11));
        copy.add(title);
        copy.add(Box.createVerticalStrut(2));
        copy.add(subtitle);
        header.add(copy, BorderLayout.WEST);
        if (onClose != null) {
            JButton close = new JButton(
                new SystemIcon(SystemIcon.Type.CLOSE, 18, UiTheme.GOLD));
            close.setPreferredSize(new Dimension(34, 34));
            close.setToolTipText("Close update window");
            close.getAccessibleContext().setAccessibleName("Close update window");
            UiTheme.applyButtonStyle(close, UiTheme.ButtonStyle.ICON, 7, 7);
            close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) { onClose.run(); }
            });
            header.add(close, BorderLayout.EAST);
        }
        return header;
    }

    protected JPanel buildContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UiTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JLabel message = new JLabel(result.message);
        message.setForeground(UiTheme.TEXT);
        message.setFont(UiTheme.body(Font.BOLD, 15));
        message.setAlignmentX(LEFT_ALIGNMENT);
        content.add(message);

        if (result.release != null) {
            content.add(Box.createVerticalStrut(18));
            JPanel releaseCard = new JPanel();
            releaseCard.setLayout(new BoxLayout(releaseCard, BoxLayout.Y_AXIS));
            releaseCard.setBackground(new Color(46, 34, 26));
            releaseCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(15, 17, 15, 17)));
            releaseCard.setAlignmentX(LEFT_ALIGNMENT);
            releaseCard.setPreferredSize(new Dimension(640, 92));
            releaseCard.setMinimumSize(new Dimension(0, 92));
            releaseCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
            JLabel version = new JLabel("<html><div style='width:580px'>" +
                html(releaseTitle(result.release)) + "</div></html>");
            version.setForeground(UiTheme.GOLD_LIGHT);
            version.setFont(UiTheme.title(Font.PLAIN, 17));
            version.setAlignmentX(LEFT_ALIGNMENT);
            JLabel channel = new JLabel(result.release.prerelease
                ? "BETA CHANNEL · Manual installation"
                : "STABLE CHANNEL · Manual installation");
            channel.setForeground(UiTheme.MUTED);
            channel.setFont(UiTheme.body(Font.BOLD, 10));
            releaseCard.add(version);
            releaseCard.add(Box.createVerticalStrut(4));
            releaseCard.add(channel);
            content.add(releaseCard);
            content.add(Box.createVerticalStrut(16));

            JTextArea notes = new JTextArea(readableNotes(result.release.notes));
            notes.setEditable(false);
            notes.setFocusable(false);
            notes.setLineWrap(true);
            notes.setWrapStyleWord(true);
            notes.setForeground(UiTheme.TEXT);
            notes.setBackground(UiTheme.SURFACE_DEEP);
            notes.setFont(UiTheme.body(Font.PLAIN, 12));
            notes.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            JScrollPane scroll = new JScrollPane(notes,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
            scroll.setAlignmentX(LEFT_ALIGNMENT);
            scroll.setPreferredSize(new Dimension(640, 220));
            scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
            FantasyScrollBarUI.install(scroll.getVerticalScrollBar(), UiTheme.GOLD);
            content.add(scroll);
        } else {
            content.add(Box.createVerticalStrut(28));
            JTextArea help = new JTextArea(
                "The game remains fully playable. Check your connection and try " +
                "again later, or open the release page directly.");
            help.setEditable(false);
            help.setFocusable(false);
            help.setLineWrap(true);
            help.setWrapStyleWord(true);
            help.setOpaque(false);
            help.setForeground(UiTheme.MUTED);
            help.setFont(UiTheme.body(Font.PLAIN, 13));
            help.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            help.setAlignmentX(LEFT_ALIGNMENT);
            content.add(help);
        }
        return content;
    }

    protected JPanel buildFooter(final Runnable onClose,
                                 final Runnable onDownload,
                                 final Runnable onViewChanges) {
        JPanel footer = footerPanel();
        if (onViewChanges != null) {
            JButton changes = UiTheme.button("VIEW CHANGES", false);
            changes.setPreferredSize(new Dimension(158, 42));
            changes.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    onViewChanges.run();
                }
            });
            footer.add(changes);
        }
        if (result.status == UpdateService.Status.AVAILABLE && onDownload != null) {
            JButton later = UiTheme.button("REMIND ME LATER", false);
            later.setPreferredSize(new Dimension(174, 42));
            later.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) { onClose.run(); }
            });
            footer.add(later);
            JButton download = UiTheme.button("DOWNLOAD UPDATE", true);
            download.setPreferredSize(new Dimension(240, 42));
            download.setToolTipText(
                "Open the official GitHub release download in your browser");
            download.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) { onDownload.run(); }
            });
            footer.add(download);
        } else if (onClose != null) {
            JButton close = UiTheme.button("CLOSE", true);
            close.setPreferredSize(new Dimension(132, 42));
            close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) { onClose.run(); }
            });
            footer.add(close);
        }
        return footer;
    }

    protected JPanel footerPanel() {
        JPanel footer = new JPanel(new java.awt.FlowLayout(
            java.awt.FlowLayout.RIGHT, 12, 13));
        footer.setBackground(HEADER);
        footer.setBorder(BorderFactory.createMatteBorder(
            1, 0, 0, 0, UiTheme.BORDER));
        return footer;
    }

    String displayedVersionForTest() {
        return result.release == null ? "" : result.release.version;
    }

    private static String releaseTitle(UpdateService.Release release) {
        return release.name.length() == 0 ? release.tag : release.name;
    }

    private static String html(String copy) {
        return copy.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String readableNotes(String markdown) {
        if (markdown == null || markdown.trim().length() == 0) {
            return "Open the release page to review complete changes.";
        }
        String[] lines = markdown.replace("\r", "").split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String readable = line.replaceFirst("^#{1,6}\\s*", "")
                .replace("**", "").replace("__", "").trim();
            if (readable.length() == 0) {
                if (result.length() > 0 &&
                        result.charAt(result.length() - 1) != '\n') result.append('\n');
                continue;
            }
            if (result.length() > 0) result.append('\n');
            result.append(readable);
            if (result.length() >= 6000) break;
        }
        return result.toString();
    }
}
