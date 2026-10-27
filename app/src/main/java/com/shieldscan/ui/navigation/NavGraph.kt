package com.shieldscan.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shieldscan.ui.dashboard.DashboardScreen
import com.shieldscan.ui.applist.AppListScreen
import com.shieldscan.ui.detail.AppDetailScreen
import com.shieldscan.ui.alerts.AlertsScreen
import com.shieldscan.ui.setup.SetupScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object AppList : Screen("applist", "Apps", Icons.Default.List)
    object Alerts : Screen("alerts", "Alerts", Icons.Default.Notifications)
    object Setup : Screen("setup", "Setup", Icons.Default.Settings)
}

@Composable
fun ShieldNavHost() {
    val nav = rememberNavController()
    val tabs = listOf(Screen.Dashboard, Screen.AppList, Screen.Alerts, Screen.Setup)

    Scaffold(
        bottomBar = {
            val backStack by nav.currentBackStackEntryAsState()
            val current = backStack?.destination?.route
            // Hide bottom bar on detail
            if (current?.startsWith("detail") == true) return@Scaffold
            NavigationBar {
                tabs.forEach { s ->
                    NavigationBarItem(
                        selected = current == s.route,
                        onClick = {
                            nav.navigate(s.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(s.icon, contentDescription = s.label) },
                        label = { Text(s.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(navController = nav, startDestination = Screen.Dashboard.route, modifier = Modifier.padding(pad)) {
            composable(Screen.Dashboard.route) { DashboardScreen(onOpenApp = { pkg -> nav.navigate("detail/$pkg") }, onOpenList = { nav.navigate(Screen.AppList.route) }) }
            composable(Screen.AppList.route) { AppListScreen(onOpenApp = { pkg -> nav.navigate("detail/$pkg") }) }
            composable(Screen.Alerts.route) { AlertsScreen(onOpenApp = { pkg -> nav.navigate("detail/$pkg") }) }
            composable(Screen.Setup.route) { SetupScreen() }
            composable("detail/{pkg}", arguments = listOf(navArgument("pkg") { type = NavType.StringType })) { entry ->
                val pkg = entry.arguments?.getString("pkg") ?: ""
                AppDetailScreen(packageName = pkg, onBack = { nav.popBackStack() })
            }
        }
    }
}
