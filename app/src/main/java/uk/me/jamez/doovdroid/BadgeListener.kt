package uk.me.jamez.doovdroid

import android.app.Notification
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlin.math.max

/**
 * WhatsApp's unread count, read from its notifications. WhatsApp puts the total unread count in
 * Notification.number on both the group summary and each chat; if that is ever missing, count the
 * messages in each chat's MessagingStyle instead. Enabled by scripts/setup-phone.sh.
 */
class BadgeListener : NotificationListenerService() {

    override fun onListenerConnected() = recount()

    override fun onListenerDisconnected() = Badges.setWhatsApp(0)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName == WHATSAPP) recount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.packageName == WHATSAPP) recount()
    }

    private fun recount() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        var fromNumber = 0
        var fromMessages = 0
        for (sbn in active) {
            if (sbn.packageName != WHATSAPP) continue
            val n = sbn.notification
            if (n.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0) continue
            fromNumber = max(fromNumber, n.number)
            if (n.flags and Notification.FLAG_GROUP_SUMMARY == 0) {
                val messages = n.extras.getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
                fromMessages += messages?.size?.takeIf { it > 0 } ?: 1
            }
        }
        Badges.setWhatsApp(if (fromNumber > 0) fromNumber else fromMessages)
    }

    companion object {
        const val WHATSAPP = "com.whatsapp"
    }
}
