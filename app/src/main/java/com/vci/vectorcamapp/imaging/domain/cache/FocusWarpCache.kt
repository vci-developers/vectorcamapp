package com.vci.vectorcamapp.imaging.domain.cache

interface FocusWarpCache {
    fun get(planeIndex: Int): FloatArray?
    fun put(planeIndex: Int, warp: FloatArray)
}
