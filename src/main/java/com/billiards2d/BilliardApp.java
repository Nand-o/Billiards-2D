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

public class BilliardApp extends Application {

    private static final int GAME_WIDTH = 800; // Ukuran area main
    private static final int GAME_HEIGHT = 450;

    private GraphicsContext gc;
    private final List<GameObject> gameObjects = new ArrayList<>();

    private Table table; // perlu akses field ini
    private CueStick cueStick; // global field cuestick
    private CueBall cueBall; // perlu akses untuk HUD

    // GUI Debug vars
    private double mouseX, mouseY;

    @Override
    public void start(Stage primaryStage) {
        // Init table dulu buat hitung ukuran window total
        table = new Table(GAME_WIDTH, GAME_HEIGHT);
        double totalW = GAME_WIDTH + (table.getWallThickness() * 2);
        double totalH = GAME_HEIGHT + (table.getWallThickness() * 2);

        Canvas canvas = new Canvas(totalW, totalH);
        gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);
        root.setStyle("-fx-background-color: #222;"); // Background gelap

        // capture input handler
        double offset = table.getWallThickness();

        canvas.setOnMouseMoved(e -> {
            mouseX = e.getX() - offset;
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

        primaryStage.setTitle("Billiard Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        initializeGameObjects();

        GameLoop gameLoop = new GameLoop();
        gameLoop.start();
    }

    // Helper buat geser koordinat mouse
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

    private void initializeGameObjects() {
        // Table sudah di-init di start()
        cueBall = new CueBall(new Vector2D(GAME_WIDTH/4, GAME_HEIGHT/2));
        ObjectBall ball1 = new ObjectBall(new Vector2D(GAME_WIDTH*0.75, GAME_HEIGHT/2), "RED");
        ObjectBall ball2 = new ObjectBall(new Vector2D(GAME_WIDTH*0.75 + 25, GAME_HEIGHT/2 - 10), "BLUE");

        List<Ball> allBalls = new ArrayList<>();
        allBalls.add(cueBall);
        allBalls.add(ball1);
        allBalls.add(ball2);

        // Add Cue Stick to visualize gameplay control
        this.cueStick = new CueStick(cueBall, allBalls, GAME_WIDTH, GAME_HEIGHT);

        // agar layering (tumpukan) gambar benar.
        gameObjects.add(cueBall);
        gameObjects.add(ball1);
        gameObjects.add(ball2);

        // Add physic engine to game loop
        PhysicsEngine physicsEngine = new PhysicsEngine(table, gameObjects);
        gameObjects.addAll(allBalls);
        gameObjects.add(physicsEngine);
    }

    private class GameLoop extends AnimationTimer {
        private long lastNanoTime = System.nanoTime();

        @Override
        public void handle(long currentNanoTime) {
            double deltaTime = (currentNanoTime - lastNanoTime) / 1_000_000_000.0;
            lastNanoTime = currentNanoTime;

            // Safety Cap
            if (deltaTime > 0.05) deltaTime = 0.05;

            // --- UPDATE LOGIC DENGAN SUB-STEPPING ---
            int subSteps = 4;
            double subDeltaTime = deltaTime/ subSteps;

            for (int step = 0; step < subSteps; step++) {
                // Update semua game object sedikit demi sedikit
                for (GameObject obj : gameObjects) {
                    obj.update(subDeltaTime);
                }
            }
            cueStick.update(deltaTime);

            // --- RENDER LOGIC ---
            gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

            // 1. Gambar Meja (Layer Bawah)
            table.draw(gc);

            // 2. Gambar Game Objects (Layer Tengah - Perlu Geser/Translate)
            gc.save();
            gc.translate(table.getWallThickness(), table.getWallThickness());

            for (GameObject obj : gameObjects) {
                // PhysicsEngine gak punya draw, Table udah digambar manual
                // Jadi cuma gambar Ball
                if(obj instanceof Ball) obj.draw(gc);
            }

            // Gambar Stik paling atas di layer game
            cueStick.draw(gc);

            gc.restore();

            // 3. Gambar HUD (Overlay)
            drawHUD();
        }

        private void drawHUD() {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", 14));
            gc.fillText(String.format("Mouse: (%.0f, %.0f)", mouseX, mouseY), 20, 30);
            double speed = cueBall.getVelocity().length();
            gc.fillText(String.format("Power: %.2f", speed), 20, 50);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}