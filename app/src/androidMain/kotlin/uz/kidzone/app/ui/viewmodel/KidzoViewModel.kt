package uz.kidzone.app.ui.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.kidzone.app.ai.AishaSpeechGenerator
import uz.kidzone.app.ai.KidzoCompanionEngine
import uz.kidzone.app.ai.KidzoVoiceRecognizer
import uz.kidzone.app.arch.ViewModel
import uz.kidzone.app.kidzo.ContentCard
import uz.kidzone.app.kidzo.KidzoAgent
import uz.kidzone.app.kidzo.KidzoState
import java.io.File
import java.util.Locale

class KidzoViewModel(val agent: KidzoAgent) : ViewModel() {

    private val TAG = "KidzoViewModel"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val state: StateFlow<KidzoState> = agent.state
    val cards: StateFlow<List<ContentCard>> = agent.cards

    private val initialGreeting = "Salom, do'stim! 🐥 Men Kidzo bo'laman. Nima haqida gaplashamiz?"

    private val _messages = MutableStateFlow<List<Pair<Boolean, String>>>(
        listOf(false to initialGreeting)
    )
    val messages: StateFlow<List<Pair<Boolean, String>>> = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenText = MutableStateFlow(initialGreeting)
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private var recognizer: KidzoVoiceRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var ttsEngine: TextToSpeech? = null
    private var ttsReady = false

    fun requestRecommendations() { agent.requestRecommendations() }

    fun askKidzo(query: String, lang: String, childName: String, context: Context) {
        if (query.isBlank()) return
        Log.d(TAG, "askKidzo requested: query='$query', lang='$lang', childName='$childName'")
        stopVoiceInput()
        stopSpeaking()

        // Add user message
        _messages.value = _messages.value + (true to query)
        _isThinking.value = true

        scope.launch {
            val reply = try {
                KidzoCompanionEngine.ask(query, lang, childName)
            } catch (e: Exception) {
                Log.e(TAG, "Error in KidzoCompanionEngine: ${e.message}", e)
                "Seni eshitdim, $childName! 🐥 Keling, birgalikda quvnoq o'yin o'ynaymiz! ✨"
            }

            Log.d(TAG, "Kidzo replied: '$reply'")
            _isThinking.value = false
            _messages.value = _messages.value + (false to reply)
            _lastSpokenText.value = reply

            speakText(reply, lang, context)
        }
    }

    fun startVoiceInput(context: Context, lang: String, childName: String) {
        stopSpeaking()
        if (_isListening.value) {
            stopVoiceInput()
            return
        }

        recognizer?.stopListening()
        recognizer = KidzoVoiceRecognizer(
            context = context,
            onSpeechResult = { text ->
                Log.d(TAG, "Speech recognized: $text")
                _isListening.value = false
                askKidzo(text, lang, childName, context)
            },
            onListeningStateChanged = { listening ->
                _isListening.value = listening
            },
            onRmsChanged = { rms ->
                _rmsLevel.value = (rms.coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
            },
            onError = { err ->
                Log.w(TAG, "Recognizer error: $err")
                _isListening.value = false
            }
        )
        recognizer?.startListening(lang)
    }

    fun stopVoiceInput() {
        recognizer?.stopListening()
        recognizer = null
        _isListening.value = false
        _rmsLevel.value = 0f
    }

    fun speakText(text: String, lang: String, context: Context) {
        if (text.isBlank()) return
        Log.d(TAG, "speakText: text='$text', lang='$lang'")
        stopSpeaking()
        _isSpeaking.value = true

        scope.launch {
            var cloudPlayed = false
            if (lang == "uz") {
                val cacheDir = context.cacheDir
                val audioFile = AishaSpeechGenerator.synthesize(text, cacheDir)
                cloudPlayed = audioFile != null && playAudioFile(audioFile)
            }
            if (!cloudPlayed) {
                withContext(Dispatchers.Main) {
                    speakViaDeviceTts(text, lang, context)
                }
            }
        }
    }

    private suspend fun playAudioFile(file: File): Boolean = withContext(Dispatchers.Main) {
        try {
            mediaPlayer?.release()
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener {
                _isSpeaking.value = false
                it.release()
                mediaPlayer = null
                file.delete()
            }
            mp.setOnErrorListener { it, _, _ ->
                _isSpeaking.value = false
                it.release()
                mediaPlayer = null
                file.delete()
                true
            }
            mp.prepare()
            mediaPlayer = mp
            mp.start()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Audio playback error: ${e.message}")
            _isSpeaking.value = false
            false
        }
    }

    private fun speakViaDeviceTts(text: String, lang: String, context: Context) {
        if (ttsEngine == null) {
            ttsEngine = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    performDeviceSpeak(text, lang)
                } else {
                    Log.w(TAG, "TTS initialization failed: status=$status")
                    _isSpeaking.value = false
                }
            }
        } else if (ttsReady) {
            performDeviceSpeak(text, lang)
        }
    }

    private fun performDeviceSpeak(text: String, lang: String) {
        val engine = ttsEngine ?: return
        val targetLocale = when (lang.lowercase()) {
            "ru" -> Locale("ru", "RU")
            "en" -> Locale.US
            else -> Locale("uz", "UZ")
        }

        fun unsupported(r: Int) =
            r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED

        var result = engine.setLanguage(targetLocale)
        // Fallback for Uzbek since Android Google TTS does not ship an Uzbek voice
        if (lang != "ru" && lang != "en" && unsupported(result)) {
            result = engine.setLanguage(Locale("tr", "TR"))
            if (unsupported(result)) result = engine.setLanguage(Locale("ru", "RU"))
        }
        if (unsupported(result)) {
            engine.setLanguage(Locale.getDefault())
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS onStart: $utteranceId")
                _isSpeaking.value = true
            }
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS onDone: $utteranceId")
                _isSpeaking.value = false
            }
            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) {
                Log.w(TAG, "TTS onError: $utteranceId")
                _isSpeaking.value = false
            }
        })

        // Clean emojis from text so TTS engine pronounces words cleanly
        val cleanText = text.replace(Regex("[\\p{So}\\p{Cn}]"), "").trim()
        val speechText = cleanText.ifBlank { text }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        engine.stop()
        engine.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, "kidzoVoice_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        _isSpeaking.value = false
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
            ttsEngine?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping speech: ${e.message}")
        }
    }

    fun repeatSpeech(context: Context, lang: String) {
        val text = _lastSpokenText.value.ifBlank { initialGreeting }
        speakText(text, lang, context)
    }

    override fun onCleared() {
        super.onCleared()
        stopVoiceInput()
        stopSpeaking()
        ttsEngine?.shutdown()
        ttsEngine = null
    }
}
