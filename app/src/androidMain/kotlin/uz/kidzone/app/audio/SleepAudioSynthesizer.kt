package uz.kidzone.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SleepSoundType {
    LULLABY,
    RAIN,
    WAVES,
    FOREST,
    WHITE_NOISE,
    TWINKLE,
}

/**
 * 100% Offline Audio Synthesizer for Bedtime & Sleep sounds.
 * Generates pure PCM audio via native AudioTrack without requiring any audio files or internet.
 */
class SleepAudioSynthesizer {

    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Serializes AudioTrack creation/teardown so play()/stop() calls issued back-to-back
    // (e.g. rapidly tapping different sound cards) never touch audioTrack concurrently.
    private val trackMutex = Mutex()

    @Volatile
    private var volume = 0.85f

    @Volatile
    private var isPlaying = false

    private val lullabyNotes = floatArrayOf(
        261.63f, 293.66f, 329.63f, 392.00f, 440.00f, 523.25f, 392.00f, 329.63f,
        440.00f, 392.00f, 329.63f, 293.66f, 261.63f, 329.63f, 392.00f, 523.25f,
    )

    /**
     * Returns immediately — AudioTrack setup/teardown is native/blocking work, so it all
     * runs on [scope] (Dispatchers.Default) instead of whatever thread calls this
     * (typically the UI thread, from a Compose click handler). The previous synthesis job
     * is fully joined before the old track is torn down, so there's no window where two
     * coroutines can write to the same AudioTrack at once.
     */
    fun play(type: SleepSoundType) {
        isPlaying = true
        val previousJob = synthesisJob
        synthesisJob = scope.launch {
            previousJob?.cancelAndJoin()
            trackMutex.withLock {
                // Switching between sounds while already playing reuses the existing
                // AudioTrack (just flushed) instead of destroying and recreating the
                // native track on every tap — cheaper and switches sounds faster.
                val existing = audioTrack
                if (existing != null) {
                    existing.flush()
                } else {
                    startTrackLocked()
                }
            }
            try {
                when (type) {
                    SleepSoundType.LULLABY, SleepSoundType.TWINKLE -> synthesizeLullaby(type == SleepSoundType.TWINKLE)
                    SleepSoundType.RAIN -> synthesizeRain()
                    SleepSoundType.WAVES -> synthesizeWaves()
                    SleepSoundType.FOREST -> synthesizeForest()
                    SleepSoundType.WHITE_NOISE -> synthesizeWhiteNoise()
                }
            } catch (e: Exception) {
                Log.w("SleepSynthesizer", "Playback stopped: ${e.message}")
            }
        }
    }

