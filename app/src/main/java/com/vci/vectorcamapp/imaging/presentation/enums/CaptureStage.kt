package com.vci.vectorcamapp.imaging.presentation.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.vci.vectorcamapp.R

enum class CaptureStage(
    @DrawableRes val iconResId: Int,
    @StringRes val labelResId: Int
) {
    CAPTURING(R.drawable.ic_camera, R.string.capture_animation_label_capturing),
    DETECTING(R.drawable.ic_visibility, R.string.capture_animation_label_detecting),
    CLASSIFYING(R.drawable.ic_specimen, R.string.capture_animation_label_classifying)
}
