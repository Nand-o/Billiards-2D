package com.billiards2d;

import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static com.billiards2d.GameConstants.*;

/**
 * Controls the main game loop logic including physics updates, turn management,
 * timer handling, and game state transitions.
 * Extracted from BilliardApp to follow Single Responsibility Principle.
 */
public class GameController {
    
    // Game objects and dependencies
    private List<GameObject> gameObjects;
    private CueStick cueStick;
    private CueBall cueBall;
    private GameRules gameRules;
    private PhysicsEngine physicsEngine;
    private List<FloatingText> floatingTexts;
    private List<Integer> pocketHistory;
    
    // Game mode and state
    private boolean is8BallMode;
    private boolean isGamePaused;
    private boolean isArcadeGameOver;
    private boolean turnInProgress;
    
    // Timers
    private double arcadeTimer;
    private double currentTurnTime;
    private int highScore;
    
    // Preferences for saving high score
    private Preferences prefs;
    
    // Callback for respawning rack
    private Runnable onRespawnRack;
    
    // Callback for game over overlay updates
    private GameOverCallback onGameOver;
    
    /**
     * Interface for game over callbacks
     */
    public interface GameOverCallback {
        void showGameOver(String title, Color titleColor, String message);
        void hideGameOver();
    }
    
    /**
     * Constructor for GameController
     */
    public GameController(List<GameObject> gameObjects, CueStick cueStick, CueBall cueBall,
                         GameRules gameRules, PhysicsEngine physicsEngine,
                         List<FloatingText> floatingTexts, List<Integer> pocketHistory,
                         boolean is8BallMode, Preferences prefs) {
        this.gameObjects = gameObjects;
        this.cueStick = cueStick;
        this.cueBall = cueBall;
        this.gameRules = gameRules;
        this.physicsEngine = physicsEngine;
        this.floatingTexts = floatingTexts;
        this.pocketHistory = pocketHistory;
        this.is8BallMode = is8BallMode;
        this.prefs = prefs;
        
        this.turnInProgress = false;
        this.currentTurnTime = TURN_TIME_LIMIT;
        this.arcadeTimer = ARCADE_START_TIME;
        this.isArcadeGameOver = false;
        
        // Load high score from Preferences
        this.highScore = (prefs != null) ? prefs.getInt(PREF_KEY_HIGH_SCORE, 0) : 0;
    }
    
    /**
     * Set callbacks for actions that need to affect external state
     */
    public void setCallbacks(Runnable onRespawnRack, GameOverCallback onGameOver) {
        this.onRespawnRack = onRespawnRack;
        this.onGameOver = onGameOver;
    }
    
    /**
     * Update game state (called every frame, but paused flag checked internally)
     */
    public void update(double deltaTime, boolean isGamePaused, double currentOffsetX, double currentOffsetY) {
        this.isGamePaused = isGamePaused;
        
        // Skip logic if paused
        if (isGamePaused) {
            return;
        }
        
        // 1. UPDATE FISIKA
        // Hanya update fisika jika TIDAK sedang menaruh bola (biar ga gerak2 sendiri)
        if (!(is8BallMode && gameRules.isBallInHand())) {
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
                        prefs = Preferences.userNodeForPackage(GameController.class);
                    }

                    // Simpan ke Registry
                    if (prefs != null) {
                        prefs.putInt(PREF_KEY_HIGH_SCORE, highScore);
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
    
    /**
     * Check if game is over and trigger overlay
     */
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
        if (isFinished && onGameOver != null) {
            onGameOver.showGameOver(titleText, titleColor, msgText);
        }

        // Jika restart (overlay msh aktif tapi game sudah reset), sembunyikan
        if (!isFinished && onGameOver != null) {
            onGameOver.hideGameOver();
        }
    }
    
    /**
     * Check game rules and process turn end
     */
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
    
    /**
     * Process turn end logic for both game modes
     */
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
                double spawnX = (GAME_WIDTH / 2);
                double spawnY = (GAME_HEIGHT / 2);

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
                double spawnX = (GAME_WIDTH / 2);
                double spawnY = (GAME_HEIGHT / 2);
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
                double cx = (GAME_WIDTH/2);
                double cy = (GAME_HEIGHT/2);
                floatingTexts.add(new FloatingText(cx, cy - 40, "TABLE CLEARED!", Color.CYAN));
                floatingTexts.add(new FloatingText(cx, cy, "+" + stageBonusScore + " pts", Color.GOLD));
                floatingTexts.add(new FloatingText(cx, cy + 40, "+" + (int)stageBonusTime + "s", Color.LIME));

                // RESPAWN RACK (Refill Bola)
                if (onRespawnRack != null) {
                    onRespawnRack.run();
                }
            }

            physicsEngine.resetTurnReport();
        }
    }
    
    /**
     * Count active object balls (not cue ball)
     */
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
    
    // ==================== GETTERS & SETTERS ====================
    
    public double getArcadeTimer() {
        return arcadeTimer;
    }
    
    public void setArcadeTimer(double arcadeTimer) {
        this.arcadeTimer = arcadeTimer;
    }
    
    public double getCurrentTurnTime() {
        return currentTurnTime;
    }
    
    public void setCurrentTurnTime(double currentTurnTime) {
        this.currentTurnTime = currentTurnTime;
    }
    
    public int getHighScore() {
        return highScore;
    }
    
    public void setHighScore(int highScore) {
        this.highScore = highScore;
    }
    
    public boolean isArcadeGameOver() {
        return isArcadeGameOver;
    }
    
    public void setArcadeGameOver(boolean arcadeGameOver) {
        this.isArcadeGameOver = arcadeGameOver;
    }
    
    public boolean isTurnInProgress() {
        return turnInProgress;
    }
    
    public void setTurnInProgress(boolean turnInProgress) {
        this.turnInProgress = turnInProgress;
    }
}
