package com.manishrnl.espansoandroid;

import java.util.List;

final class ExpansionMatcher {
    private ExpansionMatcher() {
    }

    static Match findExpansion(List<Shortcut> shortcuts, String text, int cursor) {
        if (text == null || cursor < 0 || cursor > text.length()) {
            return null;
        }

        boolean afterSpace = cursor > 0 && text.charAt(cursor - 1) == ' ';
        int keywordEnd = afterSpace ? cursor - 1 : cursor;
        String prefix = text.substring(0, keywordEnd);
        for (Shortcut shortcut : shortcuts) {
            if (shortcut.isReplaceAfterSpace() != afterSpace) {
                continue;
            }
            String keyword = shortcut.getKeyword();
            if (!keyword.isEmpty() && prefix.endsWith(keyword)) {
                return new Match(
                        shortcut,
                        keywordEnd - keyword.length(),
                        cursor,
                        afterSpace
                );
            }
        }
        return null;
    }

    static String findSuggestionFragment(
            List<Shortcut> shortcuts,
            String text,
            int cursor,
            int maximumKeywordLength
    ) {
        if (text == null || cursor <= 0 || cursor > text.length()) {
            return "";
        }
        int earliest = Math.max(0, cursor - Math.max(1, maximumKeywordLength));
        for (int start = earliest; start < cursor; start++) {
            String candidate = text.substring(start, cursor);
            for (Shortcut shortcut : shortcuts) {
                if (shortcut.getKeyword().startsWith(candidate)) {
                    return candidate;
                }
            }
        }
        return "";
    }

    static String replace(String text, int start, int end, String replacement) {
        return text.substring(0, start) + replacement + text.substring(end);
    }

    static String removeCodePointBefore(String text, int cursor) {
        if (text == null || cursor <= 0 || cursor > text.length()) {
            return text;
        }
        int start = text.offsetByCodePoints(cursor, -1);
        return text.substring(0, start) + text.substring(cursor);
    }

    static final class Match {
        final Shortcut shortcut;
        final int start;
        final int end;
        final boolean appendSpace;

        Match(Shortcut shortcut, int start, int end, boolean appendSpace) {
            this.shortcut = shortcut;
            this.start = start;
            this.end = end;
            this.appendSpace = appendSpace;
        }
    }
}
