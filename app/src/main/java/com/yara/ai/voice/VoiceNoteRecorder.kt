package com.yara.ai.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.File

class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var current: File? = null
    fun start(): File? = try {
        val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
        current = File(dir, "note_${System.currentTimeMillis()}.m4a")
        recorder = MediaRecorder().apply { setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(current!!.absolutePath); prepare(); start() }
        current
    } catch (_: Exception) { recorder = null; current = null; null }
    fun stop(): File? = try { recorder?.stop(); recorder?.release(); recorder = null; current } catch (_: Exception) { recorder?.release(); recorder = null; null }
    fun play(file: File): Boolean = try { player?.release(); player = MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start(); setOnCompletionListener { release(); player = null } }; true } catch (_: Exception) { false }
    fun release() { try { recorder?.release() } catch (_: Exception) {}; try { player?.release() } catch (_: Exception) {}; recorder = null; player = null }
}
