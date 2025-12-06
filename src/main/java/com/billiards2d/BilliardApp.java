package com.billiards2d;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Kelas utama aplikasi (Main Entry Point) yang mengatur siklus hidup permainan.
 * <p>
 * Kelas ini bertanggung jawab untuk:
 * 1. Menginisialisasi jendela aplikasi (GUI) menggunakan JavaFX.
 * 2. Menangani input pengguna (Mouse Events) dan meneruskannya ke objek game.
 * 3. Menjalankan Game Loop utama untuk memperbarui logika dan merender grafik setiap frame.
 * </p>
 */
public class BilliardApp extends Application {

    // Ukuran area permainan (Area hijau meja, tidak termasuk dinding)
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 450;

    private GraphicsContext gc;
    // Daftar semua objek game yang perlu di-update setiap frame
    private final List<GameObject> gameObjects = new ArrayList<>();

    private Table table;       // Referensi ke objek Meja
    private CueStick cueStick; // Referensi ke Stik untuk input handling
    private CueBall cueBall;   // Referensi ke Bola Putih untuk HUD info

    // Variabel debug untuk menampilkan info di HUD (Heads-Up Display)
    private double mouseX, mouseY;

    /**
     * Metode utama JavaFX yang dipanggil saat aplikasi dimulai.
     * Mengatur Stage, Scene, Canvas, dan Input Listeners.
     */
    @Override
    public void start(Stage primaryStage) {
        // 1. Init Table untuk menghitung ukuran total window (Area Main + Dinding)
        table = new Table(GAME_WIDTH, GAME_HEIGHT);
        double totalW = GAME_WIDTH + (table.getWallThickness() * 2);
        double totalH = GAME_HEIGHT + (table.getWallThickness() * 2);

        // 2. Setup Canvas sebagai area menggambar
        Canvas canvas = new Canvas(totalW, totalH);
        gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);
        root.setStyle("-fx-background-color: #222;"); // Background gelap di luar meja

        // 3. Input Handling (Mouse)
        // Kita perlu menggeser (offset) koordinat mouse karena area bermain dimulai
        // setelah ketebalan dinding meja.
        double offset = table.getWallThickness();

        canvas.setOnMouseMoved(e -> {
            mouseX = e.getX() - offset; // Koordinat relatif terhadap area main
            mouseY = e.getY() - offset;
            cueStick.handleMouseMoved(offsetEvent(e, -offset));
        });

        canvas.setOnMousePressed(e -> cueStick.handleMousePressed(offsetEvent(e, -offset)));

        canvas.setOnMouseDragged(e -> {
            mouseX = e.getX() - offset;
            mouseY = e.getY() - offset;
            cueStick.handleMouseDragged(offsetEvent(e, -offset));
        });

        canvas.setOnMouseReleased(e -> cueStick.handleMouseReleased(offsetEvent(e, -offset)));

        // 4. Finalisasi Stage
        primaryStage.setTitle("Billiard Simulation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Mencegah resize agar rasio terjaga
        primaryStage.show();

        // 5. Init Objek Game dan Mulai Loop
        initializeGameObjects();
        GameLoop gameLoop = new GameLoop();
        gameLoop.start();
    }

    /**
     * Helper method untuk membuat event mouse baru dengan koordinat yang sudah digeser.
     * Ini penting agar posisi mouse di logika game sinkron dengan visual meja.
     */
    private MouseEvent offsetEvent(MouseEvent e, double shift) {
        return new MouseEvent(
                e.getSource(), e.getTarget(), e.getEventType(),
                e.getX() + shift, e.getY() + shift, // Koordinat lokal digeser
                e.getScreenX(), e.getScreenY(),
                e.getButton(), e.getClickCount(),
                e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown(),
                e.isPrimaryButtonDown(), e.isMiddleButtonDown(), e.isSecondaryButtonDown(),
                e.isSynthesized(), e.isPopupTrigger(), e.isStillSincePress(), e.getPickResult()
        );
    }

    /**
     * Membuat dan mendaftarkan semua objek permainan awal (Bola, Stik, Engine).
     */
    private void initializeGameObjects() {
        // Inisialisasi Bola Putih
        cueBall = new CueBall(new Vector2D(GAME_WIDTH/4.0, GAME_HEIGHT/2.0));

        // List sementara untuk menampung semua bola (Putih + Warna)
        List<Ball> allBalls = new ArrayList<>();
        allBalls.add(cueBall);

        // Inisialisasi Bola Warna (15 bola dalam formasi segitiga)
        gameObjects.add(cueBall);
        setupRack(allBalls);

        // Inisialisasi Stik (Butuh referensi ke bola putih dan semua bola untuk prediksi)
        this.cueStick = new CueStick(cueBall, allBalls, GAME_WIDTH, GAME_HEIGHT);

        // Inisialisasi Physics Engine (Logika Fisika)
        // PhysicsEngine juga dimasukkan ke gameObjects agar method update()-nya dipanggil di loop
        PhysicsEngine physicsEngine = new PhysicsEngine(table, gameObjects);
        // Note: physicsEngine butuh akses ke list gameObjects untuk mendeteksi semua bola
        gameObjects.add(physicsEngine);
    }

