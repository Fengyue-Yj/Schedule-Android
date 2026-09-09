package com.schedule.app.ui.teaching

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.teaching.TeachingKind
import com.schedule.app.teaching.TeachingStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingHubScreen(
    onNavigateBack: () -> Unit,
    term: SettingEntity? = null,
    database: AppDatabase? = null
) {
    val context = LocalContext.current
    val store = remember { TeachingStore.getInstance(context) }
    val snapshot by store.snapshot.collectAsState()
    val isRefreshing by store.isRefreshing.collectAsState()
    val isSignedIn by store.isSignedIn.collectAsState()
    
    var selectedKind by remember { mutableStateOf(TeachingKind.ANNOUNCEMENT) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (selectedItemId != null) {
        TeachingItemView(
            itemId = selectedItemId!!,
            term = term,
            database = database,
            onNavigateBack = { selectedItemId = null }
        )
        return
    }

    if (!isSignedIn) {
        TeachingSignInScreen(
            onNavigateBack = onNavigateBack,
            onSignInSuccess = {
                scope.launch { store.refresh() }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teaching Network") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { store.refresh() } },
                        enabled = !isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    TextButton(onClick = { store.signOut() }) {
                        Text("Sign Out")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = TeachingKind.entries.indexOf(selectedKind)) {
                TeachingKind.entries.forEach { kind ->
                    Tab(
                        selected = selectedKind == kind,
                        onClick = { selectedKind = kind },
                        text = { Text(kind.title) }
                    )
                }
            }
            
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            val filteredItems = snapshot.items.filter { it.kind == selectedKind }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No ${selectedKind.title.lowercase()} available. Tap refresh to load from PKU network.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    store.markRead(item.id)
                                    selectedItemId = item.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = item.displayCourseTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.dueDate != null) {
                                    Text(
                                        text = "Due: ${java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(item.dueDate)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
