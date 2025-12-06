package com.billiards2d;

import javafx.scene.paint.Color;

/**
 * Kelas yang merepresentasikan Bola Objek (Object Ball).
 * <p>
 * Ini adalah bola-bola target (berwarna/bernomor) yang harus dimasukkan ke dalam lubang.
 * Kelas ini mewarisi sifat fisik dari {@link Ball} dan menambahkan properti tipe bola.
 * </p>
 */
public class ObjectBall extends Ball {

    /** Tipe atau identitas bola (misalnya "RED", "BLUE", "8"). */
    private String type;

    /**
     * Konstruktor untuk membuat Bola Objek.
     *
     * @param position Posisi awal bola (Vector2D).
     * @param type     String yang merepresentasikan warna atau tipe bola.
     * String ini akan dikonversi menjadi objek {@link Color}.
     */
    public ObjectBall(Vector2D position, String type) {
        // Memanggil konstruktor superclass (Ball)
        // Mengonversi string tipe menjadi warna JavaFX
        super(position, Color.valueOf(type), 10.0);
        this.type = type;
    }

    /**
     * Mengembalikan tipe bola.
     * @return String tipe bola.
     */
    public String getType() {
        return type;
    }
}