package com.billiards2d.ui;

import com.billiards2d.core.GameObject;
import com.billiards2d.game.GameRules;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.List;
import java.util.prefs.Preferences;

import static com.billiards2d.core.GameConstants.*;

/**
 * Mengelola pembuatan Scene dan transisi antar-tampilan pada aplikasi
 * JavaFX untuk permainan biliar.
 * <p>
 * Tanggung jawab utama mencakup pembuatan menu utama, setup scene permainan,
 * serta overlay seperti pause dan game-over. Pemisahan ini dilakukan agar
 * `BilliardApp` tetap ringan dan fokus pada lifecycle aplikasi.
 * </p>
 *
 * @since 2025-12-13
 */
public class SceneManager {
    
    private final Stage primaryStage;
    private final Preferences prefs;
    
    // Callbacks for scene transitions
    private java.util.function.Consumer<Boolean> onStartGame;
    private Runnable onRestartGame;
    private Runnable onReturnToMenu;
    private Runnable onTogglePause;
    
    /**
     * Constructor for SceneManager
     */
    /**
     * Constructor for SceneManager.
     *
     * @param primaryStage utama `Stage` dari aplikasi
     * @param prefs `Preferences` untuk menyimpan konfigurasi seperti high score
     */
    public SceneManager(Stage primaryStage, Preferences prefs) {
        this.primaryStage = primaryStage;
        this.prefs = prefs;
    }
    
    /**
     * Set callbacks used by menu/overlays to communicate with game logic.
     *
     * @param onStartGame callback accepting a boolean: true for 8-ball, false for arcade
     * @param onRestartGame callback to restart the current game
     * @param onReturnToMenu callback to return to main menu
     * @param onTogglePause callback to toggle pause state
     */
    public void setCallbacks(java.util.function.Consumer<Boolean> onStartGame, Runnable onRestartGame, Runnable onReturnToMenu, Runnable onTogglePause) {
        this.onStartGame = onStartGame;
        this.onRestartGame = onRestartGame;
        this.onReturnToMenu = onReturnToMenu;
        this.onTogglePause = onTogglePause;
    }
    
