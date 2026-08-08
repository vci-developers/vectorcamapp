package com.vci.vectorcamapp.imaging.domain

object ModelFileNames {
    const val DETECTOR = "detect.tflite"
    const val SPECIES_CLASSIFIER = "species.tflite"
    const val SEX_CLASSIFIER = "sex.tflite"
    const val ABDOMEN_STATUS_CLASSIFIER = "abdomen_status.tflite"

    val ALL = listOf(DETECTOR, SPECIES_CLASSIFIER, SEX_CLASSIFIER, ABDOMEN_STATUS_CLASSIFIER)
}
