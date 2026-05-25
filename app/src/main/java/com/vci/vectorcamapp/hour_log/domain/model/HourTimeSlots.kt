package com.vci.vectorcamapp.hour_log.domain.model

object HourTimeSlots {

    val all: List<String> = (0 until 24).map { hour ->
        "${formatHour(hour)} - ${formatHour((hour + 1) % 24)}"
    }

    private fun formatHour(hour: Int): String = when {
        hour == 0 -> "12:00 AM"
        hour < 12 -> "$hour:00 AM"
        hour == 12 -> "12:00 PM"
        else -> "${hour - 12}:00 PM"
    }
}
