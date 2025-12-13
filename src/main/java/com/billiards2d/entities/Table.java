package com.billiards2d.entities;

import static com.billiards2d.core.GameConstants.*;

import com.billiards2d.core.GameObject;
import com.billiards2d.entities.balls.Ball;
import com.billiards2d.util.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Representasi meja biliar lengkap dengan kantong (pockets) dan dinding.
 * <p>
 * Meja mengelola area permainan, deteksi masuk kantong, serta mematikan
 * dinding di sekitar entrance pocket ketika bola akan melewati lubang.
 * Logic ini penting untuk interaksi antara {@link com.billiards2d.entities.balls.Ball}
 * dan subsistem fisika {@link com.billiards2d.game.PhysicsEngine}.
 * </p>
 *
 * @since 2025-12-13
 */
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

    private List<Pocket> pockets;
    private static Image tableImage;

    public Table(double width, double height) {
        this.width = width;
        this.height = height;

        if (tableImage == null) {
            try {
                tableImage = new Image(getClass().getResourceAsStream(ASSET_TABLE_IMAGE));
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
                new Vector2D(0 + CORNER_ENTRANCE_INSET, 0 + CORNER_ENTRANCE_INSET), CORNER_ENTRANCE_RADIUS,
                new Vector2D(0 + CORNER_TARGET_OFFSET,  0 + CORNER_TARGET_OFFSET),  CORNER_TARGET_RADIUS
        ));

        // 2. KANAN ATAS (Top-Right)
        // Entrance: Geser Kiri-Bawah (-, +)
        // Target: Geser Kanan-Atas (+, -)
        // Note: Offset harus dibalik tandanya sesuai kuadran
        pockets.add(new Pocket(
                new Vector2D(w - CORNER_ENTRANCE_INSET, 0 + CORNER_ENTRANCE_INSET), CORNER_ENTRANCE_RADIUS,
                new Vector2D(w - CORNER_TARGET_OFFSET,  0 + CORNER_TARGET_OFFSET),  CORNER_TARGET_RADIUS
        ));

        // 3. KIRI BAWAH (Bottom-Left)
        // Entrance: Geser Kanan-Atas (+, -)
        // Target: Geser Kiri-Bawah (-, +)
        pockets.add(new Pocket(
                new Vector2D(0 + CORNER_ENTRANCE_INSET, h - CORNER_ENTRANCE_INSET), CORNER_ENTRANCE_RADIUS,
                new Vector2D(0 + CORNER_TARGET_OFFSET,  h - CORNER_TARGET_OFFSET),  CORNER_TARGET_RADIUS
        ));

        // 4. KANAN BAWAH (Bottom-Right)
        // Entrance: Geser Kiri-Atas (-, -)
        // Target: Geser Kanan-Bawah (+, +)
        pockets.add(new Pocket(
                new Vector2D(w - CORNER_ENTRANCE_INSET, h - CORNER_ENTRANCE_INSET), CORNER_ENTRANCE_RADIUS,
                new Vector2D(w - CORNER_TARGET_OFFSET,  h - CORNER_TARGET_OFFSET),  CORNER_TARGET_RADIUS
        ));

        // --- SIDE POCKETS ---

        // 5. TENGAH ATAS
        pockets.add(new Pocket(
                new Vector2D(w/2, 0 + SIDE_ENTRANCE_INSET), SIDE_ENTRANCE_RADIUS,
                new Vector2D(w/2, 0 + SIDE_TARGET_OFFSET),  SIDE_TARGET_RADIUS
        ));

        // 6. TENGAH BAWAH
        pockets.add(new Pocket(
                new Vector2D(w/2, h - SIDE_ENTRANCE_INSET), SIDE_ENTRANCE_RADIUS,
                new Vector2D(w/2, h - SIDE_TARGET_OFFSET),  SIDE_TARGET_RADIUS
        ));
    }

    @Override
    public void update(double deltaTime) { }

    @Override
    public void draw(GraphicsContext gc) {
        gc.save();

        // 1. Visual Meja
        double totalWidth = width + (TABLE_RAIL_SIZE_X * 2);
        double totalHeight = height + (TABLE_RAIL_SIZE_Y * 2);

        if (tableImage != null) {
            double drawX = -TABLE_RAIL_SIZE_X + TABLE_IMAGE_OFFSET_X;
            double drawY = -TABLE_RAIL_SIZE_Y + TABLE_IMAGE_OFFSET_Y;
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
    public double getWallThickness() { return Math.max(TABLE_RAIL_SIZE_X, TABLE_RAIL_SIZE_Y); }
}
