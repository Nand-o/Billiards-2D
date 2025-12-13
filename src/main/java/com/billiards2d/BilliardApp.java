package com.billiards2d;

import static com.billiards2d.GameConstants.*;

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

public class BilliardApp extends Application {

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

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage; // Simpan referensi

        try {
            // 1. Inisialisasi Objek Preferences
            prefs = Preferences.userNodeForPackage(BilliardApp.class);

            // 2. High score will be loaded by GameController
            // (Moved to avoid duplication)

        } catch (Exception e) {
            System.err.println("Gagal load preferences: " + e.getMessage());
        }

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
        showMainMenu();
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
                @Override
                public void showGameOver(String title, Color titleColor, String message) {
                    if (!gameOverOverlay.isVisible()) {
                        gameOverTitle.setText(title);
                        gameOverTitle.setFill(titleColor);
                        gameOverMessage.setText(message);
                        gameOverOverlay.setVisible(true);
                    }
                }
                
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
     * LANGKAH 5.2: Desain Visual Main Menu (Retro Arcade Style)
     * Mereplika tampilan referensi dengan background hijau dan tipografi pixel.
     */
    private void showMainMenu() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        // 1. ROOT CONTAINER (StackPane)
        StackPane root = new StackPane();

        // --- LAYER 1: BACKGROUND IMAGE ---
        try {
            // Memuat gambar dari folder resources/assets/
            Image bgImage = new Image(getClass().getResourceAsStream(ASSET_MENU_BACKGROUND));
            ImageView bgView = new ImageView(bgImage);

            // Scaling logic agar gambar memenuhi layar tanpa merusak rasio (Cover mode)
            bgView.fitWidthProperty().bind(primaryStage.widthProperty());
            bgView.fitHeightProperty().bind(primaryStage.heightProperty());

            // Opsional: Jika gambar aslinya tidak hijau, kita bisa beri filter warna hijau lewat kode
            // Tapi karena Anda bilang sudah menyiapkan gambar hijau, kita pakai mode normal saja.

            root.getChildren().add(bgView);
        } catch (Exception e) {
            // Fallback jika gambar tidak ditemukan: Pakai warna Hijau Gelap
            Canvas bgFallback = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
            GraphicsContext gc = bgFallback.getGraphicsContext2D();
            gc.setFill(Color.rgb(0, 40, 0)); // Hijau gelap banget
            gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            drawGridPattern(gc); // Gambar garis-garis grid hijau manual
            root.getChildren().add(bgFallback);
            System.err.println("Background image not found, using fallback. Error: " + e.getMessage());
        }

