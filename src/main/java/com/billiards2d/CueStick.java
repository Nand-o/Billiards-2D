package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import java.util.List;

/**
 * Kelas yang merepresentasikan Stik Biliar (Cue Stick).
 * <p>
 * Kelas ini menangani semua interaksi input pemain (mouse), termasuk:
 * 1. Membidik (menggerakkan mouse di sekitar bola putih).
 * 2. Mengatur kekuatan pukulan (drag-and-release mechanic).
 * 3. Menampilkan visualisasi bantuan seperti garis prediksi dan ghost ball.
 * </p>
 */
public class CueStick implements GameObject {

    private CueBall cueBall;
    private List<Ball> allBalls; // Referensi ke semua bola untuk perhitungan prediksi
    private double tableWidth, tableHeight;
    private GameRules gameRules;
    private boolean arcadeMode = false;

    // --- State Aiming (Status Bidikan) ---
    private boolean isAiming = false;       // Apakah pemain sedang menahan klik mouse?
    private Vector2D aimStart;              // Posisi mouse saat klik pertama kali
    private Vector2D aimCurrent;            // Posisi mouse saat ini (saat di-drag)
    private Vector2D mousePos = new Vector2D(0, 0); // Posisi mouse umum (untuk rotasi stik)
    private double lockedAngleRad = 0;      // Sudut stik yang terkunci saat mulai menarih

    // --- Konstanta Fisika & Visual ---
    // Jarak maksimal stik bisa ditarik mundur secara visual (pixel)
    private static final double MAX_PULL = 300.0;
    // Gaya maksimal yang bisa diberikan ke bola (satuan fisika arbitrer)
    private static final double MAX_FORCE = 1350.0;
    // Jarak tarik mouse yang dianggap sebagai kekuatan penuh (pixel)
    private static final double MAX_DRAG_DISTANCE = 300.0;

    /**
     * Konstruktor CueStick.
     *
     * @param cueBall  Referensi ke bola putih yang akan dipukul.
     * @param allBalls Daftar semua bola di meja (untuk deteksi prediksi tabrakan).
     * @param tableW   Lebar meja (untuk prediksi pantulan dinding).
     * @param tableH   Tinggi meja.
     */
    public CueStick(CueBall cueBall, List<Ball> allBalls, double tableW, double tableH, GameRules rules) {
        this.cueBall = cueBall;
        this.allBalls = allBalls;
        this.tableWidth = tableW;
        this.tableHeight = tableH;
        this.gameRules = rules; // Simpan rules
    }

    public void setArcadeMode(boolean isArcade) {
        this.arcadeMode = isArcade;
    }

    @Override
    public void update(double deltaTime) {
        // Logika update stik bisa ditambahkan di sini (misal animasi idle)
        // Saat ini kosong karena stik digerakkan murni oleh event mouse
    }

    /**
     * Menggambar stik dan elemen visual pendukung (garis prediksi).
     * Stik hanya digambar jika bola putih sedang berhenti atau bergerak sangat lambat.
     */
    @Override
    public void draw(GraphicsContext gc) {
        if (!areAllBallsStopped()) return;

        // 1. Tentukan Sudut Bidikan
        double angleRad;

        if (!isAiming) {
            // MODE MEMBIDIK (HOVER)
            // Hitung jarak mouse dari bola
            double dx = mousePos.getX() - cueBall.getPosition().getX();
            double dy = mousePos.getY() - cueBall.getPosition().getY();

            // --- CORE LOGIC FIX ---
            // Math.atan2(dy, dx) = Sudut MURNI dari Bola ke Mouse.
            // Kita tambah Math.PI (180 derajat) agar stik berada di SEBERANG Mouse.
            // Hasil: Mouse di Kanan (Target) -> Stik muncul di Kiri (Siap pukul).
            angleRad = Math.atan2(dy, dx) + Math.PI;

            lockedAngleRad = angleRad; // Simpan sudut terakhir
        } else {
            // MODE MENARIK (DRAG)
            // Sudut dikunci, tidak berubah meskipun mouse gerak kiri-kanan
            angleRad = lockedAngleRad;
        }

        // 2. Gambar Garis Prediksi (Raycast)
        // Arah tembakan adalah KEBALIKAN dari posisi stik.
        // Posisi Stik = angleRad.
        // Arah Tembak = angleRad + PI (180 derajat lagi) = Kembali ke arah Mouse.
        double shootAngle = angleRad + Math.PI;
        Vector2D shootDir = new Vector2D(Math.cos(shootAngle), Math.sin(shootAngle)).normalize();

        drawPredictionRay(gc, cueBall.getPosition(), shootDir);

        // 3. Gambar Stik Fisik
        drawStickVisual(gc, angleRad);
    }

