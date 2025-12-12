package com.billiards2d;

import java.util.List;

/**
 * Kelas yang bertindak sebagai "Wasit" dan Pengelola Status Permainan (Game State).
 * Kelas ini bertanggung jawab menerapkan aturan 8-Ball Pool standar.
 */
public class GameRules {

    // --- ENUMS UNTUK STATUS ---

    public enum PlayerTurn {
        PLAYER_1,
        PLAYER_2
    }

    public enum TableState {
        OPEN,           // Belum ditentukan siapa Solid/Stripes
        P1_SOLID,       // Player 1 = Solid, Player 2 = Stripes
        P1_STRIPES      // Player 1 = Stripes, Player 2 = Solid
    }

    public enum GameStatus {
        ONGOING,        // Permainan berjalan
        P1_WINS,        // Player 1 Menang
        P2_WINS         // Player 2 Menang
    }

    // --- STATE VARIABLES ---

    private PlayerTurn currentTurn;
    private TableState tableState;
    private GameStatus gameStatus;

    // Pesan status untuk ditampilkan di HUD (misal: "FOUL! Cue Ball Pocketed")
    private String statusMessage;
    private boolean isFoul;
    private boolean isBallInHand = false;
    private boolean cleanWin = false;

    public GameRules() {
        resetGame();
    }

    public void resetGame() {
        currentTurn = PlayerTurn.PLAYER_1;
        tableState = TableState.OPEN;
        gameStatus = GameStatus.ONGOING;
        statusMessage = "Game Start! Table OPEN.";
        isFoul = false;
        isBallInHand = false;
    }

    /**
     * LOGIKA INTI (THE BRAIN):
     * Method ini dipanggil setiap kali semua bola berhenti bergerak.
     * Menerima laporan bola apa saja yang masuk lubang untuk menentukan nasib giliran selanjutnya.
     *
     * @param pocketedBalls List bola objektif (warna) yang masuk lubang.
     * @param cueBallPocketed Apakah bola putih masuk lubang (Foul).
     * @param remainingBalls List semua bola yang MASIH ADA di meja (untuk cek sisa bola).
     */
    public void processTurn(List<ObjectBall> pocketedBalls,
                            boolean cueBallPocketed,
                            List<Ball> remainingBalls,
                            Ball firstHitBall) {

        isFoul = false;
        statusMessage = "";

        // --- 1. CEK CRITICAL: APAKAH BOLA 8 MASUK? ---
        // Aturan: Jika 8-Ball masuk, game SELESAI saat itu juga.
        // Tidak ada cerita lanjut main atau Ball-in-Hand.
        if (containsEightBall(pocketedBalls)) {

            // Skenario A: Masuk Bareng Bola Putih (Scratch) -> KALAH MUTLAK
            if (cueBallPocketed) {
                handleLoss();
                return;
            }

            // Skenario B: Belum Habisin Bola Kategori Sendiri (Early 8) -> KALAH MUTLAK
            if (!hasClearedGroup(currentTurn, remainingBalls)) {
                handleLoss();
                return;
            }

            // Skenario C: Foul Lain terjadi saat memasukkan 8 (Misal salah pukul duluan) -> KALAH
            // Cek validitas first hit
            if (!isValidFirstHit(firstHitBall, remainingBalls)) {
                handleLoss();
                return;
            }

            // Skenario D: Bersih -> MENANG
            handleWin();
            return;
        }

        // --- 2. CEK FOUL BIASA (Game Masih Lanjut) ---

        // A. Scratch (Bola Putih Masuk)
        if (cueBallPocketed) {
            handleFoul("FOUL! Scratch.");
            return;
        }

        // B. Pukul Angin
        if (firstHitBall == null) {
            handleFoul("FOUL! No ball hit.");
            return;
        }

        // C. Salah Pukul (Bola Lawan/8 Duluan)
        if (!isValidFirstHit(firstHitBall, remainingBalls)) {
            handleFoul("FOUL! Illegal Hit.");
            return;
        }

        // --- 3. PROSES NORMAL (Tidak Foul, Tidak Game Over) ---

        if (pocketedBalls.isEmpty()) {
            switchTurn();
            statusMessage = "No balls. Switch.";
        } else {
            // Logic Assign Category
            if (tableState == TableState.OPEN) {
                ObjectBall first = pocketedBalls.get(0);
                // Pastikan bukan bola 8 (sudah dicek diatas sih)
                assignCategory(first.getType());
                statusMessage = "Category SET: " + tableState;
                // Keep Turn
            } else {
                // Logic Bola Sendiri vs Lawan
                boolean correctBallPocketed = false;
                for (ObjectBall b : pocketedBalls) {
                    if (isBallBelongToCurrentPlayer(b.getType())) correctBallPocketed = true;
                }

                if (correctBallPocketed) {
                    statusMessage = "Good shot. Continue.";
                } else {
                    statusMessage = "Opponent ball. Switch.";
                    switchTurn();
                }
            }
        }
    }

