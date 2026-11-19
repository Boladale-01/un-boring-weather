package com.unboring.weather.data

import com.unboring.weather.model.AirQualityInfo
import com.unboring.weather.model.DailyForecast
import com.unboring.weather.model.HourlyPoint
import com.unboring.weather.model.MinutePrecipPoint
import com.unboring.weather.model.MoonInfo
import com.unboring.weather.model.WeatherCondition
import com.unboring.weather.model.WeatherLocation
import com.unboring.weather.model.WeatherModelSource
import com.unboring.weather.model.WeatherSnapshot
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenMeteoRepository {

  private val json = Json { ignoreUnknownKeys = true }

  val defaultLocations = listOf(
    WeatherLocation("Seattle", "Washington, US", 47.6062, -122.3321),
    WeatherLocation("Tokyo", "Japan", 35.6762, 139.6503),
    WeatherLocation("New York", "New York, US", 40.7128, -74.0060),
    WeatherLocation("London", "United Kingdom", 51.5072, -0.1276),
    WeatherLocation("San Francisco", "California, US", 37.7749, -122.4194)
  )

  suspend fun fetchWeather(
    location: WeatherLocation,
    modelSource: WeatherModelSource = WeatherModelSource.BEST_MATCH
  ): WeatherSnapshot = withContext(Dispatchers.IO) {
    try {
      val modelQuery = if (modelSource == WeatherModelSource.BEST_MATCH) "" else "&models=${modelSource.apiParam}"
      val forecastUrl =
        "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}" +
          "&hourly=temperature_2m,apparent_temperature,relative_humidity_2m,dew_point_2m," +
          "precipitation_probability,precipitation,weather_code,surface_pressure,cloud_cover," +
          "visibility,wind_speed_10m,wind_direction_10m,uv_index,is_day" +
          "&minutely_15=precipitation,precipitation_probability" +
          "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max" +
          "&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch&timeformat=unixtime&timezone=auto&forecast_days=8$modelQuery"

      val aqiUrl =
        "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${location.latitude}&longitude=${location.longitude}" +
          "&current=us_aqi,pm10,pm2_5,grass_pollen,birch_pollen,ragweed_pollen"

      val forecastBody = httpGet(forecastUrl)
      val forecastRoot = json.parseToJsonElement(forecastBody).jsonObject

      val aqiInfo = try {
        val aqiBody = httpGet(aqiUrl)
        parseAirQuality(json.parseToJsonElement(aqiBody).jsonObject)
      } catch (_: Exception) {
        AirQualityInfo(usAqi = 34, rating = "GOOD", pm25 = 6.8, pm10 = 14.2, pollenLevel = "Low")
      }

      parseForecast(location, forecastRoot, aqiInfo)
    } catch (_: Exception) {
      createFallbackSnapshot(location)
    }
  }

  suspend fun searchCities(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
    if (query.trim().length < 2) return@withContext defaultLocations
    try {
      val encoded = URLEncoder.encode(query.trim(), "UTF-8")
      val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=en&format=json"
      val body = httpGet(url)
      val root = json.parseToJsonElement(body).jsonObject
      val results = root["results"]?.jsonArray ?: return@withContext emptyList()
      results.mapNotNull { el ->
        val obj = el.jsonObject
        val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val country = obj["country"]?.jsonPrimitive?.content ?: ""
        val admin = obj["admin1"]?.jsonPrimitive?.content ?: ""
        val lat = obj["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        val lon = obj["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        val region = listOf(admin, country).filter { it.isNotBlank() }.joinToString(", ")
        WeatherLocation(name, region, lat, lon)
      }
    } catch (_: Exception) {
      defaultLocations.filter { it.name.contains(query, ignoreCase = true) }
    }
  }

  private fun httpGet(urlString: String): String {
    val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
      connectTimeout = 8000
      readTimeout = 8000
      requestMethod = "GET"
    }
    return conn.inputStream.bufferedReader().use { it.readText() }
  }

  private fun parseAirQuality(root: JsonObject): AirQualityInfo {
    val current = root["current"]?.jsonObject ?: return AirQualityInfo(32, "GOOD", 6.1, 12.4, "Low")
    val aqi = current["us_aqi"]?.jsonPrimitive?.intOrNull ?: 32
    val pm25 = current["pm2_5"]?.jsonPrimitive?.doubleOrNull ?: 6.1
    val pm10 = current["pm10"]?.jsonPrimitive?.doubleOrNull ?: 12.4
    val grass = current["grass_pollen"]?.jsonPrimitive?.doubleOrNull ?: 0.0
    val birch = current["birch_pollen"]?.jsonPrimitive?.doubleOrNull ?: 0.0
    val ragweed = current["ragweed_pollen"]?.jsonPrimitive?.doubleOrNull ?: 0.0
    val maxPollen = maxOf(grass, birch, ragweed)
    val pollenLabel = when {
      maxPollen > 50 -> "High"
      maxPollen > 15 -> "Moderate"
      else -> "Low"
    }
    val rating = when {
      aqi <= 50 -> "GOOD"
      aqi <= 100 -> "MODERATE"
      aqi <= 150 -> "SENSITIVE"
      else -> "UNHEALTHY"
    }
    return AirQualityInfo(aqi, rating, pm25, pm10, pollenLabel)
  }

  private fun parseForecast(
    location: WeatherLocation,
    root: JsonObject,
    aqiInfo: AirQualityInfo
  ): WeatherSnapshot {
    val utcOffsetSeconds = root["utc_offset_seconds"]?.jsonPrimitive?.intOrNull ?: 0
    val tz = TimeZone.getTimeZone("GMT").apply { rawOffset = utcOffsetSeconds * 1000 }
    val hourFmt = SimpleDateFormat("ha", Locale.US).apply { timeZone = tz }
    val dayFmt = SimpleDateFormat("EEE", Locale.US).apply { timeZone = tz }

    val hourlyObj = root["hourly"]!!.jsonObject
    val times = hourlyObj["time"]!!.jsonArray
    val temps = hourlyObj["temperature_2m"]!!.jsonArray
    val feels = hourlyObj["apparent_temperature"]!!.jsonArray
    val hums = hourlyObj["relative_humidity_2m"]!!.jsonArray
    val dews = hourlyObj["dew_point_2m"]!!.jsonArray
    val pop = hourlyObj["precipitation_probability"]!!.jsonArray
    val precip = hourlyObj["precipitation"]!!.jsonArray
    val codes = hourlyObj["weather_code"]!!.jsonArray
    val press = hourlyObj["surface_pressure"]!!.jsonArray
    val clouds = hourlyObj["cloud_cover"]!!.jsonArray
    val vis = hourlyObj["visibility"]!!.jsonArray
    val windSpd = hourlyObj["wind_speed_10m"]!!.jsonArray
    val windDir = hourlyObj["wind_direction_10m"]!!.jsonArray
    val uvs = hourlyObj["uv_index"]!!.jsonArray
    val isDays = hourlyObj["is_day"]!!.jsonArray

    val nowEpoch = System.currentTimeMillis() / 1000L
    var startIdx = 0
    for (i in 0 until times.size) {
      val t = times[i].jsonPrimitive.content.toLongOrNull() ?: 0L
      if (t >= nowEpoch - 1800) {
        startIdx = i
        break
      }
    }

    val hourlyList = mutableListOf<HourlyPoint>()
    val count = minOf(24, times.size - startIdx)
    for (offset in 0 until count) {
      val i = startIdx + offset
      val epoch = times[i].jsonPrimitive.content.toLongOrNull() ?: nowEpoch
      val date = Date(epoch * 1000L)
      val rawHour = hourFmt.format(date)
      val hourLabel = if (offset == 0) "NOW" else rawHour
      val tickLabel = if (offset == 0) "NOW" else rawHour.replace("AM", "A").replace("PM", "P")
      val wmo = codes[i].jsonPrimitive.intOrNull ?: 0
      val pressureHpa = press[i].jsonPrimitive.doubleOrNull ?: 1013.25
      val visMeters = vis[i].jsonPrimitive.doubleOrNull ?: 16000.0

      hourlyList.add(
        HourlyPoint(
          epochSeconds = epoch,
          hourLabel = hourLabel,
          shortTickLabel = tickLabel,
          tempF = temps[i].jsonPrimitive.doubleOrNull ?: 65.0,
          feelsLikeF = feels[i].jsonPrimitive.doubleOrNull ?: 65.0,
          condition = mapWmoCode(wmo),
          precipProbability = pop[i].jsonPrimitive.intOrNull ?: 0,
          precipMm = (precip[i].jsonPrimitive.doubleOrNull ?: 0.0) * 25.4,
          cloudCover = clouds[i].jsonPrimitive.intOrNull ?: 20,
          windSpeedMph = windSpd[i].jsonPrimitive.doubleOrNull ?: 5.0,
          windDirectionDeg = windDir[i].jsonPrimitive.intOrNull ?: 180,
          humidity = hums[i].jsonPrimitive.intOrNull ?: 50,
          dewPointF = dews[i].jsonPrimitive.doubleOrNull ?: 48.0,
          pressureInHg = (pressureHpa * 0.02953 * 10.0).roundToInt() / 10.0,
          uvIndex = uvs[i].jsonPrimitive.doubleOrNull ?: 1.0,
          visibilityMiles = ((visMeters / 1609.34) * 10.0).roundToInt() / 10.0,
          isDay = (isDays[i].jsonPrimitive.intOrNull ?: 1) == 1
        )
      )
    }

    val dailyObj = root["daily"]!!.jsonObject
    val dTimes = dailyObj["time"]!!.jsonArray
    val dCodes = dailyObj["weather_code"]!!.jsonArray
    val dMax = dailyObj["temperature_2m_max"]!!.jsonArray
    val dMin = dailyObj["temperature_2m_min"]!!.jsonArray
    val dPop = dailyObj["precipitation_probability_max"]!!.jsonArray
    val dSunrise = dailyObj["sunrise"]?.jsonArray
    val dSunset = dailyObj["sunset"]?.jsonArray

    val sunriseStr = dSunrise?.getOrNull(0)?.jsonPrimitive?.content?.toLongOrNull()?.let {
      hourFmt.format(Date(it * 1000L)).replace("AM", "A").replace("PM", "P")
    } ?: "6:42A"
    val sunsetStr = dSunset?.getOrNull(0)?.jsonPrimitive?.content?.toLongOrNull()?.let {
      hourFmt.format(Date(it * 1000L)).replace("AM", "A").replace("PM", "P")
    } ?: "7:18P"

    val dailyList = (0 until minOf(8, dTimes.size)).map { idx ->
      val epoch = dTimes[idx].jsonPrimitive.content.toLongOrNull() ?: nowEpoch
      val label = if (idx == 0) "TODAY" else dayFmt.format(Date(epoch * 1000L)).uppercase(Locale.US)
      DailyForecast(
        dayLabel = label,
        highF = dMax[idx].jsonPrimitive.doubleOrNull ?: 65.0,
        lowF = dMin[idx].jsonPrimitive.doubleOrNull ?: 48.0,
        condition = mapWmoCode(dCodes[idx].jsonPrimitive.intOrNull ?: 0),
        precipProbability = dPop[idx].jsonPrimitive.intOrNull ?: 0
      )
    }

    val minutelyList = parseMinutely(root["minutely_15"]?.jsonObject, nowEpoch)
    val hasThunder = hourlyList.take(6).any { it.condition == WeatherCondition.THUNDERSTORM }

    return WeatherSnapshot(
      location = location,
      sunriseLabel = sunriseStr,
      sunsetLabel = sunsetStr,
      hourly = hourlyList,
      daily = dailyList,
      minutely = minutelyList,
      moon = computeMoonInfo(),
      airQuality = aqiInfo,
      severeAlertTitle = if (hasThunder) "THUNDERSTORM" else null,
      severeAlertBody = if (hasThunder) "Lightning and gusty winds detected in the next 6 hours." else null,
      isFallback = false
    )
  }

  private fun parseMinutely(minObj: JsonObject?, nowEpoch: Long): List<MinutePrecipPoint> {
    if (minObj == null) return defaultMinutely()
    val times = minObj["time"]?.jsonArray ?: return defaultMinutely()
    val precips = minObj["precipitation"]?.jsonArray ?: return defaultMinutely()
    val pops = minObj["precipitation_probability"]?.jsonArray
    var start = 0
    for (i in 0 until times.size) {
      val t = times[i].jsonPrimitive.content.toLongOrNull() ?: 0L
      if (t >= nowEpoch - 900) {
        start = i
        break
      }
    }
    return (0 until minOf(5, times.size - start)).map { idx ->
      val i = start + idx
      val mins = idx * 15
      MinutePrecipPoint(
        offsetMinutes = mins,
        label = if (mins == 0) "NOW" else "${mins}m",
        rateInPerHr = (precips[i].jsonPrimitive.doubleOrNull ?: 0.0) * 4.0,
        probability = pops?.getOrNull(i)?.jsonPrimitive?.intOrNull ?: 0
      )
    }
  }

  private fun defaultMinutely(): List<MinutePrecipPoint> = listOf(
    MinutePrecipPoint(0, "NOW", 0.02, 40),
    MinutePrecipPoint(15, "15m", 0.06, 65),
    MinutePrecipPoint(30, "30m", 0.09, 78),
    MinutePrecipPoint(45, "45m", 0.04, 52),
    MinutePrecipPoint(60, "60m", 0.01, 25)
  )

  private fun mapWmoCode(code: Int): WeatherCondition = when (code) {
    0 -> WeatherCondition.CLEAR
    1 -> WeatherCondition.MOSTLY_CLEAR
    2 -> WeatherCondition.PARTLY_CLOUDY
    3 -> WeatherCondition.CLOUDY
    45, 48 -> WeatherCondition.FOG
    51, 53, 55, 56, 57 -> WeatherCondition.DRIZZLE
    61, 63, 66, 80, 81 -> WeatherCondition.RAIN
    65, 67, 82 -> WeatherCondition.HEAVY_RAIN
    71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
    95, 96, 99 -> WeatherCondition.THUNDERSTORM
    else -> WeatherCondition.PARTLY_CLOUDY
  }

  fun computeMoonInfo(nowMillis: Long = System.currentTimeMillis()): MoonInfo {
    val synodicMonth = 29.53058867
    // Known New Moon reference: Jan 11, 2024 11:57 UTC
    val refNewMoon = 1704974220000L
    val daysSince = (nowMillis - refNewMoon) / 86400000.0
    val cyclePos = ((daysSince % synodicMonth) + synodicMonth) % synodicMonth
    val fraction = cyclePos / synodicMonth
    val illumination = ((1.0 - cos(2.0 * PI * fraction)) * 50.0).roundToInt().coerceIn(0, 100)

    val phaseName = when {
      fraction < 0.04 || fraction >= 0.96 -> "NEW MOON"
      fraction < 0.22 -> "WAXING CRESCENT"
      fraction < 0.28 -> "FIRST QUARTER"
      fraction < 0.46 -> "WAXING GIBBOUS"
      fraction < 0.54 -> "FULL MOON"
      fraction < 0.72 -> "WANING GIBBOUS"
      fraction < 0.78 -> "LAST QUARTER"
      else -> "WANING CRESCENT"
    }

    val daysToFull = ((0.5 - fraction).let { if (it < 0) it + 1.0 else it }) * synodicMonth
    val daysToNew = (1.0 - fraction) * synodicMonth
    val dateFmt = SimpleDateFormat("M/d", Locale.US)
    val fullDate = dateFmt.format(Date(nowMillis + (daysToFull * 86400000L).toLong()))
    val newDate = dateFmt.format(Date(nowMillis + (daysToNew * 86400000L).toLong()))

    return MoonInfo(phaseName, illumination, fraction, fullDate, newDate)
  }

  fun createFallbackSnapshot(location: WeatherLocation): WeatherSnapshot {
    val cal = Calendar.getInstance()
    val baseHour = cal.get(Calendar.HOUR_OF_DAY)
    val conditions = listOf(
      WeatherCondition.PARTLY_CLOUDY,
      WeatherCondition.DRIZZLE,
      WeatherCondition.RAIN,
      WeatherCondition.THUNDERSTORM,
      WeatherCondition.CLEAR,
      WeatherCondition.MOSTLY_CLEAR
    )
    val hourly = (0 until 24).map { offset ->
      val h = (baseHour + offset) % 24
      val isDay = h in 6..19
      val amPm = if (h < 12) "AM" else "PM"
      val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
      }
      val label = if (offset == 0) "NOW" else "$h12$amPm"
      val tick = if (offset == 0) "NOW" else "$h12${amPm.first()}"
      val cond = conditions[(offset / 3) % conditions.size]
      val pop = when (cond) {
        WeatherCondition.THUNDERSTORM -> 88
        WeatherCondition.RAIN -> 78
        WeatherCondition.DRIZZLE -> 45
        else -> 12
      }
      val temp = 58.0 + 12.0 * kotlin.math.sin((h - 6) * PI / 12.0)
      HourlyPoint(
        epochSeconds = (System.currentTimeMillis() / 1000L) + offset * 3600L,
        hourLabel = label,
        shortTickLabel = tick,
        tempF = temp,
        feelsLikeF = temp - 2.0,
        condition = cond,
        precipProbability = pop,
        precipMm = if (pop > 40) 1.8 else 0.0,
        cloudCover = if (cond == WeatherCondition.CLEAR) 10 else 72,
        windSpeedMph = 11.0 + (offset % 7),
        windDirectionDeg = 215,
        humidity = 62,
        dewPointF = 47.0,
        pressureInHg = 29.8,
        uvIndex = if (isDay) 4.0 else 0.0,
        visibilityMiles = 8.4,
        isDay = isDay
      )
    }

    val dayNames = listOf("TODAY", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "MON")
    val daily = dayNames.mapIndexed { idx, name ->
      DailyForecast(
        dayLabel = name,
        highF = 64.0 + (idx % 5) * 2,
        lowF = 46.0 + (idx % 4),
        condition = conditions[idx % conditions.size],
        precipProbability = if (idx % 2 == 0) 65 else 15
      )
    }

    return WeatherSnapshot(
      location = location,
      sunriseLabel = "6:48A",
      sunsetLabel = "7:22P",
      hourly = hourly,
      daily = daily,
      minutely = defaultMinutely(),
      moon = computeMoonInfo(),
      airQuality = AirQualityInfo(44, "GOOD", 7.2, 15.1, "Low"),
      severeAlertTitle = "THUNDERSTORM",
      severeAlertBody = "Demo storm cell approaching later today.",
      isFallback = true
    )
  }
}

private fun JsonArray.getOrNull(index: Int) = if (index in 0 until size) this[index] else null
