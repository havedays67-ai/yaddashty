package com.yaddashty.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yaddashty.app.data.Note
import com.yaddashty.app.data.NoteRepository
import com.yaddashty.app.data.SettingsRepository
import com.yaddashty.app.data.YaddashtyApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: NoteRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val activeNotes = repository.activeNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val archivedNotes = repository.archivedNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val trashedNotes = repository.trashedNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val noteCount = repository.activeCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0,
    )

    val themeMode = settings.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "system",
    )

    fun observeNote(id: Long): Flow<Note?> = repository.observeNote(id)

    fun createNote(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createNote()
            onCreated(id)
        }
    }

    fun saveEdited(
        id: Long,
        title: String,
        body: String,
        color: Int,
        tags: String,
        pinned: Boolean,
    ) {
        viewModelScope.launch {
            repository.saveEdited(id, title, body, color, tags, pinned)
        }
    }

    fun duplicate(note: Note) {
        viewModelScope.launch { repository.duplicate(note) }
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch { repository.setPinned(note.id, !note.pinned) }
    }

    fun setPinned(note: Note, pinned: Boolean) {
        viewModelScope.launch { repository.setPinned(note.id, pinned) }
    }

    fun setArchived(note: Note, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(note.id, archived) }
    }

    fun moveToTrash(note: Note) {
        viewModelScope.launch { repository.setTrashed(note.id, true) }
    }

    fun restore(note: Note) {
        viewModelScope.launch { repository.setTrashed(note.id, false) }
    }

    fun deletePermanently(note: Note) {
        viewModelScope.launch { repository.deletePermanently(note.id) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(repository.exportJson()) }
    }
}

class NotesViewModelFactory(private val app: YaddashtyApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return NotesViewModel(
            repository = app.container.noteRepository,
            settings = app.container.settingsRepository,
        ) as T
    }
}
