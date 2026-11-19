package com.vci.vectorcamapp.core.data.network.api

object UploadRoutes {
    const val REGISTER_DEVICE = "devices/register"
    const val SESSIONS = "sessions"
    const val DATA_RECOVERIES = "data-recoveries"

    fun sessionUnits(sessionId: Int) = sessionUnits(sessionId.toString())
    fun sessionUnits(sessionKey: String) = "sessions/$sessionKey/units"
    fun formAnswers(sessionId: Int) = formAnswers(sessionId.toString())
    fun formAnswers(sessionKey: String) = "sessions/$sessionKey/forms/answers"
    fun surveillanceForm(sessionId: Int) = surveillanceForm(sessionId.toString())
    fun surveillanceForm(sessionKey: String) = "sessions/$sessionKey/survey"
    fun specimens(sessionId: Int) = specimens(sessionId.toString())
    fun specimens(sessionKey: String) = "sessions/$sessionKey/specimens"
    fun specimenImagesData(specimenId: Int) = "specimens/$specimenId/images/data"
    fun specimenImagesDataForUnsyncedSpecimen(sessionId: Int, specimenId: String) =
        specimenImagesDataForUnsyncedSpecimen(sessionId.toString(), specimenId)
    fun specimenImagesDataForUnsyncedSpecimen(sessionKey: String, specimenId: String) =
        "sessions/$sessionKey/specimens/$specimenId/images/data"
    fun dataRecoveryTus(recoveryId: Int) = "data-recoveries/$recoveryId/tus"
}
