package com.manishrnl.espansoandroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class ShortcutDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "shortcuts.db";
    private static final int DATABASE_VERSION = 2;
    private static final String SHORTCUT_TABLE = "shortcuts";
    private static final String FOLDER_TABLE = "folders";

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

        ContentValues initial = valuesFor(new Shortcut(
                ";git",
                "https://github.com/manishrnl/",
                false,
                1,
                "",
                "Social Media"
        ));
        db.insertOrThrow(SHORTCUT_TABLE, null, initial);
        insertFolder(db, "Social Media");
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

    public void delete(long id) {
        getWritableDatabase().delete(
                SHORTCUT_TABLE,
                "_id = ?",
                new String[]{String.valueOf(id)}
        );
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
            db.delete(SHORTCUT_TABLE, null, null);
            db.delete(FOLDER_TABLE, null, null);
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
