package com.moorthy.statussaver.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moorthy.statussaver.R
import com.moorthy.statussaver.data.StatusFolders
import com.moorthy.statussaver.data.model.StatusSourceKind
import com.moorthy.statussaver.data.model.ThemeMode
import com.moorthy.statussaver.ui.onboarding.FolderAccessViewModel
import com.moorthy.statussaver.ui.onboarding.SourceCard
import com.moorthy.statussaver.ui.onboarding.displayName

/**
 * Settings (design.md §2): the theme mode (FR7) and the statuses folders Keepsake may read
 * (FR8). [onBack] returns to the list, which reloads and goes to onboarding if no folder
 * is left.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    folderAccessViewModel: FolderAccessViewModel = viewModel(factory = FolderAccessViewModel.Factory),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
                title = { Text(stringResource(R.string.settings_title)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(R.string.settings_theme)
            val currentMode = themeMode
            if (currentMode != null) {
                ThemeModeChoices(selected = currentMode, onSelect = viewModel::setThemeMode)
            }
            SectionTitle(R.string.settings_access)
            FolderAccess(folderAccessViewModel)
        }
    }
}

/**
 * The same cards as onboarding, plus Remove access, which asks first: giving the last
 * folder back sends the user through onboarding again.
 */
@Composable
private fun FolderAccess(viewModel: FolderAccessViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = viewModel::onFolderPicked,
    )
    // Saved, so the question survives a rotation.
    var kindToRevoke by rememberSaveable { mutableStateOf<StatusSourceKind?>(null) }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (kind in StatusSourceKind.entries) {
            SourceCard(
                kind = kind,
                isGranted = kind in uiState.grantedKinds,
                onGrantClick = { folderPicker.launch(StatusFolders.initialUri(kind)) },
                onRevoke = { kindToRevoke = kind },
            )
        }
        if (uiState.pickedWrongFolder) {
            Text(
                text = stringResource(R.string.onboarding_wrong_folder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.onboarding_read_only_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }

    val pendingKind = kindToRevoke
    if (pendingKind != null) {
        AlertDialog(
            onDismissRequest = { kindToRevoke = null },
            title = {
                Text(
                    stringResource(
                        R.string.settings_remove_access_title,
                        stringResource(pendingKind.displayName()),
                    ),
                )
            },
            text = { Text(stringResource(R.string.settings_remove_access_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.revoke(pendingKind)
                        kindToRevoke = null
                    },
                ) {
                    Text(stringResource(R.string.settings_remove_access_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { kindToRevoke = null }) {
                    Text(stringResource(R.string.settings_remove_access_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

/** One radio row per mode; the whole row is the touch target. */
@Composable
private fun ThemeModeChoices(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.selectableGroup()) {
        for (mode in ThemeMode.entries) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp),
            ) {
                // The row handles the click and announces the state.
                RadioButton(selected = mode == selected, onClick = null)
                Text(
                    text = stringResource(mode.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

private val ThemeMode.labelRes: Int
    @StringRes get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }
