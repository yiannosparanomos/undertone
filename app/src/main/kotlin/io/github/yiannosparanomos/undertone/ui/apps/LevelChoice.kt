package io.github.yiannosparanomos.undertone.ui.apps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.yiannosparanomos.undertone.R
import io.github.yiannosparanomos.undertone.core.designsystem.UtSegmentedChoice
import io.github.yiannosparanomos.undertone.core.domain.model.Level

/** The four levels, in the order the spec declares them: Ring, Silent, Hidden, Blocked. */
@Composable
fun levelNames(): List<String> =
    listOf(
        stringResource(R.string.level_ring),
        stringResource(R.string.level_silent),
        stringResource(R.string.level_hidden),
        stringResource(R.string.level_blocked),
    )

@Composable
fun levelName(level: Level): String = levelNames()[level.ordinal]

/** A one-of-four level picker, used for the default rule and for each app's two levels. */
@Composable
fun UtLevelChoice(
    selected: Level,
    onSelect: (Level) -> Unit,
    modifier: Modifier = Modifier,
) {
    UtSegmentedChoice(
        options = levelNames(),
        selectedIndex = selected.ordinal,
        onSelect = { onSelect(Level.entries[it]) },
        modifier = modifier,
    )
}
