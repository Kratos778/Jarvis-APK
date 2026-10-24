package com.elizierdias.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())

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

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (recognizer == null) {
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                onError("Reconhecimento de voz não disponível. Instala o Google ou ativa o serviço de voz.")
                return null
            }
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { sr ->
                sr.setRecognitionListener(listener)
            }
        }
        return recognizer
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio do microfone"
                SpeechRecognizer.ERROR_CLIENT -> "Erro do cliente de voz"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sem permissão de microfone"
                SpeechRecognizer.ERROR_NETWORK -> "Sem internet para reconhecimento de voz"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de rede na voz"
                SpeechRecognizer.ERROR_NO_MATCH -> "Não percebi. Tenta outra vez."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Serviço de voz ocupado"
                SpeechRecognizer.ERROR_SERVER -> "Erro no servidor de voz"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Não ouvi nada. Fala mais perto."
                11 -> { // ERROR_SERVER_DISCONNECTED / TOO_MANY_REQUESTS
                    // Recria o recognizer na próxima tentativa
                    try { recognizer?.destroy() } catch (_: Exception) {}
                    recognizer = null
                    "Serviço de voz desligou. Toca no microfone outra vez."
                }
                else -> "Erro de voz ($error)"
            }
            onError(msg)
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
    }

    fun startListening() {
        val sr = ensureRecognizer() ?: return
        try {
            sr.cancel()
        } catch (_: Exception) {}

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Tenta português; o sistema faz fallback se não tiver o modelo
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }

        // Pequeno atraso evita ERROR_SERVER_DISCONNECTED ao reutilizar o serviço
        mainHandler.postDelayed({
            try {
                sr.startListening(intent)
            } catch (e: Exception) {
                onError("Não consegui iniciar a voz: ${e.message?.take(40)}")
            }
        }, 150)
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
            recognizer?.cancel()
        } catch (_: Exception) { }
        // Não destruímos aqui — reutilizamos a instância (evita erro 11)
    }

    fun speak(text: String) {
        val engine = tts ?: return
        if (!ttsReady) return
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-utt")
    }

    fun shutdown() {
        mainHandler.removeCallbacksAndMessages(null)
        stopListening()
        try {
            recognizer?.destroy()
        } catch (_: Exception) { }
        recognizer = null
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) { }
        tts = null
        ttsReady = false
    }
}
