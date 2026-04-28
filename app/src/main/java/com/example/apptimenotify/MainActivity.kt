package com.example.apptimenotify

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.apptimenotify.ui.theme.AppTimeNotifyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTimeNotifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppListScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppListScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppName by remember { mutableStateOf<String?>(null) }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load apps asynchronously
    LaunchedEffect(Unit) {
        try {
            allApps = withContext(Dispatchers.IO) {
                getInstalledApps(context)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading apps", e)
            errorMessage = "Failed to load apps: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, allApps) {
        allApps.filter { 
            it.name.contains(searchQuery, ignoreCase = true) 
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (selectedAppName != null) {
            Text(
                text = "Selected: $selectedAppName",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(onClick = { selectedAppName = null }) {
                Text("Clear Selection")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Apps") },
            modifier = Modifier.fillMaxWidth().testTag("search_bar"),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag("loading_indicator"))
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
            filteredApps.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No apps found")
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.weight(1f).testTag("app_list")) {
                    items(filteredApps) { app ->
                        AppItem(app = app) {
                            selectedAppName = app.name
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("app_item"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = app.name, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    // QUERY_ALL_PACKAGES is needed for this to work correctly on API 30+
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    return apps.mapNotNull { app ->
        try {
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                AppInfo(
                    name = pm.getApplicationLabel(app).toString(),
                    packageName = app.packageName,
                    icon = pm.getApplicationIcon(app)
                )
            } else null
        } catch (e: Exception) {
            null // Skip apps that fail to load
        }
    }.sortedBy { it.name }
}
