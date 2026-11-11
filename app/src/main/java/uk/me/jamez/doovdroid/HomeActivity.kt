package uk.me.jamez.doovdroid

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * The home screen: a clock, then one list (speed-dial people, then Messages, WhatsApp, Recent calls,
 * Contacts) that always fits on the screen. Keypad only: ▲▼ move, OK/green act, digits dial.
 */
class HomeActivity : Activity() {

    private sealed interface Item { val label: String }
    private data class Person(override val label: String, val number: String?) : Item
    private data class Shortcut(override val label: String, val kind: Kind) : Item
    private enum class Kind { MESSAGES, WHATSAPP, CALLS, CONTACTS }

    private class Row(val view: LinearLayout, val label: TextView, val badge: TextView)

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val telephony by lazy { getSystemService(TelephonyManager::class.java) }

    private val shortcuts = listOf(
        Shortcut("Messages", Kind.MESSAGES),
        Shortcut("WhatsApp", Kind.WHATSAPP),
        Shortcut("Recent calls", Kind.CALLS),
        Shortcut("Contacts", Kind.CONTACTS),
    )

    private var config = Config.DEFAULT
    private var palette = Palette.LCD
    private var people: List<Person> = emptyList()
    private var items: List<Item> = shortcuts
    private var selected = 0
    private val dial = StringBuilder()
    private var missedCalls = 0
    private var unreadTexts = 0

    private var u = 0f
    private lateinit var root: LinearLayout
    private lateinit var signal: SignalView
    private lateinit var operator: TextView
    private lateinit var battery: BatteryView
    private lateinit var batteryText: TextView
    private lateinit var clock: TextClock
    private lateinit var amPm: TextClock
    private lateinit var date: TextClock
    private lateinit var rule: View
    private lateinit var list: LinearLayout
    private lateinit var dialPanel: LinearLayout
    private lateinit var dialLabel: TextView
    private lateinit var dialNumber: TextView
    private lateinit var dialHint: TextView
    private lateinit var softRule: View
    private lateinit var softLeft: TextView
    private lateinit var softRight: TextView
    private val rows = mutableListOf<Row>()
    private var groupRule: View? = null
    private var rowTextPx = 0f

    /**
     * The left dash key also unlocks the lock screen, so a double press would unlock and then call
     * whoever is highlighted. Ignore the action keys for a moment after the screen comes back.
     */
    private var quietUntil = 0L

