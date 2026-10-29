package com.yaddashty.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

/**
 * A single note. Tags are stored as a comma separated string because a note
 * only ever holds a small set of labels, which keeps the v1 schema simple.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String = "",
    val body: String = "",
    val color: Int = 0,
    val tags: String = "",
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

fun Note.tagList(): List<String> =
    tags.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

fun Note.preview(): String = body.replace('\n', ' ').trim()

fun Note.displayTitle(): String = title.trim().ifEmpty { "یادداشت بدون عنوان" }

fun normalizeTags(raw: String): String =
    raw.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(", ")

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE trashed = 1 ORDER BY updatedAt DESC")
    fun observeTrashed(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun findById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE trashed = 0 ORDER BY updatedAt DESC")
    suspend fun getAllForExport(): List<Note>

    @Query("SELECT COUNT(*) FROM notes WHERE trashed = 0")
    fun observeActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM notes WHERE trashed = 1")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long)

    @Query("UPDATE notes SET archived = :archived, pinned = 0, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long)

    @Query("UPDATE notes SET trashed = :trashed, pinned = 0, archived = 0, updatedAt = :now WHERE id = :id")
    suspend fun setTrashed(id: Long, trashed: Boolean, now: Long)
}

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class YaddashtyDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var instance: YaddashtyDatabase? = null

        fun get(context: android.content.Context): YaddashtyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    YaddashtyDatabase::class.java,
                    "yaddashty.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}

class NoteRepository(private val dao: NoteDao) {
    val activeNotes: Flow<List<Note>> = dao.observeActive()
    val archivedNotes: Flow<List<Note>> = dao.observeArchived()
    val trashedNotes: Flow<List<Note>> = dao.observeTrashed()
    val activeCount: Flow<Int> = dao.observeActiveCount()

    fun observeNote(id: Long): Flow<Note?> = dao.observeById(id)

    suspend fun createNote(): Long =
        dao.insert(Note())

    suspend fun saveEdited(
        id: Long,
        title: String,
        body: String,
        color: Int,
        tags: String,
        pinned: Boolean,
    ) {
        val existing = dao.findById(id) ?: return
        dao.update(
            existing.copy(
                title = title.trim(),
                body = body,
                color = color,
                tags = normalizeTags(tags),
                pinned = pinned,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Saves the editor content and changes the archive state in one database
     * write. Previously the two writes raced each other and archiving could be
     * silently reverted by a late autosave.
     */
    suspend fun saveAndArchive(
        id: Long,
        title: String,
        body: String,
        color: Int,
        tags: String,
        pinned: Boolean,
        archived: Boolean,
    ) {
        val existing = dao.findById(id) ?: return
        dao.update(
            existing.copy(
                title = title.trim(),
                body = body,
                color = color,
                tags = normalizeTags(tags),
                pinned = if (archived) false else pinned,
                archived = archived,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun saveAndTrash(
        id: Long,
        title: String,
        body: String,
        color: Int,
        tags: String,
    ) {
        val existing = dao.findById(id) ?: return
        dao.update(
            existing.copy(
                title = title.trim(),
                body = body,
                color = color,
                tags = normalizeTags(tags),
                pinned = false,
                archived = false,
                trashed = true,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Removes the empty draft created by the "new note" button when left untouched. */
    suspend fun discardIfBlank(id: Long) {
        val existing = dao.findById(id) ?: return
        if (existing.title.isBlank() && existing.body.isBlank() && existing.tags.isBlank()) {
            dao.deletePermanently(id)
        }
    }

    suspend fun duplicate(note: Note): Long = dao.insert(
        note.copy(
            id = 0L,
            title = if (note.title.isBlank()) "کپی یادداشت" else "${note.title} (کپی)",
            pinned = false,
            archived = false,
            trashed = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        ),
    )

    suspend fun setPinned(id: Long, pinned: Boolean) =
        dao.setPinned(id, pinned, System.currentTimeMillis())

    suspend fun setArchived(id: Long, archived: Boolean) =
        dao.setArchived(id, archived, System.currentTimeMillis())

    suspend fun setTrashed(id: Long, trashed: Boolean) =
        dao.setTrashed(id, trashed, System.currentTimeMillis())

    suspend fun deletePermanently(id: Long) = dao.deletePermanently(id)

    suspend fun emptyTrash() = dao.emptyTrash()

    suspend fun exportJson(): String {
        val items = dao.getAllForExport()
        val root = JSONObject()
        root.put("app", "Yaddashty")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val notes = JSONArray()
        items.forEach { note ->
            notes.put(
                JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("body", note.body)
                    .put("tags", note.tags)
                    .put("color", note.color)
                    .put("pinned", note.pinned)
                    .put("archived", note.archived)
                    .put("createdAt", note.createdAt)
                    .put("updatedAt", note.updatedAt),
            )
        }
        root.put("notes", notes)
        return root.toString(2)
    }
}
