package com.chessgame.ui.board;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ChessBoardPanel extends JPanel {
    private BufferedImage currentBoardImage;
    private int imageX;
    private int imageY;

    public ChessBoardPanel() {
        setBackground(Color.BLACK);
    }

    public void setBoardImage(BufferedImage image, int offsetX, int offsetY) {
        this.currentBoardImage = image;
        this.imageX = offsetX;
        this.imageY = offsetY;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (currentBoardImage != null) {
            g.drawImage(currentBoardImage, imageX, imageY, this);
        }
    }
}
