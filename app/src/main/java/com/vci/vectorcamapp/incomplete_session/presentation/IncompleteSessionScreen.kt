package com.vci.vectorcamapp.incomplete_session.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import com.vci.vectorcamapp.incomplete_session.presentation.components.IncompleteSessionCard
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.LocalColors
import com.vci.vectorcamapp.ui.theme.screenHeightFraction

@Composable
fun IncompleteSessionScreen(
    state: IncompleteSessionState,
    onAction: (IncompleteSessionAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageHeaderHeight = 0.25f
    val pageBodyOffset = 0.2f
    
    Scaffold (
        modifier = modifier
    ){ systemBars ->
        Box(Modifier.fillMaxSize()) {

            PageHeader(
                title     = "Incomplete Sessions",
                subtitle  = "Click on session to resume",
                onBack    = { onAction(IncompleteSessionAction.NavigateBack) },
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(screenHeightFraction(pageHeaderHeight))
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = screenHeightFraction(pageBodyOffset))
                    .padding(horizontal = MaterialTheme.dimensions.paddingMedium)
                    .padding(systemBars),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
            ) {
                items(
                    items = state.sessions.asReversed(),
                    key   = { it.localId }
                ) { session ->
                    IncompleteSessionCard(
                        session  = session,
                        onClick  = { onAction(IncompleteSessionAction.ResumeSession(session.localId)) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(screenHeightFraction(pageBodyOffset)))
                }
            }
        }
    }
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current

    Column(
        modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(colors.headerGradientTopLeft, colors.headerGradientBottomRight)
                )
            )
            .padding(horizontal = MaterialTheme.dimensions.paddingMedium, vertical = MaterialTheme.dimensions.paddingMedium)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.height(MaterialTheme.dimensions.spacingMedium))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(MaterialTheme.dimensions.spacingMedium))
            Column {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}