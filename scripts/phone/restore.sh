#!/system/bin/sh
# Runs ON the phone: undoes scripts/phone/setup.sh, back to how the phone was found on 2026-11-11.
# Leaves the launcher installed; `adb uninstall uk.me.jamez.doovdroid` removes it too.
PKG=uk.me.jamez.doovdroid
GUARD=$PKG/$PKG.GuardService
LISTENER=$PKG/$PKG.BadgeListener
GBOARD=com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME
DISABLE="com.facebook.katana com.google.android.youtube com.android.chrome com.google.android.gm"

step() { echo "== $*"; }

step "home screen back to the Doov launcher"
cmd role add-role-holder android.app.role.HOME com.dp.op
echo "home is now: $(cmd role get-role-holders android.app.role.HOME)"

step "apps back on"
for p in $DISABLE; do pm enable --user 0 $p; done

step "red key in calls: back to the default (screen off)"
settings delete secure incall_power_button_behavior

step "keyboard back to Gboard"
ime set $GBOARD

step "guard off (the other accessibility services stay)"
cur=$(settings get secure enabled_accessibility_services)
new=$(echo "$cur" | sed -e "s#:$GUARD##" -e "s#$GUARD:##" -e "s#^$GUARD\$##")
if [ -z "$new" ]; then settings delete secure enabled_accessibility_services; else settings put secure enabled_accessibility_services "$new"; fi

step "WhatsApp badge access off"
cmd notification disallow_listener $LISTENER
