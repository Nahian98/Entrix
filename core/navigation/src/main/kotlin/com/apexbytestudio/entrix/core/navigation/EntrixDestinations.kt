package com.apexbytestudio.entrix.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes. Declared as serializable objects so Navigation Compose can use them
 * directly without string routes. New arguments become constructor parameters on these types.
 */
sealed interface EntrixRoute {

    @Serializable
    data object Dashboard : EntrixRoute

    @Serializable
    data object Attendance : EntrixRoute

    @Serializable
    data object Reports : EntrixRoute

    @Serializable
    data object Settings : EntrixRoute

    /** The list of configured offices. */
    @Serializable
    data object OfficeList : EntrixRoute

    /** Add or edit a single office; [officeId] is 0 (Office.NO_ID) when adding a new one. */
    @Serializable
    data class OfficeEdit(val officeId: Long = 0L) : EntrixRoute
}
