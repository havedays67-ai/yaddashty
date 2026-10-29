@file:OptIn(ExperimentalMaterial3Api::class)

package com.yaddashty.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaddashty.app.data.Note
import com.yaddashty.app.data.displayTitle
import com.yaddashty.app.data.preview
import com.yaddashty.app.data.tagList
import com.yaddashty.app.ui.NotesViewModel
import com.yaddashty.app.ui.formatRelative
import com.yaddashty.app.ui.theme.NoteAccents
import com.yaddashty.app.ui.searchKey
import com.yaddashty.app.ui.toPersianDigits

enum class NoteMode(val screenTitle: String) {
    Active("یادداشت‌های من"),
    Archived("بایگانی"),
    Trash("سطل زباله"),
}

@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    mode: NoteMode,
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val activeNotesState = viewModel.activeNotes
    val archivedNotesState = viewModel.archivedNotes
    val trashedNotesState = viewModel.trashedNotes
    val activeCountState = viewModel.noteCount

    var query by rememberSaveable { mutableStateOf("") }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    val activeNotes by activeNotesState.collectAsStateWithLifecycle()
    val archivedNotes by archivedNotesState.collectAsStateWithLifecycle()
    val trashedNotes by trashedNotesState.collectAsStateWithLifecycle()
    val activeCount by activeCountState.collectAsStateWithLifecycle()

    val source = when (mode) {
        NoteMode.Active -> activeNotes
        NoteMode.Archived -> archivedNotes
        NoteMode.Trash -> trashedNotes
    }

    val queryKey = searchKey(query)
    val activeTag = selectedTag
    val visibleNotes = source.filter { note ->
        val matchesQuery = queryKey.isEmpty() ||
            searchKey(note.title).contains(queryKey) ||
            searchKey(note.body).contains(queryKey) ||
            searchKey(note.tags).contains(queryKey)
        val matchesTag = activeTag == null ||
            note.tagList().any { tag -> searchKey(tag) == searchKey(activeTag) }
        matchesQuery && matchesTag
    }

    val pinned = visibleNotes.filter { it.pinned }
    val others = visibleNotes.filterNot { it.pinned }

    val allTags = source.flatMap { it.tagList() }.distinct().sorted()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = mode.screenTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "${toPersianDigits(visibleNotes.size.toString())} یادداشت",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "بازگشت",
                            )
                        }
                    }
                },
                actions = {
                    if (mode == NoteMode.Trash && source.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashDialog = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "خالی کردن سطل")
                        }
                    }
                    if (mode != NoteMode.Archived) {
                        IconButton(onClick = onOpenArchive) {
                            Icon(Icons.Outlined.Archive, contentDescription = "بایگانی")
                        }
                    }
                    if (mode != NoteMode.Trash) {
                        IconButton(onClick = onOpenTrash) {
                            Icon(Icons.Outlined.Delete, contentDescription = "سطل زباله")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "تنظیمات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (mode == NoteMode.Active) {
                ExtendedFloatingActionButton(
                    onClick = onNewNote,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("یادداشت تازه") },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (mode == NoteMode.Active && onBack == null) {
                item { WelcomeCard(activeCount = activeCount) }
            }

            if (mode != NoteMode.Trash) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("جستجو در یادداشت‌ها…") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }

            if (allTags.isNotEmpty() && mode != NoteMode.Trash) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { selectedTag = null },
                            label = { Text("همه") },
                        )
                        allTags.forEach { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = {
                                    selectedTag = if (selectedTag == tag) null else tag
                                },
                                label = { Text(tag) },
                            )
                        }
                    }
                }
            }

            if (visibleNotes.isEmpty()) {
                item {
                    EmptyState(
                        title = when (mode) {
                            NoteMode.Active -> "هنوز چیزی ننوشته‌اید"
                            NoteMode.Archived -> "بایگانی خالی است"
                            NoteMode.Trash -> "سطل زباله خالی است"
                        },
                        subtitle = when (mode) {
                            NoteMode.Active -> "اولین یادداشت‌تان را با دکمهٔ پایین بسازید."
                            NoteMode.Archived -> "یادداشت‌های بایگانی‌شده اینجا نگه داشته می‌شوند."
                            NoteMode.Trash -> "یادداشت‌های حذف‌شده تا خالی کردن سطل اینجا می‌مانند."
                        },
                    )
                }
            }

            if (pinned.isNotEmpty()) {
                item {
                    SectionLabel(text = "سنجاق‌شده")
                }
                items(items = pinned, key = { "pinned-${it.id}" }) { note ->
                    NoteCard(
                        note = note,
                        mode = mode,
                        onClick = { if (mode != NoteMode.Trash) onOpenNote(note.id) },
                        onPin = { viewModel.setPinned(note, !note.pinned) },
                        onArchive = { viewModel.setArchived(note, mode != NoteMode.Archived) },
                        onTrash = { viewModel.moveToTrash(note) },
                        onRestore = { viewModel.restore(note) },
                        onDeleteForever = { viewModel.deletePermanently(note) },
                        onDuplicate = { viewModel.duplicate(note) },
                    )
                }
            }

            if (others.isNotEmpty()) {
                if (pinned.isNotEmpty()) {
                    item { SectionLabel(text = "بقیه یادداشت‌ها") }
                }
                items(items = others, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        mode = mode,
                        onClick = { if (mode != NoteMode.Trash) onOpenNote(note.id) },
                        onPin = { viewModel.setPinned(note, !note.pinned) },
                        onArchive = { viewModel.setArchived(note, mode != NoteMode.Archived) },
                        onTrash = { viewModel.moveToTrash(note) },
                        onRestore = { viewModel.restore(note) },
                        onDeleteForever = { viewModel.deletePermanently(note) },
                        onDuplicate = { viewModel.duplicate(note) },
                    )
                }
            }
        }
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("خالی کردن سطل زباله؟") },
            text = { Text("همهٔ یادداشت‌های داخل سطل برای همیشه پاک می‌شوند و بازگشتی نیست.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showEmptyTrashDialog = false
                }) { Text("پاک کردن همه") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) { Text("انصراف") }
            },
        )
    }
}

