package com.billiards2d;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.util.ArrayList;
import java.util.List;

public class BilliardApp extends Application {

    // --- KONFIGURASI ---
    private static final double GAME_WIDTH = 880;
    private static final double GAME_HEIGHT = 440;

    // SWITCH MODE:
    // true  = Menggunakan Aturan 8-Ball (Giliran, Solid/Stripes, Win/Loss)
    // false = Menggunakan Mode Arcade (Skor Bebas)
    private boolean is8BallMode = true;

    // --- CORE SYSTEMS ---
    private GraphicsContext gc;
    private Canvas canvas;
    private final List<GameObject> gameObjects = new ArrayList<>();

    private Table table;
    private CueStick cueStick;
    private CueBall cueBall;
    private PhysicsEngine physicsEngine;
    private GameRules gameRules; // Wasit 8-Ball

    // --- STATE VARIABLES ---
    private double currentOffsetX = 0;
    private double currentOffsetY = 0;

    // Flag untuk menandai apakah sebuah giliran (tembakan) sedang berlangsung
    // Digunakan agar logika Rules hanya dipanggil SATU KALI setelah bola berhenti.
    private boolean turnInProgress = false;

    // Debug Info Mouse
    private double mouseLogicX, mouseLogicY;

    // ASSETS UI - UPDATE KOORDINAT PRESISI
    private static Image uiSpriteSheet;

    // 1. KUBUS (Chalk Box)
    // Geser X +1 dan Kurangi Lebar -2 agar tidak bocor piksel tetangga
    private static final double CUBE_SRC_X = 145;
    private static final double CUBE_SRC_Y = 0;
    private static final double CUBE_W = 22;  // Lebar dikecilkan dikit (Safety crop)
    private static final double CUBE_H = 22;

    // 2. PIL (Indikator Vertikal - Merah)
    // Ambil dari Y=0 agar utuh
    private static final double PILL_SRC_X = 169; // Geser +1 biar aman
    private static final double PILL_SRC_Y = 0;   // Mulai dari paling atas
    private static final double PILL_W = 6;       // Ambil tengahnya saja
    private static final double PILL_H = 16;      // Tinggi standar

    @Override
    public void start(Stage primaryStage) {
        // 1. Init Logic Systems
        table = new Table(GAME_WIDTH, GAME_HEIGHT);
        gameRules = new GameRules(); // Inisialisasi Wasit

        // LOAD UI SPRITE
        try {
            uiSpriteSheet = new Image(getClass().getResourceAsStream("/assets/SMS_GUI_Display_NO_BG.png"));
        } catch (Exception e) {
            System.err.println("Gagal load UI: " + e.getMessage());
        }

        // 2. Setup Canvas
        canvas = new Canvas(1280, 720);
        gc = canvas.getGraphicsContext2D();

        Pane root = new Pane(canvas);
        root.setStyle("-fx-background-color: #1a1a1a;");

        Scene scene = new Scene(root);
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        // --- INPUT HANDLING PINTAR (Ball in Hand vs Shooting) ---

        canvas.setOnMouseMoved(e -> {
            updateOffsets();
            double logicX = e.getX() - currentOffsetX;
            double logicY = e.getY() - currentOffsetY;
            mouseLogicX = logicX; mouseLogicY = logicY;

            if (isBallInHandActive()) {
                // MODE DRAG: Bola putih mengikuti mouse
                moveCueBallToMouse(logicX, logicY);
            } else {
                // MODE NORMAL: Gerakkan Stik
                cueStick.handleMouseMoved(logicX, logicY);
            }
        });

        canvas.setOnMousePressed(e -> {
            updateOffsets();
            double logicX = e.getX() - currentOffsetX;
            double logicY = e.getY() - currentOffsetY;

            if (isBallInHandActive()) {
                // Klik saat Ball in Hand = Mencoba Menaruh Bola
                tryPlaceCueBall(logicX, logicY);
            } else {
                cueStick.handleMousePressed(logicX, logicY);
            }
        });

        canvas.setOnMouseDragged(e -> {
            updateOffsets();
            double logicX = e.getX() - currentOffsetX;
            double logicY = e.getY() - currentOffsetY;
            mouseLogicX = logicX; mouseLogicY = logicY;

            if (isBallInHandActive()) {
                moveCueBallToMouse(logicX, logicY);
            } else {
                cueStick.handleMouseDragged(logicX, logicY);
            }
        });

        canvas.setOnMouseReleased(e -> {
            updateOffsets();
            double logicX = e.getX() - currentOffsetX;
            double logicY = e.getY() - currentOffsetY;

            if (!isBallInHandActive()) {
                cueStick.handleMouseReleased(logicX, logicY);
            }
        });

        // 4. Finalisasi Stage
        primaryStage.setTitle("Billiard Project - " + (is8BallMode ? "8-Ball Mode" : "Arcade Mode"));
        primaryStage.setScene(scene);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(720);
        primaryStage.show();

        // 5. Init Objects & Loop
        initializeGameObjects();
        GameLoop gameLoop = new GameLoop();
        gameLoop.start();
    }

