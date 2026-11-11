package uk.me.jamez.doovdroid

import android.content.Context

/**
 * Admin unlock: set by typing the admin code, lasts [UNLOCK_MINUTES]. While unlocked, the guard lets
 * Settings and the other hidden apps open. Kept in prefs so a restart of the app doesn't lose it.
 */
object Admin {
    const val UNLOCK_MINUTES = 15
    private const val UNLOCK_MS = UNLOCK_MINUTES * 60_000L
    private const val PREFS = "admin"
    private const val KEY_UNTIL = "unlocked_until"

    @Volatile private var until: Long? = null

    private fun untilMs(ctx: Context): Long =
        until ?: ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_UNTIL, 0L).also { until = it }

    fun isUnlocked(ctx: Context): Boolean {
        val left = untilMs(ctx) - System.currentTimeMillis()
        // A clock that jumped backwards must not stretch the unlock.
        return left in 1..UNLOCK_MS
    }

    fun minutesLeft(ctx: Context): Int =
        if (!isUnlocked(ctx)) 0 else (((untilMs(ctx) - System.currentTimeMillis()) + 59_999) / 60_000).toInt()

    fun unlock(ctx: Context) = set(ctx, System.currentTimeMillis() + UNLOCK_MS)

    fun lock(ctx: Context) = set(ctx, 0L)

    private fun set(ctx: Context, value: Long) {
        until = value
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_UNTIL, value).apply()
    }
}
