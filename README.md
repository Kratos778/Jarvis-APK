# Jarvis APK

Assistente **Android** em Kotlin — chat + voz em português + cérebro Groq (gratuito).

```
Package: com.elizierdias.jarvis
APK:     Jarvis.apk
```

## O que faz agora

- Interface de chat escura estilo Jarvis
- **Ouvir** (SpeechRecognizer do sistema, pt-PT / pt-BR)
- **Falar** (TextToSpeech do sistema)
- Comandos locais (horas, data, abrir definições, etc.)
- **Cérebro real**: Groq (Llama 3.1 8B Instant) — gratuito e muito rápido
- A chave API fica **contigo** (não está hardcoded no APK)

## Como configurar o cérebro (Groq)

1. Vai a [console.groq.com](https://console.groq.com) e cria uma conta grátis
2. Menu **API Keys** → **Create API Key**
3. Copia a chave (começa com `gsk_`)
4. No app, **toca longamente** no título **J.A.R.V.I.S.**
5. Cola a chave e guarda

Pronto. O Jarvis passa a responder com inteligência real.

> Limites do free tier (aproximados): ~30 pedidos/minuto e vários milhares por dia — mais que suficiente para uso pessoal.

## Build do APK

### Android Studio
1. File → Open → esta pasta
2. Sync Gradle
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. Saída: `app/build/outputs/apk/debug/Jarvis.apk`

### GitHub Actions
O workflow já está configurado. Faz push para `main` ou corre manualmente na aba **Actions**.

## Permissões

- Microfone (voz)
- Internet (só quando usas o Groq)

## Estrutura

```
com.elizierdias.jarvis/
├── MainActivity.kt
├── ai/AiEngine.kt          ← comandos locais + Groq
├── voice/VoiceManager.kt
└── ui/...
```

## Repo irmão (PC)

Setup OpenJarvis no computador: [Jarvis-Local](https://github.com/Kratos778/Jarvis-Local)
