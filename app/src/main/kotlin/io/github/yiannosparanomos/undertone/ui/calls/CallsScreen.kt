package io.github.yiannosparanomos.undertone.ui.calls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.yiannosparanomos.undertone.R
import io.github.yiannosparanomos.undertone.core.data.settings.AllowListDto
import io.github.yiannosparanomos.undertone.core.designsystem.UtLabel
import io.github.yiannosparanomos.undertone.core.designsystem.UtListRow
import io.github.yiannosparanomos.undertone.core.designsystem.UtPanel
import io.github.yiannosparanomos.undertone.core.designsystem.UtRowDivider
import io.github.yiannosparanomos.undertone.core.designsystem.UtSegmentedChoice
import io.github.yiannosparanomos.undertone.core.designsystem.UtSwitchRow
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatGroup
import io.github.yiannosparanomos.undertone.ui.common.UtScreen

/** Calls: the ring tier, the allow list, and the repeat-caller rule (spec §5). */
@Composable
fun CallsScreen(
    onBack: () -> Unit,
    viewModel: CallsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var newNumber by rememberSaveable { mutableStateOf("") }

    UtScreen(title = stringResource(R.string.nav_calls), onBack = onBack) {
        UtPanel {
            UtLabel(stringResource(R.string.calls_tier_label))
            UtSegmentedChoice(
                options =
                    listOf(
                        stringResource(R.string.tier_everyone),
                        stringResource(R.string.tier_contacts),
                        stringResource(R.string.tier_allow_list),
                        stringResource(R.string.tier_nobody),
                    ),
                selectedIndex = state.tier.ordinal,
                onSelect = { viewModel.setTier(CallTier.entries[it]) },
            )
            Text(
                text = stringResource(R.string.calls_never_rejected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        UtPanel {
            UtLabel(stringResource(R.string.calls_allow_list_label))
            UtSegmentedChoice(
                options =
                    listOf(
                        stringResource(R.string.calls_allow_starred),
                        stringResource(R.string.calls_allow_expanded),
                    ),
                selectedIndex = state.allowList.mode.ordinal,
                onSelect = { viewModel.setAllowListMode(AllowListDto.Mode.entries[it]) },
            )
            Text(
                text = stringResource(R.string.calls_allow_starred_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.allowList.mode == AllowListDto.Mode.EXPANDED) {
                UtRowDivider()
                UtLabel(stringResource(R.string.calls_allow_numbers))
                if (state.allowList.numbers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.calls_allow_numbers_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.allowList.numbers.forEach { number ->
                        UtListRow(
                            title = number,
                            value = stringResource(R.string.calls_allow_remove),
                            onClick = { viewModel.removeNumber(number) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newNumber,
                        onValueChange = { newNumber = it },
                        label = { Text(stringResource(R.string.calls_allow_add_number)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = {
                        viewModel.addNumber(newNumber)
                        newNumber = ""
                    }) {
                        Text(stringResource(R.string.action_add))
                    }
                }
            }
        }

        UtPanel {
            UtLabel(stringResource(R.string.calls_repeat_label))
            UtSwitchRow(
                title = stringResource(R.string.calls_repeat_enabled),
                subtitle = stringResource(R.string.calls_repeat_detail),
                checked = state.repeatCallers.enabled,
                onCheckedChange = viewModel::setRepeatEnabled,
            )
            if (state.repeatCallers.enabled) {
                UtRowDivider()
                UtLabel(stringResource(R.string.calls_repeat_group))
                UtSegmentedChoice(
                    options =
                        listOf(
                            stringResource(R.string.calls_repeat_group_allow_list),
                            stringResource(R.string.calls_repeat_group_everyone),
                        ),
                    selectedIndex = state.repeatCallers.group.ordinal,
                    onSelect = { viewModel.setRepeatGroup(RepeatGroup.entries[it]) },
                )

                UtLabel(stringResource(R.string.calls_repeat_calls))
                UtSegmentedChoice(
                    options = listOf("2", "3", "4"),
                    selectedIndex = (state.repeatCallers.callsNeeded - 2).coerceIn(0, 2),
                    onSelect = { viewModel.setCallsNeeded(it + 2) },
                )

                UtLabel(stringResource(R.string.calls_repeat_window))
                UtSegmentedChoice(
                    options = viewModel.windowChoices.map { stringResource(R.string.calls_minutes, it.toInt()) },
                    selectedIndex =
                        viewModel.windowChoices
                            .indexOf(state.repeatCallers.window.toMinutes())
                            .coerceAtLeast(0),
                    onSelect = { viewModel.setWindowMinutes(viewModel.windowChoices[it]) },
                )
            }
        }
    }
}
