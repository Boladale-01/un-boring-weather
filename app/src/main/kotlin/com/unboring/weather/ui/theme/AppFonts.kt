package com.unboring.weather.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.unboring.weather.R

@OptIn(ExperimentalTextApi::class)
val AntonioDisplay = FontFamily(
  Font(
    resId = R.font.antonio_bold,
    weight = FontWeight.Bold,
    variationSettings = FontVariation.Settings(FontVariation.weight(700))
  ),
  Font(
    resId = R.font.antonio_bold,
    weight = FontWeight.Black,
    variationSettings = FontVariation.Settings(FontVariation.weight(700))
  )
)

@OptIn(ExperimentalTextApi::class)
val AtkinsonMono = FontFamily(
  Font(
    resId = R.font.atkinson_mono,
    weight = FontWeight.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(400))
  ),
  Font(
    resId = R.font.atkinson_mono,
    weight = FontWeight.Bold,
    variationSettings = FontVariation.Settings(FontVariation.weight(700))
  )
)
