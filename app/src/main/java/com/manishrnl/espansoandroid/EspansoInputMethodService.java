package com.manishrnl.espansoandroid;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Build;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class EspansoInputMethodService extends InputMethodService {
    private static final int MODE_LETTERS = 0;
    private static final int MODE_SYMBOLS_PRIMARY = 1;
    private static final int MODE_SYMBOLS_SECONDARY = 2;
    private static final int MODE_EMOJI = 3;
    private static final int MODE_NUMERIC = 4;

    private final StringBuilder typedBuffer = new StringBuilder();
    private final List<Shortcut> shortcuts = new ArrayList<>();
    private final List<TextView> suggestionButtons = new ArrayList<>();

    private ShortcutDatabase database;
    private LinearLayout keyboardView;
    private LinearLayout suggestionRow;
    private EditorInfo editorInfo;
    private KeyboardColors colors;
    private boolean shifted;
    private boolean capsLock;
    private boolean privateField;
    private boolean showNumberRow;
    private boolean showSymbolHints;
    private boolean showSuggestionBar;
    private boolean hapticFeedback;
    private boolean soundFeedback;
    private int keyboardMode = MODE_LETTERS;
    private int keyHeightDp = 50;
    private int maximumKeywordLength = 1;
    private long lastShiftTap;
    private String undoKeyword = "";
    private String undoCommittedText = "";

    @Override
    public void onCreate() {
        super.onCreate();
        database = new ShortcutDatabase(this);
    }

    @Override
    public View onCreateInputView() {
        keyboardView = new LinearLayout(this);
        keyboardView.setOrientation(LinearLayout.VERTICAL);
        loadPreferences();
        rebuildKeyboard();
        return keyboardView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        editorInfo = attribute;
        privateField = isPrivateField(attribute);
        typedBuffer.setLength(0);
        clearExpansionUndo();
        capsLock = false;
        loadPreferences();
        loadShortcuts();
        keyboardMode = isNumericEditor(attribute) ? MODE_NUMERIC : MODE_LETTERS;
        shifted = shouldAutoCapitalize();
        rebuildKeyboard();
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        loadPreferences();
        loadShortcuts();
        rebuildKeyboard();
    }

    @Override
    public void onFinishInput() {
        typedBuffer.setLength(0);
        clearExpansionUndo();
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

    private void loadPreferences() {
        keyHeightDp = AppPreferences.getKeyHeight(this);
        showNumberRow = AppPreferences.isNumberRowEnabled(this);
        showSymbolHints = AppPreferences.areSymbolHintsEnabled(this);
        showSuggestionBar = AppPreferences.isSuggestionBarEnabled(this);
        hapticFeedback = AppPreferences.isHapticFeedbackEnabled(this);
        soundFeedback = AppPreferences.isSoundFeedbackEnabled(this);
        colors = KeyboardColors.from(this);
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
        if (keyboardView == null || colors == null) {
            return;
        }
        keyboardView.removeAllViews();
        suggestionButtons.clear();
        suggestionRow = null;
        keyboardView.setPadding(dp(4), dp(5), dp(4), dp(7));
        keyboardView.setBackgroundColor(colors.background);

        if (showSuggestionBar) {
            addSuggestionToolbar();
        }

        if (keyboardMode == MODE_NUMERIC) {
            addNumericKeyboard();
        } else if (keyboardMode == MODE_EMOJI) {
            addEmojiKeyboard();
        } else if (keyboardMode == MODE_SYMBOLS_PRIMARY) {
            addPrimarySymbols();
        } else if (keyboardMode == MODE_SYMBOLS_SECONDARY) {
            addSecondarySymbols();
        } else {
            addLetterKeyboard();
        }
        addControlRow();
        updateSuggestionBar();
    }

    private void addSuggestionToolbar() {
        suggestionRow = createRow(42);
        suggestionRow.setPadding(dp(1), 0, dp(1), dp(3));
        suggestionRow.addView(
                createIconKey(
                        R.drawable.ic_settings,
                        getString(R.string.settings),
                        view -> openKeyboardSettings(),
                        false
                ),
                weightedParams(0.85f, 42)
        );
        suggestionRow.addView(
                createIconKey(
                        R.drawable.ic_keyboard_left,
                        getString(R.string.cursor_left),
                        view -> moveCursor(KeyEvent.KEYCODE_DPAD_LEFT),
                        false
                ),
                weightedParams(0.55f, 42)
        );
        for (int index = 0; index < 3; index++) {
            TextView suggestion = createActionKey("", null, true);
            suggestion.setTextSize(12);
            suggestion.setSingleLine(true);
            suggestion.setEllipsize(TextUtils.TruncateAt.END);
            suggestionButtons.add(suggestion);
            suggestionRow.addView(suggestion, weightedParams(1.55f, 42));
        }
        suggestionRow.addView(
                createIconKey(
                        R.drawable.ic_keyboard_right,
                        getString(R.string.cursor_right),
                        view -> moveCursor(KeyEvent.KEYCODE_DPAD_RIGHT),
                        false
                ),
                weightedParams(0.55f, 42)
        );
        suggestionRow.addView(
                createIconKey(
                        R.drawable.ic_keyboard_paste,
                        getString(R.string.paste),
                        view -> pasteClipboard(),
                        false
                ),
                weightedParams(0.85f, 42)
        );
        keyboardView.addView(suggestionRow);
    }

    private void addLetterKeyboard() {
        if (showNumberRow) {
            addCharacterRow("1234567890", 0f);
        }
        addLetterRow("qwertyuiop", "1234567890", 0f);
        addLetterRow("asdfghjkl", "@#$&*()-+", 0.48f);

        LinearLayout row = createRow(keyHeightDp);
        row.addView(
                createIconKey(
                        capsLock
                                ? R.drawable.ic_keyboard_caps
                                : R.drawable.ic_keyboard_shift,
                        getString(R.string.shift),
                        view -> handleShift(),
                        shifted
                ),
                weightedParams(1.45f, keyHeightDp)
        );
        String letters = "zxcvbnm";
        String hints = "*\"':;!?";
        for (int index = 0; index < letters.length(); index++) {
            char character = letters.charAt(index);
            row.addView(
                    createLetterKey(
                            applyShift(character),
                            String.valueOf(hints.charAt(index))
                    ),
                    weightedParams(1f, keyHeightDp)
            );
        }
        TextView delete = createIconKey(
                R.drawable.ic_keyboard_backspace,
                getString(R.string.backspace),
                view -> handleBackspace(),
                false
        );
        delete.setOnLongClickListener(view -> {
            provideFeedback(view);
            deleteWord();
            return true;
        });
        row.addView(delete, weightedParams(1.45f, keyHeightDp));
        keyboardView.addView(row);
    }

    private void addPrimarySymbols() {
        addCharacterRow("1234567890", 0f);
        addCharacterKeys(new String[]{"@", "#", "$", "%", "&", "-", "+", "(", ")", "/"});
        addCharacterKeys(new String[]{"*", "\"", "'", ":", ";", "!", "?", "_", "=", "\\"});
        addCharacterKeysWithBackspace(
                new String[]{"[", "]", "{", "}", "<", ">", "^", "`", "~"}
        );
    }

    private void addSecondarySymbols() {
        addCharacterKeys(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"});
        addCharacterKeys(new String[]{"|", "•", "√", "π", "÷", "×", "§", "∆", "£", "€"});
        addCharacterKeys(new String[]{"¥", "¢", "°", "©", "®", "™", "✓", "[", "]", "{"});
        addCharacterKeysWithBackspace(
                new String[]{"}", "<", ">", "^", "`", "~", "_", "=", "\\"}
        );
    }

    private void addEmojiKeyboard() {
        addCharacterKeys(emojiRow(
                0x1F600, 0x1F603, 0x1F604, 0x1F601, 0x1F606,
                0x1F605, 0x1F602, 0x1F923
        ));
        addCharacterKeys(emojiRow(
                0x1F60A, 0x1F607, 0x1F642, 0x1F643, 0x1F609,
                0x1F60C, 0x1F60D, 0x1F970
        ));
        addCharacterKeys(emojiRow(
                0x1F618, 0x1F61C, 0x1F914, 0x1F610, 0x1F612,
                0x1F622, 0x1F62D, 0x1F621
        ));
        addCharacterKeysWithBackspace(emojiRow(
                0x1F44D, 0x1F44F, 0x1F64F, 0x1F4AA, 0x2764,
                0x1F525, 0x2728, 0x1F389
        ));
    }

    private void addNumericKeyboard() {
        addCharacterKeys(new String[]{"1", "2", "3"});
        addCharacterKeys(new String[]{"4", "5", "6"});
        addCharacterKeys(new String[]{"7", "8", "9"});
        addCharacterKeysWithBackspace(new String[]{"-", "0", "."});
    }

    private void addControlRow() {
        LinearLayout row = createRow(keyHeightDp + 2);
        if (keyboardMode == MODE_NUMERIC) {
            row.addView(
                    createActionKey(",", view -> handleCharacter(","), false),
                    weightedParams(1f, keyHeightDp + 2)
            );
            row.addView(
                    createIconKey(
                            R.drawable.ic_keyboard_space,
                            getString(R.string.space),
                            view -> handleSpace(),
                            false
                    ),
                    weightedParams(3f, keyHeightDp + 2)
            );
            row.addView(
                    createIconKey(
                            R.drawable.ic_keyboard_enter,
                            enterLabel(),
                            view -> handleEnter(),
                            true
                    ),
                    weightedParams(1.6f, keyHeightDp + 2)
            );
            keyboardView.addView(row);
            return;
        }

        String modeLabel;
        View.OnClickListener modeAction;
        if (keyboardMode == MODE_LETTERS) {
            modeLabel = "?123";
            modeAction = view -> setKeyboardMode(MODE_SYMBOLS_PRIMARY);
        } else {
            modeLabel = "ABC";
            modeAction = view -> setKeyboardMode(MODE_LETTERS);
        }
        row.addView(
                createActionKey(modeLabel, modeAction, false),
                weightedParams(1.15f, keyHeightDp + 2)
        );

        if (keyboardMode == MODE_SYMBOLS_PRIMARY) {
            row.addView(
                    createActionKey("#+=", view -> setKeyboardMode(
                            MODE_SYMBOLS_SECONDARY
                    ), false),
                    weightedParams(0.9f, keyHeightDp + 2)
            );
        } else if (keyboardMode == MODE_SYMBOLS_SECONDARY) {
            row.addView(
                    createActionKey("123", view -> setKeyboardMode(
                            MODE_SYMBOLS_PRIMARY
                    ), false),
                    weightedParams(0.9f, keyHeightDp + 2)
            );
        } else {
            row.addView(
                    keyboardMode == MODE_EMOJI
                            ? createActionKey(
                                    "?123",
                                    view -> setKeyboardMode(MODE_SYMBOLS_PRIMARY),
                                    false
                            )
                            : createIconKey(
                                    R.drawable.ic_keyboard_emoji,
                                    getString(R.string.emoji),
                                    view -> setKeyboardMode(MODE_EMOJI),
                                    false
                            ),
                    weightedParams(0.9f, keyHeightDp + 2)
            );
        }

        if (keyboardMode == MODE_LETTERS) {
            row.addView(
                    createCharacterKey(","),
                    weightedParams(0.72f, keyHeightDp + 2)
            );
        }
        if (!showSuggestionBar) {
            row.addView(
                    createIconKey(
                            R.drawable.ic_settings,
                            getString(R.string.settings),
                            view -> openKeyboardSettings(),
                            false
                    ),
                    weightedParams(0.9f, keyHeightDp + 2)
            );
        }

        TextView space = createIconKey(
                R.drawable.ic_keyboard_space,
                getString(R.string.space),
                view -> handleSpace(),
                false
        );
        space.setOnLongClickListener(view -> {
            provideFeedback(view);
            showInputMethodPicker();
            return true;
        });
        row.addView(space, weightedParams(3.7f, keyHeightDp + 2));
        row.addView(
                createCharacterKey("."),
                weightedParams(0.8f, keyHeightDp + 2)
        );
        row.addView(
                createIconKey(
                        R.drawable.ic_keyboard_enter,
                        enterLabel(),
                        view -> handleEnter(),
                        true
                ),
                weightedParams(1.45f, keyHeightDp + 2)
        );
        keyboardView.addView(row);
    }

    private void addCharacterRow(String characters, float sideWeight) {
        LinearLayout row = createRow(keyHeightDp);
        if (sideWeight > 0) {
            row.addView(new Space(this), weightedParams(sideWeight, keyHeightDp));
        }
        for (char character : characters.toCharArray()) {
            row.addView(
                    createCharacterKey(applyShift(character)),
                    weightedParams(1f, keyHeightDp)
            );
        }
        if (sideWeight > 0) {
            row.addView(new Space(this), weightedParams(sideWeight, keyHeightDp));
        }
        keyboardView.addView(row);
    }

    private void addCharacterKeys(String[] labels) {
        LinearLayout row = createRow(keyHeightDp);
        for (String label : labels) {
            TextView key = createCharacterKey(label);
            if (keyboardMode == MODE_EMOJI) {
                key.setTextSize(20);
            }
            row.addView(key, weightedParams(1f, keyHeightDp));
        }
        keyboardView.addView(row);
    }

    private void addCharacterKeysWithBackspace(String[] labels) {
        LinearLayout row = createRow(keyHeightDp);
        for (String label : labels) {
            TextView key = createCharacterKey(label);
            if (keyboardMode == MODE_EMOJI) {
                key.setTextSize(20);
            }
            row.addView(key, weightedParams(1f, keyHeightDp));
        }
        TextView delete = createIconKey(
                R.drawable.ic_keyboard_backspace,
                getString(R.string.backspace),
                view -> handleBackspace(),
                false
        );
        delete.setOnLongClickListener(view -> {
            provideFeedback(view);
            deleteWord();
            return true;
        });
        row.addView(delete, weightedParams(1.35f, keyHeightDp));
        keyboardView.addView(row);
    }

    private void addLetterRow(String letters, String hints, float sideWeight) {
        LinearLayout row = createRow(keyHeightDp);
        if (sideWeight > 0) {
            row.addView(new Space(this), weightedParams(sideWeight, keyHeightDp));
        }
        for (int index = 0; index < letters.length(); index++) {
            row.addView(
                    createLetterKey(
                            applyShift(letters.charAt(index)),
                            String.valueOf(hints.charAt(index))
                    ),
                    weightedParams(1f, keyHeightDp)
            );
        }
        if (sideWeight > 0) {
            row.addView(new Space(this), weightedParams(sideWeight, keyHeightDp));
        }
        keyboardView.addView(row);
    }

    private LinearLayout createRow(int heightDp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(heightDp)
        ));
        return row;
    }

    private TextView createCharacterKey(String text) {
        return createActionKey(text, view -> handleCharacter(text), false);
    }

    private TextView createLetterKey(String letter, String symbolHint) {
        TextView key = createCharacterKey(letter);
        if (showSymbolHints && !symbolHint.isEmpty()) {
            SpannableString label = new SpannableString(letter + symbolHint);
            int hintStart = letter.length();
            label.setSpan(
                    new RelativeSizeSpan(0.55f),
                    hintStart,
                    label.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            label.setSpan(
                    new SuperscriptSpan(),
                    hintStart,
                    label.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            label.setSpan(
                    new ForegroundColorSpan(colors.hint),
                    hintStart,
                    label.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            key.setText(label);
            key.setOnLongClickListener(view -> {
                provideFeedback(view);
                handleCharacter(symbolHint);
                return true;
            });
        }
        return key;
    }

    private TextView createIconKey(
            int drawableResource,
            String description,
            View.OnClickListener listener,
            boolean accent
    ) {
        TextView key = createActionKey("", listener, accent);
        Drawable icon = getDrawable(drawableResource);
        if (icon != null) {
            int size = dp(23);
            icon.setBounds(0, 0, size, size);
            icon.setTint(accent ? colors.onAccent : colors.text);
            key.setCompoundDrawables(icon, null, null, null);
        }
        key.setContentDescription(description);
        return key;
    }

    private TextView createActionKey(
            String text,
            View.OnClickListener listener,
            boolean accent
    ) {
        TextView key = new TextView(this);
        key.setGravity(Gravity.CENTER);
        key.setText(text);
        key.setTextColor(accent ? colors.onAccent : colors.text);
        key.setTextSize(18);
        key.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        key.setMaxLines(1);
        key.setPadding(dp(2), 0, dp(2), 0);
        key.setBackground(keyBackground(accent));
        key.setElevation(dp(1));
        key.setClickable(listener != null);
        if (listener != null) {
            key.setOnClickListener(view -> {
                provideFeedback(view);
                listener.onClick(view);
            });
        }
        return key;
    }

    private RippleDrawable keyBackground(boolean accent) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(accent ? colors.accent : colors.key);
        shape.setCornerRadius(dp(8));
        shape.setStroke(dp(1), accent ? colors.accent : colors.keyBorder);
        int rippleColor = Color.argb(45, 255, 255, 255);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), shape, null);
    }

    private LinearLayout.LayoutParams weightedParams(float weight, int heightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(heightDp),
                weight
        );
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private void provideFeedback(View view) {
        if (hapticFeedback) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        if (soundFeedback) {
            AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
            manager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.35f);
        }
    }

    private void handleShift() {
        long now = System.currentTimeMillis();
        if (capsLock) {
            capsLock = false;
            shifted = false;
        } else if (now - lastShiftTap < 420) {
            capsLock = true;
            shifted = true;
        } else {
            shifted = !shifted;
        }
        lastShiftTap = now;
        rebuildKeyboard();
    }

    private String applyShift(char character) {
        String text = String.valueOf(character);
        return shifted ? text.toUpperCase(Locale.getDefault()) : text;
    }

    private void setKeyboardMode(int mode) {
        keyboardMode = mode;
        if (mode != MODE_LETTERS) {
            capsLock = false;
            shifted = false;
        } else {
            shifted = shouldAutoCapitalize();
        }
        rebuildKeyboard();
    }

    private void handleCharacter(String text) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        clearExpansionUndo();
        connection.commitText(text, 1);
        appendToBuffer(text);
        expandImmediateShortcut(connection);

        if (shifted && !capsLock && isSingleLetter(text)) {
            shifted = false;
            rebuildKeyboard();
        } else {
            updateSuggestionBar();
        }
    }

    private void handleSpace() {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        clearExpansionUndo();
        Shortcut match = findMatchingShortcut(true);
        if (match != null) {
            replaceShortcut(connection, match, true);
        } else {
            connection.commitText(" ", 1);
            typedBuffer.setLength(0);
        }
        if (!capsLock && keyboardMode == MODE_LETTERS) {
            boolean shouldShift = shouldAutoCapitalize();
            if (shifted != shouldShift) {
                shifted = shouldShift;
                rebuildKeyboard();
                return;
            }
        }
        updateSuggestionBar();
    }

    private void handleBackspace() {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        if (undoLastExpansion(connection)) {
            return;
        }
        connection.deleteSurroundingText(1, 0);
        if (typedBuffer.length() > 0) {
            typedBuffer.deleteCharAt(typedBuffer.length() - 1);
        }
        updateSuggestionBar();
    }

    private void deleteWord() {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        clearExpansionUndo();
        CharSequence before = connection.getTextBeforeCursor(64, 0);
        if (before == null || before.length() == 0) {
            return;
        }
        int count = 0;
        boolean foundText = false;
        for (int index = before.length() - 1; index >= 0; index--) {
            char character = before.charAt(index);
            if (Character.isWhitespace(character)) {
                if (foundText) {
                    break;
                }
            } else {
                foundText = true;
            }
            count++;
        }
        connection.deleteSurroundingText(Math.max(1, count), 0);
        typedBuffer.setLength(0);
        updateSuggestionBar();
    }

    private void handleEnter() {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        clearExpansionUndo();
        int action = editorInfo == null
                ? EditorInfo.IME_ACTION_NONE
                : editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean useAction = editorInfo != null
                && (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
                && action != EditorInfo.IME_ACTION_NONE
                && action != EditorInfo.IME_ACTION_UNSPECIFIED;
        if (useAction) {
            connection.performEditorAction(action);
        } else {
            connection.sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            );
            connection.sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
            );
        }
        typedBuffer.setLength(0);
        if (!capsLock && keyboardMode == MODE_LETTERS) {
            shifted = true;
            rebuildKeyboard();
        } else {
            updateSuggestionBar();
        }
    }

    private String enterLabel() {
        if (editorInfo == null) {
            return "Enter";
        }
        int action = editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION;
        if (action == EditorInfo.IME_ACTION_GO) {
            return "Go";
        }
        if (action == EditorInfo.IME_ACTION_SEARCH) {
            return "Search";
        }
        if (action == EditorInfo.IME_ACTION_SEND) {
            return "Send";
        }
        if (action == EditorInfo.IME_ACTION_NEXT) {
            return "Next";
        }
        if (action == EditorInfo.IME_ACTION_DONE) {
            return "Done";
        }
        return "Enter";
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
        String replacement = render(shortcut);
        String committed = appendSpace ? replacement + " " : replacement;
        connection.beginBatchEdit();
        connection.deleteSurroundingText(shortcut.getKeyword().length(), 0);
        connection.commitText(committed, 1);
        connection.endBatchEdit();
        undoKeyword = shortcut.getKeyword();
        undoCommittedText = committed;
        typedBuffer.setLength(0);
        updateSuggestionBar();
    }

    private boolean undoLastExpansion(InputConnection connection) {
        if (undoCommittedText.isEmpty()) {
            return false;
        }
        CharSequence before = connection.getTextBeforeCursor(
                undoCommittedText.length(),
                0
        );
        if (before == null || !before.toString().endsWith(undoCommittedText)) {
            clearExpansionUndo();
            return false;
        }
        connection.beginBatchEdit();
        connection.deleteSurroundingText(undoCommittedText.length(), 0);
        connection.commitText(undoKeyword, 1);
        connection.endBatchEdit();
        typedBuffer.setLength(0);
        appendToBuffer(undoKeyword);
        clearExpansionUndo();
        updateSuggestionBar();
        return true;
    }

    private void clearExpansionUndo() {
        undoKeyword = "";
        undoCommittedText = "";
    }

    private String render(Shortcut shortcut) {
        return TemplateRenderer.render(
                shortcut.getText(),
                new Date(),
                Locale.getDefault()
        );
    }

    private void appendToBuffer(String text) {
        typedBuffer.append(text);
        int excess = typedBuffer.length() - maximumKeywordLength;
        if (excess > 0) {
            typedBuffer.delete(0, excess);
        }
    }

    private void updateSuggestionBar() {
        if (suggestionButtons.isEmpty()) {
            return;
        }
        if (privateField) {
            configureSuggestion(suggestionButtons.get(0), "Private field", null, "");
            configureSuggestion(suggestionButtons.get(1), "", null, "");
            configureSuggestion(suggestionButtons.get(2), "", null, "");
            return;
        }

        String fragment = findSuggestionFragment();
        List<Shortcut> matches = new ArrayList<>();
        for (Shortcut shortcut : shortcuts) {
            if (fragment.isEmpty() || shortcut.getKeyword().startsWith(fragment)) {
                matches.add(shortcut);
            }
            if (matches.size() == suggestionButtons.size()) {
                break;
            }
        }
        for (int index = 0; index < suggestionButtons.size(); index++) {
            if (index < matches.size()) {
                Shortcut shortcut = matches.get(index);
                String preview = shortcut.getText().replace('\n', ' ').trim();
                if (preview.length() > 18) {
                    preview = preview.substring(0, 18) + "...";
                }
                configureSuggestion(
                        suggestionButtons.get(index),
                        shortcut.getKeyword() + "  " + preview,
                        shortcut,
                        fragment
                );
            } else {
                configureSuggestion(suggestionButtons.get(index), "", null, fragment);
            }
        }
    }

    private void configureSuggestion(
            TextView view,
            String label,
            Shortcut shortcut,
            String fragment
    ) {
        view.setText(label);
        view.setClickable(shortcut != null);
        view.setAlpha(label.isEmpty() ? 0.35f : 1f);
        if (shortcut == null) {
            view.setOnClickListener(null);
            return;
        }
        view.setOnClickListener(clicked -> {
            provideFeedback(clicked);
            applySuggestedShortcut(shortcut, fragment);
        });
    }

    private String findSuggestionFragment() {
        String typed = typedBuffer.toString();
        if (typed.isEmpty()) {
            return "";
        }
        for (int start = 0; start < typed.length(); start++) {
            String candidate = typed.substring(start);
            for (Shortcut shortcut : shortcuts) {
                if (shortcut.getKeyword().startsWith(candidate)) {
                    return candidate;
                }
            }
        }
        return typed;
    }

    private void applySuggestedShortcut(Shortcut shortcut, String fragment) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        String replacement = render(shortcut);
        connection.beginBatchEdit();
        if (!fragment.isEmpty()) {
            connection.deleteSurroundingText(fragment.length(), 0);
        }
        connection.commitText(replacement, 1);
        connection.endBatchEdit();
        undoKeyword = fragment;
        undoCommittedText = replacement;
        typedBuffer.setLength(0);
        updateSuggestionBar();
    }

    private void pasteClipboard() {
        if (privateField) {
            return;
        }
        clearExpansionUndo();
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (!clipboard.hasPrimaryClip()) {
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        InputConnection connection = getCurrentInputConnection();
        if (connection != null && !TextUtils.isEmpty(text)) {
            connection.commitText(text, 1);
            typedBuffer.setLength(0);
            updateSuggestionBar();
        }
    }

    private void moveCursor(int keyCode) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) {
            return;
        }
        clearExpansionUndo();
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        typedBuffer.setLength(0);
        updateSuggestionBar();
    }

    private void openKeyboardSettings() {
        Intent intent = new Intent(this, KeyboardSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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

    private boolean shouldAutoCapitalize() {
        InputConnection connection = getCurrentInputConnection();
        return editorInfo != null
                && !privateField
                && (editorInfo.inputType & InputType.TYPE_MASK_CLASS)
                == InputType.TYPE_CLASS_TEXT
                && connection != null
                && connection.getCursorCapsMode(editorInfo.inputType) != 0;
    }

    private static boolean isSingleLetter(String text) {
        return text.length() == 1 && Character.isLetter(text.charAt(0));
    }

    private static boolean isNumericEditor(EditorInfo info) {
        if (info == null) {
            return false;
        }
        int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;
        return inputClass == InputType.TYPE_CLASS_NUMBER
                || inputClass == InputType.TYPE_CLASS_PHONE
                || inputClass == InputType.TYPE_CLASS_DATETIME;
    }

    private static boolean isPrivateField(EditorInfo info) {
        if (info == null) {
            return false;
        }
        int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        if (inputClass == InputType.TYPE_CLASS_NUMBER) {
            return variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        }
        return inputClass == InputType.TYPE_CLASS_TEXT
                && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
    }

    private static String[] emojiRow(int... codePoints) {
        String[] labels = new String[codePoints.length];
        for (int index = 0; index < codePoints.length; index++) {
            labels[index] = new String(Character.toChars(codePoints[index]));
        }
        return labels;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class KeyboardColors {
        final int background;
        final int key;
        final int keyBorder;
        final int text;
        final int hint;
        final int accent;
        final int onAccent;

        KeyboardColors(
                int background,
                int key,
                int keyBorder,
                int text,
                int hint,
                int accent,
                int onAccent
        ) {
            this.background = background;
            this.key = key;
            this.keyBorder = keyBorder;
            this.text = text;
            this.hint = hint;
            this.accent = accent;
            this.onAccent = onAccent;
        }

        static KeyboardColors from(Context context) {
            boolean dark = AppPreferences.isKeyboardDark(context);
            boolean oled = AppPreferences.isKeyboardOled(context);
            if (dark) {
                return new KeyboardColors(
                        oled ? Color.BLACK : Color.rgb(24, 25, 31),
                        oled ? Color.rgb(20, 20, 20) : Color.rgb(45, 46, 56),
                        oled ? Color.rgb(48, 48, 48) : Color.rgb(62, 63, 75),
                        Color.rgb(246, 246, 250),
                        Color.rgb(155, 157, 170),
                        Color.rgb(108, 92, 231),
                        Color.WHITE
                );
            }
            return new KeyboardColors(
                    Color.rgb(232, 233, 240),
                    Color.WHITE,
                    Color.rgb(211, 213, 224),
                    Color.rgb(31, 32, 40),
                    Color.rgb(105, 108, 122),
                    Color.rgb(108, 92, 231),
                    Color.WHITE
            );
        }
    }
}
