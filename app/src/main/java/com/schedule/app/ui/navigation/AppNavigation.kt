package com.schedule.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
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
import android.content.Context
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE) }
    val navController = rememberNavController()
    val allTerms by database.settingDao().getAll().collectAsState(initial = emptyList())
    var activeTermId by remember { mutableStateOf(prefs.getString("active_term_id", null)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val terms = database.settingDao().getAllList()
            if (terms.isEmpty()) {
                val newTerm = SettingEntity.defaultSetting()
                database.settingDao().insert(newTerm)
                withContext(Dispatchers.Main) {
                    activeTermId = newTerm.id
                    prefs.edit().putString("active_term_id", newTerm.id).apply()
                }
            } else {
                // Determine which term to activate
                val savedId = prefs.getString("active_term_id", null)
                val savedTerm = terms.firstOrNull { it.id == savedId }
                
                // Identify all terms that actually have courses
                val termsWithCourses = terms.filter { term ->
                    database.courseDao().getCoursesForTerm(term.id).isNotEmpty()
                }

                val targetTermId = if (termsWithCourses.isNotEmpty()) {
                    if (savedTerm != null && termsWithCourses.any { it.id == savedTerm.id }) {
                        savedTerm.id
                    } else {
                        // Priority: auto-recover the term with courses!
                        termsWithCourses.first().id
                    }
                } else {
                    savedTerm?.id ?: terms.first().id
                }

                // Clean up empty phantom default terms created by the previous bug
                if (termsWithCourses.isNotEmpty()) {
                    val emptyTerms = terms.filter { term ->
                        database.courseDao().getCoursesForTerm(term.id).isEmpty()
                    }
                    for (empty in emptyTerms) {
                        try {
                            database.settingDao().delete(empty)
                        } catch (_: Exception) {}
                    }
                }

                withContext(Dispatchers.Main) {
                    activeTermId = targetTermId
                    prefs.edit().putString("active_term_id", targetTermId).apply()
                }
            }
        }
    }

    LaunchedEffect(activeTermId) {
        if (activeTermId != null) {
            prefs.edit().putString("active_term_id", activeTermId).apply()
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
            .height(62.dp)
            .shadow(
                elevation = if (isDark) 0.dp else 10.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = Color(0x26000000),
                ambientColor = Color(0x12000000)
            ),
        shape = RoundedCornerShape(32.dp),
        color = if (isDark) Color(0xF01C1C1E) else Color(0xF2FFFFFF),
        border = BorderStroke(
            width = 0.5.dp,
            color = if (isDark) Color(0x38FFFFFF) else Color(0x24000000)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem.entries.forEach { item ->
                val isSelected = currentRoute == item.route
                val animatedBgColor by animateColorAsState(
                    targetValue = if (isSelected) AppTheme.colors.selectedFill else Color.Transparent,
                    animationSpec = tween(180),
                    label = "tabBg"
                )
                val animatedContentColor by animateColorAsState(
                    targetValue = if (isSelected) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    animationSpec = tween(180),
                    label = "tabContent"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "tabIconScale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
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
                            modifier = Modifier
                                .size(21.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            letterSpacing = (-0.2).sp,
                            color = animatedContentColor
                        )
                    }
                }
            }
        }
    }
}
