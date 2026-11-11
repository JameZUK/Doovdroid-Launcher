package uk.me.jamez.doovdroid

import android.content.Context
import android.util.Log
import java.io.File

data class SpeedEntry(val name: String, val number: String?)

/**
 * The settings file on the phone:
 *   /sdcard/Android/data/uk.me.jamez.doovdroid/files/doovdroid.conf
 * Written with a template the first time the app runs; edit it with adb (scripts/conf.sh).
 * Re-read every time the home screen comes back, so a change shows after the red key.
 */
data class Config(val pin: String, val style: String, val speedDial: List<SpeedEntry>) {
    companion object {
        const val FILE_NAME = "doovdroid.conf"
        const val DEFAULT_PIN = "3210"
        const val MAX_SPEED_DIAL = 8
        val DEFAULT = Config(DEFAULT_PIN, "lcd", emptyList())

        private const val TAG = "Doovdroid"

        private val TEMPLATE = """
            |# Doovdroid settings. Lines starting with # are ignored.
            |# Changes show the next time the home screen appears (press the red key).
            |
            |# Admin code: type *#<pin># on the home screen. 4 to 12 digits.
            |pin=$DEFAULT_PIN
            |
            |# Screen colours: lcd (pale green), light (black on white) or dark (white on black).
            |style=lcd
            |
            |# Speed dial, top to bottom, up to $MAX_SPEED_DIAL. The name exactly as it is in Contacts;
            |# its number is looked up. To force a number: speed=Name|07700900123
            |# With no speed= lines, starred contacts are shown instead.
            |
        """.trimMargin()

        fun file(ctx: Context): File? = ctx.getExternalFilesDir(null)?.let { File(it, FILE_NAME) }

        fun load(ctx: Context): Config {
            val f = file(ctx) ?: return DEFAULT
            if (!f.exists()) {
                runCatching { f.writeText(TEMPLATE) }.onFailure { Log.w(TAG, "Can't write $f", it) }
            }
            val lines = runCatching { f.readLines() }.getOrElse { return DEFAULT }
            var pin = DEFAULT_PIN
            var style = "lcd"
            val speed = mutableListOf<SpeedEntry>()
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val eq = line.indexOf('=')
                if (eq <= 0) continue
                val key = line.substring(0, eq).trim().lowercase()
                val value = line.substring(eq + 1).trim()
                when (key) {
                    "pin" -> if (value.length in 4..12 && value.all { it.isDigit() }) pin = value
                    "style" -> style = value.lowercase()
                    "speed" -> if (value.isNotEmpty() && speed.size < MAX_SPEED_DIAL) {
                        val bar = value.indexOf('|')
                        speed += if (bar < 0) SpeedEntry(value, null)
                        else SpeedEntry(value.substring(0, bar).trim(), value.substring(bar + 1).trim().ifEmpty { null })
                    }
                }
            }
            return Config(pin, style, speed.filter { it.name.isNotEmpty() })
        }
    }
}
