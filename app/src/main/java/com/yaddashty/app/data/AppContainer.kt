package com.yaddashty.app.data

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "yaddashty-settings",
)

/** Small hand rolled dependency container: one database, one repository, one settings store. */
class AppContainer(context: Context) {
    private val database = YaddashtyDatabase.get(context)
    val noteRepository = NoteRepository(database.noteDao())
    val settingsRepository = SettingsRepository(context.applicationContext)
}

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<String> = context.settingsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[themeKey] ?: "system" }

    suspend fun setThemeMode(mode: String) {
        context.settingsDataStore.edit { prefs -> prefs[themeKey] = mode }
    }
}

class YaddashtyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
