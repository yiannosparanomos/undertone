package io.github.yiannosparanomos.undertone.ui.health

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import io.github.yiannosparanomos.undertone.UndertoneContainer
import io.github.yiannosparanomos.undertone.core.system.sound.SoundPlayer
import io.github.yiannosparanomos.undertone.core.system.watchdog.Watchdog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One thing UnderTone needs, and whether it has it. */
data class HealthItem(
    val id: Id,
    val granted: Boolean,
    val required: Boolean,
) {
    enum class Id {
        NOTIFICATION_ACCESS,
        DND_ACCESS,
        CALL_SCREENING,
        CONTACTS,
        CALENDAR,
        EXACT_ALARMS,
        NOTIFICATION_VOLUME,
    }
}

data class HealthUiState(
    val items: List<HealthItem> = emptyList(),
) {
    /** Only the required ones count as problems; calendar is optional by design. */
    val problems: Int get() = items.count { it.required && !it.granted }
}

/**
 * The Health check screen (spec §10, device checklist D1 and D5).
 *
 * Everything is read fresh on each check rather than observed, because these are system settings
 * the user changes in Android's own screens and comes back from.
 */
class HealthCheckViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val container = UndertoneContainer.get(application)
    private val player = SoundPlayer.from(application)

    private val state = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = state.asStateFlow()

    init {
        check()
    }

    fun check() {
        val context = getApplication<Application>()
        state.value =
            HealthUiState(
                items =
                    listOf(
                        HealthItem(
                            HealthItem.Id.NOTIFICATION_ACCESS,
                            Watchdog.isListenerEnabled(context),
                            required = true,
                        ),
                        HealthItem(HealthItem.Id.DND_ACCESS, container.dnd.hasPolicyAccess, required = true),
                        HealthItem(HealthItem.Id.CALL_SCREENING, isCallScreeningRoleHeld(), required = true),
                        HealthItem(
                            HealthItem.Id.CONTACTS,
                            isGranted(Manifest.permission.READ_CONTACTS),
                            required = true,
                        ),
                        HealthItem(
                            HealthItem.Id.CALENDAR,
                            isGranted(Manifest.permission.READ_CALENDAR),
                            required = false,
                        ),
                        HealthItem(HealthItem.Id.EXACT_ALARMS, canScheduleExactAlarms(), required = true),
                        HealthItem(
                            HealthItem.Id.NOTIFICATION_VOLUME,
                            player.wouldBeAudible(),
                            required = false,
                        ),
                    ),
            )
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(getApplication(), permission) == PackageManager.PERMISSION_GRANTED

    private fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getApplication<Application>().getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }

    /**
     * The system screen that grants one item.
     *
     * Runtime permissions (contacts, calendar) are requested by the setup tour, which owns the
     * prominent-disclosure screen (M6); here the app's settings page is the honest destination
     * rather than a bare permission dialog with no explanation.
     */
    fun fixIntent(item: HealthItem): Intent? {
        val packageName = getApplication<Application>().packageName
        val appUri = Uri.fromParts("package", packageName, null)
        return when (item.id) {
            HealthItem.Id.NOTIFICATION_ACCESS ->
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            HealthItem.Id.DND_ACCESS ->
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            HealthItem.Id.CALL_SCREENING ->
                getApplication<Application>()
                    .getSystemService(RoleManager::class.java)
                    ?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            HealthItem.Id.CONTACTS, HealthItem.Id.CALENDAR ->
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    appUri,
                )
            HealthItem.Id.EXACT_ALARMS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        appUri,
                    )
                } else {
                    null
                }
            HealthItem.Id.NOTIFICATION_VOLUME ->
                Intent(Settings.ACTION_SOUND_SETTINGS)
        }
    }

    /**
     * Whether UnderTone holds the call-screening role.
     *
     * `RoleManager.isRoleHeld` needs API 29, which is our minSdk, so it is read directly.
     */
    private fun isCallScreeningRoleHeld(): Boolean =
        runCatching {
            val roleManager =
                getApplication<Application>().getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        }.getOrDefault(false)
}
