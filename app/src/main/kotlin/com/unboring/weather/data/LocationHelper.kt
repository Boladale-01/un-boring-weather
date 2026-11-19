package com.unboring.weather.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.unboring.weather.model.WeatherLocation
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationHelper(private val context: Context) {

  fun hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  suspend fun getCurrentWeatherLocation(): WeatherLocation? = withContext(Dispatchers.IO) {
    if (!hasLocationPermission()) return@withContext null
    try {
      val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
      val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
      )
      var best: Location? = null
      for (p in providers) {
        if (lm.isProviderEnabled(p)) {
          val loc = lm.getLastKnownLocation(p)
          if (loc != null && (best == null || loc.accuracy < best.accuracy)) {
            best = loc
          }
        }
      }
      val resolved = best ?: return@withContext null
      val (city, region) = reverseGeocode(resolved.latitude, resolved.longitude)
      WeatherLocation(
        name = city,
        region = region,
        latitude = resolved.latitude,
        longitude = resolved.longitude
      )
    } catch (_: Exception) {
      null
    }
  }

  @Suppress("DEPRECATION")
  private fun reverseGeocode(lat: Double, lon: Double): Pair<String, String> {
    return try {
      val geocoder = Geocoder(context, Locale.getDefault())
      val list = geocoder.getFromLocation(lat, lon, 1)
      val addr = list?.firstOrNull()
      val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: "Current Location"
      val region = listOfNotNull(addr?.adminArea, addr?.countryName).distinct().joinToString(", ")
      city to region
    } catch (_: Exception) {
      "Current Location" to "${String.format(Locale.US, "%.2f", lat)}, ${String.format(Locale.US, "%.2f", lon)}"
    }
  }
}
