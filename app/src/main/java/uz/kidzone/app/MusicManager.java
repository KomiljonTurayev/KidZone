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

    /**
     * Starts playing background music.
     * Uses a fallback URL for a calm children's melody.
     */
    public void startMusic(Context context) {
        if (mediaPlayer == null) {
            int resId = context.getResources().getIdentifier("bg_music", "raw", context.getPackageName());
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(context.getApplicationContext(), resId);
            } else {
                try {
                    mediaPlayer = new MediaPlayer();
                    // Using a calmer, acoustic royalty-free track
                    String calmMusicUrl = "https://www.bensound.com/bensound-music/bensound-lullaby.mp3"; 
                    mediaPlayer.setDataSource(calmMusicUrl);
                    mediaPlayer.prepareAsync(); 
                    Log.d(TAG, "Using calm fallback music");
                } catch (Exception e) {
                    Log.e(TAG, "Error setting music source", e);
                }
            }

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                float vol = isMuted ? 0f : 0.15f; // Sokin ovoz
                mediaPlayer.setVolume(vol, vol);
                mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            }
        }
        
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && !isMuted) {
            try {
                mediaPlayer.start();
            } catch (Exception e) {
                Log.w(TAG, "Music start delayed");
            }
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
