package com.vci.vectorcamapp.complete_session.details.presentation.components.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.composites.FormAnswerAndQuestion
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.extensions.displayText
import com.vci.vectorcamapp.ui.extensions.colors
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/* TODO: CLEANUP */
@Composable
fun CompleteSessionForm(
    session: Session,
    site: Site,
    surveillanceForm: SurveillanceForm?,
    form: Form?,
    sessionScopedFormAnswersAndQuestions: List<FormAnswerAndQuestion>,
    sessionUnits: List<SessionUnit>,
    sessionUnitAnswersAndQuestionsByUnitId: Map<UUID, List<FormAnswerAndQuestion>>,
    bucketNameBySessionUnitId: Map<UUID, String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val dateTimeFormatter =
        remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    session.completedAt?.let {
        Column(modifier = modifier.fillMaxSize()) {
            CompleteSessionFormTile(
                title = stringResource(R.string.complete_session_title_session_status),
                iconPainter = painterResource(R.drawable.ic_cloud_upload),
                iconDescription = stringResource(R.string.complete_session_content_description_cloud_upload)
            ) {
                Text(
                    text = stringResource(R.string.complete_session_label_created_at, dateTimeFormatter.format(session.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.textPrimary
                )

                Text(
                    text = stringResource(R.string.complete_session_label_completed_at, dateTimeFormatter.format(session.completedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.textPrimary
                )

                InfoPill(text = stringResource(R.string.complete_session_label_session_type, session.type.displayText(context)), color = MaterialTheme.colors.info)
            }

            CompleteSessionFormTile(
                title = stringResource(R.string.complete_session_title_general_info),
                iconPainter = painterResource(R.drawable.ic_info),
                iconDescription = stringResource(R.string.complete_session_content_description_information)
            ) {
                Text(
                    text = stringResource(R.string.complete_session_label_collector, session.collectorName, session.collectorTitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.textPrimary
                )

                Text(
                    text = stringResource(R.string.complete_session_label_hardware_id, session.hardwareId ?: stringResource(R.string.complete_session_label_not_provided)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.textPrimary
                )

                Text(
                    text = stringResource(R.string.complete_session_label_collection_date, dateFormatter.format(session.collectionDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.textPrimary
                )

                Text(
                    text = stringResource(R.string.complete_session_label_collection_method, session.collectionMethod),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.textPrimary
                )

                Text(
                    text = stringResource(R.string.complete_session_label_specimen_condition, session.specimenCondition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colors.textPrimary
                )
            }

            CompleteSessionFormTile(
                title = stringResource(R.string.complete_session_title_geographical_info),
                iconPainter = painterResource(R.drawable.ic_pin),
                iconDescription = stringResource(R.string.complete_session_content_description_pin)
            ) {
                site.district?.let { district ->
                    Text(
                        text = stringResource(R.string.complete_session_label_district, district),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                site.subCounty?.let { subCounty ->
                    Text(
                        text = stringResource(R.string.complete_session_label_sub_county, subCounty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                site.parish?.let { parish ->
                    Text(
                        text = stringResource(R.string.complete_session_label_parish, parish),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                site.villageName?.let { villageName ->
                    Text(
                        text = stringResource(R.string.complete_session_label_village_name, villageName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                site.houseNumber?.let { houseNumber ->
                    Text(
                        text = stringResource(R.string.complete_session_label_house_number, houseNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                site.healthCenter?.let { healthCenter ->
                    Text(
                        text = stringResource(R.string.complete_session_label_health_center, healthCenter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )
                }

                site.locationHierarchy
                    ?.filterValues { it.isNotBlank() }
                    ?.forEach { (key, value) ->
                        Text(
                            text = stringResource(R.string.complete_session_label_detail, key, value),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
            }

            form?.let { resolvedForm ->
                CompleteSessionFormTile(
                    title = resolvedForm.name,
                    iconPainter = painterResource(R.drawable.ic_clipboard),
                    iconDescription = stringResource(R.string.complete_session_content_description_clipboard),
                ) {
                    val visibleFormAnswersAndQuestions = sessionScopedFormAnswersAndQuestions
                        .filter { (formAnswer, _) -> formAnswer.value.isNotBlank() }

                    if (visibleFormAnswersAndQuestions.isNotEmpty()) {
                        visibleFormAnswersAndQuestions.forEach { (formAnswer, formQuestion) ->
                            val displayValue = when (formQuestion.type) {
                                "date" -> formAnswer.value.toLongOrNull()
                                    ?.let { dateFormatter.format(it) } ?: formAnswer.value
                                else -> formAnswer.value
                            }
                            Text(
                                text = stringResource(R.string.complete_session_label_detail, formQuestion.label, displayValue),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colors.textPrimary,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.complete_session_body_no_responses),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.textSecondary,
                        )
                    }
                }
            }

            surveillanceForm?.let {
                CompleteSessionFormTile(
                    title = stringResource(R.string.complete_session_title_surveillance_form),
                    iconPainter = painterResource(R.drawable.ic_clipboard),
                    iconDescription = stringResource(R.string.complete_session_content_description_clipboard)
                ) {
                    Text(
                        text = stringResource(R.string.complete_session_label_num_people_house, it.numPeopleSleptInHouse),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )

                    Text(
                        text = stringResource(R.string.complete_session_label_irs_conducted, if (it.wasIrsConducted) stringResource(R.string.complete_session_label_yes) else stringResource(R.string.complete_session_label_no)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )

                    it.monthsSinceIrs?.let { monthsSinceIrs ->
                        Text(
                            text = stringResource(R.string.complete_session_label_months_since_irs, monthsSinceIrs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }

                    Text(
                        text = stringResource(R.string.complete_session_label_num_llins, it.numLlinsAvailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )

                    it.llinType?.let { llinType ->
                        Text(
                            text = stringResource(R.string.complete_session_label_llin_type, llinType),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }

                    it.llinBrand?.let { llinBrand ->
                        Text(
                            text = stringResource(R.string.complete_session_label_llin_brand, llinBrand),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }

                    it.numPeopleSleptUnderLlin?.let { numPeopleSleptUnderLlin ->
                        Text(
                            text = stringResource(R.string.complete_session_label_num_people_llin, numPeopleSleptUnderLlin),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
                }
            }

            if (session.notes.isNotEmpty()) {
                CompleteSessionFormTile(
                    title = stringResource(R.string.complete_session_title_additional_notes),
                    iconPainter = painterResource(R.drawable.ic_notes),
                    iconDescription = stringResource(R.string.complete_session_content_description_notes)
                ) {
                    Text(
                        text = stringResource(R.string.complete_session_label_notes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )

                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textPrimary
                    )
                }
            }

            sessionUnits.forEach { sessionUnit ->
                val title = bucketNameBySessionUnitId[sessionUnit.localId] ?: stringResource(R.string.complete_session_label_batch, sessionUnit.unitOrder)

                val visibleFormAnswersAndQuestions =
                    sessionUnitAnswersAndQuestionsByUnitId[sessionUnit.localId]
                        .orEmpty()
                        .filter { (formAnswer, _) -> formAnswer.value.isNotBlank() }

                CompleteSessionFormTile(
                    title = title,
                    iconPainter = painterResource(R.drawable.ic_clipboard),
                    iconDescription = stringResource(R.string.complete_session_content_description_collection_batch),
                ) {
                    if (visibleFormAnswersAndQuestions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.complete_session_body_no_responses_batch),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.textSecondary,
                        )
                    } else {
                        visibleFormAnswersAndQuestions.forEach { (formAnswer, formQuestion) ->
                            val displayValue = when (formQuestion.type) {
                                "date" -> formAnswer.value.toLongOrNull()
                                    ?.let { dateFormatter.format(it) } ?: formAnswer.value
                                else -> formAnswer.value
                            }
                            Text(
                                text = stringResource(R.string.complete_session_label_detail, formQuestion.label, displayValue),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colors.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
