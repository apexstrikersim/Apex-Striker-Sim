package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WhistlePlayer {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()
    private val isLoadedMap = mutableMapOf<Int, Boolean>()
    private val pendingPlayMap = mutableMapOf<Int, Float>()

    private fun getOrCreateSoundPool(): SoundPool {
        return soundPool ?: run {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build().also { pool ->
                    pool.setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            isLoadedMap[sampleId] = true
                            pendingPlayMap.remove(sampleId)?.let { vol ->
                                try {
                                    pool.play(sampleId, vol, vol, 1, 0, 1.0f)
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                    soundPool = pool
                }
        }
    }

    suspend fun playWhistle(context: Context, pattern: String, volume: Float) = withContext(Dispatchers.Main) {
        if (volume <= 0f) return@withContext

        val resId = when (pattern) {
            "kickoff", "extra" -> R.raw.kick_off
            "halftime" -> R.raw.half_time
            "fulltime", "pens" -> R.raw.full_time
            else -> R.raw.kick_off
        }

        try {
            val pool = getOrCreateSoundPool()
            var soundId = soundMap[resId]
            if (soundId == null) {
                soundId = pool.load(context, resId, 1)
                if (soundId != 0) {
                    soundMap[resId] = soundId
                    pendingPlayMap[soundId] = volume
                }
            } else {
                if (isLoadedMap[soundId] == true) {
                    pool.play(soundId, volume, volume, 1, 0, 1.0f)
                } else {
                    pendingPlayMap[soundId] = volume
                }
            }
        } catch (_: Throwable) {
            // Silently ignore audio playback failures if audio backend is absent
        }
    }

    fun release() {
        try {
            soundPool?.release()
        } catch (_: Exception) {}
        soundPool = null
        soundMap.clear()
        isLoadedMap.clear()
        pendingPlayMap.clear()
    }
}

