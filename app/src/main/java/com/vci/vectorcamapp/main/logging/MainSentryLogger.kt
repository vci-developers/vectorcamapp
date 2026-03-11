package com.vci.vectorcamapp.main.logging

interface MainSentryLogger {

    fun logOpenCvInitFailure(e: Throwable)

    fun logPostHogInitFailure(e: Throwable)

    fun logDeviceFetchFailure(e: Throwable)
}
