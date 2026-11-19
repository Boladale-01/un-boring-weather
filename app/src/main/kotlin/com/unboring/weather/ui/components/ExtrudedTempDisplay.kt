package com.unboring.weather.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unboring.weather.model.WeatherSkin
import kotlin.math.roundToInt

@Composable
fun ExtrudedTempDisplay(
  tempDisplay: Int,
  conditionLabel: String,
  precipProbability: Int,
  skin: WeatherSkin,
  tiltX: Float,
  tiltY: Float,
  modifier: Modifier = Modifier
) {
  val animTemp by animateFloatAsState(
    targetValue = tempDisplay.toFloat(),
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "tempAnim"
  )
  val text = "${animTemp.roundToInt()}"
  val dx = (-tiltX * 14f).roundToInt()
  val dy = (tiltY * 10f).roundToInt() + 12

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(contentAlignment = Alignment.Center) {
      // Multi-layer 3D extrusion depth
      val layers = 8
      for (i in layers downTo 1) {
        val frac = i.toFloat() / layers
        val layerColor = lerp(skin.numeralPrimary, skin.numeralExtrude, frac)
        Text(
          text = text,
          color = layerColor,
          fontSize = 152.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.SansSerif,
          letterSpacing = (-8).sp,
          modifier = Modifier.offset {
            IntOffset((dx * frac).roundToInt(), (dy * frac).roundToInt())
          }
        )
      }

      // Front face
      Text(
        text = text,
        color = skin.numeralPrimary,
        fontSize = 152.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = (-8).sp
      )

      // Optional Monsters skin googly eyes
      if (skin.hasGooglyEyes) {
        Canvas(modifier = Modifier.size(160.dp)) {
          val eyeR = 16.dp.toPx()
          val pupilR = 7.dp.toPx()
          val leftCenter = Offset(size.width * 0.28f, size.height * 0.34f)
          val rightCenter = Offset(size.width * 0.72f, size.height * 0.62f)
          listOf(leftCenter, rightCenter).forEach { c ->
            drawCircle(Color.White, radius = eyeR, center = c)
            drawCircle(Color.Black, radius = eyeR, center = c, style = Stroke(width = 2.dp.toPx()))
            drawCircle(
              Color.Black,
              radius = pupilR,
              center = c + Offset(tiltX * 6.dp.toPx(), tiltY * 6.dp.toPx())
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(2.dp))
    val subText = if (precipProbability >= 25) {
      "${conditionLabel.uppercase()}  $precipProbability%"
    } else {
      conditionLabel.uppercase()
    }
    Text(
      text = subText,
      color = skin.onSurface.copy(alpha = 0.85f),
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.6.sp
    )
  }
}
