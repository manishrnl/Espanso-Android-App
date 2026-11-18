# Privacy Policy for RNL ESPANSO

**Effective Date:** November 18, 2026  
**Last Updated:** November 18, 2026  
**Application:** RNL ESPANSO (Package: `com.manishrnl.espansoandroid`)  
**Developer / Data Fiduciary:** Manish Kumar (Manish rnl)  
**Applicable Legal Framework:** Digital Personal Data Protection Act, 2023 (DPDP Act, India) & Information Technology Act, 2000 (India)

---

## 1. Introduction & Core Privacy Philosophy

RNL ESPANSO is a privacy-first, 100% offline text-expansion utility designed to expand shortcuts and text templates locally on Android devices.

We believe that your personal data, keystrokes, and text belongs solely to you. RNL ESPANSO operates under a strict **Zero-Data-Collection Architecture**:
- **No Internet Access:** The application does not request the `android.permission.INTERNET` permission in its manifest and is physically incapable of transmitting any data off your device.
- **No User Account:** No signup, email registration, phone verification, or authentication is required.
- **No Third-Party Trackers or Ads:** The app contains zero analytics libraries, zero advertising SDKs, and zero telemetry software.
- **App-Private Local Storage:** All shortcuts, snippets, folder structures, and settings are stored exclusively in private SQLite database storage within your device sandbox. Android Cloud Backup is explicitly disabled.

---

## 2. Android Device Permissions & Explicit Disclosures

To operate seamlessly while respecting device integrity and store policies (including the **Indus Appstore Developer Guidelines**), RNL ESPANSO requests only the permissions strictly required for its core text expansion and auto-pause features:

### A. Accessibility Service (`android.permission.BIND_ACCESSIBILITY_SERVICE`)
- **Purpose:** Used strictly to detect user-configured shortcut keywords (e.g., `;email`, `!date`) in active text input fields across standard external keyboards (such as Gboard, SwiftKey, Samsung Keyboard) and substitute them with the user's defined text snippet.
- **Safeguards:** 
  - Keystrokes are **never logged, saved to typing history, or profiled**.
  - Password and secure PIN fields are ignored by default.
  - No background surveillance or audio/visual monitoring is performed.

### B. Input Method Service (`android.permission.BIND_INPUT_METHOD`)
- **Purpose:** Powers an optional bundled input method (keyboard) that allows users to trigger expansions in specialized environments where external accessibility access is restricted.
- **Safeguards:** Only the temporary trigger buffer is held in volatile memory to match shortcut patterns. It is purged as soon as text is expanded or focus changes.

### C. Package Visibility (`android.permission.QUERY_ALL_PACKAGES`)
- **Purpose:** Used exclusively for the user-facing **"Auto-pause & Excluded Apps"** feature. This allows users to view a list of installed applications and select specific apps where Espanso must automatically pause text expansion.
- **Safeguards:** The device package inventory is used only in real time to populate the on-device selection list. Your installed app list is never stored permanently, profiled, or shared.

### D. Post Notifications (`android.permission.POST_NOTIFICATIONS`)
- **Purpose:** Used solely on Android 13+ to display the optional foreground service notification and Quick Settings tile status indicator so users know whether text expansion is currently active or paused.
- **Safeguards:** The application will never send marketing, promotional, or unrequested push notifications.

### E. Advanced Automation Permissions (Optional)
- **`PACKAGE_USAGE_STATS`:** Optional permission used strictly to detect when an excluded app opens in the foreground to pause expansion.
- **`WRITE_SECURE_SETTINGS`:** Optional advanced toggle granting users complete OS-level automated service pausing for high-security applications via ADB.

---

## 3. Financial, Banking & UPI App Protection

RNL ESPANSO is designed to ensure complete safety and compliance when using Indian financial and banking applications:
- **Automatic Payment App Exclusion:** The application automatically detects and bypasses payment and UPI applications (including PhonePe, Paytm, Google Pay, BHIM, YONO SBI, CRED, and major Indian banking apps).
- **Zero Interception:** Accessibility window events and input content within financial apps are immediately bypassed, ensuring banking apps operate without security warnings or interference.

---

## 4. Data Storage, Retention & Deletion

- **Local Storage:** All shortcut keywords, replacements, and folder categories are stored in an app-private SQLite database (`/data/data/com.manishrnl.espansoandroid/databases/`).
- **Recycle Bin (Trash):** When shortcuts or folders are deleted, they are moved to a local 90-day recycle bin, allowing users to restore them or permanently purge them manually.
- **User-Initiated Deletion:** 
  - Users can delete individual shortcuts, folders, or purge the recycle bin at any time.
  - Clearing application data via Android Settings (`Settings > Apps > RNL ESPANSO > Storage > Clear Data`) immediately erases all stored data.
  - Uninstalling RNL ESPANSO permanently removes all local app data from the device.

---

## 5. Compliance with the Indian Digital Personal Data Protection Act, 2023 (DPDP Act)

In accordance with the DPDP Act, 2023:
1. **Right to Access Information:** All data stored by the app is visible to you within the app UI (folders, shortcut list, trash).
2. **Right to Correction & Erasure:** You have direct tools to edit, update, or permanently delete any shortcut or preference.
3. **Right of Grievance Redressal:** You have direct access to our designated Grievance Officer for questions or concerns regarding data processing.
4. **Children's Privacy:** RNL ESPANSO does not collect personal data from anyone, including children under 18 years of age.

---

## 6. Grievance Redressal & Contact Information

In compliance with Rule 3(2) of the Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules, 2021 and the DPDP Act, 2023, the details of our Grievance Officer are as follows:

- **Grievance / Data Protection Officer:** Manish Kumar
- **Email:** `manish.rnl@zohomail.in`
- **Official Website:** [https://www.manishrnl.in/contact-us](https://www.manishrnl.in/contact-us)
- **Direct Phone:** +91 95014 21887
- **Jurisdiction:** India
- **Redressal Timeline:** Any grievances or inquiries will be acknowledged within 48 hours and resolved within 30 days from receipt.

---

## 7. Updates to this Policy

Any updates to this Privacy Policy will be reflected with a revised "Effective Date" at the top of this document and will be included in subsequent application releases within the app under **Settings > Privacy policy**.
