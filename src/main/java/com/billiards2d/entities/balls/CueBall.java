package com.billiards2d.entities.balls;

import static com.billiards2d.core.GameConstants.*;

import com.billiards2d.util.Vector2D;
import javafx.scene.paint.Color;
import javafx.scene.canvas.GraphicsContext;

/**
 * Kelas yang merepresentasikan Bola Putih (Cue Ball).
 * <p>
 * Ini adalah bola utama yang dikendalikan oleh pemain menggunakan stik.
 * Kelas ini mewarisi semua properti fisik dari kelas {@link Ball} namun memiliki
 * metode tambahan untuk menerima interaksi pukulan.
 * </p>
 */
public class CueBall extends Ball {

    // Flag baru untuk menandai apakah bola putih sedang masuk ke dalam lubang
    private boolean pendingRespawn = false;

    // Tambahkan properti tipe untuk konsistensi
    private BallType type = BallType.CUE;

    /**
     * Konstruktor untuk membuat Bola Putih.
     * <p>
     * Warna bola secara otomatis diset menjadi PUTIH dan radius diset ke 10.0.
     * </p>
     *
     * @param position Posisi awal bola putih di atas meja (Vector2D).
     */
    public CueBall(Vector2D position) {
        // Memanggil konstruktor superclass (Ball)
        // Warna di-hardcode ke Color.WHITE dan radius
        super(position, Color.WHITE, BALL_RADIUS);
    }

    /**
     * Menerapkan gaya pukulan dari stik ke bola.
     * <p>
     * Metode ini dipanggil ketika pemain melepaskan stik biliar.
     * Gaya yang diterima langsung dikonversi menjadi kecepatan awal (velocity) bola.
     * </p>
     *
     * @param force Vektor gaya yang dihasilkan oleh {@link CueStick},
     *              yang mencakup arah dan besaran kekuatan pukulan.
     */
    public void hit(Vector2D force) {
        // Dalam simulasi sederhana ini, gaya impuls langsung menjadi kecepatan sesaat.
        this.velocity = force;
    }

    // --- Getter & Setter untuk pendingRespawn ---
    public boolean isPendingRespawn() {
        return pendingRespawn;
    }

    public void setPendingRespawn(boolean pendingRespawn) {
        this.pendingRespawn = pendingRespawn;
    }

    public BallType getType() {
        return type;
    }

    // --- Override method draw ---
    @Override
    public void draw(GraphicsContext gc) {
        // Jika bola sedang dalam status "pending respawn" (mati sementara), jangan gambar apapun.
        // Kita cek !active (berarti sudah masuk lubang) DAN pendingRespawn (berarti belum saatnya muncul)
        if (!active && pendingRespawn) {
            return;
        }

        // Jika tidak dalam status pending, gambar bola seperti biasa (panggil method induk)
        super.draw(gc);
    }
}