        // --- LAYER 2: UI LAYOUT (BorderPane) ---
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20, 40, 20, 40)); // Padding pinggir layar

        // A. TOP: HUD (1UP, HI-SCORE, 2UP) - Font: VCR OSD Mono
        HBox topHud = new HBox();
        topHud.setAlignment(Pos.CENTER);

        // Load High Score dari Prefs
        String highScoreStr = String.format("%06d", prefs.getInt("arcade_highscore", 0));

        // Kita buat 3 bagian: Kiri (1UP), Tengah (HI-SCORE), Kanan (2UP/CREDIT)
        VBox p1Box = createHudBox("1UP", "000000"); // Skor P1 (Dummy 0 dulu)
        VBox hiScoreBox = createHudBox("HI-SCORE", highScoreStr);
        VBox p2Box = createHudBox("2UP", "000000"); // Skor P2 (Dummy)

        // Spacer agar Hi-Score pas di tengah
        Region spacer1 = new Region(); HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);

        topHud.getChildren().addAll(p1Box, spacer1, hiScoreBox, spacer2, p2Box);
        mainLayout.setTop(topHud);

        // B. CENTER: JUDUL & TOMBOL
        VBox centerBox = new VBox(15); // Jarak antar elemen vertikal
        centerBox.setAlignment(Pos.CENTER);

        // 1. MAIN TITLE "BILLIARD 2D" - Font: ArcadeClassic
        Text title = new Text("BILLIARD 2D");
        // Gunakan method helper untuk load font, fallback ke Impact jika file font tidak ada
        title.setFont(loadCustomFont("ArcadeClassic.ttf", 90, "Impact"));

        // Styling Gradient (Merah -> Oranye -> Kuning) seperti referensi
        LinearGradient titleGradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#ff4e00")),  // Atas: Merah Oranye
                new Stop(0.5, Color.web("#ffcc00")),  // Tengah: Emas
                new Stop(1.0, Color.web("#ffff00"))   // Bawah: Kuning
        );
        title.setFill(titleGradient);
        title.setStroke(Color.BLACK); // Outline Hitam
        title.setStrokeWidth(3);

        // Efek Bayangan Teks (Drop Shadow Solid)
        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.BLACK);
        titleShadow.setOffsetX(5);
        titleShadow.setOffsetY(5);
        titleShadow.setRadius(0); // Radius 0 bikin bayangan tajam (pixel style)
        title.setEffect(titleShadow);

        // 2. SUBTITLE
        Text subTitle = new Text("ULTIMATE ARCADE EXPERIENCE");
        subTitle.setFont(loadCustomFont("PixelOperator-Bold.ttf", 20, "Consolas"));
        subTitle.setFill(Color.web("#dca466")); // Warna kulit/krem retro
        subTitle.setEffect(new DropShadow(2, Color.BLACK));

        // Spacer antara judul dan tombol
        Region titleSpacer = new Region();
        titleSpacer.setPrefHeight(40);

        // 3. BUTTONS (Tombol Emas/Oranye) - Font: Pixel Operator
        Button btn8Ball = createRetroButton("PLAY 8-BALL (2 Player)", () -> startGame(true));
        Button btnArcade = createRetroButton("ARCADE RUSH (1 Player)", () -> startGame(false));
        Button btnExit = createRetroButton("EXIT GAME", () -> primaryStage.close());

        centerBox.getChildren().addAll(title, subTitle, titleSpacer, btn8Ball, btnArcade, btnExit);
        mainLayout.setCenter(centerBox);

        // C. BOTTOM: CREDIT & COPYRIGHT
        VBox bottomBox = new VBox(5);
        bottomBox.setAlignment(Pos.CENTER);

        Text creditText = new Text("CREDIT 012");
        creditText.setFont(loadCustomFont("VCR_OSD_MONO_1.001.ttf", 24, "Courier New"));
        creditText.setFill(Color.WHITE);
        creditText.setEffect(new DropShadow(2, Color.BLACK));

        Text copyText = new Text("© 2025 BILLIARD2D PROJECT. CREATED WITH JAVAFX.");
        copyText.setFont(loadCustomFont("PixelOperator-Bold.ttf", 12, "Consolas"));
        copyText.setFill(Color.WHITE);

        bottomBox.getChildren().addAll(creditText, copyText);
        mainLayout.setBottom(bottomBox);

        // Masukkan Layout UI ke Root
        root.getChildren().add(mainLayout);

        // --- LAYER 3: CRT SCANLINE EFFECT (Opsional - Estetika) ---
        // Membuat garis-garis hitam tipis transparan di seluruh layar
        Canvas crtCanvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        GraphicsContext crtGC = crtCanvas.getGraphicsContext2D();
        crtGC.setFill(Color.rgb(0, 0, 0, 0.15)); // Hitam transparan (15%)
        for (int y = 0; y < WINDOW_HEIGHT; y += 4) { // Gambar garis setiap 4 pixel
            crtGC.fillRect(0, y, WINDOW_WIDTH, 2);
        }
        // Matikan interaksi mouse ke layer efek ini supaya tombol di bawahnya bisa diklik
        crtCanvas.setMouseTransparent(true);
        root.getChildren().add(crtCanvas);

        // SET SCENE
        Scene menuScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setTitle("Billiard 2D - Main Menu");
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    // --- HELPER METHODS UNTUK MENU RETRO ---

    /**
     * Membuat kotak HUD (Skor) di bagian atas.
     */
    private VBox createHudBox(String label, String value) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);

        Text lbl = new Text(label);
        lbl.setFont(loadCustomFont("ArcadeClassic.ttf", 18, "Impact"));
        lbl.setFill(Color.CYAN); // Warna label HUD biasanya Cyan atau Merah
        lbl.setStroke(Color.BLACK);
        lbl.setStrokeWidth(1);

        Text val = new Text(value);
        val.setFont(loadCustomFont("VCR_OSD_MONO_1.001.ttf", 20, "Courier New"));
        val.setFill(Color.WHITE);
        val.setEffect(new DropShadow(2, Color.BLACK));

        box.getChildren().addAll(lbl, val);
        return box;
    }

    /**
     * Membuat tombol gaya Retro Arcade (Kapsul Emas).
     */
    private Button createRetroButton(String text, Runnable action) {
        Button btn = new Button(text);
        // Load font untuk tombol
        btn.setFont(loadCustomFont("PixelOperator-Bold.ttf", 22, "Consolas"));

        btn.setPrefWidth(400);
        btn.setPrefHeight(55);

        // CSS Styling Retro: Gradient Emas, Border Tebal, Text Hitam
        String normalStyle =
                "-fx-background-color: linear-gradient(to bottom, #ffcc00, #ff9900); " + // Emas ke Oranye
                        "-fx-text-fill: #3e2723; " + // Coklat Tua/Hitam
                        "-fx-background-radius: 15; " + // Sudut sedikit membulat (bukan bulat penuh)
                        "-fx-border-color: #3e2723; " + // Border warna Coklat Tua
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 15; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 0, 0, 4, 4);"; // Bayangan tajam ke kanan bawah

        String hoverStyle =
                "-fx-background-color: linear-gradient(to bottom, #ffeb3b, #ffc107); " + // Lebih terang saat hover
                        "-fx-text-fill: black; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: black; " +
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 15; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 0, 0, 4, 4);";

        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
        btn.setOnAction(e -> action.run());

        return btn;
    }

    /**
     * Helper untuk memuat Custom Font. Jika gagal, pakai font sistem.
     * @param fontFileName Nama file font di folder /assets/fonts/ (Idealnya) atau root.
     * @param size Ukuran font.
     * @param fallbackFontName Nama font sistem cadangan (cth: "Arial", "Impact").
     */
    private Font loadCustomFont(String fontFileName, double size, String fallbackFontName) {
        try {
            // Font path construction
            String path = "/assets/" + fontFileName;
            Font font = Font.loadFont(getClass().getResourceAsStream(path), size);

            if (font != null) {
                return font;
            }
        } catch (Exception e) {
            // Silent fail, lanjut ke fallback
        }

        // Jika gagal load, kembalikan font sistem + Bold
        return Font.font(fallbackFontName, FontWeight.BOLD, size);
    }

    /**
     * Fallback menggambar Grid jika gambar background gagal dimuat.
     */
    private void drawGridPattern(GraphicsContext gc) {
        gc.setStroke(Color.rgb(0, 100, 0)); // Hijau lebih terang
        gc.setLineWidth(2);

        // Gambar Grid
        int step = 60;
        for (int x = 0; x < WINDOW_WIDTH; x += step) gc.strokeLine(x, 0, x, WINDOW_HEIGHT);
        for (int y = 0; y < WINDOW_HEIGHT; y += step) gc.strokeLine(0, y, WINDOW_WIDTH, y);
    }

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

        // 4. SETUP OVERLAYS (UI MENU)
        createPauseOverlay();     // Helper method baru
        createGameOverOverlay();  // Helper method baru

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
                @Override
                public void showGameOver(String title, Color titleColor, String message) {
                    if (!gameOverOverlay.isVisible()) {
                        gameOverTitle.setText(title);
                        gameOverTitle.setFill(titleColor);
                        gameOverMessage.setText(message);
                        gameOverOverlay.setVisible(true);
                    }
                }
                
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

    private void createPauseOverlay() {
        pauseOverlay = new VBox(20); // Spacing 20px
        pauseOverlay.setAlignment(Pos.CENTER);
        // Background Gelap Transparan
        pauseOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        pauseOverlay.setVisible(false); // Default Hidden

        // Judul
        Text title = new Text("PAUSED");
        title.setFont(Font.font("Impact", 60));
        title.setFill(Color.WHITE);
        title.setStroke(Color.BLACK);
        title.setStrokeWidth(2);

        // Tombol-tombol
        Button btnResume = createRetroButton("RESUME GAME", () -> togglePause());
        Button btnMenu = createRetroButton("MAIN MENU", () -> showMainMenu());
        Button btnExit = createRetroButton("EXIT DESKTOP", () -> primaryStage.close());

        // Kecilkan sedikit tombol pause menu dibanding main menu
        btnResume.setPrefWidth(300);
        btnMenu.setPrefWidth(300);
        btnExit.setPrefWidth(300);

        pauseOverlay.getChildren().addAll(title, btnResume, btnMenu, btnExit);
    }

    private void createGameOverOverlay() {
        gameOverOverlay = new VBox(20);
        gameOverOverlay.setAlignment(Pos.CENTER);
        gameOverOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        gameOverOverlay.setVisible(false);

        // Kita simpan referensi text agar bisa diubah isinya (WIN/LOSE) nanti
        gameOverTitle = new Text("GAME OVER");
        gameOverTitle.setFont(Font.font("Impact", 80));
        gameOverTitle.setFill(Color.RED);
        gameOverTitle.setStroke(Color.WHITE);
        gameOverTitle.setStrokeWidth(3);

        gameOverMessage = new Text("");
        gameOverMessage.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        gameOverMessage.setFill(Color.YELLOW);

        Button btnRestart = createRetroButton("PLAY AGAIN", () -> restartGame());
        Button btnMenu = createRetroButton("MAIN MENU", () -> showMainMenu());

        gameOverOverlay.getChildren().addAll(gameOverTitle, gameOverMessage, btnRestart, btnMenu);
    }

    // Helper toggle pause sederhana
    private void togglePause() {
        // Jangan pause jika sedang game over
        if (isArcadeGameOver || (is8BallMode && gameRules.isGameOver())) return;

        isGamePaused = !isGamePaused;
        pauseOverlay.setVisible(isGamePaused);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
