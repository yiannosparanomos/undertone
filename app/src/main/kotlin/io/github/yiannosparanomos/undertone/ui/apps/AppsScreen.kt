package io.github.yiannosparanomos.undertone.ui.apps

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.yiannosparanomos.undertone.R
import io.github.yiannosparanomos.undertone.core.designsystem.UtLabel
import io.github.yiannosparanomos.undertone.core.designsystem.UtListRow
import io.github.yiannosparanomos.undertone.core.designsystem.UtPanel
import io.github.yiannosparanomos.undertone.core.designsystem.UtRowDivider
import io.github.yiannosparanomos.undertone.ui.common.Formatting
import io.github.yiannosparanomos.undertone.ui.common.UtScreen

/**
 * Apps: the All apps default, the apps with rules of their own, the blocked ones, and everything
 * else to pick from (roadmap M5).
 */
@Composable
fun AppsScreen(
    onBack: () -> Unit,
    onOpenApp: (String) -> Unit,
    viewModel: AppsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    UtScreen(title = stringResource(R.string.nav_apps), onBack = onBack) {
        UtPanel {
            UtLabel(stringResource(R.string.apps_default_label))
            Text(
                text = stringResource(R.string.apps_default_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            UtLevelChoice(selected = state.defaultRule.inHours, onSelect = viewModel::setDefaultLevel)
        }

        UtPanel {
            UtLabel(stringResource(R.string.apps_focus_label))
            if (state.focusApps.isEmpty()) {
                Text(
                    text = stringResource(R.string.apps_focus_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.focusApps.forEachIndexed { index, app ->
                    if (index > 0) UtRowDivider()
                    UtListRow(
                        title = app.label,
                        subtitle = app.rule?.let { Formatting.windows(it.hours) },
                        value = app.rule?.let { levelName(it.inHours) },
                        onClick = { onOpenApp(app.packageName) },
                    )
                }
            }
        }

        if (state.blockedApps.isNotEmpty()) {
            UtPanel {
                UtLabel(stringResource(R.string.apps_blocked_label))
                state.blockedApps.forEachIndexed { index, app ->
                    if (index > 0) UtRowDivider()
                    UtListRow(
                        title = app.label,
                        value = stringResource(R.string.level_blocked),
                        onClick = { onOpenApp(app.packageName) },
                    )
                }
                TextButton(onClick = { state.blockedApps.forEach { viewModel.removeRule(it.packageName) } }) {
                    Text(stringResource(R.string.apps_restore))
                }
            }
        }

        UtPanel {
            UtLabel(stringResource(R.string.apps_all_label))
            state.otherApps.forEachIndexed { index, app ->
                if (index > 0) UtRowDivider()
                UtListRow(
                    title = app.label,
                    value = levelName(state.defaultRule.inHours),
                    onClick = {
                        // Giving it a rule is what makes it a focus app; the detail screen then
                        // edits that rule.
                        viewModel.addFocusApp(app.packageName)
                        onOpenApp(app.packageName)
                    },
                )
            }
        }
    }
}
