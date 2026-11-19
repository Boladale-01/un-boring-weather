package com.unboring.weather.model

import androidx.compose.ui.graphics.Color

enum class WeatherCondition(val label: String) {
  CLEAR("Clear"),
  MOSTLY_CLEAR("Mostly Clear"),
  PARTLY_CLOUDY("Partly Cloudy"),
  CLOUDY("Mostly Cloudy"),
  FOG("Foggy"),
  DRIZZLE("Light Rain"),
  RAIN("Rain"),
  HEAVY_RAIN("Heavy Rain"),
  THUNDERSTORM("Thunderstorm"),
  SNOW("Snow")
}

enum class ShaderMode {
  STANDARD,
  METALLIC,
  NORMAL_DEBUG,
  WIREFRAME
}

enum class SkinId(val sku: String, val displayName: String, val subtitle: String, val isPro: Boolean) {
  GRAPHITE("SKN001", "Graphite", "Matte obsidian & coral sun", false),
  CHROMA("SKN002", "Chroma", "Bake in the golden rays", false),
  PAPER("SKN003", "Paper", "Crisp editorial daylight", false),
  GUAVA("SKN004", "Guava", "Neon citrus & pink frosting", true),
  KARAT("SKN005", "Karat", "24k gilded metallic 3D", true),
  MONSTERS("SKN006", "Monsters", "Playful eyes & vibrant pop", true),
  NORMAL("SKN007", "Normal", "Reveal the 3D contour", true),
  WIREFRAME("SKN008", "Wireframe", "Reveal the 3D structure", true)
}

data class WeatherSkin(
  val id: SkinId,
  val background: Color,
  val backgroundNight: Color,
  val surface: Color,
  val onSurface: Color,
  val numeralPrimary: Color,
  val numeralExtrude: Color,
  val sunColor: Color,
  val cloudColor: Color,
  val rainColor: Color,
  val accent: Color,
  val pillBackground: Color,
  val shaderMode: ShaderMode,
  val hasGooglyEyes: Boolean = false
)

