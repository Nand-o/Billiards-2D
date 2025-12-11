package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Kelas abstrak yang merepresentasikan entitas dasar Bola Biliar.
 * Kelas ini menangani properti fisik dan sekarang menangani VISUAL SPRITE.
 */
public abstract class Ball implements GameObject {

    protected Vector2D position;
    protected Vector2D velocity;
    protected double radius;
    protected double mass;
    protected Color color;
    protected boolean active = true;

    // --- ASSET MANAGEMENT ---
    // Gambar sprite sheet dimuat sekali untuk semua instance bola (static)
    protected static Image ballSpriteSheet;

    // Ukuran bola di file gambar (Sprite asli 16x16 pixel)
    protected static final double SPRITE_SIZE = 16.0;

    public Ball(Vector2D position, Color color, double radius) {
        this.position = position;
        this.velocity = new Vector2D(0, 0);
        this.color = color;
        this.radius = radius;
        this.mass = 1.0;

        // Load Gambar jika belum ada
        if (ballSpriteSheet == null) {
            try {
                ballSpriteSheet = new Image(getClass().getResourceAsStream("/assets/SMS_GUI_Display_NO_BG.png"));
            } catch (Exception e) {
                System.err.println("Gagal load sprite bola: " + e.getMessage());
            }
        }
    }

    @Override
    public void update(double deltaTime) {
        position = position.add(velocity.multiply(deltaTime));
        double frictionFactor = Math.pow(0.992, deltaTime * 60.0);
        velocity = velocity.multiply(frictionFactor);
        if (velocity.length() < 5) velocity = new Vector2D(0, 0);
    }

    /**
     * Menggambar bola menggunakan SPRITE (Gambar).
     */
    @Override
    public void draw(GraphicsContext gc) {
        if (!active) return;

        // Jika gambar gagal diload, fallback ke lingkaran warna lama
        if (ballSpriteSheet == null) {
            gc.setFill(this.color);
            gc.fillOval(position.getX() - radius, position.getY() - radius, radius * 2, radius * 2);
            return;
        }

        // --- LOGIKA PEMILIHAN SPRITE (CROP) ---
        double srcX = 0;
        double srcY = 0;

        if (this instanceof CueBall) {
            // Bola Putih ada di Baris 2 (Y=16), Kolom ke-8 (Index 7)
            // Koordinat X = 7 * 16 = 112
            srcX = 112;
            srcY = 16;
        }
        else if (this instanceof ObjectBall) {
            int num = ((ObjectBall) this).getNumber();

            if (num >= 1 && num <= 8) {
                // Bola 1-8 (Solid) ada di Baris 1 (Y=0)
                srcX = (num - 1) * SPRITE_SIZE;
                srcY = 0;
            } else if (num >= 9 && num <= 15) {
                // Bola 9-15 (Stripes) ada di Baris 2 (Y=16)
                // Bola 9 ada di kolom 0 -> (9-9)*16 = 0
                srcX = (num - 9) * SPRITE_SIZE;
                srcY = 16;
            }
        }

        // --- MENGGAMBAR IMAGE ---
        // source x,y,w,h -> koordinat potong di gambar asli
        // dest x,y,w,h   -> koordinat gambar di layar

        double destW = radius * 2; // Diameter di layar (20px)
        double destH = radius * 2;
        double destX = position.getX() - radius;
        double destY = position.getY() - radius;

        gc.drawImage(ballSpriteSheet,
                srcX, srcY, SPRITE_SIZE, SPRITE_SIZE, // Source (Crop)
                destX, destY, destW, destH);          // Destination (Screen)
    }

    // --- Getters/Setters Standard ---
    public Vector2D getPosition() { return position; }
    public void setPosition(Vector2D position) { this.position = position; }
    public Vector2D getVelocity() { return velocity; }
    public void setVelocity(Vector2D velocity) { this.velocity = velocity; }
    public double getRadius() { return radius; }
    public double getMass() { return mass; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}