    // --- HELPER METHODS UNTUK BALL IN HAND ---

    private boolean isBallInHandActive() {
        return is8BallMode && gameRules.isBallInHand();
    }

    private void moveCueBallToMouse(double x, double y) {
        // Clamp agar tidak keluar meja
        double r = cueBall.getRadius();
        if (x < r) x = r;
        if (x > GAME_WIDTH - r) x = GAME_WIDTH - r;
        if (y < r) y = r;
        if (y > GAME_HEIGHT - r) y = GAME_HEIGHT - r;

        cueBall.setPosition(new Vector2D(x, y));
        cueBall.setVelocity(new Vector2D(0, 0)); // Pastikan diam
    }

    private void tryPlaceCueBall(double x, double y) {
        // Cek apakah posisi valid (tidak menumpuk bola lain)
        boolean overlap = false;
        for (GameObject obj : gameObjects) {
            if (obj instanceof Ball && obj != cueBall) {
                Ball other = (Ball) obj;
                if (!other.isActive()) continue; // Abaikan bola yang sudah masuk

                double dist = cueBall.getPosition().subtract(other.getPosition()).length();
                if (dist < (cueBall.getRadius() + other.getRadius() + 2)) { // +2 buffer
                    overlap = true;
                    break;
                }
            }
        }

        if (!overlap) {
            // Posisi Valid -> Taruh Bola, Matikan Mode Ball in Hand
            gameRules.clearBallInHand();
        } else {
            // Feedback Suara atau Visual (Optional)
            System.out.println("Cannot place here! Overlap.");
        }
    }

    private void updateOffsets() {
        double screenW = canvas.getWidth();
        double screenH = canvas.getHeight();
        currentOffsetX = (screenW - GAME_WIDTH) / 2;
        currentOffsetY = (screenH - GAME_HEIGHT) / 2;
    }

    private void initializeGameObjects() {
        // Posisi bola putih (Head Spot area)
        cueBall = new CueBall(new Vector2D(GAME_WIDTH / 4.0, GAME_HEIGHT / 2.0));

        List<Ball> allBalls = new ArrayList<>();
        allBalls.add(cueBall);

        gameObjects.add(cueBall);
        setupRack(allBalls); // Setup 15 bola

        this.cueStick = new CueStick(cueBall, allBalls, GAME_WIDTH, GAME_HEIGHT, gameRules);
        this.physicsEngine = new PhysicsEngine(table, gameObjects);

        gameObjects.addAll(allBalls);
        gameObjects.add(physicsEngine);
    }

    // Method Setup Rack yang SUDAH DIPERBAIKI (No Duplicate, 8 di Tengah)
    private void setupRack(List<Ball> ballList) {
        double radius = 13.0;
        double startX = GAME_WIDTH * 0.75;
        double startY = GAME_HEIGHT / 2.0;

        // Siapkan nomor 1-15 kecuali 8
        List<Integer> availableNumbers = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            if (i != 8) availableNumbers.add(i);
        }
        // java.util.Collections.shuffle(availableNumbers); // Uncomment jika ingin acak

        int indexCounter = 0;

