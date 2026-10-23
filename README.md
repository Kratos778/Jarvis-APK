# Jarvis APK

Assistente **Android** em Kotlin — chat + voz em português. Gera **APK** instalável.

```
Package: com.elizierdias.jarvis
APK:     Jarvis.apk
```

## O que faz

- Interface de chat
- **Ouvir** (SpeechRecognizer do sistema, pt-PT / pt-BR)
- **Falar** (TextToSpeech do sistema)
- Respostas locais simples (comandos + fallback)
- Slot para ligar API de LLM depois (Grok, OpenAI, etc.) em `AiEngine.kt`

> Isto **não** corre Gemma 4 dentro do telemóvel.  
> No telemóvel usamos STT/TTS nativos + motor de respostas leve (e opcionalmente API).

## Build do APK

### Android Studio
1. File → Open → esta pasta
2. Sync Gradle
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. Saída: `app/build/outputs/apk/debug/Jarvis.apk`

### Terminal
```bash
gradle assembleDebug
# → app/build/outputs/apk/debug/Jarvis.apk
```

Instala no telemóvel (mesma `applicationId` → atualiza por cima da versão antiga).

## Permissões

- Microfone (voz)
- Internet (só se ativares API no `AiEngine`)

## Estrutura

```
com.elizierdias.jarvis/
├── MainActivity.kt
├── ai/AiEngine.kt
├── voice/VoiceManager.kt
└── ui/...
```

## Repo irmão (PC)

Setup OpenJarvis no computador: [Jarvis-Local](https://github.com/Kratos778/Jarvis-Local)
