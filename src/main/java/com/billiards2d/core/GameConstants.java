package com.billiards2d.core;

/**
 * Kumpulan konstanta konfigurasi untuk permainan (window size, fisika,
 * durasi efek, sprite offsets, dan konfigurasi lainnya).
 * <p>
 * Menggunakan kelas constants memberikan satu sumber kebenaran untuk
 * angka-angka "magic" yang digunakan di banyak tempat. Jangan mengubah nilai
 * di runtime; bila perlu ubah di sini dan lakukan pengujian ulang untuk
 * memastikan feel permainan tetap sesuai.
 * </p>
 *
 * Catatan: tidak mengandung logika; hanya konstanta publik/private.
 *
 * @since 2025-12-13
 */
public final class GameConstants {

    // Private constructor untuk mencegah instantiation
    private GameConstants() {
        throw new AssertionError("GameConstants class should not be instantiated");
    }

    // ==========================================
    //          WINDOW & SCREEN DIMENSIONS
    // ==========================================

    /** Lebar window aplikasi (HD 720p) */
    public static final double WINDOW_WIDTH = 1280;

    /** Tinggi window aplikasi (HD 720p) */
    public static final double WINDOW_HEIGHT = 720;

    /** Lebar area permainan meja biliar (Ratio 2:1) */
    public static final double GAME_WIDTH = 960;

    /** Tinggi area permainan meja biliar (Ratio 2:1) */
    public static final double GAME_HEIGHT = 480;

    // ==========================================
    //          BALL PROPERTIES
    // ==========================================

    /** Radius bola biliar (pixel) */
    public static final double BALL_RADIUS = 13.0;

    /** Ukuran sprite bola di file gambar (16x16 pixel) */
    public static final double BALL_SPRITE_SIZE = 16.0;

    // ==========================================
    //          PHYSICS CONSTANTS
    // ==========================================

    /** Koefisien friction (decay per frame) */
    public static final double FRICTION_POWER = 0.9882;

    /** Koefisien restitusi dinding (elastisitas pantulan) */
    public static final double WALL_RESTITUTION = 0.9;

    /** Koefisien restitusi antar bola */
    public static final double BALL_RESTITUTION = 0.9;

    /** Threshold kecepatan untuk menghentikan bola */
    public static final double VELOCITY_STOP_THRESHOLD = 5.0;

    /** Jumlah substep untuk physics update per frame */
    public static final int PHYSICS_SUBSTEPS = 4;

    // ==========================================
    //          CUE STICK MECHANICS
    // ==========================================

    /** Jarak maksimal tarik mundur stik (pixel) */
    public static final double MAX_PULL_DISTANCE = 300.0;

    /** Gaya maksimal pukulan */
    public static final double MAX_FORCE = 1350.0;

    /** Jarak drag mouse maksimal untuk full power */
    public static final double MAX_DRAG_DISTANCE = 300.0;

    /** Jarak default stik dari bola saat hover */
    public static final double STICK_OFFSET_FROM_BALL = 10.0;

    // ==========================================
    //          GAMEPLAY TIMERS
    // ==========================================

    /** Batas waktu per turn di 8-Ball mode (detik) */
    public static final double TURN_TIME_LIMIT = 30.0;

    /** Waktu awal Arcade mode (detik) */
    public static final double ARCADE_START_TIME = 120.0;

    /** Bonus waktu per bola masuk di Arcade mode (detik) */
    public static final double TIME_BONUS_PER_BALL = 5.0;

    /** Penalti waktu untuk foul di Arcade mode (detik) */
    public static final double TIME_PENALTY_FOUL = 10.0;

    // ==========================================
    //          SCORING
    // ==========================================

    /** Poin per bola masuk di Arcade mode */
    public static final int SCORE_PER_BALL = 10;

    /** Penalti poin untuk foul di Arcade mode */
    public static final int SCORE_PENALTY_FOUL = 10;

    /** Bonus poin untuk stage clear di Arcade mode */
    public static final int STAGE_CLEAR_BONUS = 500;

    /** Bonus waktu untuk stage clear di Arcade mode (detik) */
    public static final double STAGE_CLEAR_TIME_BONUS = 30.0;

    // ==========================================
    //          UI SPRITE COORDINATES
    // ==========================================

    // Chalk Box (Kubus)
    /** X offset sumber gambar untuk kubus (chalk box) di sprite sheet. */
    public static final double CUBE_SRC_X = 145;

    /** Y offset sumber gambar untuk kubus (chalk box) di sprite sheet. */
    public static final double CUBE_SRC_Y = 0;

    /** Lebar crop sprite kubus di gambar sumber (pixel). */
    public static final double CUBE_W = 22;

    /** Tinggi crop sprite kubus di gambar sumber (pixel). */
    public static final double CUBE_H = 22;

    // Power Pill Indicator
    /** X offset sumber gambar untuk indikator power (pill). */
    public static final double PILL_SRC_X = 169;

    /** Y offset sumber gambar untuk indikator power (pill). */
    public static final double PILL_SRC_Y = 0;

    /** Lebar crop sprite pill (pixel). */
    public static final double PILL_W = 6;

