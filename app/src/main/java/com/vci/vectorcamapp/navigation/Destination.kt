package com.vci.vectorcamapp.navigation

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object Splash : Destination

    @Serializable
    data object Landing : Destination

    @Serializable
    data object SurveillanceForm : Destination

    @Serializable
    data object Imaging : Destination

    @Serializable
    data object IncompleteSession : Destination
}