    // Helper Baru untuk validasi First Hit (SUDAH DIPERBAIKI)
    private boolean isValidFirstHit(Ball firstHitBall, List<Ball> remainingBalls) {
        if (firstHitBall == null) return false;
        if (!(firstHitBall instanceof ObjectBall)) return false;

        BallType hitType = ((ObjectBall) firstHitBall).getType();

        // A. MEJA OPEN
        if (tableState == TableState.OPEN) {
            // Boleh kena apa saja KECUALI 8-Ball
            return hitType != BallType.EIGHT_BALL;
        }

        // B. MEJA ASSIGNED (Solid vs Stripes)
        BallType myCategory = getMyCategory();

        // --- LOGIC FIX START ---

        // 1. ATURAN EMAS: Jika memukul bola kategori sendiri -> SELALU SAH.
        // Ini menangani kasus "Bola Terakhir". Meskipun setelah bola ini masuk listnya jadi kosong,
        // faktanya saat dipukul, itu adalah bola kita. Jadi Valid.
        if (hitType == myCategory) {
            return true;
        }

        // 2. ATURAN BOLA 8: Jika memukul bola 8...
        if (hitType == BallType.EIGHT_BALL) {
            // ...Hanya sah jika bola kategori kita MEMANG SUDAH HABIS (Cleared) di meja.
            return hasClearedGroup(currentTurn, remainingBalls);
        }

        // --- LOGIC FIX END ---

        // 3. Sisanya (Kena bola lawan) -> Foul
        return false;
    }

    private void handleFoul(String msg) {
        isFoul = true;
        statusMessage = msg;
        switchTurn();

        // AKTIFKAN BALL IN HAND
        isBallInHand = true;
    }

    // Method untuk mematikan status Ball in Hand setelah bola ditaruh
    public void clearBallInHand() {
        isBallInHand = false;
        statusMessage = "Ball Placed. Good Luck!";
    }

    public boolean isBallInHand() {
        return isBallInHand;
    }

    // --- HELPER LOGIC METHODS ---

    private void switchTurn() {
        if (currentTurn == PlayerTurn.PLAYER_1) {
            currentTurn = PlayerTurn.PLAYER_2;
        } else {
            currentTurn = PlayerTurn.PLAYER_1;
        }
    }

    // Method handleWin (Menang Sah)
    private void handleWin() {
        cleanWin = true; // Tandai menang bersih

        if (currentTurn == PlayerTurn.PLAYER_1) gameStatus = GameStatus.P1_WINS;
        else gameStatus = GameStatus.P2_WINS;

        statusMessage = "VICTORY! " + currentTurn + " WINS!";
    }

    // Method handleLoss (Kalah karena Blunder)
    private void handleLoss() {
        cleanWin = false; // Tandai menang kotor (karena lawan foul)

        // Simpan nama yang kalah untuk pesan error
        PlayerTurn loser = currentTurn;

        // Set Pemenang (Lawan dari yang main sekarang)
        if (currentTurn == PlayerTurn.PLAYER_1) gameStatus = GameStatus.P2_WINS;
        else gameStatus = GameStatus.P1_WINS;

        statusMessage = "GAME OVER! " + loser + " LOST (Early 8-Ball)";
    }

    // Getter Baru
    public boolean isCleanWin() {
        return cleanWin;
    }

