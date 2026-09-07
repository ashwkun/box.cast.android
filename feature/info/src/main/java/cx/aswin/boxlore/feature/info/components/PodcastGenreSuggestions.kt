package cx.aswin.boxlore.feature.info.components

import cx.aswin.boxlore.core.designsystem.icon.ALL_GENRE_SUGGESTIONS as CORE_ALL_GENRE_SUGGESTIONS
import cx.aswin.boxlore.core.designsystem.icon.GenreIconItem
import cx.aswin.boxlore.core.designsystem.icon.GenreSuggestion as CoreGenreSuggestion
import cx.aswin.boxlore.core.designsystem.icon.buildGenreSuggestionsWithFolders as coreBuildGenreSuggestionsWithFolders
import cx.aswin.boxlore.core.designsystem.icon.filterGenreSuggestions as coreFilterGenreSuggestions
import cx.aswin.boxlore.core.designsystem.icon.findSuggestedIcons as coreFindSuggestedIcons

/**
 * Re-export [CoreGenreSuggestion] for backward compatibility within feature:info.
 */
typealias GenreSuggestion = CoreGenreSuggestion

/**
 * Re-export [CORE_ALL_GENRE_SUGGESTIONS] for backward compatibility within feature:info.
 */
val ALL_GENRE_SUGGESTIONS: List<GenreSuggestion> = CORE_ALL_GENRE_SUGGESTIONS

/**
 * Filters and ranks genre suggestions according to the user's typed search query.
 * Delegates to [coreFilterGenreSuggestions] in :core:designsystem.
 */
fun filterGenreSuggestions(
    query: String,
    allSuggestions: List<GenreSuggestion> = ALL_GENRE_SUGGESTIONS,
): List<GenreSuggestion> = coreFilterGenreSuggestions(query, allSuggestions)

/**
 * Returns distinct icons suggested for the given user query.
 * Delegates to [coreFindSuggestedIcons] in :core:designsystem.
 */
fun findSuggestedIcons(
    query: String,
    allSuggestions: List<GenreSuggestion> = ALL_GENRE_SUGGESTIONS,
): List<GenreIconItem> = coreFindSuggestedIcons(query, allSuggestions)

/**
 * Augments base suggestions with user folder names.
 * Delegates to [coreBuildGenreSuggestionsWithFolders] in :core:designsystem.
 */
fun buildGenreSuggestionsWithFolders(
    folderNames: List<String>,
    baseSuggestions: List<GenreSuggestion> = ALL_GENRE_SUGGESTIONS,
): List<GenreSuggestion> = coreBuildGenreSuggestionsWithFolders(folderNames, baseSuggestions)
