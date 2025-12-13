package com.billiards2d.core;

import javafx.scene.canvas.GraphicsContext;

/**
 * Interface kontrak untuk semua entitas dalam permainan.
 * <p>
 * Setiap objek yang ingin dimasukkan ke dalam Game Loop harus mengimplementasikan interface ini.
 * Ini memungkinkan penerapan prinsip Polimorfisme, di mana objek yang berbeda
 * (Bola, Meja, Stik) dapat dikelola secara seragam.
 * </p>
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
