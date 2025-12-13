package com.billiards2d;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.util.List;

import static com.billiards2d.GameConstants.*;

/**
 * Handles all user input (mouse and keyboard events) for the billiard game.
 * Extracted from BilliardApp to follow Single Responsibility Principle.
 */
public class InputHandler {
    
    // Dependencies
    private CueStick cueStick;
    private CueBall cueBall;
    private GameRules gameRules;
    private List<GameObject> gameObjects;
    private boolean is8BallMode;
    
    // Callback interfaces for actions that need to affect BilliardApp state
    private Runnable onTogglePause;
    private Runnable onDebugClearTable;
    
    // Offsets for coordinate conversion
    private double currentOffsetX;
    private double currentOffsetY;
    
    // Game state checks (passed from BilliardApp)
    private boolean isGamePaused;
    private boolean isArcadeGameOver;

    /**
     * Constructor for InputHandler.
     */
    public InputHandler(CueStick cueStick, CueBall cueBall, GameRules gameRules, 
                       List<GameObject> gameObjects, boolean is8BallMode) {
        this.cueStick = cueStick;
        this.cueBall = cueBall;
        this.gameRules = gameRules;
        this.gameObjects = gameObjects;
        this.is8BallMode = is8BallMode;
    }
    
    /**
     * Set callbacks for actions that affect main app state.
     */
    public void setCallbacks(Runnable onTogglePause, Runnable onDebugClearTable) {
        this.onTogglePause = onTogglePause;
        this.onDebugClearTable = onDebugClearTable;
    }
    
    /**
     * Update cue stick reference (needed when cue stick is recreated).
     */
    public void setCueStick(CueStick cueStick) {
        this.cueStick = cueStick;
    }
    
    /**
     * Update cue ball reference (needed when game is restarted).
     */
    public void setCueBall(CueBall cueBall) {
        this.cueBall = cueBall;
    }
    
    /**
     * Update game rules reference (needed when game is restarted).
     */
    public void setGameRules(GameRules gameRules) {
        this.gameRules = gameRules;
    }
    
    /**
     * Update game state flags (called each frame).
     */
    public void updateState(boolean isGamePaused, boolean isArcadeGameOver, 
                           double currentOffsetX, double currentOffsetY) {
        this.isGamePaused = isGamePaused;
        this.isArcadeGameOver = isArcadeGameOver;
        this.currentOffsetX = currentOffsetX;
        this.currentOffsetY = currentOffsetY;
    }
    
    /**
     * Handle mouse moved event.
     */
    public void handleMouseMoved(MouseEvent e) {
        if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;
        
        double logicX = e.getX() - currentOffsetX;
        double logicY = e.getY() - currentOffsetY;

        if (isBallInHandActive()) moveCueBallToMouse(logicX, logicY);
        else cueStick.handleMouseMoved(logicX, logicY);
    }
    
    /**
     * Handle mouse pressed event.
     */
    public void handleMousePressed(MouseEvent e) {
        // Cek Tombol Pause di Pojok Kiri Atas
        if (e.getX() >= PAUSE_BTN_X && e.getX() <= PAUSE_BTN_X + PAUSE_BTN_SIZE &&
                e.getY() >= PAUSE_BTN_Y && e.getY() <= PAUSE_BTN_Y + PAUSE_BTN_SIZE) {

            if (onTogglePause != null) onTogglePause.run();
            return;
        }

        if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;

        double logicX = e.getX() - currentOffsetX;
        double logicY = e.getY() - currentOffsetY;

        if (isBallInHandActive()) tryPlaceCueBall(logicX, logicY);
        else cueStick.handleMousePressed(logicX, logicY);
    }
    
    /**
     * Handle mouse dragged event.
     */
    public void handleMouseDragged(MouseEvent e) {
        if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;
        
        double logicX = e.getX() - currentOffsetX;
        double logicY = e.getY() - currentOffsetY;

        if (isBallInHandActive()) moveCueBallToMouse(logicX, logicY);
        else cueStick.handleMouseDragged(logicX, logicY);
    }
    
    /**
     * Handle mouse released event.
     */
    public void handleMouseReleased(MouseEvent e) {
        if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;
        
        if (!isBallInHandActive()) {
            cueStick.handleMouseReleased(e.getX() - currentOffsetX, e.getY() - currentOffsetY);
        }
    }
    
    /**
     * Handle keyboard pressed event.
     */
    public void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        
        if (code == KeyCode.ESCAPE) {
            if (onTogglePause != null) onTogglePause.run();
        } else if (code == KeyCode.C) {
            // DEBUG: Clear table (Arcade mode only)
            if (!is8BallMode && onDebugClearTable != null) {
                onDebugClearTable.run();
            }
        }
    }
    
    // ==================== BALL IN HAND LOGIC ====================
    
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
}