    private fun startTrackLocked() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(sampleRate / 2)

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setVolume(1.0f)
            track.play()
            audioTrack = track
        } catch (e: Exception) {
            Log.e("SleepSynthesizer", "Error initializing AudioTrack: ${e.message}")
        }
    }

    // Extension functions on CoroutineScope so `isActive` below reflects THIS synthesis
    // job's own cancellation state (from cancelAndJoin in play()/stop()), not just whether
    // the shared `scope` is alive — otherwise cancelling one job never actually stops its loop.
    private suspend fun CoroutineScope.synthesizeLullaby(isTwinkle: Boolean) {
        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)
        val noteDurationSamples = if (isTwinkle) (sampleRate * 0.75f).toInt() else (sampleRate * 1.3f).toInt()
        var noteIdx = 0
        var sampleInNote = 0

        while (isActive && isPlaying) {
            val freq = lullabyNotes[noteIdx % lullabyNotes.size]
            val omega = (2.0 * PI * freq / sampleRate).toFloat()

            for (i in 0 until bufferSize) {
                val t = sampleInNote.toFloat() / sampleRate
                // Warm, audible musical box envelope with gentle sustain
                val envelope = (0.75f * exp(-t * 1.1f) + 0.25f) * volume
                val wave = sin(omega * sampleInNote) + 0.28f * sin(omega * 2f * sampleInNote)
                val sample = (wave * envelope * 28000f).toInt().coerceIn(-32767, 32767)
                buffer[i] = sample.toShort()

                sampleInNote++
                if (sampleInNote >= noteDurationSamples) {
                    sampleInNote = 0
                    noteIdx++
                }
            }
            audioTrack?.write(buffer, 0, bufferSize)
        }
    }

    private suspend fun CoroutineScope.synthesizeRain() {
        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)
        var lastOut = 0.0f

        while (isActive && isPlaying) {
            for (i in 0 until bufferSize) {
                val white = (Random.nextFloat() * 2f - 1f)
                // Filtered pink noise with audible amplitude
                lastOut = (lastOut + 0.04f * white) / 1.04f
                val sample = (lastOut * volume * 220000f).toInt().coerceIn(-32767, 32767)
                buffer[i] = sample.toShort()
            }
            audioTrack?.write(buffer, 0, bufferSize)
        }
    }

    private suspend fun CoroutineScope.synthesizeWaves() {
        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)
        var sampleCount = 0L
        var lastOut = 0.0f

        while (isActive && isPlaying) {
            for (i in 0 until bufferSize) {
                val white = (Random.nextFloat() * 2f - 1f)
                lastOut = (lastOut + 0.025f * white) / 1.025f
                val wavePhase = sin(2.0 * PI * 0.15 * (sampleCount.toDouble() / sampleRate)).toFloat()
                val waveMod = (wavePhase + 1f) * 0.5f
                val sample = (lastOut * (0.3f + 0.7f * waveMod) * volume * 240000f).toInt().coerceIn(-32767, 32767)
                buffer[i] = sample.toShort()
                sampleCount++
            }
            audioTrack?.write(buffer, 0, bufferSize)
        }
    }

    private suspend fun CoroutineScope.synthesizeForest() {
        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)
        var sampleCount = 0L
        var lastOut = 0.0f

        while (isActive && isPlaying) {
            for (i in 0 until bufferSize) {
                val white = (Random.nextFloat() * 2f - 1f)
                lastOut = (lastOut + 0.025f * white) / 1.025f

                val chirpPeriod = (sampleRate * 1.4).toLong()
                val posInPeriod = sampleCount % chirpPeriod
                val isChirping = posInPeriod < (sampleRate * 0.12)
                val cricket = if (isChirping) sin(2.0 * PI * 4200.0 * (sampleCount.toDouble() / sampleRate)).toFloat() * 0.35f else 0f

                val combined = (lastOut * 120000f + cricket * 16000f) * volume
                val sample = combined.toInt().coerceIn(-32767, 32767)
                buffer[i] = sample.toShort()
                sampleCount++
            }
            audioTrack?.write(buffer, 0, bufferSize)
        }
    }

    private suspend fun CoroutineScope.synthesizeWhiteNoise() {
        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)
        var lastOut = 0.0f

        while (isActive && isPlaying) {
            for (i in 0 until bufferSize) {
                val white = (Random.nextFloat() * 2f - 1f)
                lastOut = (lastOut + 0.08f * white) / 1.08f
                val sample = (lastOut * volume * 140000f).toInt().coerceIn(-32767, 32767)
                buffer[i] = sample.toShort()
            }
            audioTrack?.write(buffer, 0, bufferSize)
        }
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
    }

    /** Returns immediately — teardown runs on [scope], never blocking the caller. */
    fun stop() {
        isPlaying = false
        val previousJob = synthesisJob
        synthesisJob = null
        scope.launch {
            previousJob?.cancelAndJoin()
            trackMutex.withLock {
                stopTrackLocked()
            }
        }
    }

    private fun stopTrackLocked() {
        val track = audioTrack ?: return
        audioTrack = null
        try {
            track.pause()
            track.flush()
            track.stop()
            track.release()
        } catch (ignored: Exception) {}
    }

    fun release() {
        stop()
    }
}