    // ---------------------------------------------------------------- lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildViews()
        setContentView(root)
        Ui.hideSystemBars(window)
        rebuildRows()
        render()
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_NOT_EXPORTED)
        runCatching { telephony.registerTelephonyCallback(mainExecutor, signalCallback) }
            .onFailure { Log.w(TAG, "No signal updates", it) }
        for (uri in listOf(CallLog.Calls.CONTENT_URI, Uri.parse("content://sms"), Uri.parse("content://mms"))) {
            runCatching { contentResolver.registerContentObserver(uri, true, countsObserver) }
                .onFailure { Log.w(TAG, "Can't watch $uri", it) }
        }
        Badges.addListener(renderNow)
        updateOperator()
        reload()
    }

    override fun onResume() {
        super.onResume()
        quietUntil = SystemClock.uptimeMillis() + QUIET_MS
        Ui.hideSystemBars(window)
        refreshCounts()
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(batteryReceiver) }
        runCatching { telephony.unregisterTelephonyCallback(signalCallback) }
        contentResolver.unregisterContentObserver(countsObserver)
        Badges.removeListener(renderNow)
        // Coming back always starts from the top, with nothing half-typed.
        dial.clear()
        selected = 0
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dial.clear()
        selected = 0
        render()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            quietUntil = maxOf(quietUntil, SystemClock.uptimeMillis() + QUIET_MS)
            Ui.hideSystemBars(window)
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    /** Touch is switched off on the home screen. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean = true

    // ---------------------------------------------------------------- data

    private fun reload() {
        io.execute {
            val cfg = Config.load(this)
            val entries = cfg.speedDial.ifEmpty { PhoneData.starredNames(this).map { SpeedEntry(it, null) } }
            val found = entries.map { Person(it.name, it.number ?: PhoneData.numberFor(this, it.name)) }
            Log.i(TAG, "speed dial: ${found.size} people, ${found.count { it.number == null }} without a number")
            val missed = PhoneData.missedCalls(this)
            val texts = PhoneData.unreadTexts(this)
            main.post {
                if (isDestroyed) return@post
                config = cfg
                palette = Palette.named(cfg.style)
                missedCalls = missed
                unreadTexts = texts
                if (found != people) {
                    people = found
                    items = people + shortcuts
                    if (selected >= items.size) selected = 0
                    rebuildRows()
                }
                render()
            }
        }
    }

    private fun refreshCounts() {
        io.execute {
            val missed = PhoneData.missedCalls(this)
            val texts = PhoneData.unreadTexts(this)
            main.post {
                if (isDestroyed) return@post
                missedCalls = missed
                unreadTexts = texts
                render()
            }
        }
    }

    private val countsObserver = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) {
            main.removeCallbacks(refreshSoon)
            main.postDelayed(refreshSoon, 300)
        }
    }
    private val refreshSoon = Runnable { refreshCounts() }
    private val renderNow = Runnable { render() }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
            val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            battery.set(percent, charging)
            batteryText.text = if (percent >= 0) "$percent%" else ""
        }
    }

    private val signalCallback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
        override fun onSignalStrengthsChanged(strength: SignalStrength) {
            signal.level = strength.level
            updateOperator()
        }
    }

    private fun updateOperator() {
        operator.text = runCatching {
            if (telephony.simState != TelephonyManager.SIM_STATE_READY) "No SIM"
            else telephony.networkOperatorName.ifBlank { "No signal" }
        }.getOrDefault("")
    }

    // ---------------------------------------------------------------- keys

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        digitFor(keyCode)?.let { ch ->
            if (event.repeatCount == 0) {
                if (keyCode == KeyEvent.KEYCODE_0) event.startTracking()
                type(ch)
            }
            return true
        }
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { if (dial.isEmpty()) move(-1); return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { if (dial.isEmpty()) move(+1); return true }
            in OK_KEYS, in LEFT_SOFT_KEYS -> {
                if (event.repeatCount == 0 && SystemClock.uptimeMillis() >= quietUntil) {
                    if (dial.isNotEmpty()) callTyped() else activate(selected)
                }
                return true
            }
            in RIGHT_SOFT_KEYS -> {
                if (event.repeatCount == 0) {
                    event.startTracking()
                    if (dial.isNotEmpty()) { dial.setLength(dial.length - 1); render() }
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            in RIGHT_SOFT_KEYS -> { dial.clear(); render(); return true }
            KeyEvent.KEYCODE_0 -> {
                if (dial.endsWith("0")) { dial.setCharAt(dial.length - 1, '+'); render() }
                return true
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // Swallow the ups too, so the system's own fallbacks (e.g. green opening the dialler) never run.
        if (digitFor(keyCode) != null || keyCode in OK_KEYS || keyCode in LEFT_SOFT_KEYS || keyCode in RIGHT_SOFT_KEYS ||
            keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        ) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun digitFor(keyCode: Int): Char? = when (keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> '0' + (keyCode - KeyEvent.KEYCODE_0)
        KeyEvent.KEYCODE_STAR -> '*'
        KeyEvent.KEYCODE_POUND -> '#'
        else -> null
    }

    private fun move(delta: Int) {
        if (items.isEmpty()) return
        selected = (selected + delta + items.size) % items.size
        render()
    }

    private fun type(ch: Char) {
        if (dial.length >= MAX_DIAL) return
        dial.append(ch)
        if (ch == '#' && dial.toString() == "*#${config.pin}#") {
            dial.clear()
            render()
            Admin.unlock(this)
            startActivity(Intent(this, AdminActivity::class.java))
            return
        }
        render()
    }

    // ---------------------------------------------------------------- actions

    private fun activate(index: Int) {
        when (val item = items.getOrNull(index) ?: return) {
            is Person -> item.number?.let(::call) ?: toast("No phone number for ${item.label}")
            is Shortcut -> when (item.kind) {
                Kind.MESSAGES -> launchPackage(runCatching { Telephony.Sms.getDefaultSmsPackage(this) }.getOrNull() ?: "com.android.mms", "Messages")
                Kind.WHATSAPP -> launchPackage("com.whatsapp", "WhatsApp")
                Kind.CALLS -> open(Intent(Intent.ACTION_VIEW).setType(CallLog.Calls.CONTENT_TYPE), Intent(Intent.ACTION_DIAL))
                Kind.CONTACTS -> open(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI), packageManager.getLaunchIntentForPackage("com.android.contacts"))
            }
        }
    }

    private fun callTyped() {
        val number = dial.toString()
        if (number.none { it.isDigit() }) return
        dial.clear()
        render()
        call(number)
    }

    /**
     * Calls straight away. Emergency numbers can't be called this way by an ordinary app, so those
     * open the dialler with the number filled in, and green there calls it.
     */
    private fun call(number: String) {
        val uri = Uri.fromParts("tel", number, null)
        val emergency = runCatching { telephony.isEmergencyNumber(number) }.getOrDefault(number in FALLBACK_EMERGENCY)
        val action = if (emergency) Intent.ACTION_DIAL else Intent.ACTION_CALL
        if (!open(Intent(action, uri), Intent(Intent.ACTION_DIAL, uri))) toast("Couldn't call $number")
    }

    private fun launchPackage(pkg: String, name: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null || !open(intent, null)) toast("$name isn't available")
    }

    private fun open(intent: Intent, fallback: Intent?): Boolean {
        for (candidate in listOfNotNull(intent, fallback)) {
            try {
                startActivity(candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Can't open $candidate", e)
            }
        }
        return false
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    // ---------------------------------------------------------------- views

    private fun buildViews() {
        u = Ui.unit(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(3f), 0, px(3f), 0)
        }
        signal = SignalView(this)
        status.addView(signal, LinearLayout.LayoutParams(px(5.2f), px(4.4f)).apply { marginEnd = px(2f) })
        operator = Ui.text(this, 4.4f * u)
        status.addView(operator, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        battery = BatteryView(this)
        status.addView(battery, LinearLayout.LayoutParams(px(7f), px(3.8f)).apply { marginEnd = px(1.5f) })
        batteryText = Ui.text(this, 4.4f * u)
        status.addView(batteryText)
        root.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(8f)))

        val clockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isBaselineAligned = true
        }
        clock = TextClock(this).apply {
            format12Hour = "h:mm"
            format24Hour = "HH:mm"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * u)
            typeface = Ui.MEDIUM
            includeFontPadding = false
        }
        amPm = TextClock(this).apply {
            format12Hour = "a"
            format24Hour = ""
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 5.9f * u)
            typeface = Ui.MEDIUM
            includeFontPadding = false
            setPadding(px(1.2f), 0, 0, 0)
        }
        clockRow.addView(clock)
        clockRow.addView(amPm)
        root.addView(clockRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(1f)
        })

        date = TextClock(this).apply {
            format12Hour = "EEEE d MMMM"
            format24Hour = "EEEE d MMMM"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 5.4f * u)
            typeface = Ui.MEDIUM
            gravity = Gravity.CENTER
            includeFontPadding = false
            isSingleLine = true
        }
        root.addView(date, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(1.6f)
        })

        rule = View(this)
        root.addView(rule, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(0.5f).coerceAtLeast(2)).apply {
            topMargin = px(2f)
            marginStart = px(3f)
            marginEnd = px(3f)
        })

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, px(0.8f), 0, px(0.8f))
            addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
                if (bottom - top != oldBottom - oldTop) sizeRows()
            }
        }
        root.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        dialPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(4f), 0, px(4f), 0)
            visibility = View.GONE
        }
        dialLabel = Ui.text(this, 5f * u).apply { text = "Number" }
        dialNumber = Ui.text(this, 13f * u, singleLine = false).apply {
            maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(px(5f), px(13f), 1, TypedValue.COMPLEX_UNIT_PX)
        }
        dialHint = Ui.text(this, 5f * u, singleLine = false).apply { setLineSpacing(0f, 1.3f) }
        dialPanel.addView(dialLabel)
        dialPanel.addView(dialNumber, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(17f)).apply { topMargin = px(2f) })
        dialPanel.addView(dialHint, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(2f) })
        root.addView(dialPanel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Soft-key labels along the bottom edge, above the two dash keys either side of the D-pad.
        softRule = View(this)
        root.addView(softRule, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(0.5f).coerceAtLeast(2)))
        val softBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(3.4f), 0, px(3.4f), 0)
        }
        softLeft = Ui.text(this, 5.8f * u, Ui.BOLD).apply { gravity = Gravity.START }
        softRight = Ui.text(this, 5.8f * u, Ui.BOLD).apply { gravity = Gravity.END }
        softBar.addView(softLeft, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        softBar.addView(softRight, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(softBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(9f)))
    }

    private fun rebuildRows() {
        list.removeAllViews()
        rows.clear()
        groupRule = null
        items.forEachIndexed { i, _ ->
            if (i == people.size && people.isNotEmpty()) {
                groupRule = View(this).also {
                    list.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(0.3f).coerceAtLeast(1)).apply {
                        marginStart = px(3f)
                        marginEnd = px(3f)
                    })
                }
            }
            val view = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(px(3.4f), 0, px(3.4f), 0)
            }
            val label = Ui.text(this, 6f * u)
            val badge = Ui.text(this, 5f * u)
            view.addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            view.addView(badge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = px(2f)
            })
            list.addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            rows += Row(view, label, badge)
        }
        rowTextPx = 0f
        list.post { sizeRows() }
    }

    /** Text as large as the rows allow: fewer people, bigger names. */
    private fun sizeRows() {
        val h = rows.firstOrNull()?.view?.height ?: return
        if (h <= 0) return
        val textPx = min(8f * u, h * 0.62f)
        if (textPx == rowTextPx) return
        rowTextPx = textPx
        for (r in rows) {
            r.label.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx)
            r.badge.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx * 0.82f)
        }
    }

    private fun render() {
        val p = palette
        root.setBackgroundColor(p.paper)
        window.statusBarColor = p.paper
        for (t in listOf(operator, batteryText, clock, amPm, date, dialNumber)) t.setTextColor(p.ink)
        dialLabel.setTextColor(p.muted)
        signal.color = p.ink
        battery.ink = p.ink
        battery.paper = p.paper
        rule.setBackgroundColor(p.ink)
        groupRule?.setBackgroundColor((p.ink and 0x00FFFFFF) or 0x59000000)

        val dialling = dial.isNotEmpty()
        softRule.setBackgroundColor(p.ink)
        softLeft.setTextColor(p.ink)
        softRight.setTextColor(p.ink)
        softLeft.text = when {
            dialling -> "Call"
            items.getOrNull(selected) is Person -> "Call"
            else -> "Open"
        }
        softRight.text = if (dialling) "Delete" else ""

        list.visibility = if (dialling) View.GONE else View.VISIBLE
        dialPanel.visibility = if (dialling) View.VISIBLE else View.GONE
        if (dialling) {
            dialNumber.text = dial.toString()
            dialHint.text = hint(p)
            return
        }

        rows.forEachIndexed { i, r ->
            val item = items.getOrNull(i) ?: return@forEachIndexed
            val sel = i == selected
            r.view.setBackgroundColor(if (sel) p.ink else Color.TRANSPARENT)
            r.label.text = item.label
            r.label.setTextColor(if (sel) p.paper else p.ink)
            val (text, alert) = badgeFor(item)
            r.badge.text = text
            r.badge.typeface = if (alert) Ui.BOLD else Ui.MEDIUM
            r.badge.setTextColor(if (sel) p.paper else if (alert) p.alert else p.muted)
        }
    }

    private fun badgeFor(item: Item): Pair<String, Boolean> = when (item) {
        is Person -> if (item.number == null) "no number" to false else "" to false
        is Shortcut -> when (item.kind) {
            Kind.MESSAGES -> if (unreadTexts > 0) "$unreadTexts" to true else "" to false
            Kind.WHATSAPP -> Badges.whatsApp.let { if (it > 0) "$it" to true else "" to false }
            Kind.CALLS -> if (missedCalls > 0) "$missedCalls missed" to true else "" to false
            Kind.CONTACTS -> "" to false
        }
    }

    private fun hint(p: Palette): CharSequence = SpannableStringBuilder().apply {
        fun strong(s: String) {
            val start = length
            append(s)
            setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(p.ink), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        fun plain(s: String) {
            val start = length
            append(s)
            setSpan(ForegroundColorSpan(p.muted), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        strong("Green"); plain(" to call\n")
        plain("Hold "); strong("Delete"); plain(" to clear")
    }

    private fun px(units: Float): Int = (units * u).toInt()

    companion object {
        private const val TAG = "Doovdroid"
        private const val MAX_DIAL = 24
        private const val QUIET_MS = 800L
        private val OK_KEYS = setOf(
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_CALL,
        )

        /** The dash keys either side of the D-pad (see docs/device.md for what the phone sends). */
        private val LEFT_SOFT_KEYS = setOf(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SOFT_LEFT)
        private val RIGHT_SOFT_KEYS = setOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_SOFT_RIGHT)
        private val FALLBACK_EMERGENCY = setOf("999", "112", "911")
    }
}
