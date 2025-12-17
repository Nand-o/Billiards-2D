package com.billiards2d;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private static SoundManager instance;
    private Map<String, AudioClip> soundEffects = new HashMap<>();
    private MediaPlayer bgmPlayer;

    private boolean sfxMuted = false;
    private boolean bgmMuted = false;
    private double sfxVolume = 1.0;

    private SoundManager() {
        loadSounds();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private void loadSounds() {
        // Key (kiri) tetap sama agar logic di class lain tidak perlu diubah.
        // Value (kanan) disesuaikan dengan nama file asli di gambar.
        loadSFX("cue_strike", "sfx_cue_hit.wav");
        loadSFX("ball_hit",   "sfx_hit.wav");
        loadSFX("pocket",     "sfx_pocket.wav");
        loadSFX("ui_click",   "sfx_click.wav");
        loadSFX("rail",       "sfx_hit.wav");

        // Load BGM
        try {
            var url = getClass().getResource("/assets/sound/bgm_jazz.mp3");

            if (url != null) {
                String bgmPath = url.toString();
                Media bgmMedia = new Media(bgmPath);
                bgmPlayer = new MediaPlayer(bgmMedia);
                bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                bgmPlayer.setVolume(0.5);
            } else {
                System.err.println("File BGM tidak ditemukan di: /assets/sound/bgm_jazz.mp3");
            }
        } catch (Exception e) {
            System.err.println("Gagal load BGM: " + e.getMessage());
        }
    }

    private void loadSFX(String key, String fileName) {
        try {
            var url = getClass().getResource("/assets/sound/" + fileName);

            if (url != null) {
                AudioClip clip = new AudioClip(url.toString());
                soundEffects.put(key, clip);
            } else {
                System.err.println("File SFX tidak ditemukan: /assets/sound/" + fileName);
            }
        } catch (Exception e) {
            System.err.println("Gagal load SFX (" + fileName + "): " + e.getMessage());
        }
    }

    public void playSFX(String key, double volumeLevel) {
        if (sfxMuted) return;

        AudioClip clip = soundEffects.get(key);
        if (clip != null) {
            double vol = Math.max(0, Math.min(1.0, volumeLevel * sfxVolume));
            clip.play(vol);
        }
    }

    public void playSFX(String key) {
        playSFX(key, 1.0);
    }

    public void playBGM() {
        if (bgmPlayer != null && !bgmMuted) {
            bgmPlayer.play();
        }
    }

    public void stopBGM() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
        }
    }

    public void toggleMute() {
        sfxMuted = !sfxMuted;
        bgmMuted = !bgmMuted;

        if (bgmMuted && bgmPlayer != null) bgmPlayer.pause();
        else if (!bgmMuted && bgmPlayer != null) bgmPlayer.play();
    }
}