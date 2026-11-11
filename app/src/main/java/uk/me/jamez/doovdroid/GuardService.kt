package uk.me.jamez.doovdroid

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Keeps the phone on the simple home screen (an accessibility service, enabled by
 * scripts/setup-phone.sh). Unless the admin code has been entered:
 *  - the notification pull-down is closed as soon as it opens (it leads to Settings);
 *  - Settings, the app stores, the old launcher and the other hidden apps go straight back home;
 *  - when the screen goes off (the red key is the power key on this phone), the home screen is
 *    brought back, so waking the phone always starts from home. Not during a call or while ringing.
 * Calls, Messages, WhatsApp, Contacts, the keyboard and the system's own dialogs are left alone.
 */
class GuardService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val dismissShade = Runnable { performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE) }

    /**
     * Last resort: SystemUI sometimes ignores the dismiss. If the shade still has focus (and this isn't
     * the lock screen, which is also a SystemUI window), Back closes it. Only the package name of the
     * active window is read.
     */
    private val backOutOfShade = Runnable {
        if (Admin.isUnlocked(this)) return@Runnable
        val locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        val active = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
        if (!locked && active == SYSTEM_UI) {
            Log.i(TAG, "pull-down still open; pressing Back")
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Admin.isUnlocked(context)) return
            // MODE_IN_CALL (phone), MODE_IN_COMMUNICATION (WhatsApp call), MODE_RINGTONE: leave alone.
            val mode = getSystemService(AudioManager::class.java)?.mode ?: AudioManager.MODE_NORMAL
            if (mode != AudioManager.MODE_NORMAL) return
            Log.i(TAG, "screen off; back to the home screen")
            startHome()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(screenOff) }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        Log.d(TAG, "window: $pkg ${event.className}")
        if (Admin.isUnlocked(this)) return
        when {
            pkg == SYSTEM_UI && isShade(event) -> {
                Log.i(TAG, "closing the notification pull-down")
                // A dismiss that lands while the shade is still animating open is ignored, so keep
                // trying for a couple of seconds. Dismissing a closed shade does nothing.
                handler.removeCallbacks(dismissShade)
                handler.removeCallbacks(backOutOfShade)
                dismissShade.run()
                for (delay in SHADE_RETRY_MS) handler.postDelayed(dismissShade, delay)
                handler.postDelayed(backOutOfShade, SHADE_BACK_MS)
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
        startHome()
    }

    private fun startHome() {
        runCatching {
            startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }.onFailure { Log.w(TAG, "Can't start home", it) }
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "DoovdroidGuard"
        private const val SYSTEM_UI = "com.android.systemui"
        private val SHADE_WORDS = listOf("notification shade", "quick settings")
        private val SHADE_RETRY_MS = listOf(300L, 700L, 1200L, 2000L)
        private const val SHADE_BACK_MS = 2600L

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
            "com.example.google", // vendor app labelled "Gboard"; not the real keyboard
            "com.meng.tools", // "Clean Memory"
        )
    }
}
