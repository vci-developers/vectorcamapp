package com.vci.vectorcamapp.imaging.domain

import java.io.Closeable

/**
 * A model whose build is expensive enough to be worth starting before first use, and to be torn
 * down without discarding the object that owns it.
 *
 * Separating [release] from [Closeable.close] is what lets one instance outlive several
 * load/unload cycles: the models can be dropped while the app is in the background and rebuilt
 * when it returns, without rebuilding the dependency graph around them.
 */
interface WarmableModel : Closeable {

    /** Starts building the model if it is not built already. Returns without waiting for it. */
    fun warm()

    /** Frees the built model. A later [warm] rebuilds it. */
    fun release()
}
