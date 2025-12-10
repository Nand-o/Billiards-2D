package com.billiards2d.net;

import com.billiards2d.Vector2D;
import com.billiards2d.core.GameBus;
import javafx.application.Platform;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkManager {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean running = true;
    private boolean isConnected = false;

    // Mode Host
    public void startServer(int port) {
        new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                System.out.println("[SERVER] Waiting for client on port " + port + "...");
                socket = ss.accept();
                System.out.println("[SERVER] Client connected from " + socket.getInetAddress());
                isConnected = true;
                setupStreams();
            } catch (IOException e) {
                System.err.println("[SERVER] Error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // Mode Join
    public void connectClient(String host, int port) {
        new Thread(() -> {
            try {
                System.out.println("[CLIENT] Connecting to " + host + ":" + port + "...");
                socket = new Socket(host, port);
                System.out.println("[CLIENT] Connected successfully!");
                isConnected = true;
                setupStreams();
            } catch (IOException e) {
                System.err.println("[CLIENT] Connection error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void setupStreams() throws IOException {
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        System.out.println("[NETWORK] Streams setup complete");

        // Mulai thread untuk dengerin data masuk
        new Thread(this::listen).start();

        // Kalau kita nembak, kirim datanya ke lawan
        GameBus.subscribe(GameBus.EventType.SHOT_TAKEN, payload -> {
            if (!isConnected || out == null) {
                System.err.println("[NETWORK] Cannot send shot - not connected");
                return;
            }

            try {
                // Synchronized agar tidak tabrakan dengan thread sync
                synchronized (out) {
                    Vector2D f = (Vector2D) payload;
                    System.out.println("[SEND] Sending shot: X=" + f.getX() + " Y=" + f.getY());
                    out.writeByte(1); // Header: 1 = Shot
                    out.writeDouble(f.getX());
                    out.writeDouble(f.getY());
                    out.flush();
                    System.out.println("[SEND] Shot sent successfully");
                }
            } catch (IOException e) {
                System.err.println("[SEND] Error sending shot: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Method baru untuk kirim State (Skor & Giliran) - DIPERBAIKI
    public void sendState(int p1Score, int p2Score, int currentPlayer) {
        if (!isConnected || out == null) {
            System.err.println("[NETWORK] Cannot send state - not connected");
            return;
        }

        try {
            synchronized (out) {
                System.out.println("[SEND] Sending state sync: P1=" + p1Score + " P2=" + p2Score + " Turn=" + currentPlayer);
                out.writeByte(2); // Header: 2 = Sync State
                out.writeInt(p1Score);
                out.writeInt(p2Score);
                out.writeInt(currentPlayer);
                out.flush();
                System.out.println("[SEND] State sync sent successfully");
            }
        } catch (IOException e) {
            System.err.println("[SEND] Error sending state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void listen() {
        System.out.println("[LISTEN] Started listening for incoming messages...");

        while (running && socket != null && !socket.isClosed()) {
            try {
                byte type = in.readByte();

                if (type == 1) {
                    // 1 = Shot Data
                    double x = in.readDouble();
                    double y = in.readDouble();
                    System.out.println("[RECEIVE] Shot data: X=" + x + " Y=" + y);

                    // FIX: Gunakan Platform.runLater untuk thread safety
                    Platform.runLater(() -> {
                        System.out.println("[EVENT] Publishing REMOTE_SHOT event");
                        GameBus.publish(GameBus.EventType.REMOTE_SHOT, new Vector2D(x, y));
                    });

                } else if (type == 2) {
                    // 2 = Sync State
                    int p1 = in.readInt();
                    int p2 = in.readInt();
                    int turn = in.readInt();
                    System.out.println("[RECEIVE] State sync: P1=" + p1 + " P2=" + p2 + " Turn=" + turn);

                    int[] state = {p1, p2, turn};

                    // FIX: Gunakan Platform.runLater untuk thread safety
                    Platform.runLater(() -> {
                        System.out.println("[EVENT] Publishing GAME_SYNC event");
                        GameBus.publish(GameBus.EventType.GAME_SYNC, state);
                    });

                } else {
                    System.err.println("[RECEIVE] Unknown message type: " + type);
                }

            } catch (EOFException e) {
                System.out.println("[LISTEN] Connection closed by peer");
                running = false;
                isConnected = false;
            } catch (IOException e) {
                if (running) {
                    System.err.println("[LISTEN] Connection error: " + e.getMessage());
                }
                running = false;
                isConnected = false;
            }
        }

        System.out.println("[LISTEN] Stopped listening");
        cleanup();
    }

    // FIX: Tambahkan method cleanup untuk menutup koneksi dengan benar
    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[NETWORK] Cleanup complete");
        } catch (IOException e) {
            System.err.println("[NETWORK] Error during cleanup: " + e.getMessage());
        }
    }

    // FIX: Method untuk cek status koneksi
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    // FIX: Method untuk disconnect secara manual
    public void disconnect() {
        running = false;
        cleanup();
    }
}