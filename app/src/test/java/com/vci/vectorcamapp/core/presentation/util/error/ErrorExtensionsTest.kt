package com.vci.vectorcamapp.core.presentation.util.error

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
import com.vci.vectorcamapp.complete_session.details.domain.util.CompleteSessionDetailsError
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.domain.util.collector.CollectorValidationError
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import com.vci.vectorcamapp.incomplete_session.domain.util.IncompleteSessionError
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import com.vci.vectorcamapp.landing.domain.util.LandingError
import com.vci.vectorcamapp.main.domain.util.MainError
import com.vci.vectorcamapp.registration.domain.util.RegistrationError
import com.vci.vectorcamapp.settings.domain.util.SettingsError
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class ErrorExtensionsTest {

    private val context = mockk<Context> {
        every { getString(any<Int>()) } answers { firstArg<Int>().toString() }
    }

    @Test
    fun everyKnownError_mapsToItsStringResource() {
        expectedResources.forEach { (error, resId) ->
            assertThat(error.toString(context)).isEqualTo(resId.toString())
        }
    }

    @Test
    fun everyEnumConstant_hasAMapping() {
        val mapped = expectedResources.keys
        assertThat(mapped).containsAtLeastElementsIn(NetworkError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(RoomDbError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(CompleteSessionDetailsError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(ImagingError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(IncompleteSessionError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(MainError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(RegistrationError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(LandingError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(IntakeError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(FormValidationError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(CollectionBatchFormError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(CollectorValidationError.entries.toList())
        assertThat(mapped).containsAtLeastElementsIn(SettingsError.entries.toList())
    }

    @Test
    fun unknownErrorType_usesFallback() {
        val error = object : Error {}
        assertThat(error.toString(context)).isEqualTo(R.string.error_fallback.toString())
    }

    private val expectedResources: Map<Error, Int> = mapOf(
        NetworkError.REQUEST_TIMEOUT to R.string.network_error_request_timeout,
        NetworkError.TOO_MANY_REQUESTS to R.string.network_error_too_many_requests,
        NetworkError.NO_INTERNET to R.string.network_error_no_internet,
        NetworkError.SERVER_ERROR to R.string.network_error_server_error,
        NetworkError.SERIALIZATION_ERROR to R.string.network_error_serialization_error,
        NetworkError.SESSION_NOT_COMPLETED to R.string.network_error_session_not_completed,
        NetworkError.NOT_FOUND to R.string.network_error_not_found,
        NetworkError.CONFLICT to R.string.network_error_conflict,
        NetworkError.CLIENT_ERROR to R.string.network_error_client_error,
        NetworkError.UNKNOWN_ERROR to R.string.network_error_unknown_error,
        NetworkError.TUS_TRANSIENT_ERROR to R.string.network_error_tus_transient_error,
        NetworkError.TUS_PERMANENT_ERROR to R.string.network_error_tus_permanent_error,
        RoomDbError.CONSTRAINT_VIOLATION to R.string.roomdb_error_constraint_violation,
        RoomDbError.NO_ROWS_AFFECTED to R.string.roomdb_error_no_rows_affected,
        RoomDbError.UNKNOWN_ERROR to R.string.roomdb_error_unknown_error,
        CompleteSessionDetailsError.SESSION_NOT_FOUND to R.string.complete_session_error_session_not_found,
        CompleteSessionDetailsError.SITE_NOT_FOUND to R.string.complete_session_error_site_not_found,
        CompleteSessionDetailsError.SPECIMENS_NOT_FOUND to R.string.complete_session_error_specimens_not_found,
        CompleteSessionDetailsError.PROGRAM_NOT_FOUND to R.string.complete_session_error_program_not_found,
        CompleteSessionDetailsError.UNKNOWN_ERROR to R.string.complete_session_error_unknown_error,
        ImagingError.CAPTURE_ERROR to R.string.imaging_error_capture_error,
        ImagingError.SAVE_ERROR to R.string.imaging_error_save_error,
        ImagingError.PROCESSING_ERROR to R.string.imaging_error_processing_error,
        ImagingError.INVALID_SPECIMEN_ID to R.string.imaging_error_invalid_specimen_id,
        ImagingError.NO_SPECIMEN_FOUND to R.string.imaging_error_no_specimen_found,
        ImagingError.MULTIPLE_SPECIMENS_FOUND to R.string.imaging_error_multiple_specimens_found,
        ImagingError.NO_ACTIVE_SESSION to R.string.imaging_error_no_active_session,
        ImagingError.MODEL_INITIALIZATION_FAILED to R.string.imaging_error_model_initialization_failed,
        ImagingError.GPU_DELEGATE_INITIALIZATION_FAILED to R.string.imaging_error_gpu_delegate_initialization_failed,
        ImagingError.UNKNOWN_INITIALIZATION_ERROR to R.string.imaging_error_unknown_initialization_error,
        ImagingError.INVALID_INPUT_SHAPE to R.string.imaging_error_invalid_input_shape,
        ImagingError.SPECIMEN_ID_RECOGNITION_FAILED to R.string.imaging_error_specimen_id_recognition_failed,
        ImagingError.SPECIMEN_DETECTION_FAILED to R.string.imaging_error_specimen_detection_failed,
        ImagingError.SPECIMEN_ID_USED_IN_ANOTHER_COLLECTION_BATCH to
            R.string.imaging_error_specimen_id_used_in_another_collection_batch,
        ImagingError.UNKNOWN_INFERENCE_ERROR to R.string.imaging_error_unknown_inference_error,
        ImagingError.UNKNOWN_ERROR to R.string.imaging_error_unknown_error,
        IncompleteSessionError.SESSION_NOT_FOUND to R.string.incomplete_session_error_session_not_found,
        IncompleteSessionError.SESSION_RETRIEVAL_FAILED to R.string.incomplete_session_error_session_retrieval_failed,
        IncompleteSessionError.SESSION_DELETION_FAILED to R.string.incomplete_session_error_session_deletion_failed,
        IncompleteSessionError.UNKNOWN_ERROR to R.string.incomplete_session_error_unknown_error,
        MainError.DEVICE_FETCH_FAILED to R.string.main_error_device_fetch_failed,
        MainError.UNKNOWN_ERROR to R.string.main_error_unknown_error,
        RegistrationError.PROGRAM_NOT_FOUND to R.string.registration_error_program_not_found,
        RegistrationError.INVALID_PROGRAM_ACCESS_CODE to R.string.registration_error_invalid_program_access_code,
        RegistrationError.UNKNOWN_ERROR to R.string.registration_error_unknown_error,
        LandingError.PROGRAM_NOT_FOUND to R.string.landing_error_program_not_found,
        LandingError.SESSION_NOT_FOUND to R.string.landing_error_session_not_found,
        IntakeError.SITE_NOT_FOUND to R.string.intake_error_site_not_found,
        IntakeError.PROGRAM_NOT_FOUND to R.string.intake_error_missing_program_id,
        IntakeError.MISSING_COLLECTOR to R.string.intake_error_missing_collector,
        IntakeError.COLLECTOR_SAVE_FAILED to R.string.intake_error_collector_save_failed,
        IntakeError.LOCATION_PERMISSION_DENIED to R.string.intake_error_location_permission_denied,
        IntakeError.LOCATION_GPS_TIMEOUT to R.string.intake_error_location_gps_timeout,
        IntakeError.FORM_INVALID to R.string.intake_error_form_invalid,
        IntakeError.UNKNOWN_ERROR to R.string.intake_error_unknown_error,
        FormValidationError.BLANK_COLLECTOR to R.string.form_validation_error_blank_collector,
        FormValidationError.BLANK_DISTRICT to R.string.form_validation_error_blank_district,
        FormValidationError.BLANK_VILLAGE_NAME to R.string.form_validation_error_blank_village_name,
        FormValidationError.BLANK_HOUSE_NUMBER to R.string.form_validation_error_blank_house_number,
        FormValidationError.BLANK_LOCATION_TYPE_SELECTION to
            R.string.form_validation_error_blank_location_type_selection,
        FormValidationError.BLANK_LLIN_TYPE to R.string.form_validation_error_blank_llin_type,
        FormValidationError.BLANK_LLIN_BRAND to R.string.form_validation_error_blank_llin_brand,
        FormValidationError.FUTURE_COLLECTION_DATE to R.string.form_validation_error_future_collection_date,
        FormValidationError.BLANK_COLLECTION_METHOD to R.string.form_validation_error_blank_collection_method,
        FormValidationError.BLANK_SPECIMEN_CONDITION to R.string.form_validation_error_blank_specimen_condition,
        FormValidationError.INVALID_NUM_PEOPLE_SLEPT_UNDER_LLIN to
            R.string.form_validation_error_invalid_num_people_slept_under_llin_condition,
        FormValidationError.INVALID_NUM_PEOPLE_SLEPT_IN_HOUSE to
            R.string.form_validation_error_invalid_num_people_slept_in_house_condition,
        FormValidationError.INVALID_NUM_LLINS_AVAILABLE to
            R.string.form_validation_error_invalid_num_llins_available,
        FormValidationError.INVALID_MONTHS_SINCE_IRS to R.string.form_validation_error_invalid_months_since_irs,
        FormValidationError.INVALID_FORM_ANSWER to R.string.form_validation_error_invalid_form_answer,
        CollectionBatchFormError.FORM_INVALID to R.string.collection_batch_form_error_form_invalid,
        CollectionBatchFormError.INVALID_FORM_ANSWER to R.string.collection_batch_form_error_invalid_form_answer,
        CollectionBatchFormError.DUPLICATE_IDENTITY to R.string.collection_batch_form_error_duplicate_identity,
        CollectionBatchFormError.UNKNOWN_ERROR to R.string.collection_batch_form_error_unknown_error,
        CollectorValidationError.BLANK_COLLECTOR_TITLE to R.string.collector_validation_error_blank_collector_title,
        CollectorValidationError.BLANK_COLLECTOR_NAME to R.string.collector_validation_error_blank_collector_name,
        CollectorValidationError.INVALID_LAST_TRAINED_ON_DATE to
            R.string.collector_validation_error_invalid_collector_last_trained_on,
        SettingsError.COLLECTOR_SAVE_FAILED to R.string.settings_error_collector_save_failed,
        SettingsError.COLLECTOR_DELETION_FAILED to R.string.settings_error_collector_deletion_failed,
        SettingsError.DATA_SYNC_FAILED to R.string.settings_error_data_sync_failed,
        SettingsError.DATA_SYNC_IN_PROGRESS_SESSION_EXIST to
            R.string.settings_error_data_sync_in_progress_session_exist,
    )
}
