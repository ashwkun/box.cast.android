package cx.aswin.boxcast.feature.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentRegion: String = "us",
    onSetRegion: (String) -> Unit = {},
    onBack: () -> Unit,
    onResetAnalytics: () -> Unit,
    isAnalyticsEnabled: Boolean = false,
    onToggleAnalytics: (Boolean) -> Unit = {},
    isCrashReportingEnabled: Boolean = false,
    onToggleCrashReporting: (Boolean) -> Unit = {},
    appInstanceId: String? = null
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var isDeletionExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    
    // Calculate enabled state at top level to ensure recomposition
    val isDataCollectionEnabled = isCrashReportingEnabled || isAnalyticsEnabled

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            
            // SECTION: Content Preferences (Region)
            item {
                CollapsibleSection("Global Content", initiallyExpanded = false) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                             ListItem(
                                headlineContent = { Text("Content Region") },
                                supportingContent = { Text("Choose region for trending charts.") },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Rounded.Public, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                            )
                            // Region Toggle Row
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isGlobal = currentRegion != "in"
                                
                                // INDIA Option
                                FilterChip(
                                    selected = !isGlobal,
                                    onClick = { onSetRegion("in") },
                                    label = { Text("India (IN)") },
                                    leadingIcon = { 
                                        if (!isGlobal) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // GLOBAL Option
                                FilterChip(
                                    selected = isGlobal,
                                    onClick = { onSetRegion("us") },
                                    label = { Text("Global (US)") },
                                    leadingIcon = { 
                                        if (isGlobal) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION: Privacy & Data (Unified)
            item {
                CollapsibleSection("Privacy & Data", initiallyExpanded = false) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column {
                            // Analytics Toggle
                            ListItem(
                                headlineContent = { Text("App Analytics") },
                                supportingContent = { Text("Share anonymous usage data.") },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Rounded.Info, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = isAnalyticsEnabled,
                                        onCheckedChange = onToggleAnalytics
                                    )
                                }
                            )
                            HorizontalDivider()
                            
                            // Crash Reports Toggle
                            ListItem(
                                headlineContent = { Text("Crash Reporting") },
                                supportingContent = { Text("Auto-report bugs and crashes.") },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Rounded.Warning, 
                                        contentDescription = null, // Warning icon for crashes
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = isCrashReportingEnabled,
                                        onCheckedChange = onToggleCrashReporting
                                    )
                                }
                            )
                             HorizontalDivider()

                            // Privacy Policy
                            ListItem(
                                headlineContent = { Text("Privacy Policy") },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aswin.cx/boxcast/privacy"))
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                }
                            )
                            
                            // --- UNIFIED DATA MANAGEMENT SECTION ---
                             HorizontalDivider(thickness = 4.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                             
                             // Reset Identity
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        "Reset Identity",
                                        color = if (isDataCollectionEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    ) 
                                },
                                supportingContent = { Text("Generate new anonymous ID.") },
                                leadingContent = {
                                    Icon(
                                        Icons.Rounded.Delete, 
                                        contentDescription = null,
                                        tint = if (isDataCollectionEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                },
                                 modifier = Modifier.clickable(enabled = isDataCollectionEnabled) { showResetDialog = true }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                modifier = Modifier.padding(start = 56.dp)
                            )
                            
                            // Request Immediate Deletion (Expandable)
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        "Request Immediate Deletion", 
                                        color = if (isDataCollectionEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    ) 
                                },
                                supportingContent = { 
                                    if (!isDataCollectionEnabled) {
                                        Text("Data collection is currently disabled.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    } else {
                                        Text("Permanently erase server data.")
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Rounded.Email, 
                                        contentDescription = null,
                                        tint = if (isDataCollectionEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                },
                                trailingContent = {
                                    if (isDataCollectionEnabled) {
                                        Icon(
                                            if (isDeletionExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                modifier = Modifier.clickable(enabled = isDataCollectionEnabled) { 
                                    isDeletionExpanded = !isDeletionExpanded 
                                }
                            )

                            // Expanded Content (Instance ID + Email)
                            androidx.compose.animation.AnimatedVisibility(visible = isDeletionExpanded && isDataCollectionEnabled) {
                                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                    Text(
                                        text = "To request deletion, please email us your Instance ID below. This is the only way we can identify your anonymous data.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // ID Display
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            appInstanceId?.let {
                                                clipboardManager.setText(AnnotatedString(it))
                                                Toast.makeText(context, "ID Copied", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = appInstanceId ?: "Generating ID...",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Email Button
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:")
                                                putExtra(Intent.EXTRA_EMAIL, arrayOf("privacy@aswin.cx"))
                                                putExtra(Intent.EXTRA_SUBJECT, "Data Deletion Request")
                                                putExtra(Intent.EXTRA_TEXT, "Please delete data associated with Instance ID: ${appInstanceId ?: "UNKNOWN"}")
                                            }
                                            try { context.startActivity(intent) } catch(_: Exception) {
                                                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Email privacy@aswin.cx")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: Project
            item {
                CollapsibleSection(title = "Project & Community", initiallyExpanded = false) {
                    ElevatedCard(
                         modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column {
                             ListItem(
                                headlineContent = { Text("GitHub Repository") },
                                supportingContent = { Text("Open Source. Star, fork, or contribute!") },
                                leadingContent = {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(cx.aswin.boxcast.core.designsystem.R.drawable.ic_github),
                                        contentDescription = "GitHub",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                 modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ashwkun/box.cast.android"))
                                    try { context.startActivity(intent) } catch(_:Exception){}
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                             ListItem(
                                headlineContent = { Text("Podcast Index") },
                                supportingContent = { Text("Powered by the decentralized index.") },
                                leadingContent = {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFFE22828)) // Red for Index
                                },
                                 modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://podcastindex.org"))
                                    try { context.startActivity(intent) } catch(_:Exception){}
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

                             ListItem(
                                headlineContent = { Text("Apple Podcasts") },
                                supportingContent = { Text("Catalog reference.") },
                                leadingContent = {
                                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFF8E8E93))
                                },
                                 modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://podcasts.apple.com"))
                                    try { context.startActivity(intent) } catch(_:Exception){}
                                }
                            )
                        }
                    }
                }
            }

             item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "boxcast v1.0.0 (Beta)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Made with ❤️",
                        style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset Analytics Identity?") },
            text = {
                Column {
                    Text("This acts as a 'Forget Me' for our cloud servers.")
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                         color = MaterialTheme.colorScheme.surfaceContainer,
                         shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Local Data Safe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Your listening history, subscriptions, and downloads are stored ON THIS DEVICE and WILL NOT be deleted.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Your cloud analytics ID will be regenerated.")
                    Text("• Orphaned data is auto-deleted after 14 months.")
                    Text("• You will be asked to RE-CONSENT immediately.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAnalytics()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset & Re-Onboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    
    Column {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Content
        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
             content()
        }
    }
}
