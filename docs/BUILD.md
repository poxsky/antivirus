# 🛠️ ShieldScan — APK Kaise Banaye

## Option 1: GitHub Actions se (Sabse Easy, 0 Setup)

> Yehi "apk bana strat" ka fastest tareeka hai. Tumhare phone pe Android Studio ki bhi zaroorat nahi.

1. Code push karo `main` ya kisi branch pe:
   ```bash
   git add .
   git commit -m "feat: shieldscan mvp"
   git push origin arena/e75c86fe-antivirus
   ```

2. GitHub pe jao: `https://github.com/poxsky/antivirus/actions`

3. `Build ShieldScan APK` workflow automatically chalega (~4-5 min)

4. Job complete hone ke baad neeche **Artifacts** me milega:
   - `ShieldScan-debug-apk` → `app-debug.apk` (direct install)
   - `ShieldScan-release-apk` → unsigned release

5. APK ko download karo → phone me transfer → `Install` (Unknown sources allow)

---

## Option 2: Android Studio Local Build

### Requirements
- Android Studio Koala+ (JDK 17 bundled)
- SDK 34 installed

### Steps
```bash
git clone https://github.com/poxsky/antivirus.git
cd antivirus
# local.properties auto-generated, bas Android Studio open karo
```

1. Android Studio → Open Project → `antivirus` folder select karo
2. Gradle sync hone do (pehli baar 2-3 min net lagta hai)
3. Top bar me `Run ▶` → apna phone/emulator select → APK auto install

**Debug APK manually build karna ho:**
```bash
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk

./gradlew assembleRelease  # release (minified)
```

### APK phone me kaise daale
- `adb install app/build/outputs/apk/debug/app-debug.apk`
- Ya file ko Telegram/Drive pe bhej ke phone se install

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `QUERY_ALL_PACKAGES` warning | Personal use pe ignore. Play Store pe declaration form bharna padega |
| Gradle sync failed (offline) | Android Studio → `File → Invalidate Caches / Restart` + net on rakho |
| `PACKAGE_USAGE_STATS` not granted | App kholo → Setup tab → Usage Access → Allow |
| Release APK sign error | `app/build.gradle.kts` me signingConfig add karo ya debug use karo |
| Workflow fail: sdkmanager not found | Actions me version bump karo `setup-android@v3` → latest |

---

## Fast Test APK (Detection verify ke liye)

Ek harmless test APK banao jo `READ_SMS + SYSTEM_ALERT_WINDOW + sideload` maange — isko ShieldScan ko `CRITICAL` flag karna chahiye. Real malware personal phone pe kabhi mat chalana.

---

## Next Steps after Build
1. Phone pe install → Dashboard → `Scan Now`
2. Setup wizard me 3 permissions grant karo
3. App List me risk scores check karo
4. Koi sideloaded app daal ke test karo → instant notification ayega

Questions? GitHub Issues me pooche ya `security@shieldscan.dev`
