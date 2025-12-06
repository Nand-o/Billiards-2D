package com.billiards2d;

import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
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
    private int playerScore = 0;

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
     * Returns the current player score tracked by the physics engine.
     *
     * This value is updated by the engine when scoring events occur (for example,
     * when object balls are pocketed). Use this method to obtain read-only access
     * to the player's current score.
     *
     * @return the current player score
     */
    public int getPlayerScore() {
        return playerScore;
    }

    /**
     * Memperbarui simulasi fisika untuk satu frame.
     * Metode ini memeriksa interaksi setiap bola terhadap lingkungan dan bola lainnya.
     *
     * @param deltaTime Waktu yang berlalu sejak frame terakhir.
     */
    @Override
    public void update(double deltaTime) {
        for (GameObject obj1 : gameObjects) {
            // Hanya proses objek yang bertipe Ball
            if (!(obj1 instanceof Ball)) continue;
            Ball b1 = (Ball) obj1;

            if (!b1.isActive()) continue; // Lewati bola yang tidak aktif

            // --- 1. Cek Lubang (Pocket Detection) ---
            // Cek ini dilakukan PERTAMA, karena jika bola masuk lubang,
            // ia tidak perlu lagi memantul ke dinding atau bola lain.
            if (table.isBallInPocket(b1)) {
                if (b1 instanceof CueBall) {
                    // Aturan Foul: Jika bola putih masuk, reset ke posisi awal
                    CueBall cb = (CueBall) b1;
                    cb.setActive(false);         // Hilangkan dari meja (fisika)
                    cb.setPendingRespawn(true);  // Tandai butuh respawn nanti
                    cb.setVelocity(new Vector2D(0,0)); // Nol-kan kecepatan

                    // Penalti Skor saat bola putih masuk lubang
                    playerScore = Math.max(0, playerScore - 10);
                    System.out.println("Foul! Cue ball pocketed. Score penalized. Current Score: " + playerScore);
                } else {
                    // Jika bola objek masuk, tandai untuk dihapus dari permainan
                    b1.setActive(false);

                    // Tambah skor pemain
                    playerScore += 10;
                    System.out.println("Object ball pocketed! Current Score: " + playerScore);
                }
                continue; // Skip sisa logika fisika untuk bola ini
            }

            // --- 2. Cek Tumbukan Dinding (Wall Collision) ---
            checkWallCollision(b1);

            // --- 3. Cek Tumbukan Antar Bola (Ball-to-Ball Collision) ---
            for (GameObject obj2 : gameObjects) {
                if (!(obj2 instanceof Ball)) continue;
                Ball b2 = (Ball) obj2;

                // Jangan cek tumbukan dengan diri sendiri
                if (b1 == b2) continue;

                // Selesaikan tumbukan jika terjadi
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
     * Jika bola menabrak dinding, arah kecepatannya akan dipantulkan.
     */
    private void checkWallCollision(Ball ball) {
        double x = ball.getPosition().getX();
        double y = ball.getPosition().getY();
        double r = ball.getRadius();

        double vx = ball.getVelocity().getX();
        double vy = ball.getVelocity().getY();

        boolean collided = false;
        // Koefisien restitusi dinding (0.9 = bola kehilangan 10% energi saat memantul)
        double wallRestitution = 0.9;

        // Cek Dinding Kiri
        if (x - r < 0) {
            x = r; // Positional Correction: Paksa bola kembali ke batas meja
            vx = -vx * wallRestitution; // Balikkan arah X
            collided = true;
        }
        // Cek Dinding Kanan
        else if (x + r > table.getWidth()) {
            x = table.getWidth() - r;
            vx = -vx * wallRestitution;
            collided = true;
        }

        // Cek Dinding Atas
        if (y - r < 0) {
            y = r;
            vy = -vy * wallRestitution;
            collided = true;
        }
        // Cek Dinding Bawah
        else if (y + r > table.getHeight()) {
            y = table.getHeight() - r;
            vy = -vy * wallRestitution;
            collided = true;
        }

        // Terapkan perubahan jika terjadi tumbukan
        if (collided) {
            ball.setPosition(new Vector2D(x, y));
            ball.setVelocity(new Vector2D(vx, vy));
        }
    }

    /**
     * Menyelesaikan tumbukan antara dua bola menggunakan fisika tumbukan lenting (Elastic Collision).
     *
     * @param b1 Bola pertama.
     * @param b2 Bola kedua.
     */
    private void resolveBallCollision(Ball b1, Ball b2) {
        Vector2D posDiff = b1.getPosition().subtract(b2.getPosition());
        double dist = posDiff.length();
        double minDist = b1.getRadius() + b2.getRadius();

        // Cek apakah bola saling bersentuhan (Jarak < Jumlah Jari-jari)
        if (dist == 0 || dist >= minDist) return;

        // --- STEP A: Static Resolution (Pemisahan Posisi) ---
        // Mencegah bola saling menempel (sticking) atau tenggelam satu sama lain.
        // Kita geser kedua bola menjauh agar tidak lagi overlap.
        double overlap = 0.5 * (dist - minDist);
        Vector2D displacement = posDiff.normalize().multiply(overlap);

        b1.setPosition(b1.getPosition().subtract(displacement));
        b2.setPosition(b2.getPosition().add(displacement));

        // --- STEP B: Dynamic Resolution (Respon Kecepatan) ---
        // Menghitung vektor normal tumbukan
        Vector2D normalVector = posDiff.normalize();

        // Menghitung kecepatan relatif
        Vector2D relativeVel = b1.getVelocity().subtract(b2.getVelocity());

        // Kecepatan sepanjang normal (seberapa cepat mereka mendekat)
        double speed = relativeVel.dot(normalVector);

        // Jika bola sudah bergerak menjauh, jangan pantulkan lagi
        if (speed >= 0) return;

        // Rumus Impulse (Perubahan Momentum)
        // Impulse = -(1 + restitution) * relativeVelocity / (1/mass1 + 1/mass2)
        // Karena massa sama (1.0), rumusnya disederhanakan.
        double impulse = 2 * speed / (b1.getMass() + b2.getMass());

        // Restitusi tumbukan bola (sedikit hilang energi)
        double restitution = 0.9;

        // Terapkan impulse ke kecepatan masing-masing bola
        b1.setVelocity(
                b1.getVelocity().subtract(normalVector.multiply(impulse * b2.getMass()))
                        .multiply(restitution)
        );

        b2.setVelocity(
                b2.getVelocity().add(normalVector.multiply(impulse * b1.getMass()))
                        .multiply(restitution)
        );
    }
}