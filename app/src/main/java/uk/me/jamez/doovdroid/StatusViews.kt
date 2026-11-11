package uk.me.jamez.doovdroid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** Four signal bars, filled up to [level] (0–4). */
class SignalView(ctx: Context) : View(ctx) {
    var level = 0
        set(v) { field = v.coerceIn(0, 4); invalidate() }
    var color = 0xFF000000.toInt()
        set(v) { field = v; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val bars = 4
        val gap = width * 0.1f
        val barWidth = (width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val h = height * (i + 1f) / bars
            paint.color = color
            paint.alpha = if (i < level) 255 else 60
            val x = i * (barWidth + gap)
            canvas.drawRect(x, height - h, x + barWidth, height.toFloat(), paint)
        }
    }
}

/** A battery outline filled to [percent], with a bolt while charging. */
class BatteryView(ctx: Context) : View(ctx) {
    private var percent = -1
    private var charging = false
    var ink = 0xFF000000.toInt()
        set(v) { field = v; invalidate() }
    var paper = 0xFFFFFFFF.toInt()
        set(v) { field = v; invalidate() }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val body = RectF()
    private val bolt = Path()

    fun set(percent: Int, charging: Boolean) {
        this.percent = percent
        this.charging = charging
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val sw = height * 0.13f
        stroke.strokeWidth = sw
        stroke.color = ink
        fill.color = ink
        val nub = width * 0.08f
        body.set(sw / 2, sw / 2, width - nub - sw, height - sw / 2)
        canvas.drawRoundRect(body, sw, sw, stroke)
        canvas.drawRect(width - nub - sw / 2, height * 0.3f, width.toFloat(), height * 0.7f, fill)
        if (percent > 0) {
            val inset = sw * 1.6f
            val fullWidth = body.width() - inset * 2
            canvas.drawRect(body.left + inset, body.top + inset, body.left + inset + fullWidth * percent / 100f, body.bottom - inset, fill)
        }
        if (charging) {
            val l = body.left; val t = body.top; val w = body.width(); val h = body.height()
            bolt.reset()
            bolt.moveTo(l + w * 0.56f, t + h * 0.05f)
            bolt.lineTo(l + w * 0.32f, t + h * 0.58f)
            bolt.lineTo(l + w * 0.49f, t + h * 0.58f)
            bolt.lineTo(l + w * 0.42f, t + h * 0.95f)
            bolt.lineTo(l + w * 0.70f, t + h * 0.40f)
            bolt.lineTo(l + w * 0.53f, t + h * 0.40f)
            bolt.close()
            fill.color = paper
            canvas.drawPath(bolt, fill)
        }
    }
}
