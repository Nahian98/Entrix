package com.apexbytestudio.entrix.core.common.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant

private const val MINUTE_MILLIS = 60_000L

/**
 * Emits the current instant immediately, then again at each wall-clock minute boundary, so a
 * "time so far" display flips exactly when the minute changes. Runs until the collector cancels;
 * pair it with `SharingStarted.WhileSubscribed` so it stops when nothing is on screen.
 */
fun minuteTicks(clock: Clock): Flow<Instant> = flow {
    while (true) {
        val now = clock.instant()
        emit(now)
        delay(MINUTE_MILLIS - now.toEpochMilli() % MINUTE_MILLIS)
    }
}
