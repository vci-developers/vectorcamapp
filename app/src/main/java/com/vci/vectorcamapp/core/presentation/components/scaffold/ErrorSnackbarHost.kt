package com.vci.vectorcamapp.core.presentation.components.scaffold

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.util.ObserveAsEvents
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageBus
import com.vci.vectorcamapp.core.presentation.util.error.LocalErrorDataFlow
import com.vci.vectorcamapp.core.presentation.util.error.toString
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.screenWidthFraction
import kotlinx.coroutines.launch

@Composable
fun ErrorSnackbarHost(
    modifier: Modifier = Modifier
) {
    val SCREEN_WIDTH_FRACTION = 0.96f

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val errorFlow = LocalErrorDataFlow.current
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(errorFlow) { errorData ->
        coroutineScope.launch {
            ErrorMessageBus.clearLastMessage()
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
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall),
                shadowElevation = MaterialTheme.dimensions.shadowOffsetMedium,
                modifier = Modifier
                    .widthIn(max = screenWidthFraction(SCREEN_WIDTH_FRACTION))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.dimensions.paddingLarge,
                            vertical = MaterialTheme.dimensions.paddingMedium
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_error),
                        contentDescription = "Error",
                        tint = MaterialTheme.colors.error,
                        modifier = Modifier
                            .size(MaterialTheme.dimensions.iconSizeLarge)
                    )
                    Spacer(modifier = Modifier.widthIn(min = MaterialTheme.dimensions.spacingSmall))
                    Text(
                        text = snackbarData.visuals.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.widthIn(min = MaterialTheme.dimensions.spacingMedium))
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Dismiss Icon",
                        tint = MaterialTheme.colors.textSecondary,
                        modifier = Modifier
                            .size(MaterialTheme.dimensions.iconSizeExtraSmall)
                            .clickable {
                                snackbarData.dismiss()
                                ErrorMessageBus.clearLastMessage()
                            }
                    )
                }
            }
        }
    )
}
