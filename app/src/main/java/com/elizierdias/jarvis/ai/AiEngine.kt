package com.elizierdias.jarvis.ai

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class AiEngine(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("groq_api_key", "") ?: ""
        set(value) = prefs.edit().putString("groq_api_key", value.trim()).apply()

    private val model = "openai/gpt-oss-20b"

    private val systemPrompt = """
        És o J.A.R.V.I.S., o assistente pessoal do utilizador, inspirado no Jarvis do Tony Stark.
        Fala sempre em português de Portugal, de forma elegante, confiante e útil.
        Respostas curtas e diretas (máximo 2-3 frases), a menos que o utilizador peça detalhes.
        Podes usar humor subtil e inteligência.
        Se não souberes algo, admite com elegância.
        Nunca digas que és uma IA genérica — és o Jarvis.
    """.trimIndent()

    suspend fun reply(userText: String): String = withContext(Dispatchers.IO) {
        val t = userText.trim()
        if (t.isEmpty()) return@withContext "Não apanhei o que disseste."

        localCommand(t)?.let { return@withContext it }

        if (apiKey.isBlank()) {
            return@withContext "Ainda não configurei o meu cérebro. " +
                "Toca longamente no título J.A.R.V.I.S. para colar a tua chave gratuita do Groq."
        }

        try {
            callGroq(t)
        } catch (e: Exception) {
            "Desculpa, tive um problema a contactar o cérebro: ${e.message?.take(80)}"
        }
    }

    private fun localCommand(raw: String): String? {
        val t = raw.lowercase().trim()

        // --- Saudações ---
        if (t.contains("olá") || t.contains("ola") || t.contains("hey jarvis") ||
            t.contains("oi jarvis") || t == "jarvis" || t.contains("bom dia") ||
            t.contains("boa tarde") || t.contains("boa noite")
        ) return "Às suas ordens. Em que posso ajudar?"

        // --- Horas / Data ---
        if (t.contains("horas") || t.contains("que horas") || t.contains("hora é")) {
            val cal = Calendar.getInstance()
            return "São %02d:%02d, senhor.".format(
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)
            )
        }
        if (t.contains("data") || t.contains("dia é hoje") || t.contains("que dia")) {
            val cal = Calendar.getInstance()
            return "Hoje é %02d/%02d/%d.".format(
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR)
            )
        }

        // --- Despertador / Alarme ---
        if (t.contains("alarme") || t.contains("despertador") || t.contains("acordar")) {
            return setAlarmFromText(t)
        }
        if (t.contains("temporizador") || t.contains("timer") || t.contains("cronómetro") || t.contains("cronometro")) {
            return setTimerFromText(t)
        }

        // --- Música ---
        if (t.contains("música") || t.contains("musica") || t.contains("toca") ||
            t.contains("play") || t.contains("spotify") || t.contains("ouvir")
        ) {
            return playMusic(t)
        }
        if (t.contains("pausa") || t.contains("pause") || t.contains("parar música") || t.contains("para a música")) {
            return pauseMusic()
        }
        if (t.contains("próxima") || t.contains("proxima") || t.contains("seguinte") || t.contains("next")) {
            return skipTrack()
        }

        // --- Volume ---
        if (t.contains("volume mais") || t.contains("aumenta o volume") || t.contains("mais alto")) {
            return changeVolume(true)
        }
        if (t.contains("volume menos") || t.contains("diminui o volume") || t.contains("mais baixo")) {
            return changeVolume(false)
        }

        // --- Apps / Definições ---
        if (t.contains("definições") || t.contains("configurações") || t.contains("settings")) {
            return openSettings()
        }
        if (t.contains("youtube")) return openApp("com.google.android.youtube", "YouTube")
        if (t.contains("chrome") || t.contains("navegador")) return openApp("com.android.chrome", "Chrome")
        if (t.contains("whatsapp")) return openApp("com.whatsapp", "WhatsApp")
        if (t.contains("instagram")) return openApp("com.instagram.android", "Instagram")
        if (t.contains("câmara") || t.contains("camera") || t.contains("foto")) {
            return openAppByAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE, "câmara")
        }

        // --- Identidade ---
        if (t.contains("quem és") || t.contains("quem e voce") || t.contains("quem é você") || t.contains("o que és"))
            return "Sou o Jarvis, o teu assistente pessoal. Sempre às tuas ordens."
        if (t.contains("obrigado") || t.contains("obrigada"))
            return "De nada. Estou sempre aqui."
        if (t.contains("ajuda") || t.contains("o que podes") || t.contains("comandos"))
            return "Posso dizer as horas, pôr alarmes, tocar música, controlar o volume, " +
                "abrir apps (YouTube, WhatsApp, Chrome…) e conversar quando o cérebro estiver ligado."

        return null
    }

    private fun setAlarmFromText(t: String): String {
        // Procura padrões tipo "7:30", "7 e meia", "8 horas"
        val hourMin = Regex("(\\d{1,2})\\s*[:hH]\\s*(\\d{1,2})?").find(t)
        val hourOnly = Regex("(\\d{1,2})\\s*horas?").find(t)

        val hour: Int
        val minute: Int
        when {
            hourMin != null -> {
                hour = hourMin.groupValues[1].toIntOrNull() ?: return "Diz a hora, por exemplo: alarme para as 7:30"
                minute = hourMin.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            }
            hourOnly != null -> {
                hour = hourOnly.groupValues[1].toIntOrNull() ?: return "Diz a hora, por exemplo: alarme para as 7 horas"
                minute = if (t.contains("meia")) 30 else 0
            }
            else -> return "Diz a hora do alarme, por exemplo: alarme para as 7:30"
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "A configurar o alarme para as %02d:%02d.".format(hour, minute)
        } catch (_: Exception) {
            "Não consegui abrir o despertador. Verifica se tens uma app de relógio instalada."
        }
    }

    private fun setTimerFromText(t: String): String {
        val mins = Regex("(\\d+)\\s*min").find(t)?.groupValues?.get(1)?.toIntOrNull()
        val secs = Regex("(\\d+)\\s*seg").find(t)?.groupValues?.get(1)?.toIntOrNull()
        val total = when {
            mins != null -> mins * 60
            secs != null -> secs
            else -> return "Diz quanto tempo, por exemplo: temporizador 5 minutos"
        }
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, total)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Temporizador de $total segundos a iniciar."
        } catch (_: Exception) {
            "Não consegui iniciar o temporizador."
        }
    }

    private fun playMusic(t: String): String {
        // Extrai possível nome da música/artista
        val query = t
            .replace(Regex("(toca|play|música|musica|spotify|ouvir|põe|poe)", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { null }

        // Tenta Media Play From Search (funciona com Spotify, YT Music, etc.)
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                putExtra(SearchManager.QUERY, query ?: "")
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Fallbacks manuais se o intent genérico falhar
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                if (query != null) "A procurar «$query» no leitor de música."
                else "A retomar a música."
            } else {
                // Abre Spotify ou YT Music
                when {
                    isInstalled("com.spotify.music") -> {
                        openApp("com.spotify.music", "Spotify")
                        "A abrir o Spotify."
                    }
                    isInstalled("com.google.android.apps.youtube.music") -> {
                        openApp("com.google.android.apps.youtube.music", "YouTube Music")
                        "A abrir o YouTube Music."
                    }
                    else -> "Não encontrei um leitor de música. Instala Spotify ou YouTube Music."
                }
            }
        } catch (_: Exception) {
            "Não consegui controlar a música."
        }
    }

    private fun pauseMusic(): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Envia evento de media pause via AudioManager (funciona na maioria dos players)
            val event = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            am.dispatchMediaKeyEvent(event)
            val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            am.dispatchMediaKeyEvent(eventUp)
            "Música em pausa."
        } catch (_: Exception) {
            "Não consegui pausar a música."
        }
    }

    private fun skipTrack(): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            am.dispatchMediaKeyEvent(down)
            am.dispatchMediaKeyEvent(up)
            "A passar para a próxima faixa."
        } catch (_: Exception) {
            "Não consegui passar a faixa."
        }
    }

    private fun changeVolume(up: Boolean): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val dir = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
            if (up) "Volume aumentado." else "Volume diminuído."
        } catch (_: Exception) {
            "Não consegui alterar o volume."
        }
    }

    private fun openSettings(): String {
        return try {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "A abrir as definições do telemóvel."
        } catch (_: Exception) {
            "Não consegui abrir as definições."
        }
    }

    private fun openApp(packageName: String, label: String): String {
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                "A abrir $label."
            } else {
                "$label não está instalado."
            }
        } catch (_: Exception) {
            "Não consegui abrir $label."
        }
    }

    private fun openAppByAction(action: String, label: String): String {
        return try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "A abrir a $label."
        } catch (_: Exception) {
            "Não consegui abrir a $label."
        }
    }

    private fun isInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun callGroq(userText: String): String {
        val url = URL("https://api.groq.com/openai/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.7)
            put("max_tokens", 512)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", userText))
            })
        }

        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) {
            return try {
                val err = JSONObject(response).optJSONObject("error")?.optString("message")
                "Erro do Groq ($code): ${err ?: response.take(120)}"
            } catch (_: Exception) {
                "Erro do Groq ($code). Verifica a chave API."
            }
        }

        val json = JSONObject(response)
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }

    // Imports em falta usados acima
    private object MediaStore {
        const val INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH"
        const val EXTRA_MEDIA_FOCUS = "android.intent.extra.focus"
    }
    private object SearchManager {
        const val QUERY = "query"
    }
}