    /**
     * Create and display the main menu scene. Stops the provided game loop if non-null.
     *
     * @param currentGameLoop the currently running AnimationTimer for the game; may be null
     */
    public void showMainMenu(AnimationTimer currentGameLoop) {
        // Stop current game loop if running
        if (currentGameLoop != null) {
            currentGameLoop.stop();
        }

        // 1. ROOT CONTAINER (StackPane)
        StackPane root = new StackPane();

        // --- LAYER 1: BACKGROUND IMAGE ---
        try {
            // Load background image from resources
            Image bgImage = new Image(getClass().getResourceAsStream(ASSET_MENU_BACKGROUND));
            ImageView bgView = new ImageView(bgImage);

            // Scaling logic untuk cover mode
            bgView.fitWidthProperty().bind(primaryStage.widthProperty());
            bgView.fitHeightProperty().bind(primaryStage.heightProperty());

            root.getChildren().add(bgView);
        } catch (Exception e) {
            // Fallback jika gambar tidak ditemukan
            Canvas bgFallback = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
            GraphicsContext gc = bgFallback.getGraphicsContext2D();
            gc.setFill(Color.rgb(0, 40, 0)); // Dark green
            gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            drawGridPattern(gc);
            root.getChildren().add(bgFallback);
            System.err.println("Background image not found, using fallback. Error: " + e.getMessage());
        }

        // --- LAYER 2: UI LAYOUT (BorderPane) ---
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20, 40, 20, 40));

        // A. TOP: HUD (1UP, HI-SCORE, 2UP)
        HBox topHud = new HBox();
        topHud.setAlignment(Pos.CENTER);

        // Load High Score from Preferences
        String highScoreStr = String.format("%06d", prefs.getInt("arcade_highscore", 0));

        VBox p1Box = createHudBox("1UP", "000000");
        VBox hiScoreBox = createHudBox("HI-SCORE", highScoreStr);
        VBox p2Box = createHudBox("2UP", "000000");

        Region spacer1 = new Region(); HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region(); HBox.setHgrow(spacer2, Priority.ALWAYS);

        topHud.getChildren().addAll(p1Box, spacer1, hiScoreBox, spacer2, p2Box);
        mainLayout.setTop(topHud);

        // B. CENTER: TITLE & BUTTONS
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);

        // Main title
        Text title = new Text("BILLIARD 2D");
        title.setFont(loadCustomFont("ArcadeClassic.ttf", 90, "Impact"));

        LinearGradient titleGradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#ff4e00")),
                new Stop(0.5, Color.web("#ffcc00")),
                new Stop(1.0, Color.web("#ffff00"))
        );
        title.setFill(titleGradient);
        title.setStroke(Color.BLACK);
        title.setStrokeWidth(3);

        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.BLACK);
        titleShadow.setOffsetX(5);
        titleShadow.setOffsetY(5);
        titleShadow.setRadius(0);
        title.setEffect(titleShadow);

        // Subtitle
        Text subTitle = new Text("ULTIMATE ARCADE EXPERIENCE");
        subTitle.setFont(loadCustomFont("PixelOperator-Bold.ttf", 20, "Consolas"));
        subTitle.setFill(Color.web("#dca466"));
        subTitle.setEffect(new DropShadow(2, Color.BLACK));

        Region titleSpacer = new Region();
        titleSpacer.setPrefHeight(40);

        // Buttons
        Button btn8Ball = createRetroButton("PLAY 8-BALL (2 Player)", () -> {
            if (onStartGame != null) onStartGame.accept(true);
        });
        Button btnArcade = createRetroButton("ARCADE RUSH (1 Player)", () -> {
            if (onStartGame != null) onStartGame.accept(false);
        });
        Button btnExit = createRetroButton("EXIT GAME", () -> primaryStage.close());

        centerBox.getChildren().addAll(title, subTitle, titleSpacer, btn8Ball, btnArcade, btnExit);
        mainLayout.setCenter(centerBox);

        // C. BOTTOM: CREDIT & COPYRIGHT
        VBox bottomBox = new VBox(5);
        bottomBox.setAlignment(Pos.CENTER);

        Text creditText = new Text("CREDIT 001");
        creditText.setFont(loadCustomFont("VCR_OSD_MONO_1.001.ttf", 24, "Courier New"));
        creditText.setFill(Color.WHITE);
        creditText.setEffect(new DropShadow(2, Color.BLACK));

        Text copyText = new Text("© 2025 BILLIARD2D PROJECT. CREATED WITH JAVAFX.");
        copyText.setFont(loadCustomFont("PixelOperator-Bold.ttf", 12, "Consolas"));
        copyText.setFill(Color.WHITE);

        bottomBox.getChildren().addAll(creditText, copyText);
        mainLayout.setBottom(bottomBox);

        root.getChildren().add(mainLayout);

        // --- LAYER 3: CRT SCANLINE EFFECT ---
        Canvas crtCanvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        GraphicsContext crtGC = crtCanvas.getGraphicsContext2D();
        crtGC.setFill(Color.rgb(0, 0, 0, 0.15));
        for (int y = 0; y < WINDOW_HEIGHT; y += 4) {
            crtGC.fillRect(0, y, WINDOW_WIDTH, 2);
        }
        crtCanvas.setMouseTransparent(true);
        root.getChildren().add(crtCanvas);

        // Set scene
        Scene menuScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setTitle("Billiard 2D - Main Menu");
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }
    
    /**
     * Create the pause overlay UI component.
     *
     * @return a VBox that can be added as an overlay and toggled visible when paused
     */
    public VBox createPauseOverlay() {
        VBox overlay = new VBox(25);
        overlay.setAlignment(Pos.CENTER);
        overlay.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.85), CornerRadii.EMPTY, Insets.EMPTY
        )));
        overlay.setVisible(false);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Fill entire parent

        Text pauseTitle = new Text("PAUSED");
        pauseTitle.setFont(loadCustomFont("ArcadeClassic.ttf", 80, "Impact"));
        pauseTitle.setFill(Color.YELLOW);
        pauseTitle.setStroke(Color.BLACK);
        pauseTitle.setStrokeWidth(3);
        pauseTitle.setEffect(new DropShadow(5, Color.BLACK));

        Text pauseMsg = new Text("Press ESC to Resume");
        pauseMsg.setFont(loadCustomFont("PixelOperator-Bold.ttf", 24, "Consolas"));
        pauseMsg.setFill(Color.WHITE);

        Button btnResume = createRetroButton("RESUME", () -> {
            if (onTogglePause != null) onTogglePause.run();
        });
        Button btnMenu = createRetroButton("MAIN MENU", () -> {
            if (onReturnToMenu != null) onReturnToMenu.run();
        });

        overlay.getChildren().addAll(pauseTitle, pauseMsg, btnResume, btnMenu);
        return overlay;
    }
    
    /**
     * Create the game-over overlay components including title and message nodes.
     *
     * @return a GameOverComponents DTO containing overlay nodes
     */
    public GameOverComponents createGameOverOverlay() {
        VBox overlay = new VBox(20);
        overlay.setAlignment(Pos.CENTER);
        overlay.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.9), CornerRadii.EMPTY, Insets.EMPTY
        )));
        overlay.setVisible(false);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Fill entire parent

        Text gameOverTitle = new Text("GAME OVER");
        gameOverTitle.setFont(loadCustomFont("ArcadeClassic.ttf", 70, "Impact"));
        gameOverTitle.setFill(Color.RED);
        gameOverTitle.setStroke(Color.BLACK);
        gameOverTitle.setStrokeWidth(3);
        gameOverTitle.setEffect(new DropShadow(5, Color.BLACK));

        Text gameOverMessage = new Text("");
        gameOverMessage.setFont(loadCustomFont("PixelOperator-Bold.ttf", 22, "Consolas"));
        gameOverMessage.setFill(Color.WHITE);
        gameOverMessage.setEffect(new DropShadow(3, Color.BLACK));

        Button btnRestart = createRetroButton("PLAY AGAIN", () -> {
            if (onRestartGame != null) onRestartGame.run();
        });
        Button btnMenu = createRetroButton("MAIN MENU", () -> {
            if (onReturnToMenu != null) onReturnToMenu.run();
        });

        overlay.getChildren().addAll(gameOverTitle, gameOverMessage, btnRestart, btnMenu);
        
        return new GameOverComponents(overlay, gameOverTitle, gameOverMessage);
    }
    
    // ==================== HELPER METHODS ====================
    
    private VBox createHudBox(String label, String value) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);

        Text lbl = new Text(label);
        lbl.setFont(loadCustomFont("ArcadeClassic.ttf", 18, "Impact"));
        lbl.setFill(Color.CYAN);
        lbl.setStroke(Color.BLACK);
        lbl.setStrokeWidth(1);

        Text val = new Text(value);
        val.setFont(loadCustomFont("VCR_OSD_MONO_1.001.ttf", 20, "Courier New"));
        val.setFill(Color.WHITE);
        val.setEffect(new DropShadow(2, Color.BLACK));

        box.getChildren().addAll(lbl, val);
        return box;
    }
    
    private Button createRetroButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setFont(loadCustomFont("PixelOperator-Bold.ttf", 22, "Consolas"));

        btn.setPrefWidth(400);
        btn.setPrefHeight(55);

        String normalStyle =
                "-fx-background-color: linear-gradient(to bottom, #ffcc00, #ff9900); " +
                        "-fx-text-fill: #3e2723; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #3e2723; " +
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 15; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 0, 0, 4, 4);";

        String hoverStyle =
                "-fx-background-color: linear-gradient(to bottom, #ffeb3b, #ffc107); " +
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
    
    private Font loadCustomFont(String fontFileName, double size, String fallbackFontName) {
        try {
            String path = "/assets/" + fontFileName;
            Font font = Font.loadFont(getClass().getResourceAsStream(path), size);

            if (font != null) {
                return font;
            }
        } catch (Exception e) {
            // Silent fail, use fallback
        }

        return Font.font(fallbackFontName, FontWeight.BOLD, size);
    }
    
    private void drawGridPattern(GraphicsContext gc) {
        gc.setStroke(Color.rgb(0, 100, 0));
        gc.setLineWidth(2);

        int step = 60;
        for (int x = 0; x < WINDOW_WIDTH; x += step) 
            gc.strokeLine(x, 0, x, WINDOW_HEIGHT);
        for (int y = 0; y < WINDOW_HEIGHT; y += step) 
            gc.strokeLine(0, y, WINDOW_WIDTH, y);
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * DTO class to return game over overlay components
     */
    public static class GameOverComponents {
        /** Overlay container yang menutup canvas saat game over. */
        public final VBox overlay;

        /** Judul overlay (mis. "YOU WIN!" / "GAME OVER"). */
        public final Text title;

        /** Pesan tambahan atau detail skor pada overlay. */
        public final Text message;
        
        /**
         * Buat DTO overlay game-over.
         *
         * @param overlay container overlay yang akan ditampilkan
         * @param title node teks judul overlay
         * @param message node teks pesan detail / skor
         */
        public GameOverComponents(VBox overlay, Text title, Text message) {
            this.overlay = overlay;
            this.title = title;
            this.message = message;
        }
    }
}
