package uk.me.jamez.doovdroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast

/**
 * Admin menu › Speed dial: who is on the home screen, in what order. Every change is written to the
 * settings file straight away (the `speed=` lines); the home screen picks it up when it comes back.
 *
 *   list     ▲▼ choose, OK/left dash: Options (or Add on the last row), right dash: Back
 *   options  Move or Remove
 *   move     ▲▼ shift the person, OK/either dash: Done
 */
class SpeedDialActivity : Activity() {

    private enum class Mode { LIST, OPTIONS, MOVE }

    private lateinit var screen: LcdScreen
    private val entries = mutableListOf<SpeedEntry>()
    private var mode = Mode.LIST
    private var selected = 0
    private var optionSelected = 0
    private val options = listOf("Move", "Remove")

    private val canAdd get() = entries.size < Config.MAX_SPEED_DIAL
    private val rowCount get() = entries.size + if (canAdd) 1 else 0
    private fun isAddRow(i: Int) = canAdd && i == entries.size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cfg = Config.load(this)
        // With no speed= lines the home screen shows starred contacts; start from those.
        entries += cfg.speedDial.ifEmpty { PhoneData.starredNames(this).map { SpeedEntry(it, null) } }
        screen = LcdScreen(this, Palette.named(cfg.style))
        screen.onRowClick = { i ->
            if (mode == Mode.OPTIONS) optionSelected = i else if (mode == Mode.LIST) selected = i
            activate()
        }
        setContentView(screen.root)
        Ui.hideSystemBars(window)
        render()
    }

    // ---------------------------------------------------------------- keys

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { step(-1); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { step(+1); return true }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SOFT_LEFT -> {
                if (event.repeatCount == 0) activate()
                return true
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_SOFT_RIGHT -> {
                if (event.repeatCount == 0) back()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Back is handled on the way down; don't let the default handling finish the activity as well.
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun step(delta: Int) {
        when (mode) {
            Mode.LIST -> if (rowCount > 0) selected = (selected + delta + rowCount) % rowCount
            Mode.OPTIONS -> optionSelected = (optionSelected + delta + options.size) % options.size
            Mode.MOVE -> {
                val to = selected + delta
                if (to in entries.indices) {
                    entries.add(to, entries.removeAt(selected))
                    selected = to
                }
            }
        }
        render()
    }

    private fun activate() {
        when (mode) {
            Mode.LIST -> when {
                isAddRow(selected) -> pickContact()
                selected in entries.indices -> { mode = Mode.OPTIONS; optionSelected = 0 }
            }
            Mode.OPTIONS -> when (options[optionSelected]) {
                "Move" -> mode = Mode.MOVE
                "Remove" -> {
                    val removed = entries.removeAt(selected)
                    save()
                    toast("Removed ${removed.name}")
                    selected = selected.coerceAtMost(rowCount - 1).coerceAtLeast(0)
                    mode = Mode.LIST
                }
            }
            Mode.MOVE -> { save(); mode = Mode.LIST }
        }
        render()
    }

    private fun back() {
        when (mode) {
            Mode.LIST -> finish()
            Mode.OPTIONS -> mode = Mode.LIST
            Mode.MOVE -> { save(); mode = Mode.LIST }
        }
        render()
    }

    // ---------------------------------------------------------------- adding

    private fun pickContact() {
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(Intent.ACTION_PICK, Phone.CONTENT_URI), PICK)
        } catch (e: Exception) {
            Log.w(TAG, "No contact picker", e)
            toast("Couldn't open Contacts")
        }
    }

    @Deprecated("Framework Activity API; fine without AndroidX")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK && resultCode == RESULT_OK) data?.data?.let(::add)
    }

    private fun add(uri: Uri) {
        val picked = runCatching {
            contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) to c.getString(1) else null
            }
        }.getOrNull()
        val name = picked?.first?.trim()
        if (name.isNullOrEmpty()) {
            toast("Couldn't read that contact")
            return
        }
        if (entries.any { it.name == name }) {
            toast("$name is already on speed dial")
            return
        }
        // Store just the name (the number is looked up each time) unless a different number from
        // the contact's main one was picked; then keep that number.
        val number = picked.second
        val usual = PhoneData.numberFor(this, name)
        val keep = if (number == null || (usual != null && PhoneNumberUtils.compare(usual, number))) null else number
        entries += SpeedEntry(name, keep)
        save()
        selected = entries.lastIndex
        mode = Mode.LIST
        render()
        toast("Added $name")
    }

    // ---------------------------------------------------------------- drawing

    private fun render() {
        when (mode) {
            Mode.LIST -> {
                screen.setTitle("Speed dial")
                screen.setSubtitle("On the home screen, top to bottom")
                val labels = entries.mapIndexed { i, e -> "${i + 1}  ${e.name}" } +
                    if (canAdd) listOf("+ Add someone") else emptyList()
                screen.setRows(labels, selected)
                screen.setSoft(if (isAddRow(selected)) "Add" else if (entries.isEmpty()) "" else "Options", "Back")
                screen.setInfo(if (canAdd) null else "Full: ${Config.MAX_SPEED_DIAL} is the most that fit. Remove someone to add another.")
            }
            Mode.OPTIONS -> {
                screen.setTitle(entries[selected].name)
                screen.setSubtitle("Speed dial ${selected + 1} of ${entries.size}")
                screen.setRows(options, optionSelected)
                screen.setSoft("Select", "Back")
                screen.setInfo(null)
            }
            Mode.MOVE -> {
                screen.setTitle("Move ${entries[selected].name}")
                screen.setSubtitle("▲ ▼ to move it, OK when done")
                screen.setRows(entries.mapIndexed { i, e -> if (i == selected) "▲▼ ${e.name}" else "${i + 1}  ${e.name}" }, selected)
                screen.setSoft("Done", "Done")
                screen.setInfo(null)
            }
        }
    }

    private fun save() {
        if (!Config.saveSpeedDial(this, entries)) toast("Couldn't save the speed dial")
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    companion object {
        private const val TAG = "Doovdroid"
        private const val PICK = 1
    }
}
