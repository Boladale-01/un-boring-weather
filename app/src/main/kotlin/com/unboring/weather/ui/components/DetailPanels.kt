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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.unboring.weather.ui.theme.AntonioDisplay
import com.unboring.weather.ui.theme.AtkinsonMono
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
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = "PRECIP",
        color = skin.numeralExtrude.copy(alpha = 0.7f),
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = AntonioDisplay,
        modifier = Modifier.offset(x = 3.dp, y = 4.dp)
      )
      Text(
        text = "PRECIP",
        color = skin.onSurface,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = AntonioDisplay
      )
    }

    // Isometric 3D Water Cap + Faceted 3D Rock Slab + 3D Gradient Raindrops (matching Figma Screen 04)
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(210.dp)
    ) {
      val cx = size.width * 0.5f
      val isoW = 120.dp.toPx()
      val isoH = 38.dp.toPx()
      val waterTopY = size.height * 0.44f
      val waterThick = 18.dp.toPx()
      val rockDepth = 62.dp.toPx()

      // Soft ground shadow
      drawOval(
        color = Color.Black.copy(alpha = 0.16f),
        topLeft = Offset(cx - isoW * 0.92f, waterTopY + isoH * 2f + rockDepth - 12.dp.toPx()),
        size = Size(isoW * 1.84f, 28.dp.toPx())
      )

      // Faceted 3D Rock Base (Left & Right facets)
      val rockLeft = Path().apply {
        moveTo(cx - isoW, waterTopY + isoH + waterThick)
        lineTo(cx, waterTopY + isoH * 2f + waterThick)
        lineTo(cx, waterTopY + isoH * 2f + waterThick + rockDepth)
        lineTo(cx - isoW * 0.52f, waterTopY + isoH * 2f + waterThick + rockDepth * 0.72f)
        lineTo(cx - isoW * 0.85f, waterTopY + isoH + waterThick + rockDepth * 0.48f)
        close()
      }
      drawPath(rockLeft, color = Color(0xFF26282E))

      val rockRight = Path().apply {
        moveTo(cx, waterTopY + isoH * 2f + waterThick)
        lineTo(cx + isoW, waterTopY + isoH + waterThick)
        lineTo(cx + isoW * 0.85f, waterTopY + isoH + waterThick + rockDepth * 0.48f)
        lineTo(cx + isoW * 0.48f, waterTopY + isoH * 2f + waterThick + rockDepth * 0.72f)
        lineTo(cx, waterTopY + isoH * 2f + waterThick + rockDepth)
        close()
      }
      drawPath(rockRight, color = Color(0xFF141519))

      // 3D Isometric Water Cap (Top, Left, Right)
      val waterTop = Path().apply {
        moveTo(cx, waterTopY)
        lineTo(cx + isoW, waterTopY + isoH)
        lineTo(cx, waterTopY + isoH * 2f)
        lineTo(cx - isoW, waterTopY + isoH)
        close()
      }
      drawPath(waterTop, color = Color(0xFF6BD6FF))

      val waterLeft = Path().apply {
        moveTo(cx - isoW, waterTopY + isoH)
        lineTo(cx, waterTopY + isoH * 2f)
        lineTo(cx, waterTopY + isoH * 2f + waterThick)
        lineTo(cx - isoW, waterTopY + isoH + waterThick)
        close()
      }
      drawPath(waterLeft, color = Color(0xFF2EA8FF))

      val waterRight = Path().apply {
        moveTo(cx, waterTopY + isoH * 2f)
        lineTo(cx + isoW, waterTopY + isoH)
        lineTo(cx + isoW, waterTopY + isoH + waterThick)
        lineTo(cx, waterTopY + isoH * 2f + waterThick)
        close()
      }
      drawPath(waterRight, color = Color(0xFF177AD9))

      // Water splash ripple
      drawOval(
        color = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(cx - 22.dp.toPx(), waterTopY + isoH - 7.dp.toPx()),
        size = Size(44.dp.toPx(), 14.dp.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
      )

      // 3D Raindrop Capsules with Specular Highlights
      val drops = listOf(
        Triple(-76f, 10f, 22f), Triple(-44f, 44f, 26f), Triple(-16f, 4f, 20f),
        Triple(14f, 52f, 24f), Triple(44f, 18f, 22f), Triple(72f, 46f, 24f),
        Triple(-56f, 76f, 18f), Triple(56f, 78f, 18f)
      )
      drops.forEach { (ox, oy, dh) ->
        val dx = cx + ox.dp.toPx()
        val dy = oy.dp.toPx()
        val dw = 8.dp.toPx()
        val dhPx = dh.dp.toPx()
        drawRoundRect(
          brush = Brush.verticalGradient(listOf(Color(0xFF7AE0FF), Color(0xFF148EFF))),
          topLeft = Offset(dx, dy),
          size = Size(dw, dhPx),
          cornerRadius = CornerRadius(dw / 2f, dw / 2f)
        )
        drawRoundRect(
          color = Color.White.copy(alpha = 0.65f),
          topLeft = Offset(dx + 2.dp.toPx(), dy + 3.dp.toPx()),
          size = Size(2.2.dp.toPx(), dhPx * 0.5f),
          cornerRadius = CornerRadius(1.1.dp.toPx(), 1.1.dp.toPx())
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

@Composable
fun Instruments3DPanel(
  snapshot: WeatherSnapshot,
  activeHour: HourlyPoint,
  skin: WeatherSkin,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp, vertical = 78.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Text("INSTRUMENTS", color = skin.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Black)

    // 3D-style Tactile Wind Compass + Barometer Dials
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      // Wind Compass Dial
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(134.dp)) {
          val r = size.minDimension / 2f
          val c = Offset(size.width / 2f, size.height / 2f)
          // 3D Beveled Outer Ring
          drawCircle(skin.numeralExtrude, radius = r, center = c + Offset(0f, 5.dp.toPx()))
          drawCircle(skin.surface, radius = r, center = c)
          drawCircle(skin.onSurface.copy(alpha = 0.25f), radius = r - 6.dp.toPx(), center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))

          // Compass needle
          val rad = (activeHour.windDirectionDeg - 90) * PI / 180.0
          val tip = Offset(
            c.x + (kotlin.math.cos(rad) * (r - 18.dp.toPx())).toFloat(),
            c.y + (kotlin.math.sin(rad) * (r - 18.dp.toPx())).toFloat()
          )
          val tail = Offset(
            c.x - (kotlin.math.cos(rad) * (r * 0.35f)).toFloat(),
            c.y - (kotlin.math.sin(rad) * (r * 0.35f)).toFloat()
          )
          drawLine(skin.accent, tail, tip, strokeWidth = 5.dp.toPx())
          drawCircle(skin.onSurface, radius = 6.dp.toPx(), center = c)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("${activeHour.windSpeedMph.roundToInt()} MPH", color = skin.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text("WIND ${activeHour.windDirectionDeg}°", color = skin.onSurface.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
      }

      // Barometer Pressure Dial
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(134.dp)) {
          val r = size.minDimension / 2f
          val c = Offset(size.width / 2f, size.height / 2f)
          drawCircle(skin.numeralExtrude, radius = r, center = c + Offset(0f, 5.dp.toPx()))
          drawCircle(skin.surface, radius = r, center = c)
          drawArc(
            color = skin.accent.copy(alpha = 0.35f),
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
            size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(8.dp.toPx())
          )
          // Map 29.0..30.6 inHg to 140..400 deg
          val norm = ((activeHour.pressureInHg - 29.0) / 1.6).coerceIn(0.0, 1.0)
          val deg = 140.0 + norm * 260.0
          val rad = deg * PI / 180.0
          val tip = Offset(
            c.x + (kotlin.math.cos(rad) * (r - 20.dp.toPx())).toFloat(),
            c.y + (kotlin.math.sin(rad) * (r - 20.dp.toPx())).toFloat()
          )
          drawLine(skin.sunColor, c, tip, strokeWidth = 4.dp.toPx())
          drawCircle(skin.onSurface, radius = 6.dp.toPx(), center = c)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("${activeHour.pressureInHg} INHG", color = skin.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        val trend = if (activeHour.pressureInHg >= 30.0) "HIGH PRESSURE" else "LOW / STORMY"
        Text(trend, color = skin.onSurface.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
      }
    }

    // Solar Path & Golden Hour Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .background(skin.surface.copy(alpha = 0.92f))
        .padding(18.dp)
    ) {
      Text("SOLAR ARC & GOLDEN HOUR", color = skin.onSurface.copy(alpha = 0.65f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(10.dp))
      Canvas(modifier = Modifier.fillMaxWidth().height(70.dp)) {
        val w = size.width
        val h = size.height
        drawLine(skin.onSurface.copy(alpha = 0.2f), Offset(0f, h * 0.78f), Offset(w, h * 0.78f), 2.dp.toPx())
        drawArc(
          color = skin.sunColor.copy(alpha = 0.7f),
          startAngle = 180f,
          sweepAngle = 180f,
          useCenter = false,
          topLeft = Offset(w * 0.08f, h * 0.14f),
          size = Size(w * 0.84f, h * 1.28f),
          style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())
        )
        val sunPos = if (activeHour.isDay) 0.55f else 0.12f
        val sx = w * (0.08f + sunPos * 0.84f)
        val sy = h * 0.78f - kotlin.math.sin(sunPos * PI).toFloat() * (h * 0.62f)
        drawCircle(skin.sunColor, radius = 9.dp.toPx(), center = Offset(sx, sy))
      }
      Spacer(modifier = Modifier.height(8.dp))
      AstroRow("SUNRISE / GOLDEN", snapshot.sunriseLabel, skin)
      AstroRow("SUNSET / BLUE HR", snapshot.sunsetLabel, skin)
      AstroRow("DEW POINT COMFORT", if (activeHour.dewPointF > 65) "MUGGY" else if (activeHour.dewPointF > 55) "COMFORTABLE" else "CRISP", skin)
    }
  }
}
