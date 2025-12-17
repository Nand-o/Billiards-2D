package com.billiards2d.core;

import static com.billiards2d.core.GameConstants.*;

import com.billiards2d.entities.balls.Ball;
import com.billiards2d.entities.balls.CueBall;
import com.billiards2d.entities.balls.ObjectBall;
import com.billiards2d.entities.CueStick;
import com.billiards2d.entities.Table;
import com.billiards2d.game.GameController;
import com.billiards2d.game.GameRules;
import com.billiards2d.game.PhysicsEngine;
import com.billiards2d.input.InputHandler;
import com.billiards2d.ui.FloatingText;
import com.billiards2d.ui.GameUIRenderer;
import com.billiards2d.ui.HUDRenderer;
import com.billiards2d.ui.SceneManager;
import com.billiards2d.util.Vector2D;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.prefs.Preferences;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.text.Text;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.LinearGradient;

/**
 * Kelas utama aplikasi JavaFX untuk permainan Billiards-2D.
 * <p>
 * Tanggung jawab utama:
 * - Inisialisasi aset dan subsistem (physics, rules, input, UI).
 * - Mengelola lifecycle aplikasi JavaFX (`start` dan `main`).
 * - Menyediakan titik integrasi antara subsistem (GameController,
 *   PhysicsEngine, SceneManager, dan InputHandler).
 * </p>
 *
 * Catatan:
 * - Dokumentasi ini ditulis dalam Bahasa Indonesia. Semua teks yang
 *   tampil di antarmuka permainan (HUD, notifikasi, pesan kemenangan)
 *   dibiarkan dalam Bahasa Inggris dan tidak dimodifikasi oleh Javadoc.
 * - Perubahan yang dilakukan hanya pada bagian dokumentasi; tidak ada
 *   perubahan logika permainan di file ini.
 *
 * @since 2025-12-13
 */
public class BilliardApp extends Application {

    /**
     * Konstruktor default `BilliardApp`.
     * Disediakan agar Javadoc tidak menghasilkan peringatan default-constructor.
     */
    public BilliardApp() { }

    // SWITCH MODE:
    // true  = Menggunakan Aturan 8-Ball (Giliran, Solid/Stripes, Win/Loss)
    // false = Menggunakan Mode Arcade (Skor Bebas)
    private boolean is8BallMode = true;

    // --- CORE SYSTEMS ---
    private GraphicsContext mainGC;
    private Canvas canvas;
    private final List<GameObject> gameObjects = new ArrayList<>();
    private final List<Integer> pocketHistory = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

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

    // --- NEW UI OVERLAYS ---
    private javafx.scene.layout.VBox pauseOverlay;
    private javafx.scene.layout.VBox gameOverOverlay;
    private javafx.scene.text.Text gameOverTitle; // Untuk update teks "WINNER/LOSER"
    private javafx.scene.text.Text gameOverMessage; // Untuk pesan detail

    // ASSETS UI
    private static Image uiSpriteSheet;
    private static Image ballSpriteSheet;
    private static Image cueStickImage;

    // --- SHOT TIMER VARIABLES ---
    private double currentTurnTime = TURN_TIME_LIMIT;

    // --- PAUSE SYSTEM ---
    private boolean isGamePaused = false;

    // --- ARCADE MODE STATE ---
    private double arcadeTimer = ARCADE_START_TIME;
    private boolean isArcadeGameOver = false;

    // HIGH SCORE SYSTEM
    private int highScore = 0;
    private Preferences prefs; // Untuk simpan data ke registry komputer

    // --- RENDERING ---
    private GameUIRenderer gameUIRenderer;
    private HUDRenderer hudRenderer;
    
    // --- INPUT HANDLING ---
    private InputHandler inputHandler;
    
    // --- GAME CONTROLLER ---
    private GameController gameController;

    // --- SCENE MANAGEMENT ---
    private Stage primaryStage;
    private GameLoop gameLoop;
    private SceneManager sceneManager;

