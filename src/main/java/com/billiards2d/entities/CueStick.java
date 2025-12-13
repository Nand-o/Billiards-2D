package com.billiards2d.entities;

import static com.billiards2d.core.GameConstants.*;

import com.billiards2d.core.GameObject;
import com.billiards2d.entities.balls.Ball;
import com.billiards2d.entities.balls.CueBall;
import com.billiards2d.entities.balls.ObjectBall;
import com.billiards2d.game.GameRules;
import com.billiards2d.util.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import javafx.scene.image.Image;

/**
 * Mekanika stik biliar yang bertanggung jawab pada aksi bidik dan pukul.
 * <p>
 * Menangani input pemain yang berkaitan dengan pengisian power, arahan pukulan,
 * serta men-generate visualisasi prediksi (ghost ball dan garis arah).
 * CueStick berinteraksi erat dengan {@link com.billiards2d.entities.balls.CueBall}
 * dan subsistem aturan {@link com.billiards2d.game.GameRules}.
 * </p>
 *
 * Perhatikan: komentar ini hanya mendokumentasikan API; teks yang tampil di UI
 * (mis. notifikasi kemenangan) tetap dalam Bahasa Inggris dan tidak diubah.
 *
 * @since 2025-12-13
 */
public class CueStick implements GameObject {

    private CueBall cueBall;
    private List<Ball> allBalls; // Referensi ke semua bola untuk perhitungan prediksi
    private double tableWidth, tableHeight;
    private GameRules gameRules;
    private boolean arcadeMode = false;
    private double pullbackDistance = 0;

    // --- State Aiming (Status Bidikan) ---
    private boolean isAiming = false;
    private Vector2D aimStart;
    private Vector2D aimCurrent;
    private Vector2D mousePos = new Vector2D(0, 0);
    private double lockedAngleRad = 0;

    private final Image stickImage;

