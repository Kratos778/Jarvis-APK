package com.elizierdias.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceManager(
    context: Context,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureLanguage()
                ttsReady = true
            } else {
                ttsReady = false
            }
        }
    }

    private fun configureLanguage() {
        val engine = tts ?: return
        val br = Locale("pt", "BR")
        val pt = Locale("pt", "PT")
        when {
            engine.isLanguageAvailable(br) >= TextToSpeech.LANG_AVAILABLE ->
                engine.language = br
            engine.isLanguageAvailable(pt) >= TextToSpeech.LANG_AVAILABLE ->
                engine.language = pt
            else ->
                engine.language = Locale.getDefault()
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError("Reconhecimento de voz não disponível neste telemóvel")
            return
        }
        stopListening()
        val sr = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                onError("Erro de voz ($error)")
            }
            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                onResult(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onPartial(text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-PT")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        sr.startListening(intent)
    }

    fun stopListening() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) { }
        recognizer = null
    }

    fun speak(text: String) {
        val engine = tts ?: return
        if (!ttsReady) return
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-utt")
    }

    fun shutdown() {
        stopListening()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) { }
        tts = null
        ttsReady = false
    }
}
