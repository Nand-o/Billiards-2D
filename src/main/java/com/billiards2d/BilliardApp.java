package com.billiards2d;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class BilliardApp extends Application {

    // --- KONFIGURASI UKURAN GAME (AREA HIJAU) ---
    // Ini adalah ukuran "Dunia Fisika" kita.
    private static final double GAME_WIDTH = 880;
    private static final double GAME_HEIGHT = 440;

    private GraphicsContext gc;
    private Canvas canvas;
    private final List<GameObject> gameObjects = new ArrayList<>();

    private Table table;
    private CueStick cueStick;
    private CueBall cueBall;
    private PhysicsEngine physicsEngine;

    // Variabel Offset Dinamis (Agar game selalu di tengah layar)
    private double currentOffsetX = 0;
    private double currentOffsetY = 0;

    // Debug Info
    private double mouseLogicX, mouseLogicY;

    @Override
    public void start(Stage primaryStage) {
        // 1. Init Logic Game
        table = new Table(GAME_WIDTH, GAME_HEIGHT);

        // 2. Setup Canvas yang Responsif
        // Kita buat Canvas seukuran window awal (misal 1280x720)
        canvas = new Canvas(1280, 720);
        gc = canvas.getGraphicsContext2D();

        // Gunakan Pane biasa agar kita bisa kontrol posisi absolute canvas jika perlu,
        // tapi di sini kita biarkan canvas memenuhi Pane.
        Pane root = new Pane(canvas);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Scene scene = new Scene(root);

        // Binding: Canvas selalu mengikuti ukuran Scene (Window)
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        // 3. Input Handling (Mouse)
        // Kita hitung offset secara real-time saat event terjadi

        canvas.setOnMouseMoved(e -> {
            updateOffsets(); // Pastikan offset terbaru
            mouseLogicX = e.getX() - currentOffsetX;
            mouseLogicY = e.getY() - currentOffsetY;
            // Kirim event dengan koordinat yang sudah dikoreksi ke Logic Game
            cueStick.handleMouseMoved(offsetEvent(e, -currentOffsetX, -currentOffsetY));
        });

        canvas.setOnMousePressed(e -> {
            updateOffsets();
            cueStick.handleMousePressed(offsetEvent(e, -currentOffsetX, -currentOffsetY));
        });

        canvas.setOnMouseDragged(e -> {
            updateOffsets();
            mouseLogicX = e.getX() - currentOffsetX;
            mouseLogicY = e.getY() - currentOffsetY;
            cueStick.handleMouseDragged(offsetEvent(e, -currentOffsetX, -currentOffsetY));
        });

        canvas.setOnMouseReleased(e -> {
            updateOffsets();
            cueStick.handleMouseReleased(offsetEvent(e, -currentOffsetX, -currentOffsetY));
        });

        // 4. Finalisasi Stage
        primaryStage.setTitle("Billiard Simulation - Asset Integration");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(720);
        primaryStage.show();

        // 5. Init Objek Game dan Mulai Loop
        initializeGameObjects();
        GameLoop gameLoop = new GameLoop();
        gameLoop.start();
    }

    /**
     * Menghitung posisi tengah layar berdasarkan ukuran window saat ini.
     * Ini memastikan koordinat (0,0) game selalu ada di tengah visual.
     */
    private void updateOffsets() {
        double screenW = canvas.getWidth();
        double screenH = canvas.getHeight();

        // Rumus Center: (LebarLayar - LebarGame) / 2
        currentOffsetX = (screenW - GAME_WIDTH) / 2;
        currentOffsetY = (screenH - GAME_HEIGHT) / 2;
    }

    // Method Helper untuk offset mouse (3 Parameter - FIXED)
    private MouseEvent offsetEvent(MouseEvent e, double shiftX, double shiftY) {
        return new MouseEvent(
                e.getSource(), e.getTarget(), e.getEventType(),
                e.getX() + shiftX, e.getY() + shiftY,
                e.getScreenX(), e.getScreenY(),
                e.getButton(), e.getClickCount(),
                e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown(),
                e.isPrimaryButtonDown(), e.isMiddleButtonDown(), e.isSecondaryButtonDown(),
                e.isSynthesized(), e.isPopupTrigger(), e.isStillSincePress(), e.getPickResult()
        );
    }

    private void initializeGameObjects() {
        // Posisi bola relatif terhadap GAME_WIDTH (Logic Coordinates)
        cueBall = new CueBall(new Vector2D(GAME_WIDTH/4.0, GAME_HEIGHT/2.0));

        List<Ball> allBalls = new ArrayList<>();
        allBalls.add(cueBall);

        gameObjects.add(cueBall);
        setupRack(allBalls);

        this.cueStick = new CueStick(cueBall, allBalls, GAME_WIDTH, GAME_HEIGHT);
        this.physicsEngine = new PhysicsEngine(table, gameObjects);

        gameObjects.addAll(allBalls);
        gameObjects.add(physicsEngine);
    }

    private void setupRack(List<Ball> ballList) {
        double radius = 10.0;
        double startX = GAME_WIDTH * 0.75;
        double startY = GAME_HEIGHT / 2.0;

        String[] colors = {
                "YELLOW", "BLUE", "RED", "PURPLE", "ORANGE", "GREEN", "MAROON", "BLACK",
                "YELLOW", "BLUE", "RED", "PURPLE", "ORANGE", "GREEN", "MAROON"
        };

        int ballCount = 0;
        for (int col = 0; col < 5; col++) {
            for (int row = 0; row <= col; row++) {
                double x = startX + (col * (radius * Math.sqrt(3)));
                double rowHeight = col * (radius * 2);
                double yTop = startY - (rowHeight / 2.0);
                double y = yTop + (row * (radius * 2));

                String colorName = colors[ballCount % colors.length];
                if (col == 2 && row == 1) colorName = "BLACK";

                ObjectBall ball = new ObjectBall(new Vector2D(x, y), colorName);
                ballList.add(ball);
                gameObjects.add(ball);
                ballCount++;
            }
        }
    }

    private class GameLoop extends AnimationTimer {
        private long lastNanoTime = System.nanoTime();

        @Override
        public void handle(long currentNanoTime) {
            double deltaTime = (currentNanoTime - lastNanoTime) / 1_000_000_000.0;
            lastNanoTime = currentNanoTime;
            if (deltaTime > 0.05) deltaTime = 0.05;

            // --- 0. Hitung Offset Terbaru ---
            // Kita hitung setiap frame untuk mengantisipasi resize window
            updateOffsets();

            // --- 1. Update Logic ---
            int subSteps = 4;
            double subDeltaTime = deltaTime / subSteps;
            for (int step = 0; step < subSteps; step++) {
                for (GameObject obj : gameObjects) {
                    obj.update(subDeltaTime);
                }
            }
            cueStick.update(deltaTime);

            // --- 2. Render Logic ---
            gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            // PENTING: Geser titik (0,0) GraphicsContext ke tengah layar
            gc.save();
            gc.translate(currentOffsetX, currentOffsetY);

            // Debug Grid (Opsional: Cek apakah (0,0) ada di pojok kiri atas area merah)
            // gc.setStroke(Color.GRAY); gc.strokeRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

            // Gambar semua objek relatif terhadap (0,0) Area Hijau
            table.draw(gc);

            for (GameObject obj : gameObjects) {
                if(obj instanceof Ball) obj.draw(gc);
            }
            cueStick.draw(gc);

            gc.restore(); // Kembalikan ke koordinat normal untuk gambar HUD

            // Logic Respawn
            if (cueBall.isPendingRespawn()) {
                boolean allStopped = true;
                for (GameObject obj : gameObjects) {
                    if (obj instanceof Ball && obj != cueBall) {
                        Ball b = (Ball) obj;
                        if (b.isActive() && b.getVelocity().length() > 0.1) {
                            allStopped = false;
                            break;
                        }
                    }
                }
                if (allStopped) {
                    cueBall.setPosition(new Vector2D(GAME_WIDTH / 4.0, GAME_HEIGHT / 2.0));
                    cueBall.setVelocity(new Vector2D(0, 0));
                    cueBall.setPendingRespawn(false);
                    cueBall.setActive(true);
                }
            }

            drawHUD();
        }

        private void drawHUD() {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", 14));
            // Tampilkan info koordinat
            gc.fillText(String.format("Mouse Logic: (%.0f, %.0f)", mouseLogicX, mouseLogicY), 20, 30);
            gc.fillText(String.format("Window Offset: (%.0f, %.0f)", currentOffsetX, currentOffsetY), 20, 50);

            double speed = cueBall.getVelocity().length();
            gc.fillText(String.format("Power: %.2f", speed), 20, 80);

            gc.setFont(Font.font("Consolas", 20));
            gc.setFill(Color.YELLOW);
            if (physicsEngine != null) {
                gc.fillText("SCORE: " + physicsEngine.getPlayerScore(), 20, 110);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}