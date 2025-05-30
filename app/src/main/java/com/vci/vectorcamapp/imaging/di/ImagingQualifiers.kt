package com.vci.vectorcamapp.imaging.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SpecimenIdRecognizer

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Detector

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SpeciesClassifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SexClassifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AbdomenStatusClassifier