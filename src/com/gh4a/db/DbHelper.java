package com.gh4a.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.net.URISyntaxException;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "gh4adb.db";
    private static final int DATABASE_VERSION = 3;

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
        if (oldVersion < 3) {
            updateBookmarkUris(db);
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

    private void updateBookmarkUris(SQLiteDatabase db) {
        Cursor c = db.query(BOOKMARKS_TABLE, new String[] { "_id", "uri", "extra_data" },
                null, null, null, null, null);
        if (c == null) {
            return;
        }
        try {
            ContentValues cv = new ContentValues();
            final String[] userExtras = new String[] { "USER_LOGIN", "login" };
            final String[] repoOwnerExtras = new String[] { "REPO_OWNER", "owner" };
            final String[] repoNameExtras = new String[] { "REPO_NAME", "repo" };

            while (c.moveToNext()) {
                long id = c.getLong(0);
                String intentUri = c.getString(1);
                try {
                    Intent intent = Intent.parseUri(intentUri, 0);
                    String activity = intent.getComponent().getClassName();
                    String url = null;

                    if ("com.gh4a.activities.UserActivity".equals(activity)) {
                        String user = resolveExtra(intent, userExtras);
                        if (user != null) {
                            url = "https://github.com/" + user;
                        }
                    } else if ("com.gh4a.activities.RepositoryActivity".equals(activity)) {
                        String repoOwner = resolveExtra(intent, repoOwnerExtras);
                        String repoName = resolveExtra(intent, repoNameExtras);
                        if (repoOwner != null && repoName != null) {
                            url = "https://github.com/" + repoOwner + "/" + repoName;
                        }
                        String ref = c.getString(2);
                        if (ref != null) {
                            url += "/tree/" + ref;
                        }
                    }
                    if (url != null) {
                        cv.put("uri", url);
                        db.update(BOOKMARKS_TABLE, cv, "_id = ?", new String[]{Long.toString(id)});
                    }
                } catch (URISyntaxException e) {
                    // ignore
                }
            }
        } finally {
            c.close();
        }
    }

    private String resolveExtra(Intent intent, String[] names) {
        for (String name : names) {
            if (intent.hasExtra(name)) {
                return intent.getStringExtra(name);
            }
        }
        return null;
    }
}
