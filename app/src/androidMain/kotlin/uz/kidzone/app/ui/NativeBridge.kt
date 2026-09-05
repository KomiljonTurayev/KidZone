package uz.kidzone.app.ui

import android.app.Activity
import android.util.Log
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import uz.kidzone.app.MusicManager
import uz.kidzone.app.ai.StoryGenerator
import uz.kidzone.app.ui.viewmodel.MainViewModel

// Native JavaScript bridge
internal class NativeBridge(
    private val viewModel: MainViewModel,
    private val onOpenDashboard: () -> Unit,
    private val onOpenSleep: () -> Unit,
    private val onOpenKidzo: () -> Unit,
    private val onAskKidzo: (String) -> Unit,
    private val evalJs: (String) -> Unit,
    activity: Activity,
) {
    private val activity = WeakReference(activity)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun onMain(block: () -> Unit) {
        this.activity.get()?.runOnUiThread(block)
    }

    private var tts: android.speech.tts.TextToSpeech? = null
    private var ttsReady = false

    init {
        val ctx = this.activity.get()
        if (ctx != null) {
            tts = android.speech.tts.TextToSpeech(ctx) { status ->
                ttsReady = status == android.speech.tts.TextToSpeech.SUCCESS
                if (ttsReady) {
                    tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            onMain { evalJs("window.onNativeSpeechStart && window.onNativeSpeechStart()") }
                        }
                        override fun onDone(utteranceId: String?) {
                            onMain { evalJs("window.onNativeSpeechEnd && window.onNativeSpeechEnd()") }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            onMain { evalJs("window.onNativeSpeechEnd && window.onNativeSpeechEnd()") }
                        }
                    })
                }
            }
        }
    }

    private var currentPlayer: android.media.MediaPlayer? = null

    @android.webkit.JavascriptInterface
    fun speakText(text: String, lang: String) {
        if (lang != "uz") {
            onMain { speakOnDevice(text, lang) }
            return
        }
        val cacheDir = activity.get()?.cacheDir
        scope.launch {
            val audioFile = uz.kidzone.app.ai.AishaSpeechGenerator.synthesize(text, cacheDir)
            val played = audioFile != null && playAudioFile(audioFile)
            if (!played) onMain { speakOnDevice(text, lang) }
        }
    }

    private fun playAudioFile(file: java.io.File): Boolean {
        return try {
            currentPlayer?.let { it.stop(); it.release() }
            val player = android.media.MediaPlayer()
            player.setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener {
                onMain { evalJs("window.onNativeSpeechEnd && window.onNativeSpeechEnd()") }
                if (currentPlayer === it) currentPlayer = null
                it.release()
                file.delete()
            }
            player.setOnErrorListener { p, _, _ ->
                onMain { evalJs("window.onNativeSpeechEnd && window.onNativeSpeechEnd()") }
                if (currentPlayer === p) currentPlayer = null
                p.release()
                file.delete()
                true
            }
            player.prepare()
            currentPlayer = player
            onMain { evalJs("window.onNativeSpeechStart && window.onNativeSpeechStart()") }
            player.start()
            true
        } catch (e: Exception) {
            Log.w("NativeBridge", "Cloud audio playback failed: ${e.message}")
            false
        }
    }

    private fun speakOnDevice(text: String, lang: String) {
        val engine = tts
        if (engine == null || !ttsReady) {
            evalJs("window.onNativeSpeechError && window.onNativeSpeechError()")
            return
        }
        val locale = when (lang) {
            "ru" -> java.util.Locale("ru", "RU")
            "en" -> java.util.Locale.US
            else -> java.util.Locale("uz", "UZ")
        }
        fun unsupported(r: Int) =
            r == android.speech.tts.TextToSpeech.LANG_MISSING_DATA ||
                r == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED

        var result = engine.setLanguage(locale)
        if (lang != "ru" && lang != "en" && unsupported(result)) {
            result = engine.setLanguage(java.util.Locale("tr", "TR"))
            if (unsupported(result)) result = engine.setLanguage(java.util.Locale("ru", "RU"))
        }
        if (unsupported(result)) {
            engine.setLanguage(java.util.Locale.getDefault())
        }
        engine.stop()
        engine.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "kidzoStory")
    }

    @android.webkit.JavascriptInterface
    fun stopSpeaking() {
        onMain {
            tts?.stop()
            currentPlayer?.let { it.stop(); it.release() }
            currentPlayer = null
        }
    }

    fun shutdownTts() {
        tts?.shutdown()
        tts = null
        currentPlayer?.let { it.stop(); it.release() }
        currentPlayer = null
    }

    @android.webkit.JavascriptInterface
    fun showBanner() {
        onMain { viewModel.setInGame(false) }
    }

    @android.webkit.JavascriptInterface
    fun hideBanner() {
        onMain { viewModel.setInGame(true) }
    }

    @android.webkit.JavascriptInterface
    fun toggleMusic(mute: Boolean) {
        onMain { MusicManager.setMuted(mute) }
    }

    @android.webkit.JavascriptInterface
    fun updateLanguage(lang: String) {
        onMain { viewModel.setLanguage(lang) }
    }

    @android.webkit.JavascriptInterface
    fun openParentalDashboard() {
        onMain { onOpenDashboard() }
    }

    @android.webkit.JavascriptInterface
    fun openKidzo() {
        onMain { onOpenKidzo() }
    }

    @android.webkit.JavascriptInterface
    fun askKidzo(query: String) {
        onMain { onAskKidzo(query) }
    }

    @android.webkit.JavascriptInterface
    fun openSleepScreen() {
        evalJs("window.openSleep && window.openSleep()")
    }

    @android.webkit.JavascriptInterface
    fun gameLaunched(@Suppress("UNUSED_PARAMETER") gameId: String) {
        onMain { viewModel.setInGame(true) }
    }

    @android.webkit.JavascriptInterface
    fun generateStory(lang: String, ageRange: String, childName: String, scenario: String) {
        scope.launch {
            try {
                val story = StoryGenerator.generate(lang, ageRange, childName, scenario)
                onMain {
                    val payload = JSONObject().put("title", story.title).put("text", story.text).toString()
                    evalJs("window.onAiStoryReady(${JSONObject.quote(payload)})")
                }
            } catch (e: Exception) {
                onMain {
                    evalJs("window.onAiStoryError && window.onAiStoryError()")
                }
            }
        }
    }
}
