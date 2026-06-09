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

    private ShortcutDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_settings);

        database = new ShortcutDatabase(this);
        findViewById(R.id.settingsBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.settingsEnableAccessibility).setOnClickListener(view ->
                openAccessibilitySetup());
        findViewById(R.id.settingsRestrictedHelp).setOnClickListener(view ->
                showRestrictedSettingsHelp());
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
        findViewById(R.id.settingsAppearance).setOnClickListener(view ->
                startActivity(new Intent(this, KeyboardSettingsActivity.class)));
        findViewById(R.id.settingsDeveloperWebsite).setOnClickListener(view ->
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.developer_website_url))
                )));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateExpansionStatus();
        updateRecycleBinCount();
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
        TextView button = findViewById(R.id.settingsEnableAccessibility);
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
        TextView button = findViewById(R.id.settingsRecycleBin);
        button.setText(getString(R.string.recycle_bin_with_count, count));
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

    private void showRestrictedSettingsHelp() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restricted_settings_title)
                .setMessage(R.string.restricted_settings_message)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.open_accessibility, (dialog, which) ->
                        openAccessibilitySettings())
                .setPositiveButton(R.string.open_app_info, (dialog, which) ->
                        openAppInfo())
                .show();
    }

    private void openAppInfo() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
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
                .setTitle(R.string.replace_shortcuts_title)
                .setMessage(getResources().getQuantityString(
                        R.plurals.replace_shortcuts_message,
                        imported.size(),
                        imported.size()
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.import_csv, (dialog, which) -> {
                    database.replaceAll(imported);
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