    /**
     * Memulai aplikasi JavaFX: inisialisasi subsistem (prefs, scene manager,
     * renderer, assets), kemudian menampilkan menu utama.
     *
     * @param primaryStage Stage utama JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage; // Simpan referensi

        try {
            // 1. Inisialisasi Objek Preferences FIRST!
            prefs = Preferences.userNodeForPackage(BilliardApp.class);

            // 2. High score will be loaded by GameController
            // (Moved to avoid duplication)

        } catch (Exception e) {
            System.err.println("Gagal load preferences: " + e.getMessage());
        }
        
        // Initialize Scene Manager AFTER prefs
        sceneManager = new SceneManager(primaryStage, prefs);
        sceneManager.setCallbacks(
            this::startGame,
            this::restartGame,
            this::returnToMenu,
            this::togglePause
        );

        // Load Assets (Lakukan sekali di awal)
        try {
            if (uiSpriteSheet == null) uiSpriteSheet = new Image(getClass().getResourceAsStream(ASSET_UI_SPRITE));
            if (ballSpriteSheet == null) ballSpriteSheet = new Image(getClass().getResourceAsStream(ASSET_BALL_SPRITE));
            if (cueStickImage == null) cueStickImage = new Image(getClass().getResourceAsStream(ASSET_CUE_STICK));
        } catch (Exception e) {
            System.err.println("Gagal load asset: " + e.getMessage());
        }

        // Initialize renderers
        gameUIRenderer = new GameUIRenderer(uiSpriteSheet, ballSpriteSheet);
        hudRenderer = new HUDRenderer();

        // Langsung masuk ke Menu Utama
        sceneManager.showMainMenu(gameLoop);
    }

    /**
     * Tampilkan overlay Game Over dengan judul dan pesan.
     *
     * @param title teks judul overlay
     * @param titleColor warna judul
     * @param message pesan detail yang ditampilkan
     */
    public void showGameOver(String title, Color titleColor, String message) {
        if (gameOverOverlay != null) {
            gameOverTitle.setText(title);
            gameOverTitle.setFill(titleColor);
            gameOverMessage.setText(message);
            gameOverOverlay.setVisible(true);
        }
    }

    /**
     * Sembunyikan overlay Game Over jika sedang tampil.
     */
    public void hideGameOver() {
        if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
    }

    /**
     * Mereset permainan ke kondisi awal (Skor 0, Waktu Penuh, Rack Baru).
     */
    private void restartGame() {
        if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
        if (pauseOverlay != null) pauseOverlay.setVisible(false);

        // 1. Reset State Variables
        arcadeTimer = ARCADE_START_TIME;
        isArcadeGameOver = false;
        currentTurnTime = TURN_TIME_LIMIT; // CRITICAL FIX: Reset shot timer
        turnInProgress = false;

        // 2. Reset Rules FIRST (CRITICAL: Must be before initializeGameObjects!)
        gameRules.resetGame();

        // 3. Bersihkan Data Lama
        gameObjects.clear();
        pocketHistory.clear();
        floatingTexts.clear();

        // 4. Re-initialize game objects (now gameRules is already reset)
        initializeGameObjects();
        
        // 5. RECREATE GameController with new objects (CRITICAL FIX!)
        // GameController must be recreated because initializeGameObjects() created new instances
        // of cueStick, cueBall, physicsEngine, etc. Old GameController has stale references.
        gameController = new GameController(
            gameObjects, cueStick, cueBall, gameRules, physicsEngine,
            floatingTexts, pocketHistory, is8BallMode, prefs
        );
        gameController.setCallbacks(
            this::respawnArcadeRack,
            new GameController.GameOverCallback() {
                /**
                 * Callback: tampilkan overlay Game Over.
                 *
                 * @param title judul overlay
                 * @param titleColor warna judul
                 * @param message pesan detail
                 */
                @Override
                public void showGameOver(String title, Color titleColor, String message) {
                    if (!gameOverOverlay.isVisible()) {
                        gameOverTitle.setText(title);
                        gameOverTitle.setFill(titleColor);
                        gameOverMessage.setText(message);
                        gameOverOverlay.setVisible(true);
                    }
                }
                
                /**
                 * Callback: sembunyikan overlay Game Over.
                 */
                @Override
                public void hideGameOver() {
                    if (gameOverOverlay.isVisible()) {
                        gameOverOverlay.setVisible(false);
                    }
                }
            }
        );
        highScore = gameController.getHighScore();
        
        // 6. Update InputHandler with new objects (CRITICAL FIX!)
        if (inputHandler != null) {
            inputHandler.setCueStick(this.cueStick);
            inputHandler.setCueBall(this.cueBall);
            inputHandler.setGameRules(this.gameRules);
        }
    }

