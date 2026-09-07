package cx.aswin.boxlore.feature.library.subscriptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.SportsBaseball
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import cx.aswin.boxlore.core.designsystem.icon.GenreIcons
import cx.aswin.boxlore.core.model.Podcast
import java.util.Locale

/**
 * Genre pill metadata matched to Explore / Home / onboarding icons.
 * Kept in `:feature:library` (no feature→feature import of Explore).
 */
internal data class SubscriptionGenreItem(
    val label: String,
    val value: String,
    val icon: ImageVector,
)

internal val SUBSCRIPTION_GENRE_CATALOG = listOf(
    SubscriptionGenreItem("News", "News", Icons.Rounded.Newspaper),
    SubscriptionGenreItem("Tech", "Technology", Icons.Rounded.Computer),
    SubscriptionGenreItem("Business", "Business", Icons.Rounded.Work),
    SubscriptionGenreItem("Comedy", "Comedy", Icons.Rounded.SentimentVerySatisfied),
    SubscriptionGenreItem("True Crime", "True Crime", Icons.Rounded.Fingerprint),
    SubscriptionGenreItem("Sports", "Sports", Icons.Rounded.SportsBaseball),
    SubscriptionGenreItem("Health", "Health", Icons.Rounded.FavoriteBorder),
    SubscriptionGenreItem("History", "History", Icons.Rounded.AccountBalance),
    SubscriptionGenreItem("Arts", "Arts", Icons.Rounded.Palette),
    SubscriptionGenreItem("Society", "Society & Culture", Icons.Rounded.Person),
    SubscriptionGenreItem("Education", "Education", Icons.Rounded.School),
    SubscriptionGenreItem("Science", "Science", Icons.Rounded.Science),
    SubscriptionGenreItem("TV & Film", "TV & Film", Icons.Rounded.Movie),
    SubscriptionGenreItem("Fiction", "Fiction", Icons.Rounded.AutoStories),
    SubscriptionGenreItem("Music", "Music", Icons.Rounded.MusicNote),
    SubscriptionGenreItem("Religion", "Religion & Spirituality", Icons.Rounded.Star),
    SubscriptionGenreItem("Family", "Kids & Family", Icons.Rounded.Face),
    SubscriptionGenreItem("Leisure", "Leisure", Icons.Rounded.Weekend),
    SubscriptionGenreItem("Govt", "Government", Icons.Rounded.Gavel),
)

internal val AllGenreIcon: ImageVector = Icons.Rounded.Apps

internal fun resolveSubscriptionGenreItem(
    genre: String,
    podcasts: List<Podcast> = emptyList(),
): SubscriptionGenreItem {
    val standardMatch = SUBSCRIPTION_GENRE_CATALOG.find {
        it.value.equals(genre, ignoreCase = true) || it.label.equals(genre, ignoreCase = true)
    }

    val customIconKey = podcasts.firstOrNull { pod ->
        val eff = pod.effectiveGenre
        val matchesDirect = eff.equals(genre, ignoreCase = true) ||
            eff.split(",").any { it.trim().equals(genre, ignoreCase = true) }
        val matchesStandard = standardMatch != null &&
            (
            eff.equals(standardMatch.value, ignoreCase = true) ||
                eff.equals(standardMatch.label, ignoreCase = true) ||
                eff.split(",").any { token ->
                    val t = token.trim()
                    t.equals(standardMatch.value, ignoreCase = true) ||
                        t.equals(standardMatch.label, ignoreCase = true)
                }
            )
        (matchesDirect || matchesStandard) && !pod.customGenreIcon.isNullOrBlank()
    }?.customGenreIcon

    val customIcon = GenreIcons.findIcon(customIconKey)
    if (customIcon != null) {
        return SubscriptionGenreItem(
            label = standardMatch?.label ?: genre,
            value = standardMatch?.value ?: genre,
            icon = customIcon,
        )
    }

    if (standardMatch != null) return standardMatch

    return SubscriptionGenreItem(
        label = genre,
        value = genre,
        icon = GenreIcons.findIcon(genre) ?: GenreIcons.defaultGenreIcon(genre),
    )
}

internal fun resolveSubscriptionGenreItem(
    genre: String,
    customIconKey: String?,
): SubscriptionGenreItem {
    val customIcon = GenreIcons.findIcon(customIconKey)
    if (customIcon != null) {
        val standardMatch = SUBSCRIPTION_GENRE_CATALOG.find {
            it.value.equals(genre, ignoreCase = true) || it.label.equals(genre, ignoreCase = true)
        }
        return SubscriptionGenreItem(
            label = standardMatch?.label ?: genre,
            value = standardMatch?.value ?: genre,
            icon = customIcon,
        )
    }
    return resolveSubscriptionGenreItem(genre)
}

private fun parseGenreTokens(raw: String): List<String> =
    raw.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.equals("podcast", ignoreCase = true) }
        .map { genre ->
            genre.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

private fun canonicalizeGenreDisplay(rawGenre: String): String {
    val matched = SUBSCRIPTION_GENRE_CATALOG.find {
        it.value.equals(rawGenre, ignoreCase = true) || it.label.equals(rawGenre, ignoreCase = true)
    }
    return matched?.label ?: rawGenre
}

internal fun extractDistinctGenres(podcasts: List<Podcast>): List<String> {
    val customCounts = mutableMapOf<String, Int>()
    val customDisplay = mutableMapOf<String, String>()
    val catalogGenres = mutableSetOf<String>()

    for (pod in podcasts) {
        val customRaw = pod.customGenre?.takeIf { it.isNotBlank() }
        if (customRaw != null) {
            for (rawTag in parseGenreTokens(customRaw)) {
                val tag = canonicalizeGenreDisplay(rawTag)
                val key = tag.lowercase()
                customCounts[key] = (customCounts[key] ?: 0) + 1
                customDisplay.putIfAbsent(key, tag)
            }
        } else {
            catalogGenres.addAll(
                parseGenreTokens(pod.genre.orEmpty()).map { canonicalizeGenreDisplay(it) },
            )
        }
    }

    val sortedCustom = customCounts.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { customDisplay[it.key] ?: it.key },
        )
        .map { customDisplay[it.key] ?: it.key }

    val customLower = customCounts.keys.toSet()
    val sortedCatalog = catalogGenres
        .filter { it.lowercase() !in customLower }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)

    return sortedCustom + sortedCatalog
}

internal fun filterPodcastsByGenre(podcasts: List<Podcast>, selectedGenre: String): List<Podcast> {
    if (selectedGenre.equals("All", ignoreCase = true) || selectedGenre.isBlank()) return podcasts
    val resolved = resolveSubscriptionGenreItem(selectedGenre, podcasts)
    return podcasts.filter { pod ->
        pod.effectiveGenre.split(",")
            .map { it.trim() }
            .any {
                it.equals(selectedGenre, ignoreCase = true) ||
                    it.equals(resolved.value, ignoreCase = true) ||
                    it.equals(resolved.label, ignoreCase = true)
            }
    }
}
