#!/bin/bash
# Gerçek kurulum gibi: izinler VERİLMEDEN ve Bluetooth kapalıyken açar.
PKG=com.obddroid
fail() {
  msg=$(adb logcat -d -b crash | sed -E 's/^.*AndroidRuntime: //' | grep -E "Exception|Error|Caused by|com\.obddroid" | head -30)
  if [ -z "$msg" ]; then
    msg=$(adb logcat -d | grep -E "$PKG|AndroidRuntime|FATAL|Fatal signal|lowmemorykiller" | grep -E "Killing|died|crash|FATAL|Fatal|Exception|ANR|kill|Force" | tail -25 | cut -c1-260)
  fi
  msg="$msg
--start: $(echo "$START" | tr '\n' ' ')
--exit-info: $(adb shell dumpsys activity exit-info $PKG | tr -d '\r' | grep -E "reason=|description=|timestamp=" | head -8)
--log: $(adb logcat -d | grep "$PKG" | tail -15 | cut -c1-230)"
  echo "::error title=CRASH ($1)::$(echo "${msg:-kayit yok, surec yok}" | sed 's/%/%25/g' | awk '{printf "%s%%0A",$0}')"
  exit 1
}
launch() {
  adb shell am force-stop $PKG
  adb logcat -b all -c
  START=$(adb shell am start -W -n $PKG/com.obddroid.ui.activities.MainActivity 2>&1 | tr -d '\r')
  echo "$START"
  sleep 20
  if adb logcat -d -b crash | grep -q "$PKG" || ! adb shell pidof $PKG >/dev/null; then fail "$1"; fi
  echo "::notice::Gecti: $1"
}
adb install DarkOBD-Full.apk || { echo "::error::Kurulum basarisiz"; exit 1; }
adb shell svc bluetooth disable; sleep 8
echo "BT durumu: $(adb shell settings get global bluetooth_on)"
launch "izinsiz, BT kapali"
adb shell input keyevent KEYCODE_BACK; sleep 5
if adb logcat -d -b crash | grep -q "$PKG"; then fail "izin reddi"; fi
if [ "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" -ge 31 ]; then
  adb shell pm grant $PKG android.permission.BLUETOOTH_CONNECT
  adb shell pm grant $PKG android.permission.BLUETOOTH_SCAN
fi
launch "izinli, BT kapali"
adb shell svc bluetooth enable; sleep 8
launch "izinli, BT acik"
