package com.chessgame.ui;

import com.chessgame.GameSession;
import com.chessgame.model.Piece;
import com.chessgame.ui.board.BoardController;
import com.chessgame.ui.moves.PlayerPanelController;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public final class GameWindow {

    private static final double SIDE_PANEL_WIDTH_PERCENT = 0.16;

    private static final int LOOP_INTERVAL_MS = 33;

    private final GameSession session;
    private final String whitePlayerName;
    private final String blackPlayerName;

    private JFrame frame;
    private BoardController boardController;
    private PlayerPanelController whiteController;
    private PlayerPanelController blackController;
    private Timer loopTimer;
    private long lastTickAtMs;

    public GameWindow(GameSession session, String whitePlayerName, String blackPlayerName) {
        this.session = session;
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
    }

    public void init() {
        boardController = new BoardController(session, whitePlayerName, blackPlayerName);
        whiteController = new PlayerPanelController(session, Piece.Color.WHITE, whitePlayerName);
        blackController = new PlayerPanelController(session, Piece.Color.BLACK, blackPlayerName);

        JPanel contentWrapper = new JPanel(new BorderLayout(16, 16));
        contentWrapper.setBackground(Color.BLACK);
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentWrapper.add(blackController.panel(), BorderLayout.WEST);
        contentWrapper.add(boardController.panel(), BorderLayout.CENTER);
        contentWrapper.add(whiteController.panel(), BorderLayout.EAST);

        frame = new JFrame("Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(contentWrapper);
        frame.setSize(1100, 800);

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSidePanelWidths();
            }
        });

        frame.setVisible(true);
        updateSidePanelWidths();

        lastTickAtMs = System.currentTimeMillis();
        loopTimer = new Timer(LOOP_INTERVAL_MS, e -> {
            long now = System.currentTimeMillis();
            int elapsedMs = (int) Math.min(now - lastTickAtMs, LOOP_INTERVAL_MS * 5L);
            lastTickAtMs = now;

            session.gameEngine.wait(elapsedMs);
        });
        loopTimer.start();
    }

    private void updateSidePanelWidths() {
        int panelWidth = (int) (frame.getWidth() * SIDE_PANEL_WIDTH_PERCENT);
        whiteController.panel().setPreferredSize(new Dimension(panelWidth, 0));
        blackController.panel().setPreferredSize(new Dimension(panelWidth, 0));
        frame.revalidate();
    }
}