    /** Tinggi crop sprite pill (pixel). */
    public static final double PILL_H = 16;

    // ==========================================
    //          UI LAYOUT & POSITIONING
    // ==========================================

    // Pause Button
    /** Posisi X tombol pause (pixel dari kiri). */
    public static final double PAUSE_BTN_X = 20;

    /** Posisi Y tombol pause (pixel dari atas). */
    public static final double PAUSE_BTN_Y = 20;

    /** Ukuran tombol pause (pixel). */
    public static final double PAUSE_BTN_SIZE = 40;

    // Power Bar
    /** Posisi X dasar untuk power bar (pixel). */
    public static final double POWER_BAR_X = 30;

    /** Lebar maksimal power bar (pixel). */
    public static final double POWER_BAR_MAX_WIDTH = 200;

    /** Tinggi power bar (pixel). */
    public static final double POWER_BAR_HEIGHT = 12;

    // Ball Tracker
    /** Ukuran ikon tracker bola (pixel). */
    public static final double BALL_TRACKER_SIZE = 20;

    /** Jarak antar ikon tracker bola (pixel). */
    public static final double BALL_TRACKER_SPACING = 22;

    // Sidebar Ball Rack
    /** Offset horizontal sidebar dari tepi layar (pixel). */
    public static final double SIDEBAR_OFFSET_FROM_EDGE = 70;

    /** Margin dari bawah untuk sidebar (pixel). */
    public static final double SIDEBAR_BOTTOM_MARGIN = 120;

    /** Ukuran diameter bola yang ditampilkan di sidebar (pixel). */
    public static final double SIDEBAR_BALL_SIZE = 26;

    /** Spasi vertikal antar bola di sidebar (pixel). */
    public static final double SIDEBAR_BALL_SPACING = 28;

    /** Lebar rel/rail pada sidebar (pixel). */
    public static final double SIDEBAR_RAIL_WIDTH = 36; // ballSize + 10

    /** Radius busur pipa sidebar (pixel). */
    public static final double SIDEBAR_PIPE_RADIUS = 70;

    // ==========================================
    //          TABLE CONFIGURATION
    // ==========================================

    /** Ukuran rel meja di sumbu X (pixel pada sprite). */
    public static final double TABLE_RAIL_SIZE_X = 94.0;

    /** Ukuran rel meja di sumbu Y (pixel pada sprite). */
    public static final double TABLE_RAIL_SIZE_Y = 179.0;

    /** Offset gambar meja pada sumbu X (pixel). */
    public static final double TABLE_IMAGE_OFFSET_X = 0;

    /** Offset gambar meja pada sumbu Y (pixel). */
    public static final double TABLE_IMAGE_OFFSET_Y = 52;

    // Pocket Configuration
    /** Toleransi radius masuk pocket (pixel). */
    public static final double POCKET_TOLERANCE = 1.0;

    // Corner Pockets
    /** Radius area masuk pocket pada pojok (pixel). */
    public static final double CORNER_ENTRANCE_RADIUS = 20.0;

    /** Inset untuk entrance corner agar tidak terlalu jauh dari rail (pixel). */
    public static final double CORNER_ENTRANCE_INSET = 5.0;

    /** Radius target tujuan pocket corner (pixel). */
    public static final double CORNER_TARGET_RADIUS = 28.0;

    /** Offset target corner (pixel). */
    public static final double CORNER_TARGET_OFFSET = -12.0;

    // Side Pockets
    /** Radius area masuk pocket samping (pixel). */
    public static final double SIDE_ENTRANCE_RADIUS = 24.0;

    /** Inset untuk entrance samping (pixel). */
    public static final double SIDE_ENTRANCE_INSET = -8.0;

    /** Radius target pada pocket samping (pixel). */
    public static final double SIDE_TARGET_RADIUS = 25.0;

    /** Offset target pada pocket samping (pixel). */
    public static final double SIDE_TARGET_OFFSET = -25.0;

    // ==========================================
    //          RACK SETUP
    // ==========================================

    /** Spacing horizontal antar bola saat rack */
    /** Spacing horizontal antar bola saat rack (pixel). */
    public static final double RACK_HORIZONTAL_SPACING = 2.0;

    /** Spacing vertikal antar bola saat rack (pixel). */
    public static final double RACK_VERTICAL_SPACING = 2.0;

    /** Posisi horizontal rack sebagai rasio lebar meja. */
    public static final double RACK_POSITION_X_RATIO = 0.75;

    /** Buffer tambahan saat penempatan ball-in-hand (pixel). */
    public static final double BALL_PLACEMENT_BUFFER = 2.0;

    // ==========================================
    //          VISUAL EFFECTS
    // ==========================================

    /** Durasi hidup floating text (detik) */
    /** Durasi hidup untuk floating text (detik). */
    public static final double FLOATING_TEXT_LIFETIME = 1.5;

    /** Kecepatan naik floating text (pixel/detik). */
    public static final double FLOATING_TEXT_RISE_SPEED = 30.0;

    /** Interval antar scanline pada CRT effect (pixel). */
    public static final int CRT_SCANLINE_INTERVAL = 4;

