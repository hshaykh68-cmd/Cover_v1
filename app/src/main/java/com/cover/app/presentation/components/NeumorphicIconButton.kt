package com.cover.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cover.app.core.theme.CyanGlow
import com.cover.app.core.theme.Surface15
import com.cover.app.core.theme.neumorphicButton

@Composable
fun NeumorphicIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = CyanGlow,
    backgroundColor: Color = Surface15,
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .neumorphicButton(
                onClick = onClick,
                shape = shape,
                elevation = elevation,
                backgroundColor = backgroundColor
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint
        )
    }
}