    /**
     * Return to main menu (scene transition)
     */
    private void returnToMenu() {
        // Stop game loop if running
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        // Show main menu
        sceneManager.showMainMenu(gameLoop);
    }

    // ==================== SCENE MANAGEMENT METHODS MOVED ====================
    // The following methods have been extracted to SceneManager.java:
    // - showMainMenu(), createHudBox(), createRetroButton(), loadCustomFont(), drawGridPattern()
    // - createPauseOverlay(), createGameOverOverlay()
    // ==========================================================================

    /**
     * Memulai permainan (Scene Game) berdasarkan mode yang dipilih.
     */
    private void startGame(boolean mode8Ball) {
        // 1. Set Mode Sesuai Pilihan Menu
        this.is8BallMode = mode8Ball;

        // 2. Reset Variable State (Penting agar game fresh)
        isGamePaused = false;
        isArcadeGameOver = false;
        turnInProgress = false;
        arcadeTimer = ARCADE_START_TIME;
        currentTurnTime = 30.0; // Reset shot timer
        pocketHistory.clear();
        floatingTexts.clear();
        // 3. SETUP ROOT & CANVAS
        // Gunakan StackPane agar bisa menumpuk UI di atas Canvas
        StackPane root = new StackPane();

        canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        mainGC = canvas.getGraphicsContext2D();

        // Layer 1: Canvas Game
        root.getChildren().add(canvas);

        // 4. SETUP OVERLAYS (UI MENU) - Use SceneManager
        pauseOverlay = sceneManager.createPauseOverlay();
        
        SceneManager.GameOverComponents gameOverComponents = sceneManager.createGameOverOverlay();
        gameOverOverlay = gameOverComponents.overlay;
        gameOverTitle = gameOverComponents.title;
        gameOverMessage = gameOverComponents.message;

        // Layer 2 & 3: Menu (Default Hidden)
        root.getChildren().addAll(pauseOverlay, gameOverOverlay);

        // 5. Init Objects
        gameRules = new GameRules();
        initializeGameObjects();
        
        // 6. Setup GameController
        gameController = new GameController(
            gameObjects, cueStick, cueBall, gameRules, physicsEngine,
            floatingTexts, pocketHistory, is8BallMode, prefs
        );
        gameController.setCallbacks(
            this::respawnArcadeRack,
            new GameController.GameOverCallback() {
                /**
                 * Callback: tampilkan overlay Game Over.
                 *
                 * @param title judul overlay
                 * @param titleColor warna judul
                 * @param message pesan detail
                 */
                @Override
                public void showGameOver(String title, Color titleColor, String message) {
                    if (!gameOverOverlay.isVisible()) {
                        gameOverTitle.setText(title);
                        gameOverTitle.setFill(titleColor);
                        gameOverMessage.setText(message);
                        gameOverOverlay.setVisible(true);
                    }
                }
                
                /**
                 * Callback: sembunyikan overlay Game Over.
                 */
                @Override
                public void hideGameOver() {
                    if (gameOverOverlay.isVisible()) {
                        gameOverOverlay.setVisible(false);
                    }
                }
            }
        );
        
        // Load high score from GameController
        highScore = gameController.getHighScore();
        
        // 7. Setup InputHandler
        inputHandler = new InputHandler(cueStick, cueBall, gameRules, gameObjects, is8BallMode);
        inputHandler.setCallbacks(
            this::togglePause,
            this::debugClearTable
        );
        
        // 8. INPUT HANDLING - Delegate to InputHandler
        canvas.setOnMouseMoved(e -> {
            updateOffsets();
            inputHandler.updateState(isGamePaused, isArcadeGameOver, currentOffsetX, currentOffsetY);
            inputHandler.handleMouseMoved(e);
        });

        canvas.setOnMousePressed(e -> {
            updateOffsets();
            inputHandler.updateState(isGamePaused, isArcadeGameOver, currentOffsetX, currentOffsetY);
            inputHandler.handleMousePressed(e);
        });

        canvas.setOnMouseDragged(e -> {
            updateOffsets();
            inputHandler.updateState(isGamePaused, isArcadeGameOver, currentOffsetX, currentOffsetY);
            inputHandler.handleMouseDragged(e);
        });

        canvas.setOnMouseReleased(e -> {
            updateOffsets();
            inputHandler.updateState(isGamePaused, isArcadeGameOver, currentOffsetX, currentOffsetY);
            inputHandler.handleMouseReleased(e);
        });

        // 9. Setup Scene & Keyboard
        Scene gameScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        gameScene.setFill(Color.BLACK); // Background hitam di belakang canvas

        gameScene.setOnKeyPressed(event -> {
            inputHandler.handleKeyPressed(event);
        });

        // Binding Resizing
        canvas.widthProperty().bind(gameScene.widthProperty());
        canvas.heightProperty().bind(gameScene.heightProperty());

        primaryStage.setTitle(is8BallMode ? "Billiard 2D - 8 Ball Pool" : "Billiard 2D - Arcade Time Attack");
        primaryStage.setScene(gameScene);

        gameLoop = new GameLoop();
        gameLoop.start();
    }