    /**
     * Konstruktor CueStick.
     *
     * @param cueBall  Referensi ke bola putih yang akan dipukul.
     * @param allBalls Daftar semua bola di meja (untuk deteksi prediksi tabrakan).
     * @param tableW   Lebar meja (untuk prediksi pantulan dinding).
     * @param tableH   Tinggi meja.
     */
    public CueStick(CueBall cueBall, List<Ball> allBalls, double tableW, double tableH, GameRules rules, Image stickImage) {
        this.cueBall = cueBall;
        this.allBalls = allBalls;
        this.tableWidth = tableW;
        this.tableHeight = tableH;
        this.gameRules = rules;
        this.stickImage = stickImage;
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
        // Cek 1: Jangan gambar jika bola masih bergerak
        if (!areAllBallsStopped()) return;

        // Cek 2: Pastikan gambar sudah diload
        if (stickImage == null) return;

        // 1. Tentukan Sudut Bidikan
        double angleRad;

        if (!isAiming) {
            // MODE MEMBIDIK (HOVER)
            double dx = mousePos.getX() - cueBall.getPosition().getX();
            double dy = mousePos.getY() - cueBall.getPosition().getY();

            // Sudut MURNI dari Bola ke Mouse + 180 derajat (PI)
            // Hasil: Stik berada di SEBERANG Mouse (posisi memukul)
            angleRad = Math.atan2(dy, dx) + Math.PI;

            lockedAngleRad = angleRad; // Simpan untuk mode drag nanti
        } else {
            // MODE MENARIK (DRAG) - Sudut dikunci
            angleRad = lockedAngleRad;
        }

        // 2. Gambar Garis Prediksi (Raycast)
        // Arah tembakan = Kebalikan posisi stik (kembali ke arah mouse)
        double shootAngle = angleRad + Math.PI;
        Vector2D shootDir = new Vector2D(Math.cos(shootAngle), Math.sin(shootAngle)).normalize();

        // Pastikan method drawPredictionRay sudah ada (atau pakai garis simple)
        drawPredictionRay(gc, cueBall.getPosition(), shootDir);

        // 3. Gambar Stik Fisik (Menggunakan Gambar Assets)
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
        // --- 1. SETTING UKURAN ---
        // Tentukan panjang stik yang diinginkan di layar (misal 500 pixel biar panjang)
        double targetLength = 550.0;

        // --- 2. HITUNG KETEBALAN OTOMATIS (ASPECT RATIO) ---
        // Ini kuncinya: Biarkan program menghitung ketebalan berdasarkan gambar asli
        // agar stik tidak terlihat "gepeng" atau "terlalu tipis".
        double originalWidth = 1.0;
        double originalHeight = 1.0;

        if (stickImage != null) {
            originalWidth = stickImage.getWidth();
            originalHeight = stickImage.getHeight() / 1.5; // Karena gambar berisi 2 bagian (handle + tip)
        }

        // Rumus: Skala = Panjang Target / Lebar Asli
        double scale = targetLength / originalWidth;

        // Ketebalan baru mengikuti skala tersebut
        double drawWidth = targetLength;
        double drawHeight = originalHeight * scale;

        // OPTIONAL: Jika masih merasa kurang tebal, Anda bisa menambahkan pengali manual
        // Contoh: drawHeight = originalHeight * scale * 1.5; (tapi biasanya ratio asli sudah cukup)


        // --- 3. HITUNG POSISI JARAK (OFFSET) ---
        // Jarak ujung stik dari pusat bola + animasi tarik mundur (pullback)
        double distFromBall = cueBall.getRadius() + STICK_OFFSET_FROM_BALL + this.pullbackDistance;

        gc.save(); // Simpan state asli

        // A. Pindahkan titik 0,0 ke PUSAT BOLA PUTIH
        gc.translate(cueBall.getPosition().getX(), cueBall.getPosition().getY());

        // B. ROTASI
        gc.rotate(Math.toDegrees(angleRad));

        // C. POSISI GAMBAR
        // Kita geser X sejauh distFromBall.
        // Kita geser Y sejauh setengah ketebalan stik (agar center).

        double drawX = distFromBall;
        double drawY = -drawHeight / 2.08;

        // --- 4. GAMBAR ---
        if (stickImage != null) {
            // Gambar dengan ukuran yang sudah dihitung rasionya
            gc.drawImage(stickImage, drawX, drawY, drawWidth, drawHeight);
        } else {
            // Fallback jika gambar error
            gc.setFill(Color.SADDLEBROWN);
            gc.fillRect(drawX, drawY, drawWidth, 20);
        }

        gc.restore(); // Balikin state
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

    public void handleMouseDragged(double mouseX, double mouseY) {
        if (isAiming) {
            aimCurrent = new Vector2D(mouseX, mouseY);

            // 1. Hitung Vektor Arah Bidikan (Aim Direction)
            // Arah pukulan (Aim) adalah kebalikan dari stik (lockedAngleRad + PI)
            double shootAngle = lockedAngleRad;
            Vector2D direction = new Vector2D(Math.cos(shootAngle), Math.sin(shootAngle)).normalize();

            // 2. Hitung Vektor Tarikan Mouse (Drag Vector)
            Vector2D dragVector = aimCurrent.subtract(aimStart);

            // 3. Proyeksikan Vektor Tarikan ke Arah Bidikan
            // Ini memastikan tarikan hanya dihitung sepanjang garis lurus ke belakang
            double dragDist = dragVector.dot(direction);

            // 4. Batasi Jarak Tarik (Clamp)
            // Jangan sampai negatif (mendorong) atau melebihi batas maksimum
            dragDist = Math.max(0, dragDist);
            dragDist = Math.min(dragDist, MAX_DRAG_DISTANCE);

            // 5. SET JARAK MUNDUR
            // Gunakan jarak yang sudah diproyeksikan dan dibatasi
            this.pullbackDistance = dragDist; // <--- Variabel ini yang menggerakkan stik
        }
    }

    public void handleMouseReleased(double x, double y) {
        if (!isAiming) return;

        // Pastikan aimCurrent diupdate terakhir
        aimCurrent = new Vector2D(x, y);

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
        this.pullbackDistance = 0;
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
