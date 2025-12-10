package com.billiards2d;

import com.billiards2d.core.GameBus;
import javafx.scene.canvas.GraphicsContext;
import java.util.List;

/**
 * Mesin Fisika yang menangani semua interaksi fisik dalam permainan.
 * <p>
 * Kelas ini bertanggung jawab untuk:
 * 1. Mendeteksi dan menyelesaikan tumbukan antara bola dengan dinding meja.
 * 2. Mendeteksi dan menyelesaikan tumbukan antar bola (Elastic Collision).
 * 3. Mendeteksi jika bola masuk ke dalam lubang (Pocket).
 * </p>
 * Kelas ini menerapkan prinsip Single Responsibility Principle (SRP) dengan memisahkan
 * logika simulasi fisika yang kompleks dari kelas entitas {@link Ball} itu sendiri.
 */
public class PhysicsEngine implements GameObject {

    private Table table;
    private List<GameObject> gameObjects;

    /**
     * Konstruktor PhysicsEngine.
     *
     * @param table       Referensi ke meja untuk mengetahui batas dinding dan posisi lubang.
     * @param gameObjects Referensi ke daftar semua objek game untuk iterasi deteksi tumbukan.
     */
    public PhysicsEngine(Table table, List<GameObject> gameObjects) {
        this.table = table;
        this.gameObjects = gameObjects;
    }

    /**
     * Memperbarui simulasi fisika untuk satu frame.
     * Metode ini memeriksa interaksi setiap bola terhadap lingkungan dan bola lainnya.
     *
     * @param deltaTime Waktu yang berlalu sejak frame terakhir.
     */
    @Override
    public void update(double deltaTime) {
        // Gunakan loop index (bukan iterator) untuk menghindari crash jika list berubah
        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject obj1 = gameObjects.get(i);

            if (!(obj1 instanceof Ball)) continue;
            Ball b1 = (Ball) obj1;

            if (!b1.isActive()) continue; // Lewati bola yang tidak aktif

            // --- 1. Cek Lubang (Pocket Detection) ---
            if (table.isBallInPocket(b1)) {

                // NOTIFIKASI KE APP (Via GameBus)
                // PhysicsEngine tidak mengubah skor, hanya lapor "Bola Masuk"
                GameBus.publish(GameBus.EventType.BALL_POTTED, b1);

                if (b1 instanceof CueBall) {
                    CueBall cb = (CueBall) b1;
                    cb.setActive(false);
                    cb.setPendingRespawn(true); // Tandai minta respawn
                    cb.setVelocity(new Vector2D(0,0));
                } else {
                    b1.setActive(false); // Hilangkan bola dari meja
                }
                continue; // Skip logika collision untuk bola ini
            }

            // --- 2. Cek Tumbukan Dinding (Wall Collision) ---
            checkWallCollision(b1);

            // --- 3. Cek Tumbukan Antar Bola (Ball-to-Ball Collision) ---
            for (int j = 0; j < gameObjects.size(); j++) {
                if (i == j) continue; // Skip diri sendiri

                GameObject obj2 = gameObjects.get(j);
                if (!(obj2 instanceof Ball)) continue;

                Ball b2 = (Ball) obj2;
                if (!b2.isActive()) continue; // Skip bola mati

                resolveBallCollision(b1, b2);
            }
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // PhysicsEngine adalah objek logika, tidak memiliki representasi visual.
    }

    /**
     * Memeriksa dan menangani tumbukan bola dengan dinding meja.
     */
    private void checkWallCollision(Ball ball) {
        double x = ball.getPosition().getX();
        double y = ball.getPosition().getY();
        double r = ball.getRadius();

        double vx = ball.getVelocity().getX();
        double vy = ball.getVelocity().getY();

        boolean collided = false;
        double wallRestitution = 0.9;

        if (x - r < 0) { x = r; vx = -vx * wallRestitution; collided = true; }
        else if (x + r > table.getWidth()) { x = table.getWidth() - r; vx = -vx * wallRestitution; collided = true; }

        if (y - r < 0) { y = r; vy = -vy * wallRestitution; collided = true; }
        else if (y + r > table.getHeight()) { y = table.getHeight() - r; vy = -vy * wallRestitution; collided = true; }

        if (collided) {
            ball.setPosition(new Vector2D(x, y));
            ball.setVelocity(new Vector2D(vx, vy));
        }
    }

    /**
     * Menyelesaikan tumbukan antara dua bola menggunakan fisika tumbukan lenting (Elastic Collision).
     */
    private void resolveBallCollision(Ball b1, Ball b2) {
        Vector2D posDiff = b1.getPosition().subtract(b2.getPosition());
        double dist = posDiff.length();
        double minDist = b1.getRadius() + b2.getRadius();

        if (dist == 0 || dist >= minDist) return;

        double overlap = 0.5 * (dist - minDist);
        Vector2D displacement = posDiff.normalize().multiply(overlap);
        b1.setPosition(b1.getPosition().subtract(displacement));
        b2.setPosition(b2.getPosition().add(displacement));

        Vector2D normalVector = posDiff.normalize();
        Vector2D relativeVel = b1.getVelocity().subtract(b2.getVelocity());
        double speed = relativeVel.dot(normalVector);
        if (speed >= 0) return;

        double impulse = 2 * speed / (b1.getMass() + b2.getMass());
        double restitution = 0.9;

        b1.setVelocity(b1.getVelocity().subtract(normalVector.multiply(impulse * b2.getMass())).multiply(restitution));
        b2.setVelocity(b2.getVelocity().add(normalVector.multiply(impulse * b1.getMass())).multiply(restitution));
    }
}