    // Helper kecil untuk styling tombol sementara
    private void styleButton(javafx.scene.control.Button btn) {
        btn.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        btn.setPrefWidth(300);
        btn.setPrefHeight(50);
        btn.setStyle("-fx-background-color: #ffd700; -fx-text-fill: black; -fx-cursor: hand;");
    }

    /**
     * DEBUG: Membersihkan meja secara instan untuk mengetes logika Stage Clear.
     */
    private void debugClearTable() {
        boolean anyBallRemoved = false;

        for (GameObject obj : gameObjects) {
            if (obj instanceof ObjectBall) {
                ObjectBall ball = (ObjectBall) obj;
                if (ball.isActive()) {
                    // 1. Matikan Bola
                    ball.setActive(false);
                    ball.setVelocity(new Vector2D(0, 0));

                    // 2. Masukkan ke Laporan Physics (Supaya dapat bonus waktu & score)
                    physicsEngine.forcePocketBall(ball);

                    anyBallRemoved = true;
                }
            }
        }

        if (anyBallRemoved) {
            // 3. Reset cue ball ke posisi awal
            cueBall.setPosition(new Vector2D(GAME_WIDTH / 4.0, GAME_HEIGHT / 2.0));
            cueBall.setVelocity(new Vector2D(0, 0));
            
            // 4. Trigger table clear bonus BEFORE respawn (untuk arcade mode)
            if (!is8BallMode && gameController != null) {
                // Manually trigger table clear bonus in GameController
                double stageBonusTime = 30.0;
                int stageBonusScore = 500;
                
                gameController.setArcadeTimer(gameController.getArcadeTimer() + stageBonusTime);
                physicsEngine.modifyArcadeScore(stageBonusScore);
                
                // Add floating texts
                double cx = (GAME_WIDTH/2) + currentOffsetX;
                double cy = (GAME_HEIGHT/2) + currentOffsetY;
                floatingTexts.add(new FloatingText(cx, cy - 40, "TABLE CLEARED!", Color.CYAN));
                floatingTexts.add(new FloatingText(cx, cy, "+" + stageBonusScore + " pts", Color.GOLD));
                floatingTexts.add(new FloatingText(cx, cy + 40, "+" + (int)stageBonusTime + "s", Color.LIME));
            }
            
            // 5. Spawn rack baru
            respawnArcadeRack();
            
            // 6. Reset turn state agar cue stick bisa digunakan
            turnInProgress = false;
            
            // 7. Sync state to GameController
            if (gameController != null) {
                gameController.setTurnInProgress(false);
            }
            
            // 8. Reset physics report AFTER processing
            physicsEngine.resetTurnReport();
        }
    }

