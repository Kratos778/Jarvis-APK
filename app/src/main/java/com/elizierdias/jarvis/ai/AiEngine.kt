package com.elizierdias.jarvis.ai

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * Motor do Jarvis.
 * - Comandos locais do telemóvel
 * - Cérebro principal: Groq (gratuito) — o utilizador cola a sua própria chave
 */
class AiEngine(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("groq_api_key", "") ?: ""
        set(value) = prefs.edit().putString("groq_api_key", value.trim()).apply()

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

        // 1. Comandos locais (não precisam de internet)
        localCommand(t)?.let { return@withContext it }

        // 2. Se não tem chave, avisa
        if (apiKey.isBlank()) {
            return@withContext "Ainda não configurei o meu cérebro. " +
                "Vai a Definições → cola a tua chave gratuita do Groq (console.groq.com) e volta a falar comigo."
        }

        // 3. Chama o Groq
        try {
            callGroq(t)
        } catch (e: Exception) {
            "Desculpa, tive um problema a contactar o cérebro: ${e.message?.take(80)}"
        }
    }

    private fun localCommand(raw: String): String? {
        val t = raw.lowercase()

        return when {
            t.contains("olá") || t.contains("ola") || t.contains("hey jarvis") ||
                t.contains("oi jarvis") || t == "jarvis" || t.contains("bom dia") ||
                t.contains("boa tarde") || t.contains("boa noite") ->
                "Às suas ordens. Em que posso ajudar?"

            t.contains("horas") || t.contains("que horas") || t.contains("hora é") -> {
                val cal = Calendar.getInstance()
                "São %02d:%02d, senhor.".format(
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE)
                )
            }

            t.contains("data") || t.contains("dia é hoje") || t.contains("que dia") -> {
                val cal = Calendar.getInstance()
                "Hoje é %02d/%02d/%d.".format(
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.YEAR)
                )
            }

            t.contains("definições") || t.contains("configurações") || t.contains("settings") -> {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    "A abrir as definições do telemóvel."
                } catch (_: Exception) {
                    "Não consegui abrir as definições."
                }
            }

            t.contains("quem és") || t.contains("quem e voce") || t.contains("quem é você") ||
                t.contains("o que és") ->
                "Sou o Jarvis, o teu assistente pessoal. Sempre às tuas ordens."

            t.contains("obrigado") || t.contains("obrigada") ->
                "De nada. Estou sempre aqui."

            t.contains("ajuda") || t.contains("o que podes") || t.contains("comandos") ->
                "Posso conversar, dizer as horas e a data, abrir as definições do telemóvel, " +
                    "e responder a quase qualquer pergunta quando o cérebro (Groq) estiver configurado."

            else -> null
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
            put("model", "llama-3.1-8b-instant") // rápido e generoso no free tier
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
            // Tenta extrair mensagem de erro útil
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
}