    /** Ketebalan scanline CRT effect (pixel). */
    public static final int CRT_SCANLINE_THICKNESS = 2;

    /** Opacity scanline CRT effect (0.0 - 1.0). */
    public static final double CRT_SCANLINE_OPACITY = 0.15;

    // ==========================================
    //          ASSET PATHS
    // ==========================================

    /** Path resource untuk sprite sheet bola. */
    public static final String ASSET_BALL_SPRITE = "/assets/SMS_GUI_Display_NO_BG.png";

    /** Path resource untuk UI sprite sheet. */
    public static final String ASSET_UI_SPRITE = "/assets/SMS_GUI_Display_NO_BG.png";

    /** Path resource untuk gambar meja. */
    public static final String ASSET_TABLE_IMAGE = "/assets/Pool_Table_Type_1_NO_BG Wide.png";

    /** Path resource untuk gambar cue stick. */
    public static final String ASSET_CUE_STICK = "/assets/cuestick.png";

    /** Path resource untuk background menu. */
    public static final String ASSET_MENU_BACKGROUND = "/assets/BackgroundMenu.jpg";

    // ==========================================
    //          FONT PATHS
    // ==========================================

    /** Nama file font Arcade Classic (disertakan di resources). */
    public static final String FONT_ARCADE_CLASSIC = "ArcadeClassic.ttf";

    /** Nama file font VCR OSD (monospace). */
    public static final String FONT_VCR_OSD = "VCR_OSD_MONO_1.001.ttf";

    /** Nama file font Pixel Bold. */
    public static final String FONT_PIXEL_BOLD = "PixelOperator-Bold.ttf";

    // Font Fallbacks
    /** Fallback font untuk mode arcade jika tidak tersedia. */
    public static final String FONT_FALLBACK_ARCADE = "Impact";

    /** Fallback monospace font. */
    public static final String FONT_FALLBACK_VCR = "Courier New";

    /** Fallback font untuk teks pixel-style. */
    public static final String FONT_FALLBACK_PIXEL = "Consolas";

    // ==========================================
    //          PREFERENCES KEYS
    // ==========================================

    /** Key preference untuk menyimpan high score arcade. */
    public static final String PREF_KEY_HIGH_SCORE = "arcade_highscore";

    // ==========================================
    //          COLOR THEMES
    // ==========================================

    /** Warna background aplikasi (margin hitam) - komponen R. */
    public static final int BG_COLOR_R = 20;

    /** Warna background aplikasi (margin hitam) - komponen G. */
    public static final int BG_COLOR_G = 20;

    /** Warna background aplikasi (margin hitam) - komponen B. */
    public static final int BG_COLOR_B = 20;

    /** Warna grid fallback untuk menu background - komponen R. */
    public static final int MENU_GRID_COLOR_R = 0;

    /** Warna grid fallback untuk menu background - komponen G. */
    public static final int MENU_GRID_COLOR_G = 100;

    /** Warna grid fallback untuk menu background - komponen B. */
    public static final int MENU_GRID_COLOR_B = 0;

    /** Warna background fallback untuk menu - komponen R. */
    public static final int MENU_BG_COLOR_R = 0;

    /** Warna background fallback untuk menu - komponen G. */
    public static final int MENU_BG_COLOR_G = 40;

    /** Warna background fallback untuk menu - komponen B. */
    public static final int MENU_BG_COLOR_B = 0;

    /** Interval grid pattern (pixel) */
    public static final int MENU_GRID_INTERVAL = 60;

    // ==========================================
    //          WARNING THRESHOLDS
    // ==========================================

    /** Threshold waktu untuk warning color di timer (detik) */
    public static final double TIME_WARNING_THRESHOLD = 10.0;

    /** Threshold power untuk color transition di power bar */
    /** Threshold power untuk transisi warna: medium. */
    public static final double POWER_MEDIUM_THRESHOLD = 0.5;

    /** Threshold power untuk transisi warna: high. */
    public static final double POWER_HIGH_THRESHOLD = 0.8;

    // ==========================================
    //          BALL SPRITE MAPPING
    // ==========================================

    /** Koordinat sprite bola putih (Cue Ball) di sprite sheet */
    /** Koordinat X sprite untuk Cue Ball. */
    public static final double CUE_BALL_SPRITE_X = 112;

    /** Koordinat Y sprite untuk Cue Ball. */
    public static final double CUE_BALL_SPRITE_Y = 16;

    /** Koordinat sprite bola merah polos (Arcade mode) */
    /** Koordinat X sprite untuk red/plain ball (arcade). */
    public static final double RED_BALL_SPRITE_X = 128;

    /** Koordinat Y sprite untuk red/plain ball (arcade). */
    public static final double RED_BALL_SPRITE_Y = 0;

    /** Koordinat sprite bola kuning polos (Arcade mode) */
    /** Koordinat X sprite untuk yellow/plain ball (arcade). */
    public static final double YELLOW_BALL_SPRITE_X = 128;

    /** Koordinat Y sprite untuk yellow/plain ball (arcade). */
    public static final double YELLOW_BALL_SPRITE_Y = 16;
}
