package com.billiards2d;

import com.billiards2d.core.GameBus;
import com.billiards2d.net.NetworkManager;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private StackPane rootPane; // Root pane untuk menumpuk Canvas dan Menu
    private VBox menuBox;       // Container UI Menu
    private Button hamburgerBtn; // Tombol Menu (Fitur Baru)

    // Daftar semua objek game yang perlu di-update setiap frame
    private final List<GameObject> gameObjects = new ArrayList<>();

    private Table table;        // Referensi ke objek Meja
    private CueStick cueStick; // Referensi ke Stik untuk input handling
    private CueBall cueBall;    // Referensi ke Bola Putih untuk HUD info
    private PhysicsEngine physicsEngine; // Referensi ke Physics Engine
    private NetworkManager networkManager; // Manager Koneksi

    // Variabel debug untuk menampilkan info di HUD (Heads-Up Display)
    private double mouseX, mouseY;

    // --- State Permainan (Fitur Baru) ---
    private boolean isGameRunning = false;
    private boolean isOnline = false;
    private boolean isMyTurn = true; // Default true untuk Local/Host
    private int currentPlayer = 1;
    private int p1Score = 0;
    private int p2Score = 0;
    private boolean wasBallsMoving = false;
    private boolean ballPottedThisTurn = false;

    /**
     * Metode utama JavaFX yang dipanggil saat aplikasi dimulai.
     * Mengatur Stage, Scene, Canvas, dan Input Listeners.
     */
    @Override
    public void start(Stage primaryStage) {
        // Init Event Bus
        setupGameBus();

        // 1. Init Table untuk menghitung ukuran total window (Area Main + Dinding)
        table = new Table(GAME_WIDTH, GAME_HEIGHT);
        double totalW = GAME_WIDTH + (table.getWallThickness() * 2);
        double totalH = GAME_HEIGHT + (table.getWallThickness() * 2);

        // 2. Setup Canvas sebagai area menggambar
        Canvas canvas = new Canvas(totalW, totalH);
        gc = canvas.getGraphicsContext2D();

        rootPane = new StackPane(canvas); // Canvas layer paling bawah
        rootPane.setStyle("-fx-background-color: #222;");

        // 3. Buat UI (Hamburger & Menu)
        createHamburgerButton(); // Tombol Hamburger (Layer Tengah)
        createMainMenu();        // Menu Overlay (Layer Paling Atas)

        Scene scene = new Scene(rootPane, totalW, totalH);

        // 4. Input Handling (Mouse)
        double offset = table.getWallThickness();

        canvas.setOnMouseMoved(e -> {
            if (!isGameRunning) return;
            if (isOnline && !isMyTurn) return;

            mouseX = e.getX() - offset;
            mouseY = e.getY() - offset;
            cueStick.handleMouseMoved(offsetEvent(e, -offset));
        });

        canvas.setOnMousePressed(e -> {
            if (!isGameRunning || (isOnline && !isMyTurn)) return;
            cueStick.handleMousePressed(offsetEvent(e, -offset));
        });

        canvas.setOnMouseDragged(e -> {
            if (!isGameRunning || (isOnline && !isMyTurn)) return;
            mouseX = e.getX() - offset;
            mouseY = e.getY() - offset;
            cueStick.handleMouseDragged(offsetEvent(e, -offset));
        });

        canvas.setOnMouseReleased(e -> {
            if (!isGameRunning || (isOnline && !isMyTurn)) return;
            cueStick.handleMouseReleased(offsetEvent(e, -offset));
        });

        // 5. Finalisasi Stage
        primaryStage.setTitle("Billiard Simulation - 2 Player & Online");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // 6. Mulai Loop
        GameLoop gameLoop = new GameLoop();
        gameLoop.start();
    }

    private void setupGameBus() {
        GameBus.subscribe(GameBus.EventType.BALL_POTTED, o -> {
            Ball b = (Ball) o;
            if (!(b instanceof CueBall)) {
                ballPottedThisTurn = true;
                if (currentPlayer == 1) p1Score++;
                else p2Score++;
            }
        });

        GameBus.subscribe(GameBus.EventType.REMOTE_SHOT, o -> {
            Vector2D force = (Vector2D) o;
            cueBall.hit(force);
            if (isOnline) isMyTurn = true;
        });

        GameBus.subscribe(GameBus.EventType.SHOT_TAKEN, o -> {
            if (isOnline) isMyTurn = false;
        });
    }

    // --- HAMBURGER BUTTON ---
    private void createHamburgerButton() {
        hamburgerBtn = new Button("☰"); // Unicode Hamburger Icon
        hamburgerBtn.setFont(Font.font("Consolas", 24));
        // Style transparan biar menyatu dengan background
        hamburgerBtn.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

        // Posisikan di pojok kiri atas
        StackPane.setAlignment(hamburgerBtn, Pos.TOP_LEFT);
        StackPane.setMargin(hamburgerBtn, new Insets(10));

        // Logic tombol: Pause game & Buka Menu
        hamburgerBtn.setOnAction(e -> {
            isGameRunning = false;
            menuBox.setVisible(true);
            hamburgerBtn.setVisible(false); // Sembunyikan tombol saat menu terbuka
        });

        hamburgerBtn.setVisible(false); // Default hidden (karena awal mulai di menu)
        rootPane.getChildren().add(hamburgerBtn);
    }

    private void createMainMenu() {
        menuBox = new VBox(15);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-padding: 20;");

        Label title = new Label("BILLIARDS 2D");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Impact", 48));

        Button btnPractice = createButton("PRACTICE MODE", () -> startLocal(false));
        Button btnLocal = createButton("2 PLAYER (LOCAL)", () -> startLocal(true));
        Button btnHost = createButton("ONLINE: HOST GAME", this::startHost);
        Button btnJoin = createButton("ONLINE: JOIN GAME", this::startJoin);

        // Tombol Exit yang berfungsi
        Button btnExit = createButton("EXIT", Platform::exit);

        menuBox.getChildren().addAll(title, btnPractice, btnLocal, btnHost, btnJoin, btnExit);
        rootPane.getChildren().add(menuBox);
    }

    private Button createButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMinWidth(250);
        btn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 16px; -fx-border-color: #666; -fx-border-width: 2px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-size: 16px; -fx-border-color: #888; -fx-border-width: 2px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 16px; -fx-border-color: #666; -fx-border-width: 2px;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    // --- Mode Handlers ---
    private void startLocal(boolean twoPlayer) {
        isOnline = false;
        isMyTurn = true;
        resetGame();

        menuBox.setVisible(false);     // Tutup Menu
        hamburgerBtn.setVisible(true); // Munculkan Tombol Hamburger
        isGameRunning = true;
    }

    private void startHost() {
        networkManager = new NetworkManager();
        networkManager.startServer(12345);
        isOnline = true;
        isMyTurn = true;
        resetGame();

        menuBox.setVisible(false);
        hamburgerBtn.setVisible(true);
        isGameRunning = true;
    }

    private void startJoin() {
        TextInputDialog dialog = new TextInputDialog("localhost");
        dialog.setTitle("Join Game");
        dialog.setHeaderText("Enter Host IP:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(ip -> {
            networkManager = new NetworkManager();
            networkManager.connectClient(ip, 12345);
            isOnline = true;
            isMyTurn = false;
            resetGame();

            menuBox.setVisible(false);
            hamburgerBtn.setVisible(true);
            isGameRunning = true;
        });
    }

    private MouseEvent offsetEvent(MouseEvent e, double shift) {
        return new MouseEvent(
                e.getSource(), e.getTarget(), e.getEventType(),
                e.getX() + shift, e.getY() + shift,
                e.getScreenX(), e.getScreenY(),
                e.getButton(), e.getClickCount(),
                e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown(),
                e.isPrimaryButtonDown(), e.isMiddleButtonDown(), e.isSecondaryButtonDown(),
                e.isSynthesized(), e.isPopupTrigger(), e.isStillSincePress(), e.getPickResult()
        );
    }

    private void resetGame() {
        gameObjects.clear();
        p1Score = 0; p2Score = 0; currentPlayer = 1;

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

                ObjectBall ball = new ObjectBall(new Vector2D(x, y), colors[ballCount % colors.length]);

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

            gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
            table.draw(gc);

            if (!isGameRunning) return;

            // --- UPDATE LOGIC ---
            boolean anyMoving = false;

            int subSteps = 4;
            double subDeltaTime = deltaTime / subSteps;

            for (int step = 0; step < subSteps; step++) {
                for (GameObject obj : gameObjects) {
                    obj.update(subDeltaTime);
                    if (obj instanceof Ball && ((Ball) obj).getVelocity().length() > 0.1) {
                        anyMoving = true;
                    }
                }
            }

            cueStick.update(deltaTime);

            // --- TURN LOGIC ---
            if (wasBallsMoving && !anyMoving) {
                if (!ballPottedThisTurn) {
                    currentPlayer = (currentPlayer == 1) ? 2 : 1;
                }
                ballPottedThisTurn = false;
            }
            wasBallsMoving = anyMoving;

            // --- RENDER LOGIC ---
            gc.save();
            gc.translate(table.getWallThickness(), table.getWallThickness());

            for (GameObject obj : gameObjects) {
                if(obj instanceof Ball) obj.draw(gc);
            }
            cueStick.draw(gc);

            gc.restore();

            // --- RESPAWN ---
            if (cueBall.isPendingRespawn()) {
                if (!anyMoving) {
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
            gc.setFont(Font.font("Consolas", 20));
            gc.fillText("P1: " + p1Score, 30, 40);
            gc.fillText("P2: " + p2Score, GAME_WIDTH - 100, 40);

            gc.setFill(Color.YELLOW);
            String turnText;
            if (isOnline) {
                turnText = isMyTurn ? "YOUR TURN" : "OPPONENT TURN";
            } else {
                turnText = "PLAYER " + currentPlayer;
            }
            gc.fillText(turnText, GAME_WIDTH/2.0 - 60, 40);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}