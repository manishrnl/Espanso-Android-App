# Privacy Policy for RNL ESPANSO

Effective date: November 13, 2026

RNL ESPANSO is an offline text-expansion application developed by Manish rnl.
It does not require an account and does not include advertising, analytics, tracking SDKs, or internet access.

## Data processed

The app stores shortcut keywords, replacement text, folder names, and app
settings in private storage on the user's device. CSV data is read or written
only when the user selects a file through Android's system document picker.

## Accessibility service

The optional accessibility service is required for the app's core text
expansion feature when the user types with Gboard, SwiftKey, Samsung Keyboard, or another
keyboard. It processes text in the currently focused editable field to:

- match locally stored shortcut keywords;
- insert the selected local replacement;
- show local shortcut suggestions; and
- restore the typed keyword when the user immediately undoes an expansion.

Password fields are ignored by the accessibility service by default. Typed text is not
retained as typing history, collected for profiling, shared with third parties,
or transmitted off the device.

## Payment & Banking App Protection

rnl espanso automatically detects and excludes payment, UPI, banking, and financial applications (including Paytm, PhonePe, Google Pay, YONO SBI, BHIM, CRED, and mobile banking apps). Accessibility events and window content from payment apps are completely bypassed and ignored, ensuring payment applications operate securely without security flags or vulnerability warnings.

## Optional fallback keyboard

The app includes an optional fallback input method. Text entered through it is
processed locally for shortcut matching and is not transmitted or stored as
typing history.

Password-field expansion through the bundled keyboard is disabled by default
and requires explicit confirmation in Settings. When enabled, only the current
trigger buffer is held temporarily in memory for local matching. Replacement
previews are not shown in secure fields.

## Data sharing and collection

rnl espanso does not collect or share personal data. It has no internet
permission and contains no third-party advertising or analytics SDK.

## Retention and deletion

Active shortcuts remain in local app storage until the user edits, deletes, or
replaces them. Deleted folders and shortcuts remain in the local recycle bin
for up to 90 days unless restored or permanently deleted earlier. Uninstalling
the app removes its private local data.

## Security

Android application-private storage is used for the local database. Android
cloud backup is disabled.

# Version

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

## Shortcut editor fields

- **Keyword:** trigger text, for example `;email`.
- **Replacement text:** local text inserted after a match.
- **Replace after Space:** waits until Space is typed before expanding.
- **Folder:** organizational folder shown on the home page.
- **Position:** match priority; lower numbers are checked first.
- **Template strategy:** compatibility metadata preserved for desktop espanso
  CSV import/export. Android 1.7.0 does not use it to select a replacement, so
  leave it blank for normal shortcuts.

## Privacy

The app has no internet permission. Shortcuts, settings, and matching remain on
the device. CSV files are accessed only through Android's system document
picker. Android cloud backup is disabled.

The complete policy is available in [PRIVACY_POLICY.md](PRIVACY_POLICY.md) and
inside the app under **Settings > Privacy policy**.

## Developer

Manish Kumar

- Website: https://www.manishrnl.in/projects
- Instagram: https://www.instagram.com/manishrnl/
- Facebook: https://www.facebook.com/manishrnl/
- GitHub: https://github.com/manishrnl/
- Phone: +91 95014 21887