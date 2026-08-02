package com.vci.vectorcamapp.core.logging.analytics

import androidx.navigation.NavBackStackEntry

fun NavBackStackEntry.analyticsScreenName(): String {
    val route = destination.route ?: return "Unknown"
    val simpleName = route
        .substringAfterLast(".")
        .substringBefore("?")
        .substringBefore("/")
    return simpleName.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
}
