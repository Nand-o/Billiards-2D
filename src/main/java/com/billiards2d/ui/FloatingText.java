package com.billiards2d.ui;

import static com.billiards2d.core.GameConstants.*;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Small ephemeral text that floats upwards and disappears. Used for
 * short in-game feedback (e.g., +score). The textual content shown
 * in-game is maintained in English and is not modified by these docs.
 *
 * @since 2025-12-13
 */
public class FloatingText {
    private double x, y;
    private String text;
    private Color color;
    private double lifeTime; // Durasi hidup (detik)
    private double maxLife;

    /**
     * Buat FloatingText baru yang muncul di posisi (x,y) dan mengapung ke atas.
     *
     * @param x koordinat X awal teks
     * @param y koordinat Y awal teks
     * @param text isi teks yang ditampilkan
     * @param color warna teks
     */
    public FloatingText(double x, double y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.maxLife = FLOATING_TEXT_LIFETIME; // Muncul selama 1.5 detik
        this.lifeTime = maxLife;
    }

    /**
     * Perbarui posisi dan umur teks.
     *
     * @param deltaTime waktu sejak frame terakhir (detik)
     * @return true jika teks sudah kedaluwarsa dan harus dihapus
     */
    public boolean update(double deltaTime) {
        lifeTime -= deltaTime;
        y -= FLOATING_TEXT_RISE_SPEED * deltaTime; // Gerak ke atas pelan-pelan
        return lifeTime <= 0; // Return true jika sudah mati (harus dihapus)
    }

    /**
     * Gambar teks yang mengapung ke layar dengan efek fade-out.
     *
     * @param gc konteks grafis JavaFX untuk menggambar
     */
    public void draw(GraphicsContext gc) {
        double alpha = lifeTime / maxLife; // Efek memudar (Fade out)
        // Clamp alpha biar gak error
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;

        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setFill(color);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        // Efek Outline Hitam biar teks terbaca jelas
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeText(text, x, y);
        gc.fillText(text, x, y);
        gc.restore();
    }
}
