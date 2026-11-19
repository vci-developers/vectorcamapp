package com.vci.vectorcamapp.core.data.dto.data_recovery

import com.vci.vectorcamapp.core.data.dto.form_answer.PostFormAnswersRequestDto
import com.vci.vectorcamapp.core.data.dto.session.PostSessionRequestDto
import com.vci.vectorcamapp.core.data.dto.session_unit.PostSessionUnitRequestDto
import com.vci.vectorcamapp.core.data.dto.specimen.PostSpecimenRequestDto
import com.vci.vectorcamapp.core.data.dto.specimen_image.PostSpecimenImageRequestDto
import com.vci.vectorcamapp.core.data.dto.surveillance_form.PostSurveillanceFormRequestDto
import kotlinx.serialization.Serializable

@Serializable
data class SessionRecoveryBundleDto(
    val session: PostSessionRequestDto = PostSessionRequestDto(),
    val formVersion: String? = null,
    val units: List<PostSessionUnitRequestDto> = emptyList(),
    val formAnswers: PostFormAnswersRequestDto? = null,
    val surveillanceForm: PostSurveillanceFormRequestDto? = null,
    val specimens: List<SpecimenRecoveryBundleDto> = emptyList(),
)

@Serializable
data class SessionUnitRecoveryBundleDto(
    val unit: PostSessionUnitRequestDto = PostSessionUnitRequestDto(),
    val specimens: List<SpecimenRecoveryBundleDto> = emptyList(),
)

@Serializable
data class SpecimenRecoveryBundleDto(
    val specimen: PostSpecimenRequestDto = PostSpecimenRequestDto(),
    val images: List<PostSpecimenImageRequestDto> = emptyList(),
)
