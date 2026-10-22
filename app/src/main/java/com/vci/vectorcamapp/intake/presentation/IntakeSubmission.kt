package com.vci.vectorcamapp.intake.presentation

import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DefaultIntakeFieldsCache
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SurveillanceFormRepository
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.errorOrNull
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.intake.domain.use_cases.IntakeValidationUseCases
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import com.vci.vectorcamapp.intake.logging.IntakeErrorLogger
import com.vci.vectorcamapp.intake.presentation.model.IntakeErrors

internal class IntakeSubmission(
    private val validation: IntakeValidationUseCases,
    private val transactions: TransactionHelper,
    private val sessions: SessionRepository,
    private val surveillanceForms: SurveillanceFormRepository,
    private val formAnswers: FormAnswerRepository,
    private val sessionCache: CurrentSessionCache,
    private val defaultFields: DefaultIntakeFieldsCache,
) {
    suspend fun submit(
        currentState: () -> IntakeState,
        isCurrentCollectorMissing: () -> Boolean,
        updateState: ((IntakeState) -> IntakeState) -> Unit,
        reportError: (Error) -> Unit,
        onSubmitted: suspend (Session) -> Unit,
    ) {
        val snapshot = currentState()
        val results = validate(snapshot)
        updateState { it.copy(intakeErrors = results.toErrors()) }
        when {
            results.hasError() -> reportError(IntakeError.FORM_INVALID)
            isCurrentCollectorMissing() -> reportError(IntakeError.MISSING_COLLECTOR)
            else -> persist(snapshot, currentState, reportError, onSubmitted)
        }
    }

    private fun validate(snapshot: IntakeState): IntakeFieldResults {
        val session = snapshot.session
        val surveillanceForm = snapshot.surveillanceForm
        val requiresDistrict = snapshot.allSitesInProgram.any { !it.district.isNullOrBlank() }
        return IntakeFieldResults(
            collector = validation.validateCollector(session.collectorName, session.collectorTitle),
            district = if (requiresDistrict) {
                validation.validateDistrict(snapshot.selectedDistrict)
            } else {
                Result.Success(Unit)
            },
            villageName = if (requiresDistrict) {
                validation.validateVillageName(snapshot.selectedVillageName)
            } else {
                Result.Success(Unit)
            },
            houseNumber = if (requiresDistrict) {
                validation.validateHouseNumber(snapshot.selectedHouseNumber)
            } else {
                Result.Success(Unit)
            },
            locationTypeSiteSelections = snapshot.allLocationTypesInProgram.associate { locationType ->
                val selection = snapshot.siteSelectionsByLocationTypeId[locationType.id]
                locationType.id to selection.isNullOrBlank().let { blank ->
                    if (blank) FormValidationError.BLANK_LOCATION_TYPE_SELECTION else null
                }
            },
            llinType = surveillanceForm?.llinType?.let { validation.validateLlinType(it) },
            llinBrand = surveillanceForm?.llinBrand?.let { validation.validateLlinBrand(it) },
            collectionDate = validation.validateCollectionDate(session.collectionDate),
            collectionMethod = validation.validateCollectionMethod(session.collectionMethod),
            specimenCondition = validation.validateSpecimenCondition(session.specimenCondition),
            numPeopleSleptInHouse = surveillanceForm?.let {
                validation.validateNumPeopleSleptInHouse(it.numPeopleSleptInHouse)
            },
            monthsSinceIrs = monthsSinceIrsResult(surveillanceForm),
            numLlinsAvailable = surveillanceForm?.let {
                validation.validateNumLlinsAvailable(it.numLlinsAvailable)
            },
            numPeopleSleptUnderLlin = surveillanceForm?.numPeopleSleptUnderLlin?.let {
                validation.validateNumPeopleSleptUnderLlin(it)
            },
            numChildrenUnder5 = surveillanceForm?.let {
                validation.validateNumChildrenUnder5(it.numChildrenUnder5)
            },
            formAnswers = validation.validateFormAnswersUseCase(
                snapshot.formQuestions,
                snapshot.formAnswersByQuestionId,
            ),
        )
    }

    private fun monthsSinceIrsResult(
        surveillanceForm: SurveillanceForm?,
    ): Result<Unit, FormValidationError>? {
        val months = surveillanceForm
            ?.takeIf { it.wasIrsConducted && it.monthsSinceIrs != null }
            ?.monthsSinceIrs
            ?: return null
        return validation.validateMonthsSinceIrs(months)
    }

    private suspend fun persist(
        snapshot: IntakeState,
        currentState: () -> IntakeState,
        reportError: (Error) -> Unit,
        onSubmitted: suspend (Session) -> Unit,
    ) {
        val session = snapshot.session
        val selectedSite = findSelectedSite(snapshot) ?: run {
            reportError(IntakeError.SITE_NOT_FOUND)
            IntakeErrorLogger.logSiteNotFound(Exception(IntakeError.SITE_NOT_FOUND.name))
            return
        }
        val saved = saveIntake(session, snapshot.surveillanceForm, selectedSite, currentState, reportError)
        if (!saved) return
        sessionCache.saveSession(session, selectedSite.id)
        val latest = currentState()
        defaultFields.saveDefaultIntakeFields(
            collectorName = session.collectorName,
            collectorTitle = session.collectorTitle,
            collectorLastTrainedOn = session.collectorLastTrainedOn,
            hardwareId = session.hardwareId,
            district = latest.selectedDistrict,
            villageName = latest.selectedVillageName,
            locationSelections = locationSelectionsToCache(latest),
        )
        onSubmitted(session)
    }

    private suspend fun saveIntake(
        session: Session,
        surveillanceForm: SurveillanceForm?,
        selectedSite: Site,
        currentState: () -> IntakeState,
        reportError: (Error) -> Unit,
    ): Boolean {
        return transactions.runAsTransaction {
            val sessionResult = sessions.upsertSession(session, selectedSite.id)
            sessionResult.onError { error ->
                reportError(error)
                IntakeErrorLogger.logSessionUpsertFailed(
                    Exception(error.name),
                    session.localId,
                    selectedSite.id,
                )
                return@runAsTransaction false
            }
            val surveillanceFormResult = surveillanceForm?.let {
                surveillanceForms.upsertSurveillanceForm(surveillanceForm, session.localId)
            } ?: Result.Success(Unit)
            surveillanceFormResult.onError { error ->
                reportError(error)
                IntakeErrorLogger.logSurveillanceFormUpsertFailed(Exception(error.name), session.localId)
                return@runAsTransaction false
            }
            for ((questionId, answer) in currentState().formAnswersByQuestionId) {
                formAnswers.upsertFormAnswer(answer, session.localId, null, questionId).onError { error ->
                    reportError(error)
                    return@runAsTransaction false
                }
            }
            true
        }
    }
}

