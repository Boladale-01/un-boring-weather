package com.unboring.weather.feedback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthesizes crisp tactile sound effects (scrub ticks, cloud pops, thunder rumbles)
 * without requiring external audio asset files.
 */
class WeatherAudio {

  var enabled: Boolean = true
  private val sampleRate = 22050
  private var lastTickTimeMs = 0L

  fun playScrubTick() {
    if (!enabled) return
    val now = System.currentTimeMillis()
    if (now - lastTickTimeMs < 38) return
    lastTickTimeMs = now
    playPcmAsync(durationMs = 18) { i, total ->
      val t = i.toDouble() / sampleRate
      val env = exp(-t * 240.0)
      (sin(2.0 * PI * 920.0 * t) * env * 0.25 * Short.MAX_VALUE).toInt().toShort()
    }
  }

  fun playCloudPop() {
    if (!enabled) return
    playPcmAsync(durationMs = 85) { i, _ ->
      val t = i.toDouble() / sampleRate
      val freq = 260.0 + 420.0 * exp(-t * 28.0)
      val env = exp(-t * 42.0)
      (sin(2.0 * PI * freq * t) * env * 0.35 * Short.MAX_VALUE).toInt().toShort()
    }
  }

  fun playThunder() {
    if (!enabled) return
    var lp = 0.0
    playPcmAsync(durationMs = 450) { i, _ ->
      val t = i.toDouble() / sampleRate
      val noise = Random.nextDouble(-1.0, 1.0)
      lp = lp * 0.92 + noise * 0.08
      val env = exp(-t * 4.5)
      val sub = sin(2.0 * PI * 54.0 * t) * 0.35
      ((lp * 1.8 + sub) * env * 0.55 * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
  }

  private fun playPcmAsync(durationMs: Int, generator: (index: Int, total: Int) -> Short) {
    thread(isDaemon = true) {
      try {
        val totalSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
          buffer[i] = generator(i, totalSamples)
        }
        val minBuf = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(maxOf(minBuf, totalSamples * 2))
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        track.write(buffer, 0, totalSamples)
        track.play()
        Thread.sleep(durationMs.toLong() + 30L)
        track.stop()
        track.release()
      } catch (_: Exception) {
      }
    }
  }
}
