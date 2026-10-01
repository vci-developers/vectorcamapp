package com.vci.vectorcamapp.core.presentation.extensions

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.imaging.domain.enums.AbdomenStatusLabel
import com.vci.vectorcamapp.imaging.domain.enums.SexLabel
import com.vci.vectorcamapp.imaging.domain.enums.SpeciesLabel

fun SpeciesLabel.displayText(context: Context): String {
    val resId = when (this) {
        SpeciesLabel.ANOPHELES_FUNESTUS -> R.string.species_anopheles_funestus
        SpeciesLabel.ANOPHELES_GAMBIAE -> R.string.species_anopheles_gambiae
        SpeciesLabel.ANOPHELES_OTHER -> R.string.species_anopheles_other
        SpeciesLabel.CULEX -> R.string.species_culex
        SpeciesLabel.AEDES -> R.string.species_aedes
        SpeciesLabel.MANSONIA -> R.string.species_mansonia
        SpeciesLabel.NON_MOSQUITO -> R.string.species_non_mosquito
    }
    return context.getString(resId)
}

fun SexLabel.displayText(context: Context): String {
    val resId = when (this) {
        SexLabel.FEMALE -> R.string.sex_female
        SexLabel.MALE -> R.string.sex_male
    }
    return context.getString(resId)
}

fun AbdomenStatusLabel.displayText(context: Context): String {
    val resId = when (this) {
        AbdomenStatusLabel.UNFED -> R.string.abdomen_status_unfed
        AbdomenStatusLabel.FULLY_FED -> R.string.abdomen_status_fully_fed
        AbdomenStatusLabel.GRAVID -> R.string.abdomen_status_gravid
    }
    return context.getString(resId)
}

fun SpeciesLabel.Companion.displayTextFor(context: Context, wireValue: String?): String? =
    wireValue?.let { fromWireValue(it)?.displayText(context) ?: it }

fun SexLabel.Companion.displayTextFor(context: Context, wireValue: String?): String? =
    wireValue?.let { fromWireValue(it)?.displayText(context) ?: it }

fun AbdomenStatusLabel.Companion.displayTextFor(context: Context, wireValue: String?): String? =
    wireValue?.let { fromWireValue(it)?.displayText(context) ?: it }
