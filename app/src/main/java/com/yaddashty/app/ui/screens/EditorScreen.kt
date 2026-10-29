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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    LaunchedEffect(note) {
        val current = note ?: return@LaunchedEffect
        if (!loaded) {
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
    }

    LaunchedEffect(title, body, colorIndex, tags, pinned) {
        if (!loaded || !dirty) return@LaunchedEffect
        delay(900)
        viewModel.saveEdited(noteId, title, body, colorIndex, tags, pinned)
    }

    fun leaveEditor() {
        persist()
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
                        if (current != null) {
                            persist()
                            viewModel.setArchived(current, !current.archived)
                            onBack()
                        }
                    }) {
                        Icon(Icons.Outlined.Archive, contentDescription = "بایگانی")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
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
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "ذخیرهٔ خودکار روشن است",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${toPersianDigits(wordCount(body).toString())} کلمه · ${toPersianDigits(body.length.toString())} نویسه",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    val current = note
                    if (current != null) viewModel.moveToTrash(current)
                    onBack()
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
