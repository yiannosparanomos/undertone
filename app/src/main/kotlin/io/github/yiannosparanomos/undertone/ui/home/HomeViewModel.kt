package io.github.yiannosparanomos.undertone.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.yiannosparanomos.undertone.UndertoneContainer
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** What Home shows: the profile, any override, and the two counts worth seeing at a glance. */
data class HomeUiState(
    val profile: Profile = Profile.MY_RULES,
    val override: Override? = null,
    val heldCount: Int = 0,
    val blockedToday: Int = 0,
)

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val container = UndertoneContainer.get(application)

    private val state = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.settings.profile,
                // Already filtered by the clock, so an expired override reads as none.
                container.settings.override(LocalDateTime.now()),
                container.history.heldCount,
                container.history.blockedTotalOn(LocalDate.now()),
            ) { profile, override, held, blocked ->
                HomeUiState(profile, override, held, blocked)
            }.collect { state.value = it }
        }
    }

    fun setProfile(profile: Profile) {
        viewModelScope.launch { container.settings.setProfile(profile) }
    }

    /** "Quiet 1h" from the UI direction's quick actions. */
    fun quietForAnHour() {
        viewModelScope.launch {
            container.settings.setOverride(Override.Quiet(until = LocalDateTime.now().plusHours(1)))
        }
    }

    /** "Let everything through for 30 min". Blocked apps stay blocked (invariant 5). */
    fun letThroughForHalfAnHour() {
        viewModelScope.launch {
            container.settings.setOverride(Override.LetThrough(until = LocalDateTime.now().plusMinutes(30)))
        }
    }

    fun endOverride() {
        viewModelScope.launch { container.settings.setOverride(null) }
    }
}
