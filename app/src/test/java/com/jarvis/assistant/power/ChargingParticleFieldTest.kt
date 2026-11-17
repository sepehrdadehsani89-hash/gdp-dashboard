package com.jarvis.assistant.power

import com.jarvis.assistant.hud.BatteryReading
import org.junit.Assert.*
import org.junit.Test

class ChargingParticleFieldTest {
    private fun charging(percent: Int? = 64) = PowerReading(BatteryReading(percent, charging = true), PowerCondition.CHARGING, true)
    private fun budget(power: PowerReading = charging(), enabled: Boolean = true, motion: Boolean = true,
                       density: Float = .45f, lowCost: Boolean = false, visible: Boolean = true) =
        ChargingParticleField.budget(power, enabled, motion, density, lowCost, visible)
    private fun ChargingParticleField.frame(phase: Float, count: Int = 10) = update(count, phase, 360f, 720f, 180f, 330f, 120f)

    @Test fun confirmedChargingAllocatesASmallParticleBudget() { assertTrue(budget() in 1..18) }
    @Test fun unknownBatteryStateCannotInventCharging() { assertEquals(0, budget(PowerReading())) }
    @Test fun unpluggingImmediatelyDisablesTransport() { assertEquals(0, budget(charging().copy(connected = false))) }
    @Test fun connectedButNotChargingDoesNotTransportEnergy() {
        assertEquals(0, budget(charging().copy(condition = PowerCondition.PLUGGED_NOT_CHARGING)))
    }
    @Test fun verifiedFullBatteryStopsIncomingParticles() {
        assertEquals(0, budget(charging(100)))
        assertEquals(0, budget(charging(100).copy(condition = PowerCondition.FULL)))
    }
    @Test fun disabledChargingEffectsRemainDisabled() { assertEquals(0, budget(enabled = false)) }
    @Test fun reducedOrPausedMotionUsesOnlyTheExistingStaticIndicator() { assertEquals(0, budget(motion = false)) }
    @Test fun hiddenWallpaperDoesNotHaveAParticleWorkload() { assertEquals(0, budget(visible = false)) }
    @Test fun zeroParticleDensityHonorsTheSavedPreference() { assertEquals(0, budget(density = 0f)) }
    @Test fun malformedDensityDoesNotCreateUnboundedWork() {
        assertEquals(0, budget(density = Float.NaN)); assertEquals(0, budget(density = Float.POSITIVE_INFINITY))
    }
    @Test fun powerAndThermalBudgetKeepsAtMostSixParticles() {
        assertEquals(6, budget(density = 1f, lowCost = true)); assertTrue(budget(charging(5)) <= 6)
    }
    @Test fun excessiveDensityIsCappedAtTheFixedBuffer() { assertEquals(18, budget(density = 100f)) }
    @Test fun knownChargingDoesNotFabricateAnUnavailablePercentage() {
        val power = charging(null); assertTrue(budget(power) > 0); assertNull(power.battery.percent)
    }
    @Test fun invalidGeometryAndPhaseProduceNoCoordinates() {
        val field = ChargingParticleField()
        assertEquals(0, field.update(10, 0f, 0f, 720f, 0f, 330f, 120f))
        assertEquals(0, field.frame(Float.NaN)); assertEquals(0f, field.absorption, 0f)
    }
    @Test fun generatedCoordinatesAndOpacityAreFiniteAndBounded() {
        val field = ChargingParticleField()
        for (phase in listOf(0f, .1f, .5f, .99f, 3.5f)) {
            assertEquals(18, field.frame(phase, 100))
            for (particle in field.particles) {
                assertTrue(particle.x.isFinite() && particle.y.isFinite())
                assertTrue(particle.tailX.isFinite() && particle.tailY.isFinite())
                assertTrue(particle.x in 1f..359f); assertTrue(particle.alpha in 0f..1f)
                assertTrue(particle.radius > 0f)
            }
        }
    }
    @Test fun successiveVisiblePhasesActuallyChangeParticleCoordinates() {
        val field = ChargingParticleField(); field.frame(.2f)
        val y = field.particles[0].y; field.frame(.25f); assertNotEquals(y, field.particles[0].y)
    }
    @Test fun phaseWrapIsDeterministicAndDoesNotAccumulateHiddenTime() {
        val field = ChargingParticleField(); field.frame(.25f)
        val x = field.particles[0].x; val y = field.particles[0].y
        field.frame(4.25f); assertEquals(x, field.particles[0].x, .001f); assertEquals(y, field.particles[0].y, .001f)
    }
    @Test fun particlesShrinkAndFadeAsTheyAreAbsorbedByTheCore() {
        val field = ChargingParticleField(); field.frame(.5f, 1)
        val initialY = field.particles[0].y; val initialRadius = field.particles[0].radius; val initialAlpha = field.particles[0].alpha
        field.frame(.99f, 1)
        assertTrue(kotlin.math.abs(field.particles[0].y - 330f) < kotlin.math.abs(initialY - 330f))
        assertTrue(field.particles[0].radius < initialRadius); assertTrue(field.particles[0].alpha < initialAlpha)
    }
    @Test fun updatesReuseTheSameParticleObjects() {
        val field = ChargingParticleField(); val first = field.particles[0]
        field.frame(.1f); field.frame(.3f); assertSame(first, field.particles[0])
    }
    @Test fun aZeroBudgetClearsTheActiveFieldWithoutResidualAbsorption() {
        val field = ChargingParticleField(); field.frame(.95f, 1); assertTrue(field.absorption > 0f)
        assertEquals(0, field.frame(.96f, budget(charging().copy(connected = false))))
        assertEquals(0, field.count); assertEquals(0f, field.absorption, 0f)
    }
}
