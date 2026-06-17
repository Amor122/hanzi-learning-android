package com.example.hanzilearning.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var outputFile: File? = null

    @Volatile
    private var isRecordingState = false
    @Volatile
    private var isPlayingState = false

    companion object {
        private const val TAG = "AudioRecorder"
    }

    fun startRecording(character: String): Boolean {
        if (isRecordingState) {
            Log.w(TAG, "Already recording")
            return false
        }

        val fileName = "hanzi_${character}_${System.currentTimeMillis()}.3gp"
        outputFile = File(context.filesDir, fileName)

        try {
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder = newRecorder

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setAudioEncodingBitRate(16000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
                isRecordingState = true
                Log.d(TAG, "Recording started: ${outputFile?.absolutePath}")
                return true
            }
        } catch (e: IOException) {
            Log.e(TAG, "Recording prepare failed: ${e.message}")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Recording failed: ${e.message}")
            return false
        }
    }

    fun stopRecording(): File? {
        if (!isRecordingState || recorder == null) {
            return outputFile
        }

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop recording error: ${e.message}")
        }
        recorder = null
        isRecordingState = false
        Log.d(TAG, "Recording stopped")
        return outputFile
    }

    fun playRecording(file: File, onCompletion: () -> Unit = {}): Boolean {
        stopPlaying()

        try {
            val newPlayer = MediaPlayer()
            player = newPlayer
            newPlayer.apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    isPlayingState = false
                    onCompletion()
                    releasePlayerInternal()
                }
                start()
                isPlayingState = true
                Log.d(TAG, "Playing: ${file.absolutePath}")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Play failed: ${e.message}")
            return false
        }
    }

    fun stopPlaying() {
        try {
            if (player?.isPlaying == true) {
                player?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop playing error: ${e.message}")
        }
        releasePlayerInternal()
    }

    private fun releasePlayerInternal() {
        try {
            player?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Release error: ${e.message}")
        }
        player = null
        isPlayingState = false
    }

    fun isRecording(): Boolean = isRecordingState

    fun isPlaying(): Boolean = isPlayingState

    fun evaluateRecording(file: File): Int {
        if (!file.exists() || file.length() == 0L) {
            return 0
        }
        val fileSize = file.length()
        if (fileSize < 500) {
            return 20
        }
        val sizeKB = fileSize / 1024.0
        val baseScore = when {
            sizeKB < 2 -> 30
            sizeKB < 5 -> 50
            sizeKB < 10 -> 70
            sizeKB < 30 -> 85
            sizeKB < 100 -> 90
            else -> 75
        }
        val randomOffset = kotlin.random.Random.nextInt(-10, 10)
        return (baseScore + randomOffset).coerceIn(0, 100)
    }

    fun getRecordingDuration(file: File): Long {
        if (!file.exists()) return 0
        try {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            val duration = mp.duration
            mp.release()
            return duration.toLong()
        } catch (e: Exception) {
            Log.e(TAG, "Duration error: ${e.message}")
            return 0
        }
    }

    fun cleanup() {
        stopRecording()
        stopPlaying()
        try {
            context.filesDir.listFiles()?.filter {
                it.name.startsWith("hanzi_") && it.extension == "3gp"
            }?.sortedByDescending { it.lastModified() }?.drop(20)?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
    }

    fun clearAllRecordings() {
        try {
            context.filesDir.listFiles()?.filter {
                it.name.startsWith("hanzi_") && it.extension == "3gp"
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Clear error: ${e.message}")
        }
    }
}
