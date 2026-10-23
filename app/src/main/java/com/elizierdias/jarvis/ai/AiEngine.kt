package com.elizierdias.jarvis.ai

import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Motor leve de respostas — 100% local por defeito.
 * Para ligar LLM na nuvem (Grok, OpenAI, etc.), implementa [callRemoteLlm].
 */
class AiEngine(private val context: Context) {

    suspend fun reply(userText: String): String = withContext(Dispatchers.Default) {
        val t = userText.trim().lowercase()

        when {
            t.isEmpty() -> "Não apanhei o que disseste."

            t.contains("olá") || t.contains("ola") || t.contains("hey jarvis") ||
                t.contains("oi jarvis") || t == "jarvis" ->
                "Às suas ordens. Em que posso ajudar?"

            t.contains("horas") || t.contains("que horas") || t.contains("hora é") -> {
                val cal = Calendar.getInstance()
                val h = cal.get(Calendar.HOUR_OF_DAY)
                val m = cal.get(Calendar.MINUTE)
                "São %02d:%02d.".format(h, m)
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
                    "A abrir as definições."
                } catch (_: Exception) {
                    "Não consegui abrir as definições."
                }
            }

            t.contains("quem és") || t.contains("quem e voce") || t.contains("quem é você") ->
                "Sou o Jarvis, o teu assistente no telemóvel. Leve, em português, sempre às tuas ordens."

            t.contains("obrigado") || t.contains("obrigada") ->
                "De nada."

            t.contains("ajuda") || t.contains("o que podes") ->
                "Podes perguntar as horas, a data, pedir para abrir definições, ou conversar. " +
                    "Usa o microfone ou escreve."

            else -> {
                // Ponto de extensão: API remota
                callRemoteLlm(userText) ?: (
                    "Percebi: «$userText». " +
                        "Ainda estou em modo local simples — liga uma API em AiEngine para respostas mais inteligentes."
                )
            }
        }
    }

    /**
     * Implementa aqui a chamada HTTP ao teu LLM (Grok, OpenAI, etc.).
     * Devolve null para usar a resposta local de fallback.
     */
    private suspend fun callRemoteLlm(userText: String): String? {
        // Exemplo futuro:
        // val key = BuildConfig.API_KEY
        // return httpPost(...)
        return null
    }
}
