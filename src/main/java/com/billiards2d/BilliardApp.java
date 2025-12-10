package com.billiards2d;

import com.billiards2d.core.GameBus;
import com.billiards2d.net.NetworkManager;
import com.billiards2d.rules.RuleEngine;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BilliardApp extends Application {

    public static final double TOP_HEADER_HEIGHT = 80;
    public static final double TABLE_WIDTH = 1100;
    public static final double TABLE_HEIGHT = 550;
    public static final double SIDEBAR_WIDTH = 80;
    public static final double BOTTOM_HEIGHT = 50;

    private static final double APP_WIDTH = TABLE_WIDTH + SIDEBAR_WIDTH;
    private static final double APP_HEIGHT = TOP_HEADER_HEIGHT + TABLE_HEIGHT + BOTTOM_HEIGHT;
    private static final double TABLE_Y_OFFSET = TOP_HEADER_HEIGHT;

    private GraphicsContext gc;
    private final List<GameObject> gameObjects = new ArrayList<>();
    private CueStick cueStick;
    private CueBall cueBall;

    private RuleEngine ruleEngine;
    private NetworkManager networkManager;

    private boolean ballsMoving = false;
    private boolean isDraggingCueBall = false;
    private String lastLogMessage = "Welcome to 8-Ball Pro";

    private static String modeArgs = "local";
    private static String clientHost = "localhost";
    private static int clientPort = 5000;
    private boolean isMultiplayer = false;
    private boolean isPractice = false;

    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(APP_WIDTH, APP_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        StackPane uiLayer = new StackPane();
        uiLayer.setPickOnBounds(false);
        uiLayer.setAlignment(Pos.TOP_LEFT);

        HBox menuContainer = createHamburgerMenu();
        uiLayer.getChildren().add(menuContainer);

        StackPane root = new StackPane(canvas, uiLayer);
        root.setStyle("-fx-background-color: #121212;");

        Scene scene = new Scene(root, APP_WIDTH, APP_HEIGHT);
        injectStyles(scene);

        initializeGameObjects();
        initializeSystems();
        setupInputHandlers(scene);

        primaryStage.setTitle("Billiards-2D: Ultimate Edition");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        startGameLoop();
    }

    private HBox createHamburgerMenu() {
        HBox container = new HBox();
        container.setPadding(new Insets(15));
        container.setPickOnBounds(false);

        MenuButton hamburger = new MenuButton("☰");
        hamburger.getStyleClass().add("hamburger-button");

        MenuItem itemPractice = new MenuItem("Practice Mode");
        itemPractice.setOnAction(e -> switchMode("practice"));

        MenuItem itemLocal = new MenuItem("Local PvP (2 Player)");
        itemLocal.setOnAction(e -> switchMode("local"));

        Menu menuOnline = new Menu("Online Multiplayer");
        MenuItem itemHost = new MenuItem("Host Game");
        itemHost.setOnAction(e -> switchMode("server"));
        MenuItem itemJoin = new MenuItem("Join Game");
        itemJoin.setOnAction(e -> showJoinDialog());
        menuOnline.getItems().addAll(itemHost, itemJoin);

        MenuItem itemExit = new MenuItem("Exit");
        itemExit.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });

        hamburger.getItems().addAll(itemPractice, itemLocal, new SeparatorMenuItem(), menuOnline, new SeparatorMenuItem(), itemExit);

        container.getChildren().add(hamburger);
        return container;
    }

    private void injectStyles(Scene scene) {
        // --- CRITICAL VISUAL FIX ---
        // 1. Force Label Color White
        // 2. Kill Arrow Region entirely
        String css = "data:text/css," +
                ".hamburger-button { " +
                "    -fx-background-color: transparent; " +
                "    -fx-font-size: 32px; " +
                "    -fx-cursor: hand; " +
                "    -fx-padding: 0 10 0 10; " +
                "}" +
                // TARGETING INNER LABEL FOR COLOR FIX
                ".hamburger-button > .label { " +
                "    -fx-text-fill: white; " +
                "}" +
                ".hamburger-button:hover { " +
                "    -fx-background-color: rgba(255,255,255,0.1); " +
                "    -fx-background-radius: 5px; " +
                "}" +
                ".hamburger-button:pressed { " +
                "    -fx-background-color: rgba(255,255,255,0.2); " +
                "}" +
                // REMOVE ARROW
                ".hamburger-button > .arrow-button { " +
                "    -fx-padding: 0; " +
                "    -fx-background-color: transparent; " +
                "    -fx-pref-width: 0; " +
                "}" +
                ".hamburger-button > .arrow-button > .arrow { " +
                "    -fx-padding: 0; " +
                "    -fx-shape: null; " +
                "}" +
                // MENU DROPDOWN STYLING
                ".context-menu { " +
                "    -fx-background-color: #1a1a1a; " +
                "    -fx-border-color: #444; " +
                "    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0); " +
                "}" +
                ".menu-item > .label { " +
                "    -fx-text-fill: #e0e0e0; " +
                "    -fx-padding: 5 20 5 5; " +
                "}" +
                ".menu-item:focused { " +
                "    -fx-background-color: #333; " +
                "}" +
                ".menu-item:pressed { " +
                "    -fx-background-color: #0096C9; " +
                "    -fx-text-fill: white; " +
                "}";
        scene.getStylesheets().add(css);
    }

    private void switchMode(String newMode) {
        modeArgs = newMode;
        resetGame();
    }

    private void setupInputHandlers(Scene scene) {
        scene.setOnMouseMoved(e -> {
            MouseEvent shifted = shiftEvent(e);
            if (canPlayerMoveBall() && isHoveringCueBall(shifted)) {
                scene.setCursor(Cursor.HAND);
            } else {
                scene.setCursor(Cursor.DEFAULT);
            }
        });

        scene.setOnMousePressed(e -> {
            if (e.getY() < TABLE_Y_OFFSET) return;

            if (canPlayerShoot()) {
                MouseEvent shifted = shiftEvent(e);
                if (canPlayerMoveBall() && isHoveringCueBall(shifted)) {
                    isDraggingCueBall = true;
                    cueBall.setVelocity(new Vector2D(0, 0));
                } else if (!isDraggingCueBall) {
                    cueStick.handleMousePressed(shifted);
                }
            }
        });

        scene.setOnMouseDragged(e -> {
            if (e.getY() >= TABLE_Y_OFFSET && canPlayerShoot()) {
                MouseEvent shifted = shiftEvent(e);
                if (isDraggingCueBall) {
                    moveCueBallTo(shifted.getX(), shifted.getY());
                } else {
                    cueStick.handleMouseDragged(shifted);
                }
            }
        });

        scene.setOnMouseReleased(e -> {
            MouseEvent shifted = shiftEvent(e);
            if (isDraggingCueBall) {
                isDraggingCueBall = false;
            } else if (e.getY() >= TABLE_Y_OFFSET && canPlayerShoot()) {
                cueStick.handleMouseReleased(shifted);
            }
        });
    }

    private MouseEvent shiftEvent(MouseEvent e) {
        return new MouseEvent(
                e.getSource(), e.getTarget(), e.getEventType(),
                e.getX(), e.getY() - TABLE_Y_OFFSET,
                e.getScreenX(), e.getScreenY(),
                e.getButton(), e.getClickCount(), e.isShiftDown(), e.isControlDown(),
                e.isAltDown(), e.isMetaDown(), e.isPrimaryButtonDown(), e.isMiddleButtonDown(),
                e.isSecondaryButtonDown(), e.isSynthesized(), e.isPopupTrigger(), e.isStillSincePress(), null
        );
    }

    private void initializeGameObjects() {
        gameObjects.clear();
        Table table = new Table(TABLE_WIDTH, TABLE_HEIGHT);
        gameObjects.add(table);

        double startX = TABLE_WIDTH * 0.75;
        double startY = TABLE_HEIGHT / 2;
        double d = 30;
        double rowX = d * Math.cos(Math.toRadians(30));

        gameObjects.add(new ObjectBall(new Vector2D(startX, startY), 1));

        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX, startY - 16), 9));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX, startY + 16), 2));

        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*2, startY - 32), 10));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*2, startY), 8));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*2, startY + 32), 3));

        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*3, startY - 48), 11));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*3, startY - 16), 7));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*3, startY + 16), 14));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*3, startY + 48), 4));

        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*4, startY - 64), 5));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*4, startY - 32), 13));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*4, startY), 15));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*4, startY + 32), 6));
        gameObjects.add(new ObjectBall(new Vector2D(startX + rowX*4, startY + 64), 12));

        cueBall = new CueBall(new Vector2D(TABLE_WIDTH * 0.25, startY));
        gameObjects.add(cueBall);

        cueStick = new CueStick(cueBall, gameObjects);
        gameObjects.add(cueStick);
        gameObjects.add(new PhysicsEngine(table, gameObjects));
    }

    private void initializeSystems() {
        ruleEngine = new RuleEngine();

        GameBus.subscribe(GameBus.EventType.GAME_STATE_CHANGE, msg -> lastLogMessage = (String) msg);
        GameBus.subscribe(GameBus.EventType.REMOTE_SHOT, payload -> {
            Vector2D force = (Vector2D) payload;
            cueBall.hit(force);
        });

        isMultiplayer = false;
        isPractice = false;

        if (modeArgs.equalsIgnoreCase("server")) {
            isMultiplayer = true;
            lastLogMessage = "Server Mode: Waiting...";
            networkManager = new NetworkManager();
            networkManager.startServer(5000);
        } else if (modeArgs.equalsIgnoreCase("client")) {
            isMultiplayer = true;
            lastLogMessage = "Connecting to " + clientHost + ":" + clientPort + "...";
            networkManager = new NetworkManager();
            networkManager.connectClient(clientHost, clientPort);
        } else if (modeArgs.equalsIgnoreCase("practice")) {
            isPractice = true;
            lastLogMessage = "Practice Mode";
        } else {
            lastLogMessage = "Local PvP Mode";
        }
    }

    private void startGameLoop() {
        new AnimationTimer() {
            private long lastNanoTime = System.nanoTime();
            @Override
            public void handle(long currentNanoTime) {
                double deltaTime = (currentNanoTime - lastNanoTime) / 1_000_000_000.0;
                lastNanoTime = currentNanoTime;
                if (deltaTime > 0.05) deltaTime = 0.05;
                updateAndDraw(deltaTime);
                checkTurnEnd();
            }
        }.start();
    }

    private void updateAndDraw(double deltaTime) {
        if (!cueBall.isActive() && ruleEngine.isBallInHand()) {
            cueBall.setPosition(new Vector2D(TABLE_WIDTH * 0.25, TABLE_HEIGHT / 2));
            cueBall.setVelocity(new Vector2D(0, 0));
            cueBall.setActive(true);
        }

        for (GameObject obj : gameObjects) obj.update(deltaTime);

        gc.setFill(Color.rgb(18, 18, 18));
        gc.fillRect(0, 0, APP_WIDTH, APP_HEIGHT);

        drawHUD();

        gc.save();
        gc.translate(0, TABLE_Y_OFFSET);
        gc.beginPath();
        gc.rect(0, 0, TABLE_WIDTH, TABLE_HEIGHT);
        gc.clip();

        for (GameObject obj : gameObjects) {
            if (obj instanceof CueStick) {
                if (!isDraggingCueBall && !ballsMoving && canPlayerShoot()) {
                    obj.draw(gc);
                }
            } else {
                obj.draw(gc);
            }
        }
        gc.restore();

        drawSidebar();
        drawFooter();
    }

    private void checkTurnEnd() {
        boolean anyMoving = gameObjects.stream()
                .filter(o -> o instanceof Ball)
                .anyMatch(b -> ((Ball) b).getVelocity().length() > 0.1);

        if (ballsMoving && !anyMoving) {
            ballsMoving = false;
            GameBus.publish(GameBus.EventType.TURN_ENDED, null);
        } else {
            ballsMoving = anyMoving;
        }
    }

    private boolean canPlayerShoot() {
        if (ballsMoving) return false;
        if (ruleEngine.isGameOver()) return false;
        if (isPractice) return true;

        if (isMultiplayer) {
            boolean isServer = modeArgs.equalsIgnoreCase("server");
            int myPlayerId = isServer ? 1 : 2;
            if (ruleEngine.getCurrentPlayer() != myPlayerId) return false;
        }
        return true;
    }

    private boolean canPlayerMoveBall() {
        return canPlayerShoot() && ruleEngine.isBallInHand();
    }

    private void drawHUD() {
        double p1X = 100;
        double p2X = APP_WIDTH - 550;
        double y = 10;

        boolean p1Solids = true;
        String p1Type = "OPEN";
        String p2Type = "OPEN";

        if (!ruleEngine.isTableOpen()) {
            p1Solids = ruleEngine.isPlayer1Solids();
            p1Type = p1Solids ? "SOLID" : "STRIPE";
            p2Type = !p1Solids ? "SOLID" : "STRIPE";
        }

        String p1Name = "PLAYER 1";
        String p2Name = "PLAYER 2";

        if (isPractice) {
            p2Name = "TRAINING";
        } else if (isMultiplayer) {
            boolean isServer = modeArgs.equalsIgnoreCase("server");
            if (isServer) { p1Name = "YOU (HOST)"; p2Name = "OPPONENT"; }
            else { p1Name = "OPPONENT"; p2Name = "YOU (CLIENT)"; }
        }

        boolean p1Turn = ruleEngine.getCurrentPlayer() == 1;
        drawPlayerBar(p1X, y, p1Name, p1Type, p1Solids, p1Turn);
        drawPlayerBar(p2X, y, p2Name, p2Type, !p1Solids, !p1Turn && !isPractice);
    }

    private void drawPlayerBar(double x, double y, String name, String typeStr, boolean isSolid, boolean isTurn) {
        double w = 450;
        double h = 60;

        if (isTurn) {
            gc.setStroke(Color.rgb(0, 255, 100));
            gc.setLineWidth(2);
        } else {
            gc.setStroke(Color.rgb(60, 60, 60));
            gc.setLineWidth(1);
        }

        LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(40, 40, 45)), new Stop(1, Color.rgb(20, 20, 25)));
        gc.setFill(bg);
        gc.fillRoundRect(x, y, w, h, 10, 10);
        gc.strokeRoundRect(x, y, w, h, 10, 10);

        gc.setFill(isTurn ? Color.LIME : Color.GRAY);
        gc.fillOval(x + 10, y + 15, 30, 30);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(name, x + 50, y + 10);

        gc.setFont(Font.font("Arial", 12));
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText(typeStr, x + 50, y + 35);

        double ballX = x + 150;
        double ballY = y + 30;
        double gap = 22;

        if (ruleEngine.isTableOpen()) {
            gc.setFill(Color.GOLD);
            gc.fillText("TABLE OPEN", ballX, ballY + 5);
        } else {
            int start = isSolid ? 1 : 9;
            int end = isSolid ? 7 : 15;
            for (int i = start; i <= end; i++) {
                Ball.renderVisual(gc, ballX + ((i - start) * gap), ballY, 9, i, ObjectBall.getColorForNumber(i), isBallOnTable(i));
            }
            Ball.renderVisual(gc, ballX + (7 * gap) + 5, ballY, 9, 8, Color.BLACK, isBallOnTable(8));
        }
    }

    private void drawSidebar() {
        double startX = TABLE_WIDTH;
        gc.setFill(Color.rgb(20, 20, 20));
        gc.fillRect(startX, TABLE_Y_OFFSET, SIDEBAR_WIDTH, TABLE_HEIGHT);

        double barW = 20;
        double barH = TABLE_HEIGHT - 100;
        double barX = startX + (SIDEBAR_WIDTH - barW) / 2;
        double barY = TABLE_Y_OFFSET + 50;

        gc.setStroke(Color.GRAY);
        gc.strokeRect(barX, barY, barW, barH);

        double powerRatio = cueStick.getCurrentPower() / cueStick.getMaxPower();
        if (powerRatio > 0) {
            double fillH = powerRatio * barH;
            LinearGradient grad = new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.LIME), new Stop(0.5, Color.YELLOW), new Stop(1, Color.RED));
            gc.setFill(grad);
            gc.fillRect(barX + 1, barY + barH - fillH, barW - 2, fillH);
        }

        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("PWR", startX + SIDEBAR_WIDTH / 2, barY + barH + 20);
    }

    private void drawFooter() {
        double y = TABLE_Y_OFFSET + TABLE_HEIGHT;
        gc.setFill(Color.rgb(25, 25, 30));
        gc.fillRect(0, y, APP_WIDTH, BOTTOM_HEIGHT);
        gc.setStroke(Color.rgb(50, 50, 50));
        gc.strokeLine(0, y, APP_WIDTH, y);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Font.font("Consolas", 14));

        if (ruleEngine.isGameOver()) {
            gc.setFill(Color.RED);
            gc.fillText(ruleEngine.getWinMessage(), 20, y + BOTTOM_HEIGHT / 2);
        } else {
            gc.setFill(Color.CYAN);
            gc.fillText("> " + lastLogMessage, 20, y + BOTTOM_HEIGHT / 2);
        }
    }

    private boolean isBallOnTable(int number) {
        for (GameObject obj : gameObjects) {
            if (obj instanceof ObjectBall ob && ob.getNumber() == number) return ob.isActive();
        }
        return false;
    }

    private boolean isHoveringCueBall(MouseEvent e) {
        if (!cueBall.isActive()) return false;
        double dist = cueBall.getPosition().subtract(new Vector2D(e.getX(), e.getY())).length();
        return dist < cueBall.getRadius() * 2.5;
    }

    private void moveCueBallTo(double x, double y) {
        double r = cueBall.getRadius();
        double rail = 55;
        x = Math.max(rail + r, Math.min(TABLE_WIDTH - rail - r, x));
        y = Math.max(rail + r, Math.min(TABLE_HEIGHT - rail - r, y));

        boolean collision = false;
        Vector2D newPos = new Vector2D(x, y);

        for (GameObject obj : gameObjects) {
            if (obj instanceof ObjectBall ob && ob.isActive()) {
                if (newPos.subtract(ob.getPosition()).length() < r * 2.1) {
                    collision = true;
                    break;
                }
            }
        }

        if (!collision) {
            cueBall.setPosition(newPos);
            cueBall.setVelocity(new Vector2D(0, 0));
        }
    }

    private void showJoinDialog() {
        TextInputDialog dialog = new TextInputDialog("0.tcp.ap.ngrok.io:12345");
        dialog.setTitle("Join Multiplayer");
        dialog.setHeaderText("Enter Host Address");
        dialog.setContentText("Address:");
        dialog.showAndWait().ifPresent(addr -> {
            try {
                String[] parts = addr.split(":");
                clientHost = parts[0];
                clientPort = Integer.parseInt(parts[1]);
                switchMode("client");
            } catch (Exception e) { System.out.println("Invalid Address"); }
        });
    }

    private void resetGame() {
        initializeGameObjects();
        initializeSystems();
        ballsMoving = false;
        isDraggingCueBall = false;
        lastLogMessage = "Mode: " + modeArgs.toUpperCase();
    }

    public static void main(String[] args) {
        if (args.length > 0) modeArgs = args[0];
        launch(args);
    }
}