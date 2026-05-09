package com.voice0.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records audio to an .m4a file in the app cache directory using MediaRecorder.
 * Designed for press-and-hold UX: start() begins, stop() returns the file.
 */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        check(recorder == null) { "Recorder already running" }
        val file = File.createTempFile("rec_", ".m4a", context.cacheDir)
        outputFile = file

        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(64_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = r
        return file
    }

    /** Returns the recorded file (caller is responsible for deletion). */
    fun stop(): File {
        val r = recorder ?: error("Recorder not running")
        var stopFailed = false
        try {
            r.stop()
        } catch (_: RuntimeException) {
            stopFailed = true
        }
        r.release()
        recorder = null
        val file = outputFile ?: error("No output file")
        outputFile = null
        if (stopFailed || file.length() < 1024) {
            file.delete()
            error("Recording too short — hold the button while speaking")
        }
        return file
    }

    fun isRecording(): Boolean = recorder != null

    fun cancel() {
        recorder?.runCatching { stop(); release() }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
