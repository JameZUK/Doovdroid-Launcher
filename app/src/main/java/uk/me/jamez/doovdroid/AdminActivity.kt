package uk.me.jamez.doovdroid

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast

/**
 * For the family, not the phone's user: opened by typing 0000<pin> on the home screen. Unlocks the
 * guard for [Admin.UNLOCK_MINUTES] minutes so Settings and the rest can be used. Keys or touch.
 */
class AdminActivity : Activity() {

    private class Entry(val label: String, val action: () -> Unit)

    private val entries = listOf(
        Entry("Speed dial") { startActivity(Intent(this, SpeedDialActivity::class.java)) },
        Entry("Settings") { open(Intent(Settings.ACTION_SETTINGS)) },
        Entry("Doov launcher") { open(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(OLD_LAUNCHER)) },
        Entry("Play Store") { packageManager.getLaunchIntentForPackage("com.android.vending")?.let(::open) ?: toast("Play Store is switched off") },
        Entry("Lock now") { Admin.lock(this); finish() },
    )
    private var selected = 0
    private lateinit var screen: LcdScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Admin.unlock(this)
        screen = LcdScreen(this, Palette.named(Config.load(this).style))
        screen.setTitle("Admin")
        screen.setSoft("Select", "Close")
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
        screen.setInfo("Doovdroid $version")
        screen.onRowClick = { i ->
            selected = i
            render()
            entries[i].action()
        }
        setContentView(screen.root)
        Ui.hideSystemBars(window)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        screen.setSubtitle(if (Admin.isUnlocked(this)) "Unlocked for ${Admin.minutesLeft(this)} more minutes" else "Locked")
        screen.setRows(entries.map { it.label }, selected)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { selected = (selected - 1 + entries.size) % entries.size; render(); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { selected = (selected + 1) % entries.size; render(); return true }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SOFT_LEFT -> {
                if (event.repeatCount == 0) entries[selected].action()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Log.w("Doovdroid", "Can't open $intent", e)
            toast("Couldn't open that")
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    companion object {
        const val OLD_LAUNCHER = "com.dp.op"
    }
}
