package com.komikta.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
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

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isDetail = currentDestination?.route?.startsWith("detail") == true
    val showBottomBar = currentDestination?.route in tabRoutes
    var focusSearch by remember { mutableStateOf(false) }
    val goTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Color(0xFF000000),
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    items = bottomNavItems,
                    currentRoute = currentDestination?.route,
                    onSelect = goTo
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = if (isDetail) Modifier else Modifier.padding(innerPadding),
            enterTransition = { if (isDeepRoute(targetState.destination.route)) fadeIn(tween(140)) else EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { if (isDeepRoute(initialState.destination.route)) fadeOut(tween(120)) else ExitTransition.None }
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onComicClick = { s, slug -> navController.navigate("detail/$s/$slug") },
                    onReadClick = { s, comic, chapter -> navController.navigate("reader/$s/$comic/$chapter") },
                    onContinueViewAll = { goTo(BottomNavItem.Library.route) },
                    onUpdateViewAll = { goTo(BottomNavItem.Explore.route) },
                    onSearchClick = {
                        focusSearch = true
                        navController.navigate(BottomNavItem.Explore.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
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

private val tabRoutes = bottomNavItems.map { it.route }.toSet()

private fun isDeepRoute(route: String?): Boolean =
    route?.startsWith("detail") == true || route?.startsWith("reader") == true
