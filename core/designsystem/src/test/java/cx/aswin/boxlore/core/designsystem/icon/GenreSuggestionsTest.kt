package cx.aswin.boxlore.core.designsystem.icon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GenreSuggestionsTest {

    @Test
    fun `empty and blank query returns all suggestions in order`() {
        val emptyResult = filterGenreSuggestions("")
        assertEquals(ALL_GENRE_SUGGESTIONS.size, emptyResult.size)

        val blankResult = filterGenreSuggestions("   ")
        assertEquals(ALL_GENRE_SUGGESTIONS.size, blankResult.size)
    }

    @Test
    fun `exact match ranks first`() {
        val result = filterGenreSuggestions("Comedy")
        assertTrue(result.isNotEmpty())
        assertEquals("Comedy", result.first().name)
    }

    @Test
    fun `prefix match ranks ahead of substring`() {
        val result = filterGenreSuggestions("com")
        assertTrue(result.isNotEmpty())
        assertEquals("Comedy", result.first().name)
    }

    @Test
    fun `keyword match finds technology by ai keyword`() {
        val result = filterGenreSuggestions("ai")
        assertTrue(result.any { it.name == "Technology" || it.name == "Tech" })
    }

    @Test
    fun `keyword match finds sports by formula 1`() {
        val result = filterGenreSuggestions("formula 1")
        assertTrue(result.isNotEmpty())
        assertEquals("Sports", result.first().name)
    }

    @Test
    fun `buildFolderSuggestionsWithLibrary places library genres at the top with isFromLibrary flag`() {
        val libraryGenres = listOf("Tech", "Comedy", "Custom Indie Topic")
        val combined = buildFolderSuggestionsWithLibrary(libraryGenres)

        assertTrue(combined.size >= 3)
        assertEquals("Tech", combined[0].name)
        assertTrue(combined[0].isFromLibrary)
        assertEquals("tech", combined[0].iconKey)

        assertEquals("Comedy", combined[1].name)
        assertTrue(combined[1].isFromLibrary)
        assertEquals("comedy", combined[1].iconKey)

        assertEquals("Custom Indie Topic", combined[2].name)
        assertTrue(combined[2].isFromLibrary)

        // Ensure non-library suggestions follow without duplication
        val remaining = combined.drop(3)
        assertFalse(remaining.any { it.name.equals("Tech", ignoreCase = true) })
        assertFalse(remaining.any { it.name.equals("Comedy", ignoreCase = true) })
        assertFalse(remaining.any { it.isFromLibrary })
    }

    @Test
    fun `buildFolderSuggestionsWithLibrary with empty library returns deduplicated suggestions without redundant twins`() {
        val combined = buildFolderSuggestionsWithLibrary(emptyList())
        val redundant = setOf("technology", "society", "family", "religion", "govt")
        assertEquals(ALL_GENRE_SUGGESTIONS.size - redundant.size, combined.size)
        assertFalse(combined.any { it.name.equals("Technology", ignoreCase = true) })
        assertTrue(combined.any { it.name == "Tech" })
    }

    @Test
    fun `filterGenreSuggestions prioritizes library genres when match score is tied`() {
        // Both Tech and Technology match query "tech", but Tech is in library
        val libraryGenres = listOf("Tech")
        val allWithLibrary = buildFolderSuggestionsWithLibrary(libraryGenres)

        val filtered = filterGenreSuggestions("tech", allWithLibrary)
        assertTrue(filtered.isNotEmpty())
        assertEquals("Tech", filtered.first().name)
        assertTrue(filtered.first().isFromLibrary)
    }

    @Test
    fun `findSuggestedIcons returns matching icons for query`() {
        val icons = findSuggestedIcons("tech")
        assertTrue(icons.isNotEmpty())
        assertTrue(icons.any { it.key == "tech" })
    }

    @Test
    fun `buildGenreSuggestionsWithFolders maps folder names correctly`() {
        val folders = listOf("My Dev Folder", "Gaming Squad")
        val combined = buildGenreSuggestionsWithFolders(folders)

        assertEquals("My Dev Folder", combined[0].name)
        assertEquals("Gaming Squad", combined[1].name)
        assertEquals("gaming", combined[1].iconKey)
    }
}
