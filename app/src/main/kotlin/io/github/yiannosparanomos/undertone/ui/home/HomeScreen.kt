package io.github.yiannosparanomos.undertone.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.yiannosparanomos.undertone.Destinations
import io.github.yiannosparanomos.undertone.R
import io.github.yiannosparanomos.undertone.core.designsystem.UtCount
import io.github.yiannosparanomos.undertone.core.designsystem.UtLabel
import io.github.yiannosparanomos.undertone.core.designsystem.UtListRow
import io.github.yiannosparanomos.undertone.core.designsystem.UtPanel
import io.github.yiannosparanomos.undertone.core.designsystem.UtRowDivider
import io.github.yiannosparanomos.undertone.core.designsystem.UtSegmentedChoice
import io.github.yiannosparanomos.undertone.core.designsystem.UtStatus
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.ui.common.Formatting
import io.github.yiannosparanomos.undertone.ui.common.UtScreen
import java.time.LocalDateTime

/**
 * Home answers "what is happening right now, and why?" (UI direction, principle 1) and gets you
 * to every other screen in one tap.
 */
@Composable
fun HomeScreen(
    onOpen: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onOpen = onOpen,
        onProfile = viewModel::setProfile,
        onQuiet = viewModel::quietForAnHour,
        onLetThrough = viewModel::letThroughForHalfAnHour,
        onEndOverride = viewModel::endOverride,
    )
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onOpen: (String) -> Unit,
    onProfile: (Profile) -> Unit,
    onQuiet: () -> Unit,
    onLetThrough: () -> Unit,
    onEndOverride: () -> Unit,
    now: LocalDateTime = LocalDateTime.now(),
) {
    UtScreen(title = stringResource(R.string.app_name)) {
        UtPanel {
            UtLabel(stringResource(R.string.home_profile_label))
            UtSegmentedChoice(
                options =
                    listOf(
                        stringResource(R.string.profile_my_rules),
                        stringResource(R.string.profile_allow_all),
                        stringResource(R.string.profile_system_dnd),
                    ),
                selectedIndex = Profile.entries.indexOf(state.profile),
                onSelect = { onProfile(Profile.entries[it]) },
            )
            UtStatus(
                headline =
                    when (state.profile) {
                        Profile.MY_RULES -> stringResource(R.string.home_status_my_rules)
                        Profile.ALLOW_ALL -> stringResource(R.string.home_status_allow_all)
                        Profile.SYSTEM_DND -> stringResource(R.string.home_status_system_dnd)
                    },
            )
        }

        UtPanel {
            UtLabel(stringResource(R.string.home_override_label))
            Text(
                text =
                    when (val override = state.override) {
                        null -> stringResource(R.string.home_override_none)
                        is Override.Quiet ->
                            stringResource(
                                R.string.home_override_quiet,
                                Formatting.moment(override.until, now),
                            )
                        is Override.LetThrough ->
                            stringResource(
                                R.string.home_override_let_through,
                                Formatting.moment(override.until, now),
                            )
                    },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onQuiet, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_override_quiet_1h))
                }
                OutlinedButton(onClick = onLetThrough, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_override_through_30m))
                }
            }
            if (state.override != null) {
                OutlinedButton(onClick = onEndOverride, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_override_end))
                }
            }
        }

        UtPanel {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                UtCount(state.heldCount, stringResource(R.string.home_held_count))
                UtCount(state.blockedToday, stringResource(R.string.home_blocked_today))
            }
        }

        UtPanel {
            UtListRow(
                title = stringResource(R.string.nav_apps),
                onClick = { onOpen(Destinations.APPS) },
            )
            UtRowDivider()
            UtListRow(
                title = stringResource(R.string.nav_calls),
                onClick = { onOpen(Destinations.CALLS) },
            )
            UtRowDivider()
            UtListRow(
                title = stringResource(R.string.nav_rules),
                onClick = { onOpen(Destinations.RULES) },
            )
            UtRowDivider()
            UtListRow(
                title = stringResource(R.string.nav_held),
                value = if (state.heldCount > 0) state.heldCount.toString() else null,
                onClick = { onOpen(Destinations.HELD) },
            )
            UtRowDivider()
            UtListRow(
                title = stringResource(R.string.nav_sounds),
                onClick = { onOpen(Destinations.SOUNDS) },
            )
            UtRowDivider()
            UtListRow(
                title = stringResource(R.string.nav_health),
                onClick = { onOpen(Destinations.HEALTH) },
            )
        }

        UtPanel {
            UtLabel(stringResource(R.string.home_privacy_label))
            Text(
                text = stringResource(R.string.home_privacy_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
