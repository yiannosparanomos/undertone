package io.github.yiannosparanomos.undertone.ui.sounds

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.yiannosparanomos.undertone.R
import io.github.yiannosparanomos.undertone.core.designsystem.UtLabel
import io.github.yiannosparanomos.undertone.core.designsystem.UtPanel
import io.github.yiannosparanomos.undertone.core.designsystem.UtStatus
import io.github.yiannosparanomos.undertone.core.designsystem.UtSwitchRow
import io.github.yiannosparanomos.undertone.ui.common.UtScreen

/** Sounds: the default Ring sound, vibration and UnderTone's own volume (spec §6). */
@Composable
fun SoundsScreen(
    onBack: () -> Unit,
    viewModel: SoundsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    UtScreen(title = stringResource(R.string.nav_sounds), onBack = onBack) {
        UtPanel {
            Text(
                text = stringResource(R.string.sounds_why),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!state.audible) {
            UtPanel {
                // The one thing that needs attention, so the accent is earned here.
                UtStatus(
                    headline = stringResource(R.string.sounds_inaudible),
                    needsAttention = true,
                )
            }
        }

        UtPanel {
            UtLabel(stringResource(R.string.sounds_default_label))
            Text(
                text = state.sounds.defaultSoundUri ?: stringResource(R.string.sounds_system_default),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            UtSwitchRow(
                title = stringResource(R.string.sounds_vibrate),
                checked = state.sounds.vibrate,
                onCheckedChange = viewModel::setVibrate,
            )
            UtLabel("${stringResource(R.string.sounds_volume)} ${state.sounds.volumePercent}%")
            Slider(
                value = state.sounds.volumePercent.toFloat(),
                onValueChange = { viewModel.setVolume(it.toInt()) },
                valueRange = 0f..100f,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Volume" },
            )
            OutlinedButton(onClick = viewModel::test, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sounds_test))
            }
        }

        UtPanel {
            UtLabel(stringResource(R.string.sounds_per_app_label))
            if (state.sounds.perApp.isEmpty()) {
                Text(
                    text = stringResource(R.string.sounds_per_app_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.sounds.perApp.forEach { app ->
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
