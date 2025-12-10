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

    // Mode Host
    public void startServer(int port) {
        new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                System.out.println("Server Waiting...");
                socket = ss.accept();
                setupStreams();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    // Mode Join
    public void connectClient(String host, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                setupStreams();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void setupStreams() throws IOException {
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        // Mulai thread untuk dengerin data masuk
        new Thread(this::listen).start();

        // Kalau kita nembak, kirim datanya ke lawan
        GameBus.subscribe(GameBus.EventType.SHOT_TAKEN, payload -> {
            try {
                Vector2D f = (Vector2D) payload;
                out.writeByte(1); // Header: 1 = Shot
                out.writeDouble(f.getX());
                out.writeDouble(f.getY());
                out.flush();
            } catch (IOException e) { e.printStackTrace(); }
        });
    }

    private void listen() {
        while (running && socket != null && !socket.isClosed()) {
            try {
                // Protocol sederhana: Baca byte header dulu
                byte type = in.readByte();
                if (type == 1) { // 1 = Shot Data
                    double x = in.readDouble();
                    double y = in.readDouble();
                    // Update UI harus di JavaFX Thread
                    Platform.runLater(() ->
                            GameBus.publish(GameBus.EventType.REMOTE_SHOT, new Vector2D(x, y))
                    );
                }
            } catch (IOException e) {
                running = false;
                System.out.println("Disconnected");
            }
        }
    }
}