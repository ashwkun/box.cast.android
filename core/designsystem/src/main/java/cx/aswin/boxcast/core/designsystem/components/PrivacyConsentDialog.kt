package cx.aswin.boxcast.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes

@Composable
fun PrivacyConsentDialog(
    onConsentDecided: (crashReporting: Boolean, usageAnalytics: Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrivacyHeader()
                Spacer(modifier = Modifier.height(16.dp))
                
                PrivacyBody()
                Spacer(modifier = Modifier.height(24.dp))
                
                // State
                var crashReporting by remember { mutableStateOf(false) }
                var usageAnalytics by remember { mutableStateOf(false) }
                var privacyPolicyAccepted by remember { mutableStateOf(false) }

                ConsentOptions(
                    crashReporting = crashReporting,
                    onCrashChange = { crashReporting = it },
                    usageAnalytics = usageAnalytics,
                    onUsageChange = { usageAnalytics = it },
                    privacyPolicyAccepted = privacyPolicyAccepted,
                    onPolicyChange = { privacyPolicyAccepted = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                ConsentActions(
                    isEnabled = privacyPolicyAccepted,
                    onAccept = { onConsentDecided(crashReporting, usageAnalytics) }
                )
            }
        }
    }
}

@Composable
private fun PrivacyHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = ExpressiveShapes.Burst,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {}
            BoxCastLogo(textColor = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Help improve boxcast",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PrivacyBody() {
    Column {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "The data shared will only be used to help us improve the product by understanding user behaviour and global suggestions. No user-specific targeted personalization will be done, and no data collected will ever be user-identifiable. We will never use this data for advertising or any monetary gains.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        DataCollectedExpander()
    }
}

@Composable
private fun DataCollectedExpander() {
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { isExpanded = !isExpanded }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "View Data Collected",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                DataPoint("Crash Reports", "Technical data (stack traces) to help us identify and fix bugs.")
                Spacer(modifier = Modifier.height(8.dp))
                DataPoint("Usage Statistics", "Anonymous interaction data to help us understand popular features.")
            }
        }
    }
}

@Composable
private fun ConsentOptions(
    crashReporting: Boolean,
    onCrashChange: (Boolean) -> Unit,
    usageAnalytics: Boolean,
    onUsageChange: (Boolean) -> Unit,
    privacyPolicyAccepted: Boolean,
    onPolicyChange: (Boolean) -> Unit
) {
    Column {
        ToggleRow("Share Crash Reports (Optional)", crashReporting, onCrashChange)
        Spacer(modifier = Modifier.height(8.dp))
        ToggleRow("Share Usage Statistics (Optional)", usageAnalytics, onUsageChange)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Privacy Policy
        val uriHandler = LocalUriHandler.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { onPolicyChange(!privacyPolicyAccepted) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = privacyPolicyAccepted,
                onCheckedChange = null // Handled by Row
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "I agree to the ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        try { uriHandler.openUri("https://aswin.cx/boxcast/privacy") } catch(_: Exception) {}
                    }
                )
            }
        }
    }
}

@Composable
private fun ConsentActions(
    isEnabled: Boolean,
    onAccept: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onAccept,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Accept & Continue")
        }
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DataPoint(title: String, desc: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
