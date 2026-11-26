#!/bin/bash
# APK'yı kurar, açar; çökerse kaydı GitHub annotation olarak yazar.
adb install -g DarkOBD-Full.apk || { echo "::error::Kurulum basarisiz"; exit 1; }
adb logcat -c
adb shell am start -W -n com.obddroid/com.obddroid.ui.activities.MainActivity
sleep 30
crash=$(adb logcat -d -b crash)
if [ -n "$crash" ] || ! adb shell pidof com.obddroid >/dev/null; then
  msg=$(echo "$crash" | sed -E 's/^.*AndroidRuntime: //' | grep -E "Exception|Error|Caused by|com\.obddroid" | head -40)
  echo "::error title=CRASH::$(echo "${msg:-surec kapandi}" | sed 's/%/%25/g' | awk '{printf "%s%%0A",$0}')"
  exit 1
fi
echo "::notice::Uygulama 30 sn acik kaldi"
