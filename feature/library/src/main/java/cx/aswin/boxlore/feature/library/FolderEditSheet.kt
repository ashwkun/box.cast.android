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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import cx.aswin.boxlore.core.designsystem.icon.GenreSuggestion
import cx.aswin.boxlore.core.designsystem.icon.buildFolderSuggestionsWithLibrary
import cx.aswin.boxlore.core.designsystem.icon.filterGenreSuggestions
import cx.aswin.boxlore.core.designsystem.icon.findExactGenreIconKey
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
    val nameState = remember(initialFolder) { mutableStateOf(initialFolder?.name ?: "") }
    val iconState = remember(initialFolder) { mutableStateOf(initialFolder?.icon) }
    val iconManualState = remember(initialFolder) { mutableStateOf(initialFolder?.icon != null) }
    val displaySizeState = remember(initialFolder) {
        mutableStateOf(initialFolder?.displaySize ?: FolderDisplaySize.COMPACT)
    }
    val podcastGridState = remember(initialFolder) {
        mutableStateOf(initialFolder?.showPodcastGrid ?: false)
    }
    val autoSyncState = remember(initialFolder) {
        mutableStateOf(initialFolder?.isGenreLinked ?: false)
    }
    val linkedGenreState = remember(initialFolder) {
        mutableStateOf(initialFolder?.linkedGenre ?: "")
    }

    val focusManager = LocalFocusManager.current
    val isEditing = initialFolder != null
    val isTechnologyDisallowed = nameState.value.trim().equals("Technology", ignoreCase = true)
    val canSave = nameState.value.trim().isNotEmpty() && !isTechnologyDisallowed

    val effectiveLinkedGenre = if (autoSyncState.value) {
        linkedGenreState.value.trim().ifEmpty { nameState.value.trim() }
    } else {
        null
    }

    val allFolderSuggestions = remember(suggestedGenres) {
        buildFolderSuggestionsWithLibrary(suggestedGenres)
    }

    val filteredSuggestions = remember(nameState.value, allFolderSuggestions) {
        filterGenreSuggestions(nameState.value, allFolderSuggestions)
    }

    val formState = FolderEditFormState(
        isEditing = isEditing,
        canSave = canSave,
        isTechnologyDisallowed = isTechnologyDisallowed,
        nameText = nameState.value,
        selectedIconKey = iconState.value,
        selectedDisplaySize = displaySizeState.value,
        showPodcastGrid = podcastGridState.value,
        autoSyncGenre = autoSyncState.value,
        effectiveLinkedGenre = effectiveLinkedGenre,
        suggestedGenres = suggestedGenres,
        filteredSuggestions = filteredSuggestions,
    )

    val fields = remember(initialFolder) {
        FolderEditFormFields(
            nameState = nameState,
            iconState = iconState,
            iconManualState = iconManualState,
            displaySizeState = displaySizeState,
            podcastGridState = podcastGridState,
            autoSyncState = autoSyncState,
            linkedGenreState = linkedGenreState,
        )
    }

    val formActions = rememberFolderEditFormActions(
        fields = fields,
        allFolderSuggestions = allFolderSuggestions,
        focusManager = focusManager,
        onDismissRequest = onDismissRequest,
        onSave = onSave,
        onDelete = onDelete,
    )

    return formState to formActions
}

private class FolderEditFormFields(
    val nameState: MutableState<String>,
    val iconState: MutableState<String?>,
    val iconManualState: MutableState<Boolean>,
    val displaySizeState: MutableState<FolderDisplaySize>,
    val podcastGridState: MutableState<Boolean>,
    val autoSyncState: MutableState<Boolean>,
    val linkedGenreState: MutableState<String>,
)

private fun rememberFolderEditFormActions(
    fields: FolderEditFormFields,
    allFolderSuggestions: List<GenreSuggestion>,
    focusManager: FocusManager,
    onDismissRequest: () -> Unit,
    onSave: (name: String, icon: String?, displaySize: FolderDisplaySize, linkedGenre: String?, showPodcastGrid: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
): FolderEditFormActions = FolderEditFormActions(
    onNameChange = { newName ->
        fields.nameState.value = newName
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            fields.iconManualState.value = false
            fields.iconState.value = null
        } else if (!fields.iconManualState.value) {
            val matchedKey = findExactGenreIconKey(trimmed, allFolderSuggestions)
            if (matchedKey != null) {
                fields.iconState.value = matchedKey
            }
        }
    },
    onSelectIcon = {
        fields.iconState.value = it
        fields.iconManualState.value = true
        focusManager.clearFocus()
    },
    onSelectDisplaySize = { fields.displaySizeState.value = it },
    onShowPodcastGridChange = { fields.podcastGridState.value = it },
    onAutoSyncChange = { enabled -> fields.autoSyncState.value = enabled },
    onSelectLinkedGenre = { genre ->
        fields.linkedGenreState.value = if (fields.linkedGenreState.value.equals(genre, ignoreCase = true)) "" else genre
    },
    onSelectSuggestion = { suggestion ->
        fields.nameState.value = suggestion.name
        fields.iconState.value = suggestion.iconKey
        fields.iconManualState.value = true
        focusManager.clearFocus()
    },
    onSwitchToTech = {
        fields.nameState.value = "Tech"
        fields.iconState.value = "tech"
        fields.iconManualState.value = true
        focusManager.clearFocus()
    },
    onDone = { focusManager.clearFocus() },
    onSave = {
        focusManager.clearFocus()
        val finalName = fields.nameState.value.trim()
        val finalIcon = fields.iconState.value?.trim()?.takeIf { it.isNotEmpty() }
        val finalLinked = if (fields.autoSyncState.value) {
            fields.linkedGenreState.value.trim().ifEmpty { finalName }.takeIf { it.isNotEmpty() }
        } else {
            null
        }
        val finalShowPodcastGrid = if (finalIcon == null) true else fields.podcastGridState.value
        onSave(finalName, finalIcon, fields.displaySizeState.value, finalLinked, finalShowPodcastGrid)
    },
    onClose = onDismissRequest,
    onDelete = onDelete,
)

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
