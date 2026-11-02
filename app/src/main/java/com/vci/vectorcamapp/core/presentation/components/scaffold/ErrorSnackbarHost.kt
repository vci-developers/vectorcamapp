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
import com.vci.vectorcamapp.core.presentation.util.error.LocalErrorDataFlow
import com.vci.vectorcamapp.core.presentation.util.error.LocalErrorMessageEmitter
import com.vci.vectorcamapp.core.presentation.util.error.toString
import com.vci.vectorcamapp.ui.extensions.colors
import kotlinx.coroutines.launch

@Composable
fun ErrorSnackbarHost(
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val errorFlow = LocalErrorDataFlow.current
    val errorMessageEmitter = LocalErrorMessageEmitter.current
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(errorFlow) { errorData ->
        coroutineScope.launch {
            errorMessageEmitter.clearLastMessage()
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = errorData.error.toString(context),
                duration = errorData.duration
            )
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            FeedbackSnackbar(
                message = snackbarData.visuals.message,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colors.error,
                icon = painterResource(R.drawable.ic_error),
                iconContentDescription = stringResource(R.string.core_content_description_error),
                onDismiss = {
                    snackbarData.dismiss()
                    errorMessageEmitter.clearLastMessage()
                }
            )
        }
    )
}
