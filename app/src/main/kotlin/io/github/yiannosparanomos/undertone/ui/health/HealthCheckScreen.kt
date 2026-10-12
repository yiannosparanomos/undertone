package io.github.yiannosparanomos.undertone.ui.health

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.yiannosparanomos.undertone.R
import io.github.yiannosparanomos.undertone.core.designsystem.UtListRow
import io.github.yiannosparanomos.undertone.core.designsystem.UtPanel
import io.github.yiannosparanomos.undertone.core.designsystem.UtRowDivider
import io.github.yiannosparanomos.undertone.core.designsystem.UtStatus
import io.github.yiannosparanomos.undertone.ui.common.UtScreen

/**
 * Health check: everything UnderTone needs, whether it has it, and a way to each system screen
 * that grants it.
 */
@Composable
fun HealthCheckScreen(
    onBack: () -> Unit,
    viewModel: HealthCheckViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    UtScreen(title = stringResource(R.string.nav_health), onBack = onBack) {
        UtPanel {
            UtStatus(
                headline =
                    if (state.problems == 0) {
                        stringResource(R.string.health_ok)
                    } else {
                        stringResource(R.string.health_problems, state.problems)
                    },
                needsAttention = state.problems > 0,
            )
        }

        UtPanel {
            state.items.forEachIndexed { index, item ->
                if (index > 0) UtRowDivider()
                UtListRow(
                    title = stringResource(item.titleRes()),
                    subtitle = stringResource(item.whyRes()),
                    value =
                        if (item.granted) {
                            stringResource(R.string.health_granted)
                        } else {
                            stringResource(R.string.health_missing)
                        },
                    onClick =
                        if (item.granted) {
                            null
                        } else {
                            {
                                viewModel.fixIntent(item)?.let { intent ->
                                    runCatching { context.startActivity(intent) }
                                }
                            }
                        },
                )
            }
        }

        UtPanel {
            OutlinedButton(onClick = viewModel::check, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.health_recheck))
            }
        }
    }
}

private fun HealthItem.titleRes(): Int =
    when (id) {
        HealthItem.Id.NOTIFICATION_ACCESS -> R.string.health_notification_access
        HealthItem.Id.DND_ACCESS -> R.string.health_dnd_access
        HealthItem.Id.CALL_SCREENING -> R.string.health_call_screening
        HealthItem.Id.CONTACTS -> R.string.health_contacts
        HealthItem.Id.CALENDAR -> R.string.health_calendar
        HealthItem.Id.EXACT_ALARMS -> R.string.health_exact_alarms
        HealthItem.Id.NOTIFICATION_VOLUME -> R.string.health_notification_volume
    }

private fun HealthItem.whyRes(): Int =
    when (id) {
        HealthItem.Id.NOTIFICATION_ACCESS -> R.string.health_notification_access_why
        HealthItem.Id.DND_ACCESS -> R.string.health_dnd_access_why
        HealthItem.Id.CALL_SCREENING -> R.string.health_call_screening_why
        HealthItem.Id.CONTACTS -> R.string.health_contacts_why
        HealthItem.Id.CALENDAR -> R.string.health_calendar_why
        HealthItem.Id.EXACT_ALARMS -> R.string.health_exact_alarms_why
        HealthItem.Id.NOTIFICATION_VOLUME -> R.string.health_notification_volume_why
    }