    /**
     * Logika Raycasting untuk memprediksi lintasan bola putih.
     * Menggambar garis putus-putus dan "Ghost Ball" di titik tabrakan yang diprediksi.
     */
    private void drawPredictionRay(GraphicsContext gc, Vector2D start, Vector2D dir) {
        double closestDist = 1000.0;
        Vector2D hitPoint = start.add(dir.multiply(closestDist));
        Ball targetBall = null;
        boolean hitWall = false;

        // Logic Raycast (Dinding) - Tetap Sama
        double distToRight = (tableWidth - cueBall.getRadius() - start.getX()) / dir.getX();
        double distToLeft = (cueBall.getRadius() - start.getX()) / dir.getX();
        double distToBottom = (tableHeight - cueBall.getRadius() - start.getY()) / dir.getY();
        double distToTop = (cueBall.getRadius() - start.getY()) / dir.getY();

        if (distToRight > 0) closestDist = Math.min(closestDist, distToRight);
        if (distToLeft > 0) closestDist = Math.min(closestDist, distToLeft);
        if (distToBottom > 0) closestDist = Math.min(closestDist, distToBottom);
        if (distToTop > 0) closestDist = Math.min(closestDist, distToTop);
        hitWall = true;

        // Logic Raycast (Bola) - Tetap Sama
        for (Ball other : allBalls) {
            if (other == cueBall || !other.isActive()) continue;
            Vector2D toBall = other.getPosition().subtract(start);
            double t = toBall.dot(dir);
            if (t < 0) continue;
            Vector2D projPoint = start.add(dir.multiply(t));
            double distPerp = other.getPosition().subtract(projPoint).length();
            double collisionDist = cueBall.getRadius() + other.getRadius();
            if (distPerp < collisionDist) {
                double dt = Math.sqrt(collisionDist * collisionDist - distPerp * distPerp);
                double distToHit = t - dt;
                if (distToHit > 0 && distToHit < closestDist) {
                    closestDist = distToHit;
                    targetBall = other;
                    hitWall = false;
                }
            }
        }
        hitPoint = start.add(dir.multiply(closestDist));

        // --- VISUAL VALIDATOR ---
        boolean isValidShot = true;

        if (!arcadeMode) {
            if (targetBall != null && targetBall instanceof ObjectBall) {
                ObjectBall objBall = (ObjectBall) targetBall;
                // Tanya GameRules (Hanya di 8-Ball Mode)
                isValidShot = gameRules.isValidTarget(objBall.getType(), allBalls);
            }
        } else {
            // DI ARCADE MODE: Semua bola sah!
            isValidShot = true;
        }

        gc.save();

        // Warna Garis: Putih (Valid) atau Merah (Invalid)
        if (isValidShot) {
            gc.setStroke(Color.WHITE);
        } else {
            gc.setStroke(Color.RED); // Warning color
        }

        gc.setLineWidth(2);
        gc.setLineDashes(5);
        gc.strokeLine(start.getX(), start.getY(), hitPoint.getX(), hitPoint.getY());

        // Ghost Ball
        if (targetBall != null || hitWall) {
            gc.setGlobalAlpha(0.5);
            if (isValidShot) gc.setStroke(Color.WHITE);
            else gc.setStroke(Color.RED);

            gc.strokeOval(hitPoint.getX() - cueBall.getRadius(), hitPoint.getY() - cueBall.getRadius(),
                    cueBall.getRadius()*2, cueBall.getRadius()*2);
            gc.setGlobalAlpha(1.0);
        }

        // TANDA SILANG (CROSS) JIKA INVALID
        if (!isValidShot && targetBall != null) {
            gc.setStroke(Color.RED);
            gc.setLineWidth(3);
            double x = hitPoint.getX();
            double y = hitPoint.getY();
            double s = 10; // Ukuran silang
            gc.setLineDashes(null);
            gc.strokeLine(x - s, y - s, x + s, y + s);
            gc.strokeLine(x + s, y - s, x - s, y + s);
        }

        // Prediksi Lanjutan (Hanya gambar jika shot valid, biar ga menuhin layar pas salah)
        if (targetBall != null && isValidShot) {
            // ... (Kode visual prediksi pantulan yang lama tetap di sini) ...
            Vector2D collisionNormal = targetBall.getPosition().subtract(hitPoint).normalize();
            Vector2D tangent = new Vector2D(-collisionNormal.getY(), collisionNormal.getX());
            if (dir.dot(tangent) < 0) tangent = tangent.multiply(-1);

            gc.setLineDashes(null);
            double predLen = 500.0;

            gc.setStroke(Color.RED);
            gc.strokeLine(targetBall.getPosition().getX(), targetBall.getPosition().getY(),
                    targetBall.getPosition().getX() + collisionNormal.getX() * predLen,
                    targetBall.getPosition().getY() + collisionNormal.getY() * predLen);

            gc.setStroke(Color.CYAN);
            gc.strokeLine(hitPoint.getX(), hitPoint.getY(),
                    hitPoint.getX() + tangent.getX() * predLen,
                    hitPoint.getY() + tangent.getY() * predLen);
        }
        gc.restore();
    }