        for (int col = 0; col < 5; col++) {
            for (int row = 0; row <= col; row++) {
                double x = startX + (col * (radius * Math.sqrt(3) + 2));
                double rowHeight = col * (radius * 2 + 2);
                double yTop = startY - (rowHeight / 2.0);
                double y = yTop + (row * (radius * 2 + 2));

                int ballNumber;
                // Paksa Bola 8 di tengah (Col 2, Row 1)
                if (col == 2 && row == 1) {
                    ballNumber = 8;
                } else {
                    ballNumber = availableNumbers.get(indexCounter);
                    indexCounter++;
                }

                ObjectBall ball = new ObjectBall(new Vector2D(x, y), ballNumber);
                ballList.add(ball);
                gameObjects.add(ball);
            }
        }
    }

    // --- GAME LOOP UTAMA ---
    private class GameLoop extends AnimationTimer {
        private long lastNanoTime = System.nanoTime();

        @Override
        public void handle(long currentNanoTime) {
            double deltaTime = (currentNanoTime - lastNanoTime) / 1_000_000_000.0;
            lastNanoTime = currentNanoTime;
            if (deltaTime > 0.05) deltaTime = 0.05;

            updateOffsets();

            // 1. UPDATE FISIKA
            // Hanya update fisika jika TIDAK sedang menaruh bola (biar ga gerak2 sendiri)
            if (!isBallInHandActive()) {
                int subSteps = 4;
                double subDeltaTime = deltaTime / subSteps;
                for (int step = 0; step < subSteps; step++) {
                    for (GameObject obj : gameObjects) {
                        obj.update(subDeltaTime);
                    }
                }
            }

            cueStick.update(deltaTime); // Stik update (animasi idle dll)

            // 2. LOGIC TURN
            checkGameRules();

            // 3. RENDER
            renderGame();

            // Note: handleCueBallRespawn dihapus/dimodifikasi
            // karena logic respawn sekarang ditangani oleh Ball-in-Hand rules
        }

        private void checkGameRules() {
            boolean anyBallMoving = !cueStick.areAllBallsStopped();

            if (anyBallMoving) {
                turnInProgress = true;
            }
            else if (turnInProgress) {
                processTurnEnd();
                turnInProgress = false;
            }
        }

        private void processTurnEnd() {
            if (is8BallMode) {
                List<ObjectBall> potted = physicsEngine.getPocketedBalls();
                boolean foul = physicsEngine.isCueBallPocketed();
                Ball firstHit = physicsEngine.getFirstHitBall();

                List<Ball> remaining = new ArrayList<>();
                for (GameObject obj : gameObjects) {
                    if (obj instanceof Ball && ((Ball) obj).isActive()) {
                        remaining.add((Ball) obj);
                    }
                }

                if (!gameRules.isGameOver()) {
                    gameRules.processTurn(potted, foul, remaining, firstHit);

                    // CEK KHUSUS: Apakah sekarang Ball In Hand?
                    if (gameRules.isBallInHand()) {
                        // Jika Cue Ball masuk (Scratch), dia mati (active=false).
                        // Kita harus HIDUPKAN LAGI agar bisa didrag.
                        if (!cueBall.isActive()) {
                            cueBall.setActive(true);
                            cueBall.setPendingRespawn(false);
                            // Set posisi default sementara sebelum didrag user
                            cueBall.setPosition(new Vector2D(GAME_WIDTH/4, GAME_HEIGHT/2));
                            cueBall.setVelocity(new Vector2D(0,0));
                        }
                    }
                }

                physicsEngine.resetTurnReport();
            } else {
                physicsEngine.resetTurnReport();
            }
        }

        private void renderGame() {
            gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
            gc.save();
            gc.translate(currentOffsetX, currentOffsetY);

            table.draw(gc);

            // Gambar Bola (Termasuk CueBall saat didrag)
            for (GameObject obj : gameObjects) {
                if (obj instanceof Ball) obj.draw(gc);
            }

            // Stik HANYA digambar jika TIDAK sedang Ball In Hand
            if (!isBallInHandActive()) {
                cueStick.draw(gc);
            } else {
                // Visual Bantuan saat Dragging
                // Lingkaran di bawah bola putih untuk indikasi "Placing Mode"
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.setLineDashes(5);
                gc.strokeOval(cueBall.getPosition().getX() - 20, cueBall.getPosition().getY() - 20, 40, 40);
                gc.setLineDashes(null);
            }

            gc.restore();
            drawHUD();
        }

        private void drawHUD() {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
            // Debug Mouse
            // gc.fillText(String.format("Mouse: (%.0f, %.0f)", mouseLogicX, mouseLogicY), 20, 30);

            // --- HUD 8-BALL MODE ---
            if (is8BallMode) {
                gc.setFont(Font.font("Consolas", FontWeight.BOLD, 22));

                if (gameRules.isGameOver()) {
                    // --- TAMPILAN AKHIR PERMAINAN ---

                    if (gameRules.isCleanWin()) {
                        // MENANG BERSIH (HIJAU)
                        gc.setFill(Color.LIMEGREEN);
                        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 30));
                        gc.fillText("🏆 WINNER! 🏆", 20, 50);

                        gc.setFill(Color.WHITE);
                        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 22));
                        // Tampilkan pesan "VICTORY! PLAYER X WINS!"
                        gc.fillText(gameRules.getStatusMessage(), 20, 90);

                    } else {
                        // GAME OVER KARENA FOUL (MERAH)
                        gc.setFill(Color.RED);
                        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 30));
                        gc.fillText("☠ GAME OVER ☠", 20, 50);

                        gc.setFill(Color.WHITE);
                        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
                        // Tampilkan pesan "GAME OVER! PLAYER X LOST (Early 8-Ball)"
                        gc.fillText(gameRules.getStatusMessage(), 20, 90);
                    }

                    // Pesan Restart
                    gc.setFont(Font.font("Consolas", 16));
                    gc.setFill(Color.YELLOW);
                    gc.fillText("Press RESTART button to play again.", 20, 120);

                } else {
                    // ... (Tampilan In-Game / Turn Player Tetap Sama) ...
                    // Copy paste bagian 'else' dari kode sebelumnya di sini
                    String player = (gameRules.getCurrentTurn() == GameRules.PlayerTurn.PLAYER_1) ? "PLAYER 1" : "PLAYER 2";
                    gc.setFill(Color.YELLOW);
                    gc.fillText("TURN: " + player, 20, 50);

                    String targetText = "OPEN TABLE";
                    Color targetColor = Color.WHITE;

                    GameRules.TableState state = gameRules.getTableState();
                    if (state != GameRules.TableState.OPEN) {
                        boolean isP1 = (gameRules.getCurrentTurn() == GameRules.PlayerTurn.PLAYER_1);
                        boolean isSolid = (state == GameRules.TableState.P1_SOLID && isP1) ||
                                (state == GameRules.TableState.P1_STRIPES && !isP1);

                        targetText = isSolid ? "SOLIDS (1-7)" : "STRIPES (9-15)";
                        targetColor = isSolid ? Color.ORANGE : Color.CYAN;

                        // TAMBAHAN VISUAL: Jika sudah masuk fase 8-Ball
                        // Kita bisa cek manual atau tambah method di GameRules,
                        // tapi sementara pakai warna target saja sudah cukup jelas.
                    }

                    gc.setFill(targetColor);
                    gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
                    gc.fillText("GOAL: " + targetText, 20, 75);

                    gc.setFill(Color.LIGHTGREEN);
                    gc.setFont(Font.font("Consolas", 16));
                    gc.fillText("MSG: " + gameRules.getStatusMessage(), 20, 100);

                    if (gameRules.isBallInHand()) {
                        gc.setFill(Color.MAGENTA);
                        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 26));
                        gc.fillText("BALL IN HAND", 20, 140);
                        gc.setFont(Font.font("Consolas", 16));
                        gc.setFill(Color.WHITE);
                        gc.fillText("(Click to place cue ball)", 20, 165);
                    }
                }
            } else {
                // ARCADE MODE
                if (physicsEngine != null) {
                    gc.setFill(Color.YELLOW);
                    gc.fillText("SCORE: " + physicsEngine.getArcadeScore(), 20, 50);
                }
            }
            // Gambar Power Bar
            drawPowerBar(gc);
        }
    }

    private void drawPowerBar(GraphicsContext gc) {
        double ratio = cueStick.getPowerRatio();

        // Posisi Awal UI
        double startX = 30;
        double startY = canvas.getHeight() - 30; // Naikkan sedikit biar ga mepet bawah

        if (uiSpriteSheet != null) {
            // A. GAMBAR KUBUS (CHALK BOX)
            // Scale 1.5x
            double destCubeW = CUBE_W * 1.5;
            double destCubeH = CUBE_H * 1.5;

            gc.drawImage(uiSpriteSheet,
                    CUBE_SRC_X, CUBE_SRC_Y, CUBE_W, CUBE_H,
                    startX, startY - (destCubeH / 2), destCubeW, destCubeH);

            // B. GAMBAR PIL (DEKORASI)
            double pillX = startX + destCubeW + 8; // Jarak 8px dari kubus
            double destPillW = PILL_W * 1.5;
            double destPillH = PILL_H * 1.5;

            // Gambar Pil agar center secara vertikal dengan kubus
            gc.drawImage(uiSpriteSheet,
                    PILL_SRC_X, PILL_SRC_Y, PILL_W, PILL_H,
                    pillX, startY - (destPillH / 2), destPillW, destPillH);

            // C. GAMBAR POWER BAR (PROCEDURAL)
            double barX = pillX + destPillW + 8;
            double barHeight = 12; // Sedikit lebih tipis biar elegan
            double maxBarWidth = 200;
            // Center bar secara vertikal terhadap ikon
            double barY = startY - (barHeight / 2);

            // 1. Background (Hitam)
            gc.setFill(Color.rgb(20, 20, 20, 0.8));
            gc.fillRect(barX, barY, maxBarWidth, barHeight);

            // 2. Border (Putih)
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(barX, barY, maxBarWidth, barHeight);

            // 3. Isi Bar (Gradient Color)
            Color barColor;
            if (ratio < 0.5) barColor = Color.LIME;
            else if (ratio < 0.8) barColor = Color.YELLOW;
            else barColor = Color.RED;

            gc.setFill(barColor);
            // Margin 2px di dalam border
            gc.fillRect(barX + 2, barY + 2, (maxBarWidth - 4) * ratio, barHeight - 4);

            // 4. Teks Persentase
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
            // Taruh teks sedikit di atas bar
            gc.fillText((int)(ratio * 100) + "%", barX + maxBarWidth + 10, barY + barHeight);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}