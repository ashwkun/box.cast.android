package cx.aswin.boxlore.core.catalog

import cx.aswin.boxlore.core.database.FolderDao
import cx.aswin.boxlore.core.database.FolderEntity
import cx.aswin.boxlore.core.database.PodcastDao
import cx.aswin.boxlore.core.database.PodcastEntity
import cx.aswin.boxlore.core.database.PodcastFolderCrossRef
import cx.aswin.boxlore.core.model.FolderDisplaySize
import cx.aswin.boxlore.core.model.PodcastGenres
import cx.aswin.boxlore.core.model.SubscriptionFolder
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomFolderRepository(
    private val folderDao: FolderDao,
    private val podcastDao: PodcastDao,
    private val resolveGenreIconKey: ((String) -> String?)? = null,
) : FolderRepository {

    override val folders: Flow<List<SubscriptionFolder>> =
        combine(
            folderDao.getAllFolders(),
            folderDao.getAllCrossRefs(),
            podcastDao.getSubscribedPodcasts(),
        ) { folderEntities, crossRefs, subscribedPods ->
            val subscribedIds = subscribedPods.map { it.podcastId }.toSet()
            val refsByFolder = crossRefs
                .filter { it.podcastId in subscribedIds }
                .groupBy { it.folderId }
            folderEntities.map { entity ->
                val pIds = refsByFolder[entity.folderId]?.map { it.podcastId } ?: emptyList()
                SubscriptionFolder(
                    id = entity.folderId,
                    name = entity.name,
                    icon = entity.icon,
                    displaySize = entity.displaySize,
                    linkedGenre = entity.linkedGenre,
                    showPodcastGrid = entity.showPodcastGrid,
                    createdAt = entity.createdAt,
                    podcastCount = pIds.size,
                    podcastIds = pIds,
                )
            }
        }

    override val folderNames: Flow<List<String>> =
        folderDao.getAllFolderNames()

    override suspend fun getFolders(): List<SubscriptionFolder> {
        val entities = folderDao.getAllFoldersList()
        val subscribedIds = podcastDao.getSubscribedPodcastsList().map { it.podcastId }.toSet()
        val crossRefs = folderDao.getAllCrossRefsList()
            .filter { it.podcastId in subscribedIds }
            .groupBy { it.folderId }
        return entities.map { entity ->
            val pIds = crossRefs[entity.folderId]?.map { it.podcastId } ?: emptyList()
            SubscriptionFolder(
                id = entity.folderId,
                name = entity.name,
                icon = entity.icon,
                displaySize = entity.displaySize,
                linkedGenre = entity.linkedGenre,
                showPodcastGrid = entity.showPodcastGrid,
                createdAt = entity.createdAt,
                podcastCount = pIds.size,
                podcastIds = pIds,
            )
        }
    }

    override suspend fun getFolder(folderId: String): SubscriptionFolder? {
        val entity = folderDao.getFolder(folderId) ?: return null
        val subscribedIds = podcastDao.getSubscribedPodcastsList().map { it.podcastId }.toSet()
        val pIds = folderDao.getPodcastIdsForFolderList(folderId).filter { it in subscribedIds }
        return SubscriptionFolder(
            id = entity.folderId,
            name = entity.name,
            icon = entity.icon,
            displaySize = entity.displaySize,
            linkedGenre = entity.linkedGenre,
            showPodcastGrid = entity.showPodcastGrid,
            createdAt = entity.createdAt,
            podcastCount = pIds.size,
            podcastIds = pIds,
        )
    }

    override fun getFolderFlow(folderId: String): Flow<SubscriptionFolder?> =
        folders.map { list -> list.firstOrNull { it.id == folderId } }

    override suspend fun createFolder(
        name: String,
        icon: String?,
        displaySize: FolderDisplaySize,
        linkedGenre: String?,
        showPodcastGrid: Boolean,
        podcastIds: List<String>,
    ): SubscriptionFolder {
        val id = UUID.randomUUID().toString()
        val trimmedName = name.trim()
        val trimmedIcon = icon?.trim()?.takeIf { it.isNotEmpty() }
        val trimmedGenre = linkedGenre?.trim()?.takeIf { it.isNotEmpty() }
        val effectiveTargetGenre = trimmedGenre ?: PodcastGenres.canonicalize(trimmedName)?.let { trimmedName }

        val initialPodcastIds = podcastIds.toMutableList()
        if (effectiveTargetGenre != null) {
            val matchingSubscribed = podcastDao.getSubscribedPodcastsList()
                .filter { pod -> matchesGenre(pod, effectiveTargetGenre) }
                .map { it.podcastId }
            for (matchingId in matchingSubscribed) {
                if (matchingId !in initialPodcastIds) {
                    initialPodcastIds.add(matchingId)
                }
            }
        }

        val createdAt = System.currentTimeMillis()
        val entity = FolderEntity(
            folderId = id,
            name = trimmedName,
            icon = trimmedIcon,
            displaySize = displaySize,
            linkedGenre = trimmedGenre ?: effectiveTargetGenre,
            showPodcastGrid = showPodcastGrid,
            createdAt = createdAt,
        )
        folderDao.upsertFolder(entity)
        if (initialPodcastIds.isNotEmpty()) {
            folderDao.setPodcastsForFolder(id, initialPodcastIds)
        }

        return SubscriptionFolder(
            id = id,
            name = trimmedName,
            icon = trimmedIcon,
            displaySize = displaySize,
            linkedGenre = trimmedGenre ?: effectiveTargetGenre,
            showPodcastGrid = showPodcastGrid,
            createdAt = createdAt,
            podcastCount = initialPodcastIds.size,
            podcastIds = initialPodcastIds,
        )
    }

    override suspend fun updateFolder(folder: SubscriptionFolder) {
        val trimmedName = folder.name.trim()
        val trimmedIcon = folder.icon?.trim()?.takeIf { it.isNotEmpty() }
        val trimmedGenre = folder.linkedGenre?.trim()?.takeIf { it.isNotEmpty() }
        val effectiveTargetGenre = trimmedGenre ?: PodcastGenres.canonicalize(trimmedName)?.let { trimmedName }

        val entity = FolderEntity(
            folderId = folder.id,
            name = trimmedName,
            icon = trimmedIcon,
            displaySize = folder.displaySize,
            linkedGenre = trimmedGenre ?: effectiveTargetGenre,
            showPodcastGrid = folder.showPodcastGrid,
            createdAt = if (folder.createdAt > 0L) folder.createdAt else System.currentTimeMillis(),
        )
        folderDao.upsertFolder(entity)

        if (effectiveTargetGenre != null) {
            val existingIds = folderDao.getPodcastIdsForFolderList(folder.id).toMutableSet()
            val matchingSubscribed = podcastDao.getSubscribedPodcastsList()
                .filter { pod -> matchesGenre(pod, effectiveTargetGenre) }
                .map { it.podcastId }
            val added = matchingSubscribed.filter { existingIds.add(it) }
            if (added.isNotEmpty()) {
                folderDao.setPodcastsForFolder(folder.id, existingIds.toList())
            }
        }
    }

    override suspend fun deleteFolder(folderId: String) {
        folderDao.deleteFolder(folderId)
    }

    override suspend fun addPodcastToFolder(podcastId: String, folderId: String) {
        folderDao.insertCrossRefs(listOf(PodcastFolderCrossRef(podcastId = podcastId, folderId = folderId)))
    }

    override suspend fun removePodcastFromFolder(podcastId: String, folderId: String) {
        folderDao.deleteCrossRef(podcastId = podcastId, folderId = folderId)
    }

    override suspend fun setPodcastsForFolder(folderId: String, podcastIds: List<String>) {
        folderDao.setPodcastsForFolder(folderId, podcastIds)
    }

    override suspend fun syncLinkedGenres() {
        val folders = folderDao.getAllFoldersList().filter {
            !it.linkedGenre.isNullOrBlank() || PodcastGenres.canonicalize(it.name) != null
        }
        if (folders.isEmpty()) return

        val subscribed = podcastDao.getSubscribedPodcastsList()
        for (folder in folders) {
            val targetGenre = folder.linkedGenre?.trim()?.takeIf { it.isNotEmpty() } ?: folder.name.trim()
            val matchingIds = subscribed
                .filter { pod -> matchesGenre(pod, targetGenre) }
                .map { it.podcastId }
                .distinct()
            val currentIds = folderDao.getPodcastIdsForFolderList(folder.folderId)
            if (currentIds != matchingIds) {
                folderDao.setPodcastsForFolder(folder.folderId, matchingIds)
            }
            if (folder.linkedGenre.isNullOrBlank() && PodcastGenres.canonicalize(folder.name) != null) {
                folderDao.upsertFolder(folder.copy(linkedGenre = targetGenre))
            }
        }
    }

    override suspend fun autoOrganizeSubscribedShows(
        defaultDisplaySize: FolderDisplaySize?,
        showPodcastGrid: Boolean,
    ) {
        val subscribed = podcastDao.getSubscribedPodcastsList()
        if (subscribed.isEmpty()) return

        val genreToPodcasts = groupSubscribedPodcastsByGenre(subscribed)
        val existingFolders = folderDao.getAllFoldersList()

        for ((genre, podcastIds) in genreToPodcasts) {
            mergeOrCreateGenreFolder(
                genre = genre,
                podcastIds = podcastIds,
                existingFolders = existingFolders,
                defaultDisplaySize = defaultDisplaySize,
                showPodcastGrid = showPodcastGrid,
            )
        }

        syncLinkedGenres()
    }

    private suspend fun mergeOrCreateGenreFolder(
        genre: String,
        podcastIds: List<String>,
        existingFolders: List<FolderEntity>,
        defaultDisplaySize: FolderDisplaySize?,
        showPodcastGrid: Boolean,
    ) {
        val existing = existingFolders.firstOrNull { f ->
            val linked = f.linkedGenre
            linked?.equals(genre, ignoreCase = true) == true ||
                isGenreTokenMatch(f.name, genre) ||
                (linked != null && isGenreTokenMatch(linked, genre))
        }

        if (existing != null) {
            if (existing.linkedGenre.isNullOrBlank()) {
                folderDao.upsertFolder(existing.copy(linkedGenre = genre))
            }
            val currentIds = folderDao.getPodcastIdsForFolderList(existing.folderId)
            val merged = (currentIds + podcastIds).distinct()
            if (merged.size != currentIds.size) {
                folderDao.setPodcastsForFolder(existing.folderId, merged)
            }
        } else {
            createAutoOrganizeFolder(
                genre = genre,
                podcastIds = podcastIds,
                defaultDisplaySize = defaultDisplaySize,
                showPodcastGrid = showPodcastGrid,
            )
        }
    }

    private suspend fun createAutoOrganizeFolder(
        genre: String,
        podcastIds: List<String>,
        defaultDisplaySize: FolderDisplaySize?,
        showPodcastGrid: Boolean,
    ) {
        val folderId = UUID.randomUUID().toString()
        val iconKey = defaultIconForGenre(genre)
        val distinctPodcastIds = podcastIds.distinct()
        val chosenSize = defaultDisplaySize ?: when {
            distinctPodcastIds.size <= 2 -> FolderDisplaySize.COMPACT
            distinctPodcastIds.size <= 5 -> FolderDisplaySize.SHELF
            else -> FolderDisplaySize.PANEL
        }
        val resolvedGrid = if (chosenSize == FolderDisplaySize.COMPACT) {
            if (defaultDisplaySize == null) true else showPodcastGrid
        } else {
            false
        }
        val entity = FolderEntity(
            folderId = folderId,
            name = genre,
            icon = iconKey,
            displaySize = chosenSize,
            linkedGenre = genre,
            showPodcastGrid = resolvedGrid,
            createdAt = System.currentTimeMillis(),
        )
        folderDao.upsertFolder(entity)
        folderDao.setPodcastsForFolder(folderId, distinctPodcastIds)
    }

    private fun defaultIconForGenre(genre: String): String? {
        resolveGenreIconKey?.invoke(genre)?.let { return it }
        val canonical = PodcastGenres.canonicalize(genre) ?: genre
        return GENRE_DEFAULT_ICON_MAP[canonical.lowercase().trim()] ?: "folder"
    }

    private companion object {
        val GENRE_DEFAULT_ICON_MAP = mapOf(
            "news" to "news",
            "tech" to "tech",
            "technology" to "tech",
            "comedy" to "comedy",
            "sports" to "sports",
            "sport" to "sports",
            "business" to "business",
            "science" to "science",
            "music" to "music",
            "health" to "health",
            "history" to "history",
            "true crime" to "mic",
            "crime" to "mic",
            "tv & film" to "movie",
            "film" to "movie",
            "movies" to "movie",
            "fiction" to "book",
            "gaming" to "gaming",
            "games" to "gaming",
            "coding" to "code",
            "code" to "code",
            "ideas" to "bulb",
            "philosophy" to "bulb",
            "finance" to "finance",
            "money" to "finance",
        )
    }
}