private fun findSelectedSite(snapshot: IntakeState): Site? {
    return if (snapshot.allSitesInProgram.any { !it.district.isNullOrBlank() }) {
        snapshot.allSitesInProgram.find {
            it.district == snapshot.selectedDistrict &&
                it.villageName == snapshot.selectedVillageName &&
                it.houseNumber == snapshot.selectedHouseNumber
        }
    } else {
        snapshot.allSitesInProgram.find { site ->
            val locationHierarchy = site.locationHierarchy ?: return@find false
            snapshot.siteSelectionsByLocationTypeId.all { (locationTypeId, selectedLocationTypeSite) ->
                val locationTypeName = snapshot.allLocationTypesInProgram.find { it.id == locationTypeId }?.name
                locationHierarchy[locationTypeName] == selectedLocationTypeSite
            }
        }
    }
}

private fun locationSelectionsToCache(state: IntakeState): Map<Int, String> {
    val allLocationTypes = state.allLocationTypesInProgram
    if (allLocationTypes.isEmpty()) return emptyMap()
    val lowestLevelId = allLocationTypes.last().id
    return state.siteSelectionsByLocationTypeId.filterKeys { it != lowestLevelId }
}

private data class IntakeFieldResults(
    val collector: Result<Unit, FormValidationError>,
    val district: Result<Unit, FormValidationError>,
    val villageName: Result<Unit, FormValidationError>,
    val houseNumber: Result<Unit, FormValidationError>,
    val locationTypeSiteSelections: Map<Int, FormValidationError?>,
    val llinType: Result<Unit, FormValidationError>?,
    val llinBrand: Result<Unit, FormValidationError>?,
    val collectionDate: Result<Unit, FormValidationError>,
    val collectionMethod: Result<Unit, FormValidationError>,
    val specimenCondition: Result<Unit, FormValidationError>,
    val numPeopleSleptInHouse: Result<Unit, FormValidationError>?,
    val monthsSinceIrs: Result<Unit, FormValidationError>?,
    val numLlinsAvailable: Result<Unit, FormValidationError>?,
    val numPeopleSleptUnderLlin: Result<Unit, FormValidationError>?,
    val numChildrenUnder5: Result<Unit, FormValidationError>?,
    val formAnswers: Map<Int, Result<Unit, FormValidationError>>,
) {
    fun hasError(): Boolean {
        val hasFieldError = listOf(
            collector,
            district,
            villageName,
            houseNumber,
            llinType,
            llinBrand,
            collectionDate,
            collectionMethod,
            specimenCondition,
            monthsSinceIrs,
            numLlinsAvailable,
            numPeopleSleptInHouse,
            numPeopleSleptUnderLlin,
            numChildrenUnder5,
        ).any { it is Result.Error }
        return hasFieldError ||
            locationTypeSiteSelections.values.any { it != null } ||
            formAnswers.values.any { it is Result.Error }
    }

    fun toErrors(): IntakeErrors {
        return IntakeErrors(
            collector = collector.errorOrNull(),
            district = district.errorOrNull(),
            villageName = villageName.errorOrNull(),
            houseNumber = houseNumber.errorOrNull(),
            llinType = llinType?.errorOrNull(),
            llinBrand = llinBrand?.errorOrNull(),
            collectionDate = collectionDate.errorOrNull(),
            collectionMethod = collectionMethod.errorOrNull(),
            specimenCondition = specimenCondition.errorOrNull(),
            monthsSinceIrs = monthsSinceIrs?.errorOrNull(),
            numLlinsAvailable = numLlinsAvailable?.errorOrNull(),
            numPeopleSleptUnderLlin = numPeopleSleptUnderLlin?.errorOrNull(),
            numPeopleSleptInHouse = numPeopleSleptInHouse?.errorOrNull(),
            numChildrenUnder5 = numChildrenUnder5?.errorOrNull(),
            locationTypeSiteSelections = locationTypeSiteSelections,
            formAnswerErrors = formAnswers.mapValues { (_, result) -> result.errorOrNull() },
        )
    }
}
