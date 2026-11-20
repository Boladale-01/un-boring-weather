package com.unboring.weather.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unboring.weather.model.ShaderMode
import com.unboring.weather.model.WeatherCondition
import com.unboring.weather.model.WeatherSkin
import com.unboring.weather.ui.theme.AntonioDisplay
import com.unboring.weather.ui.theme.AtkinsonMono
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun Weather3DStage(
  temperatureDisplay: Int,
  condition: WeatherCondition,
  precipProbability: Int,
  cloudCover: Int,
  windSpeedMph: Double,
  isDay: Boolean,
  moonPhaseFraction: Double,
  cloudSeed: Int,
  skin: WeatherSkin,
  spinAngleDeg: Float,
  pullX: Float,
  pullY: Float,
  modifier: Modifier = Modifier
) {
  var timeSec by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(16L)
      timeSec += 0.016f
    }
  }

  val cloudSpring by animateFloatAsState(
    targetValue = 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "cloudSpring"
  )
  val density = LocalDensity.current.density
  val bg = if (isDay) skin.background else skin.backgroundNight
  val isWireframe = skin.shaderMode == ShaderMode.WIREFRAME
  val isNormal = skin.shaderMode == ShaderMode.NORMAL_DEBUG
  val isMetallic = skin.shaderMode == ShaderMode.METALLIC

  Box(modifier = modifier.fillMaxSize()) {
    // 1. Studio Vignette + Subtle Grain Layer
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            lerp(bg, Color.White, 0.06f),
            bg,
            lerp(bg, Color.Black, 0.22f)
          ),
          center = Offset(size.width * 0.5f, size.height * 0.38f),
          radius = size.maxDimension * 0.75f
        )
      )
    }

    // 2. 3D Sun / Moon + Sculpted Cloud Cluster + Rain/Snow (rotates & stretches with rubber-band)
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          cameraDistance = 14f * density
          rotationY = spinAngleDeg * 0.55f
          translationX = pullX * 95f
          translationY = pullY * 75f
        }
    ) {
      val cx = size.width * 0.5f
      val stageY = size.height * 0.23f

      // Sun or Moon
      if (isDay) {
        val sunR = 64.dp.toPx()
        val sunCenter = Offset(cx, stageY - 12.dp.toPx() + sin(timeSec * 1.2f) * 3.dp.toPx())
        if (isWireframe) {
          drawCircle(skin.sunColor, radius = sunR, center = sunCenter, style = Stroke(2.dp.toPx()))
        } else {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                if (isNormal) Color(0xFFFF7AE0) else lerp(skin.sunColor, Color.White, 0.25f),
                if (isNormal) Color(0xFF6B8BFF) else skin.sunColor,
                if (isNormal) Color(0xFF3A4BFF) else lerp(skin.sunColor, Color.Black, 0.18f)
              ),
              center = sunCenter - Offset(sunR * 0.28f, sunR * 0.28f),
              radius = sunR * 1.35f
            ),
            radius = sunR,
            center = sunCenter
          )
        }
      } else {
        val moonR = 58.dp.toPx()
        val moonCenter = Offset(cx, stageY - 10.dp.toPx())
        drawCircle(Color(0xFF23252C), radius = moonR, center = moonCenter)
        val litWidth = cos(2.0 * PI * moonPhaseFraction).toFloat()
        drawArc(
          color = Color(0xFFF2F4FA),
          startAngle = if (moonPhaseFraction < 0.5) -90f else 90f,
          sweepAngle = 180f,
          useCenter = true,
          topLeft = Offset(moonCenter.x - moonR, moonCenter.y - moonR),
          size = Size(moonR * 2f, moonR * 2f)
        )
        if (litWidth > 0f) {
          drawOval(
            color = Color(0xFF23252C),
            topLeft = Offset(moonCenter.x - moonR * litWidth, moonCenter.y - moonR),
            size = Size(moonR * 2f * litWidth, moonR * 2f)
          )
        }
      }

      // Sculpted Blender-style Cloud Cluster (matching Figma proportions!)
      if (cloudCover >= 10) {
        val rng = Random(cloudSeed * 997 + cloudCover)
        val jitterX = (rng.nextFloat() - 0.5f) * 14.dp.toPx()
        val darken = if (condition == WeatherCondition.THUNDERSTORM) 0.58f else 1f
        val baseCloud = Color(
          (skin.cloudColor.red * darken).coerceIn(0f, 1f),
          (skin.cloudColor.green * darken).coerceIn(0f, 1f),
          (skin.cloudColor.blue * darken).coerceIn(0f, 1f),
          1f
        )
        val puffs = listOf(
          Triple(-54.dp.toPx(), 18.dp.toPx(), 46.dp.toPx()),
          Triple(56.dp.toPx(), 20.dp.toPx(), 44.dp.toPx()),
          Triple(2.dp.toPx(), 8.dp.toPx(), 56.dp.toPx()),
          Triple(-20.dp.toPx(), -8.dp.toPx(), 36.dp.toPx())
        )
        puffs.forEachIndexed { idx, (ox, oy, r) ->
          val sway = sin(timeSec * 1.1f + idx) * (windSpeedMph.toFloat().coerceIn(2f, 25f) * 0.25f)
          val center = Offset(cx + ox + jitterX + sway, stageY + 28.dp.toPx() + oy)
          val rad = r * cloudSpring
          if (isWireframe) {
            drawCircle(baseCloud, radius = rad, center = center, style = Stroke(2.dp.toPx()))
          } else {
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(
                  if (isNormal) Color(0xFF80FFB5) else lerp(baseCloud, Color.White, if (isMetallic) 0.35f else 0.14f),
                  if (isNormal) Color(0xFF5CE1E6) else baseCloud,
                  if (isNormal) Color(0xFF426BFF) else lerp(baseCloud, Color.Black, 0.28f)
                ),
                center = center - Offset(rad * 0.3f, rad * 0.32f),
                radius = rad * 1.3f
              ),
              radius = rad,
              center = center
            )
          }
        }
      }

      // 3D Raindrop Capsules with Specular Highlights / Snow / Lightning
      val isSnow = condition == WeatherCondition.SNOW
      val isRain = condition in setOf(
        WeatherCondition.DRIZZLE,
        WeatherCondition.RAIN,
        WeatherCondition.HEAVY_RAIN,
        WeatherCondition.THUNDERSTORM
      ) || precipProbability >= 45

      if (isRain || isSnow) {
        val dropCount = if (condition == WeatherCondition.THUNDERSTORM || condition == WeatherCondition.HEAVY_RAIN) 14 else 9
        for (i in 0 until dropCount) {
          val phase = ((timeSec * 0.9f + i * 0.27f) % 1f)
          val dx = cx - 125.dp.toPx() + (i % 7) * 40.dp.toPx() + (i / 7) * 18.dp.toPx()
          val dy = stageY + 70.dp.toPx() + phase * 190.dp.toPx()
          if (isSnow) {
            drawCircle(Color.White.copy(alpha = 1f - phase * 0.5f), radius = 4.5.dp.toPx(), center = Offset(dx, dy))
          } else {
            rotate(degrees = 14f, pivot = Offset(dx, dy)) {
              drawRoundRect(
                brush = Brush.verticalGradient(
                  listOf(
                    lerp(skin.rainColor, Color.White, 0.35f),
                    skin.rainColor
                  )
                ),
                topLeft = Offset(dx - 2.5.dp.toPx(), dy),
                size = Size(5.dp.toPx(), 24.dp.toPx()),
                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                alpha = 1f - phase * 0.45f
              )
            }
          }
        }
      }

      // Thunderstorm Bolt
      if (condition == WeatherCondition.THUNDERSTORM && ((timeSec * 2.2f).toInt() % 5 == 0)) {
        val bolt = Path().apply {
          moveTo(cx + 6.dp.toPx(), stageY + 60.dp.toPx())
          lineTo(cx - 18.dp.toPx(), stageY + 110.dp.toPx())
          lineTo(cx + 8.dp.toPx(), stageY + 114.dp.toPx())
          lineTo(cx - 12.dp.toPx(), stageY + 170.dp.toPx())
        }
        drawPath(bolt, color = Color(0xFFFFF59D), style = Stroke(width = 5.dp.toPx()))
      }
    }

    // 3. Giant Antonio Bold 3D Extruded Temperature (spins & rubber-band pulls!)
    val tempText = "$temperatureDisplay"
    val subText = if (precipProbability >= 25) {
      "${condition.label.uppercase()}  $precipProbability%"
    } else {
      condition.label.uppercase()
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .align(Alignment.Center)
        .padding(top = 95.dp)
        .graphicsLayer {
          cameraDistance = 14f * density
          rotationY = spinAngleDeg
          translationX = pullX * 70f
          translationY = pullY * 55f
        }
    ) {
      Box(contentAlignment = Alignment.Center) {
        // Multi-layer 3D beveled shadow/extrusion matching Figma Screen 01/02/03
        val layers = listOf(
          Triple(9, 13, 0.92f),
          Triple(6, 9, 0.72f),
          Triple(3, 4, 0.45f)
        )
        layers.forEach { (ox, oy, alpha) ->
          Text(
            text = tempText,
            color = skin.numeralExtrude.copy(alpha = alpha),
            fontSize = 212.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = AntonioDisplay,
            letterSpacing = (-8).sp,
            modifier = Modifier.offset { IntOffset(ox, oy) }
          )
        }

        // Front Chiseled Face
        Text(
          text = tempText,
          color = skin.numeralPrimary,
          fontSize = 212.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = AntonioDisplay,
          letterSpacing = (-8).sp
        )

        if (skin.hasGooglyEyes) {
          Canvas(modifier = Modifier.size(160.dp)) {
            val eyeR = 15.dp.toPx()
            val pupilR = 6.5.dp.toPx()
            listOf(
              Offset(size.width * 0.25f, size.height * 0.36f),
              Offset(size.width * 0.75f, size.height * 0.62f)
            ).forEach { c ->
              drawCircle(Color.White, radius = eyeR, center = c)
              drawCircle(Color.Black, radius = eyeR, center = c, style = Stroke(2.dp.toPx()))
              drawCircle(Color.Black, radius = pupilR, center = c + Offset(pullX * 8.dp.toPx(), pullY * 8.dp.toPx()))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subText,
        color = skin.onSurface.copy(alpha = 0.88f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = AtkinsonMono,
        letterSpacing = 1.8.sp
      )
    }
  }
}
