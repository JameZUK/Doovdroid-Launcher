package uk.me.jamez.doovdroid

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The layout of the admin-side screens (admin menu, speed-dial editor), in the home screen's style:
 * a title and subtitle, a list with one highlighted row, an optional line of small print, and the
 * soft-key bar above the two dash keys. Rows are tappable as well as driven by keys.
 */
class LcdScreen(private val activity: Activity, private val palette: Palette) {

    private val u = Ui.unit(activity)
    val root = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(palette.paper)
    }
    private val title = Ui.text(activity, 7f * u, Ui.BOLD)
    private val subtitle = Ui.text(activity, 4.6f * u)
    private val rowsBox = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val info = Ui.text(activity, 3.8f * u)
    private val softLeft = Ui.text(activity, 5.8f * u, Ui.BOLD)
    private val softRight = Ui.text(activity, 5.8f * u, Ui.BOLD)
    private val rows = mutableListOf<TextView>()

    /** Called with the row index when a row is tapped. */
    var onRowClick: ((Int) -> Unit)? = null

    init {
        title.setTextColor(palette.ink)
        title.setPadding(px(3.4f), px(3f), px(3.4f), 0)
        subtitle.setTextColor(palette.muted)
        subtitle.setPadding(px(3.4f), px(1f), px(3.4f), px(2f))
        root.addView(title)
        root.addView(subtitle)
        root.addView(rule())
        root.addView(rowsBox, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        info.setTextColor(palette.muted)
        info.setPadding(px(3.4f), px(1f), px(3.4f), px(1.5f))
        info.visibility = View.GONE
        root.addView(info)
        root.addView(rule())
        val softBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(3.4f), 0, px(3.4f), 0)
        }
        softLeft.gravity = Gravity.START
        softRight.gravity = Gravity.END
        for (t in listOf(softLeft, softRight)) {
            t.setTextColor(palette.ink)
            softBar.addView(t, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(softBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(9f)))
    }

    fun setTitle(text: CharSequence) { title.text = text }

    fun setSubtitle(text: CharSequence) { subtitle.text = text }

    fun setInfo(text: CharSequence?) {
        info.text = text
        info.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    /** Labels above the left and right dash keys; "" for a key that does nothing. */
    fun setSoft(left: String, right: String) {
        softLeft.text = left
        softRight.text = right
    }

    fun setRows(labels: List<CharSequence>, selected: Int) {
        if (rows.size != labels.size) rebuild(labels.size)
        rows.forEachIndexed { i, row ->
            val sel = i == selected
            row.text = labels[i]
            row.setBackgroundColor(if (sel) palette.ink else Color.TRANSPARENT)
            row.setTextColor(if (sel) palette.paper else palette.ink)
        }
    }

    private fun rebuild(count: Int) {
        rowsBox.removeAllViews()
        rows.clear()
        for (i in 0 until count) {
            val row = Ui.text(activity, 6.4f * u).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(px(3.4f), 0, px(3.4f), 0)
                setOnClickListener { onRowClick?.invoke(i) }
                // Tappable views become focusable by default, and OK would then click whichever row
                // has Android's focus instead of the highlighted one. Keys are handled by the activity.
                isFocusable = false
                isFocusableInTouchMode = false
            }
            rowsBox.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            rows += row
        }
        // Rows keep the same height however many there are: pad out to MIN_SLOTS.
        repeat(MIN_SLOTS - count) {
            rowsBox.addView(View(activity), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    private fun rule() = View(activity).apply { setBackgroundColor(palette.ink) }.also {
        it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(0.5f).coerceAtLeast(2))
    }

    private fun px(units: Float): Int = (units * u).toInt()

    companion object {
        /** 8 speed-dial people plus "Add someone". */
        const val MIN_SLOTS = 9
    }
}
