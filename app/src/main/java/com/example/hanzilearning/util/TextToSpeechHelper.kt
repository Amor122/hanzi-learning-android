package com.example.hanzilearning.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingSpeech: String? = null

    companion object {
        private const val TAG = "TTS_Hanzi"
    }

    init {
        initTTS()
    }

    private fun initTTS() {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // 尝试中文
                    val result = tts?.setLanguage(Locale.CHINESE)
                    when (result) {
                        TextToSpeech.LANG_AVAILABLE,
                        TextToSpeech.LANG_COUNTRY_AVAILABLE,
                        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                            isReady = true
                            tts?.setSpeechRate(0.75f)
                            tts?.setPitch(1.0f)
                            Log.d(TAG, "TTS ready with Chinese")
                            pendingSpeech?.let { speak(it) }
                            pendingSpeech = null
                        }
                        else -> {
                            // 尝试简体中文
                            val simpResult = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                            if (simpResult == TextToSpeech.LANG_AVAILABLE ||
                                simpResult == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                                isReady = true
                                tts?.setSpeechRate(0.75f)
                                Log.d(TAG, "TTS ready with Simplified Chinese")
                            } else {
                                // 最后尝试默认语言
                                isReady = true
                                Log.d(TAG, "TTS ready with default (Chinese may not work)")
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "TTS initialization failed: $status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS init error: ${e.message}")
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!isReady) {
            pendingSpeech = text
            Log.d(TAG, "TTS not ready, queuing: $text")
            return
        }
        try {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, queueMode, null, "hanzi_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "speak error: ${e.message}")
        }
    }

    fun speakWithHappyTone(text: String) {
        tts?.setPitch(1.2f)
        tts?.setSpeechRate(0.85f)
        speak(text)
        // 恢复默认
        tts?.setPitch(1.0f)
        tts?.setSpeechRate(0.75f)
    }

    fun speakSlow(text: String) {
        tts?.setPitch(1.0f)
        tts?.setSpeechRate(0.5f)
        speak(text)
        tts?.setSpeechRate(0.75f)
    }

    fun isAvailable(): Boolean = isReady

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