    // --- HELPER METHODS UNTUK BALL IN HAND ---

    // ==================== OLD INPUT METHODS REMOVED ====================
    // The following methods have been extracted to InputHandler.java:
    // - isBallInHandActive(), moveCueBallToMouse(), tryPlaceCueBall()
    // =======================================================================

    private void updateOffsets() {
        double screenW = canvas.getWidth();
        double screenH = canvas.getHeight();
        currentOffsetX = (screenW - GAME_WIDTH) / 2;
        currentOffsetY = (screenH - GAME_HEIGHT) / 2;
    }

    private void initializeGameObjects() {
        gameObjects.clear();

        this.table = new Table(GAME_WIDTH, GAME_HEIGHT);

        // Posisi bola putih (Head Spot area)
        cueBall = new CueBall(new Vector2D(GAME_WIDTH / 4.0, GAME_HEIGHT / 2.0));

        List<Ball> allBalls = new ArrayList<>();
        allBalls.add(cueBall);

        gameObjects.add(cueBall);
        setupRack(allBalls); // Setup 15 bola

        this.cueStick = new CueStick(cueBall, allBalls, GAME_WIDTH, GAME_HEIGHT, gameRules, cueStickImage);
        this.cueStick.setArcadeMode(!is8BallMode);
        this.physicsEngine = new PhysicsEngine(table, gameObjects);

        for (Ball b : allBalls) {
            if (b != cueBall) { // CueBall jangan dimasukkan 2 kali
                gameObjects.add(b);
            }
        }

        gameObjects.add(physicsEngine);
    }

    // Method Setup Rack yang SUDAH DIPERBAIKI (No Duplicate, 8 di Tengah)
    private void setupRack(List<Ball> ballList) {
        double radius = BALL_RADIUS;
        double startX = GAME_WIDTH * 0.75;
        double startY = GAME_HEIGHT / 2.0;

        // LOGIKA KHUSUS 8-BALL (Nomor 1-15 Unik)
        List<Integer> availableNumbers = new ArrayList<>();
        if (is8BallMode) {
            for (int i = 1; i <= 15; i++) {
                if (i != 8) availableNumbers.add(i);
            }
            // java.util.Collections.shuffle(availableNumbers); // Acak posisi jika mau
        }

        int indexCounter = 0;

        for (int col = 0; col < 5; col++) {
            for (int row = 0; row <= col; row++) {
                double x = startX + (col * (radius * Math.sqrt(3) + RACK_HORIZONTAL_SPACING));
                double rowHeight = col * (radius * 2 + RACK_VERTICAL_SPACING);
                double yTop = startY - (rowHeight / 2.0);
                double y = yTop + (row * (radius * 2 + RACK_VERTICAL_SPACING));

                int ballNumber;
                boolean usePlain = false; // Flag sementara

                // POSISI TENGAH (Selalu Bola 8 Hitam)
                if (col == 2 && row == 1) {
                    ballNumber = 8;
                }
                else {
                    if (is8BallMode) {
                        // MODE 8-BALL: Ambil nomor urut 1-15
                        ballNumber = availableNumbers.get(indexCounter);
                        indexCounter++;
                    } else {
                        // MODE ARCADE: Merah & Kuning Polos
                        usePlain = true; // Aktifkan mode polos

                        // Pola Selang-seling
                        if ((col + row) % 2 == 0) {
                            ballNumber = 3; // Representasi Merah
                        } else {
                            ballNumber = 1; // Representasi Kuning
                        }
                    }
                }

                ObjectBall ball = new ObjectBall(new Vector2D(x, y), ballNumber);

                // Terapkan tekstur polos jika arcade mode (dan bukan bola 8)
                if (usePlain) {
                    ball.setUsePlainTexture(true);
                }

                ballList.add(ball);
                gameObjects.add(ball);
            }
        }
    }