object SkinCatalog {
  val all: List<WeatherSkin> = listOf(
    WeatherSkin(
      id = SkinId.GRAPHITE,
      background = Color(0xFF141517),
      backgroundNight = Color(0xFF0B0C0E),
      surface = Color(0xFF1F2126),
      onSurface = Color(0xFFF4F4F6),
      numeralPrimary = Color(0xFF2B2D33),
      numeralExtrude = Color(0xFF0A0B0D),
      sunColor = Color(0xFFFF3B30),
      cloudColor = Color(0xFF2D3038),
      rainColor = Color(0xFF2EA8FF),
      accent = Color(0xFF2EA8FF),
      pillBackground = Color(0xFF252830),
      shaderMode = ShaderMode.STANDARD
    ),
    WeatherSkin(
      id = SkinId.CHROMA,
      background = Color(0xFFFFAE00),
      backgroundNight = Color(0xFF2A1900),
      surface = Color(0xFFFFC133),
      onSurface = Color(0xFF1A1200),
      numeralPrimary = Color(0xFF221600),
      numeralExtrude = Color(0xFF000000),
      sunColor = Color(0xFFFFFFFF),
      cloudColor = Color(0xFFFFE08A),
      rainColor = Color(0xFF0088FF),
      accent = Color(0xFF0088FF),
      pillBackground = Color(0xFFE59B00),
      shaderMode = ShaderMode.STANDARD
    ),
    WeatherSkin(
      id = SkinId.PAPER,
      background = Color(0xFFF3F2EE),
      backgroundNight = Color(0xFF1A1B1E),
      surface = Color(0xFFE5E3DC),
      onSurface = Color(0xFF161618),
      numeralPrimary = Color(0xFF9C9B96),
      numeralExtrude = Color(0xFF6B6A65),
      sunColor = Color(0xFFFF5A4E),
      cloudColor = Color(0xFF3E3F44),
      rainColor = Color(0xFF1C8EFF),
      accent = Color(0xFF1C8EFF),
      pillBackground = Color(0xFFDAD8D0),
      shaderMode = ShaderMode.STANDARD
    ),
    WeatherSkin(
      id = SkinId.GUAVA,
      background = Color(0xFFF7E600),
      backgroundNight = Color(0xFF2E0A26),
      surface = Color(0xFFFFF04D),
      onSurface = Color(0xFF26001E),
      numeralPrimary = Color(0xFFFF4DA6),
      numeralExtrude = Color(0xFFC2186A),
      sunColor = Color(0xFFFFFFFF),
      cloudColor = Color(0xFFFF59AC),
      rainColor = Color(0xFF00D4FF),
      accent = Color(0xFFFF4DA6),
      pillBackground = Color(0xFFE0D000),
      shaderMode = ShaderMode.STANDARD
    ),
    WeatherSkin(
      id = SkinId.KARAT,
      background = Color(0xFF0F0E0B),
      backgroundNight = Color(0xFF080705),
      surface = Color(0xFF1D1A12),
      onSurface = Color(0xFFFFE599),
      numeralPrimary = Color(0xFFFFC94A),
      numeralExtrude = Color(0xFF7A5600),
      sunColor = Color(0xFFFFD76B),
      cloudColor = Color(0xFFD4A024),
      rainColor = Color(0xFF62C8FF),
      accent = Color(0xFFFFD76B),
      pillBackground = Color(0xFF262115),
      shaderMode = ShaderMode.METALLIC
    ),
    WeatherSkin(
      id = SkinId.MONSTERS,
      background = Color(0xFFF2ECE1),
      backgroundNight = Color(0xFF191D2E),
      surface = Color(0xFFE4DDD0),
      onSurface = Color(0xFF1C1C24),
      numeralPrimary = Color(0xFF7C5CFF),
      numeralExtrude = Color(0xFF3B2499),
      sunColor = Color(0xFFFF9F1C),
      cloudColor = Color(0xFF3A86FF),
      rainColor = Color(0xFF00BBF9),
      accent = Color(0xFFFF4D8D),
      pillBackground = Color(0xFFD8D0C2),
      shaderMode = ShaderMode.STANDARD,
      hasGooglyEyes = true
    ),
    WeatherSkin(
      id = SkinId.NORMAL,
      background = Color(0xFF111218),
      backgroundNight = Color(0xFF090A0E),
      surface = Color(0xFF1C1E28),
      onSurface = Color(0xFFE8ECF8),
      numeralPrimary = Color(0xFF80FFB5),
      numeralExtrude = Color(0xFF426BFF),
      sunColor = Color(0xFFFF7AE0),
      cloudColor = Color(0xFF80C4FF),
      rainColor = Color(0xFF5CE1E6),
      accent = Color(0xFF80FFB5),
      pillBackground = Color(0xFF232635),
      shaderMode = ShaderMode.NORMAL_DEBUG
    ),
    WeatherSkin(
      id = SkinId.WIREFRAME,
      background = Color(0xFF090B0E),
      backgroundNight = Color(0xFF050608),
      surface = Color(0xFF131820),
      onSurface = Color(0xFF00FFAA),
      numeralPrimary = Color(0xFF00FFAA),
      numeralExtrude = Color(0xFF007A52),
      sunColor = Color(0xFF00FFAA),
      cloudColor = Color(0xFF00FFAA),
      rainColor = Color(0xFF00D4FF),
      accent = Color(0xFF00FFAA),
      pillBackground = Color(0xFF151C24),
      shaderMode = ShaderMode.WIREFRAME
    )
  )

  fun byId(id: SkinId): WeatherSkin = all.first { it.id == id }
}

data class HourlyPoint(
  val epochSeconds: Long,
  val hourLabel: String,
  val shortTickLabel: String,
  val tempF: Double,
  val feelsLikeF: Double,
  val condition: WeatherCondition,
  val precipProbability: Int,
  val precipMm: Double,
  val cloudCover: Int,
  val windSpeedMph: Double,
  val windDirectionDeg: Int,
  val humidity: Int,
  val dewPointF: Double,
  val pressureInHg: Double,
  val uvIndex: Double,
  val visibilityMiles: Double,
  val isDay: Boolean
)

data class DailyForecast(
  val dayLabel: String,
  val highF: Double,
  val lowF: Double,
  val condition: WeatherCondition,
  val precipProbability: Int
)

data class MinutePrecipPoint(
  val offsetMinutes: Int,
  val label: String,
  val rateInPerHr: Double,
  val probability: Int
)

data class MoonInfo(
  val phaseName: String,
  val illuminationPercent: Int,
  val phaseFraction: Double, // 0.0 = new, 0.5 = full
  val nextFullMoonDate: String,
  val nextNewMoonDate: String
)

data class AirQualityInfo(
  val usAqi: Int,
  val rating: String,
  val pm25: Double,
  val pm10: Double,
  val pollenLevel: String
)

data class WeatherLocation(
  val name: String,
  val region: String,
  val latitude: Double,
  val longitude: Double
)

data class WeatherSnapshot(
  val location: WeatherLocation,
  val sunriseLabel: String,
  val sunsetLabel: String,
  val hourly: List<HourlyPoint>,
  val daily: List<DailyForecast>,
  val minutely: List<MinutePrecipPoint>,
  val moon: MoonInfo,
  val airQuality: AirQualityInfo,
  val severeAlertTitle: String? = null,
  val severeAlertBody: String? = null,
  val isFallback: Boolean = false
)
