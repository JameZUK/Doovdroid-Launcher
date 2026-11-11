package uk.me.jamez.doovdroid

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

/** Counts that come from notifications (WhatsApp), shared between [BadgeListener] and the home screen. */
object Badges {
    @Volatile var whatsApp: Int = 0
        private set

    private val listeners = CopyOnWriteArraySet<Runnable>()
    private val main = Handler(Looper.getMainLooper())

    fun setWhatsApp(count: Int) {
        if (count == whatsApp) return
        whatsApp = count
        main.post { listeners.forEach(Runnable::run) }
    }

    fun addListener(r: Runnable) { listeners += r }

    fun removeListener(r: Runnable) { listeners -= r }
}
