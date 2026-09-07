package cx.aswin.boxlore.core.designsystem.icon

/**
 * Resolves an exact case-insensitive match for a genre or topic keyword to its matching icon key.
 * Used to automatically update the folder/tag icon in real time as the user types without
 * requiring manual chip or icon grid taps.
 *
 * Matching priority:
 * 1. Exact match on suggestion display name (e.g. "Tech" -> "tech", "Comedy" -> "comedy")
 * 2. Direct canonical icon match or key/alias lookup from [GenreIcons] (e.g. "money" -> "finance", "star" -> "star", "ideas" -> "bulb")
 * 3. Exact match on suggestion topic keywords (e.g. "football" -> "sports", "ai" -> "tech", "humor" -> "comedy")
 * 4. Exact match on [GenreIcons] fallback keywords (e.g. "technology" -> "tech", "money" -> "finance")
 */
fun findExactGenreIconKey(
    query: String,
    allSuggestions: List<GenreSuggestion> = ALL_GENRE_SUGGESTIONS,
): String? {
    val trimmed = query.trim().lowercase()
    if (trimmed.isEmpty()) return null

    return findExactSuggestionNameMatch(trimmed, allSuggestions)
        ?: findDirectGenreIconMatch(trimmed)
        ?: findExactKeywordMatch(trimmed, allSuggestions)
        ?: findFallbackGenreIconMatch(trimmed)
}

private fun findExactSuggestionNameMatch(query: String, suggestions: List<GenreSuggestion>): String? =
    suggestions.firstOrNull {
        it.name.trim().equals(query, ignoreCase = true) && !it.iconKey.isNullOrBlank()
    }?.iconKey

private fun findDirectGenreIconMatch(query: String): String? {
    val directItem = GenreIcons.all.firstOrNull {
        (it.key.equals(query, ignoreCase = true) || it.label.equals(query, ignoreCase = true)) &&
            it.key != "category" &&
            it.key != "folder"
    }
    if (directItem != null) return directItem.key

    val canonicalVector = GenreIcons.findIcon(query) ?: return null
    return GenreIcons.all.firstOrNull {
        it.icon == canonicalVector &&
            it.key != "category" &&
            it.key != "folder"
    }?.key
}

private fun findExactKeywordMatch(query: String, suggestions: List<GenreSuggestion>): String? =
    suggestions.firstOrNull { suggestion ->
        !suggestion.iconKey.isNullOrBlank() &&
            suggestion.keywords.any { it.trim().equals(query, ignoreCase = true) }
    }?.iconKey

private fun findFallbackGenreIconMatch(query: String): String? {
    val fallbackVector = GenreIcons.defaultGenreIcon(query)
    return GenreIcons.all.firstOrNull {
        it.icon == fallbackVector &&
            it.key != "category" &&
            it.key != "folder"
    }?.key
}
