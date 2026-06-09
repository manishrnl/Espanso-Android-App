# Indus Appstore release checklist

## Already prepared in the project

- App title and launcher label: `rnl espanso`
- Package name: `com.manishrnl.espansoandroid`
- Version: `1.6.0` (`versionCode 7`)
- Target SDK: 36
- Minimum SDK: 26
- 1:1 vector logo source: `logo.svg`
- No internet, analytics, advertising, account, or cloud-backup functionality
- Accessibility disclosure shown before opening Android Accessibility settings
- Privacy policy draft: `PRIVACY_POLICY.md`
- Release signing can be configured through an ignored `keystore.properties`

Do not change the package name or release signing key after the first public
upload. Future updates must use a higher version code and the same signing key.

## Create the permanent release key

Use the JDK `keytool` and store the key outside source control:

```powershell
New-Item -ItemType Directory -Force release

& "C:\Users\MANISH\.jdks\openjdk-26.0.1\bin\keytool.exe" `
  -genkeypair `
  -v `
  -keystore release\rnl-espanso-upload.jks `
  -alias rnl-espanso `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

Copy `keystore.properties.example` to `keystore.properties`, enter the actual
passwords, and keep secure backups of both files. Losing this key prevents
updates to the existing store listing.

## Build upload files

```powershell
.\gradlew.bat clean testReleaseUnitTest lintRelease bundleRelease assembleRelease
```

With `keystore.properties` configured, expected signed outputs are:

```text
%LOCALAPPDATA%\EspansoAndroid\build\app\outputs\bundle\release\app-release.aab
%LOCALAPPDATA%\EspansoAndroid\build\app\outputs\apk\release\app-release.apk
```

Without `keystore.properties`, Gradle instead creates
`app-release-unsigned.apk`; the AAB is also unsigned. Do not upload either
unsigned artifact. Verify the selected release APK or AAB with your permanent
release key before uploading it. Never upload a debug APK to the public store.

## Store listing

Complete these portal sections:

1. App details: title `rnl espanso`, Productivity category, and app logo.
2. Metadata: short description, long description, and screenshots.
3. Indian-language metadata: review the automatic translations before use.
4. Support and data safety: support email, website, privacy-policy URL, and
   accurate accessibility/data declarations.
5. Upload: signed APK, or AAB plus its keystore details.

Indus currently requires at least 2 and allows up to 8 screenshots. Use portrait
`1080x1920` or landscape `1920x1080` JPG/JPEG/PNG files, each under 2 MB.

The support email must match the email published on the developer website and
privacy-policy page. The current website page does not expose an email address,
so add one before submission.

## Suggested data-safety answers

- Data collected: No
- Data shared with third parties: No
- Account required: No
- Ads: No
- Analytics: No
- Internet access: No
- Local shortcut storage: Yes, app-private device storage
- Accessibility service: Yes, required for the core expansion feature with
  third-party keyboards
- Password fields: Ignored
- User deletion: Individual deletion, recycle bin, permanent deletion, and
  automatic deletion after 90 days

## Suggested listing copy

Short description:

> Expand local text shortcuts while typing with Gboard, SwiftKey, or any keyboard.

Long description:

> rnl espanso is an offline text-expansion app for Android. Create folders and
> shortcuts, type a keyword in a supported text field, and insert your local
> replacement without switching away from Gboard, SwiftKey, or your preferred
> keyboard. The app supports popup suggestions, immediate undo, CSV import and
> export, folder organization, search, and a 90-day recycle bin. Password fields
> are ignored. The app has no account, ads, analytics, or internet permission,
> and shortcut processing remains on the device.

## Manual review before submission

- Publish `PRIVACY_POLICY.md` at a public HTTPS URL.
- Add the same support email to the policy, website, and developer portal.
- Capture screenshots without personal shortcut data.
- Test accessibility setup from the Indus-installed build.
- Confirm expansion and popup suggestions in Gboard and SwiftKey.
- Confirm import, export, recycle-bin restore, and permanent deletion.
- Keep the signing key and passwords in at least two secure backup locations.
