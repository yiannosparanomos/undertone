package io.github.yiannosparanomos.undertone.ui.held

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.yiannosparanomos.undertone.ui.common.Formatting
import io.github.yiannosparanomos.undertone.ui.common.UtScreen
import java.time.LocalDateTime

/**
 * Held and log: what is waiting, and the history of everything silenced or held.
 *
 * Notification titles and text are shown here and nowhere else — this is the one place they are
 * meant to be read, by the person they were sent to (invariant 2).
 */
@Composable
fun HeldAndLogScreen(
    onBack: () -> Unit,
    viewModel: HeldAndLogViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val now = remember { LocalDateTime.now() }
    var expandedEntry by remember { mutableStateOf<Long?>(null) }

    UtScreen(title = stringResource(R.string.nav_held), onBack = onBack) {
        if (state.suggestions.isNotEmpty()) {
            UtPanel {
                UtLabel(stringResource(R.string.suggestions_label))
                state.suggestions.forEach { suggestion ->
                    Text(
                        text =
                            stringResource(
                                R.string.suggestions_body,
                                suggestion.swipedAway,
                                viewModel.labelFor(suggestion.packageName),
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(onClick = { viewModel.silenceApp(suggestion.packageName) }) {
                        Text(stringResource(R.string.log_silence_app))
                    }
                    TextButton(onClick = { viewModel.blockApp(suggestion.packageName) }) {
                        Text(stringResource(R.string.log_block_app))
                    }
                }
            }
        }

        UtPanel {
            UtLabel(stringResource(R.string.held_inbox_label))
            if (state.held.isEmpty()) {
                Text(
                    text = stringResource(R.string.held_inbox_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.held.forEachIndexed { index, held ->
                    if (index > 0) UtRowDivider()
                    UtListRow(
                        title = held.title ?: viewModel.labelFor(held.packageName),
                        subtitle = held.text,
                        value =
                            held.releaseAt?.let { stringResource(R.string.held_until, Formatting.moment(it, now)) }
                                ?: stringResource(R.string.held_until_unknown),
                        onClick = { viewModel.release(held.notificationKey) },
                    )
                }
                OutlinedButton(onClick = viewModel::releaseAll, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.held_release_all))
                }
            }
        }

        UtPanel {
            UtLabel(stringResource(R.string.log_label))
            if (state.log.isEmpty()) {
                Text(
                    text = stringResource(R.string.log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.log.forEachIndexed { index, entry ->
                    if (index > 0) UtRowDivider()
                    UtListRow(
                        title = entry.title ?: viewModel.labelFor(entry.packageName),
                        subtitle = entry.text ?: viewModel.labelFor(entry.packageName),
                        value = Formatting.moment(entry.postedAt, now),
                        // Tapping expands the row's actions. The spec asks for long-press; a
                        // visible row of actions is easier to reach with TalkBack and does the
                        // same job, so the actions are shown rather than hidden behind a gesture.
                        onClick = { expandedEntry = if (expandedEntry == entry.id) null else entry.id },
                    )
                    if (expandedEntry == entry.id) {
                        viewModel.openAppIntent(entry.packageName)?.let { intent ->
                            TextButton(onClick = { context.startActivity(intent) }) {
                                Text(stringResource(R.string.log_open_app))
                            }
                        }
                        TextButton(onClick = { viewModel.silenceApp(entry.packageName) }) {
                            Text(stringResource(R.string.log_silence_app))
                        }
                        TextButton(onClick = { viewModel.blockApp(entry.packageName) }) {
                            Text(stringResource(R.string.log_block_app))
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.log_retention_note, state.retentionDays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
