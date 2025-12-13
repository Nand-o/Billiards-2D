package com.billiards2d.ui;

import static com.billiards2d.core.GameConstants.*;

import com.billiards2d.entities.balls.Ball;
import com.billiards2d.entities.balls.BallType;
import com.billiards2d.entities.balls.CueBall;
import com.billiards2d.entities.balls.ObjectBall;
import com.billiards2d.game.GameRules;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Renderer untuk elemen-elemen UI di dalam permainan (power bar, tracker bola,
 * status pemain, sidebar, dan menu pause).
 * <p>
 * Perhatian: string notifikasi yang tampil pada game (mis. "Player 1 wins")
 * dipertahankan dalam Bahasa Inggris dan tidak diubah oleh dokumentasi ini.
 * </p>
 *
 * @since 2025-12-13
 */
public class GameUIRenderer {

    private Image uiSpriteSheet;
    private Image ballSpriteSheet;

    public GameUIRenderer(Image uiSpriteSheet, Image ballSpriteSheet) {
        this.uiSpriteSheet = uiSpriteSheet;
        this.ballSpriteSheet = ballSpriteSheet;
    }

    /**
     * Menggambar power bar di kiri bawah layar
     */
    public void drawPowerBar(GraphicsContext gc, double canvasHeight, double powerRatio) {
        // Posisi Awal UI
        double startX = 30;
        double startY = canvasHeight - 30; // Naikkan sedikit biar ga mepet bawah

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
            if (powerRatio < 0.5) barColor = Color.LIME;
            else if (powerRatio < 0.8) barColor = Color.YELLOW;
            else barColor = Color.RED;

            gc.setFill(barColor);
            // Margin 2px di dalam border
            gc.fillRect(barX + 2, barY + 2, (maxBarWidth - 4) * powerRatio, barHeight - 4);

            // 4. Teks Persentase
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
            // Taruh teks sedikit di atas bar
            gc.fillText((int)(powerRatio * 100) + "%", barX + maxBarWidth + 10, barY + barHeight);
        }
    }

    /**
     * Menggambar ball tracker untuk 8-ball mode
     */
    public void drawBallTracker(GraphicsContext gc, double canvasWidth,
                               boolean[] isBallActive, GameRules.TableState state,
                               GameRules.PlayerTurn turn, GameRules gameRules,
                               double currentTurnTime, Image ballSpriteSheet, boolean is8BallMode) {
        boolean p1IsSolid = true;
        if (state == GameRules.TableState.P1_STRIPES) p1IsSolid = false;

        // Layout Config
        double centerX = canvasWidth / 2;
        double offsetFromCenter = 300;

        // Render Player 1 (Kiri)
        drawPlayerStats(gc, "PLAYER 1", p1IsSolid, isBallActive,
                centerX - offsetFromCenter - 150, 30,
                turn == GameRules.PlayerTurn.PLAYER_1, state, gameRules,
                currentTurnTime, ballSpriteSheet, is8BallMode);

        // Render Player 2 (Kanan)
        drawPlayerStatsRightAligned(gc, "PLAYER 2", !p1IsSolid, isBallActive,
                centerX + offsetFromCenter + 150, 30,
                turn == GameRules.PlayerTurn.PLAYER_2, state, gameRules,
                currentTurnTime, ballSpriteSheet, is8BallMode);
    }

    /**
     * Helper Player 1 (Kiri)
     */
    private void drawPlayerStats(GraphicsContext gc, String name, boolean isSolid, boolean[] activeBalls,
                                 double x, double y, boolean isMyTurn, GameRules.TableState state,
                                 GameRules gameRules, double currentTurnTime, Image ballSpriteSheet, boolean is8BallMode) {
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
        double ballSize = 20;
        double spacing = 22;
        int startBall = isSolid ? 1 : 9;
        int endBall = isSolid ? 7 : 15;

        for (int i = startBall; i <= endBall; i++) {
            double bx = x + ((i - startBall) * spacing);
            double by = y + 15; // Jarak dikit dari nama

            drawMiniBall(gc, i, bx, by, ballSize, activeBalls[i], ballSpriteSheet, is8BallMode);
        }
    }

    /**
     * Helper Player 2 (Kanan)
     */
    private void drawPlayerStatsRightAligned(GraphicsContext gc, String name, boolean isSolid, boolean[] activeBalls,
                                             double x, double y, boolean isMyTurn, GameRules.TableState state,
                                             GameRules gameRules, double currentTurnTime, Image ballSpriteSheet, boolean is8BallMode) {
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

            drawMiniBall(gc, i, bx, by, ballSize, activeBalls[i], ballSpriteSheet, is8BallMode);
        }
    }

    /**
     * Menggambar sprite bola kecil
     */
    private void drawMiniBall(GraphicsContext gc, int number, double x, double y, double size, boolean isActive,
                             Image ballSpriteSheet, boolean is8BallMode) {
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
     * Menampilkan pesan status game (Foul, Info, Win)
     */
    public void drawBottomRightStatus(GraphicsContext gc, double screenW, double screenH, String statusMessage) {
        double boxW = 350;
        double boxH = 40;
        double x = screenW - boxW - 30;
        double y = screenH - boxH - 20;

        if (statusMessage != null && !statusMessage.isEmpty()) {
            // 1. Background Box
            gc.setFill(Color.rgb(0, 0, 0, 0.8));
            gc.fillRoundRect(x, y, boxW, boxH, 10, 10);

            // 2. Border Logic
            Color statusColor = Color.WHITE;
            if (statusMessage.contains("FOUL")) statusColor = Color.RED;
            else if (statusMessage.contains("WIN") || statusMessage.contains("VICTORY")) statusColor = Color.LIME;
            else if (statusMessage.contains("Nice") || statusMessage.contains("Good")) statusColor = Color.CYAN;

            gc.setStroke(statusColor);
            gc.setLineWidth(2);
            gc.strokeRoundRect(x, y, boxW, boxH, 10, 10);

            // 3. Logic Ikon & Offset Teks
            double textOffsetX = 15;

            // GAMBAR IKON FOUL SECARA MANUAL (PROCEDURAL)
            if (statusMessage.contains("FOUL")) {
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
            gc.fillText(statusMessage, x + textOffsetX, y + 26);
        }
    }

    /**
     * LAYER BAWAH: Menggambar Pipa &amp; Keranjang Kosong
     */
    public void drawSideBarBackground(GraphicsContext gc, double screenW, double screenH,
                                     double currentOffsetX, double currentOffsetY) {
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
     * LAYER ATAS: Menggambar Bola di dalam keranjang
     */
    public void drawSideBarBalls(GraphicsContext gc, double screenW, double screenH,
                                 java.util.List<Integer> pocketHistory, Image ballSpriteSheet, boolean is8BallMode) {
        if (pocketHistory.isEmpty()) return;

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
            drawMiniBall(gc, ballNum, centerX - (ballSize/2), y, ballSize, true, ballSpriteSheet, is8BallMode);
        }
    }

    /**
     * Menggambar tombol pause di pojok kiri atas
     */
    public void drawPauseButton(GraphicsContext gc, double pauseBtnX, double pauseBtnY) {
        double btnSize = PAUSE_BTN_SIZE;
        
        // Background Hitam Transparan
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRoundRect(pauseBtnX, pauseBtnY, btnSize, btnSize, 8, 8);

        // Border Putih
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(pauseBtnX, pauseBtnY, btnSize, btnSize, 8, 8);

        // Ikon "||" (Dua Garis Vertikal)
        gc.setFill(Color.WHITE);
        double iconH = 20;
        double iconW = 4;
        double spacing = 6;
        double iconX = pauseBtnX + (btnSize / 2) - ((iconW * 2 + spacing) / 2);
        double iconY = pauseBtnY + (btnSize - iconH) / 2;

        gc.fillRect(iconX, iconY, iconW, iconH);
        gc.fillRect(iconX + iconW + spacing, iconY, iconW, iconH);
    }

    /**
     * Menggambar layar gelap + menu saat game dipause
     */
    public void drawPauseMenu(GraphicsContext gc, double screenW, double screenH) {
        // Layar Gelap Semi-transparan
        gc.setFill(Color.rgb(0, 0, 0, 0.8));
        gc.fillRect(0, 0, screenW, screenH);

        // Teks "PAUSED"
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Impact", FontWeight.BOLD, 80));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("PAUSED", screenW / 2, screenH / 2 - 50);

        // Hint
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 16));
        gc.fillText("Press ESC to Resume", screenW / 2, screenH / 2 + 30);
    }
}
