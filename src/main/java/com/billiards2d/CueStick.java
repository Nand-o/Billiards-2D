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
    public CueStick(CueBall cueBall, List<Ball> allBalls, double tableW, double tableH) {
        this.cueBall = cueBall;
        this.allBalls = allBalls;
        this.tableWidth = tableW;
        this.tableHeight = tableH;
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

        // A. Cek Tabrakan Dinding (Wall Intersection)
        // Menghitung jarak ke setiap sisi dinding berdasarkan arah vektor
        double distToRight = (tableWidth - cueBall.getRadius() - start.getX()) / dir.getX();
        double distToLeft = (cueBall.getRadius() - start.getX()) / dir.getX();
        double distToBottom = (tableHeight - cueBall.getRadius() - start.getY()) / dir.getY();
        double distToTop = (cueBall.getRadius() - start.getY()) / dir.getY();

        // Cari jarak positif terpendek (dinding yang akan ditabrak pertama kali)
        if (distToRight > 0) closestDist = Math.min(closestDist, distToRight);
        if (distToLeft > 0) closestDist = Math.min(closestDist, distToLeft);
        if (distToBottom > 0) closestDist = Math.min(closestDist, distToBottom);
        if (distToTop > 0) closestDist = Math.min(closestDist, distToTop);

        hitWall = true; // Default asumsi kena dinding dulu

        // B. Cek Tabrakan Bola (Ray-Circle Intersection)
        for (Ball other : allBalls) {
            if (other == cueBall) continue;

            // PENTING: Abaikan bola yang sudah masuk lubang (tidak aktif)
            if (!other.isActive()) continue;

            // Logika "Ghost Ball": Kita cari titik di mana pusat bola putih berjarak 2*Radius
            Vector2D toBall = other.getPosition().subtract(start);
            double t = toBall.dot(dir); // Proyeksi vektor bola ke garis bidikan

            // Jika bola ada di belakang arah bidikan, abaikan
            if (t < 0) continue;

            // Jarak tegak lurus dari garis aim ke pusat bola musuh
            Vector2D projPoint = start.add(dir.multiply(t));
            double distPerp = other.getPosition().subtract(projPoint).length();
            double collisionDist = cueBall.getRadius() + other.getRadius();

            // Jika jarak tegak lurus < 2*Radius, berarti akan terjadi tabrakan
            if (distPerp < collisionDist) {
                // Hitung mundur dari titik proyeksi ke titik sentuh sebenarnya (Pythagoras)
                double dt = Math.sqrt(collisionDist * collisionDist - distPerp * distPerp);
                double distToHit = t - dt;

                // Jika tabrakan ini lebih dekat dari dinding atau bola sebelumnya, simpan ini
                if (distToHit > 0 && distToHit < closestDist) {
                    closestDist = distToHit;
                    targetBall = other;
                    hitWall = false; // Kena bola, bukan dinding
                }
            }
        }

        // Update titik akhir garis prediksi
        hitPoint = start.add(dir.multiply(closestDist));

        // --- GAMBAR LINE VISUAL ---

        // 1. Garis Utama (Putih Putus-putus)
        gc.save();
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(5); // Efek putus-putus
        gc.strokeLine(start.getX(), start.getY(), hitPoint.getX(), hitPoint.getY());

        // Gambar "Ghost Ball" (Lingkaran outline di titik tabrakan)
        if (targetBall != null || hitWall) {
            gc.setGlobalAlpha(0.3); // Transparan
            gc.strokeOval(hitPoint.getX() - cueBall.getRadius(), hitPoint.getY() - cueBall.getRadius(),
                    cueBall.getRadius()*2, cueBall.getRadius()*2);
            gc.setGlobalAlpha(1.0);
        }

        // 2. PERCABANGAN PREDIKSI (Jika kena bola)
        if (targetBall != null) {
            // Vektor Normal: Garis hubung pusat kedua bola saat tabrakan
            Vector2D collisionNormal = targetBall.getPosition().subtract(hitPoint).normalize();

            // Vektor Tangent: Garis singgung (tegak lurus dari normal) -> Arah pantul bola putih
            Vector2D tangent = new Vector2D(-collisionNormal.getY(), collisionNormal.getX());

            // Pastikan arah tangent searah dengan gerakan asli (forward)
            if (dir.dot(tangent) < 0) {
                tangent = tangent.multiply(-1);
            }

            gc.setLineDashes(null); // Garis solid

            double predictionLength = 50.0; // Panjang garis prediksi lanjutan

            // Prediksi Arah Bola Musuh (Merah) - Selalu mengikuti Normal
            gc.setStroke(Color.RED);
            gc.strokeLine(targetBall.getPosition().getX(), targetBall.getPosition().getY(),
                    targetBall.getPosition().getX() + collisionNormal.getX() * predictionLength,
                    targetBall.getPosition().getY() + collisionNormal.getY() * predictionLength);

            // Prediksi Arah Bola Putih (Cyan) - Selalu mengikuti Tangent (90 derajat)
            gc.setStroke(Color.CYAN);
            gc.strokeLine(hitPoint.getX(), hitPoint.getY(),
                    hitPoint.getX() + tangent.getX() * predictionLength,
                    hitPoint.getY() + tangent.getY() * predictionLength);
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
    private boolean areAllBallsStopped() {
        for (Ball ball : allBalls) {
            // Hanya cek bola yang masih aktif di meja
            if (ball.isActive() && ball.getVelocity().length() > 0.1) {
                return false; // Masih ada yang bergerak
            }
        }
        return true; // Semua diam
    }
}