package uk.me.jamez.doovdroid

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Keeps the phone on the simple home screen (an accessibility service, enabled by
 * scripts/setup-phone.sh). Unless the admin code has been entered:
 *  - the notification pull-down is closed as soon as it opens (it leads to Settings);
 *  - Settings, the app stores, the old launcher and the other hidden apps go straight back home.
 * Calls, Messages, WhatsApp, Contacts, the keyboard and the system's own dialogs are left alone.
 */
class GuardService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        Log.d(TAG, "window: $pkg ${event.className}")
        if (Admin.isUnlocked(this)) return
        when {
            pkg == SYSTEM_UI && isShade(event) -> {
                Log.i(TAG, "closing the notification pull-down")
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            }
            pkg in BLOCKED || pkg == AdminActivity.OLD_LAUNCHER -> {
                Log.i(TAG, "blocked $pkg; going home")
                goHome()
            }
        }
    }

    /** The pull-down shade / quick settings, as opposed to the lock screen, volume or power menu. */
    private fun isShade(event: AccessibilityEvent): Boolean {
        val text = event.text.joinToString(" ").lowercase()
        return SHADE_WORDS.any { it in text }
    }

    private fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        runCatching {
            startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }.onFailure { Log.w(TAG, "Can't start home", it) }
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "DoovdroidGuard"
        private const val SYSTEM_UI = "com.android.systemui"
        private val SHADE_WORDS = listOf("notification shade", "quick settings")

        /** Apps the phone's user has no reason to be in. Disabled ones are listed too, in case they come back. */
        val BLOCKED = setOf(
            "com.android.settings",
            "com.android.vending",
            "com.android.launcher3",
            "com.google.android.apps.nbu.files",
            "com.android.chrome",
            "com.google.android.youtube",
            "com.facebook.katana",
            "com.google.android.gm",
            "com.android.music",
            "com.android.gallery3d",
            "com.android.calendar",
            "com.android.soundrecorder",
            "com.google.android.calculator",
            "com.android.egg",
            "com.android.traceur",
            "com.android.settings.intelligence",
            "com.google.android.healthconnect.controller",
            "com.mediatek.factorymode",
            "com.android.agingtest",
            "com.mediatek.camera",
        )
    }
}
