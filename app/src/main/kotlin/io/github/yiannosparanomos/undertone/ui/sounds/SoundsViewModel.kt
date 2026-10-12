package io.github.yiannosparanomos.undertone.ui.sounds

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.yiannosparanomos.undertone.UndertoneContainer
import io.github.yiannosparanomos.undertone.core.data.settings.SoundsDto
import io.github.yiannosparanomos.undertone.core.system.sound.SoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SoundsUiState(
    val sounds: SoundsDto = SoundsDto(),
    val audible: Boolean = true,
)

class SoundsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val container = UndertoneContainer.get(application)
    private val player = SoundPlayer.from(application)

    private val state = MutableStateFlow(SoundsUiState())
    val uiState: StateFlow<SoundsUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            container.settings.sounds.collect { sounds ->
                // Decided from the stream, never the ringer mode (ADR 0006).
                state.value = SoundsUiState(sounds, player.wouldBeAudible())
            }
        }
    }

    fun setVibrate(vibrate: Boolean) {
        viewModelScope.launch { container.settings.updateSounds { it.copy(vibrate = vibrate) } }
    }

    fun setVolume(percent: Int) {
        viewModelScope.launch {
            container.settings.updateSounds { it.copy(volumePercent = percent.coerceIn(0, 100)) }
        }
    }

    /** The Test button from spec §6: plays exactly what a Ring app would sound like. */
    fun test() {
        player.play(packageName = "", sounds = state.value.sounds)
    }
}
