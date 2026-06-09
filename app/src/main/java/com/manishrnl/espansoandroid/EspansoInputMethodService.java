package com.manishrnl.espansoandroid;

import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class EspansoInputMethodService extends InputMethodService {
    private static final int KEY_HEIGHT_DP = 48;

    private final StringBuilder typedBuffer = new StringBuilder();
    private final List<Shortcut> shortcuts = new ArrayList<>();
    private ShortcutDatabase database;
    private LinearLayout keyboardView;
    private boolean shifted;
    private boolean symbolMode;
    private int maximumKeywordLength = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        database = new ShortcutDatabase(this);
    }

    @Override
    public View onCreateInputView() {
        keyboardView = new LinearLayout(this);
        keyboardView.setOrientation(LinearLayout.VERTICAL);
        keyboardView.setPadding(dp(3), dp(4), dp(3), dp(5));
        keyboardView.setBackgroundColor(Color.rgb(226, 232, 240));
        rebuildKeyboard();
        return keyboardView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        typedBuffer.setLength(0);
        loadShortcuts();
    }

    @Override
    public void onFinishInput() {
        typedBuffer.setLength(0);
        super.onFinishInput();
    }

    @Override
    public void onDestroy() {
        database.close();
        super.onDestroy();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.isCtrlPressed() || event.isAltPressed() || event.isMetaPressed()) {
            return super.onKeyDown(keyCode, event);
        }
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            handleBackspace();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            handleEnter();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            handleSpace();
            return true;
        }
        int unicode = event.getUnicodeChar();
        if (unicode != 0) {
            handleCharacter(String.valueOf((char) unicode));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void loadShortcuts() {
        shortcuts.clear();
        shortcuts.addAll(database.getAll());
        shortcuts.sort(
                Comparator.comparingInt((Shortcut shortcut) -> shortcut.getKeyword().length())
                        .reversed()
                        .thenComparingInt(Shortcut::getPosition)
        );
        maximumKeywordLength = 1;
        for (Shortcut shortcut : shortcuts) {
            maximumKeywordLength = Math.max(
                    maximumKeywordLength,
                    shortcut.getKeyword().length()
            );
        }
    }

    private void rebuildKeyboard() {
        if (keyboardView == null) {
            return;
        }
        keyboardView.removeAllViews();
        if (symbolMode) {
            addCharacterRow("1234567890");
            addCharacterRow("!@#$%^&*()");
            addCharacterRow("-_+=/\\:;");
            addCharacterRow("[]{}<>\"'");
        } else {
            addCharacterRow("1234567890");
            addCharacterRow("qwertyuiop");
            addCharacterRow("asdfghjkl");

            LinearLayout letters = createRow();
            letters.addView(createActionButton(shifted ? "SHIFT" : "Shift", view -> {
                shifted = !shifted;
                rebuildKeyboard();
            }), weightedParams(1.5f));
            for (char character : "zxcvbnm".toCharArray()) {
                String text = applyShift(character);
                letters.addView(createCharacterButton(text), weightedParams(1f));
            }
            letters.addView(createActionButton("Del", view -> handleBackspace()),
                    weightedParams(1.5f));
            keyboardView.addView(letters);
        }

        LinearLayout controls = createRow();
        controls.addView(createCharacterButton(";"), weightedParams(1f));
        controls.addView(createActionButton(symbolMode ? "ABC" : "Sym", view -> {
            symbolMode = !symbolMode;
            shifted = false;
            rebuildKeyboard();
        }), weightedParams(1.2f));
        controls.addView(createActionButton("Space", view -> handleSpace()),
                weightedParams(4f));
        controls.addView(createCharacterButton("."), weightedParams(1f));
        controls.addView(createActionButton("Del", view -> handleBackspace()),
                weightedParams(1.2f));
        controls.addView(createActionButton("Enter", view -> handleEnter()),
                weightedParams(1.5f));
        controls.addView(createActionButton("IME", view -> showInputMethodPicker()),
                weightedParams(1.2f));
        keyboardView.addView(controls);
    }

    private void addCharacterRow(String characters) {
        LinearLayout row = createRow();
        Space leftSpacer = new Space(this);
        Space rightSpacer = new Space(this);
        row.addView(leftSpacer, weightedParams(characters.length() == 9 ? 0.5f : 0f));
        for (char character : characters.toCharArray()) {
            row.addView(createCharacterButton(applyShift(character)), weightedParams(1f));
        }
        row.addView(rightSpacer, weightedParams(characters.length() == 9 ? 0.5f : 0f));
        keyboardView.addView(row);
    }

    private LinearLayout createRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(KEY_HEIGHT_DP)
        ));
        return row;
    }

    private Button createCharacterButton(String text) {
        return createActionButton(text, view -> handleCharacter(text));
    }

    private Button createActionButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(14);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams weightedParams(float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                weight
        );
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        return params;
    }

    private String applyShift(char character) {
        String text = String.valueOf(character);
        return shifted ? text.toUpperCase(Locale.getDefault()) : text;
    }

    private void handleCharacter(String text) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        connection.commitText(text, 1);
        appendToBuffer(text);
        expandImmediateShortcut(connection);

        if (shifted && text.length() == 1 && Character.isLetter(text.charAt(0))) {
            shifted = false;
            rebuildKeyboard();
        }
    }

    private void handleSpace() {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        Shortcut match = findMatchingShortcut(true);
        if (match != null) {
            replaceShortcut(connection, match, true);
        } else {
            connection.commitText(" ", 1);
            typedBuffer.setLength(0);
        }
    }

    private void handleBackspace() {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        connection.deleteSurroundingText(1, 0);
        if (typedBuffer.length() > 0) {
            typedBuffer.deleteCharAt(typedBuffer.length() - 1);
        }
    }

    private void handleEnter() {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        typedBuffer.setLength(0);
    }

    private void expandImmediateShortcut(InputConnection connection) {
        Shortcut match = findMatchingShortcut(false);
        if (match != null) {
            replaceShortcut(connection, match, false);
        }
    }

    private Shortcut findMatchingShortcut(boolean afterSpace) {
        String typed = typedBuffer.toString();
        for (Shortcut shortcut : shortcuts) {
            if (shortcut.isReplaceAfterSpace() == afterSpace
                    && typed.endsWith(shortcut.getKeyword())) {
                return shortcut;
            }
        }
        return null;
    }

    private void replaceShortcut(
            InputConnection connection,
            Shortcut shortcut,
            boolean appendSpace
    ) {
        String replacement = TemplateRenderer.render(
                shortcut.getText(),
                new Date(),
                Locale.getDefault()
        );
        connection.beginBatchEdit();
        connection.deleteSurroundingText(shortcut.getKeyword().length(), 0);
        connection.commitText(appendSpace ? replacement + " " : replacement, 1);
        connection.endBatchEdit();
        typedBuffer.setLength(0);
    }

    private void appendToBuffer(String text) {
        typedBuffer.append(text);
        int excess = typedBuffer.length() - maximumKeywordLength;
        if (excess > 0) {
            typedBuffer.delete(0, excess);
        }
    }

    private void showInputMethodPicker() {
        InputMethodManager manager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && shouldOfferSwitchingToNextInputMethod()) {
            switchToNextInputMethod(false);
        } else {
            manager.showInputMethodPicker();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
