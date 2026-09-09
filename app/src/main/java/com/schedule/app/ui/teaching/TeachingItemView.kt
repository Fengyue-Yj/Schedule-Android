package com.schedule.app.ui.teaching

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.teaching.TeachingImporter
import com.schedule.app.teaching.TeachingKind
import com.schedule.app.teaching.TeachingStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingItemView(
    itemId: String,
    term: SettingEntity? = null,
    database: AppDatabase? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { TeachingStore.getInstance(context) }
    val snapshot by store.snapshot.collectAsState()
    val item = snapshot.items.find { it.id == itemId }
    val scope = rememberCoroutineScope()

    var imported by remember { mutableStateOf(false) }
    var courses by remember { mutableStateOf<List<CourseEntity>>(emptyList()) }
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var expandedCourseMenu by remember { mutableStateOf(false) }

    LaunchedEffect(term, database) {
        if (term != null && database != null) {
            courses = database.courseDao().getCoursesForTerm(term.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.displayCourseTitle ?: "Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (item != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = item.title, style = MaterialTheme.typography.headlineSmall)
                
                if (item.dueDate != null) {
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    Text(
                        text = "Due: ${format.format(item.dueDate)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (item.body.isNotBlank()) {
                    Text(text = item.body, style = MaterialTheme.typography.bodyMedium)
                }

                // Import Assignment button
                if (item.kind == TeachingKind.ASSIGNMENT && term != null && database != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Import to ${term.displayName}", style = MaterialTheme.typography.titleMedium)
                            
                            // Link course dropdown
                            Box {
                                OutlinedButton(
                                    onClick = { expandedCourseMenu = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val courseName = courses.find { it.id == selectedCourseId }?.name ?: "No Local Course (Optional)"
                                    Text("Link Course: $courseName")
                                }
                                DropdownMenu(
                                    expanded = expandedCourseMenu,
                                    onDismissRequest = { expandedCourseMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None") },
                                        onClick = {
                                            selectedCourseId = null
                                            expandedCourseMenu = false
                                        }
                                    )
                                    courses.forEach { course ->
                                        DropdownMenuItem(
                                            text = { Text(course.name) },
                                            onClick = {
                                                selectedCourseId = course.id
                                                expandedCourseMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val selectedCourse = courses.find { it.id == selectedCourseId }
                                        TeachingImporter.apply(
                                            item = item,
                                            term = term,
                                            course = selectedCourse,
                                            database = database
                                        )
                                        imported = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (imported) "Imported! (Tap to Update)" else "Import Assignment")
                            }
                        }
                    }
                }

                // Attachments
                if (item.attachments.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Attachments (${item.attachments.size})", style = MaterialTheme.typography.titleMedium)
                            item.attachments.forEach { att ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null)
                                        Text(att.name, maxLines = 1)
                                    }
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(att.url))
                                        context.startActivity(intent)
                                    }) {
                                        Icon(Icons.Default.Download, contentDescription = "Download")
                                    }
                                }
                            }
                        }
                    }
                }

                // Open original in browser
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceURL))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open in Browser")
                }
            }
        }
    }
}