private fun groupSubscribedPodcastsByGenre(subscribed: List<PodcastEntity>): Map<String, List<String>> {
    val result = mutableMapOf<String, MutableList<String>>()
    for (pod in subscribed) {
        val genreName = pod.customGenre?.trim()?.takeIf { it.isNotEmpty() }
            ?: pod.genre?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        if (!genreName.isNullOrBlank()) {
            val canonical = PodcastGenres.canonicalize(genreName)
                ?: (if (genreName.equals("tech", ignoreCase = true)) "Technology" else genreName.replaceFirstChar { it.uppercase() })
            result.getOrPut(canonical) { mutableListOf() }.add(pod.podcastId)
        }
    }
    return result
}

private fun matchesGenre(pod: PodcastEntity, targetGenre: String): Boolean {
    val effectiveGenre = pod.customGenre?.takeIf { it.isNotBlank() }
        ?: pod.genre?.takeIf { it.isNotBlank() }
        ?: return false
    return effectiveGenre.split(",").any { token ->
        isGenreTokenMatch(token, targetGenre)
    }
}

private fun isGenreTokenMatch(candidate: String, target: String): Boolean {
    val trimmedCandidate = candidate.trim()
    val trimmedTarget = target.trim()
    if (trimmedCandidate.equals(trimmedTarget, ignoreCase = true)) return true

    val isTechSynonym = (trimmedTarget.equals("Tech", ignoreCase = true) && trimmedCandidate.equals("Technology", ignoreCase = true)) ||
        (trimmedTarget.equals("Technology", ignoreCase = true) && trimmedCandidate.equals("Tech", ignoreCase = true))
    if (isTechSynonym) return true

    val canonicalTarget = PodcastGenres.canonicalize(trimmedTarget)
    val canonicalCandidate = PodcastGenres.canonicalize(trimmedCandidate)

    if (canonicalTarget != null) {
        if (canonicalTarget.equals(canonicalCandidate, ignoreCase = true)) return true
        if (canonicalTarget.equals(trimmedCandidate, ignoreCase = true)) return true
    }
    if (canonicalCandidate != null) {
        if (canonicalCandidate.equals(trimmedTarget, ignoreCase = true)) return true
    }
    return false
}
