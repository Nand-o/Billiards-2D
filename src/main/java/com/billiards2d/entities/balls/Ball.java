package com.billiards2d.entities.balls;

import static com.billiards2d.core.GameConstants.*;

import com.billiards2d.core.GameObject;
import com.billiards2d.util.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Abstraksi dasar untuk semua bola di meja biliar.
 * <p>
 * Menyediakan properti fisik (posisi, kecepatan, massa, radius) serta contract
 * untuk menggambar dan memperbarui status. Kelas konkret seperti
 * {@link com.billiards2d.entities.balls.CueBall} dan
 * {@link com.billiards2d.entities.balls.ObjectBall} mewarisi dari kelas ini.
 * </p>
 *
 * Dokumentasi metode utama (misal update/draw) ditulis dalam Bahasa Indonesia.
 * @since 2025-12-13
 */
public abstract class Ball implements GameObject {

    /** Posisi pusat bola dalam koordinat logika permainan. */
    protected Vector2D position;

    /** Kecepatan bola sebagai vektor (pixel/detik). */
    protected Vector2D velocity;

    /** Radius bola (pixel). */
    protected double radius;

    /** Massa bola (dipakai pada perhitungan tumbukan sederhana). */
    protected double mass;

    /** Warna fallback ketika sprite tidak tersedia. */
    protected Color color;

    /** Status aktif: false jika bola telah ter-pocketed. */
    protected boolean active = true;

    // --- ASSET MANAGEMENT ---
    /** Sprite sheet yang memuat semua gambar bola (dibagikan antar instance). */
    protected static Image ballSpriteSheet;

    /**
     * Konstruktor dasar untuk sebuah bola.
     *
     * @param position posisi awal pusat bola
     * @param color warna bola
     * @param radius radius bola dalam piksel
     */
    public Ball(Vector2D position, Color color, double radius) {
        this.position = position;
        this.velocity = new Vector2D(0, 0);
        this.color = color;
        this.radius = radius;
        this.mass = 1.0;

        // Load Gambar jika belum ada
        if (ballSpriteSheet == null) {
            try {
                ballSpriteSheet = new Image(getClass().getResourceAsStream(ASSET_BALL_SPRITE));
            } catch (Exception e) {
                System.err.println("Gagal load sprite bola: " + e.getMessage());
            }
        }
    }

    /**
     * Update logika umum objek bola per frame.
     *
     * @param deltaTime waktu yang berlalu sejak frame sebelumnya (detik)
     */
    @Override
    public void update(double deltaTime) {
        position = position.add(velocity.multiply(deltaTime));
        double frictionFactor = Math.pow(FRICTION_POWER, deltaTime * 60.0);
        velocity = velocity.multiply(frictionFactor);
        if (velocity.length() < VELOCITY_STOP_THRESHOLD) velocity = new Vector2D(0, 0);
    }

    /**
     * Menggambar bola menggunakan SPRITE (Gambar).
     *
     * Gambar representasi bola pada canvas.
     *
     * @param gc konteks grafis yang digunakan untuk menggambar
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
            srcX = CUE_BALL_SPRITE_X;
            srcY = CUE_BALL_SPRITE_Y;
        }
        else if (this instanceof ObjectBall) {
            int num = ((ObjectBall) this).getNumber();

            if (num >= 1 && num <= 8) {
                // Bola 1-8 (Solid) ada di Baris 1 (Y=0)
                srcX = (num - 1) * BALL_SPRITE_SIZE;
                srcY = 0;
            } else if (num >= 9 && num <= 15) {
                // Bola 9-15 (Stripes) ada di Baris 2 (Y=16)
                // Bola 9 ada di kolom 0 -> (9-9)*16 = 0
                srcX = (num - 9) * BALL_SPRITE_SIZE;
                srcY = BALL_SPRITE_SIZE;
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
                srcX, srcY, BALL_SPRITE_SIZE, BALL_SPRITE_SIZE, // Source (Crop)
                destX, destY, destW, destH);          // Destination (Screen)
    }

    // --- Getters/Setters Standard ---
    /**
     * Ambil posisi pusat bola.
     *
     * @return posisi sebagai Vector2D
     */
    public Vector2D getPosition() { return position; }

    /**
     * Set posisi pusat bola.
     *
     * @param position posisi baru (Vector2D)
     */
    public void setPosition(Vector2D position) { this.position = position; }

    /**
     * Ambil kecepatan saat ini dari bola.
     *
     * @return vektor kecepatan
     */
    public Vector2D getVelocity() { return velocity; }

    /**
     * Set kecepatan bola.
     *
     * @param velocity vektor kecepatan baru
     */
    public void setVelocity(Vector2D velocity) { this.velocity = velocity; }

    /**
     * Ambil radius bola (pixel).
     *
     * @return radius bola
     */
    public double getRadius() { return radius; }

    /**
     * Ambil massa bola (digunakan untuk perhitungan impuls sederhana).
     *
     * @return massa bola
     */
    public double getMass() { return mass; }

    /**
     * Apakah bola saat ini aktif di meja (belum masuk lubang).
     *
     * @return true jika aktif
     */
    public boolean isActive() { return active; }

    /**
     * Tandai bola sebagai aktif atau tidak (mis. saat masuk lubang).
     *
     * @param active true untuk mengaktifkan kembali, false untuk menonaktifkan
     */
    public void setActive(boolean active) { this.active = active; }
}
