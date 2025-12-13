package com.billiards2d.game;

import static com.billiards2d.core.GameConstants.*;

import com.billiards2d.core.GameObject;
import com.billiards2d.entities.balls.Ball;
import com.billiards2d.entities.balls.CueBall;
import com.billiards2d.entities.balls.ObjectBall;
import com.billiards2d.entities.Table;
import com.billiards2d.util.Vector2D;
import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Mesin fisika sederhana untuk simulasi gerak dan tabrakan bola.
 * <p>
 * Tanggung jawab utama:
 * - Memperbarui posisi/kecepatan bola berdasarkan integrasi sederhana.
 * - Menangani pantulan dinding, restitusi (bounciness), dan friction.
 * - Mendeteksi bola yang masuk pocket dan melaporkannya ke reporter.
 * </p>
 *
 * Catatan: Kelas ini tidak melakukan rendering; hanya mengatur status fisik
 * dari objek-objek permainan.
 *
 * @since 2025-12-13
 */
public class PhysicsEngine implements GameObject {

    private Table table;
    private List<GameObject> gameObjects;

    // Mode Arcade Score (Disimpan tapi tidak diprint otomatis)
    private int arcadeScore = 0;

    // --- REPORTER DATA (PENTING UNTUK 8-BALL) ---
    // List ini menampung bola yang masuk dalam satu pukulan
    private List<ObjectBall> pocketedBalls = new ArrayList<>();
    private boolean cueBallPocketed = false;
    private Ball firstHitBall = null;

    /**
     * Konstruktor untuk `PhysicsEngine`.
     *
     * @param table referensi ke meja permainan
     * @param gameObjects daftar objek permainan yang akan disimulasikan (biasanya semua bola)
     */
    public PhysicsEngine(Table table, List<GameObject> gameObjects) {
        this.table = table;
        this.gameObjects = gameObjects;
    }

    // Getter untuk BilliardApp mengambil data laporan
    /**
     * Ambil salinan list bola yang ter-pocket selama pukulan terakhir.
     *
     * @return list baru berisi `ObjectBall` yang ter-pocket pada turn terakhir
     */
    public List<ObjectBall> getPocketedBalls() {
        return new ArrayList<>(pocketedBalls);
    }

    /**
     * Apakah bola putih (cue ball) ter-pocket pada pukulan terakhir.
     *
     * @return true jika cue ball ter-pocket
     */
    public boolean isCueBallPocketed() {
        return cueBallPocketed;
    }

    // Getter untuk GameRules
    /**
     * Bola pertama yang tersentuh oleh cue ball pada pukulan terakhir.
     * Digunakan untuk validasi aturan first-hit.
     *
     * @return instansi `Ball` yang pertama tersentuh, atau null jika tidak ada
     */
    public Ball getFirstHitBall() {
        return firstHitBall;
    }

    /**
     * Reset laporan turn (pocketedBalls, cueBallPocketed, firstHitBall).
     * Dipanggil saat memulai turn baru.
     */
    public void resetTurnReport() {
        pocketedBalls.clear();
        cueBallPocketed = false;
        firstHitBall = null; // Reset setiap awal turn
    }

    /**
     * Ambil skor sementara untuk mode Arcade.
     *
     * @return nilai skor arcade saat ini
     */
    public int getArcadeScore() {
        return arcadeScore;
    }