    // --- GAME LOOP UTAMA ---
    private class GameLoop extends AnimationTimer {
        private long lastNanoTime = System.nanoTime();

        /**
         * Game loop handler executed by JavaFX AnimationTimer.
         *
         * @param currentNanoTime waktu sekarang dalam nanodetik
         */
        @Override
        public void handle(long currentNanoTime) {
            double deltaTime = (currentNanoTime - lastNanoTime) / 1_000_000_000.0;
            lastNanoTime = currentNanoTime;
            if (deltaTime > 0.05) deltaTime = 0.05;

            updateOffsets();

            renderGame();

            // GAMBAR UI TAMBAHAN
            gameUIRenderer.drawPauseButton(mainGC, PAUSE_BTN_X, PAUSE_BTN_Y); // Tombol selalu terlihat

            // DELEGATE GAME LOGIC TO GAME CONTROLLER
            gameController.update(deltaTime, isGamePaused, currentOffsetX, currentOffsetY);
            
            // SYNC STATE FROM GAME CONTROLLER
            arcadeTimer = gameController.getArcadeTimer();
            currentTurnTime = gameController.getCurrentTurnTime();
            highScore = gameController.getHighScore();
            isArcadeGameOver = gameController.isArcadeGameOver();
            turnInProgress = gameController.isTurnInProgress();
        }
        
        // ==================== OLD GAME LOOP METHODS REMOVED ====================
        // The following methods have been extracted to GameController.java:
        // - checkGameOverState(), checkGameRules(), processTurnEnd()
        // All physics update, timer logic, and turn management now in GameController
        // =======================================================================
    }

    private void renderGame() {
            double scrW = canvas.getWidth();
            double scrH = canvas.getHeight();

            // --- PERBAIKAN 2: JANGAN PAKAI clearRect! ---
            // clearRect membuat transparan -> tembus ke Scene putih.
            // Gunakan fillRect untuk mengecat background "Bioskop" (Margin Hitam/Abu)

            mainGC.setFill(Color.rgb(20, 20, 20)); // Warna Abu Gelap (Background Aplikasi)
            mainGC.fillRect(0, 0, scrW, scrH);
            // 2. LAYER PALING BAWAH: Background Sidebar (Pipa & Keranjang)
            // Kita gambar ini DULUAN, supaya nanti tertimpa oleh Meja.
            // Ini kuncinya agar pipa terlihat "muncul dari bawah meja".
            gameUIRenderer.drawSideBarBackground(mainGC, scrW, scrH, currentOffsetX, currentOffsetY);

            // 3. LAYER TENGAH: Game World (Meja, Bola, Stik)
            mainGC.save();
            mainGC.translate(currentOffsetX, currentOffsetY);

            table.draw(mainGC); // Meja digambar di sini, menutupi pangkal pipa

            // Gambar Bola (Termasuk CueBall saat didrag)
            for (GameObject obj : gameObjects) {
                if (obj instanceof Ball) obj.draw(mainGC);
            }

            // Stik HANYA digambar jika TIDAK sedang Ball In Hand
            if (!(is8BallMode && gameRules.isBallInHand())) {
                cueStick.draw(mainGC);
            } else {
                // Visual Bantuan saat Dragging
                mainGC.setStroke(Color.WHITE);
                mainGC.setLineWidth(2);
                mainGC.setLineDashes(5);
                mainGC.strokeOval(cueBall.getPosition().getX() - 20, cueBall.getPosition().getY() - 20, 40, 40);
                mainGC.setLineDashes(null);
            }

            mainGC.restore();

            // 4. LAYER ATAS: Isi Keranjang & HUD
            // Bola history digambar belakangan supaya muncul DI ATAS keranjang background
            gameUIRenderer.drawSideBarBalls(mainGC, scrW, scrH, pocketHistory, ballSpriteSheet, is8BallMode);

            Iterator<FloatingText> it = floatingTexts.iterator();
            while (it.hasNext()) {
                FloatingText ft = it.next();
                // Update posisi (gerak ke atas) - Butuh deltaTime (sedikit hacky ambil dr mana ya?)
                // Solusi: Kita update di GameLoop handle(), render di sini.
                // TAPI biar gampang, kita update statis 0.016 (60fps) di sini aja visual doang.
                boolean dead = ft.update(0.016);
                ft.draw(mainGC);
                if (dead) it.remove();
            }

            drawHUD();
        }

