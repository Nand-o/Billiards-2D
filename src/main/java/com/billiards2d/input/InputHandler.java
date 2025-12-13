package com.billiards2d.input;

import com.billiards2d.core.GameObject;
import com.billiards2d.entities.balls.Ball;
import com.billiards2d.entities.balls.CueBall;
import com.billiards2d.entities.CueStick;
import com.billiards2d.game.GameRules;
import com.billiards2d.util.Vector2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.util.List;

import static com.billiards2d.core.GameConstants.*;

/**
 * Handler untuk semua input pemain (mouse + keyboard).
 * <p>
 * Tugasnya menerjemahkan event input menjadi aksi di game, seperti:
 * - menggerakkan `CueStick` dan mengatur power drag-and-release,
 * - menangani tombol pause/restart, dan shortcut pengembang (debug).
 * </p>
 *
 * Catatan: teks dan notifikasi HUD tetap menggunakan Bahasa Inggris.
 *
 * @since 2025-12-13
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
     * Constructor for `InputHandler`.
     *
     * @param cueStick instance `CueStick` untuk menggerakkan/menangani bidik
     * @param cueBall instance `CueBall` (bola putih)
     * @param gameRules instance `GameRules` untuk cek state seperti ball-in-hand
     * @param gameObjects daftar objek permainan yang diperlukan untuk validasi posisi
     * @param is8BallMode apakah mode permainan adalah 8-ball
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
     * Set callbacks used to affect the top-level application state.
     *
     * @param onTogglePause callback to toggle pause
     * @param onDebugClearTable callback to clear the table in debug/arcade
     */
    public void setCallbacks(Runnable onTogglePause, Runnable onDebugClearTable) {
        this.onTogglePause = onTogglePause;
        this.onDebugClearTable = onDebugClearTable;
    }
    
    /**
     * Update the `CueStick` reference (e.g., after recreation on restart).
     *
     * @param cueStick new CueStick instance
     */
    public void setCueStick(CueStick cueStick) {
        this.cueStick = cueStick;
    }
    
    /**
     * Update the `CueBall` reference (e.g., after respawn or restart).
     *
     * @param cueBall new CueBall instance
     */
    public void setCueBall(CueBall cueBall) {
        this.cueBall = cueBall;
    }
    
    /**
     * Update `GameRules` reference.
     *
     * @param gameRules the GameRules instance to use
     */
    public void setGameRules(GameRules gameRules) {
        this.gameRules = gameRules;
    }
    
    /**
     * Update transient game state used by input handling (called each frame).
     *
     * @param isGamePaused whether game is currently paused
     * @param isArcadeGameOver whether arcade mode is over
     * @param currentOffsetX current UI offset X for coordinate mapping
     * @param currentOffsetY current UI offset Y for coordinate mapping
     */
    public void updateState(boolean isGamePaused, boolean isArcadeGameOver, 
                           double currentOffsetX, double currentOffsetY) {
        this.isGamePaused = isGamePaused;
        this.isArcadeGameOver = isArcadeGameOver;
        this.currentOffsetX = currentOffsetX;
        this.currentOffsetY = currentOffsetY;
    }
    
    /**
     * Handle mouse-move events and forward normalized coordinates to the cue stick.
     *
     * @param e MouseEvent from JavaFX
     */
    public void handleMouseMoved(MouseEvent e) {
        if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;
        
        double logicX = e.getX() - currentOffsetX;
        double logicY = e.getY() - currentOffsetY;

        if (isBallInHandActive()) moveCueBallToMouse(logicX, logicY);
        else cueStick.handleMouseMoved(logicX, logicY);
    }
    
    /**
     * Handle mouse-pressed events (starts aim or places cue ball in ball-in-hand).
     *
     * @param e MouseEvent
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
     * Handle mouse-drag events, updates dragging/pullback state.
     *
     * @param e MouseEvent
     */
    public void handleMouseDragged(MouseEvent e) {
        if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;
        
        double logicX = e.getX() - currentOffsetX;
        double logicY = e.getY() - currentOffsetY;

        if (isBallInHandActive()) moveCueBallToMouse(logicX, logicY);
        else cueStick.handleMouseDragged(logicX, logicY);
    }
    
    /**
     * Handle mouse-release events and trigger the cue stick hit when appropriate.
     *
     * @param e MouseEvent
     */
    public void handleMouseReleased(MouseEvent e) {
        if (isGamePaused || isArcadeGameOver || gameRules.isGameOver()) return;
        
        if (!isBallInHandActive()) {
            cueStick.handleMouseReleased(e.getX() - currentOffsetX, e.getY() - currentOffsetY);
        }
    }
    
    /**
     * Handle key-pressed events for pause and debug shortcuts.
     *
     * @param event KeyEvent
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
