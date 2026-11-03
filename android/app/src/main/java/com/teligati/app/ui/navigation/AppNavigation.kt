package com.teligati.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.teligati.app.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Explore : Screen("explore", "Explore", Icons.Filled.TravelExplore)
    object Opportunities : Screen("opportunities", "Careers", Icons.Filled.Work)
    object Projects : Screen("projects", "Spellbook", Icons.Filled.MenuBook)
    object AI : Screen("ai", "Teligati AI", Icons.Filled.AutoAwesome)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Explore,
        Screen.Opportunities,
        Screen.Projects,
        Screen.AI,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Explore.route) { ExploreScreen() }
            composable(Screen.Opportunities.route) { OpportunitiesScreen() }
            composable(Screen.Projects.route) { ProjectsScreen() }
            composable(Screen.AI.route) { TeligatiAiScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }
}
