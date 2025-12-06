package com.billiards2d;

import javafx.scene.paint.Color;

/**
 * Kelas yang merepresentasikan Bola Putih (Cue Ball).
 * <p>
 * Ini adalah bola utama yang dikendalikan oleh pemain menggunakan stik.
 * Kelas ini mewarisi semua properti fisik dari kelas {@link Ball} namun memiliki
 * metode tambahan untuk menerima interaksi pukulan.
 * </p>
 */
public class CueBall extends Ball {

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
        // Warna di-hardcode ke Color.WHITE dan radius ke 10.0
        super(position, Color.WHITE, 10.0);
    }

    /**
     * Menerapkan gaya pukulan dari stik ke bola.
     * <p>
     * Metode ini dipanggil ketika pemain melepaskan stik biliar.
     * Gaya yang diterima langsung dikonversi menjadi kecepatan awal (velocity) bola.
     * </p>
     *
     * @param force Vektor gaya yang dihasilkan oleh {@link CueStick},
     * yang mencakup arah dan besaran kekuatan pukulan.
     */
    public void hit(Vector2D force) {
        // Dalam simulasi sederhana ini, gaya impuls langsung menjadi kecepatan sesaat.
        this.velocity = force;
    }
}