@Composable
private fun WelcomeCard(activeCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "دفترچهٔ روزانهٔ شما",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (activeCount == 0) {
                        "امروز چه چیزی ارزش نوشتن دارد؟"
                    } else {
                        "${toPersianDigits(activeCount.toString())} یادداشت زنده، همه روی همین گوشی."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    mode: NoteMode,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onDuplicate: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = NoteAccents.getOrElse(note.color) { Color.Transparent }
    val container = when {
        note.color == 0 -> MaterialTheme.colorScheme.surface
        isDark -> accent.copy(alpha = 0.22f).compositeOver(MaterialTheme.colorScheme.surface)
        else -> accent.copy(alpha = 0.55f).compositeOver(MaterialTheme.colorScheme.surface)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = mode != NoteMode.Trash, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = "سنجاق‌شده",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = note.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "عملیات یادداشت",
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        when (mode) {
                            NoteMode.Trash -> {
                                DropdownMenuItem(
                                    text = { Text("بازگردانی") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.RestoreFromTrash, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onRestore()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("حذف برای همیشه") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteForever()
                                    },
                                )
                            }

                            NoteMode.Archived -> {
                                DropdownMenuItem(
                                    text = { Text("خروج از بایگانی") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Unarchive, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onArchive()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("حذف") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Delete, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onTrash()
                                    },
                                )
                            }

                            NoteMode.Active -> {
                                DropdownMenuItem(
                                    text = { Text(if (note.pinned) "برداشتن سنجاق" else "سنجاق به بالا") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.PushPin, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onPin()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("ساخت نسخهٔ کپی") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDuplicate()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("بایگانی") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Archive, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onArchive()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("انتقال به سطل زباله") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Delete, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onTrash()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (note.preview().isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.preview(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = formatRelative(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                note.tagList().take(3).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
