package com.yaddashty.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yaddashty.app.data.YaddashtyApp
import com.yaddashty.app.ui.NotesViewModel
import com.yaddashty.app.ui.NotesViewModelFactory
import com.yaddashty.app.ui.screens.EditorScreen
import com.yaddashty.app.ui.screens.NoteMode
import com.yaddashty.app.ui.screens.NotesListScreen
import com.yaddashty.app.ui.screens.SettingsScreen
import com.yaddashty.app.ui.theme.YaddashtyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val app = application as YaddashtyApp
            val notesViewModel: NotesViewModel = viewModel(
                factory = NotesViewModelFactory(app),
            )
            val themeMode by notesViewModel.themeMode.collectAsStateWithLifecycle()

            YaddashtyTheme(themeMode = themeMode) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        YaddashtyRoot(notesViewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun YaddashtyRoot(viewModel: NotesViewModel) {
    val navController = rememberNavController()
    val openSettings = { navController.navigate("settings") }

    NavHost(navController = navController, startDestination = "notes") {
        composable("notes") {
            NotesListScreen(
                viewModel = viewModel,
                mode = NoteMode.Active,
                onOpenNote = { id -> navController.navigate("editor/$id") },
                onNewNote = {
                    viewModel.createNote { id -> navController.navigate("editor/$id") }
                },
                onOpenArchive = { navController.navigate("archive") },
                onOpenTrash = { navController.navigate("trash") },
                onOpenSettings = openSettings,
            )
        }
        composable("archive") {
            NotesListScreen(
                viewModel = viewModel,
                mode = NoteMode.Archived,
                onOpenNote = { id -> navController.navigate("editor/$id") },
                onNewNote = {},
                onOpenArchive = {},
                onOpenTrash = { navController.navigate("trash") },
                onOpenSettings = openSettings,
                onBack = { navController.popBackStack() },
            )
        }
        composable("trash") {
            NotesListScreen(
                viewModel = viewModel,
                mode = NoteMode.Trash,
                onOpenNote = {},
                onNewNote = {},
                onOpenArchive = { navController.navigate("archive") },
                onOpenTrash = {},
                onOpenSettings = openSettings,
                onBack = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onOpenArchive = { navController.navigate("archive") },
                onOpenTrash = { navController.navigate("trash") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "editor/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
        ) { entry ->
            val noteId = entry.arguments?.getLong("noteId") ?: 0L
            EditorScreen(
                viewModel = viewModel,
                noteId = noteId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
