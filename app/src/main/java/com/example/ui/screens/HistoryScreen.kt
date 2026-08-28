package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AudioNote
import com.example.data.model.FormatStyle
import com.example.data.model.NoteCategory
import com.example.ui.components.ExportNoteDialog
import com.example.ui.components.SelectCategoryDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AudioPenViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: AudioPenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.historyFilter.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val activeCount by viewModel.activeNotesCount.collectAsState()
    val archivedCount by viewModel.archivedNotesCount.collectAsState()
    val isBulkSelectionMode by viewModel.isBulkSelectionMode.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val currentPlayingAudioPath by viewModel.audioPlayerManager.currentPlayingPath.collectAsState()
    val audioPlayerState by viewModel.audioPlayerManager.playerState.collectAsState()

    var noteToDelete by remember { mutableStateOf<AudioNote?>(null) }
    var noteToExport by remember { mutableStateOf<AudioNote?>(null) }
    var noteToEditTags by remember { mutableStateOf<AudioNote?>(null) }
    var noteToEditCategory by remember { mutableStateOf<AudioNote?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showEmptyArchiveDialog by remember { mutableStateOf(false) }
    var showBulkCategoryDialog by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        "ALL" to if (activeCount > 0) "All Notes ($activeCount)" else "All Notes",
        "FAVORITES" to "Favorites",
        "ARCHIVED" to if (archivedCount > 0) "Archived ($archivedCount)" else "Archived",
        "classic" to "Classic",
        "email" to "Email",
        "bullet_memo" to "Bullet Memo",
        "social" to "Social",
        "todo" to "To-Do",
        "meeting_summary" to "Meeting",
        "brainstorm" to "Brainstorm"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .statusBarsPadding()
    ) {
        // --- TOP BAR (DUAL: BULK SELECTION MODE VS REGULAR) ---
        if (isBulkSelectionMode) {
            Surface(
                color = GraphiteElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.exitBulkSelection() },
                            modifier = Modifier.testTag("exit_bulk_selection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Selection",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedNoteIds.size} Selected",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Select / Deselect All
                        IconButton(
                            onClick = {
                                if (selectedNoteIds.size == filteredNotes.size) {
                                    viewModel.clearSelectedNotes()
                                } else {
                                    viewModel.selectAllNotes(filteredNotes.map { it.id })
                                }
                            },
                            modifier = Modifier.testTag("bulk_select_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = if (selectedNoteIds.size == filteredNotes.size) ElectricBlueGlow else TextSecondary
                            )
                        }

                        // Bulk Pin / Unpin
                        IconButton(
                            onClick = {
                                val anyPinned = filteredNotes.filter { it.id in selectedNoteIds }.any { it.isPinned }
                                viewModel.bulkTogglePin(selectedNoteIds.toList(), !anyPinned)
                            },
                            enabled = selectedNoteIds.isNotEmpty(),
                            modifier = Modifier.testTag("bulk_pin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin / Unpin Selected",
                                tint = if (selectedNoteIds.isNotEmpty()) AmberAccent else TextTertiary
                            )
                        }

                        // Bulk Category
                        IconButton(
                            onClick = { showBulkCategoryDialog = true },
                            enabled = selectedNoteIds.isNotEmpty(),
                            modifier = Modifier.testTag("bulk_category_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Change Category",
                                tint = if (selectedNoteIds.isNotEmpty()) ElectricBlueGlow else TextTertiary
                            )
                        }

                        // Bulk Archive / Unarchive
                        IconButton(
                            onClick = {
                                if (currentFilter == "ARCHIVED") {
                                    viewModel.bulkUnarchiveSelected()
                                } else {
                                    viewModel.bulkArchiveSelected()
                                }
                            },
                            enabled = selectedNoteIds.isNotEmpty(),
                            modifier = Modifier.testTag("bulk_archive_button")
                        ) {
                            Icon(
                                imageVector = if (currentFilter == "ARCHIVED") Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = "Archive / Unarchive Selected",
                                tint = if (selectedNoteIds.isNotEmpty()) EmeraldAccent else TextTertiary
                            )
                        }

                        // Bulk Delete
                        IconButton(
                            onClick = { showBulkDeleteDialog = true },
                            enabled = selectedNoteIds.isNotEmpty(),
                            modifier = Modifier.testTag("bulk_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = if (selectedNoteIds.isNotEmpty()) CrimsonAccent else TextTertiary
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.RECORD) },
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentFilter == "ARCHIVED") "Archived Notes" else "Saved Voice Notes",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Bulk select toggle button
                    if (filteredNotes.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.toggleBulkSelectionMode() },
                            modifier = Modifier.testTag("enter_bulk_selection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Select Multiple Notes",
                                tint = TextSecondary
                            )
                        }
                    }

                    // AI Agent CTA
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CharcoalCard)
                            .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.navigateTo(AppScreen.AGENT) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("agent_from_history")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "Idea Partner",
                                tint = ElectricBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Agent",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quick Record CTA Button
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.RECORD) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("new_recording_from_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Note", fontSize = 13.sp)
                    }
                }
            }
        }

        // --- SEARCH BAR ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = {
                Text(
                    text = if (currentFilter == "ARCHIVED") "Search archived notes..." else "Search title, content, or transcripts...",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = CharcoalBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = CharcoalCard,
                unfocusedContainerColor = CharcoalCard
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("history_search_input")
        )

        // --- CATEGORY FILTER ROW WITH DROPDOWN & CHIPS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Dropdown Button
            Box {
                val currentCategoryObj = NoteCategory.fromString(selectedCategory)
                val isCustomCategorySelected = !selectedCategory.equals("ALL", ignoreCase = true)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCustomCategorySelected) ElectricBlue.copy(alpha = 0.25f) else CharcoalCard)
                        .border(
                            1.dp,
                            if (isCustomCategorySelected) ElectricBlue else CharcoalBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { showCategoryDropdown = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("category_filter_dropdown_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Category",
                            tint = if (isCustomCategorySelected) ElectricBlueGlow else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isCustomCategorySelected) "${currentCategoryObj.emoji} ${currentCategoryObj.displayName}" else "Categories",
                            color = if (isCustomCategorySelected) ElectricBlueGlow else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isCustomCategorySelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Open Dropdown",
                            tint = if (isCustomCategorySelected) ElectricBlueGlow else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false },
                    modifier = Modifier.background(CharcoalCard)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "🏷️ All Categories",
                                color = if (selectedCategory.equals("ALL", ignoreCase = true)) ElectricBlueGlow else TextPrimary,
                                fontWeight = if (selectedCategory.equals("ALL", ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            viewModel.setSelectedCategory("ALL")
                            showCategoryDropdown = false
                        }
                    )
                    NoteCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${cat.emoji} ${cat.displayName}",
                                    color = if (selectedCategory.equals(cat.name, ignoreCase = true)) ElectricBlueGlow else TextPrimary,
                                    fontWeight = if (selectedCategory.equals(cat.name, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.setSelectedCategory(cat.name)
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            // Quick Category Pills
            val isAllCategoryActive = selectedCategory.equals("ALL", ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAllCategoryActive) ElectricBlueDark.copy(alpha = 0.6f) else CharcoalCard)
                    .border(
                        1.dp,
                        if (isAllCategoryActive) ElectricBlue else CharcoalBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { viewModel.setSelectedCategory("ALL") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("category_chip_all")
            ) {
                Text(
                    text = "All",
                    color = if (isAllCategoryActive) TextPrimary else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isAllCategoryActive) FontWeight.Bold else FontWeight.Normal
                )
            }

            NoteCategory.entries.forEach { cat ->
                val isCatActive = selectedCategory.equals(cat.name, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCatActive) ElectricBlue else CharcoalCard)
                        .border(
                            1.dp,
                            if (isCatActive) ElectricBlueGlow else CharcoalBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (isCatActive) {
                                viewModel.setSelectedCategory("ALL")
                            } else {
                                viewModel.setSelectedCategory(cat.name)
                            }
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("category_chip_${cat.name.lowercase()}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${cat.emoji} ${cat.displayName}",
                            color = if (isCatActive) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isCatActive) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isCatActive) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear category",
                                tint = TextPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- FILTER CHIPS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { (filterId, label) ->
                val isSelected = currentFilter.equals(filterId, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setHistoryFilter(filterId) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CharcoalCard,
                        labelColor = TextSecondary,
                        selectedContainerColor = if (filterId == "ARCHIVED") AmberAccent.copy(alpha = 0.2f) else ElectricBlueDark.copy(alpha = 0.5f),
                        selectedLabelColor = if (filterId == "ARCHIVED") AmberAccent else ElectricBlueGlow
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = CharcoalBorder,
                        selectedBorderColor = if (filterId == "ARCHIVED") AmberAccent else ElectricBlue
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // --- TAGS FILTER ROW ---
        if (availableTags.isNotEmpty() || selectedTag != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = "Tags",
                        tint = if (selectedTag != null) ElectricBlueGlow else TextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TAGS:",
                        color = if (selectedTag != null) ElectricBlueGlow else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // All tags pill
                val isAllTagsSelected = selectedTag == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAllTagsSelected) ElectricBlueDark.copy(alpha = 0.6f) else CharcoalCard)
                        .border(
                            1.dp,
                            if (isAllTagsSelected) ElectricBlue else CharcoalBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.clearSelectedTag() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("tag_filter_all")
                ) {
                    Text(
                        text = "All Tags",
                        color = if (isAllTagsSelected) TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isAllTagsSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                availableTags.forEach { tag ->
                    val isThisTagSelected = selectedTag.equals(tag, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isThisTagSelected) ElectricBlue else CharcoalCard)
                            .border(
                                1.dp,
                                if (isThisTagSelected) ElectricBlueGlow else CharcoalBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setSelectedTag(tag) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("tag_filter_$tag")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$tag",
                                color = if (isThisTagSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isThisTagSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isThisTagSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear tag filter",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SEARCH RESULTS FEEDBACK BAR ---
        if (searchQuery.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ElectricBlueGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (filteredNotes.size == 1) "1 note matching \"$searchQuery\"" else "${filteredNotes.size} notes matching \"$searchQuery\"",
                        color = ElectricBlueGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(
                    onClick = { viewModel.setSearchQuery("") },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Clear Search",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- ARCHIVE BANNER (WHEN IN ARCHIVED FILTER) ---
        if (currentFilter == "ARCHIVED") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(AmberAccent.copy(alpha = 0.4f), CharcoalBorder)
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stored locally in Room database. Notes here are hidden from your main notes list.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    if (filteredNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { showEmptyArchiveDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = CrimsonAccent)
                        ) {
                            Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- NOTES LIST / EMPTY STATE ---
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CharcoalSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentFilter == "ARCHIVED") Icons.Default.Inventory2 else Icons.Default.Mic,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when {
                            selectedTag != null -> "No notes tagged with \"#$selectedTag\""
                            searchQuery.isNotEmpty() -> "No matching notes found"
                            currentFilter == "ARCHIVED" -> "No archived notes"
                            currentFilter == "FAVORITES" -> "No favorited notes yet"
                            else -> "No saved voice notes yet"
                        },
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            selectedTag != null -> "Try clearing the tag filter or adding \"#$selectedTag\" to your voice memos."
                            searchQuery.isNotEmpty() -> "Try searching with different keywords"
                            currentFilter == "ARCHIVED" -> "Archive notes you want to keep preserved without cluttering your active list."
                            currentFilter == "FAVORITES" -> "Bookmark important notes to access them quickly here."
                            else -> "Record your messy stream of consciousness and let Gemini structure it into clean, executive prose."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    if (selectedTag != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.clearSelectedTag() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("clear_tag_filter_button")
                        ) {
                            Text("Clear Tag Filter")
                        }
                    } else if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.setSearchQuery("") },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Search")
                        }
                    } else if (currentFilter != "ARCHIVED") {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.RECORD) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_state_record_button")
                        ) {
                            Text("Record Your First Note")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredNotes,
                    key = { it.id }
                ) { note ->
                    val isPlayingThisAudio = !note.audioFilePath.isNullOrBlank() &&
                            currentPlayingAudioPath == note.audioFilePath &&
                            audioPlayerState == com.example.audio.PlayerState.PLAYING

                    HistoryNoteCard(
                        note = note,
                        activeTag = selectedTag,
                        isSelectionMode = isBulkSelectionMode,
                        isSelected = note.id in selectedNoteIds,
                        isPlayingAudio = isPlayingThisAudio,
                        onTogglePlayAudio = {
                            note.audioFilePath?.let { path ->
                                viewModel.toggleVoiceMemo(path)
                            }
                        },
                        onClick = {
                            if (isBulkSelectionMode) {
                                viewModel.toggleNoteSelection(note.id)
                            } else {
                                viewModel.openNoteDetail(note)
                            }
                        },
                        onLongClick = {
                            if (!isBulkSelectionMode) {
                                viewModel.startBulkSelection(note.id)
                            }
                        },
                        onToggleSelect = { viewModel.toggleNoteSelection(note.id) },
                        onSelectTag = { tag -> viewModel.setSelectedTag(tag) },
                        onEditTags = { noteToEditTags = note },
                        onEditCategory = { noteToEditCategory = note },
                        onShare = { noteToExport = note },
                        onDelete = { noteToDelete = note },
                        onToggleArchive = { viewModel.toggleArchive(note.id, note.isArchived) },
                        onToggleFavorite = { viewModel.toggleFavoriteNote(note.id, note.isFavorite) },
                        onTogglePin = { viewModel.togglePinNote(note.id, note.isPinned) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // --- BULK CATEGORY DIALOG ---
    if (showBulkCategoryDialog) {
        SelectCategoryDialog(
            currentCategory = "GENERAL",
            noteTitle = "${selectedNoteIds.size} Selected Notes",
            onDismiss = { showBulkCategoryDialog = false },
            onSelectCategory = { newCat ->
                viewModel.bulkSetCategory(selectedNoteIds.toList(), newCat)
                showBulkCategoryDialog = false
            }
        )
    }

    // --- BULK DELETE CONFIRMATION DIALOG ---
    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            containerColor = CharcoalCard,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = CrimsonAccent,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete ${selectedNoteIds.size} Notes?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete all ${selectedNoteIds.size} selected notes? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bulkDeleteSelected()
                        showBulkDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBulkDeleteDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // --- EDIT CATEGORY DIALOG ---
    noteToEditCategory?.let { note ->
        SelectCategoryDialog(
            currentCategory = note.category,
            noteTitle = note.title,
            onDismiss = { noteToEditCategory = null },
            onSelectCategory = { newCategory ->
                viewModel.updateNoteCategory(note.id, newCategory)
                noteToEditCategory = null
            }
        )
    }

    // --- EDIT TAGS DIALOG ---
    noteToEditTags?.let { note ->
        EditTagsDialog(
            note = note,
            availableTags = availableTags,
            onDismiss = { noteToEditTags = null },
            onSaveTags = { newTags ->
                viewModel.updateNoteTags(note.id, newTags)
                noteToEditTags = null
            }
        )
    }

    // --- DELETE SINGLE NOTE CONFIRMATION DIALOG ---
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
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
                    text = "Are you sure you want to permanently delete \"${note.title}\"? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(note.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { noteToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // --- EMPTY ARCHIVE CONFIRMATION DIALOG ---
    if (showEmptyArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyArchiveDialog = false },
            containerColor = CharcoalCard,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = CrimsonAccent,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Empty Entire Archive?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all $archivedCount archived notes from local Room storage. This cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllArchivedNotes()
                        showEmptyArchiveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Empty Archive", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEmptyArchiveDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // --- EXPORT NOTE DIALOG ---
    noteToExport?.let { note ->
        ExportNoteDialog(
            noteTitle = note.title,
            onDismiss = { noteToExport = null },
            onExportPdf = {
                viewModel.shareAudioNoteAsPdf(context, note)
            },
            onExportPlainTextFile = {
                val file = com.example.util.ExportManager.createPlainTextFile(
                    context,
                    note.title,
                    viewModel.exportNoteAsPlainText(note)
                )
                com.example.util.ExportManager.shareFile(context, file, "text/plain", note.title)
            },
            onExportMarkdownFile = {
                val file = com.example.util.ExportManager.createMarkdownFile(
                    context,
                    note.title,
                    viewModel.exportNoteAsMarkdown(note)
                )
                com.example.util.ExportManager.shareFile(context, file, "text/markdown", note.title)
            },
            onSharePdf = {
                viewModel.shareAudioNoteAsPdf(context, note)
            },
            onShareMarkdown = {
                val md = viewModel.exportNoteAsMarkdown(note)
                com.example.util.ExportManager.shareText(context, md, note.title)
            },
            onSharePlainText = {
                val txt = viewModel.exportNoteAsPlainText(note)
                com.example.util.ExportManager.shareText(context, txt, note.title)
            },
            onCopyMarkdown = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Markdown Note", viewModel.exportNoteAsMarkdown(note))
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied Markdown to clipboard", Toast.LENGTH_SHORT).show()
            },
            onCopyPlainText = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Clean Note", viewModel.exportNoteAsPlainText(note))
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied plain text to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryNoteCard(
    note: AudioNote,
    activeTag: String? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isPlayingAudio: Boolean = false,
    onTogglePlayAudio: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onSelectTag: (String) -> Unit,
    onEditTags: () -> Unit,
    onEditCategory: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleArchive: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit = {}
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(note.createdAt))
    val styleObj = FormatStyle.fromId(note.formatStyle)
    val categoryObj = NoteCategory.fromString(note.category)
    val tags = note.getTagList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                when {
                    isSelected -> ElectricBlue
                    note.isPinned -> AmberAccent.copy(alpha = 0.5f)
                    note.isArchived -> AmberAccent.copy(alpha = 0.3f)
                    else -> CharcoalBorder
                },
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ElectricBlueDark.copy(alpha = 0.3f) else CharcoalSurface
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Selection Checkbox in Bulk Mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ElectricBlue,
                        uncheckedColor = TextSecondary,
                        checkmarkColor = TextPrimary
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Header Row: Category, Style, Pinned & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Pinned Indicator Badge
                        if (note.isPinned) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberAccent.copy(alpha = 0.2f))
                                    .border(1.dp, AmberAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pinned",
                                        tint = AmberAccent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "PINNED",
                                        color = AmberAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Category badge (clickable to change)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CharcoalCard)
                                .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable(onClick = onEditCategory)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                .testTag("note_category_badge_${note.id}")
                        ) {
                            Text(
                                text = "${categoryObj.emoji} ${categoryObj.displayName}",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Style pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricBlueDark.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = styleObj.title,
                                color = ElectricBlueGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (note.isArchived) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Archived",
                                    color = AmberAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (note.audioDurationSeconds > 0) {
                            Text(
                                text = "• ${note.audioDurationSeconds}s",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Favorite",
                                tint = AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = dateStr,
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = note.title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Snippet
                Text(
                    text = note.polishedText,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Tags row (if note has tags)
                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            val isCurrentTagActive = activeTag.equals(tag, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrentTagActive) ElectricBlueDark.copy(alpha = 0.6f) else CharcoalCard)
                                    .border(
                                        1.dp,
                                        if (isCurrentTagActive) ElectricBlue else CharcoalBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectTag(tag) }
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    color = if (isCurrentTagActive) ElectricBlueGlow else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrentTagActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer metadata & quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${note.wordCount.coerceAtLeast(note.polishedText.split(" ").size)} words",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Original Voice Recording Play/Pause Button
                        if (!note.audioFilePath.isNullOrBlank()) {
                            IconButton(
                                onClick = onTogglePlayAudio,
                                modifier = Modifier.size(30.dp).testTag("play_audio_item_${note.id}")
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingAudio) "Pause voice recording" else "Play voice recording",
                                    tint = ElectricBlueGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Pin Toggle Button
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(30.dp).testTag("pin_item_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = if (note.isPinned) "Unpin note" else "Pin note to top",
                                tint = if (note.isPinned) AmberAccent else TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Category Edit Button
                        IconButton(
                            onClick = onEditCategory,
                            modifier = Modifier.size(30.dp).testTag("edit_category_item_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Change category",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Tag Edit Button
                        IconButton(
                            onClick = onEditTags,
                            modifier = Modifier.size(30.dp).testTag("edit_tags_item_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = "Edit tags",
                                tint = if (tags.isNotEmpty()) ElectricBlueGlow else TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Favorite Toggle Button
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(30.dp).testTag("favorite_item_${note.id}")
                        ) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (note.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (note.isFavorite) AmberAccent else TextTertiary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Archive / Unarchive Button
                        IconButton(
                            onClick = onToggleArchive,
                            modifier = Modifier.size(30.dp).testTag("archive_item_${note.id}")
                        ) {
                            Icon(
                                imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (note.isArchived) "Unarchive note" else "Archive note",
                                tint = if (note.isArchived) EmeraldAccent else TextTertiary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Share Button
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(30.dp).testTag("share_item_${note.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share note",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Delete Button
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(30.dp).testTag("delete_item_${note.id}")
                        ) {
                            Icon(
                                imageVector = if (note.isArchived) Icons.Default.DeleteForever else Icons.Default.Delete,
                                contentDescription = "Delete note",
                                tint = TextTertiary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTagsDialog(
    note: AudioNote,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onSaveTags: (String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    var currentTagList by remember(note.tags) {
        mutableStateOf(note.getTagList().toMutableList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                text = "Manage Tags",
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
                    text = "Add tags to categorize \"${note.title.take(30)}\" for easy searching and filtering.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )

                // Current Tags Flow
                if (currentTagList.isNotEmpty()) {
                    Text(
                        text = "ACTIVE TAGS:",
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
                        currentTagList.forEach { tag ->
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
                                        contentDescription = "Remove $tag",
                                        tint = TextSecondary,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                currentTagList = currentTagList
                                                    .filter { it != tag }
                                                    .toMutableList()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Tag Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = { Text("New tag name...", fontSize = 13.sp, color = TextMuted) },
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
                        modifier = Modifier.weight(1f).testTag("new_tag_text_field")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val clean = tagInput.trim().replace("#", "").replace(",", "")
                            if (clean.isNotBlank() && !currentTagList.any { it.equals(clean, ignoreCase = true) }) {
                                currentTagList = (currentTagList + clean).toMutableList()
                                tagInput = ""
                            }
                        },
                        enabled = tagInput.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (tagInput.isNotBlank()) ElectricBlue else CharcoalBorder)
                            .testTag("add_tag_confirm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add tag",
                            tint = if (tagInput.isNotBlank()) TextPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Suggested existing tags
                val unassignedTags = availableTags.filter { existing ->
                    !currentTagList.any { it.equals(existing, ignoreCase = true) }
                }
                if (unassignedTags.isNotEmpty()) {
                    Text(
                        text = "SUGGESTED TAGS:",
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
                        unassignedTags.take(8).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CharcoalSurfaceVariant)
                                    .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        currentTagList = (currentTagList + tag).toMutableList()
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
                    val finalTagString = currentTagList.joinToString(",")
                    onSaveTags(finalTagString)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Tags", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
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

