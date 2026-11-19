package com.unboring.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unboring.weather.model.HourlyPoint
import com.unboring.weather.model.WeatherSkin
import com.unboring.weather.model.WeatherSnapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun PrecipDetailPanel(
  snapshot: WeatherSnapshot,
  activeHour: HourlyPoint,
  skin: WeatherSkin,
  onScrubTick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isHourly by remember { mutableStateOf(false) }
  var scrubIndex by remember { mutableIntStateOf(0) }
  val points = if (isHourly) {
    snapshot.hourly.take(12).map { it.shortTickLabel to (it.precipMm / 25.4).coerceAtLeast(0.0) to it.precipProbability }
  } else {
    snapshot.minutely.map { it.label to it.rateInPerHr to it.probability }
  }
  val safeIdx = scrubIndex.coerceIn(0, (points.size - 1).coerceAtLeast(0))
  val currentPt = points.getOrNull(safeIdx)
  val rateVal = currentPt?.first?.second ?: 0.0
  val chanceVal = currentPt?.second ?: activeHour.precipProbability

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp, vertical = 80.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = "PRECIP",
      color = skin.onSurface,
      fontSize = 36.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = (-1).sp
    )

    // 3D-style Terrain Cross-Section Slab + Water Pool
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
    ) {
      val w = size.width
      val h = size.height
      val slabTop = h * 0.48f
      val slabH = h * 0.34f
      val left = w * 0.14f
      val right = w * 0.86f

      // Water layer on top
      val waterDepth = (6.dp.toPx() + (chanceVal / 100f) * 18.dp.toPx())
      drawRect(
        color = skin.rainColor.copy(alpha = 0.85f),
        topLeft = Offset(left, slabTop - waterDepth),
        size = Size(right - left, waterDepth)
      )

      // Dark 3D terrain block
      val terrainPath = Path().apply {
        moveTo(left, slabTop)
        lineTo(right, slabTop)
        lineTo(right - 16.dp.toPx(), slabTop + slabH)
        lineTo(w * 0.52f, slabTop + slabH + 14.dp.toPx())
        lineTo(left + 14.dp.toPx(), slabTop + slabH)
        close()
      }
      drawPath(terrainPath, color = skin.numeralExtrude)

      // Falling rain streaks
      for (i in 0..14) {
        val rx = left + ((i + 0.5f) / 15f) * (right - left)
        val ry = (i * 23f) % (slabTop - 20.dp.toPx())
        drawLine(
          color = skin.rainColor,
          start = Offset(rx, ry),
          end = Offset(rx, ry + 14.dp.toPx()),
          strokeWidth = 3.dp.toPx()
        )
      }
    }

    // Telemetry card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(skin.surface.copy(alpha = 0.92f))
        .padding(18.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("RATE", color = skin.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("${String.format("%.2f", rateVal)} IN/HR", color = skin.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("CHANCE", color = skin.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("$chanceVal%", color = skin.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("TYPE", color = skin.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(activeHour.condition.label.uppercase(), color = skin.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Scrubbable Area Chart
      val maxIdx = (points.size - 1).coerceAtLeast(1)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(90.dp)
          .pointerInput(points.size) {
            detectHorizontalDragGestures { change, _ ->
              change.consume()
              val next = ((change.position.x / size.width.toFloat()) * maxIdx).roundToInt().coerceIn(0, maxIdx)
              if (next != scrubIndex) {
                scrubIndex = next
                onScrubTick()
              }
            }
          }
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height
          val path = Path().apply {
            moveTo(0f, h)
            points.forEachIndexed { i, pt ->
              val x = (i.toFloat() / maxIdx) * w
              val y = h - (pt.second / 100f) * h * 0.9f
              lineTo(x, y)
            }
            lineTo(w, h)
            close()
          }
          drawPath(path, color = skin.rainColor.copy(alpha = 0.7f))

          val cx = (safeIdx.toFloat() / maxIdx) * w
          drawLine(skin.onSurface, Offset(cx, 0f), Offset(cx, h), strokeWidth = 2.dp.toPx())
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        points.forEachIndexed { idx, pt ->
          Text(
            text = pt.first.first,
            color = skin.onSurface.copy(alpha = if (idx == safeIdx) 1f else 0.45f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(skin.pillBackground)
            .clickable { isHourly = !isHourly; scrubIndex = 0; onScrubTick() }
            .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
          Text(
            text = if (isHourly) "MIN  /  [HR]" else "[MIN]  /  HR",
            color = skin.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}

@Composable
fun MoonDetailPanel(
  snapshot: WeatherSnapshot,
  skin: WeatherSkin,
  modifier: Modifier = Modifier
) {
  val moon = snapshot.moon
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp, vertical = 80.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Text("MOON", color = skin.onSurface, fontSize = 36.sp, fontWeight = FontWeight.Black)

    // Lit Moon Phase Disc
    Canvas(modifier = Modifier.size(165.dp)) {
      val r = size.minDimension / 2f
      val center = Offset(size.width / 2f, size.height / 2f)
      drawCircle(Color(0xFF24262E), radius = r, center = center)
      val litWidth = (cos(2.0 * PI * moon.phaseFraction)).toFloat()
      drawArc(
        color = Color(0xFFF3F5FA),
        startAngle = if (moon.phaseFraction < 0.5) -90f else 90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(r * 2f, r * 2f)
      )
      if (litWidth > 0f) {
        drawOval(
          color = Color(0xFF24262E),
          topLeft = Offset(center.x - r * litWidth, center.y - r),
          size = Size(r * 2f * litWidth, r * 2f)
        )
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(skin.surface.copy(alpha = 0.92f))
        .padding(18.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(moon.phaseName, color = skin.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text("${moon.illuminationPercent}%", color = skin.accent, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
      }
      Spacer(modifier = Modifier.height(14.dp))
      AstroRow("SUNRISE", snapshot.sunriseLabel, skin)
      AstroRow("SUNSET", snapshot.sunsetLabel, skin)
      AstroRow("NEXT FULL MOON", moon.nextFullMoonDate, skin)
      AstroRow("NEXT NEW MOON", moon.nextNewMoonDate, skin)
    }
  }
}

@Composable
private fun AstroRow(label: String, value: String, skin: WeatherSkin) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, color = skin.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    Text(value, color = skin.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }
}

@Composable
fun AirQualityPanel(
  snapshot: WeatherSnapshot,
  activeHour: HourlyPoint,
  skin: WeatherSkin,
  modifier: Modifier = Modifier
) {
  val aqi = snapshot.airQuality
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp, vertical = 80.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Text("AIR QUALITY", color = skin.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Black)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = aqi.rating,
        color = skin.accent,
        fontSize = 68.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-2).sp
      )
      Text(
        text = "US AQI  ${aqi.usAqi}",
        color = skin.onSurface.copy(alpha = 0.75f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(skin.surface.copy(alpha = 0.92f))
        .padding(18.dp)
    ) {
      AstroRow("PM 2.5", "${aqi.pm25} µg/m³", skin)
      AstroRow("PM 10", "${aqi.pm10} µg/m³", skin)
      AstroRow("POLLEN", aqi.pollenLevel.uppercase(), skin)
      AstroRow("UV INDEX", "${activeHour.uvIndex.roundToInt()}", skin)
      AstroRow("VISIBILITY", "${activeHour.visibilityMiles} MI", skin)
    }
  }
}

private data class FallingCloud(val id: Int, var xFrac: Float, var yFrac: Float, val speed: Float)

@Composable
fun CloudburstMinigamePanel(
  skin: WeatherSkin,
  onCloudPop: () -> Unit,
  modifier: Modifier = Modifier
) {
  var playing by remember { mutableStateOf(false) }
  var score by remember { mutableIntStateOf(0) }
  var bestScore by remember { mutableIntStateOf(0) }
  var combo by remember { mutableIntStateOf(1) }
  val clouds = remember { mutableStateListOf<FallingCloud>() }
  var nextId by remember { mutableIntStateOf(1) }

  LaunchedEffect(playing) {
    if (!playing) return@LaunchedEffect
    clouds.clear()
    score = 0
    combo = 1
    var tick = 0
    while (playing) {
      delay(32L)
      tick++
      if (tick % 22 == 1 && clouds.size < 8) {
        clouds.add(
          FallingCloud(
            id = nextId++,
            xFrac = 0.12f + Random.nextFloat() * 0.76f,
            yFrac = 0.05f,
            speed = 0.008f + Random.nextFloat() * 0.007f
          )
        )
      }
      val iter = clouds.listIterator()
      while (iter.hasNext()) {
        val c = iter.next()
        val updated = c.copy(yFrac = c.yFrac + c.speed)
        if (updated.yFrac > 0.92f) {
          iter.remove()
          combo = 1
        } else {
          iter.set(updated)
        }
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp, vertical = 80.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text("CLOUDBURST", color = skin.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Black)
    Text(
      text = "SCORE: $score   •   x$combo   •   BEST: $bestScore",
      color = skin.onSurface.copy(alpha = 0.75f),
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
    Spacer(modifier = Modifier.height(14.dp))

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .clip(RoundedCornerShape(26.dp))
        .background(skin.surface.copy(alpha = 0.75f))
    ) {
      val boxW = constraints.maxWidth.toFloat()
      val boxH = constraints.maxHeight.toFloat()

      if (!playing) {
        Column(
          modifier = Modifier.align(Alignment.Center),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Pop storm clouds before they drop!",
            color = skin.onSurface.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = { playing = true },
            colors = ButtonDefaults.buttonColors(containerColor = skin.accent)
          ) {
            Text("PLAY CLOUDBURST", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      } else {
        clouds.forEach { cloud ->
          Box(
            modifier = Modifier
              .offset {
                IntOffset(
                  ((cloud.xFrac * boxW) - 34.dp.toPx()).roundToInt(),
                  ((cloud.yFrac * boxH) - 24.dp.toPx()).roundToInt()
                )
              }
              .size(68.dp, 46.dp)
              .clip(CircleShape)
              .background(skin.cloudColor)
              .clickable {
                clouds.removeAll { it.id == cloud.id }
                score += 10 * combo
                combo = (combo + 1).coerceAtMost(8)
                if (score > bestScore) bestScore = score
                onCloudPop()
              },
            contentAlignment = Alignment.Center
          ) {
            Text("☁", fontSize = 24.sp, color = Color.White)
          }
        }

        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(12.dp)
            .clip(RoundedCornerShape(50))
            .background(skin.pillBackground)
            .clickable { playing = false }
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          Text("STOP", color = skin.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }
    }
  }
}
