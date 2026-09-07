package cx.aswin.boxlore.core.catalog.backup

import cx.aswin.boxlore.core.catalog.FolderRepository
import cx.aswin.boxlore.core.model.FolderDisplaySize
import cx.aswin.boxlore.core.model.SubscriptionFolder
import java.util.UUID

/**
 * Data transfer object representing a subscription folder in a JSON library backup.
 */
data class SubscriptionFolderBackup(
    val id: String,
    val name: String,
    val icon: String? = null,
    val displaySize: String = FolderDisplaySize.COMPACT.name,
    val linkedGenre: String? = null,
    val showPodcastGrid: Boolean = false,
    val createdAt: Long = 0L,
    val podcastIds: List<String> = emptyList(),
)

/**
 * Helper providing export mapping and restoration logic for subscription folders in JSON backups.
 */
object LibraryBackupFolderLogic {

    fun toBackup(folder: SubscriptionFolder): SubscriptionFolderBackup =
        SubscriptionFolderBackup(
            id = folder.id,
            name = folder.name,
            icon = folder.icon,
            displaySize = folder.displaySize.name,
            linkedGenre = folder.linkedGenre,
            showPodcastGrid = folder.showPodcastGrid,
            createdAt = folder.createdAt,
            podcastIds = folder.podcastIds,
        )

    suspend fun restoreFolders(
        folders: List<SubscriptionFolderBackup>?,
        folderRepository: FolderRepository,
    ) {
        if (folders.isNullOrEmpty()) return
        for (backupFolder in folders) {
            val rawName = backupFolder.name.trim()
            if (rawName.isBlank()) continue
            val trimmedName = if (rawName.equals("Technology", ignoreCase = true)) "Tech" else rawName
            val displaySize = FolderDisplaySize.entries.find {
                it.name.equals(backupFolder.displaySize, ignoreCase = true)
            } ?: FolderDisplaySize.COMPACT

            val effectiveIcon = if (trimmedName == "Tech" && backupFolder.icon.isNullOrBlank()) {
                "tech"
            } else {
                backupFolder.icon?.trim()?.takeIf { it.isNotEmpty() }
            }

            val rawGenre = backupFolder.linkedGenre?.trim()?.takeIf { it.isNotEmpty() }
            val sanitizedGenre = if (rawGenre.equals("Technology", ignoreCase = true)) "Tech" else rawGenre

            val distinctPodIds = backupFolder.podcastIds.filter { it.isNotBlank() }.distinct()
            folderRepository.restoreFolder(
                SubscriptionFolder(
                    id = backupFolder.id.ifBlank { UUID.randomUUID().toString() },
                    name = trimmedName,
                    icon = effectiveIcon,
                    displaySize = displaySize,
                    linkedGenre = sanitizedGenre,
                    showPodcastGrid = backupFolder.showPodcastGrid,
                    createdAt = backupFolder.createdAt,
                    podcastCount = distinctPodIds.size,
                    podcastIds = distinctPodIds,
                ),
            )
        }
    }
}
