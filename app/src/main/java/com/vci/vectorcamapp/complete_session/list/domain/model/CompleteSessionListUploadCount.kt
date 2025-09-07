package com.vci.vectorcamapp.complete_session.list.domain.model

import androidx.room.Ignore
import java.util.UUID

data class CompleteSessionListUploadCount(
    val sessionId: UUID,
    val uploadedImages: Int,
    val totalImages: Int
) {
    @Ignore
    val progress: Float = if (totalImages > 0) uploadedImages.toFloat() / totalImages else 0f
}
