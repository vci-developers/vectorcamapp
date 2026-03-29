package com.vci.vectorcamapp.core.presentation.components.tile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.customShadow
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun ActionTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hue: Color = MaterialTheme.colors.cardGlow,
    fillWidth : Boolean = true,
    content: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colors.cardBackground),
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.dimensions.paddingMedium,
                vertical = MaterialTheme.dimensions.paddingSmall
            )
            .customShadow(
                color = hue.copy(alpha = 0.25f),
                offsetX = MaterialTheme.dimensions.shadowOffsetSmall,
                offsetY = MaterialTheme.dimensions.shadowOffsetSmall,
                cornerRadius = MaterialTheme.dimensions.cornerRadiusMedium,
                spread = MaterialTheme.dimensions.shadowSpreadSmall,
                blurRadius = MaterialTheme.dimensions.shadowBlurSmall,
            )
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
    ) {
        content()
    }
}