    /**
     * Update simulasi fisika untuk semua bola (dipanggil per frame).
     *
     * @param deltaTime waktu sejak frame terakhir (detik)
     */
    @Override
    public void update(double deltaTime) {
        for (GameObject obj1 : gameObjects) {
            if (!(obj1 instanceof Ball)) continue;
            Ball b1 = (Ball) obj1;

            if (!b1.isActive()) continue;

            // --- 1. CEK LUBANG ---
            if (table.isBallInPocket(b1)) {

                if (b1 instanceof CueBall) {
                    // CUE BALL MASUK
                    CueBall cb = (CueBall) b1;
                    cb.setActive(false);
                    cb.setPendingRespawn(true);
                    cb.setVelocity(new Vector2D(0,0));

                    // Catat kejadian ini
                    cueBallPocketed = true;
                    arcadeScore = Math.max(0, arcadeScore - 10);

                } else if (b1 instanceof ObjectBall) {
                    // BOLA WARNA MASUK
                    ObjectBall ob = (ObjectBall) b1;
                    ob.setActive(false);

                    // PENTING: Masukkan ke list laporan agar dibaca GameRules
                    pocketedBalls.add(ob);

                    // Update score arcade (diam-diam)
                    arcadeScore += 10;
                }
                continue;
            }

            // --- 2. CEK TUMBUKAN DINDING ---
            checkWallCollision(b1);

            // --- 3. CEK TUMBUKAN ANTAR BOLA ---
            for (GameObject obj2 : gameObjects) {
                if (!(obj2 instanceof Ball)) continue;
                Ball b2 = (Ball) obj2;
                if (b1 == b2) continue;
                resolveBallCollision(b1, b2);
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) { }

    private void checkWallCollision(Ball ball) {
        // Fitur Tembus Dinding di Mulut Lubang
        if (table.isInsidePocketEntrance(ball)) {
            return;
        }

        double x = ball.getPosition().getX();
        double y = ball.getPosition().getY();
        double r = ball.getRadius();
        double vx = ball.getVelocity().getX();
        double vy = ball.getVelocity().getY();
        boolean collided = false;

        if (x - r < table.getWidth() && x - r < 0) { // Cek batas kiri (0)
            x = r; vx = -vx * WALL_RESTITUTION; collided = true;
        } else if (x + r > table.getWidth()) {
            x = table.getWidth() - r; vx = -vx * WALL_RESTITUTION; collided = true;
        }

        if (y - r < table.getHeight() && y - r < 0) { // Cek batas atas (0)
            y = r; vy = -vy * WALL_RESTITUTION; collided = true;
        } else if (y + r > table.getHeight()) {
            y = table.getHeight() - r; vy = -vy * WALL_RESTITUTION; collided = true;
        }

        if (collided) {
            ball.setPosition(new Vector2D(x, y));
            ball.setVelocity(new Vector2D(vx, vy));
        }
    }

    private void resolveBallCollision(Ball b1, Ball b2) {
        Vector2D posDiff = b1.getPosition().subtract(b2.getPosition());
        double dist = posDiff.length();
        double minDist = b1.getRadius() + b2.getRadius();

        if (dist == 0 || dist >= minDist) return;

        if (firstHitBall == null) {
            if (b1 instanceof CueBall && b2 instanceof ObjectBall) {
                firstHitBall = b2;
            } else if (b2 instanceof CueBall && b1 instanceof ObjectBall) {
                firstHitBall = b1;
            }
        }

        double overlap = 0.5 * (dist - minDist);
        Vector2D displacement = posDiff.normalize().multiply(overlap);

        b1.setPosition(b1.getPosition().subtract(displacement));
        b2.setPosition(b2.getPosition().add(displacement));

        Vector2D normalVector = posDiff.normalize();
        Vector2D relativeVel = b1.getVelocity().subtract(b2.getVelocity());
        double speed = relativeVel.dot(normalVector);

        if (speed >= 0) return;

        double impulse = 2 * speed / (b1.getMass() + b2.getMass());

        b1.setVelocity(b1.getVelocity().subtract(normalVector.multiply(impulse * b2.getMass())).multiply(BALL_RESTITUTION));
        b2.setVelocity(b2.getVelocity().add(normalVector.multiply(impulse * b1.getMass())).multiply(BALL_RESTITUTION));
    }

    /**
     * Mengubah skor arcade secara manual (bisa plus atau minus).
     * Berguna untuk penalti foul atau bonus.
     *
     * @param amount jumlah perubahan skor (positif/negatif)
     */
    public void modifyArcadeScore(int amount) {
        this.arcadeScore += amount;
        // Opsional: Cegah skor negatif
        if (this.arcadeScore < 0) this.arcadeScore = 0;
    }

    /**
     * DEBUG FEATURE: Memaksa bola masuk ke dalam laporan (Cheat).
     *
     * @param ball bola yang akan ditandai sebagai ter-pocketed
     */
    public void forcePocketBall(ObjectBall ball) {
        if (!pocketedBalls.contains(ball)) {
            pocketedBalls.add(ball);
            // Jangan lupa update score arcade juga biar puas liat angkanya naik
            arcadeScore += 10;
        }
    }

    /**
     * Konstruktor PhysicsEngine.
     *
     * @param table referensi ke meja permainan
     * @param gameObjects daftar objek permainan yang disimulasikan
     */
    // Note: constructor JavaDoc placed near top for readability
}
