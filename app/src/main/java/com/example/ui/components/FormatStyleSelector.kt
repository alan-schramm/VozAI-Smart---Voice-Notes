package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FormatStyle
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

fun getFormatIcon(iconName: String): ImageVector {
    return when (iconName) {
        "AutoAwesome" -> Icons.Default.AutoAwesome
        "Mail" -> Icons.Default.Mail
        "FormatListBulleted" -> Icons.Default.FormatListBulleted
        "Share" -> Icons.Default.Share
        "CheckCircle" -> Icons.Default.CheckCircle
        "Groups" -> Icons.Default.Groups
        "Lightbulb" -> Icons.Default.Lightbulb
        else -> Icons.Default.AutoAwesome
    }
}

@Composable
fun FormatStyleSelector(
    selectedStyle: FormatStyle,
    onSelectStyle: (FormatStyle) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FormatStyle.entries.forEach { style ->
            val isSelected = style == selectedStyle
            FormatChip(
                style = style,
                isSelected = isSelected,
                compact = compact,
                onClick = { onSelectStyle(style) }
            )
        }
    }
}

@Composable
fun FormatChip(
    style: FormatStyle,
    isSelected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) ElectricBlueDark.copy(alpha = 0.45f) else CharcoalCard,
        animationSpec = tween(200),
        label = "chipBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ElectricBlue else CharcoalBorder,
        animationSpec = tween(200),
        label = "chipBorder"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) TextPrimary else TextSecondary,
        animationSpec = tween(200),
        label = "chipText"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 8.dp else 10.dp
            )
            .testTag("format_chip_${style.id}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getFormatIcon(style.iconName),
                contentDescription = null,
                tint = if (isSelected) ElectricBlue else TextSecondary,
                modifier = Modifier.size(if (compact) 16.dp else 18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = style.title,
                color = textColor,
                fontSize = if (compact) 13.sp else 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}
