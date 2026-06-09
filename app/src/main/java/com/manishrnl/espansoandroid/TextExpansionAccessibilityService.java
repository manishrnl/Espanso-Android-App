package com.manishrnl.espansoandroid;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.InputMethod;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.SurroundingText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressLint("UseRequiresApi")
public final class TextExpansionAccessibilityService extends AccessibilityService {
    private static final int MAX_SUGGESTIONS = 3;
    private static final int DIRECT_CONTEXT_LENGTH = 64;
    private static final long EXPECTED_EVENT_TIMEOUT_MS = 1500;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Shortcut> shortcuts = new ArrayList<>();
    private final List<TextView> suggestionViews = new ArrayList<>();

    private ShortcutDatabase database;
    private WindowManager windowManager;
    private LinearLayout suggestionOverlay;
    private WindowManager.LayoutParams overlayParams;
    private boolean overlayAttached;
    private int maximumKeywordLength = 1;

    private String activeFieldKey = "";
    private String expectedFieldKey = "";
    private String expectedText = "";
    private long expectedUntil;
    private LegacyExpansionRecord lastLegacyExpansion;

    private DirectInputMethod directInputMethod;
    private boolean directInputActive;
    private boolean directPrivateField;
    private DirectExpansionRecord lastDirectExpansion;
    private String suppressedDirectSuffix = "";
    private long suppressedDirectUntil;

    private final Runnable directInputProcessor = this::processDirectInput;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (database == null) {
            database = new ShortcutDatabase(this);
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildSuggestionOverlay();
        loadShortcuts();
    }

    @Override
    @TargetApi(33)
    public InputMethod onCreateInputMethod() {
        directInputMethod = new DirectInputMethod(this);
        return directInputMethod;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        CharSequence packageName = event.getPackageName();
        if (packageName != null && getPackageName().contentEquals(packageName)) {
            hideSuggestions();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && directInputActive) {
            scheduleDirectInputProcessing();
            return;
        }
        processLegacyEvent(event);
    }

    @Override
    public void onInterrupt() {
        hideSuggestions();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(directInputProcessor);
        hideSuggestions();
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }

