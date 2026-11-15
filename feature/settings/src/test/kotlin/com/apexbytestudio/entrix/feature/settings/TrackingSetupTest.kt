package com.apexbytestudio.entrix.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingSetupTest {

    private fun facts(
        location: Boolean = true,
        background: Boolean = true,
        battery: Boolean = true,
        needsBackground: Boolean = true,
    ) = SetupFacts(location, background, battery, needsBackground)

    @Test
    fun `everything granted is all done and ready`() {
        val items = setupItems(facts())

        assertEquals(
            listOf(SetupItemId.Location, SetupItemId.BackgroundLocation, SetupItemId.Battery),
            items.map { it.id },
        )
        assertTrue(items.all { it.state == SetupItemState.Done && it.actionLabel == null })
        assertEquals(TrackingHealth.Ready, trackingHealth(trackingEnabled = true, items))
    }

    @Test
    fun `missing location blocks tracking and disables the background step`() {
        val items = setupItems(facts(location = false, background = false))
        val background = items.first { it.id == SetupItemId.BackgroundLocation }

        assertEquals(SetupItemState.Required, items.first().state)
        assertEquals("Allow", items.first().actionLabel)
        assertEquals(SetupItemState.Required, background.state)
        assertFalse(background.actionEnabled)
        assertEquals(TrackingHealth.NeedsAction(2), trackingHealth(trackingEnabled = true, items))
    }

    @Test
    fun `restricted battery is only a suggestion`() {
        val items = setupItems(facts(battery = false))

        assertEquals(SetupItemState.Suggested, items.last().state)
        assertEquals("Fix", items.last().actionLabel)
        assertEquals(TrackingHealth.Suggestions(1), trackingHealth(trackingEnabled = true, items))
    }

    @Test
    fun `before Android 10 there is no separate background step`() {
        val items = setupItems(facts(needsBackground = false))

        assertNull(items.firstOrNull { it.id == SetupItemId.BackgroundLocation })
        assertEquals(2, items.size)
    }

    @Test
    fun `tracking off reports off regardless of setup`() {
        assertEquals(
            TrackingHealth.Off,
            trackingHealth(trackingEnabled = false, setupItems(facts(location = false))),
        )
    }
}
