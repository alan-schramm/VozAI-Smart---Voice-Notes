package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FormatStyle
import com.example.data.model.NoteCategory
import com.example.ui.components.ExportNoteDialog
import com.example.ui.components.FormatStyleSelector
import com.example.ui.components.NoteChecklistCard
import com.example.ui.components.ReformatStudioDialog
import com.example.ui.components.SelectCategoryDialog
import com.example.ui.components.TtsPlayerBar
import com.example.ui.components.VoiceMemoPlayerCard
import com.example.ui.components.WebEnrichmentCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalDivider
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AudioPenViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    viewModel: AudioPenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeNote by viewModel.activeNote.collectAsState()
    val isReformatting by viewModel.isReformatting.collectAsState()
    val isSummarizingWithGemini by viewModel.isSummarizingWithGemini.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val currentlySpeakingId by viewModel.currentlySpeakingId.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val isGeneratingChecklist by viewModel.isGeneratingChecklist.collectAsState()
    val noteEnrichments by viewModel.noteEnrichments.collectAsState()
    val isEnrichingNote by viewModel.isEnrichingNote.collectAsState()

    val note = activeNote ?: return

    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember(note.title) { mutableStateOf(note.title) }

    var isEditingText by remember { mutableStateOf(false) }
    var editedText by remember(note.polishedText) { mutableStateOf(note.polishedText) }

    var showRawTranscript by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(true) }
    var showTakeaways by remember { mutableStateOf(true) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showManageTagsDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showReformatStudioDialog by remember { mutableStateOf(false) }
    val availableTags by viewModel.availableTags.collectAsState()

    val copyToClipboard: (String, String) -> Unit = { label, textToCopy ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    val dateFormatted = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(note.createdAt))
    val wordCount = note.polishedText.split("\\s+".toRegex()).count { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .statusBarsPadding()
    ) {
        // --- TOP NAVIGATION & ACTION BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.RECORD) },
                    modifier = Modifier.testTag("back_to_record_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Record",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Polished Memo",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pin / Unpin Button
                IconButton(
                    onClick = { viewModel.togglePinActiveNote() },
                    modifier = Modifier.testTag("top_pin_button")
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.isPinned) "Unpin note" else "Pin note",
                        tint = if (note.isPinned) ElectricBlueGlow else TextSecondary
                    )
                }

                // Share Intent Button
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.testTag("top_share_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export & Share",
                        tint = ElectricBlueGlow
                    )
                }

                // Favorite Button
                IconButton(
                    onClick = { viewModel.toggleFavoriteActiveNote() },
                    modifier = Modifier.testTag("toggle_favorite_button")
                ) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Favorite",
                        tint = if (note.isFavorite) AmberAccent else TextSecondary
                    )
                }

                // Archive / Unarchive Button
                IconButton(
                    onClick = { viewModel.toggleArchiveActiveNote() },
                    modifier = Modifier.testTag("toggle_archive_button")
                ) {
                    Icon(
                        imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = if (note.isArchived) "Unarchive note" else "Archive note",
                        tint = if (note.isArchived) EmeraldAccent else TextSecondary
                    )
                }

                // Delete Button
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.testTag("delete_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete note",
                        tint = TextSecondary
                    )
                }
            }
        }

        // --- SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Archived Status Banner
            if (note.isArchived) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(AmberAccent.copy(alpha = 0.5f), CharcoalBorder))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Note is archived in local storage",
                                color = AmberAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(
                            onClick = { viewModel.toggleArchiveActiveNote() }
                        ) {
                            Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                        }
                    }
                }
            }

            // Title with Edit Option
            if (!isEditingTitle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditingTitle = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = note.title,
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { isEditingTitle = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Title",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CharcoalCard,
                            unfocusedContainerColor = CharcoalCard
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.updateActiveNoteContent(editedTitle, note.polishedText)
                            isEditingTitle = false
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Title",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pinned Toggle Pill
                if (note.isPinned) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlueDark.copy(alpha = 0.4f))
                            .border(1.dp, ElectricBlueGlow.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.togglePinActiveNote() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("pinned_pill_toggle")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = ElectricBlueGlow,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pinned",
                                color = ElectricBlueGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Favorite Toggle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (note.isFavorite) AmberAccent.copy(alpha = 0.15f) else CharcoalCard)
                        .border(
                            1.dp,
                            if (note.isFavorite) AmberAccent.copy(alpha = 0.6f) else CharcoalBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.toggleFavoriteActiveNote() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("favorite_pill_toggle")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (note.isFavorite) "Favorited" else "Add to Favorites",
                            tint = if (note.isFavorite) AmberAccent else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (note.isFavorite) "Favorited" else "Favorite",
                            color = if (note.isFavorite) AmberAccent else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (note.isFavorite) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                // Category Pill (clickable to change)
                val categoryObj = NoteCategory.fromString(note.category)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalCard)
                        .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { showCategoryDialog = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("result_category_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${categoryObj.emoji} ${categoryObj.displayName}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Category",
                            tint = ElectricBlueGlow,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Style Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricBlueDark.copy(alpha = 0.4f))
                        .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = note.formatStyle.title,
                        color = ElectricBlueGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Duration Pill
                if (note.audioDurationSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CharcoalCard)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${note.audioDurationSeconds}s audio",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Word count Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalCard)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$wordCount words",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Date Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalCard)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dateFormatted,
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }

            // --- TAGS ROW ---
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val noteTags = note.getTagList()
                noteTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlueDark.copy(alpha = 0.5f))
                            .border(1.dp, ElectricBlue.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sell,
                                contentDescription = null,
                                tint = ElectricBlueGlow,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "#$tag",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove tag $tag",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable { viewModel.removeTagFromActiveNote(tag) }
                            )
                        }
                    }
                }

                // Add / Edit Tag Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalCard)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                        .clickable { showManageTagsDialog = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("manage_tags_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add tag",
                            tint = ElectricBlueGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (noteTags.isEmpty()) "Add Tags" else "Edit Tags",
                            color = ElectricBlueGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // --- GEMINI CONCISE SUMMARY CARD (TOP OF DETAIL VIEW) ---
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_concise_summary_card")
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                ElectricBlue.copy(alpha = 0.8f),
                                EmeraldAccent.copy(alpha = 0.6f)
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row with Title, Badge, and Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ElectricBlueGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "CONCISE SUMMARY",
                                        color = ElectricBlueGlow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ElectricBlueDark.copy(alpha = 0.6f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "GEMINI",
                                            color = TextPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "AI executive synthesis of selected note",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Action Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSummarizingWithGemini) {
                                CircularProgressIndicator(
                                    color = ElectricBlue,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(2.dp)
                                )
                            } else {
                                if (note.summary.isNotBlank()) {
                                    val isCurrentSpeaking = isSpeaking && currentlySpeakingId == "summary_${note.id}"
                                    IconButton(
                                        onClick = {
                                            if (isCurrentSpeaking) {
                                                viewModel.stopSpeaking()
                                            } else {
                                                viewModel.speakText(note.summary, "summary_${note.id}")
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("speak_summary_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                            contentDescription = if (isCurrentSpeaking) "Stop speaking" else "Read summary aloud",
                                            tint = if (isCurrentSpeaking) EmeraldAccent else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { copyToClipboard("Summary", note.summary) },
                                        modifier = Modifier.size(32.dp).testTag("copy_summary_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Summary",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.summarizeActiveNoteWithGemini() },
                                    modifier = Modifier.size(32.dp).testTag("summarize_with_gemini_button")
                                ) {
                                    Icon(
                                        imageVector = if (note.summary.isNotBlank()) Icons.Default.Refresh else Icons.Default.AutoAwesome,
                                        contentDescription = "Summarize with Gemini",
                                        tint = ElectricBlueGlow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isSummarizingWithGemini) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = ElectricBlue,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sending note to Gemini API for concise synthesis...",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (note.summary.isNotBlank()) {
                        Text(
                            text = note.summary,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal
                        )

                        if (note.keyTakeaways.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = CharcoalDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTakeaways = !showTakeaways },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "KEY TAKEAWAYS",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Icon(
                                    imageVector = if (showTakeaways) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showTakeaways,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    note.keyTakeaways.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "•",
                                                color = EmeraldAccent,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                text = item,
                                                color = TextSecondary,
                                                fontSize = 13.sp,
                                                lineHeight = 19.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "No summary generated yet for this note. Tap below to send this note's content to the Gemini API for an instant concise executive summary.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.summarizeActiveNoteWithGemini() },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("generate_gemini_summary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Summary with Gemini", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- ORIGINAL VOICE NOTE PLAYBACK (MEDIARECORDER AUDIO FILE) ---
            if (!note.audioFilePath.isNullOrBlank()) {
                VoiceMemoPlayerCard(
                    audioFilePath = note.audioFilePath,
                    audioDurationSeconds = note.audioDurationSeconds,
                    audioPlayerManager = viewModel.audioPlayerManager,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // --- AUDIO PLAYBACK (TEXT-TO-SPEECH) WITH SPEED CONTROLS ---
            val isFullNoteSpeaking = isSpeaking && currentlySpeakingId == "full_note_${note.id}"
            TtsPlayerBar(
                isSpeaking = isSpeaking,
                isThisNoteSpeaking = isFullNoteSpeaking,
                currentSpeed = speechRate,
                onTogglePlay = {
                    if (isFullNoteSpeaking) {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.speakText(note.polishedText, "full_note_${note.id}")
                    }
                },
                onSetSpeed = { newSpeed ->
                    viewModel.setSpeechRate(newSpeed)
                },
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // --- ACTION CHECKLIST & TASK EXTRACTION ---
            NoteChecklistCard(
                checklist = note.actionChecklist,
                isGenerating = isGeneratingChecklist,
                onGenerateChecklist = { viewModel.generateChecklistForActiveNote() },
                onToggleItem = { index -> viewModel.toggleChecklistItem(index) },
                onAddItem = { text -> viewModel.addChecklistItem(text) },
                onRemoveItem = { index -> viewModel.removeChecklistItem(index) },
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // --- INSTANT FORMAT SWITCHER CAROUSEL & AI STUDIO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSFORM FORMAT & TONE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                TextButton(
                    onClick = { showReformatStudioDialog = true },
                    modifier = Modifier.testTag("open_reformat_studio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricBlueGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Studio", fontSize = 12.sp, color = ElectricBlueGlow, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            FormatStyleSelector(
                selectedStyle = note.formatStyle,
                onSelectStyle = { targetStyle ->
                    if (targetStyle != note.formatStyle) {
                        viewModel.reformatActiveNote(targetStyle)
                    }
                },
                compact = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- GOOGLE SEARCH GROUNDING ENRICHMENT CARD ---
            WebEnrichmentCard(
                enrichmentResult = noteEnrichments[note.id],
                isLoading = isEnrichingNote && (viewModel.enrichingNoteId.value == note.id),
                onEnrichClick = { viewModel.enrichActiveNoteWithWebSearch() },
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Brainstorm with Agent card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.discussNoteWithAgent(note) },
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(ElectricBlue.copy(alpha = 0.5f), CharcoalBorder))
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Debater esta nota com o Agente de IA",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aprofunde o plano por voz ou texto e receba respostas faladas.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Re-formatting Loading Indicator
            if (isReformatting) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricBlue, EmeraldAccent)))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = ElectricBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Restructuring thoughts with Gemini...",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // --- MAIN POLISHED TEXT CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Card Top Bar with Copy and Edit triggers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "POLISHED RESULT",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { copyToClipboard("Polished Note", note.polishedText) },
                                modifier = Modifier.size(32.dp).testTag("copy_polished_text_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Polished Text",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { isEditingText = !isEditingText },
                                modifier = Modifier.size(32.dp).testTag("edit_polished_text_button")
                            ) {
                                Icon(
                                    imageVector = if (isEditingText) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = "Edit Text",
                                    tint = if (isEditingText) EmeraldAccent else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isEditingText) {
                        Text(
                            text = note.polishedText,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.2.sp
                        )
                    } else {
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            minLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = CharcoalBackground,
                                unfocusedContainerColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.updateActiveNoteContent(note.title, editedText)
                                isEditingText = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Changes", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- RAW VERBATIM TRANSCRIPT SECTION ---
            if (note.rawTranscript.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRawTranscript = !showRawTranscript },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Original Spoken Transcript",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = if (showRawTranscript) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = showRawTranscript,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = note.rawTranscript,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { copyToClipboard("Raw Transcript", note.rawTranscript) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Raw", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- STICKY BOTTOM ACTION BAR ---
        Surface(
            color = CharcoalSurface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy Action
                Button(
                    onClick = { copyToClipboard("Full AudioPen Note", note.polishedText) },
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalCard, contentColor = TextPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("bottom_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Share Action
                Button(
                    onClick = { showExportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = TextPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("bottom_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // AI Agent Brainstorm
                IconButton(
                    onClick = { viewModel.discussNoteWithAgent(note) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalCard)
                        .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .testTag("bottom_agent_brainstorm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "Discutir com Agente de IA",
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // AI Transformation Studio (Reformat, Translate, Repurpose)
                IconButton(
                    onClick = { showReformatStudioDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalCard)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                        .testTag("bottom_ai_studio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Studio",
                        tint = ElectricBlueGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Google Drive / Export File
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalCard)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                        .testTag("bottom_drive_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMove,
                        contentDescription = "Save to Google Drive / Files",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Custom AI Prompt Rewrite
                IconButton(
                    onClick = { viewModel.setCustomPromptDialogVisible(true) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CharcoalCard)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                        .testTag("bottom_custom_prompt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Custom AI Prompt",
                        tint = ElectricBlueGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = CharcoalCard,
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = CrimsonAccent,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (note.isArchived) "Delete Archived Note?" else "Delete Voice Note?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${note.title}\"? This action will remove it from local Room storage.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteNote(note.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // --- MANAGE TAGS DIALOG ---
    if (showManageTagsDialog) {
        var newTagInput by remember { mutableStateOf("") }
        var currentTags by remember(note.tags) { mutableStateOf(note.getTagList().toMutableList()) }

        AlertDialog(
            onDismissRequest = { showManageTagsDialog = false },
            containerColor = CharcoalCard,
            icon = {
                Icon(
                    imageVector = Icons.Default.Sell,
                    contentDescription = null,
                    tint = ElectricBlueGlow,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Manage Note Tags",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tags help you organize and quickly filter your notes in history.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    // Active tags list
                    if (currentTags.isNotEmpty()) {
                        Text(
                            text = "ASSIGNED TAGS:",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricBlueDark.copy(alpha = 0.6f))
                                        .border(1.dp, ElectricBlue, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#$tag",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove tag $tag",
                                            tint = TextSecondary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    currentTags = currentTags
                                                        .filter { it != tag }
                                                        .toMutableList()
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            placeholder = { Text("Add tag name...", fontSize = 13.sp, color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = CharcoalSurface,
                                unfocusedContainerColor = CharcoalSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("result_add_tag_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val clean = newTagInput.trim().replace("#", "").replace(",", "")
                                if (clean.isNotBlank() && !currentTags.any { it.equals(clean, ignoreCase = true) }) {
                                    currentTags = (currentTags + clean).toMutableList()
                                    newTagInput = ""
                                }
                            },
                            enabled = newTagInput.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (newTagInput.isNotBlank()) ElectricBlue else CharcoalBorder)
                                .testTag("result_add_tag_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add tag",
                                tint = if (newTagInput.isNotBlank()) TextPrimary else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Suggestions from other notes
                    val suggestions = availableTags.filter { t ->
                        !currentTags.any { it.equals(t, ignoreCase = true) }
                    }
                    if (suggestions.isNotEmpty()) {
                        Text(
                            text = "EXISTING TAGS:",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            suggestions.take(8).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CharcoalSurfaceVariant)
                                        .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            currentTags = (currentTags + tag).toMutableList()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "+ #$tag",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalString = currentTags.joinToString(",")
                        viewModel.updateActiveNoteTags(finalString)
                        showManageTagsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Tags", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showManageTagsDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // --- CATEGORY SELECTION DIALOG ---
    if (showCategoryDialog) {
        SelectCategoryDialog(
            currentCategory = note.category,
            noteTitle = note.title,
            onDismiss = { showCategoryDialog = false },
            onSelectCategory = { newCategory ->
                viewModel.updateActiveNoteCategory(newCategory)
                showCategoryDialog = false
            }
        )
    }

    // --- EXPORT & SHARE NOTE DIALOG ---
    if (showExportDialog) {
        ExportNoteDialog(
            noteTitle = note.title,
            onDismiss = { showExportDialog = false },
            onExportPdf = { viewModel.shareNoteAsPdf(context, note) },
            onExportPlainTextFile = { viewModel.shareNoteAsPlainTextFile(context, note) },
            onExportMarkdownFile = { viewModel.shareNoteAsMarkdownFile(context, note) },
            onSharePdf = { viewModel.shareNoteAsPdf(context, note) },
            onShareMarkdown = { viewModel.shareNoteFormatted(context, note, asMarkdown = true) },
            onSharePlainText = { viewModel.shareNoteFormatted(context, note, asMarkdown = false) },
            onCopyMarkdown = { copyToClipboard("Markdown Note", viewModel.formatNoteAsMarkdown(note)) },
            onCopyPlainText = { copyToClipboard("Clean Note", viewModel.formatNoteAsPlainText(note)) }
        )
    }

    // --- AI TRANSFORMATION STUDIO DIALOG ---
    if (showReformatStudioDialog) {
        ReformatStudioDialog(
            currentStyle = note.formatStyle,
            isProcessing = isReformatting,
            onDismiss = { showReformatStudioDialog = false },
            onReformatStyle = { targetStyle ->
                viewModel.reformatActiveNote(targetStyle)
            },
            onTranslate = { targetLanguage ->
                viewModel.translateActiveNote(targetLanguage)
            },
            onRepurpose = { targetPurpose ->
                viewModel.repurposeActiveNote(targetPurpose)
            }
        )
    }
}
