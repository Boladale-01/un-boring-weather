package com.unboring.weather.render3d

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.ui.graphics.Color
import com.unboring.weather.model.ShaderMode
import com.unboring.weather.model.SkinCatalog
import com.unboring.weather.model.SkinId
import com.unboring.weather.model.WeatherCondition
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class WeatherSceneState(
  val skinId: SkinId = SkinId.GRAPHITE,
  val condition: WeatherCondition = WeatherCondition.PARTLY_CLOUDY,
  val temperatureDisplay: Int = 65,
  val cloudCover: Int = 65,
  val windSpeedMph: Double = 12.0,
  val windDirectionDeg: Int = 215,
  val precipProbability: Int = 35,
  val isDay: Boolean = true,
  val moonPhaseFraction: Double = 0.72,
  val cloudSeed: Int = 1,
  val tiltX: Float = 0f,
  val tiltY: Float = 0f,
  val sunOffsetX: Float = 0f,
  val sunOffsetY: Float = 0f,
  val cloudOffsetX: Float = 0f,
  val cloudOffsetY: Float = 0f,
  val tempOffsetX: Float = 0f,
  val tempOffsetY: Float = 0f
)

class WeatherGLRenderer : GLSurfaceView.Renderer {

  @Volatile
  var state: WeatherSceneState = WeatherSceneState()
    set(value) {
      val seedChanged = field.cloudSeed != value.cloudSeed
      val coverChanged = kotlin.math.abs(field.cloudCover - value.cloudCover) >= 10
      field = value
      if (seedChanged || coverChanged) {
        cloudsDirty = true
        if (seedChanged) cloudPopSpring = 0.52f
      }
    }

  private var programId = 0
  private var bgProgramId = 0
  private var uMvpLoc = 0
  private var uModelLoc = 0
  private var uColorLoc = 0
  private var uEmissiveLoc = 0
  private var uLightDirLoc = 0
  private var uShaderModeLoc = 0
  private var uMoonModeLoc = 0
  private var uMoonPhaseLoc = 0

  private var uBgColorLoc = 0
  private var uBgTimeLoc = 0

  private lateinit var sphereMesh: MeshBuffers
  private lateinit var dropMesh: MeshBuffers
  private lateinit var boltMesh: MeshBuffers
  private lateinit var streakMesh: MeshBuffers
  private var bgQuadVao: Int = 0
  private val digitMeshes = mutableMapOf<Char, MeshBuffers>()

