package com.komikta.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.komikta.feature.detail.DetailScreen
import com.komikta.feature.explore.ExploreScreen
import com.komikta.feature.home.HomeScreen
import com.komikta.feature.library.LibraryScreen
import com.komikta.feature.profile.ProfileScreen
import com.komikta.feature.reader.ReaderScreen
import com.komikta.ui.theme.Ui
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private val tabRoutes = bottomNavItems.map { it.route }

private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = route in tabRoutes
    val isDetail = route?.startsWith("detail") == true
    var focusSearch by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Ui.Bg,
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    items = bottomNavItems,
                    selectedRoute = route,
                    onSelect = navController::goToTab
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = if (isDetail) Modifier else Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onComicClick = { s, slug -> navController.navigate("detail/$s/$slug") },
                    onReadClick = { s, comic, chapter -> navController.navigate("reader/$s/$comic/$chapter") },
                    onContinueViewAll = { navController.goToTab(BottomNavItem.Library.route) },
                    onUpdateViewAll = { navController.goToTab(BottomNavItem.Explore.route) },
                    onSearchClick = {
                        focusSearch = true
                        navController.goToTab(BottomNavItem.Explore.route)
                    }
                )
            }
            composable(BottomNavItem.Explore.route) {
                ExploreScreen(
                    onComicClick = { s, slug -> navController.navigate("detail/$s/$slug") },
                    focusSearch = focusSearch,
                    onSearchFocused = { focusSearch = false }
                )
            }
            composable(BottomNavItem.Library.route) {
                LibraryScreen(onComicClick = { s, slug -> navController.navigate("detail/$s/$slug") })
            }
            composable(BottomNavItem.Profile.route) { ProfileScreen() }

            composable(
                route = "detail/{source}/{slug}",
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("slug") { type = NavType.StringType }
                )
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    onChapterClick = { source, comicSlug, chapterSlug ->
                        navController.navigate("reader/$source/$comicSlug/$chapterSlug")
                    }
                )
            }

            composable(
                route = "reader/{source}/{comicSlug}/{chapterSlug}",
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("comicSlug") { type = NavType.StringType },
                    navArgument("chapterSlug") { type = NavType.StringType }
                )
            ) {
                ReaderScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
