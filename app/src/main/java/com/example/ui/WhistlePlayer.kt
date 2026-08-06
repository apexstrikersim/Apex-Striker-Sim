package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WhistlePlayer {
    private var mediaPlayer: MediaPlayer? = null

    suspend fun playWhistle(context: Context, pattern: String, volume: Float) = withContext(Dispatchers.Main) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            val resId = when (pattern) {
                "kickoff", "extra" -> R.raw.kick_off
                "halftime" -> R.raw.half_time
                "fulltime", "pens" -> R.raw.full_time
                else -> R.raw.kick_off
            }

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer = MediaPlayer.create(context, resId, attrs, 0)?.apply {
                setVolume(volume, volume)
                setOnCompletionListener { player ->
                    player.release()
                    if (mediaPlayer == player) {
                        mediaPlayer = null
                    }
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }
}
