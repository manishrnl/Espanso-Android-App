package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class TrashActivity extends Activity {
    private ShortcutDatabase database;
    private TrashAdapter adapter;
    private TextView summary;
    private TextView empty;
    private View restoreAllTrashButton;
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
        restoreAllTrashButton = header.findViewById(R.id.restoreAllTrashButton);
        emptyTrashButton = header.findViewById(R.id.emptyTrashButton);
        header.findViewById(R.id.trashBackButton).setOnClickListener(view -> finish());
        restoreAllTrashButton.setOnClickListener(view -> confirmRestoreAllTrash());
        emptyTrashButton.setOnClickListener(view -> confirmEmptyTrash());

        list.addHeaderView(header, null, false);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof TrashEntry) {
                TrashEntry entry = (TrashEntry) item;
                if (entry.isFolder()) {
                    showFolderContentsDialog(entry);
                } else {
                    showShortcutActions(entry, null);
                }
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
        restoreAllTrashButton.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyTrashButton.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showFolderContentsDialog(TrashEntry folderEntry) {
        List<TrashEntry> folderShortcuts = database.getTrashShortcutsForFolder(folderEntry.getId());
        if (folderShortcuts.isEmpty()) {
            showFolderActions(folderEntry);
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_trash_folder_contents,
                null,
                false
        );

        TextView subtitle = dialogView.findViewById(R.id.dialogFolderSubtitle);
        TextView dateText = dialogView.findViewById(R.id.dialogFolderDate);
        LinearLayout itemsContainer = dialogView.findViewById(R.id.dialogFolderItemsContainer);

        dateText.setText(TrashAdapter.formatDeletionDetails(this, folderEntry.getDeletedAt()));

        subtitle.setText(getResources().getQuantityString(
                R.plurals.restore_folder_message,
                folderShortcuts.size(),
                folderEntry.getTitle(),
                folderShortcuts.size()
        ));

        AlertDialog[] folderDialogHolder = new AlertDialog[1];

        for (TrashEntry shortcut : folderShortcuts) {
            View itemView = LayoutInflater.from(this).inflate(
                    R.layout.row_trash,
                    itemsContainer,
                    false
            );

            ImageView icon = itemView.findViewById(R.id.trashIcon);
            TextView kind = itemView.findViewById(R.id.trashKindText);
            TextView title = itemView.findViewById(R.id.trashTitleText);
            TextView detail = itemView.findViewById(R.id.trashDetailText);
            TextView date = itemView.findViewById(R.id.trashDateText);

            icon.setImageResource(R.drawable.ic_shortcut);
            kind.setText(R.string.shortcut);
            title.setText(shortcut.getTitle().replace("\\", " / "));

            String replacement = TextUtils.isEmpty(shortcut.getDetail())
                    ? getString(R.string.empty_replacement)
                    : shortcut.getDetail();
            detail.setText(replacement);
            date.setText(TrashAdapter.formatDeletionDetails(this, shortcut.getDeletedAt()));

            itemView.setOnClickListener(v -> {
                if (folderDialogHolder[0] != null) {
                    folderDialogHolder[0].dismiss();
                }
                showShortcutActions(shortcut, folderEntry);
            });

            itemsContainer.addView(itemView);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(folderEntry.getTitle().replace("\\", " / "))
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete_permanently, (dialog, which) ->
                        confirmPermanentDelete(folderEntry))
                .setPositiveButton(R.string.restore_all, (dialog, which) -> {
                    if (database.restoreTrashEntry(folderEntry.getType(), folderEntry.getId())) {
                        Toast.makeText(this, R.string.restored, Toast.LENGTH_SHORT).show();
                    }
                    reload();
                });

        folderDialogHolder[0] = builder.show();
    }

    private void showFolderActions(TrashEntry folderEntry) {
        String baseDetail = getResources().getQuantityString(
                R.plurals.restore_folder_message,
                folderEntry.getChildCount(),
                folderEntry.getTitle(),
                folderEntry.getChildCount()
        );
        String deletionDetail = TrashAdapter.formatDeletionDetails(this, folderEntry.getDeletedAt());
        String fullDetail = baseDetail + "\n\n" + deletionDetail;

        new AlertDialog.Builder(this)
                .setTitle(folderEntry.getTitle().replace("\\", " / "))
                .setMessage(fullDetail)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete_permanently, (dialog, which) ->
                        confirmPermanentDelete(folderEntry))
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    if (database.restoreTrashEntry(folderEntry.getType(), folderEntry.getId())) {
                        Toast.makeText(this, R.string.restored, Toast.LENGTH_SHORT).show();
                    }
                    reload();
                })
                .show();
    }

    private void showShortcutActions(TrashEntry shortcut, TrashEntry parentFolder) {
        String baseDetail = getString(R.string.restore_shortcut_message, shortcut.getTitle());
        String deletionDetail = TrashAdapter.formatDeletionDetails(this, shortcut.getDeletedAt());
        String fullDetail = baseDetail + "\n\n" + deletionDetail;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(shortcut.getTitle().replace("\\", " / "))
                .setMessage(fullDetail)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    if (parentFolder != null) {
                        showFolderContentsDialog(parentFolder);
                    }
                })
                .setNeutralButton(R.string.delete_permanently, (dialog, which) -> {
                    confirmPermanentDeleteShortcut(shortcut, parentFolder);
                })
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    if (database.restoreTrashEntry(shortcut.getType(), shortcut.getId())) {
                        Toast.makeText(this, R.string.restored, Toast.LENGTH_SHORT).show();
                    }
                    reload();
                    if (parentFolder != null) {
                        showFolderContentsDialog(parentFolder);
                    }
                });

        builder.show();
    }

    private void confirmPermanentDeleteShortcut(TrashEntry shortcut, TrashEntry parentFolder) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_permanently_title)
                .setMessage(R.string.delete_permanently_message)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    if (parentFolder != null) {
                        showFolderContentsDialog(parentFolder);
                    }
                })
                .setPositiveButton(R.string.delete_permanently, (dialog, which) -> {
                    database.permanentlyDeleteTrashEntry(
                            shortcut.getType(),
                            shortcut.getId()
                    );
                    reload();
                    if (parentFolder != null) {
                        showFolderContentsDialog(parentFolder);
                    }
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

    private void confirmRestoreAllTrash() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_all_trash_title)
                .setMessage(R.string.restore_all_trash_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore_all, (dialog, which) -> {
                    database.restoreAllTrash();
                    Toast.makeText(this, R.string.restored_all, Toast.LENGTH_SHORT).show();
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
