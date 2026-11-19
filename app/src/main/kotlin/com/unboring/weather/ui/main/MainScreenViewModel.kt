package com.unboring.weather.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unboring.weather.data.AppReleaseInfo
import com.unboring.weather.data.AppUpdater
import com.unboring.weather.data.LicenseManager
import com.unboring.weather.data.LocationHelper
import com.unboring.weather.data.OpenMeteoRepository
import com.unboring.weather.feedback.WeatherAudio
import com.unboring.weather.feedback.WeatherHaptics
import com.unboring.weather.model.SkinId
import com.unboring.weather.model.WeatherCondition
import com.unboring.weather.model.WeatherLocation
import com.unboring.weather.model.WeatherModelSource
import com.unboring.weather.model.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
  val snapshot: WeatherSnapshot,
  val selectedHourIndex: Int = 0,
  val showWeekly: Boolean = false,
  val showCallouts: Boolean = false,
  val cloudSeed: Int = 1,
  val skinId: SkinId = SkinId.GRAPHITE,
  val isPro: Boolean = false,
  val useCelsius: Boolean = false,
  val soundEnabled: Boolean = true,
  val hapticsEnabled: Boolean = true,
  val isLoading: Boolean = false,
  val modelSource: WeatherModelSource = WeatherModelSource.BEST_MATCH,
  val availableUpdate: AppReleaseInfo? = null,
  val isUpdating: Boolean = false
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = OpenMeteoRepository()
  private val licenseManager = LicenseManager(application)
  private val appUpdater = AppUpdater(application)
  private val locationHelper = LocationHelper(application)
  val haptics = WeatherHaptics(application)
  val audio = WeatherAudio()

  private val initialLocation = licenseManager.getSavedLocation(repository.defaultLocations.first())

  private val _uiState = MutableStateFlow(
    WeatherUiState(
      snapshot = repository.createFallbackSnapshot(initialLocation),
      skinId = licenseManager.selectedSkin,
      isPro = licenseManager.isPro,
      useCelsius = licenseManager.useCelsius,
      soundEnabled = licenseManager.soundEnabled,
      hapticsEnabled = licenseManager.hapticsEnabled,
      isLoading = true
    )
  )
  val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

  val presetLocations: List<WeatherLocation> = repository.defaultLocations

  init {
    haptics.enabled = licenseManager.hapticsEnabled
    audio.enabled = licenseManager.soundEnabled
    loadWeather(initialLocation)
    viewModelScope.launch {
      val update = appUpdater.checkForUpdate()
      if (update != null) {
        _uiState.update { it.copy(availableUpdate = update) }
      }
    }
  }

  fun installUpdate() {
    val info = _uiState.value.availableUpdate ?: return
    if (_uiState.value.isUpdating) return
    _uiState.update { it.copy(isUpdating = true) }
    viewModelScope.launch {
      appUpdater.downloadAndInstall(info.apkDownloadUrl)
      _uiState.update { it.copy(isUpdating = false) }
    }
  }

  fun loadWeather(location: WeatherLocation) {
    licenseManager.saveLocation(location)
    _uiState.update { it.copy(isLoading = true) }
    val model = _uiState.value.modelSource
    viewModelScope.launch {
      val snap = repository.fetchWeather(location, model)
      _uiState.update {
        it.copy(
          snapshot = snap,
          selectedHourIndex = 0,
          isLoading = false
        )
      }
    }
  }

  fun cycleWeatherModel() {
    haptics.buttonClick()
    val values = WeatherModelSource.entries
    val next = values[(values.indexOf(_uiState.value.modelSource) + 1) % values.size]
    _uiState.update { it.copy(modelSource = next) }
    loadWeather(_uiState.value.snapshot.location)
  }

  fun useDeviceLocation() {
    viewModelScope.launch {
      val loc = locationHelper.getCurrentWeatherLocation()
      if (loc != null) {
        haptics.cloudPop()
        loadWeather(loc)
      }
    }
  }

  fun selectHour(index: Int) {
    val maxIdx = (_uiState.value.snapshot.hourly.size - 1).coerceAtLeast(0)
    val clamped = index.coerceIn(0, maxIdx)
    if (clamped != _uiState.value.selectedHourIndex) {
      haptics.scrubTick()
      audio.playScrubTick()
      val hour = _uiState.value.snapshot.hourly.getOrNull(clamped)
      if (hour?.condition == WeatherCondition.THUNDERSTORM) {
        haptics.thunderRumble()
        audio.playThunder()
      }
      _uiState.update { it.copy(selectedHourIndex = clamped) }
    }
  }

  fun toggleWeekly() {
    haptics.buttonClick()
    audio.playScrubTick()
    _uiState.update { it.copy(showWeekly = !it.showWeekly) }
  }

  fun onSceneTap() {
    haptics.cloudPop()
    audio.playCloudPop()
    _uiState.update {
      it.copy(
        showCallouts = !it.showCallouts,
        cloudSeed = it.cloudSeed + 1
      )
    }
  }

  fun onCloudburstPop() {
    haptics.cloudPop()
    audio.playCloudPop()
  }

  fun onScrubTick() {
    haptics.scrubTick()
    audio.playScrubTick()
  }

  fun selectSkin(skinId: SkinId) {
    if (skinId.isPro && !licenseManager.isPro) return
    haptics.buttonClick()
    licenseManager.selectedSkin = skinId
    _uiState.update { it.copy(skinId = skinId) }
  }

  fun toggleUnit() {
    haptics.buttonClick()
    val next = !_uiState.value.useCelsius
    licenseManager.useCelsius = next
    _uiState.update { it.copy(useCelsius = next) }
  }

  fun toggleSound() {
    haptics.buttonClick()
    val next = !_uiState.value.soundEnabled
    licenseManager.soundEnabled = next
    audio.enabled = next
    _uiState.update { it.copy(soundEnabled = next) }
  }

  suspend fun searchCities(query: String): List<WeatherLocation> = repository.searchCities(query)

  suspend fun verifyLicense(key: String): Result<String> {
    val res = licenseManager.verifyAndActivateKey(key)
    if (res.isSuccess) {
      haptics.cloudPop()
      _uiState.update { it.copy(isPro = true) }
    }
    return res
  }
}
