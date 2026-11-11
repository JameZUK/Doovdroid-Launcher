package uk.me.jamez.doovdroid

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * For the family, not the phone's user: opened by typing *#<pin># on the home screen. Unlocks the
 * guard for [Admin.UNLOCK_MINUTES] minutes so Settings and the rest can be used. Keys or touch.
 */
class AdminActivity : Activity() {

    private class Entry(val label: String, val action: () -> Unit)

    private val entries = listOf(
        Entry("Settings") { open(Intent(Settings.ACTION_SETTINGS)) },
        Entry("Doov launcher") { open(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(OLD_LAUNCHER)) },
        Entry("Play Store") { packageManager.getLaunchIntentForPackage("com.android.vending")?.let(::open) ?: toast("Play Store is switched off") },
        Entry("Lock now") { Admin.lock(this); finish() },
    )
    private val rows = mutableListOf<TextView>()
    private var selected = 0
    private var palette = Palette.LCD
    private lateinit var subtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Admin.unlock(this)
        palette = Palette.named(Config.load(this).style)
        val u = Ui.unit(this)
        val px = { units: Float -> (units * u).toInt() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.paper)
            setPadding(0, px(3f), 0, 0)
        }
        val title = Ui.text(this, 7f * u, Ui.BOLD).apply { text = "Admin"; setTextColor(palette.ink); setPadding(px(3.4f), 0, px(3.4f), 0) }
        subtitle = Ui.text(this, 4.6f * u).apply { setTextColor(palette.muted); setPadding(px(3.4f), px(1f), px(3.4f), px(2f)) }
        root.addView(title)
        root.addView(subtitle)
        root.addView(android.view.View(this).apply { setBackgroundColor(palette.ink) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(0.5f).coerceAtLeast(2)))

        entries.forEachIndexed { i, entry ->
            val row = Ui.text(this, 6.4f * u).apply {
                text = entry.label
                gravity = Gravity.CENTER_VERTICAL
                setPadding(px(3.4f), 0, px(3.4f), 0)
                setOnClickListener { selected = i; render(); entry.action() }
            }
            root.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(12f)))
            rows += row
        }

        val info = Ui.text(this, 3.8f * u, singleLine = false).apply {
            setTextColor(palette.muted)
            setPadding(px(3.4f), px(4f), px(3.4f), 0)
            val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
            text = "Doovdroid $version\nSettings file:\n${Config.file(this@AdminActivity)?.path ?: "(storage unavailable)"}"
        }
        root.addView(info)

        // Soft-key labels above the two dash keys, as on the home screen.
        root.addView(android.view.View(this), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(android.view.View(this).apply { setBackgroundColor(palette.ink) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(0.5f).coerceAtLeast(2)))
        val softBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(3.4f), 0, px(3.4f), 0)
        }
        for ((label, g) in listOf("Select" to Gravity.START, "Close" to Gravity.END)) {
            softBar.addView(Ui.text(this, 5.8f * u, Ui.BOLD).apply { text = label; gravity = g; setTextColor(palette.ink) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(softBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(9f)))

        setContentView(root)
        Ui.hideSystemBars(window)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        subtitle.text = if (Admin.isUnlocked(this)) "Unlocked for ${Admin.minutesLeft(this)} more minutes" else "Locked"
        rows.forEachIndexed { i, row ->
            val sel = i == selected
            row.setBackgroundColor(if (sel) palette.ink else Color.TRANSPARENT)
            row.setTextColor(if (sel) palette.paper else palette.ink)
        }
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
