package com.billiards2d.entities.balls;

import static com.billiards2d.core.GameConstants.*;

import com.billiards2d.util.Vector2D;
import javafx.scene.paint.Color;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Bola objek bernomor yang merupakan target permainan (1-15).
 * <p>
 * Menyimpan informasi nomor dan tipe (solid/stripe/8), serta logika
 * rendering sprite yang sesuai dengan nomor bola.
 * </p>
 *
 * @since 2025-12-13
 */
public class ObjectBall extends Ball {

    /** Nomor bola (1-15). Digunakan untuk menentukan sprite yang akan digambar. */
    private int number;

    /** Tipe bola (Solid/Stripe/8-Ball) untuk keperluan Rules. */
    private BallType type;
    private boolean usePlainTexture = false;

    /**
     * Konstruktor untuk membuat Bola Objek.
     *
     * @param position Posisi awal bola (Vector2D).
     * @param number   Nomor bola (1-15).
     */
    public ObjectBall(Vector2D position, int number) {
        // Memanggil konstruktor superclass (Ball)
        super(position, Color.WHITE, BALL_RADIUS);
        this.number = number;
        determineType();

        if (ballSpriteSheet == null) {
            try {
                ballSpriteSheet = new Image(getClass().getResourceAsStream(ASSET_BALL_SPRITE));
            } catch (Exception e) {
                System.err.println("Gagal load bola: " + e.getMessage());
            }
        }
    }

    /**
     * Menentukan tipe bola secara otomatis berdasarkan nomornya.
     * Aturan 8-Ball standar:
     * 1-7  : SOLID
     * 8    : EIGHT_BALL
     * 9-15 : STRIPE
     */
    private void determineType() {
        if (number == 8) {
            this.type = BallType.EIGHT_BALL;
        } else if (number >= 1 && number <= 7) {
            this.type = BallType.SOLID;
        } else if (number >= 9 && number <= 15) {
            this.type = BallType.STRIPE;
        } else {
            this.type = BallType.UNKNOWN;
        }
    }

    // SETTER BARU
    /**
     * Aktifkan tekstur polos (Arcade) untuk render bola selain bola 8.
     *
     * @param plain true untuk menggunakan tekstur polos
     */
    public void setUsePlainTexture(boolean plain) {
        this.usePlainTexture = plain;
    }

    /**
     * Mengembalikan nomor bola.
     * @return int nomor bola.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Mengembalikan tipe bola (Solid/Stripe/8Ball).
     * @return BallType.
     */
    public BallType getType() {
        return type;
    }

    /**
     * Gambar bola objek menggunakan sprite sheet.
     *
     * @param gc konteks grafis
     */
    public void draw(GraphicsContext gc) {
        if (!active) return; // Jangan gambar jika sudah masuk

        if (ballSpriteSheet != null) {
            double r = getRadius();
            double size = r * 2;
            double drawX = position.getX() - r;
            double drawY = position.getY() - r;

            // --- LOGIKA MAPPING SPRITE ---
            double srcX = 0;
            double srcY = 0;

            if (usePlainTexture && number != 8) {
                // --- MODE ARCADE (POLOS) ---
                // Bola 8 tetap pakai tekstur standar (Hitam Angka 8)

                if (number == 3) {
                    // MERAH POLOS (Menggantikan bola 3)
                    srcX = RED_BALL_SPRITE_X;
                    srcY = RED_BALL_SPRITE_Y;
                }
                else if (number == 1) {
                    // KUNING POLOS (Menggantikan bola 1)
                    srcX = YELLOW_BALL_SPRITE_X;
                    srcY = YELLOW_BALL_SPRITE_Y;
                }
            }
            else {
                // --- MODE STANDAR (ANGKA) ---
                if (number <= 8) { // Baris Atas (1-8)
                    srcX = (number - 1) * BALL_SPRITE_SIZE;
                    srcY = 0;
                } else { // Baris Bawah (9-15)
                    srcX = (number - 9) * BALL_SPRITE_SIZE;
                    srcY = BALL_SPRITE_SIZE;
                }
            }

            gc.drawImage(ballSpriteSheet, srcX, srcY, BALL_SPRITE_SIZE, BALL_SPRITE_SIZE, drawX, drawY, size, size);

        } else {
            // Fallback jika gambar gagal load
            super.draw(gc);
        }
    }
}
