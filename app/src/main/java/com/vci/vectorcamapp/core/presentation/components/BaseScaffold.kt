package com.vci.vectorcamapp.core.presentation.components

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessage
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageBus
import com.vci.vectorcamapp.core.presentation.util.error.ErrorSnackbarHost
import com.vci.vectorcamapp.core.presentation.util.error.LocalErrorMessageFlow
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun BaseScaffold(
    errorFlow: SharedFlow<ErrorMessage> = ErrorMessageBus.errors,
    content: @Composable (PaddingValues) -> Unit
) {
    CompositionLocalProvider(LocalErrorMessageFlow provides errorFlow) {
        Scaffold { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                content(innerPadding)

                ErrorSnackbarHost(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
        }
    }
}