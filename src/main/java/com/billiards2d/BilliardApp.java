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
import javafx.scene.text.FontWeight;
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
    private NetworkManager networkManager; // Manager Koneksi (Fitur Baru)

    // Variabel debug untuk menampilkan info di HUD (Heads-Up Display)
    private double mouseX, mouseY;

    // --- State Permainan ---
    private boolean isGameRunning = false;
    private boolean isOnline = false;
    private boolean isMyTurn = true; // Default true untuk Local/Host
    private int currentPlayer = 1;
    private int p1Score = 0;
    private int p2Score = 0;
    private boolean wasBallsMoving = false;
    private boolean ballPottedThisTurn = false;

    // FIX: Flag untuk mencegah double turn change
    private boolean turnChangeProcessed = false;

    // FIX: Track siapa yang terakhir menembak untuk pemberian skor yang akurat
    private int lastShooter = 1;

    // Identitas Player: 0=Local, 1=Host (Server), 2=Client (Joiner)
    private int myPlayerId = 0;

    /**
     * Metode utama JavaFX yang dipanggil saat aplikasi dimulai.
     * Mengatur Stage, Scene, Canvas, dan Input Listeners.
     */
    @Override
    public void start(Stage primaryStage) {
        // Init Event Bus untuk komunikasi antar modul
        setupGameBus();

        // 1. Init Table untuk menghitung ukuran total window (Area Main + Dinding)
        table = new Table(GAME_WIDTH, GAME_HEIGHT);
        double totalW = GAME_WIDTH + (table.getWallThickness() * 2);
        double totalH = GAME_HEIGHT + (table.getWallThickness() * 2);

        // 2. Setup Canvas sebagai area menggambar
        Canvas canvas = new Canvas(totalW, totalH);
        gc = canvas.getGraphicsContext2D();

        rootPane = new StackPane(canvas); // Canvas layer paling bawah
        rootPane.setStyle("-fx-background-color: #222;"); // Background gelap di luar meja

        // 3. Buat UI (Hamburger & Menu)
        createHamburgerButton(); // Tombol Hamburger (Layer Tengah)
        createMainMenu();        // Menu Overlay (Layer Paling Atas)

        Scene scene = new Scene(rootPane, totalW, totalH);

        // 3. Input Handling (Mouse)
        double offset = table.getWallThickness();

        canvas.setOnMouseMoved(e -> {
            if (!isGameRunning) return;
            // Proteksi: Block input mouse jika online dan bukan giliran kita
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

        // 4. Finalisasi Stage
        primaryStage.setTitle("Billiard Simulation - 2 Player & Online");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // 5. Init Objek Game dan Mulai Loop
        GameLoop gameLoop = new GameLoop();
        gameLoop.start();
    }

    // Konfigurasi sistem event agar Host bisa jadi wasit skor
    private void setupGameBus() {
        // Logic Bola Masuk
        GameBus.subscribe(GameBus.EventType.BALL_POTTED, o -> {
            Ball b = (Ball) o;

            // --- LOGIKA HOST AUTHORITY (DIPERBAIKI) ---
            // Jika saya Client (P2), SAYA TIDAK BOLEH UPDATE SKOR SENDIRI.
            // Saya harus menunggu update dari Host.
            if (isOnline && myPlayerId == 2) {
                System.out.println("[CLIENT] Detected ball potted, waiting for Host sync...");
                return; // Abaikan event ini, tunggu GAME_SYNC
            }

            // Jika saya Host atau Main Lokal, baru hitung skor
            if (!(b instanceof CueBall)) {
                // FIX: Set flag dulu bahwa ada bola masuk
                ballPottedThisTurn = true;

                // FIX: Berikan skor ke pemain yang TERAKHIR MENEMBAK (lastShooter)
                // Bukan currentPlayer, karena currentPlayer mungkin sudah berubah
                System.out.println("[SCORE] Ball potted by Player " + lastShooter + "!");

                if (lastShooter == 1) {
                    p1Score++;
                    System.out.println("[SCORE] P1 scored! Total: " + p1Score);
                } else {
                    p2Score++;
                    System.out.println("[SCORE] P2 scored! Total: " + p2Score);
                }

                // FIX: Kirim update segera setelah bola masuk (Host Only)
                if (isOnline && myPlayerId == 1 && networkManager != null) {
                    System.out.println("[HOST] Sending score update: P1=" + p1Score + " P2=" + p2Score + " CurrentPlayer=" + currentPlayer);
                    networkManager.sendState(p1Score, p2Score, currentPlayer);
                }
            } else {
                // Foul: Bola putih masuk
                ballPottedThisTurn = false;
                System.out.println("[FOUL] Cue ball potted by Player " + lastShooter + "!");
            }
        });

        // Logic Menerima Tembakan Lawan
        GameBus.subscribe(GameBus.EventType.REMOTE_SHOT, o -> {
            Vector2D force = (Vector2D) o;

            // FIX: HARDENED LOGIC untuk menentukan siapa penembak (lawan)
            // Jika online, lastShooter adalah lawan dari myPlayerId.
            // Ini mencegah Host salah mengira tembakan lawan sebagai tembakan sendiri.
            if (isOnline) {
                lastShooter = (myPlayerId == 1) ? 2 : 1;
            } else {
                lastShooter = currentPlayer;
            }

            System.out.println("[REMOTE] Received opponent shot from Player " + lastShooter + ": " + force);

            // FIX: Reset flag penting sebelum bola bergerak
            ballPottedThisTurn = false;
            turnChangeProcessed = false;

            cueBall.hit(force);
        });

        // Logic Saya Nembak
        GameBus.subscribe(GameBus.EventType.SHOT_TAKEN, o -> {
            Vector2D force = (Vector2D) o;

            // FIX: Catat siapa yang menembak SEBELUM bola bergerak
            lastShooter = currentPlayer;

            // FIX: Reset flag penting
            ballPottedThisTurn = false;
            turnChangeProcessed = false; // Reset flag untuk turn baru

            System.out.println("[LOCAL] Shot taken by Player " + lastShooter + " with force: " + force);

            if (isOnline) {
                // FIX: Lock turn hanya setelah bola benar-benar ditembak
                isMyTurn = false;
                System.out.println("[TURN] Locked turn, waiting for balls to stop...");
            }
        });

        // Logic Sinkronisasi (Client Nurut Host) - DIPERBAIKI
        GameBus.subscribe(GameBus.EventType.GAME_SYNC, o -> {
            // Hanya terima sync kalau kita Online
            if (!isOnline) return;

            int[] state = (int[]) o;
            int newP1Score = state[0];
            int newP2Score = state[1];
            int newCurrentPlayer = state[2];

            System.out.println("[SYNC] Received state - P1:" + newP1Score + " P2:" + newP2Score + " Turn:" + newCurrentPlayer);

            // FIX: Update state dengan atomic operation
            Platform.runLater(() -> {
                p1Score = newP1Score;
                p2Score = newP2Score;
                currentPlayer = newCurrentPlayer;

                // Update visual giliran berdasarkan data Host
                boolean newTurnStatus = (currentPlayer == myPlayerId);

                if (isMyTurn != newTurnStatus) {
                    isMyTurn = newTurnStatus;
                    System.out.println("[TURN] Turn changed to: " + (isMyTurn ? "MY TURN" : "OPPONENT TURN"));
                }
            });
        });
    }

    // --- UI HELPERS ---

    private void createHamburgerButton() {
        hamburgerBtn = new Button("☰");
        hamburgerBtn.setFont(Font.font("Consolas", 24));
        hamburgerBtn.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        StackPane.setAlignment(hamburgerBtn, Pos.TOP_LEFT);
        StackPane.setMargin(hamburgerBtn, new Insets(10));

        hamburgerBtn.setOnAction(e -> {
            isGameRunning = false;
            menuBox.setVisible(true);
            hamburgerBtn.setVisible(false);
        });

        hamburgerBtn.setVisible(false);
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

    // --- GAME MODES ---

    private void startLocal(boolean twoPlayer) {
        isOnline = false;
        myPlayerId = 0;
        isMyTurn = true;
        resetGame();
        menuBox.setVisible(false);
        hamburgerBtn.setVisible(true);
        isGameRunning = true;
        System.out.println("[MODE] Started Local Game");
    }

    private void startHost() {
        networkManager = new NetworkManager();
        networkManager.startServer(12345);
        isOnline = true;
        myPlayerId = 1; // Host adalah Player 1
        isMyTurn = true;
        resetGame();
        menuBox.setVisible(false);
        hamburgerBtn.setVisible(true);
        isGameRunning = true;
        System.out.println("[MODE] Started as Host (Player 1)");
    }

    private void startJoin() {
        TextInputDialog dialog = new TextInputDialog("localhost:12345");
        dialog.setTitle("Join Game");
        dialog.setHeaderText("Enter Host Address (IP:Port):");
        dialog.setContentText("Address:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            String host = input;
            int port = 12345;
            if (input.contains(":")) {
                String[] parts = input.split(":");
                host = parts[0];
                try { port = Integer.parseInt(parts[1]); } catch (Exception e) {}
            }
            networkManager = new NetworkManager();
            networkManager.connectClient(host, port);
            isOnline = true;
            myPlayerId = 2; // Client adalah Player 2
            isMyTurn = false; // Client menunggu giliran dari Host
            resetGame();
            menuBox.setVisible(false);
            hamburgerBtn.setVisible(true);
            isGameRunning = true;
            System.out.println("[MODE] Started as Client (Player 2)");
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
        p1Score = 0;
        p2Score = 0;
        currentPlayer = 1;
        lastShooter = 1; // FIX: Reset lastShooter juga
        ballPottedThisTurn = false;
        turnChangeProcessed = false;

        cueBall = new CueBall(new Vector2D(GAME_WIDTH/4.0, GAME_HEIGHT/2.0));

        List<Ball> allBalls = new ArrayList<>();
        allBalls.add(cueBall);
        gameObjects.add(cueBall);

        setupRack(allBalls);

        this.cueStick = new CueStick(cueBall, allBalls, GAME_WIDTH, GAME_HEIGHT);
        this.physicsEngine = new PhysicsEngine(table, gameObjects);

        gameObjects.addAll(allBalls);
        gameObjects.add(physicsEngine);

        System.out.println("[GAME] Game reset complete");
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

    /**
     * Inner Class yang menangani Game Loop utama menggunakan AnimationTimer.
     * Menggunakan FIXED TIME STEP agar fisika deterministik di semua komputer (Penting untuk Online).
     */
    private class GameLoop extends AnimationTimer {
        private long lastNanoTime = System.nanoTime();
        private final double FIXED_TIME_STEP = 1.0 / 60.0;
        private double accumulator = 0.0;

        @Override
        public void handle(long currentNanoTime) {
            double frameTime = (currentNanoTime - lastNanoTime) / 1_000_000_000.0;
            lastNanoTime = currentNanoTime;

            if (frameTime > 0.25) frameTime = 0.25;

            accumulator += frameTime;

            gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
            table.draw(gc);

            if (!isGameRunning) return;

            boolean anyMoving = false;
            while (accumulator >= FIXED_TIME_STEP) {
                anyMoving = false;

                int subSteps = 4;
                double subDeltaTime = FIXED_TIME_STEP / subSteps;

                for (int step = 0; step < subSteps; step++) {
                    for (int i=0; i<gameObjects.size(); i++) {
                        gameObjects.get(i).update(subDeltaTime);
                    }
                    for (GameObject obj : gameObjects) {
                        if (obj instanceof Ball && ((Ball) obj).getVelocity().length() > 0.1) {
                            anyMoving = true;
                        }
                    }
                }
                cueStick.update(FIXED_TIME_STEP);
                accumulator -= FIXED_TIME_STEP;
            }

            // --- TURN LOGIC & SYNC (DIPERBAIKI) ---
            // FIX: Deteksi transisi dari bergerak ke berhenti
            if (wasBallsMoving && !anyMoving && !turnChangeProcessed) {
                System.out.println("[TURN] All balls stopped.");
                System.out.println("[TURN] Last shooter: Player " + lastShooter);
                System.out.println("[TURN] Current player: Player " + currentPlayer);
                System.out.println("[TURN] Ball potted this turn: " + ballPottedThisTurn);

                turnChangeProcessed = true; // FIX: Set flag untuk mencegah double processing

                // HOST ONLY LOGIC: Hanya Host yang boleh ubah giliran
                if (!isOnline || myPlayerId == 1) {
                    int nextPlayer;

                    if (!ballPottedThisTurn) {
                        // Tidak ada bola masuk = ganti giliran
                        // Giliran pindah dari lastShooter ke pemain lawan
                        nextPlayer = (lastShooter == 1) ? 2 : 1;
                        System.out.println("[TURN] No ball potted, switching turn from Player " + lastShooter + " to Player " + nextPlayer);
                    } else {
                        // Ada bola masuk = pemain yang sama (lastShooter) main lagi
                        nextPlayer = lastShooter;
                        System.out.println("[TURN] Ball potted! Player " + lastShooter + " gets another turn");
                    }

                    // Update currentPlayer untuk turn berikutnya
                    currentPlayer = nextPlayer;
                    System.out.println("[TURN] Next turn: Player " + currentPlayer);

                    // Kalau Host dan Online, kirim data terbaru ke Client
                    if (isOnline && myPlayerId == 1 && networkManager != null) {
                        System.out.println("[HOST] Sending turn sync: P1=" + p1Score + " P2=" + p2Score + " NextTurn=" + currentPlayer);
                        networkManager.sendState(p1Score, p2Score, currentPlayer);

                        // Update visual Host sendiri
                        isMyTurn = (currentPlayer == myPlayerId);
                        System.out.println("[HOST] My turn status: " + isMyTurn);
                    }

                    // Kalau Lokal (tidak online)
                    if (!isOnline) {
                        isMyTurn = true; // Lokal selalu bisa main
                    }
                }

                // FIX: Reset flag bola masuk SETELAH semua logic selesai
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

            // --- LOGIKA RESPAWN (DIPERBAIKI) ---
            if (cueBall.isPendingRespawn()) {
                if (!anyMoving) {
                    System.out.println("[RESPAWN] Respawning cue ball...");
                    cueBall.setPosition(new Vector2D(GAME_WIDTH / 4.0, GAME_HEIGHT / 2.0));
                    cueBall.setVelocity(new Vector2D(0, 0));
                    cueBall.setPendingRespawn(false);
                    cueBall.setActive(true);

                    // Host kabari client posisi bola putih reset
                    if(isOnline && myPlayerId == 1 && networkManager != null) {
                        System.out.println("[HOST] Sending respawn sync");
                        networkManager.sendState(p1Score, p2Score, currentPlayer);
                    }
                }
            }

            drawHUD();
        }

        private void drawHUD() {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 20));

            String p1Text = "P1: " + p1Score + (myPlayerId == 1 ? " (YOU)" : "");
            gc.fillText(p1Text, 30, 40);

            String p2Text = "P2: " + p2Score + (myPlayerId == 2 ? " (YOU)" : "");
            gc.fillText(p2Text, GAME_WIDTH - 150, 40);

            gc.setFill(Color.YELLOW);
            String turnText;
            if (isOnline) {
                // Tampilkan siapa yang jalan
                if (isMyTurn) turnText = "YOUR TURN";
                else turnText = "OPPONENT TURN";
            } else {
                turnText = "PLAYER " + currentPlayer + " TURN";
            }
            gc.fillText(turnText, GAME_WIDTH/2.0 - 60, 40);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}