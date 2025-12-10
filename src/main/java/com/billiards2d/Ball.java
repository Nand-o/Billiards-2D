package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Kelas abstrak yang merepresentasikan entitas dasar Bola Biliar.
 * Kelas ini menangani semua properti fisik (posisi, kecepatan, massa, radius)
 * dan logika pergerakan dasar seperti perpindahan posisi dan gesekan (friction).
 * Kelas ini mengimplementasikan interface {@link GameObject}.
 */
public abstract class Ball implements GameObject {

    /** Posisi bola dalam koordinat 2D (x, y) pada meja. */
    protected Vector2D position;

    /** Vektor kecepatan bola yang menentukan arah dan laju pergerakan. */
    protected Vector2D velocity;

    /** Jari-jari bola dalam satuan pixel. */
    protected double radius;

    /** Massa bola, digunakan untuk perhitungan momentum saat tumbukan antar-bola. */
    protected double mass;

    /** Warna visual bola saat digambar ke layar. */
    protected Color color;

    /** Status bola: true jika bola masih ada di meja, false jika sudah masuk lubang. */
    protected boolean active = true;

    /**
     * Konstruktor untuk membuat objek Ball baru.
     * @param position Posisi awal bola (Vector2D).
     * @param color    Warna bola.
     * @param radius   Ukuran jari-jari bola.
     */
    public Ball(Vector2D position, Color color, double radius) {
        this.position = position;
        this.velocity = new Vector2D(0, 0); // Kecepatan awal selalu 0 (diam)
        this.color = color;
        this.radius = radius;
        this.mass = 1.0; // Massa default diset ke 1.0
    }

    /**
     * Memperbarui status fisik bola untuk setiap frame permainan.
     * Metode ini dipanggil oleh Game Loop.
     * @param deltaTime Waktu yang berlalu sejak frame terakhir (dalam detik).
     * Digunakan agar gerakan bola konsisten (frame-rate independent).
     */
    @Override
    public void update(double deltaTime) {
        // 1. Integrasi Posisi:
        // Posisi baru = Posisi lama + (Kecepatan * Waktu)
        position = position.add(velocity.multiply(deltaTime));

        // 2. Penerapan Gesekan (Time-Based Friction):
        // Mengurangi kecepatan secara bertahap untuk mensimulasikan gesekan karpet meja.
        // Menggunakan Math.pow agar tingkat perlambatan tetap sama berapapun FPS komputernya.
        // Angka 0.992 adalah koefisien gesekan (semakin dekat ke 1, semakin licin).
        double frictionFactor = Math.pow(0.992, deltaTime * 60.0);
        velocity = velocity.multiply(frictionFactor);

        // 3. Batas Berhenti (Stop Threshold):
        // Jika bola bergerak sangat lambat (kurang dari 5 pixel/detik), paksa berhenti total.
        // Ini mencegah bola bergerak mikro (jitter) dan memungkinkan giliran bermain selesai.
        if (velocity.length() < 5)
            velocity = new Vector2D(0, 0); // biar ga gerak ketika sudah tidak ada gaya
    }

    /**
     * Menggambar bentuk visual bola ke Canvas JavaFX.
     * @param gc Konteks grafis dari Canvas tempat menggambar.
     */
    @Override
    public void draw(GraphicsContext gc) {
        // Jika bola tidak aktif (sudah masuk lubang), jangan gambar apapun.
        if (!active) {
            return;
        }

        gc.setFill(this.color);
        // Menggambar lingkaran (Oval).
        // JavaFX menggambar dari sudut kiri-atas, jadi kita kurangi posisi dengan radius
        // agar titik (x,y) berada tepat di tengah bola.
        gc.fillOval(
                this.position.getX() - this.radius,
                this.position.getY() - this.radius,
                this.radius * 2, // Lebar
                this.radius * 2 // Tinggi
        );
    }

    // --- Getter dan Setter ---

    public Vector2D getPosition() { return position; }
    public void setPosition(Vector2D position) { this.position = position; }
    public Vector2D getVelocity() { return velocity; }
    public void setVelocity(Vector2D velocity) { this.velocity = velocity; }
    public double getRadius() { return radius; }
    public double getMass() { return mass; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}