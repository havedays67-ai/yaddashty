@file:OptIn(ExperimentalMaterial3Api::class)

package com.yaddashty.app.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaddashty.app.ui.NotesViewModel
import com.yaddashty.app.ui.theme.NoteAccents
import com.yaddashty.app.ui.toPersianDigits
import com.yaddashty.app.ui.wordCount
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(
    viewModel: NotesViewModel,
    noteId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val noteFlow = remember(noteId) { viewModel.observeNote(noteId) }
    val note by noteFlow.collectAsStateWithLifecycle(initialValue = null)

    var loaded by remember(noteId) { mutableStateOf(false) }
    var title by remember(noteId) { mutableStateOf("") }
    var body by remember(noteId) { mutableStateOf("") }
    var colorIndex by remember(noteId) { mutableIntStateOf(0) }
    var tags by remember(noteId) { mutableStateOf("") }
    var pinned by remember(noteId) { mutableStateOf(false) }
    var dirty by remember(noteId) { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var startedBlank by remember(noteId) { mutableStateOf(false) }
    val editHistory = remember(noteId) { mutableStateListOf<Triple<String, String, String>>() }
    var lastHistoryPush by remember(noteId) { mutableStateOf(0L) }

    /** Snapshots the three text fields, coalescing rapid keystrokes. */
    fun pushSnapshot() {
        val snapshot = Triple(title, body, tags)
        val now = System.currentTimeMillis()
        if (editHistory.lastOrNull() != snapshot && now - lastHistoryPush > 600) {
            editHistory.add(snapshot)
            lastHistoryPush = now
            if (editHistory.size > 40) editHistory.removeAt(0)
        }
    }

    fun undoEdit() {
        val snapshot = editHistory.removeLastOrNull() ?: return
        title = snapshot.first
        body = snapshot.second
        tags = snapshot.third
        dirty = true
        viewModel.saveEdited(noteId, title, body, colorIndex, tags, pinned)
    }

    LaunchedEffect(note) {
        val current = note ?: return@LaunchedEffect
        if (!loaded) {
            startedBlank = current.title.isBlank() && current.body.isBlank() && current.tags.isBlank()
            title = current.title
            body = current.body
            colorIndex = current.color
            tags = current.tags
            pinned = current.pinned
            loaded = true
        }
    }

    fun persist() {
        if (!loaded) return
        viewModel.saveEdited(noteId, title, body, colorIndex, tags, pinned)
        dirty = false
    }

    LaunchedEffect(title, body, colorIndex, tags, pinned) {
        if (!loaded || !dirty) return@LaunchedEffect
        delay(900)
        viewModel.saveEdited(noteId, title, body, colorIndex, tags, pinned)
        dirty = false
    }

    // Last safety net for the debounce window: if the editor disappears (back
    // gesture, process death, navigation) the freshest draft is still written.
    val pendingDraft by rememberUpdatedState(
        EditorDraft(title, body, colorIndex, tags, pinned, loaded && dirty),
    )
    DisposableEffect(noteId) {
        onDispose {
            val draft = pendingDraft
            if (draft.dirty) {
                viewModel.saveEdited(
                    id = noteId,
                    title = draft.title,
                    body = draft.body,
                    color = draft.color,
                    tags = draft.tags,
                    pinned = draft.pinned,
                )
            }
        }
    }

    fun leaveEditor() {
        if (startedBlank && title.isBlank() && body.isBlank() && tags.isBlank()) {
            // An untouched "new note" draft should not pollute the notebook.
            viewModel.discardBlankNote(noteId)
        } else {
            persist()
        }
        onBack()
    }

    BackHandler { leaveEditor() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ویرایش یادداشت",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { leaveEditor() }) {
                        Icon(Icons.Outlined.Close, contentDescription = "بستن")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (!loaded) return@IconButton
                        pinned = !pinned
                        dirty = true
                        viewModel.saveEdited(noteId, title, body, colorIndex, tags, pinned)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = if (pinned) "برداشتن سنجاق" else "سنجاق کردن",
                            tint = if (pinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(onClick = {
                        if (!loaded) return@IconButton
                        val shareText = buildString {
                            append(title.ifBlank { "یادداشت" })
                            if (body.isNotBlank()) append("\n\n").append(body)
                            if (tags.isNotBlank()) append("\n\n").append(tags)
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری یادداشت"))
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "اشتراک‌گذاری")
                    }
                    IconButton(onClick = {
                        val current = note
                        if (loaded && current != null) {
                            viewModel.saveAndArchive(
                                id = noteId,
                                title = title,
                                body = body,
                                color = colorIndex,
                                tags = tags,
                                pinned = pinned,
                                archived = !current.archived,
                            ) { onBack() }
                        }
                    }) {
                        Icon(Icons.Outlined.Archive, contentDescription = "بایگانی")
                    }
                    IconButton(onClick = {
                        if (loaded) showDeleteDialog = true
                    }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "حذف")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoteAccents.forEachIndexed { index, accent ->
                    ColorDot(
                        color = accent,
                        selected = colorIndex == index,
                        onClick = {
                            colorIndex = index
                            dirty = true
                        },
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    pushSnapshot()
                    title = it
                    dirty = true
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("عنوان یادداشت") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = tags,
                onValueChange = {
                    pushSnapshot()
                    tags = it
                    dirty = true
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("برچسب‌ها؛ با ویرگول جدا کنید: کار، ایده") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = body,
                onValueChange = {
                    pushSnapshot()
                    body = it
                    dirty = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("اینجا بنویسید…") },
                shape = RoundedCornerShape(16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${toPersianDigits(wordCount(body).toString())} کلمه · ${toPersianDigits(body.length.toString())} نویسه",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { undoEdit() },
                    enabled = editHistory.isNotEmpty(),
                ) { Text("واگرد") }
                TextButton(
                    onClick = {
                        pushSnapshot()
                        dirty = true
                        persist()
                    },
                ) { Text("ذخیره") }
                TextButton(onClick = { leaveEditor() }) { Text("بازگشت") }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("انتقال به سطل زباله؟") },
            text = { Text("یادداشت به سطل زباله می‌رود و تا خالی نکردن سطل قابل بازگردانی است.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    if (loaded && note != null) {
                        viewModel.saveAndTrash(noteId, title, body, colorIndex, tags) { onBack() }
                    } else {
                        onBack()
                    }
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("انصراف") }
            },
        )
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fill = when {
        color == Color.Transparent -> MaterialTheme.colorScheme.surface
        isDark -> color.copy(alpha = 0.55f)
        else -> color
    }
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(fill)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (color == Color.Transparent) {
            Text(
                text = "×",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Snapshot of the editor fields used to flush the pending draft on dispose. */
private data class EditorDraft(
    val title: String,
    val body: String,
    val color: Int,
    val tags: String,
    val pinned: Boolean,
    val dirty: Boolean,
)
