package com.apexbytestudio.entrix.feature.settings

/** The system-owned facts that decide whether automatic tracking can actually work. */
data class SetupFacts(
    val locationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val batteryUnrestricted: Boolean,
    /** Background location is a separate grant on Android 10+; earlier it comes with location. */
    val needsBackgroundPermission: Boolean,
)

enum class SetupItemId { Location, BackgroundLocation, Battery }

enum class SetupItemState {
    Done,

    /** Tracking cannot work until this is fixed. */
    Required,

    /** Tracking works, but less reliably. */
    Suggested,
}

data class SetupItem(
    val id: SetupItemId,
    val title: String,
    val description: String,
    val state: SetupItemState,
    /** Label of the button that fixes it; null when done. */
    val actionLabel: String?,
    /** False when another item has to be fixed first (background needs location). */
    val actionEnabled: Boolean = true,
)

/** One-line answer to "is tracking working?", shown on the main switch. */
sealed interface TrackingHealth {
    data object Off : TrackingHealth
    data object Ready : TrackingHealth
    data class NeedsAction(val count: Int) : TrackingHealth
    data class Suggestions(val count: Int) : TrackingHealth
}

/** The checklist, in the order a person should work through it. */
fun setupItems(facts: SetupFacts): List<SetupItem> = buildList {
    add(
        SetupItem(
            id = SetupItemId.Location,
            title = "Location access",
            description = if (facts.locationGranted) {
                "Entrix can detect when you're at an office."
            } else {
                "Needed to detect when you arrive at an office."
            },
            state = if (facts.locationGranted) SetupItemState.Done else SetupItemState.Required,
            actionLabel = if (facts.locationGranted) null else "Allow",
        ),
    )
    if (facts.needsBackgroundPermission) {
        val granted = facts.backgroundLocationGranted
        add(
            SetupItem(
                id = SetupItemId.BackgroundLocation,
                title = "Location all the time",
                description = when {
                    granted -> "Works even when the app is closed."
                    !facts.locationGranted -> "Allow location access first."
                    else -> "Choose \"Allow all the time\" so visits are logged with the app closed."
                },
                state = if (granted) SetupItemState.Done else SetupItemState.Required,
                actionLabel = if (granted) null else "Allow",
                actionEnabled = facts.locationGranted,
            ),
        )
    }
    add(
        SetupItem(
            id = SetupItemId.Battery,
            title = "Unrestricted battery",
            description = if (facts.batteryUnrestricted) {
                "Check-ins and check-outs won't be delayed."
            } else {
                "Battery saving can delay check-ins and check-outs."
            },
            state = if (facts.batteryUnrestricted) SetupItemState.Done else SetupItemState.Suggested,
            actionLabel = if (facts.batteryUnrestricted) null else "Fix",
        ),
    )
}

fun trackingHealth(trackingEnabled: Boolean, items: List<SetupItem>): TrackingHealth {
    if (!trackingEnabled) return TrackingHealth.Off
    val required = items.count { it.state == SetupItemState.Required }
    val suggested = items.count { it.state == SetupItemState.Suggested }
    return when {
        required > 0 -> TrackingHealth.NeedsAction(required)
        suggested > 0 -> TrackingHealth.Suggestions(suggested)
        else -> TrackingHealth.Ready
    }
}
