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
        if (isMuted) {
            mediaPlayer?.pause()
        } else {
            mediaPlayer?.setVolume(0.15f, 0.15f)
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        }
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
