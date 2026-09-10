package com.schedule.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.ui.calendar.CalendarScreen
import com.schedule.app.ui.insights.InsightsScreen
import com.schedule.app.ui.schedule.HomeScreen
import com.schedule.app.ui.tasks.TasksScreen
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

enum class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    Schedule("schedule", "Schedule", Icons.Default.GridView),
    Calendar("calendar", "Calendar", Icons.Default.CalendarMonth),
    Tasks("tasks", "Tasks", Icons.Default.CheckCircle),
    Insights("insights", "Insights", Icons.Default.BarChart)
}

@Composable
fun AppNavigation(database: AppDatabase) {
    val navController = rememberNavController()
    val allTerms by database.settingDao().getAll().collectAsState(initial = emptyList())
    var activeTermId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(allTerms) {
        if (allTerms.isEmpty()) {
            withContext(Dispatchers.IO) {
                val newTerm = SettingEntity.defaultSetting()
                database.settingDao().insert(newTerm)
                withContext(Dispatchers.Main) {
                    activeTermId = newTerm.id
                }
            }
        } else if (activeTermId == null || allTerms.none { it.id == activeTermId }) {
            activeTermId = allTerms.first().id
        }
    }

    val term = allTerms.firstOrNull { it.id == activeTermId } ?: allTerms.firstOrNull()

    if (term == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.accent
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppTheme.colors.accent,
                            selectedTextColor = AppTheme.colors.accent,
                            indicatorColor = AppTheme.colors.selectedFill,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate(item.route) {
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
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Schedule.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Schedule.route) {
                HomeScreen(
                    term = term,
                    database = database,
                    onTermChanged = { newTerm -> activeTermId = newTerm.id }
                )
            }
            composable(BottomNavItem.Calendar.route) {
                CalendarScreen(term = term, database = database)
            }
            composable(BottomNavItem.Tasks.route) {
                TasksScreen(term = term, database = database)
            }
            composable(BottomNavItem.Insights.route) {
                InsightsScreen(term = term, database = database)
            }
        }
    }
}
