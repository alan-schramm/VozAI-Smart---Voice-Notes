package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ExportNoteDialog(
    noteTitle: String,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onExportPlainTextFile: () -> Unit,
    onExportMarkdownFile: () -> Unit,
    onSharePdf: () -> Unit,
    onShareMarkdown: () -> Unit,
    onSharePlainText: () -> Unit,
    onCopyMarkdown: () -> Unit,
    onCopyPlainText: () -> Unit
) {
    val scrollState = rememberScrollState()

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
                    imageVector = Icons.Default.DriveFileMove,
                    contentDescription = null,
                    tint = ElectricBlueGlow,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Export & Share Note",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Export or share \"${noteTitle.take(30)}\" for external use:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "DOCUMENT EXPORTS",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                // PDF Export & Share Option
                ExportOptionItem(
                    icon = Icons.Default.PictureAsPdf,
                    iconTint = AmberAccent,
                    title = "Export as PDF (.pdf)",
                    subtitle = "Professional multi-page PDF with executive styling",
                    testTag = "export_pdf_button",
                    onClick = {
                        onExportPdf()
                        onDismiss()
                    }
                )

                // Plain Text File Option
                ExportOptionItem(
                    icon = Icons.Default.TextFields,
                    iconTint = ElectricBlueGlow,
                    title = "Export as Plain Text (.txt)",
                    subtitle = "Universal plain text file for any device or editor",
                    testTag = "export_txt_button",
                    onClick = {
                        onExportPlainTextFile()
                        onDismiss()
                    }
                )

                // Markdown File Option
                ExportOptionItem(
                    icon = Icons.Default.Description,
                    iconTint = EmeraldAccent,
                    title = "Export as Markdown (.md)",
                    subtitle = "Structured document for Obsidian, Notion, Drive",
                    testTag = "export_md_button",
                    onClick = {
                        onExportMarkdownFile()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "INSTANT SHARING & CLIPBOARD",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                // Share PDF directly
                ExportOptionItem(
                    icon = Icons.Default.Share,
                    iconTint = AmberAccent,
                    title = "Share PDF Document",
                    subtitle = "Send formatted PDF file via WhatsApp, Gmail, Slack",
                    testTag = "share_pdf_button",
                    onClick = {
                        onSharePdf()
                        onDismiss()
                    }
                )

                // Share as Plain Text
                ExportOptionItem(
                    icon = Icons.Default.Share,
                    iconTint = TextSecondary,
                    title = "Share as Plain Text",
                    subtitle = "Clean text without markdown code symbols",
                    testTag = "share_plain_button",
                    onClick = {
                        onSharePlainText()
                        onDismiss()
                    }
                )

                // Copy Clean Text
                ExportOptionItem(
                    icon = Icons.Default.ContentCopy,
                    iconTint = ElectricBlue,
                    title = "Copy Note Text",
                    subtitle = "Copy clean text directly to clipboard",
                    testTag = "copy_plain_button",
                    onClick = {
                        onCopyPlainText()
                        onDismiss()
                    }
                )

                // Copy Markdown
                ExportOptionItem(
                    icon = Icons.Default.ContentCopy,
                    iconTint = ElectricBlueGlow,
                    title = "Copy Full Markdown",
                    subtitle = "Includes headings, summary, takeaways, and tasks",
                    testTag = "copy_md_button",
                    onClick = {
                        onCopyMarkdown()
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("close_export_dialog_button")
            ) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ExportOptionItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
