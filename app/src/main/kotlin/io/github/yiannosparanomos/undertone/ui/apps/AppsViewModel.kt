package io.github.yiannosparanomos.undertone.ui.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.yiannosparanomos.undertone.UndertoneContainer
import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.UndertoneSettings
import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One app as the Apps screen lists it. */
data class AppRow(
    val packageName: String,
    val label: String,
    val rule: AppRule?,
    val isSystemApp: Boolean,
) {
    /** True when this app follows the All apps default rather than one of its own. */
    val usesDefault: Boolean get() = rule == null
}

data class AppsUiState(
    val defaultRule: AppRule = AppRule(AppRule.ALL_APPS, Level.RING),
    val focusApps: List<AppRow> = emptyList(),
    val blockedApps: List<AppRow> = emptyList(),
    val otherApps: List<AppRow> = emptyList(),
    val loading: Boolean = true,
)

class AppsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val container = UndertoneContainer.get(application)

    private val state = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            // Listing launchable apps hits the package manager, so it happens off the main
            // thread once; the rules then stream on top of that list.
            val installed = withContext(Dispatchers.IO) { container.apps.launchableApps() }
            container.settings.settings.collect { settings ->
                state.value = buildState(settings, installed)
            }
        }
    }

    private fun buildState(
        settings: UndertoneSettings,
        installed: List<io.github.yiannosparanomos.undertone.core.system.apps.AppCatalog.LaunchableApp>,
    ): AppsUiState {
        val rows =
            installed.map { app ->
                AppRow(
                    packageName = app.packageName,
                    label = app.label,
                    rule = settings.appRules[app.packageName],
                    isSystemApp = app.isSystemApp,
                )
            }
        // An app with a rule can have been uninstalled since; it still belongs in the list so
        // the user can see and remove the rule.
        val installedPackages = installed.map { it.packageName }.toSet()
        val orphanRows =
            settings.appRules.keys
                .filterNot { it in installedPackages }
                .map { AppRow(it, container.apps.labelFor(it), settings.appRules[it], isSystemApp = false) }

        val all = (rows + orphanRows).sortedBy { it.label.lowercase() }
        return AppsUiState(
            defaultRule = settings.defaultRule,
            focusApps = all.filter { it.rule != null && it.rule.inHours != Level.BLOCKED },
            blockedApps = all.filter { it.rule?.inHours == Level.BLOCKED },
            otherApps = all.filter { it.usesDefault },
            loading = false,
        )
    }

    fun setDefaultLevel(level: Level) {
        viewModelScope.launch {
            container.settings.updateSettings {
                it.copy(
                    defaultRule = it.defaultRule.copy(inHours = level, outsideHours = level),
                )
            }
        }
    }

    /** Gives an app its own rule, starting from whatever the default currently is. */
    fun addFocusApp(packageName: String) {
        viewModelScope.launch {
            container.settings.updateSettings { settings ->
                if (settings.appRules.containsKey(packageName)) {
                    settings
                } else {
                    settings.copy(
                        appRules =
                            settings.appRules +
                                (
                                    packageName to
                                        AppRule(
                                            packageName = packageName,
                                            inHours = settings.defaultRule.inHours,
                                            outsideHours = settings.defaultRule.outsideHours,
                                        )
                                ),
                    )
                }
            }
        }
    }

    fun blockApp(packageName: String) {
        viewModelScope.launch {
            container.settings.updateSettings { settings ->
                settings.copy(
                    appRules =
                        settings.appRules +
                            (packageName to AppRule(packageName, Level.BLOCKED, Level.BLOCKED)),
                )
            }
        }
    }

    /** Back to the All apps default: the rule is removed rather than set to something. */
    fun removeRule(packageName: String) {
        viewModelScope.launch {
            container.settings.updateSettings { it.copy(appRules = it.appRules - packageName) }
        }
    }

    fun setLevels(
        packageName: String,
        inHours: Level,
        outsideHours: Level,
    ) {
        updateRule(packageName) { it.copy(inHours = inHours, outsideHours = outsideHours) }
    }

    fun setHours(
        packageName: String,
        hours: List<WeeklyWindow>,
    ) {
        updateRule(packageName) { it.copy(hours = hours) }
    }

    private fun updateRule(
        packageName: String,
        transform: (AppRule) -> AppRule,
    ) {
        viewModelScope.launch {
            container.settings.updateSettings { settings ->
                val existing = settings.appRules[packageName] ?: settings.defaultRule.copy(packageName = packageName)
                settings.copy(appRules = settings.appRules + (packageName to transform(existing)))
            }
        }
    }

    fun labelFor(packageName: String): String = container.apps.labelFor(packageName)

    fun ruleFor(packageName: String): AppRule? =
        state.value.let { current ->
            (current.focusApps + current.blockedApps + current.otherApps)
                .firstOrNull { it.packageName == packageName }
                ?.rule
        }
}
