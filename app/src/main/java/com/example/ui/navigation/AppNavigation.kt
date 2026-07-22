package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.local.MangaDatabase
import com.example.data.network.AnilistClient
import com.example.data.network.MangapillClient
import com.example.data.repository.MangaRepository
import com.example.ui.screens.detail.MangaDetailScreen
import com.example.ui.screens.detail.MangaDetailViewModel
import com.example.ui.screens.explore.ExploreScreen
import com.example.ui.screens.explore.ExploreViewModel
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.library.LibraryViewModel
import com.example.ui.screens.reader.ReaderScreen
import com.example.ui.screens.reader.ReaderViewModel
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.search.SearchViewModel
import com.example.ui.screens.settings.SettingsScreen

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current

    // Instantiate Singletons / Repository
    val database = remember { MangaDatabase.getDatabase(context) }
    val anilistClient = remember { AnilistClient() }
    val mangapillClient = remember { MangapillClient() }
    val repository = remember {
        MangaRepository(
            anilistClient = anilistClient,
            mangapillClient = mangapillClient,
            mangaDao = database.mangaDao()
        )
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Explore.route,
            label = "Explore",
            icon = { Icon(imageVector = Icons.Default.Explore, contentDescription = "Explore") },
            testTag = "nav_item_explore"
        ),
        BottomNavItem(
            route = Screen.Search.route,
            label = "Search",
            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            testTag = "nav_item_search"
        ),
        BottomNavItem(
            route = Screen.Library.route,
            label = "Library",
            icon = { Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Library") },
            testTag = "nav_item_library"
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            label = "Settings",
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
            testTag = "nav_item_settings"
        )
    )

    // Hide bottom bar in Reader screen and Detail screen
    val shouldShowBottomBar = currentRoute in listOf(
        Screen.Explore.route,
        Screen.Search.route,
        Screen.Library.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    if (item.route == Screen.Explore.route) {
                                        val popped = navController.popBackStack(Screen.Explore.route, inclusive = false)
                                        if (!popped) {
                                            navController.navigate(Screen.Explore.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    inclusive = true
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            },
                            icon = item.icon,
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Explore.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Explore Screen
            composable(Screen.Explore.route) {
                val exploreViewModel: ExploreViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ExploreViewModel(repository) as T
                        }
                    }
                )
                ExploreScreen(
                    viewModel = exploreViewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate(Screen.MangaDetail.createRoute(mangaId))
                    },
                    onSearchClick = {
                        if (currentRoute != Screen.Search.route) {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }

            // 2. Search Screen
            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return SearchViewModel(repository) as T
                        }
                    }
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate(Screen.MangaDetail.createRoute(mangaId))
                    }
                )
            }

            // 3. Library Screen
            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return LibraryViewModel(repository) as T
                        }
                    }
                )
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate(Screen.MangaDetail.createRoute(mangaId))
                    },
                    onResumeReaderClick = { mangaId, chapterId, chapterTitle ->
                        navController.navigate(Screen.Reader.createRoute(mangaId, chapterId, chapterTitle))
                    }
                )
            }

            // 4. Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    repository = repository,
                    onClearHistorySuccess = {}
                )
            }

            // 5. Manga Detail Screen
            composable(
                route = Screen.MangaDetail.route,
                arguments = listOf(navArgument("mangaId") { type = NavType.IntType })
            ) { backStackEntry ->
                val mangaId = backStackEntry.arguments?.getInt("mangaId") ?: 0
                val detailViewModel: MangaDetailViewModel = viewModel(
                    key = "detail_$mangaId",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MangaDetailViewModel(repository, mangaId) as T
                        }
                    }
                )
                MangaDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onChapterClick = { manga, chapter ->
                        navController.navigate(Screen.Reader.createRoute(manga.id, chapter.id, chapter.title))
                    }
                )
            }

            // 6. Reader Screen
            composable(
                route = Screen.Reader.route,
                arguments = listOf(
                    navArgument("mangaId") { type = NavType.IntType },
                    navArgument("chapterId") { type = NavType.StringType },
                    navArgument("chapterTitle") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mangaId = backStackEntry.arguments?.getInt("mangaId") ?: 0
                val encodedChapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                val encodedChapterTitle = backStackEntry.arguments?.getString("chapterTitle") ?: ""

                val chapterId = java.net.URLDecoder.decode(encodedChapterId, "UTF-8")
                val chapterTitle = java.net.URLDecoder.decode(encodedChapterTitle, "UTF-8")

                val readerViewModel: ReaderViewModel = viewModel(
                    key = "reader_${mangaId}_$chapterId",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ReaderViewModel(repository, mangaId, chapterId, chapterTitle) as T
                        }
                    }
                )

                ReaderScreen(
                    viewModel = readerViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
