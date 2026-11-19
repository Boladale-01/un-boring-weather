package com.unboring.weather.ui.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.unboring.weather.ui.components.Instruments3DPanel
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
  var rawPullX by remember { mutableFloatStateOf(0f) }
  var rawPullY by remember { mutableFloatStateOf(0f) }
  var spinAngleY by remember { mutableFloatStateOf(0f) }
  var spinVelY by remember { mutableFloatStateOf(0f) }

  // Rubber-band spring snap-back when released
  val pullX by animateFloatAsState(
    targetValue = kotlin.math.tanh(rawPullX.toDouble() * 1.1).toFloat() * 0.28f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "pullX"
  )
  val pullY by animateFloatAsState(
    targetValue = kotlin.math.tanh(rawPullY.toDouble() * 1.1).toFloat() * 0.24f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "pullY"
  )

  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(16L)
      if (kotlin.math.abs(spinVelY) > 0.0005f) {
        spinAngleY += spinVelY
        spinVelY *= 0.94f
      } else {
        spinVelY = 0f
      }
    }
  }

  var showSkinsSheet by remember { mutableStateOf(false) }
  var showLocationSheet by remember { mutableStateOf(false) }
  var showMenuSheet by remember { mutableStateOf(false) }

  val locationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    if (grants.values.any { it }) {
      viewModel.useDeviceLocation()
    }
  }

  LaunchedEffect(Unit) {
    locationLauncher.launch(
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
    )
  }

  val pagerState = rememberPagerState(pageCount = { 6 })
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
                    spinAngleY = 0f
                    spinVelY = 0f
                    rawPullX = 0f
                    rawPullY = 0f
                    tiltX = 0f; tiltY = 0f
                    viewModel.onScrubTick()
                  }
                )
              }
              .pointerInput(Unit) {
                detectDragGestures(
                  onDragEnd = {
                    rawPullX = 0f
                    rawPullY = 0f
                    tiltX = 0f
                    tiltY = 0f
                    viewModel.onScrubTick()
                  },
                  onDragCancel = {
                    rawPullX = 0f
                    rawPullY = 0f
                    tiltX = 0f
                    tiltY = 0f
                  }
                ) { change, dragAmount ->
                  change.consume()
                  // Spin freely around Y with rotational inertia
                  val spinDelta = dragAmount.x / 110f
                  spinAngleY += spinDelta
                  spinVelY = spinDelta * 0.65f

                  // Struggle / rubber-band tether when pulled (snaps back on release)
                  rawPullX += dragAmount.x / 240f
                  rawPullY += -dragAmount.y / 240f

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
                pullX = pullX,
                pullY = pullY,
                spinAngleY = spinAngleY
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
                .padding(bottom = 224.dp)
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
                .padding(bottom = 78.dp)
            )
          }
        }

        1 -> PrecipDetailPanel(
          snapshot = state.snapshot,
          activeHour = activeHour,
          skin = skin,
          onScrubTick = viewModel::onScrubTick
        )

        2 -> Instruments3DPanel(
          snapshot = state.snapshot,
          activeHour = activeHour,
          skin = skin
        )

        3 -> MoonDetailPanel(
          snapshot = state.snapshot,
          skin = skin
        )

        4 -> AirQualityPanel(
          snapshot = state.snapshot,
          activeHour = activeHour,
          skin = skin
        )

        5 -> CloudburstMinigamePanel(
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

    // Tactile Floating Pill Dock
    val dockItems = listOf(
      Icons.Rounded.WbSunny to "NOW",
      Icons.Rounded.WaterDrop to "RAIN",
      Icons.Rounded.Speed to "DIALS",
      Icons.Rounded.NightsStay to "MOON",
      Icons.Rounded.Air to "AQI",
      Icons.Rounded.SportsEsports to "PLAY"
    )
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
        .clip(RoundedCornerShape(28.dp))
        .background(skin.pillBackground.copy(alpha = 0.94f))
        .padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      dockItems.forEachIndexed { idx, (icon, label) ->
        val active = pagerState.currentPage == idx
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) skin.accent else Color.Transparent)
            .clickable {
              viewModel.onScrubTick()
              scope.launch { pagerState.animateScrollToPage(idx) }
            }
            .padding(horizontal = if (active) 12.dp else 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) Color.White else skin.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.size(16.dp)
          )
          if (active) {
            Text(
              text = label,
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace
            )
          }
        }
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
        MenuActionRow("Weather Model", state.modelSource.displayName) {
          viewModel.cycleWeatherModel()
        }
        MenuActionRow("Temperature Unit", if (state.useCelsius) "Celsius (°C)" else "Fahrenheit (°F)") {
          viewModel.toggleUnit()
        }
        MenuActionRow("Sound Effects", if (state.soundEnabled) "On" else "Muted") {
          viewModel.toggleSound()
        }
        MenuActionRow("Reset 3D Spin", "Center 3D rotation") {
          spinAngleY = 0f; spinVelY = 0f
          rawPullX = 0f; rawPullY = 0f
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
      onUseCurrentLocation = {
        locationLauncher.launch(
          arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
          )
        )
      },
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
