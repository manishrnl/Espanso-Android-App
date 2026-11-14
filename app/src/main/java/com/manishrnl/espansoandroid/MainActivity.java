package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class MainActivity extends Activity {
    private ShortcutDatabase database;
    private FolderAdapter adapter;
    private TextView folderSummary;
    private TextView emptyFolders;
    private String appliedThemeMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appliedThemeMode = AppPreferences.getThemeMode(this);
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_main);

        database = new ShortcutDatabase(this);
        adapter = new FolderAdapter(this);

        ListView folderList = findViewById(R.id.folderList);
        View header = LayoutInflater.from(this).inflate(
                R.layout.header_main,
                folderList,
                false
        );
        folderSummary = header.findViewById(R.id.folderSummary);
        emptyFolders = header.findViewById(R.id.emptyFolders);
        header.findViewById(R.id.addFolderButton).setOnClickListener(view ->
                showCreateFolderDialog());
        header.findViewById(R.id.searchButton).setOnClickListener(view ->
                startActivity(new Intent(this, SearchActivity.class)));
        header.findViewById(R.id.settingsButton).setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class)));

        folderList.addHeaderView(header, null, false);
        folderList.setAdapter(adapter);
        folderList.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof FolderAdapter.FolderItem) {
                FolderAdapter.FolderItem folder = (FolderAdapter.FolderItem) item;
                Intent intent = new Intent(this, FolderActivity.class);
                intent.putExtra(FolderActivity.EXTRA_FOLDER, folder.getFolder());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!appliedThemeMode.equals(AppPreferences.getThemeMode(this))) {
            recreate();
            return;
        }
        reloadFolders();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void reloadFolders() {
        List<Shortcut> shortcuts = database.getAll();
        adapter.setData(shortcuts, database.getFolders());
        String folderCount = getResources().getQuantityString(
                R.plurals.folder_count,
                adapter.getCount(),
                adapter.getCount()
        );
        String shortcutCount = getResources().getQuantityString(
                R.plurals.shortcut_count,
                shortcuts.size(),
                shortcuts.size()
        );
        folderSummary.setText(getString(
                R.string.folder_and_shortcut_summary,
                folderCount,
                shortcutCount
        ));
        emptyFolders.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showCreateFolderDialog() {
        View content = LayoutInflater.from(this).inflate(
                R.layout.dialog_create_folder,
                null,
                false
        );
        EditText input = content.findViewById(R.id.folderNameInput);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();
        content.findViewById(R.id.folderCancelButton).setOnClickListener(view ->
                dialog.dismiss());
        content.findViewById(R.id.folderCreateButton).setOnClickListener(view -> {
            String folderName = input.getText().toString().trim();
            if (TextUtils.isEmpty(folderName)) {
                input.setError(getString(R.string.folder_name_required));
                return;
            }
            if (!database.insertFolder(folderName)) {
                Toast.makeText(
                        this,
                        R.string.folder_already_exists,
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            dialog.dismiss();
            reloadFolders();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            );
        }
        input.requestFocus();
    }
}