    // Method helper untuk menyusun 15 bola dalam formasi segitiga
    private void setupRack(List<Ball> ballList) {
        double radius = 10.0;
        // Posisi puncak segitiga (Foot Spot), kira-kira di 75% lebar meja
        double startX = GAME_WIDTH * 0.75;
        double startY = GAME_HEIGHT / 2.0;

        // Array warna untuk variasi bola (Siklus warna standar biliar)
        String[] colors = {
                "YELLOW", "BLUE", "RED", "PURPLE", "ORANGE", "GREEN", "MAROON", "BLACK",
                "YELLOW", "BLUE", "RED", "PURPLE", "ORANGE", "GREEN", "MAROON"
        };

        int ballCount = 0;

        // Loop untuk 5 kolom (baris vertikal segitiga)
        // Kolom 0 = 1 bola (ujung), Kolom 4 = 5 bola (belakang)
        for (int col = 0; col < 5; col++) {
            for (int row = 0; row <= col; row++) {
                // Hitung Posisi X:
                // Setiap kolom mundur sejauh (radius * akar 3) agar rapat
                double x = startX + (col * (radius * Math.sqrt(3)));

                // Hitung Posisi Y:
                // Kita perlu menengahkan barisan bola secara vertikal terhadap startY
                // Tinggi total kolom ini = (jumlah bola * diameter)
                // Posisi awal (atas) = Tengah - (Setengah Tinggi)
                double rowHeight = col * (radius * 2);
                double yTop = startY - (rowHeight / 2.0);

                // Posisi Y bola saat ini
                double y = yTop + (row * (radius * 2));

                // Tentukan warna (Bola ke-5 di tengah biasanya Hitam/8-Ball)
                String colorName = colors[ballCount % colors.length];
                if (col == 2 && row == 1) colorName = "BLACK";

                // Buat Bola
                ObjectBall ball = new ObjectBall(new Vector2D(x, y), colorName);

                // Tambahkan ke list (PENTING: Tambah ke kedua list)
                ballList.add(ball);     // Untuk Physics & CueStick
                gameObjects.add(ball);  // Untuk Rendering & Update Loop

                ballCount++;
            }
        }
    }

    /**
     * Inner Class yang menangani Game Loop utama menggunakan AnimationTimer.
     * Berjalan sekitar 60 kali per detik (tergantung refresh rate monitor).
     */
    private class GameLoop extends AnimationTimer {
        private long lastNanoTime = System.nanoTime();

        @Override
        public void handle(long currentNanoTime) {
            // 1. Hitung Delta Time (waktu dalam detik sejak frame terakhir)
            double deltaTime = (currentNanoTime - lastNanoTime) / 1_000_000_000.0;
            lastNanoTime = currentNanoTime;

            // Safety Cap: Jika lag parah (dt > 0.05s), batasi dt agar fisika tidak "meledak" (tunneling)
            if (deltaTime > 0.05) deltaTime = 0.05;

            // --- UPDATE LOGIC (PHYSICS) ---
            // Physics Sub-stepping: Memecah satu update besar menjadi beberapa langkah kecil
            // untuk meningkatkan akurasi deteksi tabrakan dan mencegah bola tembus dinding.
            int subSteps = 4;
            double subDeltaTime = deltaTime / subSteps;

            for (int step = 0; step < subSteps; step++) {
                for (GameObject obj : gameObjects) {
                    obj.update(subDeltaTime); // Update posisi & fisika
                }
            }

            // Update stik (visual/input) cukup sekali per frame, tidak perlu sub-stepping
            cueStick.update(deltaTime);

            // --- RENDER LOGIC (DRAWING) ---
            // Bersihkan layar sebelum menggambar frame baru
            gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            // 1. Gambar Meja (Background Layer)
            table.draw(gc);

            // 2. Gambar Objek Game (Middle Layer)
            // Geser koordinat (translate) agar (0,0) game berada di dalam dinding meja
            gc.save();
            gc.translate(table.getWallThickness(), table.getWallThickness());

            for (GameObject obj : gameObjects) {
                // Hanya gambar bola di sini (PhysicsEngine tidak punya visual)
                if(obj instanceof Ball) obj.draw(gc);
            }

            // Gambar Stik paling atas agar tidak tertutup bola
            cueStick.draw(gc);

            gc.restore(); // Kembalikan koordinat normal (termasuk dinding)

            // --- LOGIKA RESPAWN CUE BALL ---
            if (cueBall.isPendingRespawn()) {
                // Cek apakah semua bola LAIN (selain cueball) sudah berhenti
                boolean allStopped = true;
                for (GameObject obj : gameObjects) {
                    if (obj instanceof Ball && obj != cueBall) {
                        Ball b = (Ball) obj;
                        // Cek jika bola aktif dan masih bergerak
                        if (b.isActive() && b.getVelocity().length() > 0.1) {
                            allStopped = false;
                            break;
                        }
                    }
                }

                // Jika semua sudah berhenti, baru munculkan bola putih
                if (allStopped) {
                    cueBall.setPosition(new Vector2D(GAME_WIDTH / 4.0, GAME_HEIGHT / 2.0)); // Posisi Reset
                    cueBall.setVelocity(new Vector2D(0, 0));
                    cueBall.setPendingRespawn(false);
                    cueBall.setActive(true); // Aktifkan kembali fisikanya
                }
            }

            // 3. Gambar HUD (Overlay Layer) - Info Debug
            drawHUD();
        }

        private void drawHUD() {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", 14));
            // Menampilkan koordinat mouse relatif terhadap area main
            gc.fillText(String.format("Mouse: (%.0f, %.0f)", mouseX, mouseY), 20, 30);
            // Menampilkan kecepatan bola putih
            double speed = cueBall.getVelocity().length();
            gc.fillText(String.format("Power: %.2f", speed), 20, 50);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}