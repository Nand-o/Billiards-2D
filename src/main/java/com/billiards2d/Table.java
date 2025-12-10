package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Kelas yang merepresentasikan Meja Biliar.
 * <p>
 * Meja berfungsi sebagai lingkungan permainan yang memiliki batas fisik (dinding/bantalan)
 * dan area skor (lubang). Kelas ini bertanggung jawab untuk menggambar visual meja
 * dan menyediakan logika deteksi apakah bola masuk ke dalam lubang.
 * </p>
 */
public class Table implements GameObject {

    private double width;
    private double height;

    // --- Pengaturan Visual & Fisik ---
    /** Ketebalan dinding/bantalan meja dalam pixel. */
    private double wallThickness = 25.0;

    /** Jari-jari visual dari lubang meja. */
    private double pocketRadius = 20.0;

    /** Daftar posisi koordinat pusat dari ke-6 lubang meja. */
    private List<Vector2D> pockets;

    /**
     * Faktor toleransi untuk deteksi bola masuk lubang.
     * Nilai 0.855 berarti bola dianggap masuk jika jaraknya < (radius lubang * 0.855).
     */
    private static final double POCKET_TOLERANCE = 0.875;

    /**
     * Konstruktor Meja.
     *
     * @param width  Lebar area permainan (bagian hijau saja).
     * @param height Tinggi area permainan (bagian hijau saja).
     */
    public Table(double width, double height) {
        this.width = width;
        this.height = height;
        initPockets();
    }

    /**
     * Menginisialisasi posisi ke-6 lubang meja standar.
     * Lubang ditempatkan di setiap sudut (4) dan di tengah sisi panjang (2).
     */
    private void initPockets() {
        pockets = new ArrayList<>();

        // Kiri Atas
        pockets.add(new Vector2D(0, 0));
        // Tengah Atas
        pockets.add(new Vector2D(width / 2, 0));
        // Kanan Atas
        pockets.add(new Vector2D(width, 0));

        // Kiri Bawah
        pockets.add(new Vector2D(0, height));
        // Tengah Bawah
        pockets.add(new Vector2D(width / 2, height));
        // Kanan Bawah
        pockets.add(new Vector2D(width, height));
    }

    @Override
    public void update(double deltaTime) {
        // Meja adalah objek statis, tidak memerlukan update logika per frame.
    }

    /**
     * Menggambar representasi visual meja secara berlapis.
     */
    @Override
    public void draw(GraphicsContext gc) {
        // 1. Gambar Background Lantai (agar tidak ada sisa frame sebelumnya di luar meja)
        gc.setFill(Color.rgb(20, 20, 20));
        gc.fillRect(-100, -100, width + 500, height + 500);

        // Simpan state grafis sebelum transformasi koordinat
        gc.save();
        // Geser titik (0,0) menggambar ke area dalam dinding
        gc.translate(wallThickness, wallThickness);

        // 2. Gambar Frame Kayu (Bingkai Luar)
        gc.setFill(Color.SADDLEBROWN.darker());
        gc.fillRect(-wallThickness, -wallThickness, width + wallThickness * 2, height + wallThickness * 2);

        // 3. Gambar Karpet Hijau (Area Permainan)
        gc.setFill(Color.web("#006400"));
        gc.fillRect(0, 0, width, height);

        // 4. Gambar Detail Visual (Diamond Sights & Garis Break)
        drawDiamonds(gc);
        drawBreakLine(gc);

        // 5. Gambar 6 Lubang (Pockets)
        gc.setFill(Color.BLACK);
        for (Vector2D p : pockets) {
            // Menggambar lingkaran hitam di posisi lubang
            gc.fillOval(p.getX() - pocketRadius, p.getY() - pocketRadius, pocketRadius * 2, pocketRadius * 2);
        }

        // Kembalikan state grafis ke posisi semula
        gc.restore();
    }

    /**
     * Menggambar garis putih tipis (Head String) dan titik Head Spot.
     */
    private void drawBreakLine(GraphicsContext gc) {
        double breakLineX = width * 0.25; // Posisi 1/4 dari kiri meja

        // A. Gambar Garis Putih Tipis
        gc.setStroke(Color.rgb(255, 255, 255, 0.5));
        gc.setLineWidth(2);
        gc.strokeLine(breakLineX, 0, breakLineX, height);

        // B. Gambar Titik Head Spot (Titik tengah di garis break)
        gc.setFill(Color.WHITE);
        double spotSize = 6;
        gc.fillOval(breakLineX - spotSize / 2, (height / 2) - spotSize / 2, spotSize, spotSize);
    }

    /**
     * Menggambar titik-titik penanda (Diamonds) di bingkai kayu meja.
     */
    private void drawDiamonds(GraphicsContext gc) {
        gc.setFill(Color.BEIGE);
        double dSize = 5;
        // Menggambar 3 titik di setiap sisi vertikal (kiri dan kanan)
        for (int i = 1; i < 4; i++) {
            gc.fillOval(-wallThickness / 2, (height / 4) * i, dSize, dSize);
            gc.fillOval(width + wallThickness / 2 - dSize, (height / 4) * i, dSize, dSize);
        }
    }

    /**
     * Memeriksa apakah sebuah bola telah masuk ke dalam salah satu lubang.
     *
     * @param ball Objek bola yang akan diperiksa.
     * @return true jika bola masuk ke dalam radius deteksi lubang, false jika tidak.
     */
    public boolean isBallInPocket(Ball ball) {
        for (Vector2D pocketPos : pockets) {
            // Hitung jarak Euclidean antara pusat bola dan pusat lubang
            double dx = ball.getPosition().getX() - pocketPos.getX();
            double dy = ball.getPosition().getY() - pocketPos.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Syarat masuk lubang: jarak < (radius lubang * toleransi)
            if (distance < pocketRadius * POCKET_TOLERANCE) {
                return true; // Bola masuk lubang
            }
        }
        return false; // Bola tidak masuk lubang
    }

    // --- Getter ---

    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getWallThickness() { return wallThickness; }
}