    private void loadShortcuts() {
        if (database == null) {
            return;
        }
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

    private void processLegacyEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo node = editableNode(event);
        if (event.isPassword() || (node != null && node.isPassword())) {
            hideSuggestions();
            return;
        }

        String text = readEventText(event, node);
        if (text == null) {
            hideSuggestions();
            return;
        }
        int cursor = resolveCursor(node, event, text.length());
        String fieldKey = node == null ? legacyEventKey(event) : fieldKey(node);
        activeFieldKey = fieldKey;

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            loadShortcuts();
            lastLegacyExpansion = null;
            updateSuggestions(node, text, cursor, false);
            return;
        }
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                && event.getEventType()
                != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            return;
        }

        long now = System.currentTimeMillis();
        if (fieldKey.equals(expectedFieldKey)
                && text.equals(expectedText)
                && now <= expectedUntil) {
            clearExpectedEvent();
            updateSuggestions(node, text, cursor, false);
            return;
        }
        clearExpectedEvent();

        if (lastLegacyExpansion != null
                && lastLegacyExpansion.fieldKey.equals(fieldKey)) {
            if (text.equals(lastLegacyExpansion.afterText)) {
                updateSuggestions(node, text, cursor, false);
                return;
            }
            if (text.equals(lastLegacyExpansion.afterSingleBackspaceText)
                    && node != null) {
                LegacyExpansionRecord record = lastLegacyExpansion;
                lastLegacyExpansion = null;
                setNodeText(node, fieldKey, record.restoreText, record.restoreCursor);
                return;
            }
            lastLegacyExpansion = null;
        }

        int selectionStart = node == null ? cursor : node.getTextSelectionStart();
        if (node != null
                && cursor >= 0
                && (selectionStart < 0 || selectionStart == cursor)) {
            ExpansionMatcher.Match match =
                    ExpansionMatcher.findExpansion(shortcuts, text, cursor);
            if (match != null) {
                applyLegacyExpansion(node, fieldKey, text, match);
                return;
            }
        }
        updateSuggestions(node, text, cursor, false);
    }

    private AccessibilityNodeInfo editableNode(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (isEditableTextNode(source)) {
            return source;
        }
        return focusedEditableNode();
    }

    private AccessibilityNodeInfo focusedEditableNode() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return null;
        }
        AccessibilityNodeInfo focused =
                root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        return isEditableTextNode(focused) ? focused : null;
    }

    private static boolean isEditableTextNode(AccessibilityNodeInfo node) {
        return node != null && node.isEditable() && !node.isPassword();
    }

    private static boolean supportsSetText(AccessibilityNodeInfo node) {
        return node != null
                && (node.getActions() & AccessibilityNodeInfo.ACTION_SET_TEXT) != 0;
    }

    private static String readEventText(
            AccessibilityEvent event,
            AccessibilityNodeInfo node
    ) {
        if (node != null && node.getText() != null) {
            return node.getText().toString();
        }
        List<CharSequence> eventText = event.getText();
        if (eventText == null || eventText.isEmpty()) {
            return null;
        }
        CharSequence value = eventText.get(eventText.size() - 1);
        return value == null ? null : value.toString();
    }

    private static int resolveCursor(
            AccessibilityNodeInfo node,
            AccessibilityEvent event,
            int textLength
    ) {
        int cursor = node == null ? -1 : node.getTextSelectionEnd();
        if (cursor < 0 || cursor > textLength) {
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                cursor = event.getFromIndex() + event.getAddedCount();
            } else {
                cursor = event.getToIndex();
            }
        }
        if (cursor < 0 || cursor > textLength) {
            cursor = textLength;
        }
        return cursor;
    }

    private static String legacyEventKey(AccessibilityEvent event) {
        return event.getWindowId()
                + "|"
                + String.valueOf(event.getPackageName())
                + "|"
                + String.valueOf(event.getClassName());
    }

    private static String fieldKey(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return node.getWindowId()
                + "|"
                + String.valueOf(node.getViewIdResourceName())
                + "|"
                + String.valueOf(node.getClassName())
                + "|"
                + bounds.flattenToString();
    }

    private void applyLegacyExpansion(
            AccessibilityNodeInfo node,
            String fieldKey,
            String currentText,
            ExpansionMatcher.Match match
    ) {
        String rendered = render(match.shortcut);
        String committed = match.appendSpace ? rendered + " " : rendered;
        String afterText = ExpansionMatcher.replace(
                currentText,
                match.start,
                match.end,
                committed
        );
        int afterCursor = match.start + committed.length();
        String restoreText = ExpansionMatcher.replace(
                currentText,
                match.start,
                match.end,
                match.shortcut.getKeyword()
        );
        int restoreCursor = match.start + match.shortcut.getKeyword().length();

        lastLegacyExpansion = new LegacyExpansionRecord(
                fieldKey,
                afterText,
                ExpansionMatcher.removeCodePointBefore(afterText, afterCursor),
                restoreText,
                restoreCursor
        );
        if (!setNodeText(node, fieldKey, afterText, afterCursor)) {
            lastLegacyExpansion = null;
            updateSuggestions(node, currentText, match.end, false);
        }
    }

    private boolean setNodeText(
            AccessibilityNodeInfo node,
            String fieldKey,
            String text,
            int cursor
    ) {
        if (!supportsSetText(node)) {
            return false;
        }
        Bundle textArguments = new Bundle();
        textArguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
        );
        expectedFieldKey = fieldKey;
        expectedText = text;
        expectedUntil = System.currentTimeMillis() + EXPECTED_EVENT_TIMEOUT_MS;
        boolean changed = node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                textArguments
        );
        if (!changed) {
            clearExpectedEvent();
            return false;
        }

        Bundle selectionArguments = new Bundle();
        selectionArguments.putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                cursor
        );
        selectionArguments.putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                cursor
        );
        node.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION,
                selectionArguments
        );
        hideSuggestions();
        return true;
    }

    private void clearExpectedEvent() {
        expectedFieldKey = "";
        expectedText = "";
        expectedUntil = 0;
    }

    @TargetApi(33)
    private void scheduleDirectInputProcessing() {
        handler.removeCallbacks(directInputProcessor);
        handler.postDelayed(directInputProcessor, 35);
    }

    @TargetApi(33)
    private void processDirectInput() {
        if (!directInputActive || directPrivateField || directInputMethod == null) {
            hideSuggestions();
            return;
        }
        InputMethod.AccessibilityInputConnection connection =
                directInputMethod.getCurrentInputConnection();
        if (connection == null) {
            hideSuggestions();
            return;
        }

        int requestedBefore = 4096;
        if (lastDirectExpansion != null) {
            requestedBefore = Math.min(
                    32768,
                    Math.max(
                            requestedBefore,
                            lastDirectExpansion.remainingAfterBackspaceLength
                                    + DIRECT_CONTEXT_LENGTH
                                    + 32
                    )
            );
        }
        SurroundingText surrounding =
                connection.getSurroundingText(requestedBefore, 128, 0);
        if (surrounding == null) {
            hideSuggestions();
            return;
        }

        String text = surrounding.getText().toString();
        int selectionStart = surrounding.getSelectionStart();
        int selectionEnd = surrounding.getSelectionEnd();
        if (selectionStart < 0
                || selectionEnd < 0
                || selectionStart > text.length()
                || selectionEnd > text.length()
                || selectionStart != selectionEnd) {
            hideSuggestions();
            return;
        }

        String beforeCursor = text.substring(0, selectionStart);
        long now = System.currentTimeMillis();
        if (!suppressedDirectSuffix.isEmpty()
                && now <= suppressedDirectUntil
                && beforeCursor.endsWith(suppressedDirectSuffix)) {
            hideSuggestions();
            return;
        }
        if (now > suppressedDirectUntil) {
            suppressedDirectSuffix = "";
        }

        if (lastDirectExpansion != null) {
            if (beforeCursor.endsWith(lastDirectExpansion.afterSuffix)) {
                hideSuggestions();
                return;
            }
            if (beforeCursor.endsWith(lastDirectExpansion.afterBackspaceSuffix)) {
                connection.deleteSurroundingText(
                        lastDirectExpansion.remainingAfterBackspaceLength,
                        0
                );
                connection.commitText(
                        lastDirectExpansion.restoreKeyword,
                        1,
                        null
                );
                suppressedDirectSuffix = lastDirectExpansion.context
                        + lastDirectExpansion.restoreKeyword;
                suppressedDirectUntil = now + EXPECTED_EVENT_TIMEOUT_MS;
                lastDirectExpansion = null;
                hideSuggestions();
                return;
            }
            lastDirectExpansion = null;
        }

        ExpansionMatcher.Match match = ExpansionMatcher.findExpansion(
                shortcuts,
                beforeCursor,
                beforeCursor.length()
        );
        if (match != null) {
            String rendered = render(match.shortcut);
            String committed = match.appendSpace ? rendered + " " : rendered;
            String context = directContext(beforeCursor, match.start);
            connection.deleteSurroundingText(match.end - match.start, 0);
            connection.commitText(committed, 1, null);
            lastDirectExpansion = directExpansionRecord(
                    context,
                    committed,
                    match.shortcut.getKeyword()
            );
            hideSuggestions();
            return;
        }

        updateSuggestions(
                focusedEditableNode(),
                beforeCursor,
                beforeCursor.length(),
                true
        );
    }

    private void updateSuggestions(
            AccessibilityNodeInfo node,
            String text,
            int cursor,
            boolean direct
    ) {
        String fragment = ExpansionMatcher.findSuggestionFragment(
                shortcuts,
                text,
                cursor,
                maximumKeywordLength
        );
        if (fragment.isEmpty()) {
            hideSuggestions();
            return;
        }

        List<Shortcut> matches = new ArrayList<>();
        for (Shortcut shortcut : shortcuts) {
            if (shortcut.getKeyword().startsWith(fragment)) {
                matches.add(shortcut);
            }
            if (matches.size() == MAX_SUGGESTIONS) {
                break;
            }
        }
        if (matches.isEmpty()) {
            hideSuggestions();
            return;
        }

        for (int index = 0; index < suggestionViews.size(); index++) {
            TextView view = suggestionViews.get(index);
            if (index >= matches.size()) {
                view.setVisibility(View.GONE);
                continue;
            }
            Shortcut shortcut = matches.get(index);
            String preview = shortcut.getText().replace('\n', ' ').trim();
            if (preview.length() > 26) {
                preview = preview.substring(0, 26) + "...";
            }
            view.setText(getString(
                    R.string.suggestion_label,
                    shortcut.getKeyword(),
                    preview
            ));
            view.setVisibility(View.VISIBLE);
            if (direct && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                view.setOnClickListener(clicked ->
                        applyDirectSuggestion(shortcut, fragment));
            } else {
                view.setOnClickListener(clicked ->
                        applyLegacySuggestion(shortcut, fragment));
            }
        }
        positionAndShowOverlay(node);
    }

    @TargetApi(33)
    private void applyDirectSuggestion(Shortcut shortcut, String expectedFragment) {
        if (directInputMethod == null) {
            hideSuggestions();
            return;
        }
        InputMethod.AccessibilityInputConnection connection =
                directInputMethod.getCurrentInputConnection();
        if (connection == null) {
            hideSuggestions();
            return;
        }
        SurroundingText surrounding = connection.getSurroundingText(
                Math.max(4096, expectedFragment.length() + DIRECT_CONTEXT_LENGTH),
                0,
                0
        );
        if (surrounding == null
                || surrounding.getSelectionStart() != surrounding.getSelectionEnd()) {
            hideSuggestions();
            return;
        }

        String text = surrounding.getText().toString();
        int cursor = surrounding.getSelectionStart();
        if (cursor < expectedFragment.length()
                || cursor > text.length()
                || !text.substring(0, cursor).endsWith(expectedFragment)) {
            hideSuggestions();
            return;
        }

        String beforeCursor = text.substring(0, cursor);
        int start = cursor - expectedFragment.length();
        String context = directContext(beforeCursor, start);
        String rendered = render(shortcut);
        connection.deleteSurroundingText(expectedFragment.length(), 0);
        connection.commitText(rendered, 1, null);
        lastDirectExpansion = directExpansionRecord(
                context,
                rendered,
                expectedFragment
        );
        hideSuggestions();
    }

    private void applyLegacySuggestion(Shortcut shortcut, String fragment) {
        AccessibilityNodeInfo node = focusedEditableNode();
        if (node == null || node.getText() == null || !supportsSetText(node)) {
            hideSuggestions();
            return;
        }

        String fieldKey = fieldKey(node);
        String text = node.getText().toString();
        int cursor = node.getTextSelectionEnd();
        if (!fieldKey.equals(activeFieldKey)
                || cursor < fragment.length()
                || cursor > text.length()
                || !text.substring(0, cursor).endsWith(fragment)) {
            hideSuggestions();
            return;
        }

        String rendered = render(shortcut);
        int start = cursor - fragment.length();
        String afterText = ExpansionMatcher.replace(text, start, cursor, rendered);
        int afterCursor = start + rendered.length();
        lastLegacyExpansion = new LegacyExpansionRecord(
                fieldKey,
                afterText,
                ExpansionMatcher.removeCodePointBefore(afterText, afterCursor),
                text,
                cursor
        );
        if (!setNodeText(node, fieldKey, afterText, afterCursor)) {
            lastLegacyExpansion = null;
        }
    }

    private String render(Shortcut shortcut) {
        return TemplateRenderer.render(
                shortcut.getText(),
                new Date(),
                Locale.getDefault()
        );
    }

    private static String directContext(String beforeCursor, int replacementStart) {
        int contextStart = Math.max(0, replacementStart - DIRECT_CONTEXT_LENGTH);
        return beforeCursor.substring(contextStart, replacementStart);
    }

    private static DirectExpansionRecord directExpansionRecord(
            String context,
            String committed,
            String restoreKeyword
    ) {
        String afterBackspace = ExpansionMatcher.removeCodePointBefore(
                committed,
                committed.length()
        );
        return new DirectExpansionRecord(
                context,
                context + committed,
                context + afterBackspace,
                afterBackspace.length(),
                restoreKeyword
        );
    }

    private void buildSuggestionOverlay() {
        suggestionViews.clear();
        suggestionOverlay = new LinearLayout(this);
        suggestionOverlay.setOrientation(LinearLayout.HORIZONTAL);
        suggestionOverlay.setPadding(dp(6), dp(6), dp(6), dp(6));
        suggestionOverlay.setElevation(dp(12));

        GradientDrawable background = new GradientDrawable();
        background.setColor(AppPreferences.isAppDark(this)
                ? Color.rgb(31, 32, 40)
                : Color.WHITE);
        background.setCornerRadius(dp(14));
        background.setStroke(
                dp(1),
                AppPreferences.isAppDark(this)
                        ? Color.rgb(76, 77, 92)
                        : Color.rgb(218, 219, 230)
        );
        suggestionOverlay.setBackground(background);

        for (int index = 0; index < MAX_SUGGESTIONS; index++) {
            TextView suggestion = new TextView(this);
            suggestion.setGravity(Gravity.CENTER_VERTICAL);
            suggestion.setMaxLines(2);
            suggestion.setEllipsize(TextUtils.TruncateAt.END);
            suggestion.setPadding(dp(11), dp(6), dp(11), dp(6));
            suggestion.setTextSize(12);
            suggestion.setTypeface(
                    Typeface.create("sans-serif-medium", Typeface.NORMAL)
            );
            suggestion.setTextColor(AppPreferences.isAppDark(this)
                    ? Color.WHITE
                    : Color.rgb(31, 32, 40));

            GradientDrawable itemBackground = new GradientDrawable();
            itemBackground.setColor(AppPreferences.isAppDark(this)
                    ? Color.rgb(45, 46, 56)
                    : Color.rgb(246, 247, 251));
            itemBackground.setCornerRadius(dp(10));
            suggestion.setBackground(itemBackground);

            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    0,
                    dp(56),
                    1
            );
            itemParams.setMargins(dp(3), 0, dp(3), 0);
            suggestionOverlay.addView(suggestion, itemParams);
            suggestionViews.add(suggestion);
        }

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayParams.x = dp(8);
        overlayParams.width = getResources().getDisplayMetrics().widthPixels - dp(16);
    }

    private void positionAndShowOverlay(AccessibilityNodeInfo node) {
        if (windowManager == null || suggestionOverlay == null) {
            return;
        }

        int overlayHeight = dp(68);
        Rect keyboardBounds = inputMethodBounds();
        if (keyboardBounds != null && keyboardBounds.top > overlayHeight) {
            overlayParams.y = keyboardBounds.top - overlayHeight - dp(6);
        } else if (node != null) {
            Rect fieldBounds = new Rect();
            node.getBoundsInScreen(fieldBounds);
            overlayParams.y = fieldBounds.top >= overlayHeight + dp(8)
                    ? fieldBounds.top - overlayHeight - dp(6)
                    : fieldBounds.bottom + dp(6);
        } else {
            overlayParams.y = Math.max(
                    dp(8),
                    getResources().getDisplayMetrics().heightPixels - dp(380)
            );
        }

        try {
            if (overlayAttached) {
                windowManager.updateViewLayout(suggestionOverlay, overlayParams);
            } else {
                windowManager.addView(suggestionOverlay, overlayParams);
                overlayAttached = true;
            }
        } catch (RuntimeException ignored) {
            overlayAttached = false;
        }
    }

    private Rect inputMethodBounds() {
        for (AccessibilityWindowInfo window : getWindows()) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                Rect bounds = new Rect();
                window.getBoundsInScreen(bounds);
                return bounds;
            }
        }
        return null;
    }

    private void hideSuggestions() {
        if (!overlayAttached || windowManager == null || suggestionOverlay == null) {
            return;
        }
        try {
            windowManager.removeView(suggestionOverlay);
        } catch (RuntimeException ignored) {
            // The system may already have removed the accessibility overlay.
        }
        overlayAttached = false;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static boolean isPrivateEditor(EditorInfo info) {
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

    @TargetApi(33)
    private final class DirectInputMethod extends InputMethod {
        DirectInputMethod(AccessibilityService service) {
            super(service);
        }

        @Override
        public void onStartInput(EditorInfo attribute, boolean restarting) {
            directInputActive = true;
            directPrivateField = isPrivateEditor(attribute)
                    || getPackageName().equals(attribute.packageName);
            lastDirectExpansion = null;
            suppressedDirectSuffix = "";
            loadShortcuts();
            scheduleDirectInputProcessing();
        }

        @Override
        public void onUpdateSelection(
                int oldSelStart,
                int oldSelEnd,
                int newSelStart,
                int newSelEnd,
                int candidatesStart,
                int candidatesEnd
        ) {
            scheduleDirectInputProcessing();
        }

        @Override
        public void onFinishInput() {
            directInputActive = false;
            directPrivateField = false;
            lastDirectExpansion = null;
            suppressedDirectSuffix = "";
            handler.removeCallbacks(directInputProcessor);
            hideSuggestions();
        }
    }

    private static final class LegacyExpansionRecord {
        final String fieldKey;
        final String afterText;
        final String afterSingleBackspaceText;
        final String restoreText;
        final int restoreCursor;

        LegacyExpansionRecord(
                String fieldKey,
                String afterText,
                String afterSingleBackspaceText,
                String restoreText,
                int restoreCursor
        ) {
            this.fieldKey = fieldKey;
            this.afterText = afterText;
            this.afterSingleBackspaceText = afterSingleBackspaceText;
            this.restoreText = restoreText;
            this.restoreCursor = restoreCursor;
        }
    }

    private static final class DirectExpansionRecord {
        final String context;
        final String afterSuffix;
        final String afterBackspaceSuffix;
        final int remainingAfterBackspaceLength;
        final String restoreKeyword;

        DirectExpansionRecord(
                String context,
                String afterSuffix,
                String afterBackspaceSuffix,
                int remainingAfterBackspaceLength,
                String restoreKeyword
        ) {
            this.context = context;
            this.afterSuffix = afterSuffix;
            this.afterBackspaceSuffix = afterBackspaceSuffix;
            this.remainingAfterBackspaceLength = remainingAfterBackspaceLength;
            this.restoreKeyword = restoreKeyword;
        }
    }
}
