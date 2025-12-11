package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class Table implements GameObject {

    // --- INNER CLASS UPDATED ---
    private class Pocket {
        Vector2D entrancePos; // Posisi Lingkaran Cyan (Untuk mematikan dinding)
        double entranceRadius;

        Vector2D targetPos;   // Posisi Lingkaran Magenta (Titik bola mati/masuk)
        double targetRadius;

        Pocket(Vector2D entPos, double entR, Vector2D tgtPos, double tgtR) {
            this.entrancePos = entPos;
            this.entranceRadius = entR;
            this.targetPos = tgtPos;
            this.targetRadius = tgtR;
        }
    }

    private double width;
    private double height;

    // ==========================================
    //          AREA TUNING (RACIKAN PRO)
    // ==========================================

    private boolean debugMode = true;

    // 1. VISUAL MEJA
    private double railSizeX = 86.0;
    private double railSizeY = 163.0;
    private double imageOffsetX = 0;
    private double imageOffsetY = 48;

    // --- 2. CONFIG CORNER POCKETS (4 POJOK) ---
    // Cyan (Entrance): Area dimana bola menembus dinding
    private double cornerEntranceRadius = 20.0;
    private double cornerEntranceInset  = 5.0;  // Geser masuk ke area hijau (+)

    // Magenta (Target): Titik bola dianggap masuk/hilang
    // Tips: Buat target offset NEGATIF agar titiknya ada di dalam kayu (pojok dalam)
    private double cornerTargetRadius = 28.0; // Radius area "kill"
    private double cornerTargetOffset = -12.0; // Geser KELUAR ke arah kayu (-)

    // --- 3. CONFIG SIDE POCKETS (2 TENGAH) ---
    private double sideEntranceRadius = 24.0;
    private double sideEntranceInset  = -8.0;

    private double sideTargetRadius = 25.0;
    private double sideTargetOffset = -25.0; // Geser KELUAR ke arah kayu (Vertikal)

    // ==========================================

    private List<Pocket> pockets;
    private static Image tableImage;

    // Toleransi kita buat 1.0 karena sekarang kita punya TargetRadius sendiri
    private static final double POCKET_TOLERANCE = 1.0;

    public Table(double width, double height) {
        this.width = width;
        this.height = height;

        if (tableImage == null) {
            try {
                tableImage = new Image(getClass().getResourceAsStream("/assets/Pool_Table_Type_1_NO_BG Wide.png"));
            } catch (Exception e) {
                System.err.println("Gagal memuat gambar meja: " + e.getMessage());
            }
        }
        initPockets();
    }

    private void initPockets() {
        pockets = new ArrayList<>();

        // Helper variables
        double w = width;
        double h = height;

        // --- CORNER POCKETS (Manual Coordinate Logic) ---
        // Kita atur arah offset (keluar/masuk) untuk setiap pojok

        // 1. KIRI ATAS (Top-Left)
        // Entrance: Geser Kanan-Bawah (+, +)
        // Target: Geser Kiri-Atas (-, -)
        pockets.add(new Pocket(
                new Vector2D(0 + cornerEntranceInset, 0 + cornerEntranceInset), cornerEntranceRadius,
                new Vector2D(0 + cornerTargetOffset,  0 + cornerTargetOffset),  cornerTargetRadius
        ));

        // 2. KANAN ATAS (Top-Right)
        // Entrance: Geser Kiri-Bawah (-, +)
        // Target: Geser Kanan-Atas (+, -)
        // Note: Offset harus dibalik tandanya sesuai kuadran
        pockets.add(new Pocket(
                new Vector2D(w - cornerEntranceInset, 0 + cornerEntranceInset), cornerEntranceRadius,
                new Vector2D(w - cornerTargetOffset,  0 + cornerTargetOffset),  cornerTargetRadius
        ));

        // 3. KIRI BAWAH (Bottom-Left)
        // Entrance: Geser Kanan-Atas (+, -)
        // Target: Geser Kiri-Bawah (-, +)
        pockets.add(new Pocket(
                new Vector2D(0 + cornerEntranceInset, h - cornerEntranceInset), cornerEntranceRadius,
                new Vector2D(0 + cornerTargetOffset,  h - cornerTargetOffset),  cornerTargetRadius
        ));

        // 4. KANAN BAWAH (Bottom-Right)
        // Entrance: Geser Kiri-Atas (-, -)
        // Target: Geser Kanan-Bawah (+, +)
        pockets.add(new Pocket(
                new Vector2D(w - cornerEntranceInset, h - cornerEntranceInset), cornerEntranceRadius,
                new Vector2D(w - cornerTargetOffset,  h - cornerTargetOffset),  cornerTargetRadius
        ));

        // --- SIDE POCKETS ---

        // 5. TENGAH ATAS
        pockets.add(new Pocket(
                new Vector2D(w/2, 0 + sideEntranceInset), sideEntranceRadius,
                new Vector2D(w/2, 0 + sideTargetOffset),  sideTargetRadius
        ));

        // 6. TENGAH BAWAH
        pockets.add(new Pocket(
                new Vector2D(w/2, h - sideEntranceInset), sideEntranceRadius,
                new Vector2D(w/2, h - sideTargetOffset),  sideTargetRadius
        ));
    }

    @Override
    public void update(double deltaTime) { }

    @Override
    public void draw(GraphicsContext gc) {
        gc.save();

        // 1. Visual Meja
        double totalWidth = width + (railSizeX * 2);
        double totalHeight = height + (railSizeY * 2);

        if (tableImage != null) {
            double drawX = -railSizeX + imageOffsetX;
            double drawY = -railSizeY + imageOffsetY;
            gc.drawImage(tableImage, drawX, drawY, totalWidth, totalHeight);
        } else {
            gc.setFill(Color.GREEN);
            gc.fillRect(0, 0, width, height);
        }

        // 2. Debug Overlay
        if (debugMode) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(2);
            gc.strokeRect(0, 0, width, height);

            for(Pocket p : pockets) {
                // CYAN = ENTRANCE ZONE (Tembok Mati)
                gc.setFill(Color.rgb(0, 255, 255, 0.4));
                gc.fillOval(p.entrancePos.getX() - p.entranceRadius,
                        p.entrancePos.getY() - p.entranceRadius,
                        p.entranceRadius * 2, p.entranceRadius * 2);

                // MAGENTA = TARGET ZONE (Bola Masuk)
                gc.setFill(Color.rgb(255, 0, 255, 0.6));
                gc.fillOval(p.targetPos.getX() - p.targetRadius,
                        p.targetPos.getY() - p.targetRadius,
                        p.targetRadius * 2, p.targetRadius * 2);
            }
        }

        gc.restore();
    }

    // UPDATE LOGIC: Cek terhadap TARGET POS (Magenta)
    public boolean isBallInPocket(Ball ball) {
        for (Pocket p : pockets) {
            double dx = ball.getPosition().getX() - p.targetPos.getX();
            double dy = ball.getPosition().getY() - p.targetPos.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < p.targetRadius * POCKET_TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    // UPDATE LOGIC: Perketat deteksi Entrance
    public boolean isInsidePocketEntrance(Ball ball) {
        for (Pocket p : pockets) {
            double dx = ball.getPosition().getX() - p.entrancePos.getX();
            double dy = ball.getPosition().getY() - p.entrancePos.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            // PERBAIKAN BUG BOLA BOCOR:
            // Hapus "+ ball.getRadius()".
            // Kita ingin dinding hanya mati jika TITIK TENGAH bola sudah masuk area Cyan.
            // Ini mencegah bola yang hanya "menyenggol" pinggiran lubang untuk tembus dinding.

            if (distance < p.entranceRadius) {
                return true;
            }
        }
        return false;
    }

    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getWallThickness() { return Math.max(railSizeX, railSizeY); }
}