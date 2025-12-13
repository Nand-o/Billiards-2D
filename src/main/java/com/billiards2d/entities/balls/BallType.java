package com.billiards2d.entities.balls;

/**
 * Tipe bola dalam permainan 8-Ball.
 * <p>
 * Digunakan oleh subsistem aturan untuk menentukan kepemilikan (solid/stripe),
 * serta pengaturan kemenangan saat bola 8 terlibat.
 * </p>
 *
 * @since 2025-12-13
 */
public enum BallType {
    CUE,        // Bola Putih
    SOLID,      // Bola 1-7 (Warna Penuh)
    STRIPE,     // Bola 9-15 (Garis Putih)
    EIGHT_BALL, // Bola 8 (Hitam - Penentu Kemenangan/Kekalahan)
    UNKNOWN     // Default/Error
}
