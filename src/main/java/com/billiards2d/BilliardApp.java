package com.billiards2d;

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

    // 1. UKURAN LAYAR APLIKASI (HD 720p)
    // Ini ukuran jendelanya (Canvas & Scene)
    private static final double WINDOW_WIDTH = 1280;
    private static final double WINDOW_HEIGHT = 720;

    // 2. UKURAN MEJA BILIAR (Play Area - Ratio 2:1)
    // Ini ukuran area hijaunya. Kita buat lebih kecil dari layar agar muat di tengah.
    // Contoh: 960x480 (Tetap 2:1 dan pas di tengah layar 720p)
    private static final double GAME_WIDTH = 960;
    private static final double GAME_HEIGHT = 480;

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

    // ASSETS UI - UPDATE KOORDINAT PRESISI
    private static Image uiSpriteSheet;
    private static Image ballSpriteSheet;   // Untuk Ball Tracker
    private static Image cueStickImage;

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

    // --- ARCADE MODE (TIME ATTACK) CONFIG ---
    private static final double ARCADE_START_TIME = 120.0; // 2 Menit
    private static final double TIME_BONUS_PER_BALL = 5.0; // Tambah 5 detik per bola
    private static final double TIME_PENALTY_FOUL = 10.0;  // Kurang 10 detik jika foul

    // STATE VARIABLES
    private double arcadeTimer = ARCADE_START_TIME;
    private boolean isArcadeGameOver = false;

    // HIGH SCORE SYSTEM
    private int highScore = 0;
    private Preferences prefs; // Untuk simpan data ke registry komputer

    // --- SCENE MANAGEMENT ---
    private Stage primaryStage;
    private GameLoop gameLoop;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage; // Simpan referensi

        try {
            // 1. Inisialisasi Objek Preferences
            prefs = Preferences.userNodeForPackage(BilliardApp.class);

            // 2. LOAD SCORE LAMA (Ini yang bikin Best Score kamu 0 terus sebelumnya)
            // Ambil data "arcade_highscore", kalau tidak ada, isi dengan 0
            highScore = prefs.getInt("arcade_highscore", 0);

        } catch (Exception e) {
            System.err.println("Gagal load preferences: " + e.getMessage());
        }

        // Load Assets (Lakukan sekali di awal)
        try {
            if (uiSpriteSheet == null) uiSpriteSheet = new Image(getClass().getResourceAsStream("/assets/SMS_GUI_Display_NO_BG.png"));
            if (ballSpriteSheet == null) ballSpriteSheet = new Image(getClass().getResourceAsStream("/assets/SMS_GUI_Display_NO_BG.png"));
            if (cueStickImage == null) cueStickImage = new Image(getClass().getResourceAsStream("/assets/cuestick.png"));
        } catch (Exception e) {
            System.err.println("Gagal load asset: " + e.getMessage());
        }

        // Langsung masuk ke Menu Utama
        showMainMenu();
    }

    /**
     * Mereset permainan ke kondisi awal (Skor 0, Waktu Penuh, Rack Baru).
     */
    private void restartGame() {
        if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
        if (pauseOverlay != null) pauseOverlay.setVisible(false);

        // 1. Reset State Arcade
        arcadeTimer = ARCADE_START_TIME;
        isArcadeGameOver = false;

        // 2. Bersihkan Data Lama
        gameObjects.clear();
        pocketHistory.clear();
        floatingTexts.clear();

        // 3. Reset Physics Engine Score (Penting!)
        // Kita buat instance baru atau reset manual score di dalamnya
        // Cara paling aman: Re-initialize semuanya
        initializeGameObjects();

        // 4. Reset Rules (Jika main 8-Ball)
        gameRules.resetGame();
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
            // Memuat gambar dari folder resources/assets/BackgroundMenu.jpg
            Image bgImage = new Image(getClass().getResourceAsStream("/assets/BackgroundMenu.jpg"));
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
            // Asumsi file font ada di folder /assets/
            // Pastikan Anda menaruh file .ttf di folder resources/assets/
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

        // 5. INPUT HANDLING
        // Mouse Handling (Logic Game)
        canvas.setOnMouseMoved(e -> {
            if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;
            updateOffsets();
            double logicX = e.getX() - currentOffsetX;
            double logicY = e.getY() - currentOffsetY;

            if (isBallInHandActive()) moveCueBallToMouse(logicX, logicY);
            else cueStick.handleMouseMoved(logicX, logicY);
        });

        canvas.setOnMousePressed(e -> {
            updateOffsets();
            // Cek Tombol Pause di Pojok Kiri Atas
            if (e.getX() >= PAUSE_BTN_X && e.getX() <= PAUSE_BTN_X + PAUSE_BTN_SIZE &&
                    e.getY() >= PAUSE_BTN_Y && e.getY() <= PAUSE_BTN_Y + PAUSE_BTN_SIZE) {

                togglePause(); // Method baru
                return;
            }

            if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;

            double logicX = e.getX() - currentOffsetX;
            double logicY = e.getY() - currentOffsetY;

            if (isBallInHandActive()) tryPlaceCueBall(logicX, logicY);
            else cueStick.handleMousePressed(logicX, logicY);
        });

        canvas.setOnMouseDragged(e -> {
            if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;
            updateOffsets();
            double logicX = e.getX() - currentOffsetX;
            double logicY = e.getY() - currentOffsetY;

            if (isBallInHandActive()) moveCueBallToMouse(logicX, logicY);
            else cueStick.handleMouseDragged(logicX, logicY);
        });

        canvas.setOnMouseReleased(e -> {
            if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;
            updateOffsets();
            if (!isBallInHandActive()) cueStick.handleMouseReleased(e.getX() - currentOffsetX, e.getY() - currentOffsetY);
        });

        // 6. Init Objects
        gameRules = new GameRules();
        initializeGameObjects();

        // 7. Setup Scene & Keyboard
        Scene gameScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        gameScene.setFill(Color.BLACK); // Background hitam di belakang canvas

        gameScene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE: togglePause(); break; // ESC sekarang toggle pause menu
                case C: if (!is8BallMode) debugClearTable(); break;
            }
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
            // 3. Paksa Turn Logic berjalan
            // Kita set turnInProgress = true, supaya di update loop berikutnya
            // dia mendeteksi bola diam -> lalu panggil processTurnEnd() -> lalu deteksi Table Cleared.
            turnInProgress = true;
        }
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
        }
    }

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
        double radius = 13.0;
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
                double x = startX + (col * (radius * Math.sqrt(3) + 2));
                double rowHeight = col * (radius * 2 + 2);
                double yTop = startY - (rowHeight / 2.0);
                double y = yTop + (row * (radius * 2 + 2));

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
            drawPauseButton(mainGC); // Tombol selalu terlihat

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
            } else if (!is8BallMode && !isArcadeGameOver) {
                // Kurangi waktu setiap frame
                arcadeTimer -= deltaTime;

                // Cek Game Over (Waktu Habis)
                if (arcadeTimer <= 0) {
                    arcadeTimer = 0;
                    isArcadeGameOver = true;
                    // Kita akan handle tampilan game over nanti di HUD
                }
            }

            if (!is8BallMode) {
                int currentScore = physicsEngine.getArcadeScore();
                if (currentScore > highScore) {
                    highScore = currentScore;

                    // --- PERBAIKAN 2: Safety Check & Save ---
                    try {
                        // Jaga-jaga: Jika prefs tiba-tiba null (penyebab error kamu), kita isi lagi
                        if (prefs == null) {
                            prefs = Preferences.userNodeForPackage(BilliardApp.class);
                        }

                        // Simpan ke Registry
                        if (prefs != null) {
                            prefs.putInt("arcade_highscore", highScore);
                            prefs.flush(); // Paksa simpan sekarang juga
                        }
                    } catch (Exception e) {
                        System.err.println("Gagal save score: " + e.getMessage());
                    }
                }
            }

            if (!is8BallMode && cueBall.isPendingRespawn()) {
                // Syarat: Tunggu bola lain berhenti dulu biar tidak chaos
                if (cueStick.areAllBallsStopped()) {
                    cueBall.setPosition(new Vector2D(GAME_WIDTH / 4.0, GAME_HEIGHT / 2.0));
                    cueBall.setVelocity(new Vector2D(0, 0));
                    cueBall.setPendingRespawn(false); // Matikan flag
                    cueBall.setActive(true);          // Munculkan bola
                }
            }

            // 2. LOGIC TURN
            checkGameOverState();
            checkGameRules();
        }

        private void checkGameOverState() {
            boolean isFinished = false;
            String titleText = "";
            Color titleColor = Color.WHITE;
            String msgText = "";

            if (is8BallMode && gameRules.isGameOver()) {
                isFinished = true;
                if (gameRules.isCleanWin()) {
                    titleText = "YOU WIN!";
                    titleColor = Color.LIME;
                } else {
                    titleText = "GAME OVER";
                    titleColor = Color.RED;
                }
                msgText = gameRules.getStatusMessage();
            }
            else if (!is8BallMode && isArcadeGameOver) {
                isFinished = true;
                titleText = "TIME'S UP!";
                titleColor = Color.ORANGE;
                msgText = "FINAL SCORE: " + physicsEngine.getArcadeScore();
            }

            // Trigger Overlay jika belum muncul
            if (isFinished && !gameOverOverlay.isVisible()) {
                gameOverTitle.setText(titleText);
                gameOverTitle.setFill(titleColor);
                gameOverMessage.setText(msgText);

                gameOverOverlay.setVisible(true);
                // Kita tidak stop loop agar background game masih ter-render di belakang overlay
            }

            // Jika restart (overlay msh aktif tapi game sudah reset), sembunyikan
            if (!isFinished && gameOverOverlay.isVisible()) {
                gameOverOverlay.setVisible(false);
            }
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
                // --- ARCADE MODE (TIME ATTACK) ---

                List<ObjectBall> potted = physicsEngine.getPocketedBalls();
                boolean isFoul = physicsEngine.isCueBallPocketed();

                // 1. HITUNG SCORE & WAKTU (BOLA WARNA)
                if (!potted.isEmpty()) {
                    double timeBonus = potted.size() * TIME_BONUS_PER_BALL;
                    int scoreBonus = potted.size() * 10; // Hitung total poin (10 per bola)

                    arcadeTimer += timeBonus;

                    // Koordinat Spawn (Tengah Meja acak dikit)
                    double spawnX = (GAME_WIDTH / 2) + currentOffsetX + (Math.random() * 40 - 20);
                    double spawnY = (GAME_HEIGHT / 2) + currentOffsetY;

                    // A. Teks Waktu (Warna Hijau)
                    floatingTexts.add(new FloatingText(spawnX, spawnY, "+" + (int)timeBonus + "s", Color.LIME));

                    // B. Teks Skor (Warna Emas) - TAMBAHAN BARU
                    // Kita munculkan sedikit di bawah teks waktu (+30px y)
                    floatingTexts.add(new FloatingText(spawnX, spawnY + 30, "+" + scoreBonus + " pts", Color.GOLD));
                }

                // 2. HITUNG PENALTI (FOUL)
                if (isFoul) {
                    // A. Penalti Waktu
                    arcadeTimer -= TIME_PENALTY_FOUL;
                    if (arcadeTimer < 0) arcadeTimer = 0;

                    // B. Penalti Skor (IMPLEMENTASI BARU)
                    physicsEngine.modifyArcadeScore(-10); // Kurangi 10 poin

                    // C. Efek Teks
                    double spawnX = (GAME_WIDTH / 2) + currentOffsetX;
                    double spawnY = (GAME_HEIGHT / 2) + currentOffsetY;
                    floatingTexts.add(new FloatingText(spawnX, spawnY, "-10s & -10pts", Color.RED));

                    // D. SET FLAG RESPAWN (CRITICAL FIX)
                    cueBall.setActive(false); // Pastikan mati dulu
                    cueBall.setPendingRespawn(true); // Minta hidup lagi
                    cueBall.setVelocity(new Vector2D(0,0)); // Stop gerak
                }

                // 3. Update History
                for (ObjectBall b : potted) {
                    pocketHistory.add(b.getNumber());
                }

                if (countActiveObjectBalls() == 0) {
                    // BERIKAN BONUS BESAR
                    double stageBonusTime = 30.0;
                    int stageBonusScore = 500;

                    arcadeTimer += stageBonusTime;
                    physicsEngine.modifyArcadeScore(stageBonusScore);

                    // Efek Teks Besar di Tengah
                    double cx = (GAME_WIDTH/2) + currentOffsetX;
                    double cy = (GAME_HEIGHT/2) + currentOffsetY;
                    floatingTexts.add(new FloatingText(cx, cy - 40, "TABLE CLEARED!", Color.CYAN));
                    floatingTexts.add(new FloatingText(cx, cy, "+" + stageBonusScore + " pts", Color.GOLD));
                    floatingTexts.add(new FloatingText(cx, cy + 40, "+" + (int)stageBonusTime + "s", Color.LIME));

                    // RESPAWN RACK (Refill Bola)
                    respawnArcadeRack();
                }

                physicsEngine.resetTurnReport();
            }
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
            drawSideBarBackground(mainGC);

            // 3. LAYER TENGAH: Game World (Meja, Bola, Stik)
            mainGC.save();
            mainGC.translate(currentOffsetX, currentOffsetY);

            table.draw(mainGC); // Meja digambar di sini, menutupi pangkal pipa

            // Gambar Bola (Termasuk CueBall saat didrag)
            for (GameObject obj : gameObjects) {
                if (obj instanceof Ball) obj.draw(mainGC);
            }

            // Stik HANYA digambar jika TIDAK sedang Ball In Hand
            if (!isBallInHandActive()) {
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
            drawSideBarBalls(mainGC);

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
                drawBallTracker(mainGC);

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
            drawPowerBar(mainGC);
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

        // --- UPDATE LOGIKA: CEK MODE GAME ---
        if (!is8BallMode && number != 8) {
            // ARCADE MODE (TEXTURE POLOS)
            // Di setupRack Arcade, kita hanya pakai nomor 1 (Kuning) dan 3 (Merah)

            if (number == 3) {
                // MERAH POLOS (Posisi X=128, Y=0)
                srcX = 128;
                srcY = 0;
            }
            else if (number == 1) {
                // KUNING POLOS (Posisi X=128, Y=16)
                srcX = 128;
                srcY = 16;
            }
            // Fallback: Jika ada nomor lain (bug), tetap pakai polos merah/kuning based on odd/even
            else {
                srcX = 128;
                srcY = (number % 2 == 0) ? 0 : 16;
            }
        }
        else {
            // 8-BALL MODE (TEXTURE ANGKA STANDAR)
            if (number <= 8) { // 1-8 (Row 1)
                srcX = (number - 1) * spriteSize;
                srcY = 0;
            } else { // 9-15 (Row 2)
                srcX = (number - 9) * spriteSize;
                srcY = 16;
            }
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
        double sidebarX = screenW - 70;
        double bottomY = screenH - 120;
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
        double sidebarX = screenW - 70;
        double bottomY = screenH - 120;
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

        // 1. SIMPAN STATE SEBELUM MENGUBAH ALIGNMENT
        gc.save();

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

        // 2. KEMBALIKAN STATE (Reset Alignment ke Default/LEFT)
        gc.restore();

        // (Nanti di Fase Main Menu, kita bisa ganti ini dengan tombol interaktif Restart/Exit)
    }

    private int countActiveObjectBalls() {
        int count = 0;
        for (GameObject obj : gameObjects) {
            if (obj instanceof ObjectBall) {
                if (((ObjectBall) obj).isActive()) {
                    count++;
                }
            }
        }
        return count;
    }

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