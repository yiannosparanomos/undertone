package io.github.yiannosparanomos.undertone.core.system.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

/**
 * The apps the user can set rules for: everything with a launcher icon.
 *
 * Found through a `<queries>` entry for the launcher intent, **not** `QUERY_ALL_PACKAGES`
 * (docs/play-store/permissions.md). That keeps the list to apps the user can actually see on
 * their phone, which is also the only list worth showing.
 */
class AppCatalog(
    private val context: Context,
) {
    data class LaunchableApp(
        val packageName: String,
        val label: String,
        val isSystemApp: Boolean,
    )

    /** Launchable apps, UnderTone itself excluded, sorted by the name the user sees. */
    fun launchableApps(): List<LaunchableApp> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .asSequence()
                .mapNotNull { resolved ->
                    val activity = resolved.activityInfo ?: return@mapNotNull null
                    val packageName = activity.applicationInfo.packageName
                    if (packageName == context.packageName) return@mapNotNull null
                    LaunchableApp(
                        packageName = packageName,
                        label = resolved.loadLabel(packageManager).toString(),
                        isSystemApp = activity.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    )
                }.distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
                .toList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not list launchable apps: ${e.javaClass.simpleName}")
            emptyList()
        }
    }

    /** The name to show for one package, falling back to the package name itself. */
    fun labelFor(packageName: String): String =
        try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            // An app the user has since uninstalled can still have a rule and log entries.
            packageName
        }

    /** An intent that opens [packageName], or null if it has no launcher activity any more. */
    fun launchIntentFor(packageName: String): Intent? = context.packageManager.getLaunchIntentForPackage(packageName)

    private companion object {
        const val TAG = "UndertoneApps"
    }
}
