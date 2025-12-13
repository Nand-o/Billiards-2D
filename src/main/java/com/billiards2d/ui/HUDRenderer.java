package com.billiards2d.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Renderer untuk Heads-Up Display (HUD) pada game modes seperti Arcade.
 * <p>
 * Menangani tampilan timer, score, dan high-score di layar.
 * Semua teks HUD direncanakan agar tampil dalam Bahasa Inggris di UI.
 * </p>
 *
 * @since 2025-12-13
 */
public class HUDRenderer {
    /**
     * Konstruktor HUDRenderer (kosong). Menyediakan titik ekstensi jika dibutuhkan.
     */
    public HUDRenderer() { }

    /**
     * Menggambar HUD untuk arcade mode (timer, score, high score).
     *
     * @param gc konteks grafis untuk menggambar
     * @param canvasWidth lebar kanvas saat ini
     * @param arcadeTimer sisa waktu untuk mode arcade (detik)
     * @param arcadeScore skor pemain saat ini
     * @param highScore high score yang tersimpan
     */
    public void drawArcadeHUD(GraphicsContext gc, double canvasWidth, double arcadeTimer,
                             int arcadeScore, int highScore) {
        // Method-level docs in Indonesian describe purpose and parameters
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
