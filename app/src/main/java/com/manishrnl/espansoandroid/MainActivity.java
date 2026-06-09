package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQUEST_IMPORT = 1001;
    private static final int REQUEST_EXPORT = 1002;

    private ShortcutDatabase database;
    private ShortcutAdapter adapter;
    private final List<Shortcut> allShortcuts = new ArrayList<>();
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_main);

        database = new ShortcutDatabase(this);
        adapter = new ShortcutAdapter(this);

        ListView listView = findViewById(R.id.shortcutList);
        listView.setAdapter(adapter);
        listView.setEmptyView(findViewById(R.id.emptyView));
        listView.setOnItemClickListener((parent, view, position, id) ->
                openEditor(adapter.getItem(position).getId()));

        findViewById(R.id.buttonEnableKeyboard).setOnClickListener(view ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));

        findViewById(R.id.buttonChooseKeyboard).setOnClickListener(view -> {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.showInputMethodPicker();
        });

        findViewById(R.id.buttonImport).setOnClickListener(view -> openImportPicker());
        findViewById(R.id.buttonExport).setOnClickListener(view -> openExportPicker());
        findViewById(R.id.buttonAdd).setOnClickListener(view -> openEditor(-1));
        findViewById(R.id.buttonTheme).setOnClickListener(view -> showThemeDialog());
        findViewById(R.id.buttonKeyboardSettings).setOnClickListener(view ->
                startActivity(new Intent(this, KeyboardSettingsActivity.class)));

        EditText searchInput = findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                currentQuery = text.toString();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable text) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadShortcuts();
        updateKeyboardStatus();
        updateThemeButton();
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

    private void openEditor(long shortcutId) {
        Intent intent = new Intent(this, EditorActivity.class);
        if (shortcutId >= 0) {
            intent.putExtra(EditorActivity.EXTRA_SHORTCUT_ID, shortcutId);
        }
        startActivity(intent);
    }

    private void reloadShortcuts() {
        allShortcuts.clear();
        allShortcuts.addAll(database.getAll());
        applyFilter();
    }

    private void applyFilter() {
        List<Shortcut> filtered = new ArrayList<>();
        for (Shortcut shortcut : allShortcuts) {
            if (shortcut.matchesSearch(currentQuery)) {
                filtered.add(shortcut);
            }
        }
        adapter.setItems(filtered);
        TextView count = findViewById(R.id.shortcutCount);
        count.setText(getResources().getQuantityString(
                R.plurals.shortcut_count,
                filtered.size(),
                filtered.size()
        ));
    }

    private void showThemeDialog() {
        String[] labels = {
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };
        String[] values = {
                AppPreferences.THEME_SYSTEM,
                AppPreferences.THEME_LIGHT,
                AppPreferences.THEME_DARK
        };
        String current = AppPreferences.getAppTheme(this);
        int selected = 0;
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(current)) {
                selected = index;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    AppPreferences.setAppTheme(this, values[which]);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateThemeButton() {
        TextView button = findViewById(R.id.buttonTheme);
        button.setText(AppPreferences.appThemeLabel(this));
    }

    private void updateKeyboardStatus() {
        InputMethodManager manager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        ComponentName component = new ComponentName(this, EspansoInputMethodService.class);
        boolean enabled = false;
        for (android.view.inputmethod.InputMethodInfo info
                : manager.getEnabledInputMethodList()) {
            if (getPackageName().equals(info.getPackageName())
                    && EspansoInputMethodService.class.getName()
                    .equals(info.getServiceName())) {
                enabled = true;
                break;
            }
        }

        String current = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD
        );
        boolean active = component.flattenToShortString().equals(current)
                || component.flattenToString().equals(current);

        TextView status = findViewById(R.id.keyboardStatusText);
        TextView detail = findViewById(R.id.keyboardStatusDetail);
        if (active) {
            status.setText(R.string.keyboard_status_active);
            detail.setText(R.string.keyboard_status_active_detail);
        } else if (enabled) {
            status.setText(R.string.keyboard_status_enabled);
            detail.setText(R.string.keyboard_status_enabled_detail);
        } else {
            status.setText(R.string.keyboard_status_disabled);
            detail.setText(R.string.keyboard_status_disabled_detail);
        }
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
            imported = CsvCodec.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception error) {
            showError("Import failed", error);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Replace current shortcuts?")
                .setMessage("Import " + imported.size()
                        + " shortcuts and replace all shortcuts currently stored on this device?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.import_csv, (dialog, which) -> {
                    database.replaceAll(imported);
                    reloadShortcuts();
                    Toast.makeText(
                            this,
                            "Imported " + imported.size() + " shortcuts.",
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
                    "Exported " + shortcuts.size() + " shortcuts.",
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
