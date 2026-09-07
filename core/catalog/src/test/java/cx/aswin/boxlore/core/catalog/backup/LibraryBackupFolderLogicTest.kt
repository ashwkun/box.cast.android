package cx.aswin.boxlore.core.catalog.backup

import com.google.gson.Gson
import cx.aswin.boxlore.core.catalog.FolderRepository
import cx.aswin.boxlore.core.model.FolderDisplaySize
import cx.aswin.boxlore.core.model.SubscriptionFolder
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibraryBackupFolderLogicTest {

    private val gson = Gson()

    private class TestFolderRepository : FolderRepository {
        val restoredList = mutableListOf<SubscriptionFolder>()
        private val _folders = MutableStateFlow<List<SubscriptionFolder>>(emptyList())
        override val folders: Flow<List<SubscriptionFolder>> = _folders
        override val folderNames: Flow<List<String>> = _folders.map { list -> list.map { it.name } }

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
                icon = icon,
                displaySize = displaySize,
                linkedGenre = linkedGenre,
                showPodcastGrid = showPodcastGrid,
                podcastCount = podcastIds.size,
                podcastIds = podcastIds,
            )
            _folders.update { it + folder }
            return folder
        }

        override suspend fun restoreFolder(folder: SubscriptionFolder): SubscriptionFolder {
            restoredList.add(folder)
            _folders.update { list -> list.filter { it.id != folder.id } + folder }
            return folder
        }

        override suspend fun updateFolder(folder: SubscriptionFolder) {}
        override suspend fun deleteFolder(folderId: String) {}
        override suspend fun addPodcastToFolder(podcastId: String, folderId: String) {}
        override suspend fun removePodcastFromFolder(podcastId: String, folderId: String) {}
        override suspend fun removePodcastFromAllFolders(podcastId: String) {}
        override suspend fun setPodcastsForFolder(folderId: String, podcastIds: List<String>) {}
        override suspend fun syncLinkedGenres() {}
        override suspend fun autoOrganizeSubscribedShows(
            defaultDisplaySize: FolderDisplaySize?,
            showPodcastGrid: Boolean,
        ) {}
    }

    @Test
    fun `toBackup correctly maps domain SubscriptionFolder to backup DTO`() {
        val folder = SubscriptionFolder(
            id = "f-123",
            name = "Tech News",
            icon = "tech",
            displaySize = FolderDisplaySize.SHELF,
            linkedGenre = "Tech",
            showPodcastGrid = true,
            createdAt = 1700000000000L,
            podcastCount = 3,
            podcastIds = listOf("pod-1", "pod-2", "pod-3"),
        )

        val backup = LibraryBackupFolderLogic.toBackup(folder)
        assertEquals("f-123", backup.id)
        assertEquals("Tech News", backup.name)
        assertEquals("tech", backup.icon)
        assertEquals("SHELF", backup.displaySize)
        assertEquals("Tech", backup.linkedGenre)
        assertTrue(backup.showPodcastGrid)
        assertEquals(1700000000000L, backup.createdAt)
        assertEquals(listOf("pod-1", "pod-2", "pod-3"), backup.podcastIds)
    }

    @Test
    fun `gson round trips SubscriptionFolderBackup successfully`() {
        val backup = SubscriptionFolderBackup(
            id = "folder-abc",
            name = "Comedy Highlights",
            icon = "comedy",
            displaySize = "PANEL",
            linkedGenre = "Comedy",
            showPodcastGrid = false,
            createdAt = 1700000050000L,
            podcastIds = listOf("show-1", "show-2"),
        )

        val json = gson.toJson(backup)
        val parsed = gson.fromJson(json, SubscriptionFolderBackup::class.java)

        assertEquals(backup.id, parsed.id)
        assertEquals(backup.name, parsed.name)
        assertEquals(backup.icon, parsed.icon)
        assertEquals(backup.displaySize, parsed.displaySize)
        assertEquals(backup.linkedGenre, parsed.linkedGenre)
        assertEquals(backup.showPodcastGrid, parsed.showPodcastGrid)
        assertEquals(backup.createdAt, parsed.createdAt)
        assertEquals(backup.podcastIds, parsed.podcastIds)
    }

    @Test
    fun `restoreFolders invokes repository restoreFolder with sanitized Tech mapping`() = runTest {
        val repo = TestFolderRepository()
        val backups = listOf(
            SubscriptionFolderBackup(
                id = "folder-tech",
                name = "Technology",
                icon = null,
                displaySize = "SHELF",
                linkedGenre = "Technology",
                showPodcastGrid = true,
                createdAt = 1000L,
                podcastIds = listOf("pod-1"),
            ),
            SubscriptionFolderBackup(
                id = "folder-sci",
                name = "Science",
                icon = "science",
                displaySize = "SHOWCASE",
                linkedGenre = "Science",
                showPodcastGrid = false,
                createdAt = 2000L,
                podcastIds = listOf("pod-2", "pod-3"),
            ),
        )

        LibraryBackupFolderLogic.restoreFolders(backups, repo)

        assertEquals(2, repo.restoredList.size)

        val restoredTech = repo.restoredList[0]
        assertEquals("folder-tech", restoredTech.id)
        assertEquals("Tech", restoredTech.name)
        assertEquals("tech", restoredTech.icon)
        assertEquals(FolderDisplaySize.SHELF, restoredTech.displaySize)
        assertEquals("Tech", restoredTech.linkedGenre)
        assertTrue(restoredTech.showPodcastGrid)
        assertEquals(1000L, restoredTech.createdAt)
        assertEquals(listOf("pod-1"), restoredTech.podcastIds)

        val restoredSci = repo.restoredList[1]
        assertEquals("folder-sci", restoredSci.id)
        assertEquals("Science", restoredSci.name)
        assertEquals("science", restoredSci.icon)
        assertEquals(FolderDisplaySize.SHOWCASE, restoredSci.displaySize)
        assertFalse(restoredSci.showPodcastGrid)
        assertEquals(listOf("pod-2", "pod-3"), restoredSci.podcastIds)
    }

    @Test
    fun `restoreFolders skips blank names and gracefully handles invalid display sizes`() = runTest {
        val repo = TestFolderRepository()
        val backups = listOf(
            SubscriptionFolderBackup(
                id = "blank",
                name = "   ",
                podcastIds = listOf("pod-1"),
            ),
            SubscriptionFolderBackup(
                id = "invalid-size",
                name = "News",
                displaySize = "INVALID_SIZE_HERE",
                podcastIds = listOf("pod-news"),
            ),
        )

        LibraryBackupFolderLogic.restoreFolders(backups, repo)

        assertEquals(1, repo.restoredList.size)
        val restored = repo.restoredList[0]
        assertEquals("invalid-size", restored.id)
        assertEquals("News", restored.name)
        assertEquals(FolderDisplaySize.COMPACT, restored.displaySize)
    }

    @Test
    fun `restoreFolders handles null or empty list safely`() = runTest {
        val repo = TestFolderRepository()
        LibraryBackupFolderLogic.restoreFolders(null, repo)
        LibraryBackupFolderLogic.restoreFolders(emptyList(), repo)
        assertTrue(repo.restoredList.isEmpty())
    }
}
