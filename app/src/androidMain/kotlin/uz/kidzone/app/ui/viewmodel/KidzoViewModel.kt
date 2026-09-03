package uz.kidzone.app.ui.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
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

    val state: StateFlow<KidzoState> = agent.state
    val cards: StateFlow<List<ContentCard>> = agent.cards

    private val _messages = MutableStateFlow<List<Pair<Boolean, String>>>(
        listOf(false to "Salom, do'stim! 🐥 Men Kidzo bo'laman. Nima haqida gaplashamiz?")
    )
    val messages: StateFlow<List<Pair<Boolean, String>>> = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
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
        stopVoiceInput()
        stopSpeaking()

        // Add user message
        _messages.value = _messages.value + (true to query)
        _isThinking.value = true

        viewModelScope.launch {
            val reply = try {
                KidzoCompanionEngine.ask(query, lang, childName)
            } catch (e: Exception) {
                "Seni eshitdim, $childName! 🐥 Keling, birgalikda quvnoq o'yin o'ynaymiz! ✨"
            }

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
                _isListening.value = false
                askKidzo(text, lang, childName, context)
            },
            onListeningStateChanged = { listening ->
                _isListening.value = listening
            },
            onRmsChanged = { rms ->
                _rmsLevel.value = (rms.coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
            },
            onError = { _ ->
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
        stopSpeaking()
        _isSpeaking.value = true

        viewModelScope.launch {
            if (lang == "uz") {
                // Try Aisha TTS first
                val cacheDir = context.cacheDir
                val audioFile = AishaSpeechGenerator.synthesize(text, cacheDir)
                val played = audioFile != null && playAudioFile(audioFile)
                if (!played) {
                    speakViaDeviceTts(text, lang, context)
                }
            } else {
                speakViaDeviceTts(text, lang, context)
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
                    _isSpeaking.value = false
                }
            }
        } else if (ttsReady) {
            performDeviceSpeak(text, lang)
        }
    }

    private fun performDeviceSpeak(text: String, lang: String) {
        val engine = ttsEngine ?: return
        val locale = when (lang.lowercase()) {
            "ru" -> Locale("ru", "RU")
            "en" -> Locale.US
            else -> Locale("uz", "UZ")
        }
        engine.language = locale
        engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
            override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) { _isSpeaking.value = false }
        })
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kidzoVoice")
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
        val text = _lastSpokenText.value
        if (text.isNotBlank()) {
            speakText(text, lang, context)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopVoiceInput()
        stopSpeaking()
        ttsEngine?.shutdown()
        ttsEngine = null
    }
}
