package com.moorthy.statussaver.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moorthy.statussaver.R
import com.moorthy.statussaver.data.StatusFolders
import com.moorthy.statussaver.data.model.StatusSourceKind

/**
 * One app's statuses folder: whether Keepsake may read it, and the button that opens
 * the folder picker. With [onRevoke] (settings only) a granted folder can also be given
 * back.
 */
@Composable
fun SourceCard(
    kind: StatusSourceKind,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
    onRevoke: (() -> Unit)? = null,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(kind.displayName()),
                style = MaterialTheme.typography.titleMedium,
            )
            // Shown so the user can find the folder by hand if the picker ignores the
            // start folder (risk R3).
            Text(
                text = StatusFolders.expectedPath(kind),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isGranted) {
                Text(
                    text = stringResource(R.string.onboarding_access_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                // Wraps the second button onto its own line when there is no room, for
                // example at large font sizes, instead of breaking its words apart.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedButton(onClick = onGrantClick) {
                        Text(stringResource(R.string.onboarding_choose_again))
                    }
                    if (onRevoke != null) {
                        TextButton(onClick = onRevoke) {
                            Text(stringResource(R.string.settings_remove_access))
                        }
                    }
                }
            } else {
                Button(onClick = onGrantClick) {
                    Text(stringResource(R.string.onboarding_grant_access))
                }
            }
        }
    }
}

/** The app's name as the user knows it. */
fun StatusSourceKind.displayName(): Int =
    when (this) {
        StatusSourceKind.WHATSAPP -> R.string.source_whatsapp
        StatusSourceKind.BUSINESS -> R.string.source_business
    }
