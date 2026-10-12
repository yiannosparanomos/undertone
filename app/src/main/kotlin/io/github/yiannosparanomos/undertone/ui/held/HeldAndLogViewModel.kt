package io.github.yiannosparanomos.undertone.ui.held

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.yiannosparanomos.undertone.UndertoneContainer
import io.github.yiannosparanomos.undertone.core.data.history.HeldNotification
import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.data.history.LogEntry
import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HeldAndLogUiState(
    val held: List<HeldNotification> = emptyList(),
    val log: List<LogEntry> = emptyList(),
    val suggestions: List<HistoryStore.BlockSuggestion> = emptyList(),
    val retentionDays: Int = 7,
)

class HeldAndLogViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val container = UndertoneContainer.get(application)

    private val state = MutableStateFlow(HeldAndLogUiState())
    val uiState: StateFlow<HeldAndLogUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.history.held,
                container.history.log,
                container.settings.retentionDays,
            ) { held, log, retention ->
                Triple(held, log, retention)
            }.collect { (held, log, retention) ->
                state.value =
                    HeldAndLogUiState(
                        held = held,
                        log = log,
                        // Over the retention period, which is also the window the spec's
                        // "10 swipes in a week" is phrased against.
                        suggestions =
                            container.history.blockSuggestions(LocalDate.now().minusDays(retention.toLong())),
                        retentionDays = retention,
                    )
            }
        }
    }

    fun releaseAll() {
        viewModelScope.launch { container.history.releaseAllHeld() }
    }

    fun release(notificationKey: String) {
        viewModelScope.launch { container.history.releaseHeld(notificationKey) }
    }

    fun blockApp(packageName: String) {
        viewModelScope.launch {
            container.settings.updateSettings { settings ->
                settings.copy(
                    appRules =
                        settings.appRules + (packageName to AppRule(packageName, Level.BLOCKED, Level.BLOCKED)),
                )
            }
        }
    }

    fun silenceApp(packageName: String) {
        viewModelScope.launch {
            container.settings.updateSettings { settings ->
                settings.copy(
                    appRules =
                        settings.appRules + (packageName to AppRule(packageName, Level.SILENT, Level.SILENT)),
                )
            }
        }
    }

    fun labelFor(packageName: String): String = container.apps.labelFor(packageName)

    /**
     * Opens the app a log entry came from (spec §3). Null when the app has no launcher activity
     * any more, in which case the row simply does not offer it.
     */
    fun openAppIntent(packageName: String): Intent? = container.apps.launchIntentFor(packageName)
}
