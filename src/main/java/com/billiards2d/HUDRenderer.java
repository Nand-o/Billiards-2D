package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Class untuk rendering HUD Arcade Mode
 * Handles: Timer, Score, High Score display
 */
public class HUDRenderer {

    /**
     * Menggambar HUD untuk arcade mode (timer, score, high score)
     */
    public void drawArcadeHUD(GraphicsContext gc, double canvasWidth, double arcadeTimer,
                             int arcadeScore, int highScore) {
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 30));

        // 1. TAMPILAN TIMER
        // Cegah timer negatif visual saat game over
        double displayTime = Math.max(0, arcadeTimer);

        int minutes = (int) displayTime / 60;
        int seconds = (int) displayTime % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);

        // Warna Timer
        if (displayTime <= 10.0 && displayTime > 0) gc.setFill(Color.RED);
        else gc.setFill(Color.WHITE);

        gc.fillText(timeStr, (canvasWidth/2) - 40, 35);

        // 2. TAMPILAN SKOR
        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        gc.fillText("SCORE: " + arcadeScore, (canvasWidth/2) - 43, 60);

        // 3. HIGH SCORE
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        String bestText = "BEST: " + highScore;
        double bestX = canvasWidth - 150;
        gc.fillText(bestText, bestX, 50);

        // Efek New Record
        if (arcadeScore > 0 && arcadeScore >= highScore) {
            if ((System.currentTimeMillis() / 200) % 2 == 0) {
                gc.setFill(Color.LIME);
                gc.fillText("NEW RECORD!", bestX, 75);
            }
        }
    }
}