        private void drawHUD() {
            // Jika Overlay Game Over sedang muncul, kita bisa stop gambar HUD
            // (atau biarkan tetap jalan jika ingin terlihat di background).
            // Untuk kerapian, biarkan HUD standar (Skor/Timer) tetap terlihat di belakang overlay.

            mainGC.setFill(Color.WHITE);
            mainGC.setFont(Font.font("Consolas", FontWeight.BOLD, 14));

            // --- HUD 8-BALL MODE ---
            if (is8BallMode) {
                // BAGIAN YANG DIHAPUS: Blok if (gameRules.isGameOver()) ...
                // Kita ganti dengan logika ini:

                // Selama game belum selesai, gambar tracker
                // Atau biarkan tracker tetap terlihat meski game over (opsional)
                boolean[] isBallActive = new boolean[16];
                for (GameObject obj : gameObjects) {
                    if (obj instanceof ObjectBall) {
                        ObjectBall b = (ObjectBall) obj;
                        if (b.isActive()) isBallActive[b.getNumber()] = true;
                    }
                }
                gameUIRenderer.drawBallTracker(mainGC, canvas.getWidth(), isBallActive, 
                        gameRules.getTableState(), gameRules.getCurrentTurn(), 
                        gameRules, currentTurnTime, ballSpriteSheet, is8BallMode);
                gameUIRenderer.drawBottomRightStatus(mainGC, canvas.getWidth(), canvas.getHeight(), 
                        gameRules.getStatusMessage());

                // Alert Ball in Hand (Ini Status In-Game, jadi TETAP DISIMPAN)
                if (gameRules.isBallInHand() && !gameRules.isGameOver()) {
                    mainGC.setFill(Color.WHITE);
                    mainGC.setFont(Font.font("Consolas", FontWeight.BOLD, 30));
                    mainGC.fillText("BALL IN HAND", (canvas.getWidth()/2) - 100, 150);
                }
            }
            else {
                // --- HUD ARCADE (TIME ATTACK) ---

                // BAGIAN YANG DIHAPUS: Blok if (isArcadeGameOver) ... "TIME'S UP" ...

                // KITA HANYA PERTAHANKAN BAGIAN SKOR & TIMER (Bekas blok 'else')
                // Tujuannya agar saat Overlay muncul, skor terakhir tetap terlihat di background

                mainGC.setFont(Font.font("Consolas", FontWeight.BOLD, 30));

                // 1. TAMPILAN TIMER
                // Cegah timer negatif visual saat game over
                double displayTime = Math.max(0, arcadeTimer);

                int minutes = (int) displayTime / 60;
                int seconds = (int) displayTime % 60;
                String timeStr = String.format("%02d:%02d", minutes, seconds);

                mainGC.setTextAlign(javafx.scene.text.TextAlignment.LEFT);

                // Warna Timer
                if (displayTime <= 10.0 && displayTime > 0) mainGC.setFill(Color.RED);
                else mainGC.setFill(Color.WHITE);

                mainGC.fillText(timeStr, (canvas.getWidth()/2) - 40, 35);

                // 2. TAMPILAN SKOR
                mainGC.setFill(Color.YELLOW);
                mainGC.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
                mainGC.fillText("SCORE: " + physicsEngine.getArcadeScore(), (canvas.getWidth()/2) - 43, 60);

                // 3. HIGH SCORE
                mainGC.setFill(Color.GOLD);
                mainGC.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
                String bestText = "BEST: " + highScore;
                double bestX = canvas.getWidth() - 150;
                mainGC.fillText(bestText, bestX, 50);

                // Efek New Record
                if (physicsEngine.getArcadeScore() > 0 && physicsEngine.getArcadeScore() >= highScore) {
                    if ((System.currentTimeMillis() / 200) % 2 == 0) {
                        mainGC.setFill(Color.LIME);
                        mainGC.fillText("NEW RECORD!", bestX, 75);
                    }
                }
            }

            // Gambar Power Bar (Selalu Muncul)
            gameUIRenderer.drawPowerBar(mainGC, canvas.getHeight(), cueStick.getPowerRatio());
        }

