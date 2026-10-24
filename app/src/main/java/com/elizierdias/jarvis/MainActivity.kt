package com.elizierdias.jarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.elizierdias.jarvis.ai.AiEngine
import com.elizierdias.jarvis.databinding.ActivityMainBinding
import com.elizierdias.jarvis.ui.ChatAdapter
import com.elizierdias.jarvis.ui.ChatMessage
import com.elizierdias.jarvis.voice.VoiceManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var voice: VoiceManager
    private lateinit var ai: AiEngine
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else Toast.makeText(this, R.string.permission_mic, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ai = AiEngine(this)
        voice = VoiceManager(
            context = this,
            onPartial = { /* opcional */ },
            onResult = { text ->
                binding.status.text = "ONLINE"
                if (text.isNotBlank()) submitUserText(text)
            },
            onError = { msg ->
                binding.status.text = "ONLINE"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )

        adapter = ChatAdapter(messages)
        binding.chatList.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.chatList.adapter = adapter

        // Mensagem de boas-vindas
        val welcome = if (ai.apiKey.isBlank()) {
            "Às suas ordens. Ainda não tenho o cérebro ligado.\n" +
                "Toca longamente no título J.A.R.V.I.S. para colar a tua chave gratuita do Groq."
        } else {
            getString(R.string.welcome)
        }
        addMessage(ChatMessage(welcome, fromUser = false), speak = true)

        binding.btnSend.setOnClickListener {
            val text = binding.input.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                binding.input.text = null
                submitUserText(text)
            }
        }

        binding.btnMic.setOnClickListener {
            ensureMicAndListen()
        }

        // Toque longo no título = configurar chave API
        binding.title.setOnLongClickListener {
            showApiKeyDialog()
            true
        }
    }

    private fun showApiKeyDialog() {
        val input = EditText(this).apply {
            hint = "gsk_... (chave do console.groq.com)"
            setText(ai.apiKey)
            setPadding(48, 32, 48, 32)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0x99FFFFFF.toInt())
        }

        AlertDialog.Builder(this)
            .setTitle("Chave Groq (gratuita)")
            .setMessage("Cria uma conta grátis em console.groq.com → API Keys → Create API Key e cola aqui.")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                ai.apiKey = input.text.toString()
                val msg = if (ai.apiKey.isBlank()) {
                    "Chave removida. Estou em modo local."
                } else {
                    "Chave guardada. Cérebro online."
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                addMessage(ChatMessage(msg, fromUser = false), speak = true)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ensureMicAndListen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED -> startListening()
            else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        binding.status.text = getString(R.string.listening)
        voice.startListening()
    }

    private fun submitUserText(text: String) {
        addMessage(ChatMessage(text, fromUser = true), speak = false)
        binding.status.text = "A pensar…"
        lifecycleScope.launch {
            val reply = ai.reply(text)
            binding.status.text = "ONLINE"
            addMessage(ChatMessage(reply, fromUser = false), speak = true)
        }
    }

    private fun addMessage(msg: ChatMessage, speak: Boolean) {
        messages.add(msg)
        adapter.notifyItemInserted(messages.lastIndex)
        binding.chatList.scrollToPosition(messages.lastIndex)
        if (speak && !msg.fromUser) {
            voice.speak(msg.text)
        }
    }

    override fun onDestroy() {
        voice.shutdown()
        super.onDestroy()
    }
}
