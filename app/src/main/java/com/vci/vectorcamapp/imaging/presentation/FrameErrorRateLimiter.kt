package com.vci.vectorcamapp.imaging.presentation

/**
 * Suppresses repetitive frame-processing error events so that analytics and crash
 * logs are not flooded during sustained failure periods (e.g. a bad camera frame
 * arriving at 30 fps). Records the very first occurrence and then every [every]-th
 * subsequent occurrence, keeping signal-to-noise ratio manageable.
 */
internal class FrameErrorRateLimiter(private val every: Int = 50) {
    private var count = 0
    fun shouldRecord(): Boolean {
        val n = ++count
        return n == 1 || n % every == 0
    }
}
