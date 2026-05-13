package uz.kidzone.app;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * Manages background music for the application.
 * Follows Singleton pattern to ensure only one music instance plays.
 */
public class MusicManager {
    private static final String TAG = "MusicManager";
    private static MusicManager instance;
    private MediaPlayer mediaPlayer;
    private boolean isMuted = false;

    private MusicManager() {}

    public static synchronized MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
        if (mediaPlayer != null) {
            if (isMuted) {
                mediaPlayer.setVolume(0, 0);
            } else {
                mediaPlayer.setVolume(0.15f, 0.15f); // Calm volume
            }
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void startMusic(Context context) {
        if (mediaPlayer == null) {
            initializeMediaPlayer(context);
        } else {
            resumeMusic();
        }
    }

    private void initializeMediaPlayer(Context context) {
        int resId = context.getResources().getIdentifier("bg_music", "raw", context.getPackageName());
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context.getApplicationContext(), resId);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(isMuted ? 0f : 0.15f, isMuted ? 0f : 0.15f);
                if (!isMuted) mediaPlayer.start();
            }
        } else {
            loadNetworkMusic();
        }
    }

    private void loadNetworkMusic() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource("https://www.bensound.com/bensound-music/bensound-lullaby.mp3");
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(isMuted ? 0f : 0.15f, isMuted ? 0f : 0.15f);
            mediaPlayer.setOnPreparedListener(mp -> { if (!isMuted) mp.start(); });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "Network music unavailable (what=" + what + "). Running silent.");
                mp.reset();
                mp.release();
                mediaPlayer = null;
                return true; // handled — no retry
            });
            mediaPlayer.prepareAsync();
            Log.d(TAG, "Loading fallback network music");
        } catch (Exception e) {
            Log.w(TAG, "Could not start network music: " + e.getMessage());
            mediaPlayer = null;
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && !isMuted) {
            mediaPlayer.start();
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
