package io.github.yiannosparanomos.undertone.ui.calls

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.yiannosparanomos.undertone.UndertoneContainer
import io.github.yiannosparanomos.undertone.core.data.settings.AllowListDto
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatCallerSettings
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Duration

data class CallsUiState(
    val tier: CallTier = CallTier.ALLOW_LIST,
    val allowList: AllowListDto = AllowListDto(),
    val repeatCallers: RepeatCallerSettings = RepeatCallerSettings(),
)

class CallsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val container = UndertoneContainer.get(application)

    private val state = MutableStateFlow(CallsUiState())
    val uiState: StateFlow<CallsUiState> = state.asStateFlow()

    /** The windows the spec offers: 2, 5 or 15 minutes. */
    val windowChoices = listOf(2L, 5L, 15L)

    init {
        viewModelScope.launch {
            combine(container.settings.settings, container.settings.allowList) { settings, allowList ->
                CallsUiState(settings.defaultCallTier, allowList, settings.repeatCallers)
            }.collect { state.value = it }
        }
    }

    fun setTier(tier: CallTier) {
        viewModelScope.launch { container.settings.updateSettings { it.copy(defaultCallTier = tier) } }
    }

    fun setAllowListMode(mode: AllowListDto.Mode) {
        viewModelScope.launch { container.settings.updateAllowList { it.copy(mode = mode) } }
    }

    fun addNumber(number: String) {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            container.settings.updateAllowList { list ->
                if (list.numbers.any { it.filter(Char::isDigit) == trimmed.filter(Char::isDigit) }) {
                    list
                } else {
                    list.copy(numbers = list.numbers + trimmed)
                }
            }
        }
    }

    fun removeNumber(number: String) {
        viewModelScope.launch {
            container.settings.updateAllowList { it.copy(numbers = it.numbers - number) }
        }
    }

    fun setRepeatEnabled(enabled: Boolean) = updateRepeat { it.copy(enabled = enabled) }

    fun setRepeatGroup(group: RepeatGroup) = updateRepeat { it.copy(group = group) }

    /** The engine requires at least two calls for a repeat to mean anything. */
    fun setCallsNeeded(calls: Int) = updateRepeat { it.copy(callsNeeded = calls.coerceAtLeast(2)) }

    fun setWindowMinutes(minutes: Long) = updateRepeat { it.copy(window = Duration.ofMinutes(minutes)) }

    private fun updateRepeat(transform: (RepeatCallerSettings) -> RepeatCallerSettings) {
        viewModelScope.launch {
            container.settings.updateSettings { it.copy(repeatCallers = transform(it.repeatCallers)) }
        }
    }
}
