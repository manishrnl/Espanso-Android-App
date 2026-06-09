package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class FolderActivity extends Activity {
    public static final String EXTRA_FOLDER = "folder";

    private ShortcutDatabase database;
    private ShortcutAdapter adapter;
    private String folder;
    private TextView shortcutSummary;
    private TextView emptyShortcuts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_folder);

        folder = getIntent().getStringExtra(EXTRA_FOLDER);
        if (folder == null) {
            folder = "";
        }
        database = new ShortcutDatabase(this);
        adapter = new ShortcutAdapter(this);

        ListView shortcutList = findViewById(R.id.folderShortcutList);
        View header = LayoutInflater.from(this).inflate(
                R.layout.header_folder,
                shortcutList,
                false
        );
        TextView title = header.findViewById(R.id.folderTitle);
        if (folder.isEmpty()) {
            title.setText(R.string.unfiled);
        } else {
            title.setText(folder.replace("\\", " / "));
        }
        shortcutSummary = header.findViewById(R.id.folderShortcutSummary);
        emptyShortcuts = header.findViewById(R.id.emptyShortcuts);
        header.findViewById(R.id.folderBackButton).setOnClickListener(view -> finish());
        header.findViewById(R.id.folderAddButton).setOnClickListener(view ->
                openEditor(-1));

        shortcutList.addHeaderView(header, null, false);
        shortcutList.setAdapter(adapter);
        shortcutList.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof Shortcut) {
                openEditor(((Shortcut) item).getId());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadShortcuts();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void reloadShortcuts() {
        List<Shortcut> folderShortcuts = new ArrayList<>();
        for (Shortcut shortcut : database.getAll()) {
            if (folder.equals(shortcut.getFolder().trim())) {
                folderShortcuts.add(shortcut);
            }
        }
        adapter.setItems(folderShortcuts);
        shortcutSummary.setText(getResources().getQuantityString(
                R.plurals.shortcut_count,
                folderShortcuts.size(),
                folderShortcuts.size()
        ));
        emptyShortcuts.setVisibility(
                folderShortcuts.isEmpty() ? View.VISIBLE : View.GONE
        );
    }

    private void openEditor(long shortcutId) {
        Intent intent = new Intent(this, EditorActivity.class);
        if (shortcutId >= 0) {
            intent.putExtra(EditorActivity.EXTRA_SHORTCUT_ID, shortcutId);
        } else {
            intent.putExtra(EditorActivity.EXTRA_DEFAULT_FOLDER, folder);
        }
        startActivity(intent);
    }
}
