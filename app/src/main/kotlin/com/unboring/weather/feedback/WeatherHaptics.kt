package com.unboring.weather.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class WeatherHaptics(context: Context) {

  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    mgr?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  var enabled: Boolean = true

  fun scrubTick() {
    if (!enabled) return
    vibrateOneShot(10L, 75)
  }

  fun cloudPop() {
    if (!enabled) return
    vibrateWaveform(longArrayOf(0, 14, 24, 18), intArrayOf(0, 120, 0, 180))
  }

  fun thunderRumble() {
    if (!enabled) return
    vibrateWaveform(longArrayOf(0, 45, 30, 80, 40, 50), intArrayOf(0, 220, 90, 255, 70, 140))
  }

  fun buttonClick() {
    if (!enabled) return
    vibrateOneShot(14L, 110)
  }

  private fun vibrateOneShot(durationMs: Long, amplitude: Int) {
    val v = vibrator ?: return
    if (!v.hasVibrator()) return
    try {
      v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
    } catch (_: Exception) {
    }
  }

  private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
    val v = vibrator ?: return
    if (!v.hasVibrator()) return
    try {
      v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    } catch (_: Exception) {
    }
  }
}
