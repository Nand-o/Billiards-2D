package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import java.util.List;

public class CueStick implements GameObject {

    private CueBall cueBall;
    private List<Ball> allBalls; // Referensi ke semua bola musuh
    private double tableWidth, tableHeight;

    // State Aiming
    private boolean isAiming = false;
    private Vector2D aimStart;
    private Vector2D aimCurrent;
    private Vector2D mousePos = new Vector2D(0, 0);
    private double lockedAngleRad = 0;

    // Visual Constants
    private static final double MAX_PULL = 300.0;
    private static final double MAX_FORCE = 1400.0;
    private static final double MAX_DRAG_DISTANCE = 300.0;

    // Constructor Baru: Menerima List Bola & Ukuran Meja
    public CueStick(CueBall cueBall, List<Ball> allBalls, double tableW, double tableH) {
        this.cueBall = cueBall;
        this.allBalls = allBalls;
        this.tableWidth = tableW;
        this.tableHeight = tableH;
    }

    @Override
    public void update(double deltaTime) {
        //
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (cueBall.getVelocity().length() > 0.1) return;

        // 1. Tentukan Sudut Bidikan (Locked atau Mouse)
        double angleRad;
        if (!isAiming) {
            double dx = mousePos.getX() - cueBall.getPosition().getX();
            double dy = mousePos.getY() - cueBall.getPosition().getY();
            angleRad = Math.atan2(dy, dx);
            lockedAngleRad = angleRad;
        } else {
            angleRad = lockedAngleRad;
        }

        // 2. Gambar Garis Prediksi (Raycast)
        double shootAngle = angleRad + Math.PI;
        Vector2D shootDir = new Vector2D(Math.cos(shootAngle), Math.sin(shootAngle)).normalize();

        drawPredictionRay(gc, cueBall.getPosition(), shootDir);

        // 3. Gambar Stik (Visual)
        drawStickVisual(gc, angleRad);
    }

    // --- LOGIKA UTAMA: RAYCASTING & PREDIKSI ---
    private void drawPredictionRay(GraphicsContext gc, Vector2D start, Vector2D dir) {
        double closestDist = 1000.0;
        Vector2D hitPoint = start.add(dir.multiply(closestDist));
        Ball targetBall = null;
        boolean hitWall = false;

        // A. Cek Tabrakan Dinding (Wall Intersection)
        double distToRight = (tableWidth - cueBall.getRadius() - start.getX()) / dir.getX();
        double distToLeft = (cueBall.getRadius() - start.getX()) / dir.getX();
        double distToBottom = (tableHeight - cueBall.getRadius() - start.getY()) / dir.getY();
        double distToTop = (cueBall.getRadius() - start.getY()) / dir.getY();

        if (distToRight > 0) closestDist = Math.min(closestDist, distToRight);
        if (distToLeft > 0) closestDist = Math.min(closestDist, distToLeft);
        if (distToBottom > 0) closestDist = Math.min(closestDist, distToBottom);
        if (distToTop > 0) closestDist = Math.min(closestDist, distToTop);

        hitWall = true;

        // B. Cek Tabrakan Bola (Ray-Circle Intersection)
        for (Ball other : allBalls) {
            if (other == cueBall) continue;

            if (!other.isActive()) continue;

            // Logika "Ghost Ball": Kita cari titik dimana pusat bola putih berjarak 2*Radius
            Vector2D toBall = other.getPosition().subtract(start);
            double t = toBall.dot(dir); // Proyeksi ke garis aim

            // Jika bola ada di belakang arah bidikan, skip
            if (t < 0) continue;

            // Jarak tegak lurus dari garis aim ke pusat bola musuh
            Vector2D projPoint = start.add(dir.multiply(t));
            double distPerp = other.getPosition().subtract(projPoint).length();
            double collisionDist = cueBall.getRadius() + other.getRadius();

            // Jika jarak tegak lurus < 2*Radius, berarti kena
            if (distPerp < collisionDist) {
                // Hitung mundur dari titik proyeksi ke titik sentuh (Pythagoras)
                double dt = Math.sqrt(collisionDist * collisionDist - distPerp * distPerp);
                double distToHit = t - dt;

                if (distToHit > 0 && distToHit < closestDist) {
                    closestDist = distToHit;
                    targetBall = other;
                    hitWall = false;
                }
            }
        }

        // Titik akhir garis utama
        hitPoint = start.add(dir.multiply(closestDist));

        // --- GAMBAR LINE VISUAL ---

        // 1. Garis Utama (Putih Putus-putus)
        gc.save();
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(5);
        gc.strokeLine(start.getX(), start.getY(), hitPoint.getX(), hitPoint.getY());

        // Gambar "Ghost Ball" (Bayangan bola putih di titik tabrakan)
        if (targetBall != null || hitWall) {
            gc.setGlobalAlpha(0.3);
            gc.strokeOval(hitPoint.getX() - cueBall.getRadius(), hitPoint.getY() - cueBall.getRadius(),
                    cueBall.getRadius()*2, cueBall.getRadius()*2);
            gc.setGlobalAlpha(1.0);
        }

        // 2. PERCABANGAN (Jika kena bola)
        if (targetBall != null) {
            // Vektor Normal (Garis hubung pusat kedua bola saat tabrakan)
            Vector2D collisionNormal = targetBall.getPosition().subtract(hitPoint).normalize();

            // Vektor Tangent (Garis singgung / Arah pantul bola putih)
            // Tegak lurus (-y, x) dari normal
            Vector2D tangent = new Vector2D(-collisionNormal.getY(), collisionNormal.getX());

            // Cek arah tangent (harus searah dengan gerakan asli)
            if (dir.dot(tangent) < 0) {
                tangent = tangent.multiply(-1);
            }

            gc.setLineDashes(null);

            // Cabang 1: Arah Bola Musuh (Merah) - Mengikuti Normal
            gc.setStroke(Color.RED);
            gc.strokeLine(targetBall.getPosition().getX(), targetBall.getPosition().getY(),
                    targetBall.getPosition().getX() + collisionNormal.getX() * 50,
                    targetBall.getPosition().getY() + collisionNormal.getY() * 50);

            // Cabang 2: Arah Bola Putih (Putih/Biru) - Mengikuti Tangent (90 derajat)
            gc.setStroke(Color.CYAN);
            gc.strokeLine(hitPoint.getX(), hitPoint.getY(),
                    hitPoint.getX() + tangent.getX() * 50,
                    hitPoint.getY() + tangent.getY() * 50);
        }
        gc.restore();
    }

