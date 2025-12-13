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
    /** Bola putih (cue ball). */
    CUE,
    /** Bola solid (1-7). */
    SOLID,
    /** Bola stripe (9-15). */
    STRIPE,
    /** Bola 8, penentu kemenangan/kekalahan. */
    EIGHT_BALL,
    /** Tipe tidak diketahui / fallback. */
    UNKNOWN
}
