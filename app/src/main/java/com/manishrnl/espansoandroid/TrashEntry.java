package com.manishrnl.espansoandroid;

public final class TrashEntry {
    public static final int TYPE_FOLDER = 1;
    public static final int TYPE_SHORTCUT = 2;

    private final long id;
    private final int type;
    private final String title;
    private final String detail;
    private final String folder;
    private final long deletedAt;
    private final int childCount;

    private TrashEntry(
            long id,
            int type,
            String title,
            String detail,
            String folder,
            long deletedAt,
            int childCount
    ) {
        this.id = id;
        this.type = type;
        this.title = title == null ? "" : title;
        this.detail = detail == null ? "" : detail;
        this.folder = folder == null ? "" : folder;
        this.deletedAt = deletedAt;
        this.childCount = childCount;
    }

    public static TrashEntry folder(
            long id,
            String name,
            long deletedAt,
            int childCount
    ) {
        return new TrashEntry(
                id,
                TYPE_FOLDER,
                name,
                "",
                name,
                deletedAt,
                childCount
        );
    }

    public static TrashEntry shortcut(
            long id,
            String keyword,
            String replacement,
            String folder,
            long deletedAt
    ) {
        return new TrashEntry(
                id,
                TYPE_SHORTCUT,
                keyword,
                replacement,
                folder,
                deletedAt,
                0
        );
    }

    public long getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getFolder() {
        return folder;
    }

    public long getDeletedAt() {
        return deletedAt;
    }

    public int getChildCount() {
        return childCount;
    }

    public boolean isFolder() {
        return type == TYPE_FOLDER;
    }
}
