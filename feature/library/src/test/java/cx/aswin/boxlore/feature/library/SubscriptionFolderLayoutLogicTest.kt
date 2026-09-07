package cx.aswin.boxlore.feature.library

import cx.aswin.boxlore.core.model.Episode
import cx.aswin.boxlore.core.model.FolderDisplaySize
import cx.aswin.boxlore.core.model.Podcast
import cx.aswin.boxlore.core.model.SubscriptionFolder
import cx.aswin.boxlore.feature.library.subscriptions.FolderInterSort
import cx.aswin.boxlore.feature.library.subscriptions.FolderIntraSort
import cx.aswin.boxlore.feature.library.subscriptions.buildUnifiedGridItems
import cx.aswin.boxlore.feature.library.subscriptions.calculateFolderSlots
import cx.aswin.boxlore.feature.library.subscriptions.countFolderOverflowNew
import cx.aswin.boxlore.feature.library.subscriptions.filterFoldersByGenre
import cx.aswin.boxlore.feature.library.subscriptions.hasAnyFolderShowNew
import cx.aswin.boxlore.feature.library.subscriptions.hasFolderOverflowNew
import cx.aswin.boxlore.feature.library.subscriptions.partitionSubscribedShows
import cx.aswin.boxlore.feature.library.subscriptions.truncateCompactFolderName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubscriptionFolderLayoutLogicTest {

    private fun mockPodcast(
        id: String,
        title: String = "Podcast $id",
        genre: String = "Technology",
        customGenre: String? = null,
        latestPubDate: Long = 0L,
        rssHasNewEpisodes: Boolean = false,
    ) = Podcast(
        id = id,
        title = title,
        artist = "Host $id",
        imageUrl = "https://example.com/$id.jpg",
        genre = genre,
        customGenre = customGenre,
        rssHasNewEpisodes = rssHasNewEpisodes,
        latestEpisode = if (latestPubDate > 0L) {
            Episode(
                id = "ep-$id",
                podcastId = id,
                title = "Episode $id",
                description = "Description $id",
                audioUrl = "https://example.com/audio-$id.mp3",
                duration = 1800,
                publishedDate = latestPubDate,
            )
        } else {
            null
        },
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

    @Test
    fun `active folder show resolution preserves folder order and filters missing shows`() {
        val p1 = mockPodcast("p-1", genre = "Tech")
        val p2 = mockPodcast("p-2", genre = "Tech")
        val allPodcasts = listOf(p1, p2)
        val podcastsById = allPodcasts.associateBy { it.id }

        val folder = SubscriptionFolder(
            id = "f-1",
            name = "Tech Favorites",
            podcastIds = listOf("p-2", "p-999", "p-1"), // "p-999" is not subscribed/missing
        )

        val resolvedShows = folder.podcastIds.mapNotNull(podcastsById::get)
        assertEquals(2, resolvedShows.size)
        assertEquals("p-2", resolvedShows[0].id)
        assertEquals("p-1", resolvedShows[1].id)

        // Empty folder
        val emptyFolder = SubscriptionFolder(id = "f-empty", name = "Empty", podcastIds = emptyList())
        val emptyShows = emptyFolder.podcastIds.mapNotNull(podcastsById::get)
        assertTrue(emptyShows.isEmpty())
    }

    @Test
    fun `buildUnifiedGridItems interleaves compact folders and podcasts in manual order`() {
        val p1 = mockPodcast("p-1", title = "Alpha")
        val p2 = mockPodcast("p-2", title = "Beta")
        val p3 = mockPodcast("p-3", title = "Gamma")

        val folder1 = SubscriptionFolder(id = "f-1", name = "Tech", displaySize = FolderDisplaySize.COMPACT)
        val folder2 = SubscriptionFolder(id = "f-2", name = "Comedy", displaySize = FolderDisplaySize.COMPACT)

        val unfiled = listOf(p1, p2, p3)
        val folders = listOf(folder1, folder2)
        val byFolderId = mapOf("f-1" to emptyList<Podcast>(), "f-2" to emptyList<Podcast>())

        // Default order (non-manual): folders first, then unfiled podcasts
        val defaultItems = buildUnifiedGridItems(
            compactFolders = folders,
            unfiledPodcasts = unfiled,
            podcastsByFolderId = byFolderId,
            isManualSort = false,
        )
        assertEquals(
            listOf("folder:f-1", "folder:f-2", "p-1", "p-2", "p-3"),
            defaultItems.map { it.key },
        )

        // Manual order: custom interleaving
        val manualOrder = listOf("p-2", "folder:f-2", "p-1", "folder:f-1")
        val manualItems = buildUnifiedGridItems(
            compactFolders = folders,
            unfiledPodcasts = unfiled,
            podcastsByFolderId = byFolderId,
            manualOrder = manualOrder,
            isManualSort = true,
        )
        // p-3 was not in manualOrder, so it appends at the end
        assertEquals(
            listOf("p-2", "folder:f-2", "p-1", "folder:f-1", "p-3"),
            manualItems.map { it.key },
        )
    }

    @Test
    fun `partitionSubscribedShows sorts in-folder shows to match active podcast sort order`() {
        // Feed in sorted podcasts (e.g. sorted by Recently Updated: p3 newest, then p1, then p2)
        val p3 = mockPodcast("p-3", latestPubDate = 3000L)
        val p1 = mockPodcast("p-1", latestPubDate = 2000L)
        val p2 = mockPodcast("p-2", latestPubDate = 1000L)
        val sortedPodcasts = listOf(p3, p1, p2)

        // Folder has podcastIds in arbitrary unsorted order: p1, p2, p3
        val folder = SubscriptionFolder(
            id = "f-1",
            name = "Folder",
            displaySize = FolderDisplaySize.SHELF,
            podcastIds = listOf("p-1", "p-2", "p-3"),
        )

        val partition = partitionSubscribedShows(
            podcasts = sortedPodcasts,
            folders = listOf(folder),
            sort = SubscriptionSort.RecentlyUpdated,
        )

        val inFolderShows = partition.podcastsByFolderId["f-1"].orEmpty()
        assertEquals(listOf("p-3", "p-1", "p-2"), inFolderShows.map { it.id })
    }

    @Test
    fun `partitionSubscribedShows sorts pinned and compact folders according to active sort`() {
        val pOld = mockPodcast("p-old", title = "Old Show", latestPubDate = 1000L)
        val pNew = mockPodcast("p-new", title = "New Show", latestPubDate = 5000L)
        val podcasts = listOf(pNew, pOld)

        val fShelfZ = SubscriptionFolder(
            id = "f-z",
            name = "Zeta Shelf",
            displaySize = FolderDisplaySize.SHELF,
            podcastIds = listOf("p-old"), // max pub date 1000L
        )
        val fShelfA = SubscriptionFolder(
            id = "f-a",
            name = "Alpha Shelf",
            displaySize = FolderDisplaySize.SHELF,
            podcastIds = listOf("p-new"), // max pub date 5000L
        )
        val fCompactZ = SubscriptionFolder(
            id = "c-z",
            name = "Zeta Compact",
            displaySize = FolderDisplaySize.COMPACT,
            podcastIds = listOf("p-old"),
        )
        val fCompactA = SubscriptionFolder(
            id = "c-a",
            name = "Alpha Compact",
            displaySize = FolderDisplaySize.COMPACT,
            podcastIds = listOf("p-new"),
        )

        val allFolders = listOf(fShelfZ, fShelfA, fCompactZ, fCompactA)

        // 1. RecentlyUpdated: newest episode date first
        val recentPartition = partitionSubscribedShows(
            podcasts = podcasts,
            folders = allFolders,
            sort = SubscriptionSort.RecentlyUpdated,
        )
        assertEquals(listOf("f-a", "f-z"), recentPartition.pinnedFolders.map { it.id })
        assertEquals(listOf("c-a", "c-z"), recentPartition.compactFolders.map { it.id })

        // 2. Alphabetical: A-Z by name
        val alphaPartition = partitionSubscribedShows(
            podcasts = podcasts,
            folders = allFolders,
            sort = SubscriptionSort.Alphabetical,
        )
        assertEquals(listOf("f-a", "f-z"), alphaPartition.pinnedFolders.map { it.id })
        assertEquals(listOf("c-a", "c-z"), alphaPartition.compactFolders.map { it.id })
    }

    @Test
    fun `hasFolderOverflowNew and hasAnyFolderShowNew detect new episodes correctly`() {
        val showWithNew = mockPodcast("p-new", rssHasNewEpisodes = true)
        val showWithoutNew = mockPodcast("p-old", rssHasNewEpisodes = false)

        val lastSeenEpisodes = mapOf("p-old" to "ep-p-old")

        // Overflow shows detection
        val overflowWithNew = listOf(showWithoutNew, showWithNew)
        val overflowWithoutNew = listOf(showWithoutNew)
        val overflowWithMultipleNew = listOf(showWithNew, showWithNew.copy(id = "p-new2"), showWithoutNew)

        assertTrue(hasFolderOverflowNew(overflowWithNew, lastSeenEpisodes))
        assertFalse(hasFolderOverflowNew(overflowWithoutNew, lastSeenEpisodes))
        assertEquals(1, countFolderOverflowNew(overflowWithNew, lastSeenEpisodes))
        assertEquals(0, countFolderOverflowNew(overflowWithoutNew, lastSeenEpisodes))
        assertEquals(2, countFolderOverflowNew(overflowWithMultipleNew, lastSeenEpisodes))

        // Any folder show detection (for 1x1 compact folders)
        assertTrue(hasAnyFolderShowNew(listOf(showWithNew), lastSeenEpisodes))
        assertFalse(hasAnyFolderShowNew(listOf(showWithoutNew), lastSeenEpisodes))
    }

    @Test
    fun `truncateCompactFolderName limits names exceeding max length with ellipsis`() {
        // <= 10 chars remain untouched
        assertEquals("Tech", truncateCompactFolderName("Tech"))
        assertEquals("Technology", truncateCompactFolderName("Technology"))
        assertEquals("1234567890", truncateCompactFolderName("1234567890"))

        // > 10 chars are truncated to first 10 + ellipsis
        assertEquals("Technology…", truncateCompactFolderName("Technology News"))
        assertEquals("My Favorit…", truncateCompactFolderName("My Favorite Shows"))
    }

    @Test
    fun `partitionSubscribedShows respects FolderInterSort options`() {
        val p1 = mockPodcast("p-1", latestPubDate = 5000L)
        val p2 = mockPodcast("p-2", latestPubDate = 2000L)
        val p3 = mockPodcast("p-3", latestPubDate = 8000L)
        val podcasts = listOf(p1, p2, p3)

        val folderA = SubscriptionFolder(
            id = "f-a",
            name = "Alpha",
            displaySize = FolderDisplaySize.SHELF,
            podcastIds = listOf("p-1"), // pub date 5000
        )
        val folderB = SubscriptionFolder(
            id = "f-b",
            name = "Beta",
            displaySize = FolderDisplaySize.SHELF,
            podcastIds = listOf("p-2", "p-3"), // pub date 8000, 2 shows
        )
        val folders = listOf(folderA, folderB)

        // Inter-sort: RecentlyUpdated -> folderB (8000) then folderA (5000)
        val recentPartition = partitionSubscribedShows(
            podcasts = podcasts,
            folders = folders,
            folderSort = FolderInterSort.RecentlyUpdated,
        )
        assertEquals(listOf("f-b", "f-a"), recentPartition.pinnedFolders.map { it.id })

        // Inter-sort: Alphabetical -> folderA (Alpha) then folderB (Beta)
        val alphaPartition = partitionSubscribedShows(
            podcasts = podcasts,
            folders = folders,
            folderSort = FolderInterSort.Alphabetical,
        )
        assertEquals(listOf("f-a", "f-b"), alphaPartition.pinnedFolders.map { it.id })

        // Inter-sort: MostShows -> folderB (2 shows) then folderA (1 show)
        val mostShowsPartition = partitionSubscribedShows(
            podcasts = podcasts,
            folders = folders,
            folderSort = FolderInterSort.MostShows,
        )
        assertEquals(listOf("f-b", "f-a"), mostShowsPartition.pinnedFolders.map { it.id })
    }

    @Test
    fun `partitionSubscribedShows respects FolderIntraSort options`() {
        val pA = mockPodcast("p-a", title = "Zebra", latestPubDate = 1000L)
        val pB = mockPodcast("p-b", title = "Apple", latestPubDate = 9000L)
        val pC = mockPodcast("p-c", title = "Mango", latestPubDate = 5000L)
        val podcasts = listOf(pA, pB, pC)

        val folder = SubscriptionFolder(
            id = "f-test",
            name = "Test Folder",
            displaySize = FolderDisplaySize.SHELF,
            podcastIds = listOf("p-c", "p-a", "p-b"), // Manual order: C, A, B
        )

        // Intra-sort: Alphabetical -> Apple (B), Mango (C), Zebra (A)
        val alphaPartition = partitionSubscribedShows(
            podcasts = podcasts,
            folders = listOf(folder),
            intraFolderSort = FolderIntraSort.Alphabetical,
        )
        assertEquals(listOf("p-b", "p-c", "p-a"), alphaPartition.podcastsByFolderId["f-test"]?.map { it.id })

        // Intra-sort: RecentlyUpdated -> Apple (9000), Mango (5000), Zebra (1000)
        val recentPartition = partitionSubscribedShows(
            podcasts = podcasts,
            folders = listOf(folder),
            intraFolderSort = FolderIntraSort.RecentlyUpdated,
        )
        assertEquals(listOf("p-b", "p-c", "p-a"), recentPartition.podcastsByFolderId["f-test"]?.map { it.id })

        // Intra-sort: Manual -> folder.podcastIds order: C, A, B
        val manualPartition = partitionSubscribedShows(
            podcasts = podcasts,
            folders = listOf(folder),
            intraFolderSort = FolderIntraSort.Manual,
        )
        assertEquals(listOf("p-c", "p-a", "p-b"), manualPartition.podcastsByFolderId["f-test"]?.map { it.id })

        // Intra-sort: Inherit with Alphabetical show sort
        val inheritAlpha = partitionSubscribedShows(
            podcasts = podcasts,
            folders = listOf(folder),
            sort = SubscriptionSort.Alphabetical,
            intraFolderSort = FolderIntraSort.Inherit,
        )
        assertEquals(listOf("p-b", "p-c", "p-a"), inheritAlpha.podcastsByFolderId["f-test"]?.map { it.id })
    }
}