    // ==================== OLD RENDERING METHODS REMOVED ====================
    // The following methods have been extracted to GameUIRenderer.java and HUDRenderer.java:
    // - drawPowerBar(), drawBallTracker(), drawPlayerStats(), drawPlayerStatsRightAligned()
    // - drawMiniBall(), drawBottomRightStatus(), drawSideBarBackground(), drawSideBarBalls()
    // - drawPauseButton(), drawPauseMenu()
    // =======================================================================

    // ==================== countActiveObjectBalls() REMOVED ====================
    // This method has been extracted to GameController.java
    // ==========================================================================

    private void respawnArcadeRack() {
        // 1. Hapus semua ObjectBall yang ada di list gameObjects
        // (Walaupun harusnya sudah inactive/masuk lubang, kita bersihkan biar memory aman)
        gameObjects.removeIf(obj -> obj instanceof ObjectBall);

        // 2. Setup Ulang Rack Baru
        List<Ball> newBalls = new ArrayList<>();
        setupRack(newBalls); // Panggil method setupRack yang sudah ada

        // 3. Masukkan ke Game Objects
        gameObjects.addAll(newBalls);

        // 4. Update Referensi di PhysicsEngine & CueStick
        // Karena PhysicsEngine memegang referensi ke list 'gameObjects' utama,
        // dia otomatis tahu ada bola baru.
        // TAPI CueStick memegang list 'allBalls' sendiri, jadi harus kita update juga.

        // Kita perlu update list 'allBalls' di CueStick.
        // Sayangnya di constructor CueStick kita pass List, tapi tidak ada setter.
        // SOLUSI CEPAT: Kita reset CueStick juga.

        // Re-create Cue Stick dengan list bola baru
        // Kita butuh list yang berisi CueBall + ObjectBalls baru
        List<Ball> allBallsForStick = new ArrayList<>();
        allBallsForStick.add(cueBall); // CueBall yang lama
        allBallsForStick.addAll(newBalls);

        // Update Stick
        this.cueStick = new CueStick(cueBall, allBallsForStick, GAME_WIDTH, GAME_HEIGHT, gameRules, cueStickImage);
        this.cueStick.setArcadeMode(true); // Jangan lupa set mode lagi
        
        // Update InputHandler dengan cueStick baru
        if (inputHandler != null) {
            inputHandler.setCueStick(this.cueStick);
        }

        // 5. Reset Posisi Cue Ball ke Head String (Biar adil)
        cueBall.setPosition(new Vector2D(GAME_WIDTH / 4.0, GAME_HEIGHT / 2.0));
        cueBall.setVelocity(new Vector2D(0, 0));
        cueBall.setActive(true);
        cueBall.setPendingRespawn(false);
    }

    // --- OVERLAY BUILDERS ---



    // Helper toggle pause sederhana
    private void togglePause() {
        // Jangan pause jika sedang game over
        if (isArcadeGameOver || (is8BallMode && gameRules.isGameOver())) return;

        isGamePaused = !isGamePaused;
        pauseOverlay.setVisible(isGamePaused);
    }

    /**
     * Entry point aplikasi ketika dijalankan dari command line.
     * Mendelegasikan kontrol ke JavaFX runtime dan memanggil {@link #start}.
     *
     * @param args Argumen baris perintah (tidak digunakan)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Game loop handler implementation for the main AnimationTimer.
     *
     * @param currentNanoTime waktu saat ini dalam nanodetik
     */
    @SuppressWarnings("unused")
    public void handleGameLoop(long currentNanoTime) {
        // Proxy to internal GameLoop.handle if needed
    }
}