  private val projMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)

  private data class CloudPuff(
    val x: Float,
    val y: Float,
    val z: Float,
    val rx: Float,
    val ry: Float,
    val rz: Float,
    val driftPhase: Float
  )

  private data class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var speed: Float,
    var scale: Float
  )

  private var cloudPuffs = emptyList<CloudPuff>()
  @Volatile private var cloudsDirty = true
  @Volatile private var cloudPopSpring = 1f
  private var lastSeed = -1

  private val rainParticles = List(90) {
    Particle(
      x = Random.nextFloat() * 5.6f - 2.8f,
      y = Random.nextFloat() * 6.5f - 2.8f,
      z = Random.nextFloat() * 2.2f - 0.8f,
      speed = 3.8f + Random.nextFloat() * 2.4f,
      scale = 0.75f + Random.nextFloat() * 0.5f
    )
  }

  private val windStreaks = List(5) { idx ->
    Particle(
      x = -2.2f + idx * 1.1f,
      y = 0.85f + (idx % 3) * 0.42f,
      z = 0.25f,
      speed = 1.6f + idx * 0.2f,
      scale = 0.55f + (idx % 2) * 0.35f
    )
  }

  private var startTimeNs = System.nanoTime()
  private var lastFrameNs = System.nanoTime()
  private var lightningFlash = 0f
  private var nextFlashTime = 2.0f

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    GLES30.glEnable(GLES30.GL_CULL_FACE)
    GLES30.glEnable(GLES30.GL_BLEND)
    GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

    programId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    uMvpLoc = GLES30.glGetUniformLocation(programId, "uMvp")
    uModelLoc = GLES30.glGetUniformLocation(programId, "uModel")
    uColorLoc = GLES30.glGetUniformLocation(programId, "uColor")
    uEmissiveLoc = GLES30.glGetUniformLocation(programId, "uEmissive")
    uLightDirLoc = GLES30.glGetUniformLocation(programId, "uLightDir")
    uShaderModeLoc = GLES30.glGetUniformLocation(programId, "uShaderMode")
    uMoonModeLoc = GLES30.glGetUniformLocation(programId, "uMoonMode")
    uMoonPhaseLoc = GLES30.glGetUniformLocation(programId, "uMoonPhase")

    bgProgramId = createProgram(BG_VERTEX_SHADER, BG_FRAGMENT_SHADER)
    uBgColorLoc = GLES30.glGetUniformLocation(bgProgramId, "uBgColor")
    uBgTimeLoc = GLES30.glGetUniformLocation(bgProgramId, "uTime")
    bgQuadVao = buildBgQuad()

    sphereMesh = buildSphereMesh(stacks = 22, slices = 30)
    dropMesh = buildCylinderMesh(slices = 8)
    boltMesh = buildLightningBoltMesh()
    streakMesh = buildStreakMesh()

    "0123456789-".forEach { ch ->
      val raw = Digit3DMeshBuilder.buildDigit(ch)
      digitMeshes[ch] = uploadMesh(raw.vertices, raw.indices)
    }
    cloudsDirty = true
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES30.glViewport(0, 0, width, height)
    val aspect = width.toFloat() / maxOf(1, height).toFloat()
    Matrix.perspectiveM(projMatrix, 0, 42f, aspect, 0.5f, 40f)
  }

  override fun onDrawFrame(gl: GL10?) {
    val now = System.nanoTime()
    val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
    val elapsed = (now - startTimeNs) / 1_000_000_000f
    lastFrameNs = now

    val s = state
    val skin = SkinCatalog.byId(s.skinId)

    if (cloudsDirty || lastSeed != s.cloudSeed) {
      rebuildClouds(s.cloudCover, s.cloudSeed)
      cloudsDirty = false
      lastSeed = s.cloudSeed
    }

    cloudPopSpring += (1f - cloudPopSpring) * minOf(1f, dt * 10f)

    if (s.condition == WeatherCondition.THUNDERSTORM) {
      if (elapsed > nextFlashTime) {
        lightningFlash = 1.0f
        nextFlashTime = elapsed + 2.4f + Random.nextFloat() * 3.0f
      }
    }
    lightningFlash = (lightningFlash - dt * 3.8f).coerceAtLeast(0f)

    val bg = if (s.isDay) skin.background else skin.backgroundNight
    val flashBoost = lightningFlash * 0.45f
    val bgR = (bg.red + flashBoost).coerceAtMost(1f)
    val bgG = (bg.green + flashBoost).coerceAtMost(1f)
    val bgB = (bg.blue + flashBoost * 1.1f).coerceAtMost(1f)

    GLES30.glClearColor(bgR, bgG, bgB, 1f)
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

    // 0. Analog Film Grain + Studio Vignette Background Pass
    GLES30.glDisable(GLES30.GL_DEPTH_TEST)
    GLES30.glUseProgram(bgProgramId)
    GLES30.glUniform3f(uBgColorLoc, bgR, bgG, bgB)
    GLES30.glUniform1f(uBgTimeLoc, elapsed)
    GLES30.glBindVertexArray(bgQuadVao)
    GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    GLES30.glBindVertexArray(0)
    GLES30.glEnable(GLES30.GL_DEPTH_TEST)

    GLES30.glUseProgram(programId)

    val camX = s.tiltX * 0.75f
    val camY = 0.25f + s.tiltY * 0.55f
    Matrix.setLookAtM(viewMatrix, 0, camX, camY, 7.6f, 0f, 0.25f, 0f, 0f, 1f, 0f)

    val shaderIdx = when (skin.shaderMode) {
      ShaderMode.STANDARD -> 0
      ShaderMode.METALLIC -> 1
      ShaderMode.NORMAL_DEBUG -> 2
      ShaderMode.WIREFRAME -> 3
    }
    GLES30.glUniform1i(uShaderModeLoc, shaderIdx)
    GLES30.glUniform3f(uLightDirLoc, 0.52f, 0.78f, 0.72f)
    val wireframe = skin.shaderMode == ShaderMode.WIREFRAME

    // 1. Giant 3D Chiseled Temperature Numerals (inside 3D space!)
    val tempStr = s.temperatureDisplay.toString()
    val spacing = 0.76f
    val startX = -((tempStr.length - 1) * spacing) / 2f
    tempStr.forEachIndexed { idx, ch ->
      val mesh = digitMeshes[ch] ?: return@forEachIndexed
      drawMesh(
        mesh = mesh,
        tx = startX + idx * spacing + s.tempOffsetX,
        ty = -0.35f + s.tempOffsetY,
        tz = 0.0f,
        sx = 1.0f,
        sy = 1.05f,
        sz = 1.0f,
        rotY = s.tiltX * 0.16f,
        rotZ = 0f,
        color = skin.numeralPrimary,
        emissive = 0.05f,
        moonMode = false,
        moonPhase = 0.5f,
        wireframe = wireframe
      )
    }

    // 2. 3D Sun or Moon
    if (s.isDay) {
      val sunY = 1.38f + sin(elapsed * 0.7f) * 0.03f + s.sunOffsetY
      drawMesh(
        mesh = sphereMesh,
        tx = s.sunOffsetX,
        ty = sunY,
        tz = -0.45f,
        sx = 0.86f,
        sy = 0.86f,
        sz = 0.86f,
        rotY = 0f,
        rotZ = 0f,
        color = skin.sunColor,
        emissive = 0.48f,
        moonMode = false,
        moonPhase = 0.5f,
        wireframe = wireframe
      )
    } else {
      val moonY = 1.42f + sin(elapsed * 0.5f) * 0.03f + s.sunOffsetY
      drawMesh(
        mesh = sphereMesh,
        tx = s.sunOffsetX,
        ty = moonY,
        tz = -0.45f,
        sx = 0.78f,
        sy = 0.78f,
        sz = 0.78f,
        rotY = 0f,
        rotZ = 0f,
        color = Color(0xFFE8ECF4),
        emissive = 0.14f,
        moonMode = true,
        moonPhase = s.moonPhaseFraction.toFloat(),
        wireframe = wireframe
      )
    }

    // 3. Sculptural Blender-style 3D Cloud Clusters
    val windFactor = (s.windSpeedMph / 15.0).toFloat().coerceIn(0.25f, 2.2f)
    val cloudDarken = if (s.condition == WeatherCondition.THUNDERSTORM) 0.55f else 1f
    val cColor = Color(
      red = (skin.cloudColor.red * cloudDarken).coerceIn(0f, 1f),
      green = (skin.cloudColor.green * cloudDarken).coerceIn(0f, 1f),
      blue = (skin.cloudColor.blue * cloudDarken).coerceIn(0f, 1f),
      alpha = 1f
    )
    for (puff in cloudPuffs) {
      val sway = sin(elapsed * 0.85f * windFactor + puff.driftPhase) * 0.05f
      val bob = cos(elapsed * 1.0f + puff.driftPhase) * 0.025f
      drawMesh(
        mesh = sphereMesh,
        tx = puff.x + sway + s.cloudOffsetX,
        ty = puff.y + bob + s.cloudOffsetY,
        tz = puff.z,
        sx = puff.rx * cloudPopSpring,
        sy = puff.ry * cloudPopSpring,
        sz = puff.rz * cloudPopSpring,
        rotY = 0f,
        rotZ = 0f,
        color = cColor,
        emissive = lightningFlash * 0.35f,
        moonMode = false,
        moonPhase = 0f,
        wireframe = wireframe
      )
    }

    // 4. Wind Streaks when breezy
    if (s.windSpeedMph >= 8.0) {
      for (streak in windStreaks) {
        streak.x += streak.speed * windFactor * dt
        if (streak.x > 2.4f) streak.x = -2.4f
        drawMesh(
          mesh = streakMesh,
          tx = streak.x + s.sunOffsetX * 0.5f,
          ty = streak.y + s.sunOffsetY * 0.5f,
          tz = streak.z,
          sx = streak.scale,
          sy = 1f,
          sz = 1f,
          rotY = 0f,
          rotZ = 0f,
          color = skin.onSurface.copy(alpha = 0.65f),
          emissive = 0.25f,
          moonMode = false,
          moonPhase = 0f,
          wireframe = wireframe
        )
      }
    }

    // 5. Lightning Bolt
    if (s.condition == WeatherCondition.THUNDERSTORM && lightningFlash > 0.15f) {
      drawMesh(
        mesh = boltMesh,
        tx = s.cloudOffsetX,
        ty = 0.55f + s.cloudOffsetY,
        tz = 0.35f,
        sx = 0.68f,
        sy = 0.82f,
        sz = 0.68f,
        rotY = 0f,
        rotZ = 0f,
        color = Color(0xFFFFF59D),
        emissive = 1.0f,
        moonMode = false,
        moonPhase = 0f,
        wireframe = wireframe
      )
    }

    // 6. Physics Rain / Snow Particles
    val isSnow = s.condition == WeatherCondition.SNOW
    val isRain = s.condition in setOf(
      WeatherCondition.DRIZZLE,
      WeatherCondition.RAIN,
      WeatherCondition.HEAVY_RAIN,
      WeatherCondition.THUNDERSTORM
    ) || s.precipProbability >= 45

    if (isRain || isSnow) {
      val activeCount = when (s.condition) {
        WeatherCondition.HEAVY_RAIN, WeatherCondition.THUNDERSTORM -> rainParticles.size
        WeatherCondition.RAIN, WeatherCondition.SNOW -> 60
        else -> 34
      }
      val windTiltRad = ((s.windSpeedMph / 35.0) * 0.35).toFloat().coerceIn(0.06f, 0.42f)
      val windDriftX = sin(windTiltRad) * 2.6f
      val pColor = if (isSnow) Color.White else skin.rainColor

      for (i in 0 until activeCount) {
        val p = rainParticles[i]
        val fallSpeed = if (isSnow) p.speed * 0.35f else p.speed * 1.45f
        p.y -= fallSpeed * dt
        p.x += windDriftX * dt * (if (isSnow) 0.5f else 1.0f)
        if (isSnow) p.x += sin(elapsed * 2.5f + i) * 0.3f * dt
        if (p.y < -2.8f) {
          p.y = 2.4f + Random.nextFloat() * 0.8f
          p.x = Random.nextFloat() * 5.6f - 2.8f
        }
        if (p.x > 3.2f) p.x -= 6.4f

        if (isSnow) {
          drawMesh(
            mesh = sphereMesh,
            tx = p.x,
            ty = p.y,
            tz = p.z,
            sx = 0.042f * p.scale,
            sy = 0.042f * p.scale,
            sz = 0.042f * p.scale,
            rotY = 0f,
            rotZ = 0f,
            color = pColor,
            emissive = 0.25f,
            moonMode = false,
            moonPhase = 0f,
            wireframe = wireframe
          )
        } else {
          drawMesh(
            mesh = dropMesh,
            tx = p.x,
            ty = p.y,
            tz = p.z,
            sx = 0.02f * p.scale,
            sy = 0.24f * p.scale,
            sz = 0.02f * p.scale,
            rotY = 0f,
            rotZ = -windTiltRad,
            color = pColor,
            emissive = 0.35f,
            moonMode = false,
            moonPhase = 0f,
            wireframe = wireframe
          )
        }
      }
    }
  }

  private fun rebuildClouds(cloudCover: Int, seed: Int) {
    if (cloudCover < 8) {
      cloudPuffs = emptyList()
      return
    }
    val rng = Random(seed * 7919 + cloudCover * 31)
    val list = mutableListOf<CloudPuff>()

    // Main sculptural foreground cloud cluster (tightly nestled lobes like Blender clay clouds)
    val mainX = (rng.nextFloat() - 0.5f) * 0.45f
    val mainY = 1.02f + (rng.nextFloat() - 0.5f) * 0.14f
    list.add(CloudPuff(mainX, mainY + 0.08f, 0.35f, 0.46f, 0.42f, 0.40f, 0.2f))
    list.add(CloudPuff(mainX - 0.36f, mainY - 0.04f, 0.30f, 0.33f, 0.28f, 0.30f, 1.1f))
    list.add(CloudPuff(mainX + 0.38f, mainY - 0.02f, 0.32f, 0.36f, 0.30f, 0.32f, 2.0f))
    list.add(CloudPuff(mainX + 0.14f, mainY + 0.18f, 0.28f, 0.30f, 0.28f, 0.28f, 2.8f))

    // Secondary cloud cluster if overcast
    if (cloudCover >= 45) {
      val sx = if (mainX > 0f) -0.78f else 0.78f
      val sy = 1.24f + (rng.nextFloat() - 0.5f) * 0.18f
      list.add(CloudPuff(sx, sy, -0.15f, 0.38f, 0.34f, 0.34f, 3.6f))
      list.add(CloudPuff(sx - 0.28f, sy - 0.06f, -0.18f, 0.26f, 0.23f, 0.25f, 4.2f))
      list.add(CloudPuff(sx + 0.28f, sy - 0.04f, -0.18f, 0.27f, 0.24f, 0.25f, 5.0f))
    }

    // Third small accent puff for heavy cloud cover
    if (cloudCover >= 75) {
      list.add(CloudPuff(-0.95f, 0.88f, 0.42f, 0.24f, 0.20f, 0.22f, 5.7f))
      list.add(CloudPuff(0.95f, 0.92f, 0.40f, 0.25f, 0.21f, 0.22f, 0.9f))
    }

    cloudPuffs = list
  }

  private fun drawMesh(
    mesh: MeshBuffers,
    tx: Float,
    ty: Float,
    tz: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    rotY: Float,
    rotZ: Float,
    color: Color,
    emissive: Float,
    moonMode: Boolean,
    moonPhase: Float,
    wireframe: Boolean
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, tx, ty, tz)
    if (rotY != 0f) {
      Matrix.rotateM(modelMatrix, 0, Math.toDegrees(rotY.toDouble()).toFloat(), 0f, 1f, 0f)
    }
    if (rotZ != 0f) {
      Matrix.rotateM(modelMatrix, 0, Math.toDegrees(rotZ.toDouble()).toFloat(), 0f, 0f, 1f)
    }
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

    val viewModel = FloatArray(16)
    Matrix.multiplyMM(viewModel, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewModel, 0)

    GLES30.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
    GLES30.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)
    GLES30.glUniform4f(uColorLoc, color.red, color.green, color.blue, color.alpha)
    GLES30.glUniform1f(uEmissiveLoc, emissive)
    GLES30.glUniform1i(uMoonModeLoc, if (moonMode) 1 else 0)
    GLES30.glUniform1f(uMoonPhaseLoc, moonPhase)

    GLES30.glBindVertexArray(mesh.vao)
    val mode = if (wireframe) GLES30.GL_LINES else GLES30.GL_TRIANGLES
    GLES30.glDrawElements(mode, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, 0)
    GLES30.glBindVertexArray(0)
  }

  private data class MeshBuffers(val vao: Int, val indexCount: Int)

  private fun buildBgQuad(): Int {
    val verts = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
    val vao = IntArray(1)
    val vbo = IntArray(1)
    GLES30.glGenVertexArrays(1, vao, 0)
    GLES30.glGenBuffers(1, vbo, 0)
    GLES30.glBindVertexArray(vao[0])
    val buf = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
      put(verts); position(0)
    }
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, buf, GLES30.GL_STATIC_DRAW)
    GLES30.glEnableVertexAttribArray(0)
    GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
    GLES30.glBindVertexArray(0)
    return vao[0]
  }

  private fun buildSphereMesh(stacks: Int, slices: Int): MeshBuffers {
    val verts = mutableListOf<Float>()
    val indices = mutableListOf<Short>()
    for (i in 0..stacks) {
      val v = i.toFloat() / stacks
      val phi = v * PI
      for (j in 0..slices) {
        val u = j.toFloat() / slices
        val theta = u * 2.0 * PI
        val x = (cos(theta) * sin(phi)).toFloat()
        val y = cos(phi).toFloat()
        val z = (sin(theta) * sin(phi)).toFloat()
        verts.addAll(listOf(x, y, z, x, y, z))
      }
    }
    val stride = slices + 1
    for (i in 0 until stacks) {
      for (j in 0 until slices) {
        val a = (i * stride + j).toShort()
        val b = ((i + 1) * stride + j).toShort()
        val c = ((i + 1) * stride + j + 1).toShort()
        val d = (i * stride + j + 1).toShort()
        indices.addAll(listOf(a, b, d, b, c, d))
      }
    }
    return uploadMesh(verts.toFloatArray(), indices.toShortArray())
  }

  private fun buildCylinderMesh(slices: Int): MeshBuffers {
    val verts = mutableListOf<Float>()
    val indices = mutableListOf<Short>()
    for (i in 0..1) {
      val y = if (i == 0) -0.5f else 0.5f
      for (j in 0..slices) {
        val theta = (j.toFloat() / slices) * 2.0 * PI
        val x = cos(theta).toFloat()
        val z = sin(theta).toFloat()
        verts.addAll(listOf(x, y, z, x, 0f, z))
      }
    }
    val stride = slices + 1
    for (j in 0 until slices) {
      val a = j.toShort()
      val b = (stride + j).toShort()
      val c = (stride + j + 1).toShort()
      val d = (j + 1).toShort()
      indices.addAll(listOf(a, b, d, b, c, d))
    }
    return uploadMesh(verts.toFloatArray(), indices.toShortArray())
  }

  private fun buildStreakMesh(): MeshBuffers {
    val w = 0.35f
    val h = 0.012f
    val verts = floatArrayOf(
      -w, -h, 0f, 0f, 0f, 1f,
      w, -h, 0f, 0f, 0f, 1f,
      w, h, 0f, 0f, 0f, 1f,
      -w, h, 0f, 0f, 0f, 1f
    )
    val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
    return uploadMesh(verts, indices)
  }

  private fun buildLightningBoltMesh(): MeshBuffers {
    val pts = listOf(
      0.0f to 0.8f,
      -0.25f to 0.25f,
      0.05f to 0.22f,
      -0.22f to -0.35f,
      0.04f to -0.38f,
      -0.15f to -0.95f
    )
    val verts = mutableListOf<Float>()
    val indices = mutableListOf<Short>()
    val w = 0.08f
    for (i in 0 until pts.size - 1) {
      val (x0, y0) = pts[i]
      val (x1, y1) = pts[i + 1]
      val base = (i * 4).toShort()
      verts.addAll(
        listOf(
          x0 - w, y0, 0f, 0f, 0f, 1f,
          x0 + w, y0, 0f, 0f, 0f, 1f,
          x1 - w * 0.7f, y1, 0f, 0f, 0f, 1f,
          x1 + w * 0.7f, y1, 0f, 0f, 0f, 1f
        )
      )
      indices.addAll(listOf(base, (base + 2).toShort(), (base + 1).toShort(), (base + 1).toShort(), (base + 2).toShort(), (base + 3).toShort()))
    }
    return uploadMesh(verts.toFloatArray(), indices.toShortArray())
  }

  private fun uploadMesh(vertices: FloatArray, indices: ShortArray): MeshBuffers {
    val vaoArr = IntArray(1)
    val vboArr = IntArray(1)
    val eboArr = IntArray(1)
    GLES30.glGenVertexArrays(1, vaoArr, 0)
    GLES30.glGenBuffers(1, vboArr, 0)
    GLES30.glGenBuffers(1, eboArr, 0)

    GLES30.glBindVertexArray(vaoArr[0])

    val vBuf: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
      .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(vertices)
        position(0)
      }
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboArr[0])
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, vBuf, GLES30.GL_STATIC_DRAW)

    val iBuf: ShortBuffer = ByteBuffer.allocateDirect(indices.size * 2)
      .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
        put(indices)
        position(0)
      }
    GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboArr[0])
    GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, iBuf, GLES30.GL_STATIC_DRAW)

    val strideBytes = 6 * 4
    GLES30.glEnableVertexAttribArray(0)
    GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, strideBytes, 0)
    GLES30.glEnableVertexAttribArray(1)
    GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, 3 * 4)

    GLES30.glBindVertexArray(0)
    return MeshBuffers(vaoArr[0], indices.size)
  }

  private fun createProgram(vsSource: String, fsSource: String): Int {
    val vs = compileShader(GLES30.GL_VERTEX_SHADER, vsSource)
    val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fsSource)
    val prog = GLES30.glCreateProgram()
    GLES30.glAttachShader(prog, vs)
    GLES30.glAttachShader(prog, fs)
    GLES30.glLinkProgram(prog)
    return prog
  }

  private fun compileShader(type: Int, src: String): Int {
    val shader = GLES30.glCreateShader(type)
    GLES30.glShaderSource(shader, src)
    GLES30.glCompileShader(shader)
    return shader
  }

  companion object {
    private const val BG_VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec2 aPos;
out vec2 vUv;
void main() {
  vUv = aPos * 0.5 + 0.5;
  gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

    private const val BG_FRAGMENT_SHADER = """#version 300 es
precision mediump float;
in vec2 vUv;
uniform vec3 uBgColor;
uniform float uTime;
out vec4 fragColor;

float hash(vec2 p) {
  return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
  float dist = distance(vUv, vec2(0.5, 0.45));
  float vignette = 1.0 - smoothstep(0.25, 0.95, dist) * 0.18;
  float grain = (hash(vUv * 512.0 + fract(uTime)) - 0.5) * 0.032;
  vec3 col = clamp(uBgColor * vignette + grain, 0.0, 1.0);
  fragColor = vec4(col, 1.0);
}
"""

    private const val VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
uniform mat4 uMvp;
uniform mat4 uModel;
out vec3 vNormal;
void main() {
  vNormal = normalize(mat3(uModel) * aNormal);
  gl_Position = uMvp * vec4(aPos, 1.0);
}
"""

    private const val FRAGMENT_SHADER = """#version 300 es
precision mediump float;
in vec3 vNormal;
uniform vec4 uColor;
uniform float uEmissive;
uniform vec3 uLightDir;
uniform int uShaderMode;
uniform int uMoonMode;
uniform float uMoonPhase;
out vec4 fragColor;

void main() {
  vec3 n = normalize(vNormal);
  if (uShaderMode == 2) {
    fragColor = vec4(n * 0.5 + 0.5, uColor.a);
    return;
  }
  if (uShaderMode == 3) {
    fragColor = vec4(uColor.rgb, 1.0);
    return;
  }

  vec3 lDir = normalize(uLightDir);
  if (uMoonMode == 1) {
    float angle = uMoonPhase * 6.2831853;
    lDir = normalize(vec3(sin(angle), 0.15, -cos(angle)));
  }

  float diff = max(dot(n, lDir), 0.0);
  float rim = pow(1.0 - max(dot(n, vec3(0.0, 0.0, 1.0)), 0.0), 2.4);

  vec3 base = uColor.rgb;
  if (uShaderMode == 1) {
    vec3 halfVec = normalize(lDir + vec3(0.0, 0.0, 1.0));
    float spec = pow(max(dot(n, halfVec), 0.0), 36.0);
    vec3 lit = base * (0.28 + 0.72 * diff) + vec3(1.0, 0.92, 0.72) * spec * 0.85 + base * rim * 0.35;
    fragColor = vec4(lit, uColor.a);
    return;
  }

  float ambient = (uMoonMode == 1) ? 0.12 : 0.44;
  vec3 lit = base * (ambient + (1.0 - ambient) * diff) + base * uEmissive + vec3(rim * 0.12);
  fragColor = vec4(lit, uColor.a);
}
"""
  }
}
