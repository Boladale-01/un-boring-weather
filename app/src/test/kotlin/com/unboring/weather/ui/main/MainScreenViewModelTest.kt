package com.unboring.weather.ui.main

import com.unboring.weather.data.OpenMeteoRepository
import com.unboring.weather.model.SkinCatalog
import com.unboring.weather.model.SkinId
import com.unboring.weather.render3d.Digit3DMeshBuilder
import com.unboring.weather.ui.components.formatTemp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {

  @Test
  fun fallbackSnapshot_has24HoursAnd8Days() {
    val repo = OpenMeteoRepository()
    val snap = repo.createFallbackSnapshot(repo.defaultLocations.first())
    assertEquals(24, snap.hourly.size)
    assertEquals(8, snap.daily.size)
    assertEquals("Seattle", snap.location.name)
  }

  @Test
  fun moonPhase_isWithinValidBounds() {
    val repo = OpenMeteoRepository()
    val moon = repo.computeMoonInfo()
    assertTrue(moon.illuminationPercent in 0..100)
    assertTrue(moon.phaseFraction in 0.0..1.0)
  }

  @Test
  fun digit3DMeshBuilder_buildsAllDigits() {
    "0123456789-".forEach { ch ->
      val mesh = Digit3DMeshBuilder.buildDigit(ch)
      assertTrue(mesh.vertices.isNotEmpty())
      assertTrue(mesh.indices.isNotEmpty())
    }
  }

  @Test
  fun skinCatalog_and_tempConversion_work() {
    assertEquals(8, SkinCatalog.all.size)
    assertEquals(SkinId.KARAT, SkinCatalog.byId(SkinId.KARAT).id)
    assertEquals(68, formatTemp(68.0, useCelsius = false))
    assertEquals(20, formatTemp(68.0, useCelsius = true))
  }
}
