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
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** Figma-aligned general settings content with an initial Audio section. */
final class GameSettingsPanel extends JPanel {
    interface Listener {
        void onCancel();
        void onApplied();
        void onCredits();
        void onReleaseNotes();
        void onFeedback();
        void onBugReport();
    }

    private static final long serialVersionUID = 1L;
    private static final Color HEADER = new Color(20, 13, 9);
    private static final Color CARD = new Color(46, 34, 26);

    private final SoundManager soundManager;
    private final GamePreferences preferences;
    private final Listener listener;
    private final JSlider master = slider();
    private final JSlider music = slider();
    private final JSlider ambience = slider();
    private final JSlider effects = slider();
    private final JLabel masterValue = valueLabel();
    private final JLabel musicValue = valueLabel();
    private final JLabel ambienceValue = valueLabel();
    private final JLabel effectsValue = valueLabel();
    private final ToggleSwitch muteWhenUnfocused =
        new ToggleSwitch("Mute when game loses focus");
    private final ToggleSwitch reducedMotion =
        new ToggleSwitch("Reduced motion");

    GameSettingsPanel(final SoundManager soundManager, final GamePreferences preferences,
                      final Listener listener) {
        this.soundManager = soundManager;
        this.preferences = preferences;
        this.listener = listener;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(620, 620));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createLineBorder(UiTheme.GOLD, 2));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(17, 20, 17, 20));
        JPanel titleCopy = new JPanel();
        titleCopy.setLayout(new BoxLayout(titleCopy, BoxLayout.Y_AXIS));
        titleCopy.setOpaque(false);
        JLabel title = new JLabel("GAME SETTINGS");
        title.setForeground(UiTheme.GOLD);
        title.setFont(UiTheme.title(Font.PLAIN, 22));
        JLabel subtitle = new JLabel("Audio and game preferences");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.body(Font.PLAIN, 11));
        titleCopy.add(title);
        titleCopy.add(Box.createVerticalStrut(2));
        titleCopy.add(subtitle);
        header.add(titleCopy, BorderLayout.WEST);
        JButton close = iconButton(SystemIcon.Type.CLOSE, "Close settings");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onCancel(); }
        });
        header.add(close, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UiTheme.SURFACE);
        body.add(buildNavigation(), BorderLayout.WEST);
        body.add(buildAudio(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 13));
        footer.setBackground(HEADER);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        JButton cancel = UiTheme.button("CANCEL", false);
        cancel.setPreferredSize(new Dimension(118, 42));
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onCancel(); }
        });
        JButton apply = UiTheme.button("APPLY SETTINGS", true);
        apply.setPreferredSize(new Dimension(204, 42));
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                applySettings();
                listener.onApplied();
            }
        });
        footer.add(cancel);
        footer.add(apply);
        add(footer, BorderLayout.SOUTH);

        ChangeListener updateValues = new ChangeListener() {
            public void stateChanged(ChangeEvent event) { updateValueLabels(); }
        };
        master.addChangeListener(updateValues);
        music.addChangeListener(updateValues);
        ambience.addChangeListener(updateValues);
        effects.addChangeListener(updateValues);
        refreshFromManager();
    }

    void refreshFromManager() {
        master.setValue(soundManager.getMasterVolume());
        music.setValue(soundManager.getMusicVolume());
        ambience.setValue(soundManager.getAmbienceVolume());
        effects.setValue(soundManager.getEffectsVolume());
        muteWhenUnfocused.setSelected(soundManager.isMuteWhenUnfocused());
        reducedMotion.setSelected(preferences.isReducedMotion());
        updateValueLabels();
    }

    void requestInitialFocus() {
        master.requestFocusInWindow();
    }

    private JPanel buildNavigation() {
        JPanel navigation = new JPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setPreferredSize(new Dimension(160, 0));
        navigation.setBackground(HEADER);
        navigation.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.BORDER),
            BorderFactory.createEmptyBorder(20, 12, 20, 12)));
        navigation.add(navItem("AUDIO", true));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(navItem("DISPLAY", false));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(navItem("ACCESSIBILITY", false));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(navItem("GAMEPLAY", false));
        navigation.add(Box.createVerticalGlue());
        JButton notes = navigationButton("WHAT'S NEW", "View current release notes");
        notes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onReleaseNotes(); }
        });
        navigation.add(notes);
        navigation.add(Box.createVerticalStrut(6));
        JButton feedback = navigationButton("FEEDBACK", ProjectLinks.FEEDBACK);
        feedback.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onFeedback(); }
        });
        navigation.add(feedback);
        navigation.add(Box.createVerticalStrut(6));
        JButton bug = navigationButton("REPORT A BUG", ProjectLinks.BUG_REPORT);
        bug.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onBugReport(); }
        });
        navigation.add(bug);
        navigation.add(Box.createVerticalStrut(6));
        JButton credits = navigationButton("CREDITS & LICENSES",
            "View contributors, asset sources, and licenses");
        credits.getAccessibleContext().setAccessibleName("Credits and licenses");
        credits.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) { listener.onCredits(); }
        });
        navigation.add(credits);
        return navigation;
    }

    private JButton navigationButton(String text, String tooltip) {
        JButton button = UiTheme.button(text, false);
        button.setFont(UiTheme.body(Font.BOLD, 9));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.setPreferredSize(new Dimension(136, 38));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setToolTipText(tooltip);
        return button;
    }

    private JLabel navItem(String text, boolean selected) {
        JLabel item = new JLabel(text);
        item.setForeground(selected ? UiTheme.GOLD : UiTheme.MUTED);
        item.setFont(UiTheme.body(Font.BOLD, 11));
        item.setPreferredSize(new Dimension(136, 32));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        item.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, selected ? 2 : 0, 0, 0,
                selected ? UiTheme.GOLD : HEADER),
            BorderFactory.createEmptyBorder(0, selected ? 8 : 10, 0, 0)));
        return item;
    }

    private JPanel buildAudio() {
        JPanel audio = new JPanel();
        audio.setLayout(new BoxLayout(audio, BoxLayout.Y_AXIS));
        audio.setBackground(UiTheme.SURFACE);
        audio.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel heading = new JLabel("AUDIO");
        heading.setForeground(UiTheme.GOLD);
        heading.setFont(UiTheme.title(Font.PLAIN, 19));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        audio.add(heading);
        audio.add(Box.createVerticalStrut(6));
        JLabel help = new JLabel("Adjust game sounds without leaving your quest.");
        help.setForeground(UiTheme.MUTED);
        help.setFont(UiTheme.body(Font.PLAIN, 10));
        help.setAlignmentX(LEFT_ALIGNMENT);
        audio.add(help);
        audio.add(Box.createVerticalStrut(12));
        audio.add(sliderCard("Master Volume", master, masterValue));
        audio.add(Box.createVerticalStrut(8));
        audio.add(sliderCard("Music Volume", music, musicValue));
        audio.add(Box.createVerticalStrut(8));
        audio.add(sliderCard("Ambience Volume", ambience, ambienceValue));
        audio.add(Box.createVerticalStrut(8));
        audio.add(sliderCard("Sound Effects Volume", effects, effectsValue));
        audio.add(Box.createVerticalStrut(8));
        audio.add(toggleCard());
        audio.add(Box.createVerticalStrut(8));
        audio.add(reducedMotionCard());
        audio.add(Box.createVerticalGlue());
        return audio;
    }

    private JPanel sliderCard(String title, JSlider control, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        card.setPreferredSize(new Dimension(400, 72));
        JLabel label = new JLabel(title);
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.body(Font.BOLD, 12));
        card.add(label, BorderLayout.WEST);
        card.add(value, BorderLayout.EAST);
        card.add(control, BorderLayout.SOUTH);
        control.getAccessibleContext().setAccessibleName(title);
        card.setAlignmentX(LEFT_ALIGNMENT);
        return card;
    }

    private JPanel toggleCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(8, 11, 8, 11)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        card.setPreferredSize(new Dimension(400, 58));
        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);
        JLabel label = new JLabel("Mute when game loses focus");
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.body(Font.BOLD, 12));
        JLabel help = new JLabel("Prevents background game audio");
        help.setForeground(UiTheme.MUTED);
        help.setFont(UiTheme.body(Font.PLAIN, 10));
        copy.add(label);
        copy.add(Box.createVerticalStrut(2));
        copy.add(help);
        card.add(copy, BorderLayout.CENTER);
        card.add(muteWhenUnfocused, BorderLayout.EAST);
        card.setAlignmentX(LEFT_ALIGNMENT);
        return card;
    }

    private JPanel reducedMotionCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTheme.BORDER),
            BorderFactory.createEmptyBorder(8, 11, 8, 11)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        card.setPreferredSize(new Dimension(400, 58));
        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);
        JLabel label = new JLabel("Reduced motion");
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.body(Font.BOLD, 12));
        JLabel help = new JLabel("Uses short fades instead of cinematic transitions");
        help.setForeground(UiTheme.MUTED);
        help.setFont(UiTheme.body(Font.PLAIN, 10));
        copy.add(label);
        copy.add(Box.createVerticalStrut(2));
        copy.add(help);
        card.add(copy, BorderLayout.CENTER);
        card.add(reducedMotion, BorderLayout.EAST);
        card.setAlignmentX(LEFT_ALIGNMENT);
        return card;
    }

    private void applySettings() {
        soundManager.setMasterVolume(master.getValue());
        soundManager.setMusicVolume(music.getValue());
        soundManager.setAmbienceVolume(ambience.getValue());
        soundManager.setEffectsVolume(effects.getValue());
        soundManager.setMuteWhenUnfocused(muteWhenUnfocused.isSelected());
        preferences.setReducedMotion(reducedMotion.isSelected());
        soundManager.play(SoundManager.Cue.UI_CONFIRM);
    }

    private void updateValueLabels() {
        masterValue.setText(master.getValue() + "%");
        musicValue.setText(music.getValue() + "%");
        ambienceValue.setText(ambience.getValue() + "%");
        effectsValue.setText(effects.getValue() + "%");
    }

    private static JSlider slider() {
        JSlider slider = new JSlider(0, 100, 75);
        slider.setOpaque(false);
        slider.setForeground(UiTheme.GOLD);
        slider.setFocusable(true);
        slider.setPreferredSize(new Dimension(320, 20));
        slider.setUI(new FantasySliderUI(slider));
        return slider;
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("0%", SwingConstants.RIGHT);
        label.setForeground(UiTheme.GOLD);
        label.setFont(UiTheme.body(Font.PLAIN, 11));
        return label;
    }

    private static JButton iconButton(SystemIcon.Type type, String tooltip) {
        JButton button = new JButton(new SystemIcon(type, 18, UiTheme.GOLD));
        button.setPreferredSize(new Dimension(34, 34));
        button.setToolTipText(tooltip);
        UiTheme.applyButtonStyle(button, UiTheme.ButtonStyle.ICON, 7, 7);
        return button;
    }
}
