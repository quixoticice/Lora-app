package com.example.service

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AudioRecorderService(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tempAudioFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordDurationSec = MutableStateFlow(0f)
    val recordDurationSec: StateFlow<Float> = _recordDurationSec

    private val _recordingAmplitude = MutableStateFlow(0f)
    val recordingAmplitude: StateFlow<Float> = _recordingAmplitude

    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId: StateFlow<String?> = _playingAudioId

    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val maxRecordTimeSec = 4.0f // LoRa standard limit

    fun startRecording(onAutoStop: (base64Data: String, durationSec: Float) -> Unit) {
        if (_isRecording.value) return

        try {
            tempAudioFile = File.createTempFile("lora_ptt_", ".amr", context.cacheDir)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setAudioSamplingRate(8000)
                setAudioEncodingBitRate(4750) // Ultra low bitrate ~4.75kbps for LoRa transmission
                setOutputFile(tempAudioFile?.absolutePath)
                prepare()
                start()
            }

            _isRecording.value = true
            _recordDurationSec.value = 0f

            recordJob = scope.launch {
                val startTime = System.currentTimeMillis()
                while (isActive && _isRecording.value) {
                    delay(100)
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                    _recordDurationSec.value = elapsed

                    // Update amplitude for live waveform
                    val maxAmp = try { mediaRecorder?.maxAmplitude ?: 0 } catch (e: Exception) { 0 }
                    _recordingAmplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)

                    if (elapsed >= maxRecordTimeSec) {
                        // Auto-stop at max duration
                        val (base64, dur) = stopRecordingInternal()
                        if (base64.isNotEmpty()) {
                            onAutoStop(base64, dur)
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecordingInternal()
        }
    }

    fun stopRecording(): Pair<String, Float> {
        return stopRecordingInternal()
    }

    private fun stopRecordingInternal(): Pair<String, Float> {
        recordJob?.cancel()
        val duration = _recordDurationSec.value
        _isRecording.value = false
        _recordDurationSec.value = 0f
        _recordingAmplitude.value = 0f

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore stop errors if released early
        } finally {
            mediaRecorder = null
        }

        val base64Data = try {
            tempAudioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    val bytes = file.readBytes()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else ""
            } ?: ""
        } catch (e: Exception) {
            ""
        }

        return Pair(base64Data, duration)
    }

    fun cancelRecording() {
        recordJob?.cancel()
        _isRecording.value = false
        _recordDurationSec.value = 0f
        _recordingAmplitude.value = 0f

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaRecorder = null
        }
        tempAudioFile?.delete()
    }

    fun playAudio(audioId: String, base64Data: String, onFinished: () -> Unit = {}) {
        if (_playingAudioId.value == audioId) {
            stopPlayback()
            return
        }

        stopPlayback()

        try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val audioPlayFile = File(context.cacheDir, "play_$audioId.amr")
            FileOutputStream(audioPlayFile).use { it.write(audioBytes) }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPlayFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    _playingAudioId.value = null
                    it.release()
                    mediaPlayer = null
                    onFinished()
                }
                start()
            }
            _playingAudioId.value = audioId
        } catch (e: Exception) {
            e.printStackTrace()
            _playingAudioId.value = null
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
            _playingAudioId.value = null
        }
    }
}
