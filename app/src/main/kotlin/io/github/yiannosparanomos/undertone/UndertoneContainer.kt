package io.github.yiannosparanomos.undertone

import android.content.Context
import io.github.yiannosparanomos.undertone.core.data.db.UndertoneDatabase
import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.data.settings.UndertoneSettingsStore
import io.github.yiannosparanomos.undertone.core.system.apps.AppCatalog
import io.github.yiannosparanomos.undertone.core.system.calendar.CalendarReader
import io.github.yiannosparanomos.undertone.core.system.dnd.DndController

/**
 * What the screens need, in one place.
 *
 * A plain lazily-built container rather than a dependency-injection framework: UnderTone has one
 * process, a handful of stores and no variants to swap, so a DI library would be ceremony. The
 * settings store and the database are singletons of their own, so the services and the UI always
 * see the same state.
 */
class UndertoneContainer(
    private val context: Context,
) {
    val settings: UndertoneSettingsStore by lazy { UndertoneSettingsStore.get(context) }
    val history: HistoryStore by lazy { HistoryStore.from(UndertoneDatabase.get(context)) }
    val apps: AppCatalog by lazy { AppCatalog(context) }
    val calendar: CalendarReader by lazy { CalendarReader(context) }
    val dnd: DndController by lazy { DndController.from(context) }

    companion object {
        @Volatile
        private var instance: UndertoneContainer? = null

        fun get(context: Context): UndertoneContainer =
            instance ?: synchronized(this) {
                instance ?: UndertoneContainer(context.applicationContext).also { instance = it }
            }
    }
}
