package com.unboring.weather.render3d

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

data class RawMesh(val vertices: FloatArray, val indices: ShortArray)

/**
 * Procedurally builds tall, ultra-condensed 3D chiseled/beveled digit meshes (0-9)
 * inspired by monolithic architectural display typefaces.
 */
object Digit3DMeshBuilder {

  // Segment layout on a tall condensed grid: width = 0.68, height = 1.68
  // Segments:
  // 0: top bar, 1: top-right, 2: bottom-right, 3: bottom bar,
  // 4: bottom-left, 5: top-left, 6: middle bar
  private val DIGIT_SEGMENTS = mapOf(
    '0' to listOf(0, 1, 2, 3, 4, 5),
    '1' to listOf(1, 2),
    '2' to listOf(0, 1, 6, 4, 3),
    '3' to listOf(0, 1, 6, 2, 3),
    '4' to listOf(5, 6, 1, 2),
    '5' to listOf(0, 5, 6, 2, 3),
    '6' to listOf(0, 5, 6, 4, 2, 3),
    '7' to listOf(0, 1, 2),
    '8' to listOf(0, 1, 2, 3, 4, 5, 6),
    '9' to listOf(0, 5, 1, 6, 2, 3),
    '-' to listOf(6)
  )

  private data class BoxSpec(val cx: Float, val cy: Float, val w: Float, val h: Float)

  private val W = 0.66f
  private val H = 1.66f
  private val T = 0.175f // stroke thickness
  private val halfW = W / 2f
  private val halfH = H / 2f
  private val halfT = T / 2f

  private val SEGMENT_BOXES = listOf(
    // 0: top horizontal
    BoxSpec(0f, halfH - halfT, W, T),
    // 1: top-right vertical
    BoxSpec(halfW - halfT, halfH / 2f, T, halfH + halfT * 0.6f),
    // 2: bottom-right vertical
    BoxSpec(halfW - halfT, -halfH / 2f, T, halfH + halfT * 0.6f),
    // 3: bottom horizontal
    BoxSpec(0f, -halfH + halfT, W, T),
    // 4: bottom-left vertical
    BoxSpec(-halfW + halfT, -halfH / 2f, T, halfH + halfT * 0.6f),
    // 5: top-left vertical
    BoxSpec(-halfW + halfT, halfH / 2f, T, halfH + halfT * 0.6f),
    // 6: middle horizontal
    BoxSpec(0f, 0f, W, T)
  )

  fun buildDigit(ch: Char): RawMesh {
    val segs = DIGIT_SEGMENTS[ch] ?: DIGIT_SEGMENTS['0']!!
    val verts = mutableListOf<Float>()
    val indices = mutableListOf<Short>()

    for (segIdx in segs) {
      val b = SEGMENT_BOXES[segIdx]
      appendBeveledBox(
        cx = b.cx,
        cy = b.cy,
        cz = 0f,
        w = b.w,
        h = b.h,
        d = 0.28f,
        bevel = 0.042f,
        verts = verts,
        indices = indices
      )
    }

    return RawMesh(verts.toFloatArray(), indices.toShortArray())
  }

  private fun appendBeveledBox(
    cx: Float,
    cy: Float,
    cz: Float,
    w: Float,
    h: Float,
    d: Float,
    bevel: Float,
    verts: MutableList<Float>,
    indices: MutableList<Short>
  ) {
    val hw = w / 2f
    val hh = h / 2f
    val hd = d / 2f
    val bw = (hw - bevel).coerceAtLeast(0.01f)
    val bh = (hh - bevel).coerceAtLeast(0.01f)

    // Front face (z = cz + hd, inset by bevel)
    val fz = cz + hd
    val mz = cz + hd - bevel
    val bz = cz - hd

    // 4 corners of front inset face
    val f0 = floatArrayOf(cx - bw, cy - bh, fz)
    val f1 = floatArrayOf(cx + bw, cy - bh, fz)
    val f2 = floatArrayOf(cx + bw, cy + bh, fz)
    val f3 = floatArrayOf(cx - bw, cy + bh, fz)

    // 4 corners of outer rim at mz
    val m0 = floatArrayOf(cx - hw, cy - hh, mz)
    val m1 = floatArrayOf(cx + hw, cy - hh, mz)
    val m2 = floatArrayOf(cx + hw, cy + hh, mz)
    val m3 = floatArrayOf(cx - hw, cy + hh, mz)

    // 4 corners of back face at bz
    val b0 = floatArrayOf(cx - hw, cy - hh, bz)
    val b1 = floatArrayOf(cx + hw, cy - hh, bz)
    val b2 = floatArrayOf(cx + hw, cy + hh, bz)
    val b3 = floatArrayOf(cx - hw, cy + hh, bz)

    // Front flat face
    addQuad(f0, f1, f2, f3, verts, indices)
    // 4 45-degree chiseled bevels
    addQuad(m0, m1, f1, f0, verts, indices)
    addQuad(m1, m2, f2, f1, verts, indices)
    addQuad(m2, m3, f3, f2, verts, indices)
    addQuad(m3, m0, f0, f3, verts, indices)
    // 4 extruded sides
    addQuad(b0, b1, m1, m0, verts, indices)
    addQuad(b1, b2, m2, m1, verts, indices)
    addQuad(b2, b3, m3, m2, verts, indices)
    addQuad(b3, b0, m0, m3, verts, indices)
    // Back face
    addQuad(b1, b0, b3, b2, verts, indices)
  }

  private fun addQuad(
    p0: FloatArray,
    p1: FloatArray,
    p2: FloatArray,
    p3: FloatArray,
    verts: MutableList<Float>,
    indices: MutableList<Short>
  ) {
    val ux = p1[0] - p0[0]
    val uy = p1[1] - p0[1]
    val uz = p1[2] - p0[2]
    val vx = p3[0] - p0[0]
    val vy = p3[1] - p0[1]
    val vz = p3[2] - p0[2]
    var nx = uy * vz - uz * vy
    var ny = uz * vx - ux * vz
    var nz = ux * vy - uy * vx
    val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
    nx /= len
    ny /= len
    nz /= len

    val base = (verts.size / 6).toShort()
    listOf(p0, p1, p2, p3).forEach { p ->
      verts.add(p[0]); verts.add(p[1]); verts.add(p[2])
      verts.add(nx); verts.add(ny); verts.add(nz)
    }
    indices.add(base)
    indices.add((base + 1).toShort())
    indices.add((base + 2).toShort())
    indices.add(base)
    indices.add((base + 2).toShort())
    indices.add((base + 3).toShort())
  }
}
