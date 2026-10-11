package io.github.yiannosparanomos.undertone.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yiannosparanomos.undertone.R
import io.github.yiannosparanomos.undertone.core.designsystem.UnderToneTheme
import io.github.yiannosparanomos.undertone.core.designsystem.UtLabel
import io.github.yiannosparanomos.undertone.core.designsystem.UtPanel
import io.github.yiannosparanomos.undertone.core.designsystem.UtSegmentedChoice
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationEngine
import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.domain.model.UndertoneSettings
import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import java.time.LocalDate
import java.time.LocalTime

/**
 * First version of Home: the profile switch and a live preview of one rule decision.
 * Settings are hard-coded until the data layer (core:data) exists.
 */
@Composable
fun HomeScreen() {
    var profileIndex by rememberSaveable { mutableIntStateOf(0) }
    val profile = Profile.entries[profileIndex]

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            UtPanel {
                UtLabel(stringResource(R.string.home_profile_label))
                UtSegmentedChoice(
                    options =
                        listOf(
                            stringResource(R.string.profile_my_rules),
                            stringResource(R.string.profile_allow_all),
                            stringResource(R.string.profile_system_dnd),
                        ),
                    selectedIndex = profileIndex,
                    onSelect = { profileIndex = it },
                )
            }

            UtPanel {
                UtLabel(stringResource(R.string.home_status_label))
                Text(
                    text = stringResource(statusText(profile)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            UtPanel {
                UtLabel(stringResource(R.string.home_preview_label))
                Text(
                    text = stringResource(R.string.home_preview_body, previewDecision(profile)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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
}

private fun statusText(profile: Profile): Int =
    when (profile) {
        Profile.MY_RULES -> R.string.home_status_my_rules
        Profile.ALLOW_ALL -> R.string.home_status_allow_all
        Profile.SYSTEM_DND -> R.string.home_status_system_dnd
    }

private const val TEAMS = "com.microsoft.teams"

/** Runs the real rule engine on a sample rule, so the screen shows engine output end to end. */
private fun previewDecision(profile: Profile): String {
    val settings =
        UndertoneSettings(
            appRules =
                mapOf(
                    TEAMS to
                        AppRule(
                            packageName = TEAMS,
                            inHours = Level.RING,
                            outsideHours = Level.HIDDEN,
                            hours =
                                listOf(
                                    WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                                ),
                        ),
                ),
        )
    val saturday = LocalDate.of(2026, 10, 17).atTime(11, 0)
    val decision = NotificationEngine.decide(TEAMS, saturday, RuleState(profile, settings))
    return decision.releaseAt?.let { "${decision.outcome} until ${it.dayOfWeek} ${it.toLocalTime()}" }
        ?: decision.outcome.name
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HomeScreenPreview() {
    UnderToneTheme(darkTheme = true) {
        HomeScreen()
    }
}
