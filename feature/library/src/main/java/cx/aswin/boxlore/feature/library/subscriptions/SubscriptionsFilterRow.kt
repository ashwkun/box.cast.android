package cx.aswin.boxlore.feature.library.subscriptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cx.aswin.boxlore.core.designsystem.components.PillFilterChip
import cx.aswin.boxlore.core.model.Podcast

/**
 * Explore-style genre pills (icons + short labels) reflecting standard and custom podcast genres.
 */
@Composable
internal fun SubscriptionsFilterRow(
    selectedGenre: String,
    onGenreChange: (String) -> Unit,
    distinctGenres: List<String>,
    podcasts: List<Podcast> = emptyList(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    onNewFolderClick: (() -> Unit)? = null,
) {
    val genreItems = remember(distinctGenres, podcasts) {
        distinctGenres
            .map { resolveSubscriptionGenreItem(it, podcasts) }
            .distinctBy { it.value.lowercase() }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onNewFolderClick != null) {
            item(key = "action_new_folder") {
                CompactNewFolderChip(
                    onClick = onNewFolderClick,
                )
            }
        }
        items(genreItems, key = { it.value }) { genre ->
            val isSelected = !selectedGenre.equals("All", ignoreCase = true) &&
                selectedGenre.isNotBlank() &&
                (
                    selectedGenre.equals(genre.value, ignoreCase = true) ||
                        selectedGenre.equals(genre.label, ignoreCase = true)
                )
            PillFilterChip(
                label = genre.label,
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        onGenreChange("All")
                    } else {
                        onGenreChange(genre.value)
                    }
                },
                icon = genre.icon,
                trailingIcon = if (isSelected) Icons.Rounded.Close else null,
            )
        }
    }
}

@Composable
internal fun CompactNewFolderChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.CreateNewFolder,
                contentDescription = "New folder",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
