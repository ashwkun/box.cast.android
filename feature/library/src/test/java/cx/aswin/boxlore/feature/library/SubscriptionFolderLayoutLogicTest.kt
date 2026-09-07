package cx.aswin.boxlore.feature.library

import cx.aswin.boxlore.core.model.FolderDisplaySize
import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.model.SubscriptionFolder
import cx.aswin.boxlore.feature.library.subscriptions.calculateFolderSlots
import cx.aswin.boxlore.feature.library.subscriptions.filterFoldersByGenre
import cx.aswin.boxlore.feature.library.subscriptions.partitionSubscribedShows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubscriptionFolderLayoutLogicTest {

    private fun mockPodcast(id: String, genre: String = "Technology", customGenre: String? = null) = Podcast(
        id = id,
        title = "Podcast $id",
        artist = "Host $id",
        imageUrl = "https://example.com/$id.jpg",
        genre = genre,
        customGenre = customGenre,
    )

    @Test
    fun `calculateFolderSlots compact size fits up to 4 shows`() {
        val shows = (1..3).map { mockPodcast(it.toString()) }
        val slots = calculateFolderSlots(shows, FolderDisplaySize.COMPACT)

        assertEquals(3, slots.visibleShows.size)
        assertEquals(0, slots.overflowShows.size)
        assertFalse(slots.hasOverflow)
        assertEquals(3, slots.totalCount)

        // Exactly 4 shows
        val fourShows = (1..4).map { mockPodcast(it.toString()) }
        val fourSlots = calculateFolderSlots(fourShows, FolderDisplaySize.COMPACT)
        assertEquals(4, fourSlots.visibleShows.size)
        assertFalse(fourSlots.hasOverflow)

        // 6 shows -> 3 visible + 3 overflow
        val sixShows = (1..6).map { mockPodcast(it.toString()) }
        val sixSlots = calculateFolderSlots(sixShows, FolderDisplaySize.COMPACT)
        assertEquals(3, sixSlots.visibleShows.size)
        assertEquals(3, sixSlots.overflowShows.size)
        assertTrue(sixSlots.hasOverflow)
        assertEquals(3, sixSlots.overflowCount)
        assertEquals(6, sixSlots.totalCount)
    }

    @Test
    fun `calculateFolderSlots shelf 3x1 size fits 3 shows before overflow`() {
        val twoShows = (1..2).map { mockPodcast(it.toString()) }
        val twoSlots = calculateFolderSlots(twoShows, FolderDisplaySize.SHELF)
        assertEquals(2, twoSlots.visibleShows.size)
        assertFalse(twoSlots.hasOverflow)

        val threeShows = (1..3).map { mockPodcast(it.toString()) }
        val threeSlots = calculateFolderSlots(threeShows, FolderDisplaySize.SHELF)
        assertEquals(3, threeSlots.visibleShows.size)
        assertFalse(threeSlots.hasOverflow)

        val fiveShows = (1..5).map { mockPodcast(it.toString()) }
        val fiveSlots = calculateFolderSlots(fiveShows, FolderDisplaySize.SHELF)
        assertEquals(2, fiveSlots.visibleShows.size)
        assertEquals(3, fiveSlots.overflowShows.size)
        assertTrue(fiveSlots.hasOverflow)
        assertEquals(3, fiveSlots.overflowCount)
    }

    @Test
    fun `calculateFolderSlots panel 3x2 size fits 6 shows before overflow`() {
        val sixShows = (1..6).map { mockPodcast(it.toString()) }
        val sixSlots = calculateFolderSlots(sixShows, FolderDisplaySize.PANEL)
        assertEquals(6, sixSlots.visibleShows.size)
        assertFalse(sixSlots.hasOverflow)

        val tenShows = (1..10).map { mockPodcast(it.toString()) }
        val tenSlots = calculateFolderSlots(tenShows, FolderDisplaySize.PANEL)
        assertEquals(5, tenSlots.visibleShows.size)
        assertEquals(5, tenSlots.overflowShows.size)
        assertTrue(tenSlots.hasOverflow)
        assertEquals(5, tenSlots.overflowCount)
    }

    @Test
    fun `calculateFolderSlots showcase 3x3 size fits 9 shows before overflow`() {
        val nineShows = (1..9).map { mockPodcast(it.toString()) }
        val nineSlots = calculateFolderSlots(nineShows, FolderDisplaySize.SHOWCASE)
        assertEquals(9, nineSlots.visibleShows.size)
        assertFalse(nineSlots.hasOverflow)

        val twelveShows = (1..12).map { mockPodcast(it.toString()) }
        val twelveSlots = calculateFolderSlots(twelveShows, FolderDisplaySize.SHOWCASE)
        assertEquals(8, twelveSlots.visibleShows.size)
        assertEquals(4, twelveSlots.overflowShows.size)
        assertTrue(twelveSlots.hasOverflow)
    }

    @Test
    fun `partitionSubscribedShows correctly separates pinned folders, compact folders, and unfiled shows`() {
        val podcasts = (1..10).map { mockPodcast(it.toString()) }

        val pinnedFolder = SubscriptionFolder(
            id = "pinned-1",
            name = "Tech Shelf",
            displaySize = FolderDisplaySize.SHELF,
            podcastIds = listOf("1", "2", "3"),
        )
        val compactFolder = SubscriptionFolder(
            id = "compact-1",
            name = "Comedy 1x1",
            displaySize = FolderDisplaySize.COMPACT,
            podcastIds = listOf("4", "5"),
        )

        val partition = partitionSubscribedShows(
            podcasts = podcasts,
            folders = listOf(pinnedFolder, compactFolder),
        )

        assertEquals(1, partition.pinnedFolders.size)
        assertEquals("pinned-1", partition.pinnedFolders.first().id)

        assertEquals(1, partition.compactFolders.size)
        assertEquals("compact-1", partition.compactFolders.first().id)

        // Shows 1, 2, 3, 4, 5 are filed into folders. Shows 6..10 remain unfiled in root grid.
        assertEquals(5, partition.unfiledPodcasts.size)
        assertEquals(listOf("6", "7", "8", "9", "10"), partition.unfiledPodcasts.map { it.id })

        // Check podcast map per folder
        assertEquals(3, partition.podcastsByFolderId["pinned-1"]?.size)
        assertEquals(2, partition.podcastsByFolderId["compact-1"]?.size)
    }

    @Test
    fun `filterFoldersByGenre matches by folder name, linked genre, or contained podcasts`() {
        val p1 = mockPodcast("1", genre = "Technology")
        val p2 = mockPodcast("2", genre = "Comedy")
        val p3 = mockPodcast("3", genre = "News", customGenre = "Tech")
        val allPodcasts = listOf(p1, p2, p3)

        val techFolderByName = SubscriptionFolder(
            id = "f-1",
            name = "Technology",
            podcastIds = listOf("1"),
        )
        val comedyFolderByLinkedGenre = SubscriptionFolder(
            id = "f-2",
            name = "Funny Stuff",
            linkedGenre = "Comedy",
            podcastIds = listOf("2"),
        )
        val customTagFolderByMember = SubscriptionFolder(
            id = "f-3",
            name = "My Mixed Bag",
            podcastIds = listOf("3"), // Member p3 has customGenre = "Tech"
        )
        val folders = listOf(techFolderByName, comedyFolderByLinkedGenre, customTagFolderByMember)

        // "All" returns all folders
        assertEquals(3, filterFoldersByGenre(folders, "All", allPodcasts).size)
        assertEquals(3, filterFoldersByGenre(folders, "", allPodcasts).size)

        // "Comedy" matches f-2
        val comedyMatches = filterFoldersByGenre(folders, "Comedy", allPodcasts)
        assertEquals(1, comedyMatches.size)
        assertEquals("f-2", comedyMatches.first().id)

        // "Tech" matches f-1 (by name) and f-3 (by member podcast custom tag)
        val techMatches = filterFoldersByGenre(folders, "Tech", allPodcasts)
        assertEquals(2, techMatches.size)
        assertTrue(techMatches.any { it.id == "f-1" })
        assertTrue(techMatches.any { it.id == "f-3" })
    }
}
