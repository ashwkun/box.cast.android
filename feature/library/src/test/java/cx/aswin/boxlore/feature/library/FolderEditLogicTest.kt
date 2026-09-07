package cx.aswin.boxlore.feature.library

import cx.aswin.boxlore.core.catalog.FolderRepository
import cx.aswin.boxlore.core.designsystem.icon.GenreIcons
import cx.aswin.boxlore.core.designsystem.icon.GenreSuggestion
import cx.aswin.boxlore.core.designsystem.icon.buildFolderSuggestionsWithLibrary
import cx.aswin.boxlore.core.designsystem.icon.filterGenreSuggestions
import cx.aswin.boxlore.core.designsystem.icon.findExactGenreIconKey
import cx.aswin.boxlore.core.model.FolderDisplaySize
import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.model.SubscriptionFolder
import cx.aswin.boxlore.feature.library.subscriptions.extractDistinctGenres
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FolderEditLogicTest {

    private class FakeFolderRepository : FolderRepository {
        private val _folders = MutableStateFlow<List<SubscriptionFolder>>(emptyList())
        override val folders: Flow<List<SubscriptionFolder>> = _folders
        override val folderNames: Flow<List<String>> = _folders.map { list -> list.map { it.name }.sorted() }

        override suspend fun getFolders(): List<SubscriptionFolder> = _folders.value

        override suspend fun getFolder(folderId: String): SubscriptionFolder? =
            _folders.value.firstOrNull { it.id == folderId }

        override fun getFolderFlow(folderId: String): Flow<SubscriptionFolder?> =
            _folders.map { list -> list.firstOrNull { it.id == folderId } }

        override suspend fun createFolder(
            name: String,
            icon: String?,
            displaySize: FolderDisplaySize,
            linkedGenre: String?,
            showPodcastGrid: Boolean,
            podcastIds: List<String>,
        ): SubscriptionFolder {
            val folder = SubscriptionFolder(
                id = UUID.randomUUID().toString(),
                name = name,
                icon = icon?.takeIf { it.isNotBlank() },
                displaySize = displaySize,
                linkedGenre = linkedGenre?.takeIf { it.isNotBlank() },
                showPodcastGrid = showPodcastGrid,
                podcastCount = podcastIds.size,
                podcastIds = podcastIds,
            )
            _folders.update { it + folder }
            return folder
        }

        override suspend fun updateFolder(folder: SubscriptionFolder) {
            _folders.update { list ->
                list.map { if (it.id == folder.id) folder else it }
            }
        }

        override suspend fun deleteFolder(folderId: String) {
            _folders.update { list -> list.filter { it.id != folderId } }
        }

        override suspend fun addPodcastToFolder(podcastId: String, folderId: String) {
            _folders.update { list ->
                list.map { folder ->
                    if (folder.id == folderId && podcastId !in folder.podcastIds) {
                        val newIds = folder.podcastIds + podcastId
                        folder.copy(podcastIds = newIds, podcastCount = newIds.size)
                    } else {
                        folder
                    }
                }
            }
        }

        override suspend fun removePodcastFromFolder(podcastId: String, folderId: String) {
            _folders.update { list ->
                list.map { folder ->
                    if (folder.id == folderId && podcastId in folder.podcastIds) {
                        val newIds = folder.podcastIds - podcastId
                        folder.copy(podcastIds = newIds, podcastCount = newIds.size)
                    } else {
                        folder
                    }
                }
            }
        }

        override suspend fun setPodcastsForFolder(folderId: String, podcastIds: List<String>) {
            _folders.update { list ->
                list.map { folder ->
                    if (folder.id == folderId) {
                        folder.copy(podcastIds = podcastIds, podcastCount = podcastIds.size)
                    } else {
                        folder
                    }
                }
            }
        }

        override suspend fun syncLinkedGenres() {
            // No-op in test fake
        }
    }

    @Test
    fun folderDisplaySize_enumCases() {
        assertEquals(7, FolderDisplaySize.entries.size)
        assertTrue(FolderDisplaySize.entries.contains(FolderDisplaySize.COMPACT))
        assertTrue(FolderDisplaySize.entries.contains(FolderDisplaySize.WIDE))
        assertTrue(FolderDisplaySize.entries.contains(FolderDisplaySize.FEATURED))
        assertTrue(FolderDisplaySize.entries.contains(FolderDisplaySize.LARGE))
        assertTrue(FolderDisplaySize.entries.contains(FolderDisplaySize.SHELF))
        assertTrue(FolderDisplaySize.entries.contains(FolderDisplaySize.PANEL))
        assertTrue(FolderDisplaySize.entries.contains(FolderDisplaySize.SHOWCASE))

        // Verify dimensions and metadata
        assertEquals(1, FolderDisplaySize.COMPACT.spanCols)
        assertEquals(1, FolderDisplaySize.COMPACT.spanRows)
        assertEquals("1×1", FolderDisplaySize.COMPACT.dimensionsLabel)

        assertEquals(2, FolderDisplaySize.WIDE.spanCols)
        assertEquals(1, FolderDisplaySize.WIDE.spanRows)
        assertEquals("2×1", FolderDisplaySize.WIDE.dimensionsLabel)

        assertEquals(2, FolderDisplaySize.FEATURED.spanCols)
        assertEquals(2, FolderDisplaySize.FEATURED.spanRows)
        assertEquals("2×2", FolderDisplaySize.FEATURED.dimensionsLabel)

        assertEquals(2, FolderDisplaySize.LARGE.spanCols)
        assertEquals(3, FolderDisplaySize.LARGE.spanRows)
        assertEquals("2×3", FolderDisplaySize.LARGE.dimensionsLabel)

        assertEquals(3, FolderDisplaySize.SHELF.spanCols)
        assertEquals(1, FolderDisplaySize.SHELF.spanRows)
        assertEquals("3×1", FolderDisplaySize.SHELF.dimensionsLabel)

        assertEquals(3, FolderDisplaySize.PANEL.spanCols)
        assertEquals(2, FolderDisplaySize.PANEL.spanRows)
        assertEquals("3×2", FolderDisplaySize.PANEL.dimensionsLabel)

        assertEquals(3, FolderDisplaySize.SHOWCASE.spanCols)
        assertEquals(3, FolderDisplaySize.SHOWCASE.spanRows)
        assertEquals("3×3", FolderDisplaySize.SHOWCASE.dimensionsLabel)
    }

    @Test
    fun optionalIcon_nullAndEmptyFallbackToDefaultFolderIcon() {
        val defaultIcon = GenreIcons.defaultFolderIcon()
        assertEquals(defaultIcon, GenreIcons.folderIconOrFallback(null))
        assertEquals(defaultIcon, GenreIcons.folderIconOrFallback(""))
        assertEquals(defaultIcon, GenreIcons.folderIconOrFallback("   "))

        // Known icon resolves to specific icon
        val techIcon = GenreIcons.folderIconOrFallback("tech")
        assertNotNull(techIcon)
    }

    @Test
    fun suggestedGenres_extractsAndDeduplicatesDistinctTags() {
        val podcasts = listOf(
            Podcast(id = "1", title = "P1", artist = "A1", imageUrl = "", genre = "Technology", customGenre = "AI"),
            Podcast(id = "2", title = "P2", artist = "A2", imageUrl = "", genre = "Technology", customGenre = "AI, Coding"),
            Podcast(id = "3", title = "P3", artist = "A3", imageUrl = "", genre = "News", customGenre = null),
            Podcast(id = "4", title = "P4", artist = "A4", imageUrl = "", genre = "Podcast", customGenre = null),
        )

        val genres = extractDistinctGenres(podcasts)
        assertTrue(genres.contains("AI"))
        assertTrue(genres.contains("Coding"))
        assertTrue(genres.contains("News"))
        // Generic "Podcast" should be excluded
        assertFalse(genres.contains("Podcast"))
    }

    @Test
    fun fakeFolderRepository_createUpdateDeleteLifecycle() = runTest {
        val fakeRepo = FakeFolderRepository()

        // 1. Create with optional icon = null
        val created = fakeRepo.createFolder(
            name = "Science & Nature",
            icon = null,
            displaySize = FolderDisplaySize.SHELF,
            linkedGenre = "Science",
        )

        assertNotNull(created.id)
        assertEquals("Science & Nature", created.name)
        assertNull(created.icon)
        assertFalse(created.hasIcon)
        assertEquals(FolderDisplaySize.SHELF, created.displaySize)
        assertEquals("Science", created.linkedGenre)
        assertTrue(created.isGenreLinked)

        // 2. Observe flow
        val folders = fakeRepo.folders.first()
        assertEquals(1, folders.size)
        assertEquals(created.id, folders.first().id)

        // 3. Update folder
        val updated = created.copy(name = "Deep Science", icon = "science")
        fakeRepo.updateFolder(updated)

        val afterUpdate = fakeRepo.getFolder(created.id)
        assertNotNull(afterUpdate)
        assertEquals("Deep Science", afterUpdate?.name)
        assertEquals("science", afterUpdate?.icon)
        assertTrue(afterUpdate?.hasIcon == true)

        // 4. Delete folder
        fakeRepo.deleteFolder(created.id)
        assertTrue(fakeRepo.getFolders().isEmpty())
        assertNull(fakeRepo.getFolder(created.id))
    }

    @Test
    fun linkedGenreFallback_logicWhenAutoSyncEnabled() {
        fun resolveLinkedGenre(autoSync: Boolean, linkedGenreText: String, folderNameText: String): String? = if (autoSync) {
                linkedGenreText.trim().ifEmpty { folderNameText.trim() }.takeIf { it.isNotEmpty() }
            } else {
                null
            }

        // When autoSync is disabled -> null
        assertNull(resolveLinkedGenre(autoSync = false, linkedGenreText = "Tech", folderNameText = "My Tech"))

        // When autoSync is enabled and text provided -> uses text
        assertEquals("Tech", resolveLinkedGenre(autoSync = true, linkedGenreText = "Tech", folderNameText = "My Tech"))

        // When autoSync is enabled and text is empty -> falls back to folder name
        assertEquals("My Tech", resolveLinkedGenre(autoSync = true, linkedGenreText = "", folderNameText = "My Tech"))

        // When autoSync is enabled and both empty -> null
        assertNull(resolveLinkedGenre(autoSync = true, linkedGenreText = "", folderNameText = ""))
    }

    @Test
    fun quickFillChipIcon_autoSwitchesUnlessManuallySelected() {
        var isIconManuallySelected = false
        var selectedIconKey: String? = null

        fun onSelectSuggestedGenre(genre: String) {
            if (!isIconManuallySelected) {
                val matchedIcon = GenreIcons.findIcon(genre) ?: GenreIcons.defaultGenreIcon(genre)
                val item = GenreIcons.all.firstOrNull { it.icon == matchedIcon }
                selectedIconKey = item?.key
            }
        }

        // Tap News chip -> icon becomes news
        onSelectSuggestedGenre("News")
        assertEquals("news", selectedIconKey)

        // Switch to Comedy chip -> icon updates to comedy
        onSelectSuggestedGenre("Comedy")
        assertEquals("comedy", selectedIconKey)

        // User manually chooses star icon
        selectedIconKey = "star"
        isIconManuallySelected = true

        // Switch to Tech chip -> icon remains star
        onSelectSuggestedGenre("Technology")
        assertEquals("star", selectedIconKey)
    }

    @Test
    fun linkedGenre_doesNotFreezeWhenToggledBeforeTypingCompletes() {
        var autoSyncGenre = false
        val linkedGenreText = ""
        var nameText = "T"

        // User toggles autoSync ON early while typing
        autoSyncGenre = true

        // User finishes typing "Technology"
        nameText = "Technology"

        // Resolve effective linked genre
        val effectiveLinkedGenre = if (autoSyncGenre) {
            linkedGenreText.trim().ifEmpty { nameText.trim() }.takeIf { it.isNotEmpty() }
        } else {
            null
        }

        // Must dynamically evaluate to "Technology", NOT "T"
        assertEquals("Technology", effectiveLinkedGenre)
    }

    @Test
    fun folderSuggestions_prioritizeLibraryGenresAndFilterByKeyword() {
        val libraryGenres = listOf("Tech", "Comedy")
        val allFolderSuggestions = buildFolderSuggestionsWithLibrary(libraryGenres)

        // When query is blank, library genres are at the top
        val blankFiltered = filterGenreSuggestions("", allFolderSuggestions)
        assertEquals("Tech", blankFiltered[0].name)
        assertTrue(blankFiltered[0].isFromLibrary)
        assertEquals("Comedy", blankFiltered[1].name)
        assertTrue(blankFiltered[1].isFromLibrary)

        // Real-time keyword filter: typing "ai" matches Tech/Technology
        val aiMatches = filterGenreSuggestions("ai", allFolderSuggestions)
        assertTrue(aiMatches.isNotEmpty())
        assertTrue(aiMatches.any { it.name == "Tech" })
        assertFalse(aiMatches.any { it.name == "Technology" })

        // Real-time keyword filter: typing "jokes" matches Comedy
        val jokesMatches = filterGenreSuggestions("jokes", allFolderSuggestions)
        assertTrue(jokesMatches.isNotEmpty())
        assertEquals("Comedy", jokesMatches.first().name)
    }

    @Test
    fun folderSuggestions_selectingChipSetsNameAndPresetIcon() {
        val allFolderSuggestions = buildFolderSuggestionsWithLibrary(listOf("Tech", "Comedy"))
        var nameText = ""
        var selectedIconKey: String? = null

        fun onSelectSuggestion(suggestion: GenreSuggestion) {
            nameText = suggestion.name
            selectedIconKey = suggestion.iconKey
        }

        val comedySuggestion = allFolderSuggestions.first { it.name == "Comedy" }
        onSelectSuggestion(comedySuggestion)

        assertEquals("Comedy", nameText)
        assertEquals("comedy", selectedIconKey)
    }

    @Test
    fun folderName_disallowsTechnologyAndAllowsSwitchToTech() {
        var nameText = "Technology"
        var selectedIconKey: String? = null

        fun isTechnologyDisallowed(name: String) = name.trim().equals("Technology", ignoreCase = true)
        fun canSave(name: String) = name.trim().isNotEmpty() && !isTechnologyDisallowed(name)

        assertTrue(isTechnologyDisallowed(nameText))
        assertFalse(canSave(nameText))

        // Also case-insensitive check
        assertTrue(isTechnologyDisallowed("  technology  "))
        assertFalse(canSave("  technology  "))

        // Switch to Tech
        nameText = "Tech"
        selectedIconKey = "tech"

        assertFalse(isTechnologyDisallowed(nameText))
        assertTrue(canSave(nameText))
        assertEquals("Tech", nameText)
        assertEquals("tech", selectedIconKey)
    }

    @Test
    fun folderName_typingExactKeywordSwitchesIconAutomaticallyWithoutTap() {
        val allFolderSuggestions = buildFolderSuggestionsWithLibrary(listOf("Tech", "Comedy"))
        var nameText = ""
        var selectedIconKey: String? = null
        var isIconManuallySelected = false

        fun onNameChange(newName: String) {
            nameText = newName
            val trimmed = newName.trim()
            if (trimmed.isEmpty()) {
                isIconManuallySelected = false
                selectedIconKey = null
            } else if (!isIconManuallySelected) {
                val matchedKey = findExactGenreIconKey(trimmed, allFolderSuggestions)
                if (matchedKey != null) {
                    selectedIconKey = matchedKey
                }
            }
        }

        fun onSelectIcon(icon: String?) {
            selectedIconKey = icon
            isIconManuallySelected = true
        }

        // 1. Partial typing does not switch icon
        onNameChange("c")
        assertNull(selectedIconKey)
        onNameChange("co")
        assertNull(selectedIconKey)
        onNameChange("comed")
        assertNull(selectedIconKey)

        // 2. Exact genre match automatically switches icon without tapping
        onNameChange("comedy")
        assertEquals("comedy", selectedIconKey)

        // 3. Refining name to a multi-word phrase retains the matched genre icon
        onNameChange("comedy shows")
        assertEquals("comedy", selectedIconKey)

        // 4. Clearing text resets icon to null
        onNameChange("")
        assertNull(selectedIconKey)
        assertFalse(isIconManuallySelected)

        // 5. Typing exact keyword for sports switches icon to sports
        onNameChange("football")
        assertEquals("sports", selectedIconKey)

        // 6. Typing another keyword like "tech" switches to tech
        onNameChange("tech")
        assertEquals("tech", selectedIconKey)

        // 7. Typing "technology" switches to tech icon
        onNameChange("technology")
        assertEquals("tech", selectedIconKey)

        // 8. Manual icon selection locks icon so typing doesn't overwrite it
        onSelectIcon("star")
        assertEquals("star", selectedIconKey)
        assertTrue(isIconManuallySelected)

        onNameChange("comedy")
        assertEquals("star", selectedIconKey) // Preserves manual choice
    }

    @Test
    fun folderDisplaySize_placementProperties() {
        assertFalse(FolderDisplaySize.COMPACT.isPinnedToTop)
        assertEquals("Can be placed anywhere in the grid", FolderDisplaySize.COMPACT.placementLabel)

        val nonCompactSizes = listOf(
            FolderDisplaySize.WIDE,
            FolderDisplaySize.FEATURED,
            FolderDisplaySize.LARGE,
            FolderDisplaySize.SHELF,
            FolderDisplaySize.PANEL,
            FolderDisplaySize.SHOWCASE,
        )

        nonCompactSizes.forEach { size ->
            assertTrue(size.isPinnedToTop)
            assertEquals("Pinned to the top of the page", size.placementLabel)
        }
    }

    @Test
    fun defaultFolderDisplaySize_isShelf3x1() {
        assertEquals(FolderDisplaySize.SHELF, DefaultFolderDisplaySize)
        assertEquals("3×1", DefaultFolderDisplaySize.dimensionsLabel)
    }
}
