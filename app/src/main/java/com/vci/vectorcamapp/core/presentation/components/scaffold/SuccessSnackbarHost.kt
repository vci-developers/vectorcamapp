package com.vci.vectorcamapp.core.presentation.components.scaffold

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.util.ObserveAsEvents
import com.vci.vectorcamapp.core.presentation.util.success.LocalSuccessMessageEmitter
import com.vci.vectorcamapp.core.presentation.util.success.toString
import com.vci.vectorcamapp.ui.extensions.colors
import kotlinx.coroutines.launch

@Composable
fun SuccessSnackbarHost(
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val successMessageEmitter = LocalSuccessMessageEmitter.current
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(successMessageEmitter.messages) { successData ->
        coroutineScope.launch {
            successMessageEmitter.clearLastMessage()
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = successData.message.toString(context),
                duration = successData.duration
            )
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            FeedbackSnackbar(
                message = snackbarData.visuals.message,
                containerColor = MaterialTheme.colors.accent,
                contentColor = MaterialTheme.colors.successConfirm,
                icon = painterResource(R.drawable.ic_complete),
                iconContentDescription = stringResource(R.string.core_content_description_success),
                onDismiss = {
                    snackbarData.dismiss()
                    successMessageEmitter.clearLastMessage()
                }
            )
        }
    )
}
