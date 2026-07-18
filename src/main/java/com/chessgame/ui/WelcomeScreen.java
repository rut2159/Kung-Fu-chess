package com.chessgame.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public final class WelcomeScreen extends JFrame {
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 26);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 18);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 17);
    private static final int FIELD_WIDTH = 280;

    public WelcomeScreen(BiConsumer<String, String> onStart) {
        super("Kong Fu Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.BLACK);
        content.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel title = new JLabel("ברוכים הבאים ל-Kong Fu Chess!", SwingConstants.CENTER);
        title.setForeground(ACCENT_GOLD);
        title.setFont(TITLE_FONT);
        content.add(title, BorderLayout.NORTH);

        content.add(buildFormPanel(onStart), BorderLayout.CENTER);
        setContentPane(content);
    }

    private JPanel buildFormPanel(BiConsumer<String, String> onStart) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.BLACK);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        JTextField whiteField = buildNameField("Player 1");
        JTextField blackField = buildNameField("Player 2");

        gbc.gridy = 0;
        form.add(buildLabel("שם שחקן לבן:"), gbc);
        gbc.gridy = 1;
        form.add(whiteField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(24, 0, 8, 0);
        form.add(buildLabel("שם שחקן שחור:"), gbc);
        gbc.gridy = 3;
        gbc.insets = new Insets(8, 0, 8, 0);
        form.add(blackField, gbc);

        JButton startButton = new JButton("התחל משחק");
        startButton.setFont(BUTTON_FONT);
        startButton.setBackground(ACCENT_GOLD);
        startButton.setForeground(Color.BLACK);
        startButton.setFocusPainted(false);
        startButton.setPreferredSize(new Dimension(220, 46));
        startButton.addActionListener(e -> {
            String whiteName = nameOrDefault(whiteField, "Player 1");
            String blackName = nameOrDefault(blackField, "Player 2");
            onStart.accept(whiteName, blackName);
            dispose();
        });

        gbc.gridy = 4;
        gbc.insets = new Insets(36, 0, 0, 0);
        form.add(startButton, gbc);

        return form;
    }

    private JLabel buildLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(LABEL_FONT);
        return label;
    }

    private JTextField buildNameField(String placeholderDefault) {
        JTextField field = new JTextField(placeholderDefault);
        field.setFont(FIELD_FONT);
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setPreferredSize(new Dimension(FIELD_WIDTH, 38));
        return field;
    }

    private String nameOrDefault(JTextField field, String fallback) {
        String text = field.getText().trim();
        return text.isEmpty() ? fallback : text;
    }
}
