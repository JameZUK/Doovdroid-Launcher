package uk.me.jamez.doovdroid

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView

/** Screen colours. Chosen with `style=` in the config file; see docs/admin.md. */
data class Palette(val paper: Int, val ink: Int, val muted: Int, val alert: Int) {
    companion object {
        /** Pale green LCD with near-black text: the default. */
        val LCD = Palette(0xFFC9D9A6.toInt(), 0xFF15230E.toInt(), 0xFF4F6139.toInt(), 0xFF8F1A10.toInt())
        val LIGHT = Palette(0xFFFFFFFF.toInt(), 0xFF101010.toInt(), 0xFF5C5C5C.toInt(), 0xFFB3261E.toInt())
        val DARK = Palette(0xFF0B0B0B.toInt(), 0xFFF3F3F3.toInt(), 0xFFA8A8A8.toInt(), 0xFFFF8F84.toInt())

        fun named(name: String): Palette = when (name.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> LCD
        }
    }
}

/**
 * Layout helpers. Everything is sized in "units" of 1% of the screen width and set in pixels, so the
 * one-screen layout always fits (the phone's font_scale of 1.3 would otherwise push rows off screen).
 */
object Ui {
    val MEDIUM: Typeface = Typeface.create(Typeface.SANS_SERIF, 500, false)
    val BOLD: Typeface = Typeface.create(Typeface.SANS_SERIF, 700, false)

    fun unit(ctx: Context): Float = ctx.resources.displayMetrics.widthPixels / 100f

    fun text(ctx: Context, sizePx: Float, face: Typeface = MEDIUM, singleLine: Boolean = true) =
        TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
            typeface = face
            includeFontPadding = false
            if (singleLine) {
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
            }
        }

    /** Hide the status bar: our own header shows signal and battery, in large type. */
    fun hideSystemBars(window: Window) {
        window.insetsController?.apply {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
