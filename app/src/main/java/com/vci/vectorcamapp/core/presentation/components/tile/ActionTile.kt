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
import androidx.compose.ui.unit.Dp
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.customShadow
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun ActionTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shadowColor: Color = MaterialTheme.colors.cardGlow.copy(alpha = 0.2f),
    shadowOffsetX: Dp = MaterialTheme.dimensions.shadowOffsetSmall,
    shadowOffsetY: Dp  = MaterialTheme.dimensions.shadowOffsetSmall,
    shadowCornerRadius: Dp  = MaterialTheme.dimensions.cornerRadiusMedium,
    shadowSpread: Dp  = MaterialTheme.dimensions.shadowSpreadSmall,
    shadowBlurRadius: Dp  = MaterialTheme.dimensions.shadowBlurSmall,
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
                color = shadowColor,
                offsetX = shadowOffsetX,
                offsetY = shadowOffsetY,
                cornerRadius = shadowCornerRadius,
                spread = shadowSpread,
                blurRadius = shadowBlurRadius,
            )
            .fillMaxWidth()
    ) {
        content()
    }
}
