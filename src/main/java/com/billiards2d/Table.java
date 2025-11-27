package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class Table implements GameObject {

    private double width;
    private double height;

    // Visual settings
    private double wallThickness = 25.0;
    private double pocketRadius = 20.0;
    private List<Vector2D> pockets;

    public Table(double width, double height) {
        this.width = width;
        this.height = height;
        initPockets();
    }

    private void initPockets() {
        pockets = new ArrayList<>();
        // 6 lubang
        double off = 5.0;
        pockets.add(new Vector2D(-off, -off)); // Kiri Atas
        pockets.add(new Vector2D(width/2, -off*1.5)); // Tengah Atas
        pockets.add(new Vector2D(width+off, -off)); // Kanan Atas
        pockets.add(new Vector2D(-off, height+off)); // Kiri Bawah
        pockets.add(new Vector2D(width/2, height+off*1.5)); // Tengah Bawah
        pockets.add(new Vector2D(width+off, height+off)); // Kanan Bawah
    }

    @Override
    public void update(double deltaTime) {
        // Static object, no updates
    }

    @Override
    public void draw(GraphicsContext gc) {
        // --- GAMBAR BACKGROUND (Lantai) ---
        gc.setFill(Color.rgb(20, 20, 20));
        gc.fillRect(-100, -100, width + 500, height + 500);

        // --- SETUP KOORDINAT DINDING ---
        gc.save();
        gc.translate(wallThickness, wallThickness);

        // 1. Gambar Frame Kayu
        gc.setFill(Color.SADDLEBROWN.darker());
        gc.fillRect(-wallThickness, -wallThickness, width + wallThickness*2, height + wallThickness*2);

        // 2. Gambar Karpet Hijau
        gc.setFill(Color.web("#006400"));
        gc.fillRect(0, 0, width, height);

        // 3. Gambar Diamond Sights (Titik Putih di Kayu)
        drawDiamonds(gc);

        // 4. GAMBAR GARIS BREAK (HEAD STRING)
        drawBreakLine(gc);

        // 5. Gambar Lubang
        gc.setFill(Color.BLACK);
        for (Vector2D p : pockets) {
            gc.fillOval(p.getX() - pocketRadius, p.getY() - pocketRadius, pocketRadius*2, pocketRadius*2);
        }

        gc.restore();
    }

    // Method untuk menggambar garis break
    private void drawBreakLine(GraphicsContext gc) {
        double breakLineX = width * 0.25; // Posisi 1/4 dari kiri meja

        // A. Gambar Garis Putih Tipis
        gc.setStroke(Color.rgb(255, 255, 255, 0.5));
        gc.setLineWidth(2);
        gc.strokeLine(breakLineX, 0, breakLineX, height);

        // B. Gambar Titik Head Spot (Titik tengah di garis break)
        gc.setFill(Color.WHITE);
        double spotSize = 6;
        gc.fillOval(breakLineX - spotSize/2, (height/2) - spotSize/2, spotSize, spotSize);
    }

    private void drawDiamonds(GraphicsContext gc) {
        gc.setFill(Color.BEIGE);
        double dSize = 5;
        for(int i=1; i<4; i++) {
            gc.fillOval(-wallThickness/2, (height/4)*i, dSize, dSize);
            gc.fillOval(width + wallThickness/2 - dSize, (height/4)*i, dSize, dSize);
        }
    }

    // Getters
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getWallThickness() { return wallThickness; }
}