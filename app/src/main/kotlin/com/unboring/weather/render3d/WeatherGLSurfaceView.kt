package com.unboring.weather.render3d

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WeatherGLSurfaceView(
  sceneState: WeatherSceneState,
  modifier: Modifier = Modifier
) {
  val renderer = remember { WeatherGLRenderer() }
  renderer.state = sceneState

  AndroidView(
    modifier = modifier,
    factory = { context ->
      GLSurfaceView(context).apply {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
      }
    },
    update = {
      renderer.state = sceneState
    }
  )
}
