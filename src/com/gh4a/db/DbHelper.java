package com.gh4a.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "gh4adb.db";
    private static final int DATABASE_VERSION = 1;

    static final String BOOKMARKS_TABLE = "bookmarks";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + BOOKMARKS_TABLE + " ("
                + "_id integer primary key autoincrement, "
                + "name text not null, "
                + "type integer not null, "
                + "uri text not null, "
                + "extra_data text);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
