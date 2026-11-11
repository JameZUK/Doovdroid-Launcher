#!/system/bin/sh
# Runs ON the phone (adb shell), started by scripts/setup-phone.sh. Safe to run again.
# Every change is listed in docs/installing.md ("What setup changes"); scripts/phone/restore.sh undoes them.
PKG=uk.me.jamez.doovdroid
GUARD=$PKG/$PKG.GuardService
LISTENER=$PKG/$PKG.BadgeListener
T9=com.marshaltec.ime.t9ime/.T9IME
# Play Store stays ENABLED (the guard hides it): WhatsApp needs it for updates. See docs/troubleshooting.md.
DISABLE="com.facebook.katana com.google.android.youtube com.android.chrome com.google.android.gm"

step() { echo "== $*"; }

step "permissions for the launcher"
for p in CALL_PHONE READ_CONTACTS READ_CALL_LOG READ_SMS; do pm grant $PKG android.permission.$p; done

step "WhatsApp badge: notification access"
cmd notification allow_listener $LISTENER

step "guard: accessibility service, added beside the ones already on"
cur=$(settings get secure enabled_accessibility_services)
case "$cur" in
  *"$GUARD"*) echo "already on" ;;
  null|"") settings put secure enabled_accessibility_services "$GUARD" ;;
  *) settings put secure enabled_accessibility_services "$cur:$GUARD" ;;
esac
settings put secure accessibility_enabled 1

step "keyboard: T9 on the keypad"
ime enable $T9 >/dev/null
ime set $T9

step "red key (= power key) hangs up calls"
settings put secure incall_power_button_behavior 2

step "switch off apps nobody uses"
for p in $DISABLE; do pm disable-user --user 0 $p; done

step "home screen"
cmd role add-role-holder android.app.role.HOME $PKG
echo "home is now: $(cmd role get-role-holders android.app.role.HOME)"
