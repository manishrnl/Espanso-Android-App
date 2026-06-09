package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class TrashActivity extends Activity {
    private ShortcutDatabase database;
    private TrashAdapter adapter;
    private TextView summary;
    private TextView empty;
    private View emptyTrashButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_trash);

        database = new ShortcutDatabase(this);
        adapter = new TrashAdapter(this);

        ListView list = findViewById(R.id.trashList);
        View header = LayoutInflater.from(this).inflate(
                R.layout.header_trash,
                list,
                false
        );
        summary = header.findViewById(R.id.trashSummary);
        empty = header.findViewById(R.id.emptyTrash);
        emptyTrashButton = header.findViewById(R.id.emptyTrashButton);
        header.findViewById(R.id.trashBackButton).setOnClickListener(view -> finish());
        emptyTrashButton.setOnClickListener(view -> confirmEmptyTrash());

        list.addHeaderView(header, null, false);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof TrashEntry) {
                showEntryActions((TrashEntry) item);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void reload() {
        List<TrashEntry> entries = database.getTrashEntries();
        adapter.setItems(entries);
        summary.setText(getResources().getQuantityString(
                R.plurals.trash_entry_count,
                entries.size(),
                entries.size()
        ));
        boolean isEmpty = entries.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        emptyTrashButton.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showEntryActions(TrashEntry entry) {
        String detail = entry.isFolder()
                ? getResources().getQuantityString(
                        R.plurals.restore_folder_message,
                        entry.getChildCount(),
                        entry.getTitle(),
                        entry.getChildCount()
                )
                : getString(R.string.restore_shortcut_message, entry.getTitle());
        new AlertDialog.Builder(this)
                .setTitle(entry.getTitle().replace("\\", " / "))
                .setMessage(detail)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete_permanently, (dialog, which) ->
                        confirmPermanentDelete(entry))
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    if (database.restoreTrashEntry(entry.getType(), entry.getId())) {
                        Toast.makeText(this, R.string.restored, Toast.LENGTH_SHORT).show();
                    }
                    reload();
                })
                .show();
    }

    private void confirmPermanentDelete(TrashEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_permanently_title)
                .setMessage(R.string.delete_permanently_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_permanently, (dialog, which) -> {
                    database.permanentlyDeleteTrashEntry(
                            entry.getType(),
                            entry.getId()
                    );
                    reload();
                })
                .show();
    }

    private void confirmEmptyTrash() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.empty_recycle_bin_title)
                .setMessage(R.string.empty_recycle_bin_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.empty_recycle_bin, (dialog, which) -> {
                    database.emptyTrash();
                    reload();
                })
                .show();
    }
}
