package cx.aswin.boxlore.core.catalog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cx.aswin.boxlore.core.database.BoxLoreDatabase
import cx.aswin.boxlore.core.database.FolderDao
import cx.aswin.boxlore.core.database.PodcastDao
import cx.aswin.boxlore.core.database.PodcastEntity
import cx.aswin.boxlore.core.model.FolderDisplaySize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests verifying the [FolderRepository] contract.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderRepositoryTest {
    private lateinit var database: BoxLoreDatabase
    private lateinit var folderDao: FolderDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var repository: FolderRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, BoxLoreDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        folderDao = database.folderDao()
        podcastDao = database.podcastDao()
        repository = RoomFolderRepository(folderDao, podcastDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertPodcast(
        id: String,
        title: String,
        genre: String? = null,
        customGenre: String? = null,
    ) {
        podcastDao.upsert(
            PodcastEntity(
                podcastId = id,
                title = title,
                author = "Author",
                imageUrl = "https://example.com/art.jpg",
                description = "Desc",
                isSubscribed = true,
                genre = genre,
                customGenre = customGenre,
            ),
        )
    }

    @Test
    fun createFolder_supportsOptionalIcon() = runTest {
        val folder = repository.createFolder(
            name = "No Icon Folder",
            icon = null,
            displaySize = FolderDisplaySize.COMPACT,
        )

        assertNotNull(folder.id)
        assertEquals("No Icon Folder", folder.name)
        assertNull(folder.icon)
        assertFalse(folder.hasIcon)

        val retrieved = repository.getFolder(folder.id)
        assertNotNull(retrieved)
        assertNull(retrieved?.icon)
    }

    @Test
    fun observeFolderFlow_emitsUpdates() = runTest {
        val folder = repository.createFolder(
            name = "Tech Shows",
            icon = "tech",
            displaySize = FolderDisplaySize.FEATURED,
        )

        val flowFirst = repository.getFolderFlow(folder.id).first()
        assertNotNull(flowFirst)
        assertEquals("Tech Shows", flowFirst?.name)

        repository.updateFolder(folder.copy(name = "Updated Tech Shows"))
        val flowUpdated = repository.getFolderFlow(folder.id).first()
        assertEquals("Updated Tech Shows", flowUpdated?.name)
    }

    @Test
    fun deleteFolder_removesFolderAndPreservesPodcasts() = runTest {
        insertPodcast("pod-1", "Coding Daily")
        val folder = repository.createFolder(
            name = "Programming",
            podcastIds = listOf("pod-1"),
        )

        assertEquals(1, repository.getFolders().size)
        assertEquals(listOf("pod-1"), repository.getFolder(folder.id)?.podcastIds)

        repository.deleteFolder(folder.id)
        assertEquals(0, repository.getFolders().size)
        assertNull(repository.getFolder(folder.id))

        // Podcast entity remains intact
        val podcast = podcastDao.getPodcast("pod-1")
        assertNotNull(podcast)
        assertEquals("Coding Daily", podcast?.title)
    }

    @Test
    fun observeFolderNames_emitsSortedNames() = runTest {
        repository.createFolder(name = "Zebra")
        repository.createFolder(name = "Apple")
        repository.createFolder(name = "Mango")

        val names = repository.folderNames.first()
        assertEquals(listOf("Apple", "Mango", "Zebra"), names)
    }

    @Test
    fun createFolder_supportsShowPodcastGrid() = runTest {
        val folder = repository.createFolder(
            name = "Grid Folder",
            icon = "tech",
            displaySize = FolderDisplaySize.COMPACT,
            showPodcastGrid = true,
        )

        assertNotNull(folder.id)
        org.junit.Assert.assertTrue(folder.showPodcastGrid)
        org.junit.Assert.assertTrue(folder.effectiveShowPodcastGrid)

        val retrieved = repository.getFolder(folder.id)
        assertNotNull(retrieved)
        org.junit.Assert.assertTrue(retrieved?.showPodcastGrid == true)
    }

    @Test
    fun autoOrganizeSubscribedShows_createsDefaultShelfFoldersAndPopulatesShows() = runTest {
        insertPodcast("pod-1", "Laugh Out Loud", genre = "Comedy")
        insertPodcast("pod-2", "Silicon Talk", genre = "Technology")
        insertPodcast("pod-3", "Code Bytes", genre = "Technology")

        repository.autoOrganizeSubscribedShows(
            defaultDisplaySize = FolderDisplaySize.SHELF,
            showPodcastGrid = false,
        )

        val folders = repository.getFolders()
        assertEquals(2, folders.size)

        val techFolder = folders.firstOrNull { it.name == "Technology" }
        assertNotNull(techFolder)
        assertEquals(FolderDisplaySize.SHELF, techFolder?.displaySize)
        assertEquals(listOf("pod-2", "pod-3"), techFolder?.podcastIds?.sorted())
        assertEquals("tech", techFolder?.icon)

        val comedyFolder = folders.firstOrNull { it.name == "Comedy" }
        assertNotNull(comedyFolder)
        assertEquals(FolderDisplaySize.SHELF, comedyFolder?.displaySize)
        assertEquals(listOf("pod-1"), comedyFolder?.podcastIds)
        assertEquals("comedy", comedyFolder?.icon)
    }

    @Test
    fun autoOrganizeSubscribedShows_preservesExistingGenreAndCustomFolders() = runTest {
        insertPodcast("pod-1", "My Fav", genre = "Comedy")
        insertPodcast("pod-2", "Tech News", genre = "Technology")

        // 1. Existing custom non-genre folder
        val custom = repository.createFolder(
            name = "Favorites",
            icon = "star",
            displaySize = FolderDisplaySize.COMPACT,
            podcastIds = listOf("pod-1"),
        )

        // 2. Existing genre folder with custom size
        val existingTech = repository.createFolder(
            name = "Technology",
            icon = "custom_icon",
            displaySize = FolderDisplaySize.PANEL,
            linkedGenre = "Technology",
        )

        repository.autoOrganizeSubscribedShows()

        val allFolders = repository.getFolders()
        assertEquals(3, allFolders.size)

        // Custom folder is 100% untouched
        val retrievedCustom = repository.getFolder(custom.id)
        assertNotNull(retrievedCustom)
        assertEquals(FolderDisplaySize.COMPACT, retrievedCustom?.displaySize)
        assertEquals("star", retrievedCustom?.icon)
        assertEquals(listOf("pod-1"), retrievedCustom?.podcastIds)

        // Existing tech folder kept its size and icon and gained pod-2
        val retrievedTech = repository.getFolder(existingTech.id)
        assertNotNull(retrievedTech)
        assertEquals(FolderDisplaySize.PANEL, retrievedTech?.displaySize)
        assertEquals("custom_icon", retrievedTech?.icon)
        assertEquals(listOf("pod-2"), retrievedTech?.podcastIds)

        // Comedy was auto-created
        val comedy = allFolders.firstOrNull { it.name == "Comedy" }
        assertNotNull(comedy)
        assertEquals(listOf("pod-1"), comedy?.podcastIds)
    }

    @Test
    fun syncLinkedGenres_movesShowWhenGenreChanges() = runTest {
        val comedyFolder = repository.createFolder(
            name = "Comedy Shows",
            linkedGenre = "Comedy",
        )
        val techFolder = repository.createFolder(
            name = "Tech Hub",
            linkedGenre = "Tech",
        )
        val manualFolder = repository.createFolder(
            name = "My Commute",
            podcastIds = listOf("pod-1"),
        )

        insertPodcast("pod-1", "Dynamic Show", genre = "Comedy")

        repository.syncLinkedGenres()

        assertEquals(listOf("pod-1"), repository.getFolder(comedyFolder.id)?.podcastIds)
        assertEquals(emptyList<String>(), repository.getFolder(techFolder.id)?.podcastIds)
        assertEquals(listOf("pod-1"), repository.getFolder(manualFolder.id)?.podcastIds)

        // User edits genre from Comedy to Tech
        insertPodcast("pod-1", "Dynamic Show", genre = "Comedy", customGenre = "Tech")

        repository.syncLinkedGenres()

        // Removed from Comedy, added to Tech, preserved in manual folder
        assertEquals(emptyList<String>(), repository.getFolder(comedyFolder.id)?.podcastIds)
        assertEquals(listOf("pod-1"), repository.getFolder(techFolder.id)?.podcastIds)
        assertEquals(listOf("pod-1"), repository.getFolder(manualFolder.id)?.podcastIds)
    }
}
