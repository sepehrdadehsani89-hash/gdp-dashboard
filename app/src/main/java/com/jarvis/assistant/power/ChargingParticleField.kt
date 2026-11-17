package com.jarvis.assistant.power

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Decorative charging energy, driven only by verified power state and the visible render clock.
 * Reuses a small fixed buffer; it never represents microphone activity or charging speed. */
class ChargingParticleField {
    class Particle {
        var x = 0f; internal set
        var y = 0f; internal set
        var tailX = 0f; internal set
        var tailY = 0f; internal set
        var radius = 0f; internal set
        var alpha = 0f; internal set
    }

    val particles = Array(MAX_PARTICLES) { Particle() }
    var count = 0; private set
    var absorption = 0f; private set

    /** Phase comes from ChargingVisualClock, so time spent hidden is not replayed. */
    fun update(requestedCount: Int, phase: Float, width: Float, height: Float,
               centerX: Float, centerY: Float, coreRadius: Float): Int {
        count = 0; absorption = 0f
        if (requestedCount <= 0 || !phase.isFinite() || !width.isFinite() || !height.isFinite() ||
            !coreRadius.isFinite() || width <= 2f || height <= 2f || coreRadius <= 0f ||
            centerX !in 0f..width || centerY !in 0f..height) return 0
        count = requestedCount.coerceAtMost(MAX_PARTICLES)
        for (i in 0 until count) {
            val t = (phase + i.toFloat() / count).let { it - floor(it) }
            val particle = particles[i]
            position(particle, i, t, width, height, centerX, centerY, coreRadius, false)
            position(particle, i, (t - .025f).coerceAtLeast(0f), width, height, centerX, centerY, coreRadius, true)
            val fadeIn = (t / .12f).coerceIn(0f, 1f)
            val fadeOut = ((1f - t) / .22f).coerceIn(0f, 1f)
            particle.alpha = fadeIn * fadeOut * .8f
            particle.radius = coreRadius * (.012f + (i % 3) * .004f) * (1f - t * .7f)
            if (t > .82f) absorption += (1f - (1f - t) / .18f) * particle.alpha
        }
        absorption = (absorption / 3f).coerceIn(0f, 1f)
        return count
    }

    private fun position(particle: Particle, index: Int, t: Float, width: Float, height: Float,
                         centerX: Float, centerY: Float, coreRadius: Float, tail: Boolean) {
        val ease = t * t * (3f - 2f * t)
        val seed = index * 2.399963f
        val envelope = sin(PI * t).toFloat() * (1f - t)
        val spin = seed + t * (PI * 4).toFloat()
        val startX = centerX + sin(seed).toFloat() * width * .32f
        val x = (startX + (centerX - startX) * ease + sin(spin).toFloat() * coreRadius * .9f * envelope)
            .coerceIn(1f, width - 1f)
        val bottom = height + coreRadius * .08f
        val y = bottom + (centerY - bottom) * ease + cos(spin).toFloat() * coreRadius * .3f * envelope
        if (tail) { particle.tailX = x; particle.tailY = y } else { particle.x = x; particle.y = y }
    }

    companion object {
        const val MAX_PARTICLES = 18

        fun budget(power: PowerReading, enabled: Boolean, motion: Boolean, density: Float,
                   lowCost: Boolean = false, visible: Boolean = true): Int {
            if (!visible || !enabled || !motion || power.connected != true || !power.charging ||
                power.verifiedFull || !density.isFinite() || density <= 0f) return 0
            val normal = (4 + 14 * density.coerceIn(0f, 1f)).toInt()
            return if (lowCost || power.battery.percent?.let { it < 20 } == true) minOf(6, normal) else normal
        }
    }
}
