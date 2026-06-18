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

        // 强制清理之前的 recorder 资源，避免状态残留导致后续录音失败
        forceResetRecorder()
        stopPlaying()

        val fileName = "hanzi_${character}_${System.currentTimeMillis()}.3gp"
        outputFile = File(context.filesDir, fileName)

        var tempRecorder: MediaRecorder? = null
        try {
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            tempRecorder = newRecorder

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setAudioEncodingBitRate(16000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
            }
            // 只有完全成功后才赋值并更新状态
            recorder = newRecorder
            isRecordingState = true
            Log.d(TAG, "Recording started: ${outputFile?.absolutePath}")
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Recording prepare failed: ${e.message}")
            // 失败时释放半初始化的 recorder
            tempRecorder?.let { safeReleaseRecorder(it) }
            recorder = null
            outputFile = null
            isRecordingState = false
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Recording failed: ${e.message}")
            tempRecorder?.let { safeReleaseRecorder(it) }
            recorder = null
            outputFile = null
            isRecordingState = false
            return false
        }
    }

    fun stopRecording(): File? {
        val resultFile = outputFile
        if (!isRecordingState || recorder == null) {
            // 即使不在录音中，也强制重置 recorder 状态，避免残留
            forceResetRecorder()
            return resultFile
        }

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop recording error: ${e.message}")
            // 出错时也强制重置
            forceResetRecorder()
        }
        recorder = null
        isRecordingState = false
        Log.d(TAG, "Recording stopped")
        return resultFile
    }

    // 安全释放单个 MediaRecorder 实例（忽略所有异常）
    private fun safeReleaseRecorder(mr: MediaRecorder) {
        try {
            mr.stop()
        } catch (_: Exception) { }
        try {
            mr.release()
        } catch (_: Exception) { }
    }

    // 强制重置 recorder 状态：释放现有 recorder 并清空状态
    private fun forceResetRecorder() {
        recorder?.let {
            safeReleaseRecorder(it)
            recorder = null
        }
        isRecordingState = false
    }

    // 用于外部强制重置状态（切换字时调用）
    fun resetForNewCharacter() {
        forceResetRecorder()
        stopPlaying()
        outputFile = null
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
