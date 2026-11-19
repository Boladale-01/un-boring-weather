package com.unboring.weather.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unboring.weather.model.HourlyPoint
import com.unboring.weather.model.MoonInfo
import com.unboring.weather.model.WeatherSkin
import kotlin.math.roundToInt

@Composable
fun BlueprintCalloutsOverlay(
  visible: Boolean,
  hour: HourlyPoint,
  sunriseLabel: String,
  sunsetLabel: String,
  moon: MoonInfo,
  useCelsius: Boolean,
  skin: WeatherSkin,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier
  ) {
    val lineColor = skin.onSurface.copy(alpha = 0.65f)
    val textColor = skin.onSurface.copy(alpha = 0.9f)
    val tempUnit = if (useCelsius) "C" else "F"
    val tempVal = formatTemp(hour.tempF, useCelsius)
    val feelsVal = formatTemp(hour.feelsLikeF, useCelsius)
    val dewVal = formatTemp(hour.dewPointF, useCelsius)

    Box(modifier = Modifier.fillMaxSize()) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stroke = 1.2.dp.toPx()
        val dotR = 3.dp.toPx()

        // Top-left cloud callout line
        drawLine(lineColor, Offset(w * 0.28f, h * 0.20f), Offset(w * 0.42f, h * 0.28f), stroke)
        drawCircle(lineColor, dotR, Offset(w * 0.42f, h * 0.28f))

        // Top-right sun/moon callout line
        drawLine(lineColor, Offset(w * 0.72f, h * 0.22f), Offset(w * 0.56f, h * 0.26f), stroke)
        drawCircle(lineColor, dotR, Offset(w * 0.56f, h * 0.26f))

        // Bottom-left temp callout line
        drawLine(lineColor, Offset(w * 0.24f, h * 0.65f), Offset(w * 0.40f, h * 0.56f), stroke)
        drawCircle(lineColor, dotR, Offset(w * 0.40f, h * 0.56f))

        // Bottom-right UV/vis callout line
        drawLine(lineColor, Offset(w * 0.74f, h * 0.64f), Offset(w * 0.60f, h * 0.57f), stroke)
        drawCircle(lineColor, dotR, Offset(w * 0.60f, h * 0.57f))
      }

      // Top center wind
      Text(
        text = "${compassDir(hour.windDirectionDeg)}  ${hour.windSpeedMph.roundToInt()} MPH",
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 74.dp)
      )

      // Top left clouds/rain
      Column(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(start = 24.dp, top = 116.dp)
      ) {
        Text("CLOUDS: ${hour.cloudCover}%", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text("RAIN: ${if (hour.precipProbability < 15) "NONE" else "${hour.precipProbability}%"}", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
      }

      // Top right astro
      Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(end = 24.dp, top = 126.dp)
      ) {
        Text("RISE: $sunriseLabel", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text("SET: $sunsetLabel", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text("MOON: ${moon.illuminationPercent}%", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
      }

      // Bottom left telemetry
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(start = 24.dp, bottom = 185.dp)
      ) {
        Text("TEMP: $tempVal°$tempUnit", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text("FEELS: $feelsVal°", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text("HUM: ${hour.humidity}%", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text("DEW: $dewVal°", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text("INHG: ${hour.pressureInHg}", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
      }

      // Bottom right UV / Vis
      Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 24.dp, bottom = 200.dp)
      ) {
        Text("MAX UV: ${hour.uvIndex.roundToInt()}", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text("VIS: ${hour.visibilityMiles} MI", color = textColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

fun formatTemp(tempF: Double, useCelsius: Boolean): Int {
  return if (useCelsius) ((tempF - 32.0) * 5.0 / 9.0).roundToInt() else tempF.roundToInt()
}

private fun compassDir(deg: Int): String {
  val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
  return dirs[((deg % 360 + 360) % 360 / 45) % 8]
}
