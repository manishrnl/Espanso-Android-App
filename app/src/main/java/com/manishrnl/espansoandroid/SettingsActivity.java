package com.manishrnl.espansoandroid;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class SettingsActivity extends Activity {
    private static final int REQUEST_IMPORT = 1001;
    private static final int REQUEST_EXPORT = 1002;
    private static final String STATE_PENDING_RESTRICTED_SETUP =
            "pending_restricted_setup";

    private ShortcutDatabase database;
    private boolean pendingRestrictedSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_settings);

        database = new ShortcutDatabase(this);
        pendingRestrictedSetup = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_PENDING_RESTRICTED_SETUP);
        findViewById(R.id.settingsBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.settingsEnableAccessibility).setOnClickListener(view ->
                openAccessibilitySetup());
        findViewById(R.id.settingsRestrictedHelp).setOnClickListener(view ->
                startRestrictedSetup());
        findViewById(R.id.settingsChooseKeyboard).setOnClickListener(view -> {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.showInputMethodPicker();
        });
        findViewById(R.id.settingsImport).setOnClickListener(view ->
                openImportPicker());
        findViewById(R.id.settingsExport).setOnClickListener(view ->
                openExportPicker());
        findViewById(R.id.settingsNewShortcut).setOnClickListener(view ->
                startActivity(new Intent(this, EditorActivity.class)));
        findViewById(R.id.settingsRecycleBin).setOnClickListener(view ->
                startActivity(new Intent(this, TrashActivity.class)));
        findViewById(R.id.settingsGuide).setOnClickListener(view ->
                startActivity(new Intent(this, GuideActivity.class)));
        findViewById(R.id.settingsPrivacy).setOnClickListener(view ->
                openInfoPage(InfoActivity.PAGE_PRIVACY));
        findViewById(R.id.settingsDeveloper).setOnClickListener(view ->
                openInfoPage(InfoActivity.PAGE_DEVELOPER));
        findViewById(R.id.settingsDonate).setOnClickListener(view ->
                startActivity(new Intent(this, DonateActivity.class)));
        setupThemeControls();
        setupPasswordExpansion();
        setupAutoPauseControls();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateExpansionStatus();
        updateRecycleBinCount();
        updateAutoPauseSummary();
        if (pendingRestrictedSetup) {
            pendingRestrictedSetup = false;
            openAccessibilitySettings();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_PENDING_RESTRICTED_SETUP, pendingRestrictedSetup);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQUEST_IMPORT) {
            importCsv(uri);
        } else if (requestCode == REQUEST_EXPORT) {
            exportCsv(uri);
        }
    }

    private void updateExpansionStatus() {
        AccessibilityManager manager =
                (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        boolean enabled = false;
        for (AccessibilityServiceInfo info : manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )) {
            android.content.pm.ServiceInfo serviceInfo =
                    info.getResolveInfo().serviceInfo;
            if (getPackageName().equals(serviceInfo.packageName)
                    && TextExpansionAccessibilityService.class.getName()
                    .equals(serviceInfo.name)) {
                enabled = true;
                break;
            }
        }

        TextView status = findViewById(R.id.settingsExpansionStatus);
        TextView detail = findViewById(R.id.settingsExpansionDetail);
        TextView button = findViewById(R.id.settingsEnableAccessibilityText);
        View restrictedHelp = findViewById(R.id.settingsRestrictedHelpCard);
        if (enabled) {
            status.setText(R.string.global_expansion_active);
            detail.setText(R.string.global_expansion_active_detail);
            button.setText(R.string.manage_global_expansion);
            restrictedHelp.setVisibility(View.GONE);
        } else {
            status.setText(R.string.global_expansion_disabled);
            detail.setText(R.string.global_expansion_disabled_detail);
            button.setText(R.string.enable_global_expansion);
            restrictedHelp.setVisibility(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ? View.VISIBLE
                            : View.GONE
            );
        }
    }

    private void updateRecycleBinCount() {
        int count = database.getTrashEntryCount();
        TextView button = findViewById(R.id.settingsRecycleBinText);
        button.setText(getString(R.string.recycle_bin_with_count, count));
    }

    private void setupThemeControls() {
        RadioGroup group = findViewById(R.id.settingsThemeGroup);
        String currentMode = AppPreferences.getThemeMode(this);
        if (AppPreferences.THEME_LIGHT.equals(currentMode)) {
            group.check(R.id.settingsThemeLight);
        } else if (AppPreferences.THEME_DARK.equals(currentMode)) {
            group.check(R.id.settingsThemeDark);
        } else {
            group.check(R.id.settingsThemeSystem);
        }
        updateThemeSummary(currentMode);
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            String mode;
            if (checkedId == R.id.settingsThemeLight) {
                mode = AppPreferences.THEME_LIGHT;
            } else if (checkedId == R.id.settingsThemeDark) {
                mode = AppPreferences.THEME_DARK;
            } else {
                mode = AppPreferences.THEME_SYSTEM;
            }
            if (!mode.equals(AppPreferences.getThemeMode(this))) {
                AppPreferences.setThemeMode(this, mode);
                recreate();
            }
        });
    }

    private void updateThemeSummary(String mode) {
        int label = R.string.theme_system;
        if (AppPreferences.THEME_LIGHT.equals(mode)) {
            label = R.string.theme_light;
        } else if (AppPreferences.THEME_DARK.equals(mode)) {
            label = R.string.theme_dark;
        }
        TextView summary = findViewById(R.id.settingsThemeSummary);
        summary.setText(getString(R.string.theme_current, getString(label)));
    }

    @SuppressWarnings("UseSwitchCompatOrMaterialCode")
    private void setupPasswordExpansion() {
        Switch toggle = findViewById(R.id.settingsPasswordExpansion);
        toggle.setChecked(AppPreferences.isPasswordExpansionEnabled(this));
        toggle.setOnCheckedChangeListener((button, enabled) -> {
            if (!enabled) {
                AppPreferences.setPasswordExpansionEnabled(this, false);
                return;
            }
            if (AppPreferences.isPasswordExpansionEnabled(this)) {
                return;
            }
            button.setChecked(false);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.password_expansion_warning_title)
                    .setMessage(R.string.password_expansion_warning_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.enable, (dialog, which) -> {
                        AppPreferences.setPasswordExpansionEnabled(this, true);
                        button.setChecked(true);
                    })
                    .show();
        });
    }

    @SuppressWarnings("UseSwitchCompatOrMaterialCode")
    private void setupAutoPauseControls() {
        Switch autoPauseSwitch = findViewById(R.id.settingsAutoPausePayment);
        autoPauseSwitch.setChecked(AppPreferences.isAutoPauseForPaymentAppsEnabled(this));
        autoPauseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreferences.setAutoPauseForPaymentAppsEnabled(this, isChecked);
            updateAutoPauseSummary();
        });

        findViewById(R.id.settingsManageExcludedApps).setOnClickListener(v ->
                startActivity(new Intent(this, ExcludedAppsActivity.class)));
        findViewById(R.id.settingsManagePermissions).setOnClickListener(v ->
                PermissionsDialogHelper.show(this));
        updateAutoPauseSummary();
    }

    private void updateAutoPauseSummary() {
        Switch autoPauseSwitch = findViewById(R.id.settingsAutoPausePayment);
        if (autoPauseSwitch != null) {
            autoPauseSwitch.setChecked(AppPreferences.isAutoPauseForPaymentAppsEnabled(this));
        }

        TextView detailView = findViewById(R.id.settingsManageExcludedAppsDetail);
        if (detailView != null) {
            int count = AppPreferences.getExcludedPaymentApps(this).size();
            boolean enabled = AppPreferences.isAutoPauseForPaymentAppsEnabled(this);
            String modeStr = AppPreferences.AUTO_PAUSE_MODE_SYSTEM.equals(AppPreferences.getAutoPauseMode(this))
                    ? getString(R.string.auto_pause_mode_system)
                    : getString(R.string.auto_pause_mode_smart);

            if (enabled) {
                detailView.setText(getString(R.string.excluded_apps_summary_format,
                        getString(R.string.excluded_apps_count, count), modeStr));
            } else {
                detailView.setText(getString(R.string.excluded_apps_summary_format,
                        getString(R.string.global_expansion_disabled),
                        getString(R.string.excluded_apps_count, count)));
            }
        }
    }

    private void openAccessibilitySetup() {
        if (AppPreferences.isAccessibilityDisclosureAccepted(this)) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.accessibility_disclosure_title)
                .setMessage(R.string.accessibility_disclosure_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.continue_label, (dialog, which) -> {
                    AppPreferences.setAccessibilityDisclosureAccepted(this, true);
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                })
                .show();
    }

    private void startRestrictedSetup() {
        pendingRestrictedSetup = true;
        Toast.makeText(
                this,
                R.string.restricted_settings_toast,
                Toast.LENGTH_LONG
        ).show();
        openAppInfo();
    }

    private void openAppInfo() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void openInfoPage(String page) {
        Intent intent = new Intent(this, InfoActivity.class);
        intent.putExtra(InfoActivity.EXTRA_PAGE, page);
        startActivity(intent);
    }

    private void openImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    private void openExportPicker() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "espanso-shortcuts-" + date + ".csv");
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    private void importCsv(Uri uri) {
        final List<Shortcut> imported;
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) {
                throw new IOException("The selected file could not be opened.");
            }
            imported = CsvCodec.read(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            );
        } catch (Exception error) {
            showError("Import failed", error);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.import_shortcuts_title)
                .setMessage(getResources().getQuantityString(
                        R.plurals.import_shortcuts_message,
                        imported.size(),
                        imported.size()
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.import_csv, (dialog, which) -> {
                    database.importShortcuts(imported); // Import new shortcuts
                    Toast.makeText(
                            this,
                            getResources().getQuantityString(
                                    R.plurals.imported_shortcuts,
                                    imported.size(),
                                    imported.size()
                            ),
                            Toast.LENGTH_LONG
                    ).show();
                })
                .show();
    }

    private void exportCsv(Uri uri) {
        List<Shortcut> shortcuts = database.getAll();
        try (OutputStream stream = getContentResolver().openOutputStream(uri, "wt")) {
            if (stream == null) {
                throw new IOException("The selected file could not be opened.");
            }
            CsvCodec.write(
                    new OutputStreamWriter(stream, StandardCharsets.UTF_8),
                    shortcuts
            );
            Toast.makeText(
                    this,
                    getResources().getQuantityString(
                            R.plurals.exported_shortcuts,
                            shortcuts.size(),
                            shortcuts.size()
                    ),
                    Toast.LENGTH_LONG
            ).show();
        } catch (Exception error) {
            showError("Export failed", error);
        }
    }

    private void showError(String title, Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = error.getClass().getSimpleName();
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
