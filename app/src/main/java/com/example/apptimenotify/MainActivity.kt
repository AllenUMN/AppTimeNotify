package com.example.apptimenotify

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.apptimenotify.ui.theme.AppTimeNotifyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
                val navController = rememberNavController()
                var showPermissionDialog by remember { mutableStateOf(false) }
                val context = LocalContext.current

                // Request notification permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (!isGranted) {
                            Log.w("MainActivity", "Notification permission denied")
                        }
                    }
                    SideEffect {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Re-check permission on every resume to dismiss dialog correctly
                DisposableEffect(Unit) {
                    if (!hasUsageStatsPermission(context)) {
                        showPermissionDialog = true
                    }
                    onDispose { }
                }

                // Better way to handle resume check in Compose
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            if (hasUsageStatsPermission(context)) {
                                showPermissionDialog = false
                            } else {
                                showPermissionDialog = true
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                if (showPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text("Permission Required") },
                        text = { Text("This app needs Usage Access to monitor your app usage. Please enable it in the settings.") },
                        confirmButton = {
                            Button(onClick = {
                                showPermissionDialog = false
                                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }) {
                                Text("Go to Settings")
                            }
                        }
                    )
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "app_list",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("app_list") {
                            AppListScreen(
                                onAppSelected = { packageName, appName ->
                                    navController.navigate("time_limit/$packageName/$appName")
                                }
                            )
                        }
                        composable(
                            "time_limit/{packageName}/{appName}",
                            arguments = listOf(
                                navArgument("packageName") { type = NavType.StringType },
                                navArgument("appName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                            val appName = backStackEntry.arguments?.getString("appName") ?: ""
                            TimeLimitScreen(
                                packageName = packageName,
                                appName = appName,
                                onConfirmed = {
                                    // Start the tracking service
                                    val intent = Intent(this@MainActivity, AppUsageService::class.java)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(intent)
                                    } else {
                                        startService(intent)
                                    }
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun AppListScreen(
    modifier: Modifier = Modifier,
    onAppSelected: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val currentLimit by UsagePrefs.getAppLimit(context).collectAsState(initial = null)
    
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
        if (currentLimit != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tracking: ${currentLimit!!.appName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Limit: ${currentLimit!!.hours}h ${currentLimit!!.minutes}m per day",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { 
                            scope.launch {
                                UsagePrefs.clearLimit(context)
                                context.stopService(Intent(context, AppUsageService::class.java))
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Stop Tracking")
                    }
                }
            }
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
                            onAppSelected(app.packageName, app.name)
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
