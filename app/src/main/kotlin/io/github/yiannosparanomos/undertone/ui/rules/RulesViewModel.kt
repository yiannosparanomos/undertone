package io.github.yiannosparanomos.undertone.ui.rules

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.yiannosparanomos.undertone.UndertoneContainer
import io.github.yiannosparanomos.undertone.core.data.settings.toDomain
import io.github.yiannosparanomos.undertone.core.domain.model.CalendarRule
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.EventMatch
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.system.calendar.CalendarReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class RulesUiState(
    val calendarRules: List<CalendarRule> = emptyList(),
    val calendars: List<CalendarReader.Calendar> = emptyList(),
    val hasCalendarPermission: Boolean = false,
    val retentionDays: Int = 7,
    val message: String? = null,
)

class RulesViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val container = UndertoneContainer.get(application)

    private val state = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = state.asStateFlow()

    val retentionChoices = listOf(3, 7, 14, 30)

    init {
        viewModelScope.launch {
            container.settings.state.collect { stored ->
                state.value =
                    state.value.copy(
                        calendarRules = stored.settings.toDomain().calendarRules,
                        retentionDays = stored.retentionDays,
                    )
            }
        }
        refreshCalendars()
    }

    fun refreshCalendars() {
        viewModelScope.launch {
            val granted =
                ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_CALENDAR) ==
                    PackageManager.PERMISSION_GRANTED
            val calendars = if (granted) withContext(Dispatchers.IO) { container.calendar.calendars() } else emptyList()
            state.value = state.value.copy(hasCalendarPermission = granted, calendars = calendars)
        }
    }

    /**
     * Adds a rule covering a whole calendar: the simplest useful rule and the one the spec leads
     * with. Narrowing it to a series or a title is an edit of the rule afterwards.
     */
    fun addCalendarRule(calendarId: String) {
        viewModelScope.launch {
            container.settings.updateSettings { settings ->
                settings.copy(
                    calendarRules =
                        settings.calendarRules +
                            CalendarRule(
                                id = UUID.randomUUID().toString(),
                                match = EventMatch.WholeCalendar(calendarId),
                                ringApps = emptySet(),
                                othersLevel = Level.SILENT,
                                callTier = CallTier.ALLOW_LIST,
                            ),
                )
            }
        }
    }

    fun setOthersLevel(
        ruleId: String,
        level: Level,
    ) = updateRule(ruleId) { it.copy(othersLevel = level) }

    fun setCallTier(
        ruleId: String,
        tier: CallTier,
    ) = updateRule(ruleId) { it.copy(callTier = tier) }

    fun setIncludeAllDay(
        ruleId: String,
        include: Boolean,
    ) = updateRule(ruleId) { it.copy(includeAllDayEvents = include) }

    fun setTitleMatch(
        ruleId: String,
        text: String,
    ) = updateRule(ruleId) { it.copy(match = EventMatch.TitleContains(text)) }

    fun removeRule(ruleId: String) {
        viewModelScope.launch {
            container.settings.updateSettings { settings ->
                settings.copy(calendarRules = settings.calendarRules.filterNot { it.id == ruleId })
            }
        }
    }

    private fun updateRule(
        ruleId: String,
        transform: (CalendarRule) -> CalendarRule,
    ) {
        viewModelScope.launch {
            container.settings.updateSettings { settings ->
                settings.copy(
                    calendarRules = settings.calendarRules.map { if (it.id == ruleId) transform(it) else it },
                )
            }
        }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch { container.settings.setRetentionDays(days) }
    }

    fun calendarName(calendarId: String): String =
        state.value.calendars
            .firstOrNull { it.id == calendarId }
            ?.displayName ?: calendarId

    /** Settings export: rules only, never notification content (invariant 2). */
    fun export(
        output: OutputStream,
        doneMessage: String,
    ) {
        viewModelScope.launch {
            runCatching { output.use { container.settings.export(it) } }
                .onSuccess { state.value = state.value.copy(message = doneMessage) }
                .onFailure { state.value = state.value.copy(message = it.message) }
        }
    }

    fun import(
        input: InputStream,
        doneMessage: String,
        failureTemplate: (String) -> String,
    ) {
        viewModelScope.launch {
            runCatching { input.use { container.settings.import(it) } }
                .onSuccess { state.value = state.value.copy(message = doneMessage) }
                .onFailure {
                    state.value = state.value.copy(message = failureTemplate(it.message ?: ""))
                }
        }
    }

    fun clearMessage() {
        state.value = state.value.copy(message = null)
    }
}
