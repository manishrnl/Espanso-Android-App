package com.manishrnl.espansoandroid;

import java.util.Locale;

public final class Shortcut {
    private long id;
    private String keyword;
    private String text;
    private boolean replaceAfterSpace;
    private int position;
    private String selectionStrategy;
    private String folder;

    public Shortcut(long id, String keyword, String text, boolean replaceAfterSpace,
                    int position, String selectionStrategy, String folder) {
        this.id = id;
        this.keyword = keyword == null ? "" : keyword;
        this.text = text == null ? "" : text;
        this.replaceAfterSpace = replaceAfterSpace;
        this.position = position;
        this.selectionStrategy = selectionStrategy == null ? "" : selectionStrategy;
        this.folder = folder == null ? "" : folder;
    }

    public Shortcut(String keyword, String text, boolean replaceAfterSpace,
                    int position, String selectionStrategy, String folder) {
        this(0, keyword, text, replaceAfterSpace, position, selectionStrategy, folder);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getText() {
        return text;
    }

    public boolean isReplaceAfterSpace() {
        return replaceAfterSpace;
    }

    public int getPosition() {
        return position;
    }

    public String getSelectionStrategy() {
        return selectionStrategy;
    }

    public String getFolder() {
        return folder;
    }

    public boolean matchesSearch(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return keyword.toLowerCase(Locale.ROOT).contains(normalized)
                || text.toLowerCase(Locale.ROOT).contains(normalized)
                || folder.toLowerCase(Locale.ROOT).contains(normalized);
    }
}

