package cx.aswin.boxlore.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import cx.aswin.boxlore.core.designsystem.icon.GenreSuggestion
import cx.aswin.boxlore.core.designsystem.icon.buildFolderSuggestionsWithLibrary
import cx.aswin.boxlore.core.designsystem.icon.filterGenreSuggestions
import cx.aswin.boxlore.core.model.FolderDisplaySize
import cx.aswin.boxlore.core.model.SubscriptionFolder

internal data class FolderEditFormState(
    val isEditing: Boolean,
    val canSave: Boolean,
    val isTechnologyDisallowed: Boolean,
    val nameText: String,
    val selectedIconKey: String?,
    val selectedDisplaySize: FolderDisplaySize,
    val showPodcastGrid: Boolean,
    val autoSyncGenre: Boolean,
    val effectiveLinkedGenre: String?,
    val suggestedGenres: List<String>,
    val filteredSuggestions: List<GenreSuggestion>,
)

internal data class FolderEditFormActions(
    val onNameChange: (String) -> Unit,
    val onSelectIcon: (String?) -> Unit,
    val onSelectDisplaySize: (FolderDisplaySize) -> Unit,
    val onShowPodcastGridChange: (Boolean) -> Unit,
    val onAutoSyncChange: (Boolean) -> Unit,
    val onSelectLinkedGenre: (String) -> Unit,
    val onSelectSuggestion: (GenreSuggestion) -> Unit,
    val onSwitchToTech: () -> Unit,
    val onDone: () -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
    val onDelete: (() -> Unit)?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderEditSheet(
    initialFolder: SubscriptionFolder? = null,
    suggestedGenres: List<String> = emptyList(),
    onDismissRequest: () -> Unit,
    onSave: (name: String, icon: String?, displaySize: FolderDisplaySize, linkedGenre: String?, showPodcastGrid: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (formState, formActions) = rememberFolderEditStateAndActions(
        initialFolder = initialFolder,
        suggestedGenres = suggestedGenres,
        onDismissRequest = onDismissRequest,
        onSave = onSave,
        onDelete = onDelete,
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentWindowInsets = { WindowInsets.navigationBars },
        modifier = Modifier.imePadding(),
    ) {
        FolderEditSheetContent(
            state = formState,
            actions = formActions,
        )
    }
}

@Composable
private fun rememberFolderEditStateAndActions(
    initialFolder: SubscriptionFolder?,
    suggestedGenres: List<String>,
    onDismissRequest: () -> Unit,
    onSave: (name: String, icon: String?, displaySize: FolderDisplaySize, linkedGenre: String?, showPodcastGrid: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
): Pair<FolderEditFormState, FolderEditFormActions> {
    var nameText by remember(initialFolder) { mutableStateOf(initialFolder?.name ?: "") }
    var selectedIconKey by remember(initialFolder) { mutableStateOf(initialFolder?.icon) }
    var isIconManuallySelected by remember(initialFolder) {
        mutableStateOf(initialFolder?.icon != null)
    }
    var selectedDisplaySize by remember(initialFolder) {
        mutableStateOf(initialFolder?.displaySize ?: FolderDisplaySize.COMPACT)
    }
    var showPodcastGrid by remember(initialFolder) {
        mutableStateOf(initialFolder?.showPodcastGrid ?: false)
    }
    var autoSyncGenre by remember(initialFolder) {
        mutableStateOf(initialFolder?.isGenreLinked ?: false)
    }
    var linkedGenreText by remember(initialFolder) {
        mutableStateOf(initialFolder?.linkedGenre ?: "")
    }

    val focusManager = LocalFocusManager.current
    val isEditing = initialFolder != null
    val isTechnologyDisallowed = nameText.trim().equals("Technology", ignoreCase = true)
    val canSave = nameText.trim().isNotEmpty() && !isTechnologyDisallowed

    val effectiveLinkedGenre = if (autoSyncGenre) {
        linkedGenreText.trim().ifEmpty { nameText.trim() }
    } else {
        null
    }

    val allFolderSuggestions = remember(suggestedGenres) {
        buildFolderSuggestionsWithLibrary(suggestedGenres)
    }

    val filteredSuggestions = remember(nameText, allFolderSuggestions) {
        filterGenreSuggestions(nameText, allFolderSuggestions)
    }

    val formState = FolderEditFormState(
        isEditing = isEditing,
        canSave = canSave,
        isTechnologyDisallowed = isTechnologyDisallowed,
        nameText = nameText,
        selectedIconKey = selectedIconKey,
        selectedDisplaySize = selectedDisplaySize,
        showPodcastGrid = showPodcastGrid,
        autoSyncGenre = autoSyncGenre,
        effectiveLinkedGenre = effectiveLinkedGenre,
        suggestedGenres = suggestedGenres,
        filteredSuggestions = filteredSuggestions,
    )

    val formActions = FolderEditFormActions(
        onNameChange = { nameText = it },
        onSelectIcon = {
            selectedIconKey = it
            isIconManuallySelected = true
            focusManager.clearFocus()
        },
        onSelectDisplaySize = { selectedDisplaySize = it },
        onShowPodcastGridChange = { showPodcastGrid = it },
        onAutoSyncChange = { enabled -> autoSyncGenre = enabled },
        onSelectLinkedGenre = { genre ->
            linkedGenreText = if (linkedGenreText.equals(genre, ignoreCase = true)) "" else genre
        },
        onSelectSuggestion = { suggestion ->
            nameText = suggestion.name
            selectedIconKey = suggestion.iconKey
            isIconManuallySelected = true
            focusManager.clearFocus()
        },
        onSwitchToTech = {
            nameText = "Tech"
            selectedIconKey = "tech"
            isIconManuallySelected = true
            focusManager.clearFocus()
        },
        onDone = { focusManager.clearFocus() },
        onSave = {
            focusManager.clearFocus()
            val finalName = nameText.trim()
            val finalIcon = selectedIconKey?.trim()?.takeIf { it.isNotEmpty() }
            val finalLinked = if (autoSyncGenre) {
                linkedGenreText.trim().ifEmpty { finalName }.takeIf { it.isNotEmpty() }
            } else {
                null
            }
            val finalShowPodcastGrid = if (finalIcon == null) true else showPodcastGrid
            onSave(finalName, finalIcon, selectedDisplaySize, finalLinked, finalShowPodcastGrid)
        },
        onClose = onDismissRequest,
        onDelete = onDelete,
    )

    return formState to formActions
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderEditSheetContent(
    state: FolderEditFormState,
    actions: FolderEditFormActions,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalOverscrollFactory provides null,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FolderEditTopBar(
                isEditing = state.isEditing,
                canSave = state.canSave,
                onClose = actions.onClose,
                onDelete = actions.onDelete,
                onSave = actions.onSave,
            )

            FolderIdentityHeader(
                nameText = state.nameText,
                onNameChange = actions.onNameChange,
                iconKey = state.selectedIconKey,
                onDone = actions.onDone,
            )

            if (state.isTechnologyDisallowed) {
                FolderTechnologyWarningCard(
                    onSwitchToTech = actions.onSwitchToTech,
                )
            }

            if (state.filteredSuggestions.isNotEmpty()) {
                FolderQuickFillChipsRow(
                    suggestions = state.filteredSuggestions,
                    queryText = state.nameText,
                    hasLibraryGenres = state.suggestedGenres.isNotEmpty(),
                    onSelectSuggestion = actions.onSelectSuggestion,
                )
            }

            FolderIconPickerRow(
                selectedIconKey = state.selectedIconKey,
                queryText = state.nameText,
                onSelectIcon = actions.onSelectIcon,
            )

            FolderDisplaySizeSelector(
                selectedSize = state.selectedDisplaySize,
                onSizeSelected = actions.onSelectDisplaySize,
            )

            if (state.selectedDisplaySize == FolderDisplaySize.COMPACT) {
                FolderCompactCoverStyleCard(
                    showPodcastGrid = state.showPodcastGrid,
                    hasIcon = !state.selectedIconKey.isNullOrBlank(),
                    selectedIconKey = state.selectedIconKey,
                    onShowPodcastGridChange = actions.onShowPodcastGridChange,
                )
            }

            FolderOrganizationCard(
                state = FolderOrganizationState(
                    autoSync = state.autoSyncGenre,
                    onAutoSyncChange = actions.onAutoSyncChange,
                    linkedGenre = state.effectiveLinkedGenre ?: state.nameText.trim(),
                    suggestedGenres = state.suggestedGenres,
                    onSelectLinkedGenre = actions.onSelectLinkedGenre,
                ),
            )
        }
    }
}
