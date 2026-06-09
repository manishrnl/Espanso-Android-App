package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public final class EditorActivity extends Activity {
    public static final String EXTRA_SHORTCUT_ID = "shortcut_id";
    public static final String EXTRA_DEFAULT_FOLDER = "default_folder";

    private ShortcutDatabase database;
    private long shortcutId = -1;
    private EditText keywordInput;
    private EditText replacementInput;
    private CheckBox replaceAfterSpaceInput;
    private EditText positionInput;
    private EditText strategyInput;
    private EditText folderInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_editor);

        database = new ShortcutDatabase(this);
        shortcutId = getIntent().getLongExtra(EXTRA_SHORTCUT_ID, -1);
        keywordInput = findViewById(R.id.keywordInput);
        replacementInput = findViewById(R.id.replacementInput);
        replaceAfterSpaceInput = findViewById(R.id.replaceAfterSpaceInput);
        positionInput = findViewById(R.id.positionInput);
        strategyInput = findViewById(R.id.strategyInput);
        folderInput = findViewById(R.id.folderInput);

        TextView title = findViewById(R.id.editorTitle);
        title.setText(shortcutId >= 0 ? R.string.edit_shortcut : R.string.new_shortcut);
        positionInput.setText("1");
        if (shortcutId < 0) {
            folderInput.setText(getIntent().getStringExtra(EXTRA_DEFAULT_FOLDER));
        }

        View deleteButton = findViewById(R.id.deleteButton);
        if (shortcutId >= 0) {
            Shortcut shortcut = database.getById(shortcutId);
            if (shortcut == null) {
                Toast.makeText(this, "Shortcut no longer exists.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            populate(shortcut);
            deleteButton.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.cancelButton).setOnClickListener(view -> finish());
        findViewById(R.id.saveButton).setOnClickListener(view -> save());
        deleteButton.setOnClickListener(view -> confirmDelete());
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void populate(Shortcut shortcut) {
        keywordInput.setText(shortcut.getKeyword());
        replacementInput.setText(shortcut.getText());
        replaceAfterSpaceInput.setChecked(shortcut.isReplaceAfterSpace());
        positionInput.setText(String.valueOf(shortcut.getPosition()));
        strategyInput.setText(shortcut.getSelectionStrategy());
        folderInput.setText(shortcut.getFolder());
    }

    private void save() {
        String keyword = keywordInput.getText().toString();
        if (TextUtils.isEmpty(keyword)) {
            keywordInput.setError("Keyword is required.");
            keywordInput.requestFocus();
            return;
        }

        int position;
        try {
            position = Math.max(1, Integer.parseInt(positionInput.getText().toString()));
        } catch (NumberFormatException error) {
            positionInput.setError("Enter a whole number of 1 or greater.");
            positionInput.requestFocus();
            return;
        }

        Shortcut shortcut = new Shortcut(
                shortcutId,
                keyword,
                replacementInput.getText().toString(),
                replaceAfterSpaceInput.isChecked(),
                position,
                strategyInput.getText().toString(),
                folderInput.getText().toString()
        );

        if (shortcutId >= 0) {
            database.update(shortcut);
        } else {
            database.insert(shortcut);
        }
        setResult(RESULT_OK);
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete shortcut?")
                .setMessage("This removes the shortcut from local device storage.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    database.delete(shortcutId);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }
}
