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
    private final List<Integer> pocketHistory = new ArrayList<>();

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
    private static Image ballSpriteSheet;   // Untuk Ball Tracker

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
    private static final double PILL_H = 16;     // Tinggi standar

    // --- SHOT TIMER VARIABLES ---
    private static final double TURN_TIME_LIMIT = 30.0; // 30 Detik
    private double currentTurnTime = TURN_TIME_LIMIT;

    // --- PAUSE SYSTEM ---
    private boolean isGamePaused = false;

    // Koordinat Tombol Pause (Pojok Kiri Atas)
    private static final double PAUSE_BTN_X = 20;
    private static final double PAUSE_BTN_Y = 20;
    private static final double PAUSE_BTN_SIZE = 40;

    @Override
    public void start(Stage primaryStage) {
        // 1. Init Logic Systems
        table = new Table(GAME_WIDTH, GAME_HEIGHT);
        gameRules = new GameRules(); // Inisialisasi Wasit

        // LOAD UI SPRITE
        try {
            uiSpriteSheet = new Image(getClass().getResourceAsStream("/assets/SMS_GUI_Display_NO_BG.png"));
            // Kita load juga ball sprite sheet untuk keperluan UI Tracker
            ballSpriteSheet = new Image(getClass().getResourceAsStream("/assets/SMS_GUI_Display_NO_BG.png"));
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
            if (isGamePaused) return;
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

            // --- DETEKSI KLIK TOMBOL PAUSE (Screen Coordinates) ---
            // Kita pakai e.getX() (Screen Space) karena tombol pause tidak ikut geser meja
            if (e.getX() >= PAUSE_BTN_X && e.getX() <= PAUSE_BTN_X + PAUSE_BTN_SIZE &&
                    e.getY() >= PAUSE_BTN_Y && e.getY() <= PAUSE_BTN_Y + PAUSE_BTN_SIZE) {

                isGamePaused = !isGamePaused; // Toggle Pause
                return; // Stop processing click ke game
            }

            // --- JIKA GAME PAUSED, ABAIKAN INPUT LAIN ---
            if (isGamePaused) return;

            // Logic Game Normal
            if (isBallInHandActive()) {
                tryPlaceCueBall(logicX, logicY);
            } else {
                cueStick.handleMousePressed(logicX, logicY);
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (isGamePaused) return;
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
            if (isGamePaused) return;
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

            renderGame();

            // GAMBAR UI TAMBAHAN
            drawPauseMenu(gc);   // Overlay hanya jika isGamePaused=true
            drawPauseButton(gc); // Tombol selalu terlihat

            // --- JIKA PAUSE, STOP LOGIC DI BAWAH INI ---
            if (isGamePaused) {
                return; // Skip update fisika & rules
            }

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

            // --- UPDATE LOGIC SHOT TIMER ---
            if (is8BallMode && !gameRules.isGameOver()) {
                // Timer hanya jalan jika:
                // 1. Bola semua diam (fase membidik)
                // 2. Tidak sedang menaruh bola (Ball in Hand placement mode pause dulu biar ga panik)
                boolean isAimingPhase = cueStick.areAllBallsStopped();
                boolean isPlacingMode = gameRules.isBallInHand();

                if (isAimingPhase && !isPlacingMode) {
                    currentTurnTime -= deltaTime;

                    // JIKA WAKTU HABIS (TIME FOUL)
                    if (currentTurnTime <= 0) {
                        // Reset Timer
                        currentTurnTime = TURN_TIME_LIMIT;

                        // Panggil Rule: Time Foul
                        // Kita bisa pakai handleFoul lewat method helper atau public access
                        // Karena handleFoul private, kita trigger via switchTurn manual atau bikin method public baru.
                        // Solusi Cepat: Kita paksa ganti giliran & kasih ball in hand via GameRules logic.

                        // PENTING: Kita harus buat method public 'triggerTimeFoul' di GameRules dulu
                        // Tapi untuk sekarang, kita anggap GameRules punya method itu.
                        gameRules.triggerTimeFoul();
                    }
                }
            }
            // 2. LOGIC TURN
            checkGameRules();
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

                for (ObjectBall b : potted) {
                    pocketHistory.add(b.getNumber());
                }

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

                    currentTurnTime = TURN_TIME_LIMIT;
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
            // 1. BERSIHKAN LAYAR
            gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            // 2. LAYER PALING BAWAH: Background Sidebar (Pipa & Keranjang)
            // Kita gambar ini DULUAN, supaya nanti tertimpa oleh Meja.
            // Ini kuncinya agar pipa terlihat "muncul dari bawah meja".
            drawSideBarBackground(gc);

            // 3. LAYER TENGAH: Game World (Meja, Bola, Stik)
            gc.save();
            gc.translate(currentOffsetX, currentOffsetY);

            table.draw(gc); // Meja digambar di sini, menutupi pangkal pipa

            // Gambar Bola (Termasuk CueBall saat didrag)
            for (GameObject obj : gameObjects) {
                if (obj instanceof Ball) obj.draw(gc);
            }

            // Stik HANYA digambar jika TIDAK sedang Ball In Hand
            if (!isBallInHandActive()) {
                cueStick.draw(gc);
            } else {
                // Visual Bantuan saat Dragging
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.setLineDashes(5);
                gc.strokeOval(cueBall.getPosition().getX() - 20, cueBall.getPosition().getY() - 20, 40, 40);
                gc.setLineDashes(null);
            }

            gc.restore();

            // 4. LAYER ATAS: Isi Keranjang & HUD
            // Bola history digambar belakangan supaya muncul DI ATAS keranjang background
            drawSideBarBalls(gc);

            drawHUD();
        }

        private void drawHUD() {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
            // Debug Mouse
            // gc.fillText(String.format("Mouse: (%.0f, %.0f)", mouseLogicX, mouseLogicY), 20, 30);

            // --- HUD 8-BALL MODE ---
            if (is8BallMode) {
                // TAMPILAN BARU 8-BALL
                if (gameRules.isGameOver()) {
                    // Layar Game Over (Biarkan kode Game Over yang sudah Anda buat sebelumnya di sini)
                    // ... (Kode Winner/Loser Screen Anda yang bagus tadi) ...

                    // Copy paste logic Game Over warna hijau/merah Anda di sini
                    if (gameRules.isCleanWin()) {
                        gc.setFill(Color.LIMEGREEN);
                        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 40));
                        gc.fillText("🏆 WINNER! 🏆", 400, 300); // Center Screen roughly
                        gc.fillText(gameRules.getStatusMessage(), 350, 350);
                    } else {
                        gc.setFill(Color.RED);
                        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 40));
                        gc.fillText("☠ GAME OVER ☠", 380, 300);
                        gc.fillText(gameRules.getStatusMessage(), 300, 350);
                    }

                } else {
                    // IN-GAME HUD (BALL TRACKER)
                    drawBallTracker(gc);

                    // BALL IN HAND ALERT (Tampilkan Besar di Tengah Layar)
                    if (gameRules.isBallInHand()) {
                        gc.setFill(Color.WHITE);
                        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 30));
                        gc.fillText("BALL IN HAND", (canvas.getWidth()/2) - 100, 150);
                    }
                }
            } else {
                // ARCADE MODE
                if (physicsEngine != null) {
                    gc.setFill(Color.YELLOW);
                    gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
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

    /**
     * Menggambar status bola pemain (Mana yang sisa, mana yang sudah masuk).
     * UPDATE LAYOUT: Menggeser posisi player lebih ke tengah layar (Area Kuning).
     */
    /**
     * Menggambar status bola pemain.
     * UPDATE: Hide bola saat OPEN table, dan Hapus teks kategori.
     */
    private void drawBallTracker(GraphicsContext gc) {
        boolean[] isBallActive = new boolean[16];
        for (GameObject obj : gameObjects) {
            if (obj instanceof ObjectBall) {
                ObjectBall b = (ObjectBall) obj;
                if (b.isActive()) isBallActive[b.getNumber()] = true;
            }
        }

        GameRules.TableState state = gameRules.getTableState();
        GameRules.PlayerTurn turn = gameRules.getCurrentTurn();

        boolean p1IsSolid = true;
        if (state == GameRules.TableState.P1_STRIPES) p1IsSolid = false;

        // Layout Config
        double centerX = canvas.getWidth() / 2;
        double offsetFromCenter = 300;

        // Render Player 1 (Kiri)
        // Kita kirim 'state' untuk pengecekan apakah harus gambar bola atau tidak
        drawPlayerStats(gc, "PLAYER 1", p1IsSolid, isBallActive,
                centerX - offsetFromCenter - 150, 30,
                turn == GameRules.PlayerTurn.PLAYER_1, state);

        // Render Player 2 (Kanan)
        drawPlayerStatsRightAligned(gc, "PLAYER 2", !p1IsSolid, isBallActive,
                centerX + offsetFromCenter + 150, 30,
                turn == GameRules.PlayerTurn.PLAYER_2, state);

        drawBottomRightStatus(gc);
    }

    // Helper Player 1 (Kiri)
    private void drawPlayerStats(GraphicsContext gc, String name, boolean isSolid, boolean[] activeBalls,
                                 double x, double y, boolean isMyTurn, GameRules.TableState state) {
        // 1. Nama Pemain (Selalu Tampil)
        gc.setFill(isMyTurn ? Color.YELLOW : Color.GRAY);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        gc.fillText(name, x, y);

        // --- VISUAL SHOT TIMER (KIRI) ---
        if (isMyTurn && !gameRules.isGameOver() && !gameRules.isBallInHand()) {
            double barW = 120; // Panjang Bar
            double barH = 4;   // Tebal Bar
            double barY = y + 5; // Sedikit di bawah nama

            // Background Abu
            gc.setFill(Color.rgb(50, 50, 50));
            gc.fillRect(x, barY, barW, barH);

            // Isi Bar (Menyusut)
            double ratio = Math.max(0, currentTurnTime / TURN_TIME_LIMIT);

            // Warna: Hijau -> Kuning -> Merah
            if (ratio > 0.5) gc.setFill(Color.LIME);
            else if (ratio > 0.2) gc.setFill(Color.ORANGE);
            else gc.setFill(Color.RED);

            gc.fillRect(x, barY, barW * ratio, barH);
        }

        // 2. Cek State: Jika OPEN, berhenti di sini (Jangan gambar bola/teks apapun)
        if (state == GameRules.TableState.OPEN) {
            return;
        }

        // 3. Gambar Bola Mini (Hanya jika sudah assigned)
        // Teks "SOLIDS/STRIPES" SUDAH DIHAPUS sesuai request

        double ballSize = 20;
        double spacing = 22;
        int startBall = isSolid ? 1 : 9;
        int endBall = isSolid ? 7 : 15;

        for (int i = startBall; i <= endBall; i++) {
            double bx = x + ((i - startBall) * spacing);
            double by = y + 15; // Jarak dikit dari nama

            drawMiniBall(gc, i, bx, by, ballSize, activeBalls[i]);
        }
    }

    // Helper Player 2 (Kanan)
    private void drawPlayerStatsRightAligned(GraphicsContext gc, String name, boolean isSolid, boolean[] activeBalls,
                                             double x, double y, boolean isMyTurn, GameRules.TableState state) {
        // 1. Nama Pemain
        gc.setFill(isMyTurn ? Color.YELLOW : Color.GRAY);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        gc.fillText(name, x - 90, y);

        // --- VISUAL SHOT TIMER (KANAN) ---
        if (isMyTurn && !gameRules.isGameOver() && !gameRules.isBallInHand()) {
            double barW = 120;
            double barH = 4;
            double barY = y + 5;
            double barX = x - 90; // Samakan start X dengan nama

            // Background
            gc.setFill(Color.rgb(50, 50, 50));
            gc.fillRect(barX, barY, barW, barH);

            // Isi Bar (Rata Kanan effect? Atau Rata Kiri biasa saja biar konsisten)
            // Kita buat rata kiri (standard reading)
            double ratio = Math.max(0, currentTurnTime / TURN_TIME_LIMIT);

            if (ratio > 0.5) gc.setFill(Color.LIME);
            else if (ratio > 0.2) gc.setFill(Color.ORANGE);
            else gc.setFill(Color.RED);

            gc.fillRect(barX, barY, barW * ratio, barH);
        }

        // 2. Cek State: Jika OPEN, return.
        if (state == GameRules.TableState.OPEN) {
            return;
        }

        // 3. Gambar Bola Mini (Rata Kanan)
        double ballSize = 20;
        double spacing = 22;
        int startBall = isSolid ? 1 : 9;
        int endBall = isSolid ? 7 : 15;

        for (int i = endBall; i >= startBall; i--) {
            int offset = endBall - i;
            double bx = x - ballSize - (offset * spacing);
            double by = y + 15;

            drawMiniBall(gc, i, bx, by, ballSize, activeBalls[i]);
        }
    }

    // Menggambar Sprite Bola Kecil
    private void drawMiniBall(GraphicsContext gc, int number, double x, double y, double size, boolean isActive) {
        if (ballSpriteSheet == null) return;

        // Logika Sprite (Sama dengan class Ball)
        double srcX = 0;
        double srcY = 0;
        double spriteSize = 16; // Ukuran asli di PNG

        if (number <= 8) { // 1-8 (Row 1)
            srcX = (number - 1) * spriteSize;
            srcY = 0;
        } else { // 9-15 (Row 2)
            srcX = (number - 9) * spriteSize;
            srcY = 16;
        }

        // Efek Visual: Jika bola sudah masuk (isActive == false), buat transparan/gelap
        if (!isActive) {
            gc.setGlobalAlpha(0.3); // Redup
        }

        gc.drawImage(ballSpriteSheet, srcX, srcY, spriteSize, spriteSize, x, y, size, size);

        gc.setGlobalAlpha(1.0); // Reset alpha
    }

    /**
     * Menampilkan pesan status game (Foul, Info, Win).
     * UPDATE: Menggunakan JavaFX Shapes untuk menggambar ikon Foul (Lebih Konsisten).
     */
    private void drawBottomRightStatus(GraphicsContext gc) {
        double screenW = canvas.getWidth();
        double screenH = canvas.getHeight();

        double boxW = 350;
        double boxH = 40;
        double x = screenW - boxW - 30;
        double y = screenH - boxH - 20;

        String msg = gameRules.getStatusMessage();

        if (msg != null && !msg.isEmpty()) {
            // 1. Background Box
            gc.setFill(Color.rgb(0, 0, 0, 0.8));
            gc.fillRoundRect(x, y, boxW, boxH, 10, 10);

            // 2. Border Logic
            Color statusColor = Color.WHITE;
            if (msg.contains("FOUL")) statusColor = Color.RED;
            else if (msg.contains("WIN") || msg.contains("VICTORY")) statusColor = Color.LIME;
            else if (msg.contains("Nice") || msg.contains("Good")) statusColor = Color.CYAN;

            gc.setStroke(statusColor);
            gc.setLineWidth(2);
            gc.strokeRoundRect(x, y, boxW, boxH, 10, 10);

            // 3. Logic Ikon & Offset Teks
            double textOffsetX = 15;

            // GAMBAR IKON FOUL SECARA MANUAL (PROCEDURAL)
            if (msg.contains("FOUL")) {
                double iconSize = 22;
                double iconX = x + 10;
                double iconY = y + (boxH - iconSize) / 2; // Center Vertikal

                gc.save();
                gc.setStroke(Color.RED);
                gc.setLineWidth(2.5); // Ketebalan garis

                // A. Gambar Lingkaran
                gc.strokeOval(iconX, iconY, iconSize, iconSize);

                // B. Gambar Silang (X)
                // Kita beri padding 5px agar silang berada di dalam lingkaran
                gc.strokeLine(iconX + 6, iconY + 6, iconX + iconSize - 6, iconY + iconSize - 6);
                gc.strokeLine(iconX + iconSize - 6, iconY + 6, iconX + 6, iconY + iconSize - 6);

                gc.restore();

                textOffsetX = 45; // Geser teks agar tidak menumpuk ikon
            }

            // 4. Render Teks
            gc.setFill(statusColor);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
            gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
            gc.fillText(msg, x + textOffsetX, y + 26);
        }
    }

    /**
     * LAYER BAWAH: Menggambar Pipa & Keranjang Kosong.
     */
    private void drawSideBarBackground(GraphicsContext gc) {
        double screenW = canvas.getWidth();
        double screenH = canvas.getHeight();

        // --- KOORDINAT ---
        double sidebarX = screenW - 100;
        double bottomY = screenH - 110;
        double ballSize = 26;
        double spacing = 28;
        double maxBalls = 15;

        double railWidth = ballSize + 10;
        double railHeight = (maxBalls * spacing) + 20;
        double railTopY = bottomY - railHeight;
        double centerX = sidebarX + (railWidth / 2);

        // Titik Keluar Meja (Pojok Kanan Atas)
        // Geser ke KIRI (-5) agar ujung pipa nanti tertutup meja saat meja digambar
        double tableExitX = currentOffsetX + GAME_WIDTH - 5;
        double tableExitY = currentOffsetY + 5;
        double pipeRadius = 70;

        // --- 1. GAMBAR PIPA PENGHUBUNG ---
        gc.save();
        gc.setStroke(Color.rgb(85, 0, 0)); // Warna Kayu Gelap
        gc.setLineWidth(25);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);

        // Garis Horizontal (Dari Bawah Meja -> Mulai Lengkung)
        double turnCenterX = centerX - pipeRadius;
        gc.strokeLine(tableExitX, tableExitY, turnCenterX, tableExitY);

        // Busur Lengkung (Elbow)
        gc.strokeArc(centerX - (pipeRadius * 2), tableExitY,
                pipeRadius * 2, pipeRadius * 2,
                90, -90, javafx.scene.shape.ArcType.OPEN);

        // Garis Vertikal
        double verticalStartY = tableExitY + pipeRadius;
        gc.strokeLine(centerX, verticalStartY, centerX, railTopY + 15);
        gc.restore();

        // --- 2. GAMBAR KERANJANG (RAIL FRAME) ---
        gc.setFill(Color.rgb(20, 20, 20, 0.8));
        gc.fillRoundRect(sidebarX, railTopY - 5, railWidth, railHeight + 5, 15, 15);

        gc.setStroke(Color.rgb(85, 0, 0));
        gc.setLineWidth(4);
        gc.strokeLine(sidebarX, railTopY + 5, sidebarX, bottomY);
        gc.strokeLine(sidebarX + railWidth, railTopY + 5, sidebarX + railWidth, bottomY);
        gc.strokeArc(sidebarX, bottomY - 15, railWidth, 30, 180, 180, javafx.scene.shape.ArcType.OPEN);
    }

    /**
     * LAYER ATAS: Menggambar Bola di dalam keranjang.
     */
    private void drawSideBarBalls(GraphicsContext gc) {
        if (pocketHistory.isEmpty()) return;

        double screenW = canvas.getWidth();
        double screenH = canvas.getHeight();
        double sidebarX = screenW - 100;
        double bottomY = screenH - 110;
        double ballSize = 26;
        double spacing = 28;
        double railWidth = ballSize + 10;
        double centerX = sidebarX + (railWidth / 2);

        // Logika Stack Up (Bawah ke Atas)
        for (int i = 0; i < pocketHistory.size(); i++) {
            int ballNum = pocketHistory.get(i);
            double y = bottomY - 15 - (i * spacing);
            drawMiniBall(gc, ballNum, centerX - (ballSize/2), y, ballSize, true);
        }
    }

    /**
     * Menggambar Tombol Pause (Ikon Garis Dua ||) di pojok kiri atas.
     */
    private void drawPauseButton(GraphicsContext gc) {
        // Efek Hover/Click bisa ditambahkan nanti, sekarang statis dulu

        // 1. Background Kotak
        gc.setFill(Color.rgb(30, 30, 30, 0.8));
        gc.fillRoundRect(PAUSE_BTN_X, PAUSE_BTN_Y, PAUSE_BTN_SIZE, PAUSE_BTN_SIZE, 10, 10);

        // 2. Border Putih
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(PAUSE_BTN_X, PAUSE_BTN_Y, PAUSE_BTN_SIZE, PAUSE_BTN_SIZE, 10, 10);

        // 3. Ikon Pause (Dua Garis Vertikal)
        gc.setFill(Color.WHITE);
        double barW = 6;
        double barH = 16;
        double gap = 6;

        // Center ikon di dalam kotak
        double centerX = PAUSE_BTN_X + (PAUSE_BTN_SIZE / 2);
        double centerY = PAUSE_BTN_Y + (PAUSE_BTN_SIZE / 2);

        gc.fillRect(centerX - barW - (gap/2), centerY - (barH/2), barW, barH); // Bar Kiri
        gc.fillRect(centerX + (gap/2), centerY - (barH/2), barW, barH);       // Bar Kanan
    }

    /**
     * Menggambar Layar Gelap + Menu saat game dipause.
     */
    private void drawPauseMenu(GraphicsContext gc) {
        if (!isGamePaused) return;

        double screenW = canvas.getWidth();
        double screenH = canvas.getHeight();

        // 1. Overlay Gelap (Dimmed Background)
        gc.setFill(Color.rgb(0, 0, 0, 0.7)); // Hitam transparan 70%
        gc.fillRect(0, 0, screenW, screenH);

        // 2. Teks Judul PAUSED
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 60));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("PAUSED", screenW / 2, screenH / 2 - 50);

        // 3. Instruksi Sederhana
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 20));
        gc.setFill(Color.YELLOW);
        gc.fillText("Click PAUSE button again to Resume", screenW / 2, screenH / 2 + 20);

        // (Nanti di Fase Main Menu, kita bisa ganti ini dengan tombol interaktif Restart/Exit)
    }

    public static void main(String[] args) {
        launch(args);
    }
}