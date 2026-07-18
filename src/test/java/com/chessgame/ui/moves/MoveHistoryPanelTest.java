package com.chessgame.ui.moves;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveHistoryPanelTest {

    @Test
    void constructor_setsBlackBackground() {
        MoveHistoryPanel panel = new MoveHistoryPanel("Alice");
        assertEquals(Color.BLACK, panel.getBackground());
    }

    @Test
    void setRows_actuallyPopulatesTheTable() throws Exception {
        MoveHistoryPanel panel = new MoveHistoryPanel("Alice");
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"00:01.000", "e4"});
        rows.add(new String[]{"00:03.500", "Nf3"});
        panel.setRows(rows);

        DefaultTableModel model = tableModelOf(panel);
        assertEquals(2, model.getRowCount());
        assertEquals("00:01.000", model.getValueAt(0, 0));
        assertEquals("e4", model.getValueAt(0, 1));
        assertEquals("00:03.500", model.getValueAt(1, 0));
        assertEquals("Nf3", model.getValueAt(1, 1));
    }

    @Test
    void setRows_replacesPreviousRows_doesNotAccumulate() throws Exception {
        MoveHistoryPanel panel = new MoveHistoryPanel("Alice");

        List<String[]> firstBatch = new ArrayList<>();
        firstBatch.add(new String[]{"00:01.000", "e4"});
        panel.setRows(firstBatch);

        List<String[]> secondBatch = new ArrayList<>();
        secondBatch.add(new String[]{"00:01.000", "e4"});
        secondBatch.add(new String[]{"00:02.000", "e5"});
        panel.setRows(secondBatch);

        DefaultTableModel model = tableModelOf(panel);
        assertEquals(2, model.getRowCount(), "אמור להיות בדיוק 2 - לא 3 (לא מצטבר)");
    }

    @Test
    void setRows_withEmptyList_clearsTheTable() throws Exception {
        MoveHistoryPanel panel = new MoveHistoryPanel("Alice");
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"00:01.000", "e4"});
        panel.setRows(rows);
        panel.setRows(new ArrayList<>());

        DefaultTableModel model = tableModelOf(panel);
        assertEquals(0, model.getRowCount());
    }

    @Test
    void setScore_actuallyUpdatesTheLabel() throws Exception {
        MoveHistoryPanel panel = new MoveHistoryPanel("Alice");
        panel.setScore(9);

        JLabel scoreLabel = scoreLabelOf(panel);
        assertEquals("Score: 9", scoreLabel.getText());
    }

    @Test
    void setScore_calledTwice_showsLatestValue() throws Exception {
        MoveHistoryPanel panel = new MoveHistoryPanel("Alice");
        panel.setScore(3);
        panel.setScore(12);

        JLabel scoreLabel = scoreLabelOf(panel);
        assertEquals("Score: 12", scoreLabel.getText());
    }

    private DefaultTableModel tableModelOf(MoveHistoryPanel panel) throws Exception {
        Field field = MoveHistoryPanel.class.getDeclaredField("tableModel");
        field.setAccessible(true);
        return (DefaultTableModel) field.get(panel);
    }

    private JLabel scoreLabelOf(MoveHistoryPanel panel) throws Exception {
        Field field = MoveHistoryPanel.class.getDeclaredField("scoreLabel");
        field.setAccessible(true);
        return (JLabel) field.get(panel);
    }
}
