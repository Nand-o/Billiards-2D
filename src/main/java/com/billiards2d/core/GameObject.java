package com.billiards2d.core;

import javafx.scene.canvas.GraphicsContext;

/**
 * Kontrak (interface) untuk setiap entitas yang berpartisipasi di dalam
 * game loop (update/draw). Semua objek permainan yang perlu diperbarui
 * setiap frame harus mengimplementasikan interface ini.
 * <p>
 * Tujuan: memungkinkan pengelolaan koleksi heterogen dari objek-objek
 * game (seperti bola, stik, meja) dengan pola yang konsisten.
 * </p>
 *
 * @since 2025-12-13
 */
public interface GameObject {

    /**
     * Memperbarui logika internal objek untuk frame saat ini.
     *
     * @param deltaTime Waktu yang berlalu sejak frame terakhir (dalam detik).
     * Digunakan untuk perhitungan fisika yang konsisten (frame-rate independent).
     */
    void update(double deltaTime);

    /**
     * Menggambar representasi visual objek ke layar.
     *
     * @param gc Konteks grafis JavaFX yang digunakan untuk menggambar.
     */
    void draw(GraphicsContext gc);
}
