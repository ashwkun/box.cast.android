package cx.aswin.boxcast.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreSelector(
    selectedCategory: String?, // Null = For You
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "For You" to null,
        "News" to "News",
        "Technology" to "Technology",
        "Business" to "Business",
        "Comedy" to "Comedy",
        "True Crime" to "True Crime",
        "Sports" to "Sports",
        "Health" to "Health",
        "History" to "History",
        "Arts" to "Arts"
    )

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp), // 8dp + 16dp grid = 24dp edge
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { (label, value) ->
            val isSelected = selectedCategory == value
            
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = FilterChipDefaults.filterChipBorder(
                     enabled = true,
                     selected = isSelected,
                     borderColor = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
}
