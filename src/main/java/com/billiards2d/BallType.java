package com.billiards2d;

/**
 * Enum untuk mengkategorikan jenis bola biliar.
 * Penting untuk logika aturan main (Solid vs Stripes).
 */
public enum BallType {
    CUE,        // Bola Putih
    SOLID,      // Bola 1-7 (Warna Penuh)
    STRIPE,     // Bola 9-15 (Garis Putih)
    EIGHT_BALL, // Bola 8 (Hitam - Penentu Kemenangan/Kekalahan)
    UNKNOWN     // Default/Error
}