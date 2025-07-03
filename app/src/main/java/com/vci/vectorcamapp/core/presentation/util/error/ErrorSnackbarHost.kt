package com.vci.vectorcamapp.core.presentation.util.error

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun ErrorSnackbarHost(
    modifier: Modifier = Modifier,
    errorFlow: SharedFlow<ErrorMessage> = LocalErrorMessageFlow.current
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorFlow) {
        errorFlow.collect { error ->
            snackbarHostState.showSnackbar(
                message = error.message,
                duration = error.duration
            )
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            Snackbar(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .widthIn(max = 420.dp)
            ) {
                Text(text = snackbarData.visuals.message)
            }
        }
    )
}
