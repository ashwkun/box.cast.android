package cx.aswin.boxlore.feature.library

import cx.aswin.boxlore.core.designsystem.icon.GenreSuggestion
import cx.aswin.boxlore.core.designsystem.icon.findExactGenreIconKey
import cx.aswin.boxlore.core.model.FolderDisplaySize

data class FolderSaveParams(
    val name: String,
    val icon: String?,
    val displaySize: FolderDisplaySize,
    val linkedGenre: String?,
    val showPodcastGrid: Boolean,
)

data class NameChangeOutcome(
    val updatedIcon: String?,
    val isIconManual: Boolean,
    val autoSync: Boolean? = null,
    val linkedGenre: String? = null,
)

object FolderEditLogic {
    fun isTechnologyDisallowed(name: String): Boolean =
        name.trim().equals("Technology", ignoreCase = true)

    fun canSave(name: String): Boolean =
        name.trim().isNotEmpty() && !isTechnologyDisallowed(name)

    fun resolveEffectiveLinkedGenre(
        autoSync: Boolean,
        linkedGenre: String,
        folderName: String,
    ): String? = if (autoSync) {
        linkedGenre.trim().ifEmpty { folderName.trim() }.takeIf { it.isNotEmpty() }
    } else {
        null
    }

    fun computeNameChange(
        newName: String,
        isIconManuallySelected: Boolean,
        allFolderSuggestions: List<GenreSuggestion>,
    ): NameChangeOutcome {
        val trimmed = newName.trim()
        return if (trimmed.isEmpty()) {
            NameChangeOutcome(
                updatedIcon = null,
                isIconManual = false,
            )
        } else if (!isIconManuallySelected) {
            val matchedKey = findExactGenreIconKey(trimmed, allFolderSuggestions)
            if (matchedKey != null) {
                NameChangeOutcome(
                    updatedIcon = matchedKey,
                    isIconManual = false,
                    autoSync = true,
                    linkedGenre = trimmed,
                )
            } else {
                NameChangeOutcome(
                    updatedIcon = null,
                    isIconManual = false,
                )
            }
        } else {
            NameChangeOutcome(
                updatedIcon = null,
                isIconManual = true,
            )
        }
    }

    fun resolveFinalSaveParams(
        name: String,
        icon: String?,
        displaySize: FolderDisplaySize,
        autoSync: Boolean,
        linkedGenre: String,
        showPodcastGrid: Boolean,
    ): FolderSaveParams {
        val finalName = name.trim()
        val finalIcon = icon?.trim()?.takeIf { it.isNotEmpty() }
        val finalLinked = resolveEffectiveLinkedGenre(
            autoSync = autoSync,
            linkedGenre = linkedGenre,
            folderName = finalName,
        )
        val finalShowPodcastGrid = finalIcon == null || showPodcastGrid
        return FolderSaveParams(
            name = finalName,
            icon = finalIcon,
            displaySize = displaySize,
            linkedGenre = finalLinked,
            showPodcastGrid = finalShowPodcastGrid,
        )
    }
}
