package com.komikta.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.komikta.feature.profile.ProfileScreen
import com.komikta.feature.profile.SettingsViewModel
import com.komikta.feature.reader.ReaderScreen

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isDetail = currentDestination?.route?.startsWith("detail") == true
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }
    val settings: SettingsViewModel = hiltViewModel()
    val floatingNav by settings.floatingNav.collectAsState()
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
            if (showBottomBar && floatingNav) {
                FloatingNavBar(
                    items = bottomNavItems,
                    currentRoute = currentDestination?.hierarchy?.firstOrNull { d -> bottomNavItems.any { it.route == d.route } }?.route,
                    onSelect = goTo
                )
            } else if (showBottomBar) {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFF2E2E30))
                    NavigationBar(
                        containerColor = Color(0xFF232325),
                        contentColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        item.title,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Color(0xFF9C98A2),
                                    unselectedTextColor = Color(0xFF9C98A2),
                                    indicatorColor = Color(0xFF3A3A3C)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = if (isDetail) Modifier else Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onComicClick = { s, slug -> navController.navigate("detail/$s/$slug") },
                    onReadClick = { s, comic, chapter -> navController.navigate("reader/$s/$comic/$chapter") },
                    onContinueViewAll = { goTo(BottomNavItem.Library.route) },
                    onUpdateViewAll = { goTo(BottomNavItem.Explore.route) },
                    onSearchClick = {
                        navController.navigate(BottomNavItem.Explore.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomNavItem.Explore.route) {
                ExploreScreen(onComicClick = { s, slug -> navController.navigate("detail/$s/$slug") })
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
