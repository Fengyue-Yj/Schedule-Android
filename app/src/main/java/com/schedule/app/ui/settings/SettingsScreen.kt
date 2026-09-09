package com.schedule.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.data.models.TermSeason
import com.schedule.app.ui.teaching.TeachingHubScreen
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.util.CalendarManager
import com.schedule.app.util.CourseImporter
import com.schedule.app.util.ImportSummary
import com.schedule.app.util.TermDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    term: SettingEntity,
    database: AppDatabase,
    onDismiss: () -> Unit,
    onTermChanged: (SettingEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTerms by database.settingDao().getAll().collectAsState(initial = listOf(term))

    var currentDraft by remember(term) { mutableStateOf(TermDraft(term)) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showTeachingHub by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportSummary?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // CSV File Picker
    val csvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            CalendarManager.decodeCsvBytes(bytes)
                        } ?: ""
                    }
                    if (content.isNotBlank()) {
                        val summary = CourseImporter.importCSV(
                            csv = content,
                            termId = term.id,
                            courseDao = database.courseDao(),
                            meetingDao = database.courseMeetingDao()
                        )
                        importResult = summary
                    } else {
                        importError = "所选文件内容为空，请重新选择。"
                    }
                } catch (e: Exception) {
                    importError = e.localizedMessage ?: "导入日历/课表文件失败。"
                }
            }
        }
    }

    if (showTeachingHub) {
        TeachingHubScreen(
            onNavigateBack = { showTeachingHub = false },
            term = term,
            database = database
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    TextButton(onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val updated = currentDraft.apply(term)
                            database.settingDao().update(updated)
                            withContext(Dispatchers.Main) {
                                onTermChanged(updated)
                                onDismiss()
                            }
                        }
                    }) {
                        Text("Done", fontWeight = FontWeight.Bold, color = AppTheme.colors.accent)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Semesters Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Semesters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    var expandedDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Current: ${term.displayName}")
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            allTerms.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.displayName) },
                                    onClick = {
                                        onTermChanged(item)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Semester")
                    }
                }
            }

            // Semester Details Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Semester Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = currentDraft.name,
                        onValueChange = { currentDraft = currentDraft.copy(name = it) },
                        label = { Text("Semester Name") },
                        placeholder = { Text(term.displayName) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Week 1 Start Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Week 1 Starts", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                dateFormat.format(currentDraft.startDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = currentDraft.startDate }
                            cal.add(Calendar.DAY_OF_YEAR, -7)
                            currentDraft = currentDraft.copy(startDate = CalendarManager.mondayOnOrBefore(cal.timeInMillis))
                        }) {
                            Text("-1 Wk")
                        }
                        OutlinedButton(onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = currentDraft.startDate }
                            cal.add(Calendar.DAY_OF_YEAR, 7)
                            currentDraft = currentDraft.copy(startDate = CalendarManager.mondayOnOrBefore(cal.timeInMillis))
                        }) {
                            Text("+1 Wk")
                        }
                    }

                    // Total Weeks Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Weeks: ${currentDraft.totalWeeks}", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    if (currentDraft.totalWeeks > 1) {
                                        currentDraft = currentDraft.copy(totalWeeks = currentDraft.totalWeeks - 1)
                                    }
                                }
                            ) {
                                Text("-")
                            }
                            FilledTonalButton(
                                onClick = {
                                    if (currentDraft.totalWeeks < 52) {
                                        currentDraft = currentDraft.copy(totalWeeks = currentDraft.totalWeeks + 1)
                                    }
                                }
                            ) {
                                Text("+")
                            }
                        }
                    }

                    // Season Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Season:", style = MaterialTheme.typography.bodyMedium)
                        TermSeason.entries.forEach { season ->
                            FilterChip(
                                selected = currentDraft.season == season,
                                onClick = { currentDraft = currentDraft.copy(season = season) },
                                label = { Text(season.displayName) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = currentDraft.grade,
                        onValueChange = { currentDraft = currentDraft.copy(grade = it) },
                        label = { Text("Grade / Year (Optional)") },
                        placeholder = { Text("e.g. 2024") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Schedule View Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Schedule View", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show Weekends (Sat / Sun)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = currentDraft.showWeekends,
                            onCheckedChange = { currentDraft = currentDraft.copy(showWeekends = it) }
                        )
                    }
                }
            }

            // Import Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("导入课表 / 日历文件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "支持标准课表 CSV 文件及 iCalendar (.ics) 日历文件。自动识别课程名称、教师、教室、星期及节次（如1-2节）。兼容 UTF-8、GBK 编码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { csvPicker.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("选择 CSV / 日历文件导入")
                    }
                }
            }

            // Connections Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Connections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { showTeachingHub = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PKU Teaching Network")
                    }
                }
            }

            // Appearance Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val courses = database.courseDao().getCoursesForTerm(term.id)
                                courses.forEach { course ->
                                    database.courseDao().update(course.copy(colorSeed = (1..1000000).random()))
                                }
                                statusMessage = "Randomized colors for ${courses.size} courses."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Randomize Course Colors")
                    }
                }
            }

            // Data Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Current Semester Data")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Create Semester Dialog
    if (showCreateDialog) {
        var newName by remember { mutableStateOf("") }
        var newSeason by remember { mutableStateOf(TermSeason.FALL) }
        var newWeeks by remember { mutableIntStateOf(20) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Semester") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Semester Name") },
                        placeholder = { Text("e.g. 2026 Fall") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TermSeason.entries.forEach { s ->
                            FilterChip(
                                selected = newSeason == s,
                                onClick = { newSeason = s },
                                label = { Text(s.displayName) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val draft = TermDraft().copy(
                                name = newName.trim(),
                                season = newSeason,
                                totalWeeks = newWeeks
                            )
                            val newSetting = draft.makeSetting()
                            database.settingDao().insert(newSetting)
                            withContext(Dispatchers.Main) {
                                onTermChanged(newSetting)
                                showCreateDialog = false
                            }
                        }
                    },
                    enabled = newName.trim().isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Clear Data Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Semester Data?") },
            text = { Text("This will permanently delete all courses, schedule sessions, assignments, exams, and plans in ${term.displayName}. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        database.courseDao().deleteByTermId(term.id)
                        database.assignmentDao().deleteByTermId(term.id)
                        database.examDao().deleteByTermId(term.id)
                        database.flexiblePlanDao().deleteByTermId(term.id)
                        withContext(Dispatchers.Main) {
                            showClearDialog = false
                            statusMessage = "Cleared data for ${term.displayName}."
                        }
                    }
                }) {
                    Text("Clear Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Status / Import Result Alert
    importResult?.let { summary ->
        AlertDialog(
            onDismissRequest = { importResult = null },
            title = { Text("Import Successful") },
            text = { Text(summary.message) },
            confirmButton = {
                TextButton(onClick = { importResult = null }) { Text("OK") }
            }
        )
    }

    importError?.let { err ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("Import Failed") },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = { importError = null }) { Text("OK") }
            }
        )
    }

    statusMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { statusMessage = null },
            title = { Text("Notice") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { statusMessage = null }) { Text("OK") }
            }
        )
    }
}
