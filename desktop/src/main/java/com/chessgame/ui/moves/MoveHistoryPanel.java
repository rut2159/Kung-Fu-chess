package com.chessgame.ui.moves;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public final class MoveHistoryPanel extends JPanel {
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Color ROW_EVEN = new Color(18, 18, 18);
    private static final Color ROW_ODD = Color.BLACK;
    private static final Font NAME_FONT = new Font("SansSerif", Font.BOLD, 17);
    private static final Font SCORE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font TABLE_HEADER_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font TABLE_FONT = new Font("SansSerif", Font.PLAIN, 13);

    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"TIME", "MOVE"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);

    public MoveHistoryPanel(String playerName) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 0));
        setBackground(Color.BLACK);
        setBorder(new EmptyBorder(0, 8, 0, 8));

        add(buildHeader(playerName), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
    }

    private JPanel buildHeader(String playerName) {
        JLabel nameLabel = new JLabel(playerName, SwingConstants.CENTER);
        nameLabel.setForeground(ACCENT_GOLD);
        nameLabel.setFont(NAME_FONT);

        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(SCORE_FONT);

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBackground(Color.BLACK);
        header.add(nameLabel);
        header.add(scoreLabel);
        header.setBorder(new javax.swing.border.CompoundBorder(
                new MatteBorder(0, 0, 2, 0, ACCENT_GOLD),
                new EmptyBorder(10, 0, 8, 0)));
        return header;
    }

    private JScrollPane buildTableArea() {
        table.setBackground(Color.BLACK);
        table.setForeground(Color.WHITE);
        table.setFont(TABLE_FONT);
        table.setGridColor(new Color(40, 40, 40));
        table.setRowHeight(24);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);

        table.getTableHeader().setBackground(Color.BLACK);
        table.getTableHeader().setForeground(ACCENT_GOLD);
        table.getTableHeader().setFont(TABLE_HEADER_FONT);
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, ACCENT_GOLD));
        table.getTableHeader().setPreferredSize(new Dimension(0, 28));

        DefaultTableCellRenderer stripedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                c.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                c.setForeground(Color.WHITE);
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, stripedRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.BLACK);
        return scrollPane;
    }

    public void
    setRows(List<String[]> rows) {
        tableModel.setRowCount(0);
        for (String[] row : rows) {
            tableModel.addRow(row);
        }
        int lastRow = table.getRowCount() - 1;
        if (lastRow >= 0) {
            table.scrollRectToVisible(table.getCellRect(lastRow, 0, true));
        }
    }

    public void setScore(int score) {
        scoreLabel.setText("Score: " + score);
    }
}
