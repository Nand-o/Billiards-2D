package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class FloatingText {
    private double x, y;
    private String text;
    private Color color;
    private double lifeTime; // Durasi hidup (detik)
    private double maxLife;

    public FloatingText(double x, double y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.maxLife = 1.5; // Muncul selama 1.5 detik
        this.lifeTime = maxLife;
    }

    public boolean update(double deltaTime) {
        lifeTime -= deltaTime;
        y -= 30 * deltaTime; // Gerak ke atas pelan-pelan
        return lifeTime <= 0; // Return true jika sudah mati (harus dihapus)
    }

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