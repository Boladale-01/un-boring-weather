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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import com.unboring.weather.ui.components.LocationSearchSheet
import com.unboring.weather.ui.components.MoonDetailPanel
import com.unboring.weather.ui.components.PlayTheWeatherBar
import com.unboring.weather.ui.components.PrecipDetailPanel
import com.unboring.weather.ui.components.SkinsAndProSheet
import com.unboring.weather.ui.components.formatTemp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
  var sunOffsetX by remember { mutableFloatStateOf(0f) }
  var sunOffsetY by remember { mutableFloatStateOf(0f) }
  var cloudOffsetX by remember { mutableFloatStateOf(0f) }
  var cloudOffsetY by remember { mutableFloatStateOf(0f) }
  var tempOffsetX by remember { mutableFloatStateOf(0f) }
  var tempOffsetY by remember { mutableFloatStateOf(0f) }

  var showSkinsSheet by remember { mutableStateOf(false) }
  var showLocationSheet by remember { mutableStateOf(false) }
  var showMenuSheet by remember { mutableStateOf(false) }

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
          // Pure 3D Stage — zero clutter
          Box(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(Unit) {
                detectTapGestures(
                  onTap = { viewModel.onSceneTap() },
                  onDoubleTap = {
                    sunOffsetX = 0f; sunOffsetY = 0f
                    cloudOffsetX = 0f; cloudOffsetY = 0f
                    tempOffsetX = 0f; tempOffsetY = 0f
                    tiltX = 0f; tiltY = 0f
                    viewModel.onScrubTick()
                  }
                )
              }
              .pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = { tiltX = 0f; tiltY = 0f },
                  onDragCancel = { tiltX = 0f; tiltY = 0f }
                ) { change, dragAmount ->
                  change.consume()
                  val yFrac = change.position.y / size.height.toFloat()
                  val dx = dragAmount.x / 180f
                  val dy = -dragAmount.y / 180f

                  if (yFrac < 0.36f) {
                    // Upper stage: drag 3D clouds & sun/moon freely
                    cloudOffsetX = (cloudOffsetX + dx).coerceIn(-1.6f, 1.6f)
                    cloudOffsetY = (cloudOffsetY + dy).coerceIn(-1.2f, 1.2f)
                    sunOffsetX = (sunOffsetX + dx * 0.65f).coerceIn(-1.4f, 1.4f)
                    sunOffsetY = (sunOffsetY + dy * 0.65f).coerceIn(-1.0f, 1.0f)
                  } else if (yFrac < 0.70f) {
                    // Middle stage: drag 3D temperature numerals
                    tempOffsetX = (tempOffsetX + dx).coerceIn(-1.4f, 1.4f)
                    tempOffsetY = (tempOffsetY + dy).coerceIn(-1.0f, 1.0f)
                  }

                  tiltX = (tiltX + dragAmount.x / 360f).coerceIn(-1f, 1f)
                  tiltY = (tiltY + dragAmount.y / 360f).coerceIn(-1f, 1f)
                }
              }
          ) {
            WeatherGLSurfaceView(
              sceneState = WeatherSceneState(
                skinId = state.skinId,
                condition = activeHour.condition,
                temperatureDisplay = formatTemp(activeHour.tempF, state.useCelsius),
                cloudCover = activeHour.cloudCover,
                windSpeedMph = activeHour.windSpeedMph,
                windDirectionDeg = activeHour.windDirectionDeg,
                precipProbability = activeHour.precipProbability,
                isDay = activeHour.isDay,
                moonPhaseFraction = state.snapshot.moon.phaseFraction,
                cloudSeed = state.cloudSeed,
                tiltX = tiltX,
                tiltY = tiltY,
                sunOffsetX = sunOffsetX,
                sunOffsetY = sunOffsetY,
                cloudOffsetX = cloudOffsetX,
                cloudOffsetY = cloudOffsetY,
                tempOffsetX = tempOffsetX,
                tempOffsetY = tempOffsetY
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
                  .padding(top = 56.dp)
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

            // Condition subtitle below the 3D numeral
            val condSub = if (activeHour.precipProbability >= 25) {
              "${activeHour.condition.label.uppercase()}  ${activeHour.precipProbability}%"
            } else {
              activeHour.condition.label.uppercase()
            }
            Text(
              text = condSub,
              color = skin.onSurface.copy(alpha = 0.85f),
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 1.8.sp,
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 176.dp)
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
                .padding(bottom = 30.dp)
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

    // Whisper-quiet Top Bar (City label + subtle menu dot)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = state.snapshot.location.name.uppercase(),
        color = skin.onSurface.copy(alpha = 0.72f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.6.sp,
        modifier = Modifier.clickable { showLocationSheet = true }
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        state.availableUpdate?.let { rel ->
          Text(
            text = "UPDATE ${rel.tagName}",
            color = Color(0xFF00E676),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { viewModel.installUpdate() }
          )
        }

        Icon(
          imageVector = Icons.Rounded.MoreHoriz,
          contentDescription = "Menu",
          tint = skin.onSurface.copy(alpha = 0.72f),
          modifier = Modifier
            .size(24.dp)
            .clickable { showMenuSheet = true }
        )
      }
    }

    // Subtle 5-dot Page Indicator at the very bottom
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(7.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      repeat(5) { idx ->
        val active = pagerState.currentPage == idx
        Box(
          modifier = Modifier
            .size(if (active) 7.dp else 5.dp)
            .clip(CircleShape)
            .background(if (active) skin.onSurface else skin.onSurface.copy(alpha = 0.28f))
            .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
        )
      }
    }
  }

  if (showMenuSheet) {
    ModalBottomSheet(onDismissRequest = { showMenuSheet = false }) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text("UN BORING WEATHER", fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        MenuActionRow("3D Skins & Pro", skin.id.displayName) {
          showMenuSheet = false
          showSkinsSheet = true
        }
        MenuActionRow("Location", state.snapshot.location.name) {
          showMenuSheet = false
          showLocationSheet = true
        }
        MenuActionRow("Temperature Unit", if (state.useCelsius) "Celsius (°C)" else "Fahrenheit (°F)") {
          viewModel.toggleUnit()
        }
        MenuActionRow("Sound Effects", if (state.soundEnabled) "On" else "Muted") {
          viewModel.toggleSound()
        }
        MenuActionRow("Reset 3D Positions", "Center sun, clouds & temp") {
          sunOffsetX = 0f; sunOffsetY = 0f
          cloudOffsetX = 0f; cloudOffsetY = 0f
          tempOffsetX = 0f; tempOffsetY = 0f
          showMenuSheet = false
        }
        Spacer(modifier = Modifier.height(14.dp))
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

@Composable
private fun MenuActionRow(title: String, value: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(vertical = 10.dp, horizontal = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Text(value, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
  }
}
