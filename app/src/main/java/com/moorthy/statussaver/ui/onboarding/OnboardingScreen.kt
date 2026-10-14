package com.moorthy.statussaver.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moorthy.statussaver.R
import com.moorthy.statussaver.data.StatusFolders
import com.moorthy.statussaver.data.model.StatusSourceKind

/**
 * First-run screen (FR1): explains the folder access and grants it per app. [onContinue]
 * is offered once at least one app is granted, so the user can grant both apps first.
 */
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    viewModel: FolderAccessViewModel = viewModel(factory = FolderAccessViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = viewModel::onFolderPicked,
    )

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.onboarding_explanation),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.onboarding_picker_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            for (kind in StatusSourceKind.entries) {
                SourceCard(
                    kind = kind,
                    isGranted = kind in uiState.grantedKinds,
                    onGrantClick = { folderPicker.launch(StatusFolders.initialUri(kind)) },
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
            )
            if (uiState.grantedKinds.isNotEmpty()) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.onboarding_continue))
                }
            }
        }
    }
}
