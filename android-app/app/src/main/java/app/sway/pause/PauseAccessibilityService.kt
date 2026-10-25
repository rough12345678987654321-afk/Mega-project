package app.sway.pause

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * Watches only Android window-state changes for packages the user selected.
 * Content retrieval is disabled in pause_accessibility_service.xml; this service
 * never reads view trees, typed text, or screen contents.
 */
class PauseAccessibilityService : AccessibilityService() {
    private var lastInterceptedPackage: String? = null
    private var lastInterceptAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
        refreshTargetFilter()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val target = event?.packageName?.toString() ?: return
        if (target == packageName || target !in AppPrefs.targets(this)) return

        val now = SystemClock.elapsedRealtime()
        if (target == bypassPackage && now <= bypassUntil) {
            bypassPackage = null
            bypassUntil = 0L
            return
        }
        // Some launchers emit repeated window events for a single app launch.
        if (target == lastInterceptedPackage && now - lastInterceptAt < INTERCEPT_COOLDOWN_MS) return
        lastInterceptedPackage = target
        lastInterceptAt = now

        val label = runCatching {
            val info = packageManager.getApplicationInfo(target, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault("your app")

        runCatching {
            startActivity(Intent(this, BridgeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(BridgeActivity.EXTRA_TARGET_PACKAGE, target)
                putExtra(BridgeActivity.EXTRA_TARGET_LABEL, label)
            })
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        if (activeService === this) activeService = null
        return super.onUnbind(intent)
    }

    fun refreshTargetFilter() {
        if (activeService !== this) return
        val selected = AppPrefs.targets(this)
        // Limit delivered window events to chosen apps. When none are selected,
        // listen only to Sway itself rather than monitoring other packages.
        val info = serviceInfo
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = 0
        info.packageNames = selected.ifEmpty { setOf(packageName) }.toTypedArray()
        serviceInfo = info
    }

    companion object {
        private const val INTERCEPT_COOLDOWN_MS = 1_500L
        @Volatile private var activeService: PauseAccessibilityService? = null
        @Volatile private var bypassPackage: String? = null
        @Volatile private var bypassUntil: Long = 0L

        fun refreshTargetFilter() {
            activeService?.refreshTargetFilter()
        }

        fun allowOneLaunch(packageName: String) {
            bypassPackage = packageName
            bypassUntil = SystemClock.elapsedRealtime() + 3_000L
        }
    }
}
