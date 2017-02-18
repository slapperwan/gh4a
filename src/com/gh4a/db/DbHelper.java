package com.gh4a.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "gh4adb.db";
    private static final int DATABASE_VERSION = 2;

    static final String BOOKMARKS_TABLE = "bookmarks";
    static final String SUGGESTIONS_TABLE = "suggestions";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createBookmarksTable(db);
        createSuggestionsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createSuggestionsTable(db);
        }
    }

    private void createBookmarksTable(SQLiteDatabase db) {
        db.execSQL("create table " + BOOKMARKS_TABLE + " ("
                + "_id integer primary key autoincrement, "
                + "name text not null, "
                + "type integer not null, "
                + "uri text not null, "
                + "extra_data text);");
    }

    private void createSuggestionsTable(SQLiteDatabase db) {
        db.execSQL("create table " + SUGGESTIONS_TABLE + " ("
                + "_id integer primary key autoincrement, "
                + "type integer not null, "
                + "suggestion text, "
                + "date long, "
                + "unique (type, suggestion) on conflict replace);");
    }
}
