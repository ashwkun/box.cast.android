package cx.aswin.boxlore.core.designsystem.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.aswin.boxlore.core.designsystem.theme.GoogleSansWeight

/**
 * Material 3 confirmation dialog displayed before removing a downloaded episode.
 */
@Composable
fun RemoveDownloadConfirmationDialog(
    episodeTitle: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = "Remove download?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = GoogleSansWeight.bold,
            )
        },
        text = {
            Text(
                text = if (!episodeTitle.isNullOrBlank()) {
                    "Remove downloaded episode \"$episodeTitle\"? You'll need an internet connection to listen to it again."
                } else {
                    "Remove this downloaded episode? You'll need an internet connection to listen to it again."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(
                    text = "Remove",
                    fontWeight = GoogleSansWeight.bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    )
}
