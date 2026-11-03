package com.komikta.ui.navigation

import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.komikta.feature.community.CommentsScreen
import com.komikta.feature.community.ModerationScreen
import com.komikta.feature.detail.DetailScreen
import com.komikta.feature.explore.ExploreScreen
import com.komikta.feature.home.HomeScreen
import com.komikta.feature.library.LibraryScreen
import com.komikta.feature.profile.ProfileScreen
import com.komikta.feature.profile.PublicProfileScreen
import com.komikta.feature.reader.ReaderScreen
import com.komikta.ui.theme.Mono
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
    val isDetail = route?.startsWith("detail") == true || route?.startsWith("comments") == true || route == "moderation" || route?.startsWith("public-profile") == true
    var focusSearch by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = if (route == BottomNavItem.Profile.route) Mono.Header else Ui.Bg,
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
            // Pindah antar tab: fade halus. Buka layar lain (detail, reader, komentar): geser tipis + fade.
            enterTransition = {
                if (initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes) fadeIn(tween(180))
                else slideInHorizontally(tween(240)) { it / 10 } + fadeIn(tween(240))
            },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(tween(220)) { it / 10 } + fadeOut(tween(200)) }
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
                LibraryScreen(
                    onComicClick = { s, slug -> navController.navigate("detail/$s/$slug") },
                    onExplore = { navController.goToTab(BottomNavItem.Explore.route) }
                )
            }
            composable(BottomNavItem.Profile.route) { ProfileScreen(
                    onOpen = { s, slug, chapter ->
                        if (chapter != null) navController.navigate("reader/$s/$slug/$chapter") else navController.navigate("detail/$s/$slug")
                    },
                    onModeration = { navController.navigate("moderation") }
                ) }

            composable("moderation") { ModerationScreen(onBack = { navController.popBackStack() }) }

            composable(
                route = "public-profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { entry ->
                PublicProfileScreen(
                    userId = entry.arguments?.getString("userId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpen = { source, slug, chapter ->
                        if (chapter != null) navController.navigate("reader/$source/$slug/$chapter") else navController.navigate("detail/$source/$slug")
                    }
                )
            }

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
                    },
                    onCommentsClick = { source, slug -> navController.navigate("comments/$source/$slug") }
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
                ReaderScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { id -> navController.navigate("public-profile/$id") }
                )
            }

            composable(
                route = "comments/{source}/{slug}",
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("slug") { type = NavType.StringType }
                )
            ) { CommentsScreen(onBack = { navController.popBackStack() }, onUserClick = { id -> navController.navigate("public-profile/$id") }) }

            composable(
                route = "comments/{source}/{slug}/{chapter}",
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("chapter") { type = NavType.StringType }
                )
            ) { CommentsScreen(onBack = { navController.popBackStack() }, onUserClick = { id -> navController.navigate("public-profile/$id") }) }
        }
    }
}
