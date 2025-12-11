package com.billiards2d;

import javafx.scene.paint.Color;

/**
 * Kelas yang merepresentasikan Bola Objek (Object Ball).
 * <p>
 * Ini adalah bola-bola target (berwarna/bernomor) yang harus dimasukkan ke dalam lubang.
 * Kelas ini mewarisi sifat fisik dari {@link Ball} dan menambahkan properti nomor dan Tipe Bola.
 * </p>
 */
public class ObjectBall extends Ball {

    /** Nomor bola (1-15). Digunakan untuk menentukan sprite yang akan digambar. */
    private int number;

    /** Tipe bola (Solid/Stripe/8-Ball) untuk keperluan Rules. */
    private BallType type;

    /**
     * Konstruktor untuk membuat Bola Objek.
     *
     * @param position Posisi awal bola (Vector2D).
     * @param number   Nomor bola (1-15).
     */
    public ObjectBall(Vector2D position, int number) {
        // Memanggil konstruktor superclass (Ball)
        super(position, Color.WHITE, 13.0); // Radius 13.0 sesuai update terakhir
        this.number = number;
        determineType();
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
}