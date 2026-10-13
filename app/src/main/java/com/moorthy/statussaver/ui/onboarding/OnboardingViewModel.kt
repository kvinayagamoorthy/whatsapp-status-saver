package com.moorthy.statussaver.ui.onboarding

import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moorthy.statussaver.StatusSaverApp
import com.moorthy.statussaver.data.StatusFolders
import com.moorthy.statussaver.data.StatusSourceStore
import com.moorthy.statussaver.data.model.StatusSourceKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    /** Apps whose statuses folder is granted and still readable. */
    val grantedKinds: Set<StatusSourceKind> = emptySet(),
    /** The last picked folder was not a statuses folder of either app. */
    val pickedWrongFolder: Boolean = false,
)

class OnboardingViewModel(
    private val statusSourceStore: StatusSourceStore,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val grantedKinds = statusSourceStore.validSources().map { source -> source.kind }
            mutableUiState.update { state -> state.copy(grantedKinds = grantedKinds.toSet()) }
        }
    }

    /**
     * Handles the folder picker's result; [treeUri] is null when the user backed out.
     *
     * The folder decides which app it belongs to, not the button that opened the picker,
     * so picking the Business folder from the WhatsApp row still grants Business.
     */
    fun onFolderPicked(treeUri: Uri?) {
        if (treeUri == null) {
            return
        }
        val kind = StatusFolders.classify(DocumentsContract.getTreeDocumentId(treeUri))
        if (kind == null) {
            mutableUiState.update { state -> state.copy(pickedWrongFolder = true) }
            return
        }
        viewModelScope.launch {
            statusSourceStore.grant(kind, treeUri)
            mutableUiState.update { state ->
                state.copy(grantedKinds = state.grantedKinds + kind, pickedWrongFolder = false)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as StatusSaverApp
                OnboardingViewModel(app.statusSourceStore)
            }
        }
    }
}
