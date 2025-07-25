package com.vci.vectorcamapp.core.data.room

import kotlinx.coroutines.CompletableDeferred

object DbSeedStatus {
    val seeded = CompletableDeferred<Unit>()
}
