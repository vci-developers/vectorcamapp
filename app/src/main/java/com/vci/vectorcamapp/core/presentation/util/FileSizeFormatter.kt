package com.vci.vectorcamapp.core.presentation.util

import java.util.Locale

object FileSizeFormatter {
    private const val BYTES_PER_KIB = 1024.0

    fun format(bytes: Long): String {
        if (bytes < BYTES_PER_KIB) return "$bytes B"
        val kib = bytes / BYTES_PER_KIB
        if (kib < BYTES_PER_KIB) {
            return String.format(Locale.US, "%.1f KB", kib)
        }
        val mib = kib / BYTES_PER_KIB
        return String.format(Locale.US, "%.1f MB", mib)
    }
}
