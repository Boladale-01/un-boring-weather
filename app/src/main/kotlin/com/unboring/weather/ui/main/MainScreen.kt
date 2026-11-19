package com.unboring.weather.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unboring.weather.model.SkinCatalog
import com.unboring.weather.render3d.WeatherGLSurfaceView
import com.unboring.weather.render3d.WeatherSceneState
import com.unboring.weather.ui.components.AirQualityPanel
import com.unboring.weather.ui.components.BlueprintCalloutsOverlay
import com.unboring.weather.ui.components.CloudburstMinigamePanel
import com.unboring.weather.ui.components.ExtrudedTempDisplay
import com.unboring.weather.ui.components.LocationSearchSheet
import com.unboring.weather.ui.components.MoonDetailPanel
import com.unboring.weather.ui.components.PlayTheWeatherBar
import com.unboring.weather.ui.components.PrecipDetailPanel
import com.unboring.weather.ui.components.SkinsAndProSheet
import com.unboring.weather.ui.components.formatTemp
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
  viewModel: MainScreenViewModel = viewModel()
) {
  val state by viewModel.uiState.collectAsState()
  val skin = SkinCatalog.byId(state.skinId)
  val hourly = state.snapshot.hourly
  val activeHour = hourly.getOrElse(state.selectedHourIndex) { hourly.first() }

  var tiltX by remember { mutableFloatStateOf(0f) }
  var tiltY by remember { mutableFloatStateOf(0f) }
  var showSkinsSheet by remember { mutableStateOf(false) }
  var showLocationSheet by remember { mutableStateOf(false) }

  val pagerState = rememberPagerState(pageCount = { 5 })
  val scope = rememberCoroutineScope()
  val bgColor = if (activeHour.isDay) skin.background else skin.backgroundNight

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColor)
  ) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize()
    ) { page ->
      when (page) {
        0 -> {
          // Main 3D Stage
          Box(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(Unit) {
                detectTapGestures { viewModel.onSceneTap() }
              }
              .pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = {
                    tiltX = 0f
                    tiltY = 0f
                  },
                  onDragCancel = {
                    tiltX = 0f
                    tiltY = 0f
                  }
                ) { change, dragAmount ->
                  change.consume()
                  tiltX = (tiltX + dragAmount.x / 320f).coerceIn(-1f, 1f)
                  tiltY = (tiltY + dragAmount.y / 320f).coerceIn(-1f, 1f)
                }
              }
          ) {
            WeatherGLSurfaceView(
              sceneState = WeatherSceneState(
                skinId = state.skinId,
                condition = activeHour.condition,
                cloudCover = activeHour.cloudCover,
                windSpeedMph = activeHour.windSpeedMph,
                windDirectionDeg = activeHour.windDirectionDeg,
                precipProbability = activeHour.precipProbability,
                isDay = activeHour.isDay,
                moonPhaseFraction = state.snapshot.moon.phaseFraction,
                cloudSeed = state.cloudSeed,
                tiltX = tiltX,
                tiltY = tiltY
              ),
              modifier = Modifier.fillMaxSize()
            )

            // Blueprint Telemetry Callouts (tap scene to toggle)
            BlueprintCalloutsOverlay(
              visible = state.showCallouts,
              hour = activeHour,
              sunriseLabel = state.snapshot.sunriseLabel,
              sunsetLabel = state.snapshot.sunsetLabel,
              moon = state.snapshot.moon,
              useCelsius = state.useCelsius,
              skin = skin,
              modifier = Modifier.fillMaxSize()
            )

            // Severe Weather Alert Banner
            state.snapshot.severeAlertTitle?.let { alertTitle ->
              Box(
                modifier = Modifier
                  .align(Alignment.TopCenter)
                  .windowInsetsPadding(WindowInsets.statusBars)
                  .padding(top = 64.dp)
                  .clip(RoundedCornerShape(50))
                  .background(Color(0xFFFFD60A))
                  .padding(horizontal = 14.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "!  $alertTitle  !",
                  color = Color.Black,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace
                )
              }
            }

            // Giant 3D Extruded Temperature
            ExtrudedTempDisplay(
              tempDisplay = formatTemp(activeHour.tempF, state.useCelsius),
              conditionLabel = activeHour.condition.label,
              precipProbability = activeHour.precipProbability,
              skin = skin,
              tiltX = tiltX,
              tiltY = tiltY,
              modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 80.dp)
            )

            // Bottom "Play the Weather" Scrub Bar
            PlayTheWeatherBar(
              hourly = hourly,
              daily = state.snapshot.daily,
              selectedHourIndex = state.selectedHourIndex,
              showWeekly = state.showWeekly,
              useCelsius = state.useCelsius,
              skin = skin,
              onSelectHour = viewModel::selectHour,
              onToggleWeekly = viewModel::toggleWeekly,
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 36.dp)
            )
          }
        }

        1 -> PrecipDetailPanel(
          snapshot = state.snapshot,
          activeHour = activeHour,
          skin = skin,
          onScrubTick = viewModel::onScrubTick
        )

        2 -> MoonDetailPanel(
          snapshot = state.snapshot,
          skin = skin
        )

        3 -> AirQualityPanel(
          snapshot = state.snapshot,
          activeHour = activeHour,
          skin = skin
        )

        4 -> CloudburstMinigamePanel(
          skin = skin,
          onCloudPop = viewModel::onCloudburstPop
        )
      }
    }

    // Minimal Floating Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(skin.pillBackground.copy(alpha = 0.85f))
          .clickable { showLocationSheet = true }
          .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = state.snapshot.location.name.uppercase(),
          color = skin.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.Rounded.KeyboardArrowDown,
          contentDescription = "Select City",
          tint = skin.onSurface,
          modifier = Modifier.size(16.dp)
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        state.availableUpdate?.let { rel ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(Color(0xFF00C853))
              .clickable { viewModel.installUpdate() }
              .padding(horizontal = 10.dp, vertical = 7.dp)
          ) {
            Text(
              text = if (state.isUpdating) "UPDATING..." else "UPDATE ${rel.tagName}",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        // °F / °C toggle
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(skin.pillBackground.copy(alpha = 0.85f))
            .clickable { viewModel.toggleUnit() }
            .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
          Text(
            text = if (state.useCelsius) "°C" else "°F",
            color = skin.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
          )
        }

        // Sound toggle
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(skin.pillBackground.copy(alpha = 0.85f))
            .clickable { viewModel.toggleSound() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (state.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
            contentDescription = "Sound",
            tint = skin.onSurface,
            modifier = Modifier.size(16.dp)
          )
        }

        // Skins & Pro button
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(skin.accent)
            .clickable { showSkinsSheet = true }
            .padding(horizontal = 12.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Rounded.Palette,
            contentDescription = "Skins",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (state.isPro) "SKINS" else "PRO",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // Bottom Page Navigation Dots (3D • PRECIP • MOON • AQI • GAME)
    val pageLabels = listOf("3D", "PRECIP", "MOON", "AQI", "GAME")
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      pageLabels.forEachIndexed { idx, label ->
        val active = pagerState.currentPage == idx
        Text(
          text = label,
          color = if (active) skin.accent else skin.onSurface.copy(alpha = 0.45f),
          fontSize = 10.sp,
          fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.clickable {
            scope.launch { pagerState.animateScrollToPage(idx) }
          }
        )
      }
    }
  }

  if (showSkinsSheet) {
    SkinsAndProSheet(
      activeSkinId = state.skinId,
      isPro = state.isPro,
      onSelectSkin = viewModel::selectSkin,
      onVerifyLicense = viewModel::verifyLicense,
      onDismiss = { showSkinsSheet = false }
    )
  }

  if (showLocationSheet) {
    LocationSearchSheet(
      presets = viewModel.presetLocations,
      onSearch = viewModel::searchCities,
      onSelectLocation = viewModel::loadWeather,
      onDismiss = { showLocationSheet = false }
    )
  }
}
