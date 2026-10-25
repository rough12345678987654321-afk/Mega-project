package app.sway.pause

import android.content.Context

object AppPrefs {
    private const val FILE = "sway-local"
    private const val KEY_TARGETS = "selected-target-packages"
    private const val KEY_DISCLOSURE_ACCEPTED = "accessibility-disclosure-accepted"

    fun targets(context: Context): Set<String> =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getStringSet(KEY_TARGETS, emptySet())
            ?.toSet()
            ?: emptySet()

    fun setTarget(context: Context, packageName: String, enabled: Boolean) {
        val copy = targets(context).toMutableSet()
        if (enabled) copy.add(packageName) else copy.remove(packageName)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_TARGETS, copy)
            .apply()
        PauseAccessibilityService.refreshTargetFilter()
    }

    fun disclosureAccepted(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISCLOSURE_ACCEPTED, false)

    fun acceptDisclosure(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DISCLOSURE_ACCEPTED, true)
            .apply()
    }
}
