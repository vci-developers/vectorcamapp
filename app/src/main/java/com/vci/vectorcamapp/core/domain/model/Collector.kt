package com.vci.vectorcamapp.core.domain.model

import java.util.UUID

data class Collector(
    val id: UUID,
    val name: String,
    val title: String
)
