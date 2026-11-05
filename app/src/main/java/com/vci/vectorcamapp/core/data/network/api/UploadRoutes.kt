package com.vci.vectorcamapp.core.data.network.api

object UploadRoutes {
    const val REGISTER_DEVICE = "devices/register"
    const val SESSIONS = "sessions"
    const val DATA_RECOVERIES = "data-recoveries"

    fun sessionUnits(sessionId: Int) = "sessions/$sessionId/units"
    fun formAnswers(sessionId: Int) = "sessions/$sessionId/forms/answers"
    fun surveillanceForm(sessionId: Int) = "sessions/$sessionId/survey"
    fun specimens(sessionId: Int) = "sessions/$sessionId/specimens"
    fun specimenImagesData(specimenId: Int) = "specimens/$specimenId/images/data"
}
