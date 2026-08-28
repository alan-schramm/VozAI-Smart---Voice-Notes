package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FormatStyle
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReformatStudioDialog(
    currentStyle: FormatStyle,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onReformatStyle: (FormatStyle) -> Unit,
    onTranslate: (String) -> Unit,
    onRepurpose: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Style", "Translate", "Repurpose")

    val languageOptions = listOf(
        "Português" to "🇧🇷",
        "English" to "🇺🇸",
        "Español" to "🇪🇸",
        "Français" to "🇫🇷",
        "Deutsch" to "🇩🇪",
        "Italiano" to "🇮🇹",
        "日本語 (Japanese)" to "🇯🇵",
        "中文 (Chinese)" to "🇨🇳"
    )

    val repurposeOptions = listOf(
        "LinkedIn Thought Leadership Post" to "💼",
        "Engaging Twitter/X Thread" to "🐦",
        "Executive Email Newsletter" to "📧",
        "Structured Meeting Minutes" to "📋",
        "Blog Post Draft with Headers" to "✍️",
        "Podcast / Video Talking Points" to "🎙️"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalCard,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ElectricBlueDark.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricBlueGlow,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "AI Transformation Studio",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Selection Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CharcoalSurface,
                    contentColor = ElectricBlueGlow,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricBlue
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) ElectricBlueGlow else TextSecondary
                                )
                            }
                        )
                    }
                }

                if (isProcessing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = ElectricBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Transforming with Gemini AI...",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    when (selectedTab) {
                        // TAB 0: FORMAT STYLES
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Restructure the narrative tone of this note:",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                FormatStyle.entries.forEach { style ->
                                    val isSelected = style == currentStyle
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onReformatStyle(style)
                                                onDismiss()
                                            }
                                            .border(
                                                1.dp,
                                                if (isSelected) ElectricBlue else CharcoalBorder,
                                                RoundedCornerShape(12.dp)
                                            ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) ElectricBlueDark.copy(alpha = 0.4f) else CharcoalSurface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = getFormatIcon(style.iconName),
                                                contentDescription = null,
                                                tint = if (isSelected) ElectricBlueGlow else TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = style.title,
                                                    color = if (isSelected) ElectricBlueGlow else TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = style.description,
                                                    color = TextTertiary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // TAB 1: TRANSLATION
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Translate this note seamlessly with context-aware Gemini translation:",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                languageOptions.forEach { (lang, flag) ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onTranslate(lang)
                                                onDismiss()
                                            }
                                            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = flag, fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = lang,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = ElectricBlueGlow,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // TAB 2: REPURPOSE
                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Repurpose this memo for social media, email, or executive reporting:",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                repurposeOptions.forEach { (purpose, emoji) ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onRepurpose(purpose)
                                                onDismiss()
                                            }
                                            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = emoji, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = purpose,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PublishedWithChanges,
                                                contentDescription = null,
                                                tint = EmeraldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
