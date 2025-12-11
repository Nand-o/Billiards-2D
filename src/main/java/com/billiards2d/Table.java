package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class Table implements GameObject {

    private double width;  // Lebar Area Hijau (Logic)
    private double height; // Tinggi Area Hijau (Logic)

    // --- VARIABEL TUNING (SESUAIKAN INI) ---

    // 1. Debug Mode (Matikan jika sudah pas)
    private boolean debugMode = true;

    // 2. KETEBALAN DINDING VISUAL (RAILS)
    // Ubah ini agar gambar tidak gepeng.
    // Jika lubang terlihat "Gepeng Horizontal" (melebar) -> BESARKAN railSizeX
    // Jika lubang terlihat "Gepeng Vertikal" (memanjang) -> BESARKAN railSizeY
    private double railSizeX = 86.0; // Tebal kayu Kiri & Kanan
    private double railSizeY = 163.0; // Tebal kayu Atas & Bawah

    // 3. POSISI GAMBAR
    // Geser gambar agar Pas di tengah kotak merah
    private double imageOffsetX = 0;
    private double imageOffsetY = 48;

    // ---------------------------------------

    private double pocketRadius = 20.0;
    private List<Vector2D> pockets;
    private static Image tableImage;
    private static final double POCKET_TOLERANCE = 0.875;

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
        pockets.add(new Vector2D(0, 0));
        pockets.add(new Vector2D(width / 2, 0));
        pockets.add(new Vector2D(width, 0));
        pockets.add(new Vector2D(0, height));
        pockets.add(new Vector2D(width / 2, height));
        pockets.add(new Vector2D(width, height));
    }

    @Override
    public void update(double deltaTime) {
        // Static
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.save();

        // --- 1. GAMBAR MEJA (VISUAL) ---
        // Kita hitung ukuran gambar total berdasarkan ketebalan rel yang Anda set
        double totalWidth = width + (railSizeX * 2);
        double totalHeight = height + (railSizeY * 2);

        if (tableImage != null) {
            // Gambar dimulai dari minus ketebalan rel
            double drawX = -railSizeX + imageOffsetX;
            double drawY = -railSizeY + imageOffsetY;

            gc.drawImage(tableImage, drawX, drawY, totalWidth, totalHeight);
        } else {
            gc.setFill(Color.GREEN);
            gc.fillRect(0, 0, width, height);
        }

        // --- 2. DEBUG BORDER (PHYSICS) ---
        // Area ini JANGAN BERUBAH. Sesuaikan gambar di atas agar masuk ke sini.
        if (debugMode) {
            // Border Merah (Batas Fisika)
            gc.setStroke(Color.RED);
            gc.setLineWidth(3);
            gc.strokeRect(0, 0, width, height);

            // Lubang Fisika (Cyan)
            gc.setFill(Color.CYAN);
            for(Vector2D p : pockets) {
                gc.fillOval(p.getX()-5, p.getY()-5, 10, 10);
            }
        }

        gc.restore();
    }

    public boolean isBallInPocket(Ball ball) {
        for (Vector2D pocketPos : pockets) {
            double dx = ball.getPosition().getX() - pocketPos.getX();
            double dy = ball.getPosition().getY() - pocketPos.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < pocketRadius * POCKET_TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    public double getWidth() { return width; }
    public double getHeight() { return height; }
    // Method ini dipanggil BilliardApp untuk hitung ukuran Canvas
    // Kita pakai X terbesar untuk margin aman
    public double getWallThickness() { return Math.max(railSizeX, railSizeY); }
}