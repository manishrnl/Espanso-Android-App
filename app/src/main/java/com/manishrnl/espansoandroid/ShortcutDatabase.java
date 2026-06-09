package com.manishrnl.espansoandroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ShortcutDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "shortcuts.db";
    private static final int DATABASE_VERSION = 4;
    private static final String SHORTCUT_TABLE = "shortcuts";
    private static final String FOLDER_TABLE = "folders";
    private static final String TRASH_FOLDER_TABLE = "trash_folders";
    private static final String TRASH_SHORTCUT_TABLE = "trash_shortcuts";
    private static final long TRASH_RETENTION_MILLIS =
            90L * 24L * 60L * 60L * 1000L;

    public ShortcutDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + SHORTCUT_TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "keyword TEXT NOT NULL,"
                + "replacement TEXT NOT NULL,"
                + "replace_after_space INTEGER NOT NULL DEFAULT 0,"
                + "position INTEGER NOT NULL DEFAULT 1,"
                + "selection_strategy TEXT NOT NULL DEFAULT '',"
                + "folder TEXT NOT NULL DEFAULT ''"
                + ")");
        db.execSQL(
                "CREATE INDEX shortcuts_keyword_idx ON "
                        + SHORTCUT_TABLE
                        + "(keyword)"
        );
        createFolderTable(db);
        createTrashTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createFolderTable(db);
            db.execSQL(
                    "INSERT OR IGNORE INTO "
                            + FOLDER_TABLE
                            + "(name) SELECT DISTINCT TRIM(folder) FROM "
                            + SHORTCUT_TABLE
                            + " WHERE TRIM(folder) <> ''"
            );
        }
        if (oldVersion < 3) {
            createTrashTables(db);
        }
        if (oldVersion < 4) {
            removePredefinedShortcut(db);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            purgeExpiredTrash(db);
        }
    }

    public List<Shortcut> getAll() {
        List<Shortcut> shortcuts = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                SHORTCUT_TABLE,
                null,
                null,
                null,
                null,
                null,
                "folder COLLATE NOCASE, position, keyword COLLATE NOCASE"
        )) {
            while (cursor.moveToNext()) {
                shortcuts.add(fromCursor(cursor));
            }
        }
        return shortcuts;
    }

    public Shortcut getById(long id) {
        try (Cursor cursor = getReadableDatabase().query(
                SHORTCUT_TABLE,
                null,
                "_id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        }
    }

    public long insert(Shortcut shortcut) {
        SQLiteDatabase db = getWritableDatabase();
        ensureFolder(db, shortcut.getFolder());
        return db.insertOrThrow(SHORTCUT_TABLE, null, valuesFor(shortcut));
    }

    public void update(Shortcut shortcut) {
        SQLiteDatabase db = getWritableDatabase();
        ensureFolder(db, shortcut.getFolder());
        db.update(
                SHORTCUT_TABLE,
                valuesFor(shortcut),
                "_id = ?",
                new String[]{String.valueOf(shortcut.getId())}
        );
    }

    public boolean moveShortcutToTrash(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try (Cursor cursor = db.query(
                SHORTCUT_TABLE,
                null,
                "_id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return false;
            }
            db.insertOrThrow(
                    TRASH_SHORTCUT_TABLE,
                    null,
                    trashValuesFor(cursor, null, System.currentTimeMillis())
            );
            db.delete(
                    SHORTCUT_TABLE,
                    "_id = ?",
                    new String[]{String.valueOf(id)}
            );
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean moveFolderToTrash(String folder) {
        String normalized = normalizeFolder(folder);
        if (normalized.isEmpty()) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            boolean moved = moveFolderToTrash(
                    db,
                    normalized,
                    System.currentTimeMillis()
            );
            db.setTransactionSuccessful();
            return moved;
        } finally {
            db.endTransaction();
        }
    }

    public List<TrashEntry> getTrashEntries() {
        List<TrashEntry> entries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT f._id, f.name, f.deleted_at, COUNT(s._id) AS child_count "
                        + "FROM " + TRASH_FOLDER_TABLE + " f "
                        + "LEFT JOIN " + TRASH_SHORTCUT_TABLE + " s "
                        + "ON s.trash_folder_id = f._id "
                        + "GROUP BY f._id, f.name, f.deleted_at",
                null
        )) {
            while (cursor.moveToNext()) {
                entries.add(TrashEntry.folder(
                        cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("deleted_at")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("child_count"))
                ));
            }
        }
        try (Cursor cursor = db.query(
                TRASH_SHORTCUT_TABLE,
                new String[]{"_id", "keyword", "replacement", "folder", "deleted_at"},
                "trash_folder_id IS NULL",
                null,
                null,
                null,
                null
        )) {
            while (cursor.moveToNext()) {
                entries.add(TrashEntry.shortcut(
                        cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("keyword")),
                        cursor.getString(cursor.getColumnIndexOrThrow("replacement")),
                        cursor.getString(cursor.getColumnIndexOrThrow("folder")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("deleted_at"))
                ));
            }
        }
        entries.sort(Comparator
                .comparingLong(TrashEntry::getDeletedAt)
                .reversed()
                .thenComparing(TrashEntry::getTitle, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    public int getTrashEntryCount() {
        SQLiteDatabase db = getReadableDatabase();
        int folderCount = countRows(db, TRASH_FOLDER_TABLE, null, null);
        int shortcutCount = countRows(
                db,
                TRASH_SHORTCUT_TABLE,
                "trash_folder_id IS NULL",
                null
        );
        return folderCount + shortcutCount;
    }

    public boolean restoreTrashEntry(int type, long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            boolean restored = type == TrashEntry.TYPE_FOLDER
                    ? restoreFolder(db, id)
                    : restoreShortcut(db, id);
            if (restored) {
                db.setTransactionSuccessful();
            }
            return restored;
        } finally {
            db.endTransaction();
        }
    }

    public void permanentlyDeleteTrashEntry(int type, long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (type == TrashEntry.TYPE_FOLDER) {
                db.delete(
                        TRASH_SHORTCUT_TABLE,
                        "trash_folder_id = ?",
                        new String[]{String.valueOf(id)}
                );
                db.delete(
                        TRASH_FOLDER_TABLE,
                        "_id = ?",
                        new String[]{String.valueOf(id)}
                );
            } else {
                db.delete(
                        TRASH_SHORTCUT_TABLE,
                        "_id = ? AND trash_folder_id IS NULL",
                        new String[]{String.valueOf(id)}
                );
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void emptyTrash() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TRASH_SHORTCUT_TABLE, null, null);
            db.delete(TRASH_FOLDER_TABLE, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<String> getFolders() {
        List<String> folders = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                FOLDER_TABLE,
                new String[]{"name"},
                null,
                null,
                null,
                null,
                "name COLLATE NOCASE"
        )) {
            while (cursor.moveToNext()) {
                folders.add(cursor.getString(0));
            }
        }
        return folders;
    }

    public boolean insertFolder(String name) {
        String normalized = normalizeFolder(name);
        if (normalized.isEmpty()) {
            return false;
        }
        return insertFolder(getWritableDatabase(), normalized) != -1;
    }

    public void replaceAll(List<Shortcut> shortcuts) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            archiveAllActiveData(db, System.currentTimeMillis());
            for (Shortcut shortcut : shortcuts) {
                ensureFolder(db, shortcut.getFolder());
                long id = db.insertOrThrow(
                        SHORTCUT_TABLE,
                        null,
                        valuesFor(shortcut)
                );
                shortcut.setId(id);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static ContentValues valuesFor(Shortcut shortcut) {
        ContentValues values = new ContentValues();
        values.put("keyword", shortcut.getKeyword());
        values.put("replacement", shortcut.getText());
        values.put("replace_after_space", shortcut.isReplaceAfterSpace() ? 1 : 0);
        values.put("position", Math.max(1, shortcut.getPosition()));
        values.put("selection_strategy", shortcut.getSelectionStrategy());
        values.put("folder", shortcut.getFolder());
        return values;
    }

    private static ContentValues valuesForRestoredShortcut(Cursor cursor) {
        ContentValues values = new ContentValues();
        values.put("keyword", cursor.getString(
                cursor.getColumnIndexOrThrow("keyword")
        ));
        values.put("replacement", cursor.getString(
                cursor.getColumnIndexOrThrow("replacement")
        ));
        values.put("replace_after_space", cursor.getInt(
                cursor.getColumnIndexOrThrow("replace_after_space")
        ));
        values.put("position", cursor.getInt(
                cursor.getColumnIndexOrThrow("position")
        ));
        values.put("selection_strategy", cursor.getString(
                cursor.getColumnIndexOrThrow("selection_strategy")
        ));
        values.put("folder", cursor.getString(
                cursor.getColumnIndexOrThrow("folder")
        ));
        return values;
    }

    private static ContentValues trashValuesFor(
            Cursor cursor,
            Long trashFolderId,
            long deletedAt
    ) {
        ContentValues values = valuesForRestoredShortcut(cursor);
        if (trashFolderId == null) {
            values.putNull("trash_folder_id");
        } else {
            values.put("trash_folder_id", trashFolderId);
        }
        values.put("deleted_at", deletedAt);
        return values;
    }

    private static Shortcut fromCursor(Cursor cursor) {
        return new Shortcut(
                cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("keyword")),
                cursor.getString(cursor.getColumnIndexOrThrow("replacement")),
                cursor.getInt(cursor.getColumnIndexOrThrow("replace_after_space")) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow("position")),
                cursor.getString(cursor.getColumnIndexOrThrow("selection_strategy")),
                cursor.getString(cursor.getColumnIndexOrThrow("folder"))
        );
    }

    private static void createFolderTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + FOLDER_TABLE + " ("
                + "name TEXT PRIMARY KEY COLLATE NOCASE"
                + ")");
    }

    private static void createTrashTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TRASH_FOLDER_TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL COLLATE NOCASE,"
                + "deleted_at INTEGER NOT NULL"
                + ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TRASH_SHORTCUT_TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "trash_folder_id INTEGER,"
                + "keyword TEXT NOT NULL,"
                + "replacement TEXT NOT NULL,"
                + "replace_after_space INTEGER NOT NULL DEFAULT 0,"
                + "position INTEGER NOT NULL DEFAULT 1,"
                + "selection_strategy TEXT NOT NULL DEFAULT '',"
                + "folder TEXT NOT NULL DEFAULT '',"
                + "deleted_at INTEGER NOT NULL"
                + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS trash_shortcut_folder_idx ON "
                + TRASH_SHORTCUT_TABLE + "(trash_folder_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS trash_shortcut_deleted_idx ON "
                + TRASH_SHORTCUT_TABLE + "(deleted_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS trash_folder_deleted_idx ON "
                + TRASH_FOLDER_TABLE + "(deleted_at)");
    }

    private static void removePredefinedShortcut(SQLiteDatabase db) {
        String exactSeed = "keyword = ?"
                + " AND replacement IN (?, ?)"
                + " AND replace_after_space = 0"
                + " AND position = 1"
                + " AND selection_strategy = ''"
                + " AND folder = ? COLLATE NOCASE";
        String[] seedValues = {
                ";git",
                "https://github.com/",
                "https://github.com/manishrnl/",
                "Social Media"
        };
        db.delete(SHORTCUT_TABLE, exactSeed, seedValues);
        db.delete(TRASH_SHORTCUT_TABLE, exactSeed, seedValues);
        db.execSQL(
                "DELETE FROM " + FOLDER_TABLE
                        + " WHERE name = ? COLLATE NOCASE"
                        + " AND NOT EXISTS (SELECT 1 FROM " + SHORTCUT_TABLE
                        + " WHERE TRIM(folder) = ? COLLATE NOCASE)",
                new Object[]{"Social Media", "Social Media"}
        );
        db.execSQL(
                "DELETE FROM " + TRASH_FOLDER_TABLE
                        + " WHERE name = ? COLLATE NOCASE"
                        + " AND NOT EXISTS (SELECT 1 FROM " + TRASH_SHORTCUT_TABLE
                        + " WHERE trash_folder_id = " + TRASH_FOLDER_TABLE + "._id)",
                new Object[]{"Social Media"}
        );
    }

    private static boolean moveFolderToTrash(
            SQLiteDatabase db,
            String folder,
            long deletedAt
    ) {
        boolean exists = folderExists(db, folder);
        try (Cursor cursor = db.query(
                SHORTCUT_TABLE,
                null,
                "TRIM(folder) = ? COLLATE NOCASE",
                new String[]{folder},
                null,
                null,
                null
        )) {
            if (!exists && cursor.getCount() == 0) {
                return false;
            }
            ContentValues folderValues = new ContentValues();
            folderValues.put("name", folder);
            folderValues.put("deleted_at", deletedAt);
            long trashFolderId = db.insertOrThrow(
                    TRASH_FOLDER_TABLE,
                    null,
                    folderValues
            );
            while (cursor.moveToNext()) {
                db.insertOrThrow(
                        TRASH_SHORTCUT_TABLE,
                        null,
                        trashValuesFor(cursor, trashFolderId, deletedAt)
                );
            }
        }
        db.delete(
                SHORTCUT_TABLE,
                "TRIM(folder) = ? COLLATE NOCASE",
                new String[]{folder}
        );
        db.delete(
                FOLDER_TABLE,
                "name = ? COLLATE NOCASE",
                new String[]{folder}
        );
        return true;
    }

    private static void archiveAllActiveData(SQLiteDatabase db, long deletedAt) {
        List<String> folders = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT name FROM " + FOLDER_TABLE
                        + " UNION SELECT DISTINCT TRIM(folder) FROM "
                        + SHORTCUT_TABLE + " WHERE TRIM(folder) <> ''",
                null
        )) {
            while (cursor.moveToNext()) {
                folders.add(cursor.getString(0));
            }
        }
        for (String folder : folders) {
            moveFolderToTrash(db, folder, deletedAt);
        }

        try (Cursor cursor = db.query(
                SHORTCUT_TABLE,
                null,
                "TRIM(folder) = ''",
                null,
                null,
                null,
                null
        )) {
            while (cursor.moveToNext()) {
                db.insertOrThrow(
                        TRASH_SHORTCUT_TABLE,
                        null,
                        trashValuesFor(cursor, null, deletedAt)
                );
            }
        }
        db.delete(SHORTCUT_TABLE, null, null);
        db.delete(FOLDER_TABLE, null, null);
    }

    private static boolean restoreFolder(SQLiteDatabase db, long id) {
        String folder;
        try (Cursor cursor = db.query(
                TRASH_FOLDER_TABLE,
                new String[]{"name"},
                "_id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return false;
            }
            folder = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        }
        insertFolder(db, folder);
        try (Cursor cursor = db.query(
                TRASH_SHORTCUT_TABLE,
                null,
                "trash_folder_id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                "_id"
        )) {
            while (cursor.moveToNext()) {
                db.insertOrThrow(
                        SHORTCUT_TABLE,
                        null,
                        valuesForRestoredShortcut(cursor)
                );
            }
        }
        db.delete(
                TRASH_SHORTCUT_TABLE,
                "trash_folder_id = ?",
                new String[]{String.valueOf(id)}
        );
        db.delete(
                TRASH_FOLDER_TABLE,
                "_id = ?",
                new String[]{String.valueOf(id)}
        );
        return true;
    }

    private static boolean restoreShortcut(SQLiteDatabase db, long id) {
        try (Cursor cursor = db.query(
                TRASH_SHORTCUT_TABLE,
                null,
                "_id = ? AND trash_folder_id IS NULL",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return false;
            }
            String folder = cursor.getString(cursor.getColumnIndexOrThrow("folder"));
            ensureFolder(db, folder);
            db.insertOrThrow(
                    SHORTCUT_TABLE,
                    null,
                    valuesForRestoredShortcut(cursor)
            );
        }
        db.delete(
                TRASH_SHORTCUT_TABLE,
                "_id = ? AND trash_folder_id IS NULL",
                new String[]{String.valueOf(id)}
        );
        return true;
    }

    private static boolean folderExists(SQLiteDatabase db, String folder) {
        try (Cursor cursor = db.query(
                FOLDER_TABLE,
                new String[]{"name"},
                "name = ? COLLATE NOCASE",
                new String[]{folder},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst();
        }
    }

    private static int countRows(
            SQLiteDatabase db,
            String table,
            String selection,
            String[] selectionArgs
    ) {
        try (Cursor cursor = db.query(
                table,
                new String[]{"COUNT(*)"},
                selection,
                selectionArgs,
                null,
                null,
                null
        )) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    private static void purgeExpiredTrash(SQLiteDatabase db) {
        String[] cutoff = new String[]{
                String.valueOf(System.currentTimeMillis() - TRASH_RETENTION_MILLIS)
        };
        db.delete(TRASH_SHORTCUT_TABLE, "deleted_at < ?", cutoff);
        db.delete(TRASH_FOLDER_TABLE, "deleted_at < ?", cutoff);
    }

    private static void ensureFolder(SQLiteDatabase db, String name) {
        String normalized = normalizeFolder(name);
        if (!normalized.isEmpty()) {
            insertFolder(db, normalized);
        }
    }

    private static long insertFolder(SQLiteDatabase db, String name) {
        ContentValues values = new ContentValues();
        values.put("name", normalizeFolder(name));
        return db.insertWithOnConflict(
                FOLDER_TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    private static String normalizeFolder(String name) {
        return name == null ? "" : name.trim();
    }
}
