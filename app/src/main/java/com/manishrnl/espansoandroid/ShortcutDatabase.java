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
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE = "shortcuts";

    public ShortcutDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "keyword TEXT NOT NULL,"
                + "replacement TEXT NOT NULL,"
                + "replace_after_space INTEGER NOT NULL DEFAULT 0,"
                + "position INTEGER NOT NULL DEFAULT 1,"
                + "selection_strategy TEXT NOT NULL DEFAULT '',"
                + "folder TEXT NOT NULL DEFAULT ''"
                + ")");
        db.execSQL("CREATE INDEX shortcuts_keyword_idx ON " + TABLE + "(keyword)");

        ContentValues initial = valuesFor(new Shortcut(
                ";git",
                "https://github.com/manishrnl/",
                false,
                1,
                "",
                "Social Media"
        ));
        db.insertOrThrow(TABLE, null, initial);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future schema migrations belong here.
    }

    public List<Shortcut> getAll() {
        List<Shortcut> shortcuts = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE,
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
                TABLE,
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
        return getWritableDatabase().insertOrThrow(TABLE, null, valuesFor(shortcut));
    }

    public void update(Shortcut shortcut) {
        getWritableDatabase().update(
                TABLE,
                valuesFor(shortcut),
                "_id = ?",
                new String[]{String.valueOf(shortcut.getId())}
        );
    }

    public void delete(long id) {
        getWritableDatabase().delete(TABLE, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void replaceAll(List<Shortcut> shortcuts) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE, null, null);
            for (Shortcut shortcut : shortcuts) {
                long id = db.insertOrThrow(TABLE, null, valuesFor(shortcut));
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
}

