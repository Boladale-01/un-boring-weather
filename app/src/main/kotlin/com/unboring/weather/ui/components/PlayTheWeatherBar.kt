package com.unboring.weather.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unboring.weather.model.DailyForecast
import com.unboring.weather.model.HourlyPoint
import com.unboring.weather.model.WeatherCondition
import com.unboring.weather.model.WeatherSkin
import kotlin.math.roundToInt

@Composable
fun PlayTheWeatherBar(
  hourly: List<HourlyPoint>,
  daily: List<DailyForecast>,
  selectedHourIndex: Int,
  showWeekly: Boolean,
  useCelsius: Boolean,
  skin: WeatherSkin,
  onSelectHour: (Int) -> Unit,
  onToggleWeekly: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (hourly.isEmpty()) return
  val activeHour = hourly[selectedHourIndex.coerceIn(0, hourly.lastIndex)]

  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Mode toggle pill above the bar
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(50))
        .background(skin.pillBackground.copy(alpha = 0.85f))
        .clickable { onToggleWeekly() }
        .padding(horizontal = 12.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (showWeekly) "8-DAY  •  TAP FOR TIMELINE" else "TIMELINE  •  TAP FOR 8-DAY",
        color = skin.onSurface.copy(alpha = 0.78f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    AnimatedContent(targetState = showWeekly, label = "forecastMode") { weekly ->
      if (weekly) {
        WeeklyForecastStrip(daily = daily, useCelsius = useCelsius, skin = skin)
      } else {
        HourlyScrubBar(
          hourly = hourly,
          selectedIndex = selectedHourIndex,
          activeHour = activeHour,
          useCelsius = useCelsius,
          skin = skin,
          onSelectHour = onSelectHour
        )
      }
    }
  }
}

@Composable
private fun HourlyScrubBar(
  hourly: List<HourlyPoint>,
  selectedIndex: Int,
  activeHour: HourlyPoint,
  useCelsius: Boolean,
  skin: WeatherSkin,
  onSelectHour: (Int) -> Unit
) {
  var barWidthPx = 1f
  val maxIdx = (hourly.size - 1).coerceAtLeast(1)
  val fraction = selectedIndex.toFloat() / maxIdx.toFloat()

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    // Floating active time bubble (e.g. "3AM" / "NOW")
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(38.dp)
        .onSizeChanged { barWidthPx = it.width.toFloat().coerceAtLeast(1f) }
    ) {
      val bubblePx = 44.dp
      Box(
        modifier = Modifier
          .offset {
            val usable = (barWidthPx - 44.dp.toPx()).coerceAtLeast(1f)
            IntOffset((fraction * usable).roundToInt(), 0)
          }
          .size(bubblePx, 32.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(skin.accent),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = activeHour.hourLabel,
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Pill Timeline Bar with rain-wave fill & scrub gesture
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clip(RoundedCornerShape(22.dp))
        .background(skin.pillBackground)
        .pointerInput(hourly.size) {
          detectTapGestures { offset ->
            val idx = ((offset.x / size.width.toFloat()) * maxIdx).roundToInt().coerceIn(0, maxIdx)
            onSelectHour(idx)
          }
        }
        .pointerInput(hourly.size) {
          detectHorizontalDragGestures { change, _ ->
            change.consume()
            val idx = ((change.position.x / size.width.toFloat()) * maxIdx).roundToInt().coerceIn(0, maxIdx)
            onSelectHour(idx)
          }
        }
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        val w = size.width
        val h = size.height

        // Rain probability wave inside pill
        val path = Path().apply {
          moveTo(0f, h)
          hourly.forEachIndexed { i, pt ->
            val x = (i.toFloat() / maxIdx) * w
            val y = h - (pt.precipProbability / 100f) * h * 0.85f
            lineTo(x, y)
          }
          lineTo(w, h)
          close()
        }
        drawPath(path = path, color = skin.rainColor.copy(alpha = 0.72f))

        // Scrub indicator line
        val markerX = fraction * w
        drawRoundRect(
          color = Color.White,
          topLeft = Offset((markerX - 2.dp.toPx()).coerceIn(0f, w - 4.dp.toPx()), 4.dp.toPx()),
          size = Size(4.dp.toPx(), h - 8.dp.toPx()),
          cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
      }

      // High / Low / Current Temp markers inside pill
      Row(
        modifier = Modifier
          .matchParentSize()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val sampleIndices = listOf(0, hourly.size / 3, (hourly.size * 2) / 3, hourly.lastIndex).distinct()
        sampleIndices.forEach { idx ->
          val pt = hourly[idx]
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${formatTemp(pt.tempF, useCelsius)}°",
              color = skin.onSurface,
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.size(4.dp))
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (pt.isDay) skin.sunColor else skin.onSurface.copy(alpha = 0.7f))
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Tick labels underneath (NOW, 3, 6, 9, 12...)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      val step = maxOf(1, hourly.size / 7)
      for (i in hourly.indices step step) {
        val isSelected = kotlin.math.abs(i - selectedIndex) <= step / 2
        Text(
          text = hourly[i].shortTickLabel,
          color = skin.onSurface.copy(alpha = if (isSelected) 1f else 0.55f),
          fontSize = 10.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

@Composable
private fun WeeklyForecastStrip(
  daily: List<DailyForecast>,
  useCelsius: Boolean,
  skin: WeatherSkin
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(skin.pillBackground)
      .padding(horizontal = 10.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    daily.take(8).forEach { day ->
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = day.dayLabel,
          color = skin.onSurface.copy(alpha = 0.75f),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Icon(
          imageVector = conditionIcon(day.condition),
          contentDescription = day.condition.label,
          tint = when (day.condition) {
            WeatherCondition.CLEAR, WeatherCondition.MOSTLY_CLEAR -> skin.sunColor
            WeatherCondition.RAIN, WeatherCondition.DRIZZLE, WeatherCondition.HEAVY_RAIN -> skin.rainColor
            WeatherCondition.THUNDERSTORM -> Color(0xFFFFD60A)
            else -> skin.onSurface
          },
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "${formatTemp(day.highF, useCelsius)}",
          color = skin.onSurface,
          fontSize = 13.sp,
          fontWeight = FontWeight.Black
        )
        Text(
          text = "${formatTemp(day.lowF, useCelsius)}",
          color = skin.onSurface.copy(alpha = 0.5f),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

fun conditionIcon(condition: WeatherCondition) = when (condition) {
  WeatherCondition.CLEAR, WeatherCondition.MOSTLY_CLEAR -> Icons.Rounded.WbSunny
  WeatherCondition.THUNDERSTORM -> Icons.Rounded.Bolt
  WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> Icons.Rounded.WaterDrop
  WeatherCondition.FOG -> Icons.Rounded.NightsStay
  else -> Icons.Rounded.Cloud
}
