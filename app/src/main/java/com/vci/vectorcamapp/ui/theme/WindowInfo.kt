package com.vci.vectorcamapp.ui.theme

enum class WindowType {
    Compact,
    Medium,
    Expanded
}

fun getWindowType(widthDp: Float): WindowType {
    return when {
        widthDp < 600 -> WindowType.Compact
        widthDp < 840 -> WindowType.Medium
        else -> WindowType.Expanded
    }
