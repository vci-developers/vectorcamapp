package com.vci.vectorcamapp.navigation

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object Landing : Destination

    @Serializable
    data object SurveillanceForm : Destination

    @Serializable
    data object Imaging : Destination

    @Serializable
    data object IncompleteSession : Destination

    @Serializable
    data object CompleteSessionList : Destination

    @Serializable
    data object CompleteSessionDetails : Destination {
        const val ROUTE = "complete_session_details"
        fun createRoute(sessionId: String) = "$ROUTE/$sessionId"
    }

    @Serializable
    data object CompleteSessionSpecimens : Destination {
        const val ROUTE = "complete_session_specimens"
        fun createRoute(sessionId: String) = "$ROUTE/$sessionId"
    }
}
