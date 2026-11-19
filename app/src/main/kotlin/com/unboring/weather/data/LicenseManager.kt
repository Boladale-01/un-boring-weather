package com.unboring.weather.data

import android.content.Context
import android.content.SharedPreferences
import com.unboring.weather.model.SkinId
import com.unboring.weather.model.WeatherLocation
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Manages Pro License Key validation (Gumroad / Lemon Squeezy) and user preferences.
 * Replace [GUMROAD_PRODUCT_ID] and [CHECKOUT_URL] with your own product details before release.
 */
class LicenseManager(context: Context) {

  companion object {
    const val CHECKOUT_URL = "https://gumroad.com/"
    private const val GUMROAD_PRODUCT_ID = "unboring-weather-pro"
    private const val DEMO_KEY = "UNBORING-PRO-DEMO"
    private const val PREFS_NAME = "unboring_weather_prefs"
    private const val KEY_IS_PRO = "is_pro"
    private const val KEY_LICENSE = "license_key"
    private const val KEY_SKIN = "skin_id"
    private const val KEY_USE_CELSIUS = "use_celsius"
    private const val KEY_SOUND = "sound_enabled"
    private const val KEY_HAPTICS = "haptics_enabled"
    private const val KEY_LOC_NAME = "loc_name"
    private const val KEY_LOC_REGION = "loc_region"
    private const val KEY_LOC_LAT = "loc_lat"
    private const val KEY_LOC_LON = "loc_lon"
  }

  private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val json = Json { ignoreUnknownKeys = true }

  var isPro: Boolean
    get() = prefs.getBoolean(KEY_IS_PRO, false)
    private set(value) = prefs.edit().putBoolean(KEY_IS_PRO, value).apply()

  var savedLicenseKey: String
    get() = prefs.getString(KEY_LICENSE, "") ?: ""
    private set(value) = prefs.edit().putString(KEY_LICENSE, value).apply()

  var selectedSkin: SkinId
    get() {
      val name = prefs.getString(KEY_SKIN, SkinId.GRAPHITE.name) ?: SkinId.GRAPHITE.name
      return runCatching { SkinId.valueOf(name) }.getOrDefault(SkinId.GRAPHITE)
    }
    set(value) = prefs.edit().putString(KEY_SKIN, value.name).apply()

  var useCelsius: Boolean
    get() = prefs.getBoolean(KEY_USE_CELSIUS, false)
    set(value) = prefs.edit().putBoolean(KEY_USE_CELSIUS, value).apply()

  var soundEnabled: Boolean
    get() = prefs.getBoolean(KEY_SOUND, true)
    set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

  var hapticsEnabled: Boolean
    get() = prefs.getBoolean(KEY_HAPTICS, true)
    set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

  fun getSavedLocation(defaultLocation: WeatherLocation): WeatherLocation {
    val name = prefs.getString(KEY_LOC_NAME, null) ?: return defaultLocation
    val region = prefs.getString(KEY_LOC_REGION, "") ?: ""
    val lat = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LOC_LAT, java.lang.Double.doubleToRawLongBits(defaultLocation.latitude)))
    val lon = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LOC_LON, java.lang.Double.doubleToRawLongBits(defaultLocation.longitude)))
    return WeatherLocation(name, region, lat, lon)
  }

  fun saveLocation(location: WeatherLocation) {
    prefs.edit()
      .putString(KEY_LOC_NAME, location.name)
      .putString(KEY_LOC_REGION, location.region)
      .putLong(KEY_LOC_LAT, java.lang.Double.doubleToRawLongBits(location.latitude))
      .putLong(KEY_LOC_LON, java.lang.Double.doubleToRawLongBits(location.longitude))
      .apply()
  }

  suspend fun verifyAndActivateKey(rawKey: String): Result<String> = withContext(Dispatchers.IO) {
    val key = rawKey.trim().uppercase()
    if (key.isEmpty()) {
      return@withContext Result.failure(IllegalArgumentException("Please enter a license key."))
    }
    if (key == DEMO_KEY) {
      isPro = true
      savedLicenseKey = key
      return@withContext Result.success("Demo Pro activated! All 3D Skins unlocked.")
    }

    // Gumroad License Verification API
    return@withContext try {
      val conn = (URL("https://api.gumroad.com/v2/licenses/verify").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
      }
      val payload = "product_id=${URLEncoder.encode(GUMROAD_PRODUCT_ID, "UTF-8")}" +
        "&license_key=${URLEncoder.encode(key, "UTF-8")}"
      OutputStreamWriter(conn.outputStream).use { it.write(payload) }

      val code = conn.responseCode
      val stream = if (code in 200..299) conn.inputStream else conn.errorStream
      val body = stream.bufferedReader().use { it.readText() }
      val obj = json.parseToJsonElement(body).jsonObject
      val success = obj["success"]?.jsonPrimitive?.booleanOrNull == true
      if (success) {
        isPro = true
        savedLicenseKey = key
        Result.success("Pro unlocked! Thank you for supporting Un Boring Weather.")
      } else {
        val msg = obj["message"]?.jsonPrimitive?.content ?: "Invalid license key."
        Result.failure(IllegalStateException(msg))
      }
    } catch (e: Exception) {
      Result.failure(IllegalStateException("Could not verify key (${e.message ?: "network error"}). Try $DEMO_KEY to test."))
    }
  }

  fun lockProForTesting() {
    isPro = false
    savedLicenseKey = ""
  }
}
