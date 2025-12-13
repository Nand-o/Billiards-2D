package com.billiards2d.util;

/**
 * Representasi vektor 2D immutable (x, y) yang digunakan untuk semua
 * perhitungan posisi, kecepatan, dan operasi vektor di seluruh aplikasi.
 * <p>
 * Kelas ini menyediakan operasi dasar seperti penjumlahan, pengurangan,
 * perkalian skalar, normalisasi, serta dot-product. Objek bersifat
 * immutable: setiap operasi mengembalikan instance baru sehingga
 * menghindari efek samping saat digunakan di subsistem fisika.
 * </p>
 *
 * Dokumentasi metode ditulis dalam Bahasa Indonesia. Notifikasi atau teks
 * yang tampil di UI tetap menggunakan Bahasa Inggris dan tidak diubah di sini.
 *
 * @since 2025-12-13
 */
public class Vector2D {
    private final double x;
    private final double y;

    /**
     * Konstruktor Vector2D.
     * @param x Komponen horizontal.
     * @param y Komponen vertikal.
     */
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    /**
     * Menjumlahkan vektor ini dengan vektor lain.
     * @return Vektor baru hasil penjumlahan (this + other).
     */
    public Vector2D add(Vector2D other) {
        return new Vector2D(this.x + other.x, this.y + other.y);
    }

    /**
     * Mengurangkan vektor lain dari vektor ini.
     * @return Vektor baru hasil pengurangan (this - other).
     */
    public Vector2D subtract(Vector2D other) {
        return new Vector2D(this.x - other.x, this.y - other.y);
    }

    /**
     * Mengalikan vektor dengan nilai skalar.
     * Digunakan untuk penskalaan (misalnya memperbesar/memperkecil kecepatan).
     * @return Vektor baru hasil perkalian (this * scalar).
     */
    public Vector2D multiply(double scalar) {
        return new Vector2D(this.x * scalar, this.y * scalar);
    }

    /**
     * Menghitung panjang (magnitude) dari vektor.
     * Menggunakan rumus Pythagoras: sqrt(x^2 + y^2).
     * @return Panjang vektor.
     */
    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Menghasilkan vektor satuan (unit vector) yang memiliki arah sama
     * tetapi panjangnya 1.
     * @return Vektor normalisasi, atau vektor (0,0) jika panjang awalnya 0.
     */
    public Vector2D normalize() {
        double len = length();
        if (len == 0) return new Vector2D(0, 0);
        return new Vector2D(x / len, y / len);
    }

    /**
     * Menghitung perkalian titik (dot product) dengan vektor lain.
     * Dot product berguna untuk memproyeksikan satu vektor ke vektor lain
     * (misalnya saat menghitung pantulan).
     * @return Nilai skalar hasil dot product.
     */
    public double dot(Vector2D other) {
        return this.x * other.x + this.y * other.y;
    }

    @Override
    public String toString() {
        return String.format("Vector2D(%.2f, %.2f)", x, y);
    }
}
