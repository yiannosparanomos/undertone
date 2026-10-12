package io.github.yiannosparanomos.undertone.ui.rules

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
import io.github.yiannosparanomos.undertone.core.designsystem.UtLabel
import io.github.yiannosparanomos.undertone.core.designsystem.UtListRow
import io.github.yiannosparanomos.undertone.core.designsystem.UtPanel
import io.github.yiannosparanomos.undertone.core.designsystem.UtRowDivider
import io.github.yiannosparanomos.undertone.core.designsystem.UtSegmentedChoice
import io.github.yiannosparanomos.undertone.core.designsystem.UtSwitchRow
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.EventMatch
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.ui.apps.UtLevelChoice
import io.github.yiannosparanomos.undertone.ui.common.UtScreen

/**
 * Rules: calendar rules, how long history is kept, and settings export and import.
 *
 * Export and import go through the system file picker, so the file is wherever the user chooses
 * and UnderTone never writes anything outside its own storage on its own (spec §9).
 */
@Composable
fun RulesScreen(
    onBack: () -> Unit,
    viewModel: RulesViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val exportDone = stringResource(R.string.rules_export_done)
    val importDone = stringResource(R.string.rules_import_done)
    val importFailed = stringResource(R.string.rules_import_failed, "%s")

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let {
                context.contentResolver.openOutputStream(it)?.let { out -> viewModel.export(out, exportDone) }
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                context.contentResolver.openInputStream(it)?.let { input ->
                    viewModel.import(input, importDone) { reason -> importFailed.format(reason) }
                }
            }
        }

    UtScreen(title = stringResource(R.string.nav_rules), onBack = onBack) {
        state.message?.let { message ->
            UtPanel {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(onClick = viewModel::clearMessage) {
                    Text(stringResource(R.string.action_done))
                }
            }
        }

        UtPanel {
            UtLabel(stringResource(R.string.rules_calendar_label))
            Text(
                text = stringResource(R.string.rules_stricter_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!state.hasCalendarPermission) {
                Text(
                    text = stringResource(R.string.rules_calendar_permission),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.rules_calendar_grant))
                }
            } else if (state.calendarRules.isEmpty()) {
                Text(
                    text = stringResource(R.string.rules_calendar_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.calendarRules.forEachIndexed { index, rule ->
                if (index > 0) UtRowDivider()
                UtLabel(
                    when (val match = rule.match) {
                        is EventMatch.WholeCalendar ->
                            "${stringResource(R.string.rules_match_calendar)}: " +
                                viewModel.calendarName(match.calendarId)
                        is EventMatch.Series -> stringResource(R.string.rules_match_series)
                        is EventMatch.SingleEvent -> stringResource(R.string.rules_match_series)
                        is EventMatch.TitleContains ->
                            "${stringResource(R.string.rules_match_title)} \"${match.text}\""
                    },
                )
                UtLabel(stringResource(R.string.rules_others_level))
                UtLevelChoice(
                    selected = rule.othersLevel,
                    onSelect = { viewModel.setOthersLevel(rule.id, it) },
                )
                UtLabel(stringResource(R.string.rules_call_tier))
                UtSegmentedChoice(
                    options =
                        listOf(
                            stringResource(R.string.tier_everyone),
                            stringResource(R.string.tier_contacts),
                            stringResource(R.string.tier_allow_list),
                            stringResource(R.string.tier_nobody),
                        ),
                    selectedIndex = rule.callTier.ordinal,
                    onSelect = { viewModel.setCallTier(rule.id, CallTier.entries[it]) },
                )
                UtSwitchRow(
                    title = stringResource(R.string.rules_all_day),
                    checked = rule.includeAllDayEvents,
                    onCheckedChange = { viewModel.setIncludeAllDay(rule.id, it) },
                )
                UtListRow(
                    title = stringResource(R.string.rules_remove),
                    onClick = { viewModel.removeRule(rule.id) },
                )
            }

            if (state.hasCalendarPermission) {
                UtRowDivider()
                UtLabel(stringResource(R.string.rules_calendar_add))
                if (state.calendars.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rules_calendar_no_calendars),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.calendars.forEach { calendar ->
                        UtListRow(
                            title = calendar.displayName,
                            onClick = { viewModel.addCalendarRule(calendar.id) },
                        )
                    }
                }
            }
        }

        UtPanel {
            UtLabel(stringResource(R.string.rules_retention_label))
            UtSegmentedChoice(
                options = viewModel.retentionChoices.map { stringResource(R.string.rules_retention_days, it) },
                selectedIndex = viewModel.retentionChoices.indexOf(state.retentionDays).coerceAtLeast(0),
                onSelect = { viewModel.setRetentionDays(viewModel.retentionChoices[it]) },
            )
        }

        UtPanel {
            OutlinedButton(
                onClick = { exportLauncher.launch("undertone-settings.json") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.rules_export))
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.rules_import))
            }
        }
    }
}
