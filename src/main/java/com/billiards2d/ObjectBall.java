package com.billiards2d;

import javafx.scene.paint.Color;

/**
 * Kelas yang merepresentasikan Bola Objek (Object Ball).
 * <p>
 * Ini adalah bola-bola target (berwarna/bernomor) yang harus dimasukkan ke dalam lubang.
 * Kelas ini mewarisi sifat fisik dari {@link Ball} dan menambahkan properti nomor bola.
 * </p>
 */
public class ObjectBall extends Ball {

    /** Nomor bola (1-15). Digunakan untuk menentukan sprite yang akan digambar. */
    private int number;

    /**
     * Konstruktor untuk membuat Bola Objek.
     *
     * @param position Posisi awal bola (Vector2D).
     * @param number   Nomor bola (1-15).
     * Warna akan ditentukan otomatis atau diabaikan karena kita pakai sprite.
     */
    public ObjectBall(Vector2D position, int number) {
        // Memanggil konstruktor superclass (Ball)
        // Warna kita set dummy (WHITE) saja karena visualnya nanti pakai Gambar.
        super(position, Color.WHITE, 13.0);
        this.number = number;
    }

    /**
     * Mengembalikan nomor bola.
     * @return int nomor bola.
     */
    public int getNumber() {
        return number;
    }
}