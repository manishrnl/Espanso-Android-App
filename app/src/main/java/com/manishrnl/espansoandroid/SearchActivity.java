package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SearchActivity extends Activity {
    private final List<Shortcut> allShortcuts = new ArrayList<>();

    private ShortcutDatabase database;
    private ShortcutAdapter adapter;
    private EditText searchInput;
    private TextView searchSummary;
    private TextView emptySearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_search);

        database = new ShortcutDatabase(this);
        adapter = new ShortcutAdapter(this, true);

        ListView shortcutList = findViewById(R.id.searchShortcutList);
        View header = LayoutInflater.from(this).inflate(
                R.layout.header_search,
                shortcutList,
                false
        );
        searchInput = header.findViewById(R.id.searchInput);
        searchSummary = header.findViewById(R.id.searchSummary);
        emptySearch = header.findViewById(R.id.emptySearch);
        header.findViewById(R.id.searchBackButton).setOnClickListener(view -> finish());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count
            ) {
                filterShortcuts(text == null ? "" : text.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        shortcutList.addHeaderView(header, null, false);
        shortcutList.setAdapter(adapter);
        shortcutList.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof Shortcut) {
                Intent intent = new Intent(this, EditorActivity.class);
                intent.putExtra(
                        EditorActivity.EXTRA_SHORTCUT_ID,
                        ((Shortcut) item).getId()
                );
                startActivity(intent);
            }
        });

        searchInput.requestFocus();
        searchInput.postDelayed(() -> {
            InputMethodManager manager = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE
            );
            manager.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        }, 180);
    }

    @Override
    protected void onResume() {
        super.onResume();
        allShortcuts.clear();
        allShortcuts.addAll(database.getAll());
        allShortcuts.sort(
                Comparator.comparing(
                        Shortcut::getKeyword,
                        String.CASE_INSENSITIVE_ORDER
                ).thenComparing(
                        Shortcut::getFolder,
                        String.CASE_INSENSITIVE_ORDER
                )
        );
        filterShortcuts(searchInput.getText().toString());
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void filterShortcuts(String query) {
        String normalized = query == null ? "" : query.trim();
        List<Shortcut> matches = new ArrayList<>();
        for (Shortcut shortcut : allShortcuts) {
            if (normalized.isEmpty() || shortcut.matchesSearch(normalized)) {
                matches.add(shortcut);
            }
        }
        adapter.setItems(matches);
        searchSummary.setText(getResources().getQuantityString(
                R.plurals.shortcut_count,
                matches.size(),
                matches.size()
        ));
        emptySearch.setText(normalized.isEmpty()
                ? R.string.no_shortcuts_available
                : R.string.no_search_results);
        emptySearch.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