    /**
     * Menggambar bentuk visual stik biliar.
     * Stik digambar dengan rotasi sesuai sudut bidikan dan posisi mundur sesuai tarikan mouse.
     */
    private void drawStickVisual(GraphicsContext gc, double angleRad) {
        double angleDeg = Math.toDegrees(angleRad);
        double pullDistance = 20; // Jarak default dari bola

        // Jika sedang membidik, stik mundur sesuai jarak tarik mouse
        if (isAiming) {
            double dragDist = aimStart.subtract(aimCurrent).length();
            pullDistance = Math.min(dragDist, MAX_PULL);
        }

        gc.save();
        // Pindahkan titik asal (0,0) ke pusat bola putih untuk memudahkan rotasi
        gc.translate(cueBall.getPosition().getX(), cueBall.getPosition().getY());
        gc.rotate(angleDeg);

        double stickLen = 300;
        double stickWidth = 8;
        double tipOffset = cueBall.getRadius() + pullDistance;

        // Gambar Batang Kayu
        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(tipOffset, -stickWidth/2, stickLen, stickWidth);
        // Gambar Ujung Stik (Tip)
        gc.setFill(Color.CYAN);
        gc.fillRect(tipOffset, -stickWidth/2, 5, stickWidth);

        gc.restore();
    }

    // --- UPDATE HANDLERS: Terima koordinat langsung ---

    public void handleMouseMoved(double x, double y) {
        if (!isAiming) this.mousePos = new Vector2D(x, y);
    }

    public void handleMousePressed(double x, double y) {
        if (!areAllBallsStopped()) return;
        isAiming = true;
        aimStart = new Vector2D(x, y);
        aimCurrent = new Vector2D(x, y);
    }

    public void handleMouseDragged(double x, double y) {
        if (!isAiming) return;
        aimCurrent = new Vector2D(x, y);
    }

    public void handleMouseReleased(double x, double y) {
        if (!isAiming) return;

        // Logika release tetap sama... (Code tidak berubah)
        // ... (copy paste logika release yang lama) ...
        // ...

        // Pastikan aimCurrent diupdate terakhir
        aimCurrent = new Vector2D(x, y);

        // ... (Lanjutkan logika hitung force & hit bola) ...

        double dragDist = aimStart.subtract(aimCurrent).length();
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

    // Helper: Cek apakah SEMUA bola (putih + warna) sudah berhenti
    public boolean areAllBallsStopped() {
        for (Ball ball : allBalls) {
            // Hanya cek bola yang masih aktif di meja
            if (ball.isActive() && ball.getVelocity().length() > 0.1) {
                return false; // Masih ada yang bergerak
            }
        }
        return true; // Semua diam
    }

    public boolean isAiming() {
        return isAiming;
    }

    /**
     * Mengembalikan rasio kekuatan tarikan saat ini (0.0 sampai 1.0).
     * Digunakan untuk menggambar panjang Power Bar di UI.
     */
    public double getPowerRatio() {
        if (!isAiming) return 0.0;

        // Hitung jarak tarik
        double dragDist = aimStart.subtract(aimCurrent).length();

        // Clamp (Batasi) maksimum
        if (dragDist > MAX_DRAG_DISTANCE) dragDist = MAX_DRAG_DISTANCE;

        // Return rasio (0.0 - 1.0)
        return dragDist / MAX_DRAG_DISTANCE;
    }
}