    private void drawStickVisual(GraphicsContext gc, double angleRad) {
        double angleDeg = Math.toDegrees(angleRad);
        double pullDistance = 20;

        if (isAiming) {
            double dragDist = aimStart.subtract(aimCurrent).length();
            pullDistance = Math.min(dragDist, MAX_PULL);
        }

        gc.save();
        gc.translate(cueBall.getPosition().getX(), cueBall.getPosition().getY());
        gc.rotate(angleDeg);

        double stickLen = 300;
        double stickWidth = 8;
        double tipOffset = cueBall.getRadius() + pullDistance;

        // Gambar Batang
        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(tipOffset, -stickWidth/2, stickLen, stickWidth);
        gc.setFill(Color.CYAN);
        gc.fillRect(tipOffset, -stickWidth/2, 5, stickWidth);

        gc.restore();
    }

    // --- INPUT HANDLING (TETAP SAMA) ---
    public void handleMouseMoved(MouseEvent e) {
        if (!isAiming) this.mousePos = new Vector2D(e.getX(), e.getY());
    }

    public void handleMousePressed(MouseEvent e) {
        if (cueBall.getVelocity().length() > 0) return;
        isAiming = true;
        aimStart = new Vector2D(e.getX(), e.getY());
        aimCurrent = new Vector2D(e.getX(), e.getY());
    }

    public void handleMouseDragged(MouseEvent e) {
        if (!isAiming) return;
        aimCurrent = new Vector2D(e.getX(), e.getY());
    }

    public void handleMouseReleased(MouseEvent e) {
        if (!isAiming) return;
        double dragDist = aimStart.subtract(aimCurrent).length();

        // Hitung Arah Tembakan
        if (dragDist > MAX_DRAG_DISTANCE) dragDist = MAX_DRAG_DISTANCE;
        double dragRatio = dragDist / MAX_DRAG_DISTANCE;
        double powerCurve = dragRatio * dragRatio;
        double finalForce = powerCurve * MAX_FORCE;

        double shootAngle = lockedAngleRad + Math.PI;
        Vector2D direction = new Vector2D(Math.cos(shootAngle), Math.sin(shootAngle)).normalize();
        if (finalForce > 5) {
            cueBall.hit(direction.multiply(finalForce));
        }
        isAiming = false;
    }
}