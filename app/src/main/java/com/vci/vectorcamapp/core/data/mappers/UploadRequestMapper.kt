package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.core.data.dto.device.RegisterDeviceRequestDto
import com.vci.vectorcamapp.core.data.dto.form_answer.FormAnswerRequestItemDto
import com.vci.vectorcamapp.core.data.dto.form_answer.PostFormAnswersRequestDto
import com.vci.vectorcamapp.core.data.dto.inference_result.InferenceResultDto
import com.vci.vectorcamapp.core.data.dto.session.PostSessionRequestDto
import com.vci.vectorcamapp.core.data.dto.session_unit.PostSessionUnitRequestDto
import com.vci.vectorcamapp.core.data.dto.specimen.PostSpecimenRequestDto
import com.vci.vectorcamapp.core.data.dto.specimen_image.PostSpecimenImageRequestDto
import com.vci.vectorcamapp.core.data.dto.surveillance_form.PostSurveillanceFormRequestDto
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm

fun Device.toRegisterDeviceRequestDto(programId: Int): RegisterDeviceRequestDto {
    return RegisterDeviceRequestDto(
        model = this.model,
        registeredAt = this.registeredAt,
        programId = programId,
        appVersion = BuildConfig.VERSION_CODE.toString() + "(" + BuildConfig.VERSION_NAME + ")",
    )
}

fun Session.toPostSessionRequestDto(
    siteId: Int, deviceId: Int, expectedSpecimens: Int
): PostSessionRequestDto {
    return PostSessionRequestDto(
        frontendId = this.localId,
        hardwareId = this.hardwareId,
        collectorTitle = this.collectorTitle,
        collectorName = this.collectorName,
        collectorLastTrainedOn = this.collectorLastTrainedOn,
        collectionDate = this.collectionDate,
        collectionMethod = this.collectionMethod,
        specimenCondition = this.specimenCondition,
        createdAt = this.createdAt,
        completedAt = this.completedAt ?: 0L,
        notes = this.notes,
        latitude = this.latitude,
        longitude = this.longitude,
        gpsAccuracy = this.gpsAccuracy,
        type = this.type,
        expectedSpecimens = expectedSpecimens,
        siteId = siteId,
        deviceId = deviceId,
    )
}

fun SessionUnit.toPostSessionUnitRequestDto(): PostSessionUnitRequestDto {
    return PostSessionUnitRequestDto(
        frontendId = this.localId,
        unitOrder = this.unitOrder,
        createdAt = this.createdAt,
    )
}

fun toPostFormAnswersRequestDto(
    formVersion: String,
    sessionScopedAnswers: Map<Int, FormAnswer>,
    sessionUnitScopedAnswersBySessionUnitRemoteId: Map<Int, Map<Int, FormAnswer>>,
    submittedAt: Long,
): PostFormAnswersRequestDto {
    return PostFormAnswersRequestDto(
        answers = buildList {
            sessionScopedAnswers.forEach { (questionId, answer) ->
                add(answer.toFormAnswerRequestItemDto(questionId, sessionUnitId = null))
            }
            sessionUnitScopedAnswersBySessionUnitRemoteId.forEach { (remoteSessionUnitId, answers) ->
                answers.forEach { (questionId, answer) ->
                    add(answer.toFormAnswerRequestItemDto(questionId, remoteSessionUnitId))
                }
            }
        },
        formVersion = formVersion,
        submittedAt = submittedAt,
    )
}

private fun FormAnswer.toFormAnswerRequestItemDto(
    questionId: Int, sessionUnitId: Int?
): FormAnswerRequestItemDto {
    return FormAnswerRequestItemDto(
        frontendId = this.localId,
        questionId = questionId,
        sessionUnitId = sessionUnitId,
        value = this.value,
        dataType = this.dataType,
    )
}

fun SurveillanceForm.toPostSurveillanceFormRequestDto(sessionId: Int): PostSurveillanceFormRequestDto {
    return PostSurveillanceFormRequestDto(
        sessionId = sessionId,
        numPeopleSleptInHouse = this.numPeopleSleptInHouse,
        wasIrsConducted = this.wasIrsConducted,
        monthsSinceIrs = this.monthsSinceIrs,
        numLlinsAvailable = this.numLlinsAvailable,
        llinType = this.llinType,
        llinBrand = this.llinBrand,
        numPeopleSleptUnderLlin = this.numPeopleSleptUnderLlin,
        numChildrenUnder5 = this.numChildrenUnder5,
        hasPregnantWoman = this.hasPregnantWoman,
    )
}

fun Specimen.toPostSpecimenRequestDto(
    sessionUnitId: Int?, expectedImages: Int
): PostSpecimenRequestDto {
    return PostSpecimenRequestDto(
        specimenId = this.id,
        sessionUnitId = sessionUnitId,
        shouldProcessFurther = this.shouldProcessFurther,
        expectedImages = expectedImages,
    )
}

fun SpecimenImage.toPostSpecimenImageRequestDto(
    inferenceResult: InferenceResult?
): PostSpecimenImageRequestDto {
    return PostSpecimenImageRequestDto(
        filemd5 = this.localId,
        species = this.species,
        sex = this.sex,
        abdomenStatus = this.abdomenStatus,
        capturedAt = this.capturedAt,
        inferenceResult = inferenceResult?.toInferenceResultDto(),
        metadata = this.imageMetadata?.toImageMetadataDto(),
    )
}

fun InferenceResult.toInferenceResultDto(): InferenceResultDto {
    return InferenceResultDto(
        bboxTopLeftX = this.bboxTopLeftX,
        bboxTopLeftY = this.bboxTopLeftY,
        bboxWidth = this.bboxWidth,
        bboxHeight = this.bboxHeight,
        bboxConfidence = this.bboxConfidence,
        bboxClassId = this.bboxClassId,
        speciesLogits = this.speciesLogits,
        sexLogits = this.sexLogits,
        abdomenStatusLogits = this.abdomenStatusLogits,
        bboxDetectionDuration = this.bboxDetectionDuration,
        speciesInferenceDuration = this.speciesInferenceDuration,
        sexInferenceDuration = this.sexInferenceDuration,
        abdomenStatusInferenceDuration = this.abdomenStatusInferenceDuration
    )
}
