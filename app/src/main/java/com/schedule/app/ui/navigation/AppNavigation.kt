package com.schedule.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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
        containerColor = AppTheme.colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Schedule.route,
                modifier = Modifier.fillMaxSize()
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

            FloatingBottomBar(
                navController = navController,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun FloatingBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavItem.Schedule.route
    val isDark = isSystemInDarkTheme()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color(0x3D000000),
                ambientColor = Color(0x18000000)
            ),
        shape = RoundedCornerShape(32.dp),
        color = if (isDark) Color(0xF21C2621) else Color(0xF7FFFFFF),
        border = BorderStroke(
            width = 0.8.dp,
            color = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem.entries.forEach { item ->
                val isSelected = currentRoute == item.route
                val animatedBgColor by animateColorAsState(
                    targetValue = if (isSelected) AppTheme.colors.selectedFill else Color.Transparent,
                    animationSpec = tween(200),
                    label = "tabBg"
                )
                val animatedContentColor by animateColorAsState(
                    targetValue = if (isSelected) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    animationSpec = tween(200),
                    label = "tabContent"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(animatedBgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = animatedContentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = animatedContentColor
                        )
                    }
                }
            }
        }
    }
}
