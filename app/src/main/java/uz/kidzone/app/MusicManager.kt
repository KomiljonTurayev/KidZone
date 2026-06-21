package uz.kidzone.app

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

object MusicManager {
    private const val TAG = "MusicManager"
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false

    /** Java compatibility: MusicManager.getInstance().method() */
    @JvmStatic
    fun getInstance(): MusicManager = this

    @JvmStatic
    fun setMuted(muted: Boolean) {
        isMuted = muted
        mediaPlayer?.setVolume(if (isMuted) 0f else 0.15f, if (isMuted) 0f else 0.15f)
    }

    @JvmStatic
    fun isMuted(): Boolean = isMuted

    @JvmStatic
    fun startMusic(context: Context) {
        if (mediaPlayer == null) initializeMediaPlayer(context) else resumeMusic()
    }

    private fun initializeMediaPlayer(context: Context) {
        val resId = context.resources.getIdentifier("bg_music", "raw", context.packageName)
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context.applicationContext, resId)?.apply {
                isLooping = true
                setVolume(if (isMuted) 0f else 0.15f, if (isMuted) 0f else 0.15f)
                if (!isMuted) start()
            }
        } else {
            loadNetworkMusic()
        }
    }

    private fun loadNetworkMusic() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource("https://www.bensound.com/bensound-music/bensound-lullaby.mp3")
                isLooping = true
                setVolume(if (isMuted) 0f else 0.15f, if (isMuted) 0f else 0.15f)
                setOnPreparedListener { if (!isMuted) it.start() }
                setOnErrorListener { mp, what, _ ->
                    Log.w(TAG, "Network music unavailable (what=$what). Running silent.")
                    mp.reset(); mp.release(); mediaPlayer = null; true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start network music: ${e.message}")
            mediaPlayer = null
        }
    }

    @JvmStatic
    fun pauseMusic() {
        if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
    }

    @JvmStatic
    fun resumeMusic() {
        if (mediaPlayer?.isPlaying == false && !isMuted) mediaPlayer?.start()
    }

    @JvmStatic
    fun stopMusic() {
        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
    }
}
