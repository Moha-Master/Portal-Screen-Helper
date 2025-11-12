package cafe.jiahui.portalscreenhelper

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import cafe.jiahui.portalscreenhelper.ui.theme.PortalScreenHelperTheme
import cafe.jiahui.portalscreenhelper.utils.PinyinUtils
import cafe.jiahui.portalscreenhelper.components.AlphabetIndex

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notifications permission granted.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications permission denied. Service may not work correctly.", Toast.LENGTH_LONG).show()
            }
        }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for permissions
        askNotificationPermission()
        checkOverlayPermission()

        setContent {
            PortalScreenHelperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding() // Add padding for system bars (status bar and navigation bar)
                    ) {
                        val mainViewModel: MainViewModel = viewModel(
                            factory = viewModelFactory {
                                initializer {
                                    MainViewModel(application)
                                }
                            }
                        )
                        val context = LocalContext.current
                        val appLauncher = remember { AppLauncher(context.packageManager) }
                        val apps = remember { appLauncher.getLaunchableApps() }

                        val displayId by mainViewModel.externalDisplayId.collectAsState()
                        val isDisplayConnected by mainViewModel.isDisplayConnected.collectAsState()
                        val toastMessage by mainViewModel.toastMessage.collectAsState()

                        // Show toast when connection status changes
                        LaunchedEffect(isDisplayConnected) {
                            if (displayId != "Detecting...") { // Only show if not initial detection
                                val statusText = if (isDisplayConnected) "External display connected: $displayId" else "External display disconnected"
                                Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                            }
                        }

                        // Show toast for specific messages (like app launching)
                        LaunchedEffect(toastMessage) {
                            toastMessage?.let {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                mainViewModel.onToastShown()
                            }
                        }

                        var searchText by remember { mutableStateOf("") }
                        val filteredApps = remember(apps, searchText) {
                            if (searchText.isEmpty()) {
                                apps
                            } else {
                                apps.filter { appInfo ->
                                    appInfo.appName.contains(searchText, ignoreCase = true) ||
                                            appInfo.packageName.contains(searchText, ignoreCase = true)
                                }
                            }
                        }
                        
                        // Add state for lazy grid to use in the AlphabetIndex
                        val lazyListState = rememberLazyGridState()
                        val coroutineScope = rememberCoroutineScope()
                        
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4), // Fixed to 4 columns
                                    state = lazyListState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredApps.size) { index ->
                                        val app = filteredApps[index]
                                        AppListItem(
                                            appInfo = app,
                                            isEnabled = isDisplayConnected,
                                            modifier = Modifier.clickable(enabled = isDisplayConnected) { 
                                                mainViewModel.launchApp(app) 
                                            }
                                        )
                                    }
                                }
                                AlphabetIndex(
                                    onLetterSelected = { letter ->
                                        // Find the first app with the selected letter and scroll to it
                                        val firstIndex = filteredApps.indexOfFirst { app ->
                                            val firstChar = app.appName.firstOrNull()?.uppercaseChar()
                                            val appLetter = if (firstChar in 'A'..'Z') firstChar.toString() else "#"
                                            appLetter == letter
                                        }
                                        
                                        if (firstIndex >= 0) {
                                            // Scroll to the item
                                            coroutineScope.launch {
                                                lazyListState.animateScrollToItem(firstIndex)
                                            }
                                        }
                                    }
                                )
                            }
                            SearchAndFooter(
                                isDisplayConnected = isDisplayConnected,
                                displayId = displayId,
                                onAppClick = { appInfo ->
                                    mainViewModel.launchApp(appInfo)
                                },
                                searchText = searchText,
                                onSearchTextChanged = { searchText = it }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent) // No need for result, user can grant it anytime
        }
    }
}

@Composable
fun Footer(displayId: String?, isDisplayConnected: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val statusText = if (isDisplayConnected) "Connected" else "Not Connected"
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun SearchAndFooter(
    isDisplayConnected: Boolean,
    displayId: String?,
    onAppClick: (AppInfo) -> Unit,
    searchText: String = "",
    onSearchTextChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column {
        // Search TextField at the bottom
        TextField(
            value = searchText,
            onValueChange = onSearchTextChanged,
            placeholder = { Text("Search Apps") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            singleLine = true,
            shape = MaterialTheme.shapes.small, // Rounded shape
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
        
        // Status text below search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val statusText = if (isDisplayConnected) "Connected" else "Not Connected"
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}


class AppLauncher(private val packageManager: PackageManager) {
    fun getLaunchableApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        return resolveInfoList.mapNotNull { resolveInfo ->
            val appName = resolveInfo.loadLabel(packageManager).toString()
            val packageName = resolveInfo.activityInfo.packageName
            val icon = resolveInfo.loadIcon(packageManager)
            AppInfo(appName, packageName, icon)
        }.sortedWith { appInfo1, appInfo2 ->
            PinyinUtils.compareAppNames(appInfo1.appName, appInfo2.appName)
        }
    }
}




@Composable
fun AppListItem(appInfo: AppInfo, isEnabled: Boolean, modifier: Modifier = Modifier) {
    val alpha = if (isEnabled) 1f else 0.4f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(4.dp), // Reduced padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp) // Reduced size
                .clip(RoundedCornerShape(12.dp)) // Slightly smaller corner radius
        ) {
            Image(
                bitmap = appInfo.icon.toBitmap().asImageBitmap(),
                contentDescription = "${appInfo.appName} icon",
                modifier = Modifier.size(52.dp) // Reduced size
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = appInfo.appName,
            style = MaterialTheme.typography.labelSmall, // Smaller text
            modifier = Modifier.align(Alignment.CenterHorizontally),
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}