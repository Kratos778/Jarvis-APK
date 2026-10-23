package com.elizierdias.jarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
            onPartial = { /* opcional: mostrar texto parcial */ },
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

        addMessage(ChatMessage(getString(R.string.welcome), fromUser = false), speak = true)

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
