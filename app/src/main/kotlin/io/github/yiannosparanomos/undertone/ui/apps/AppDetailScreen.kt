package io.github.yiannosparanomos.undertone.ui.apps

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.yiannosparanomos.undertone.R
import io.github.yiannosparanomos.undertone.core.designsystem.UtLabel
import io.github.yiannosparanomos.undertone.core.designsystem.UtListRow
import io.github.yiannosparanomos.undertone.core.designsystem.UtPanel
import io.github.yiannosparanomos.undertone.core.designsystem.UtRowDivider
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import io.github.yiannosparanomos.undertone.ui.common.Formatting
import io.github.yiannosparanomos.undertone.ui.common.UtScreen
import java.time.LocalTime

/**
 * One app's rule: its level inside and outside its hours, and the hours themselves.
 *
 * Hours are chosen from a few named windows rather than a free time picker. The spec's examples
 * are all of this shape ("Mon-Fri 09:00-18:00"), and a list of presets is far quicker to set and
 * to read back than four spinners. A custom editor can come later if anyone wants one.
 */
@Composable
fun AppDetailScreen(
    packageName: String,
    onBack: () -> Unit,
    viewModel: AppsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val row =
        (state.focusApps + state.blockedApps + state.otherApps)
            .firstOrNull { it.packageName == packageName }
    val rule = row?.rule ?: state.defaultRule.copy(packageName = packageName)
    val label = row?.label ?: viewModel.labelFor(packageName)

    UtScreen(title = label, onBack = onBack) {
        UtPanel {
            UtLabel(stringResource(R.string.apps_in_hours))
            UtLevelChoice(
                selected = rule.inHours,
                onSelect = { viewModel.setLevels(packageName, it, rule.outsideHours) },
            )
        }

        UtPanel {
            UtLabel(stringResource(R.string.apps_outside_hours))
            UtLevelChoice(
                selected = rule.outsideHours,
                onSelect = { viewModel.setLevels(packageName, rule.inHours, it) },
            )
            if (rule.hours.isEmpty()) {
                Text(
                    text = stringResource(R.string.apps_hours_always),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        UtPanel {
            UtLabel(stringResource(R.string.apps_hours))
            if (rule.hours.isEmpty()) {
                Text(
                    text = stringResource(R.string.apps_hours_always),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                rule.hours.forEachIndexed { index, window ->
                    if (index > 0) UtRowDivider()
                    UtListRow(
                        title = Formatting.window(window),
                        value = stringResource(R.string.apps_hours_remove),
                        onClick = { viewModel.setHours(packageName, rule.hours - window) },
                    )
                }
            }
            UtRowDivider()
            UtLabel(stringResource(R.string.apps_hours_add))
            HoursPresets.forEach { preset ->
                if (preset.window !in rule.hours) {
                    UtListRow(
                        title = stringResource(preset.labelRes),
                        onClick = { viewModel.setHours(packageName, rule.hours + preset.window) },
                    )
                }
            }
        }

        if (row?.rule != null) {
            UtPanel {
                OutlinedButton(
                    onClick = {
                        viewModel.removeRule(packageName)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.apps_remove_rule))
                }
            }
        }

        if (rule.inHours != Level.BLOCKED) {
            UtPanel {
                TextButton(onClick = { viewModel.blockApp(packageName) }) {
                    Text(stringResource(R.string.log_block_app))
                }
            }
        }
    }
}

/** The named windows the hours editor offers. */
private data class HoursPreset(
    val labelRes: Int,
    val window: WeeklyWindow,
)

private val HoursPresets =
    listOf(
        HoursPreset(
            R.string.apps_hours_weekdays,
            WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0)),
        ),
        HoursPreset(
            R.string.apps_hours_weekend,
            WeeklyWindow(WeeklyWindow.WEEKEND, LocalTime.of(10, 0), LocalTime.of(23, 0)),
        ),
        HoursPreset(
            R.string.apps_hours_evening,
            WeeklyWindow(WeeklyWindow.EVERY_DAY, LocalTime.of(18, 0), LocalTime.of(23, 0)),
        ),
        // Overnight: end is not after start, which WeeklyWindow reads as running to the morning.
        HoursPreset(
            R.string.apps_hours_night,
            WeeklyWindow(WeeklyWindow.EVERY_DAY, LocalTime.of(22, 0), LocalTime.of(7, 0)),
        ),
    )
