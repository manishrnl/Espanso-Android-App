# RNL ESPANSO for Android

Offline text expansion for Android with folder organization, CSV portability,
global light/dark themes, and an optional keyboard for secure fields.

Current release: **1.7.0** (`versionCode 9`)

## What it does

- Expands local shortcuts while Gboard, SwiftKey, Samsung Keyboard, or another
  keyboard remains selected in supported non-password fields.
- Provides an optional bundled keyboard for password-field expansion.
- Stores all shortcuts and settings in app-private device storage.
- Includes no account, backend, ads, analytics, tracking SDK, cloud backup, or
  internet permission.

## Main features

- Android 13+ direct accessibility editor integration
- Android 8-12 accessibility compatibility mode
- Optional secure-field expansion through the bundled keyboard
- Popup suggestions and first-backspace undo
- Folder-based shortcut organization and global search
- Premium System, Light, and Dark themes
- Premium folder creation and shortcut editor interfaces
- Dynamic tokens such as `%dd%`, `%MM%`, `%yyyy%`, `%HH%`, and `%mm%`
- CSV import/export through Android's system document picker
- Quoted commas, quotes, and multiline replacement support
- 90-day recycle bin for deleted folders and shortcuts
- In-app privacy policy, developer profile, setup guide, and troubleshooting
- Guided App info to Accessibility flow for Android restricted settings
- Automatic exclusion of payment, banking, and financial apps (Paytm, PhonePe, GPay, SBI, etc.) for device security compliance

## Required setup

1. Install and open `rnl espanso`.
2. Create a folder and shortcut, or import a compatible CSV from Settings.
3. Open **Settings > Enable global expansion**.
4. Accept the in-app accessibility disclosure.
5. Enable **rnl espanso expansion** in Android Accessibility settings.
6. Type a shortcut keyword in a supported editable field.

### Android "Restricted setting"

Android 13 and newer may block Accessibility for manually installed APK files.
An app cannot silently bypass or approve this security control.

Use **Settings > Fix restricted setting > Start guided setup**:

1. The app opens its Android App info page.
2. Open the three-dot menu.
3. Select **Allow restricted settings** and confirm.
4. Return to the app.
5. The app automatically continues to Accessibility settings.

The same workflow is available under **Settings > Setup & troubleshooting**.

## Password fields

Android hides password text from accessibility services. Expansion in password
fields therefore cannot work reliably while Gboard, SwiftKey, or another
external keyboard is selected.

To enable secure-field expansion:

1. Open **Settings > Setup & troubleshooting**.
2. Tap **Enable the bundled keyboard** and enable `rnl espanso`.
3. Select the `rnl espanso` keyboard for the password field.
4. Enable **Settings > Password-field expansion** and confirm the warning.

Password-field expansion is disabled by default. In secure fields, the bundled
keyboard keeps only the current trigger buffer in memory, hides shortcut
suggestions and replacement previews, and clears the buffer when input ends.

## Shortcut editor fields

- **Keyword:** trigger text, for example `;email`.
- **Replacement text:** local text inserted after a match.
- **Replace after Space:** waits until Space is typed before expanding.
- **Folder:** organizational folder shown on the home page.
- **Position:** match priority; lower numbers are checked first.
- **Template strategy:** compatibility metadata preserved for desktop espanso
  CSV import/export. Android 1.7.0 does not use it to select a replacement, so
  leave it blank for normal shortcuts.

## CSV format

Imports and exports use these exact headers:

```csv
Keyword,Text,Replace after Typing Space,Position,Multiple Templates Selection Strategy,Folder
;email,hello@example.com,false,1,,Personal
```

Import replaces the current local shortcut database only after confirmation.
Export a backup first when the current shortcuts must be preserved.

## Known limits and fixes

- **Expansion stops after updating the app:** turn `rnl espanso expansion` off
  and on again in Accessibility settings.
- **A game, terminal, or custom editor does not expand:** that app may not
  expose a compatible editor connection. Try the bundled keyboard.
- **Password expansion does not work:** select the bundled keyboard and enable
  Password-field expansion in app Settings.
- **Payment & Banking apps (Paytm, PhonePe, GPay, etc.):** payment apps perform
  security checks on active accessibility services. Open **Settings > Auto-pause & excluded apps**
  to configure apps where Espanso automatically pauses text expansion.
  - **Smart In-App Pause (Default):** Text expansion and suggestions are completely
    paused while in excluded apps, and automatically re-enabled the moment you exit
    back to your home screen or another app. No manual switching required!
  - **Full System Disable (Optional):** For strict banking apps requiring the service
    to be turned off in Android Settings, you can grant `WRITE_SECURE_SETTINGS` via ADB
    (`adb shell pm grant com.manishrnl.espansoandroid android.permission.WRITE_SECURE_SETTINGS`)
    for 100% automated OS-level toggle and automatic re-enable upon app exit.
- **Restricted setting appears:** use the guided App info flow described above.

These instructions are also included inside the app under
**Settings > Setup & troubleshooting**.

## Build requirements

- JDK 26 for Gradle
- JDK 21 toolchain for Android compilation
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0

### Build an APK into the project root

From PowerShell or Command Prompt in the repository root:

```powershell
.\build-apk.bat
```

This runs unit tests, Android lint, and the debug build, then creates:

```text
.\rnl-espanso-debug.apk
```

For a signed release APK, configure `keystore.properties` first, then run:

```powershell
.\build-apk.bat release
```

The signed output is copied to:

```text
.\rnl-espanso-release.apk
```

## Signed release build

Release signing reads the ignored `keystore.properties` file:

```properties
storeFile=release/rnl-espanso-release.jks
storePassword=...
keyAlias=rnl-espanso
keyPassword=...
```

Build the signed APK and Android App Bundle:

```powershell
.\gradlew.bat clean test lintRelease bundleRelease assembleRelease
```

Outputs:

```text
%LOCALAPPDATA%\EspansoAndroid\build\app\outputs\apk\release\app-release.apk
%LOCALAPPDATA%\EspansoAndroid\build\app\outputs\bundle\release\app-release.aab
```

Never upload a debug or unsigned APK. Keep permanent offline backups of the
release keystore and its passwords; future store updates require the same key
and a higher `versionCode`.

See [docs/INDUS_APPSTORE_RELEASE.md](docs/INDUS_APPSTORE_RELEASE.md) for the
store checklist and [docs/RELEASE_NOTES_1.7.0.md](docs/RELEASE_NOTES_1.7.0.md)
for this release.

## Privacy

The app has no internet permission. Shortcuts, settings, and matching remain on
the device. CSV files are accessed only through Android's system document
picker. Android cloud backup is disabled.

The complete policy is available in [PRIVACY_POLICY.md](PRIVACY_POLICY.md) and
inside the app under **Settings > Privacy policy**.

## Developer

Manish Kumar / Manish rnl

- Website: https://www.manishrnl.in/projects
- Instagram: https://www.instagram.com/manishrnl/
- Facebook: https://www.facebook.com/manishrnl/
- GitHub: https://github.com/manishrnl/
- Phone: +91 95014 21887