    private void assignCategory(BallType type) {
        if (type == BallType.SOLID) {
            if (currentTurn == PlayerTurn.PLAYER_1) tableState = TableState.P1_SOLID;
            else tableState = TableState.P1_STRIPES; // P2 dapet Solid -> P1 jadi Stripes
        } else if (type == BallType.STRIPE) {
            if (currentTurn == PlayerTurn.PLAYER_1) tableState = TableState.P1_STRIPES;
            else tableState = TableState.P1_SOLID; // P2 dapet Stripe -> P1 jadi Solid
        }
    }

    private boolean isBallBelongToCurrentPlayer(BallType type) {
        if (tableState == TableState.OPEN) return true; // Belum ada kepemilikan
        if (type == BallType.EIGHT_BALL) return false; // Bola 8 bukan milik siapa2 sampai akhir

        if (currentTurn == PlayerTurn.PLAYER_1) {
            return (tableState == TableState.P1_SOLID && type == BallType.SOLID) ||
                    (tableState == TableState.P1_STRIPES && type == BallType.STRIPE);
        } else {
            // Logic Player 2 adalah kebalikan Player 1
            return (tableState == TableState.P1_SOLID && type == BallType.STRIPE) ||
                    (tableState == TableState.P1_STRIPES && type == BallType.SOLID);
        }
    }

    private boolean containsEightBall(List<ObjectBall> balls) {
        for (ObjectBall b : balls) {
            if (b.getType() == BallType.EIGHT_BALL) return true;
        }
        return false;
    }

    /**
     * Helper untuk mengecek target yang sah.
     * UPDATE: Sekarang menerima list sisa bola untuk mengecek apakah grup sudah habis.
     */
    public boolean isValidTarget(BallType targetType, List<Ball> remainingBalls) {
        if (targetType == BallType.CUE) return false;

        // 1. MEJA OPEN
        if (tableState == TableState.OPEN) {
            // Semua boleh KECUALI 8-Ball
            return targetType != BallType.EIGHT_BALL;
        }

        // 2. MEJA ASSIGNED
        BallType myType = getMyCategory();

        // A. Cek jika membidik Bola Sendiri -> SAH
        if (targetType == myType) return true;

        // B. Cek jika membidik Bola 8
        if (targetType == BallType.EIGHT_BALL) {
            // SAH HANYA JIKA semua bola kategori pemain sudah habis
            return hasClearedGroup(currentTurn, remainingBalls);
        }

        // C. Sisanya (Bola Lawan) -> TIDAK SAH
        return false;
    }

    // Helper: Cek apakah pemain sudah menghabiskan semua bola miliknya
    // Ubah visibility jadi public atau protected jika perlu, tapi private cukup untuk internal
    private boolean hasClearedGroup(PlayerTurn player, List<Ball> remainingBalls) {
        if (tableState == TableState.OPEN) return false;

        BallType targetType;
        if (player == PlayerTurn.PLAYER_1) {
            targetType = (tableState == TableState.P1_SOLID) ? BallType.SOLID : BallType.STRIPE;
        } else {
            targetType = (tableState == TableState.P1_SOLID) ? BallType.STRIPE : BallType.SOLID;
        }

        // Loop cari bola target di meja. Jika ketemu SATU saja, berarti BELUM clear.
        for (Ball b : remainingBalls) {
            if (b instanceof ObjectBall) {
                ObjectBall ob = (ObjectBall) b;
                if (ob.isActive() && ob.getType() == targetType) { // Cek isActive() jaga-jaga
                    return false;
                }
            }
        }
        return true; // Tidak ada bola target tersisa -> CLEAR
    }

    // Helper internal
    private BallType getMyCategory() {
        if (currentTurn == PlayerTurn.PLAYER_1) {
            if (tableState == TableState.P1_SOLID) return BallType.SOLID;
            if (tableState == TableState.P1_STRIPES) return BallType.STRIPE;
        } else {
            if (tableState == TableState.P1_SOLID) return BallType.STRIPE;
            if (tableState == TableState.P1_STRIPES) return BallType.SOLID;
        }
        return BallType.UNKNOWN;
    }

    // --- GETTERS ---
    public PlayerTurn getCurrentTurn() { return currentTurn; }
    public String getStatusMessage() { return statusMessage; }
    public GameStatus getGameStatus() { return gameStatus; }
    public boolean isGameOver() { return gameStatus != GameStatus.ONGOING; }
    public TableState getTableState() { return tableState; }
}