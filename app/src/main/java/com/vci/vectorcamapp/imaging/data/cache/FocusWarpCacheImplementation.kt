package com.vci.vectorcamapp.imaging.data.cache

import com.vci.vectorcamapp.imaging.domain.cache.FocusWarpCache
import javax.inject.Inject

class FocusWarpCacheImplementation @Inject constructor() : FocusWarpCache {

    private val warps = mutableMapOf<Int, FloatArray>()

    @Synchronized
    override fun get(planeIndex: Int): FloatArray? = warps[planeIndex]?.copyOf()

    @Synchronized
    override fun put(planeIndex: Int, warp: FloatArray) {
        warps[planeIndex] = warp.copyOf()
    }
}
