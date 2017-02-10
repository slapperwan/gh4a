package com.gh4a.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.Log;

public class BookmarksProvider extends ContentProvider {
    private static final String TAG = "BookmarksProvider";

    public interface Columns extends BaseColumns {
        Uri CONTENT_URI = Uri.parse("content://com.gh4a/bookmarks");

        String NAME = "name";
        String TYPE = "type";
        String URI = "uri";
        String EXTRA = "extra_data";

        int TYPE_USER = 0;
        int TYPE_REPO = 1;
    }

    private static final int MATCH_ALL = 0;
    private static final int MATCH_ID  = 1;

    private static final UriMatcher
            sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI("com.gh4a", "bookmarks", MATCH_ALL);
        sURIMatcher.addURI("com.gh4a", "bookmarks/#", MATCH_ID);
    }

    private static class DbHelper extends SQLiteOpenHelper {
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

    private DbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int match = sURIMatcher.match(uri);

        qb.setTables(DbHelper.BOOKMARKS_TABLE);

        switch (match) {
            case MATCH_ALL:
                break;
            case MATCH_ID:
                qb.appendWhere(Columns._ID + " = " + uri.getLastPathSegment());
                break;
            default:
                Log.e(TAG, "query: invalid request: " + uri);
                return null;
        }

        if (sortOrder == null) {
            sortOrder = Columns.TYPE + " asc";
        }

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        ret.setNotificationUri(getContext().getContentResolver(), uri);

        return ret;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (sURIMatcher.match(uri) != MATCH_ALL) {
            return null;
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long rowID = db.insert(DbHelper.BOOKMARKS_TABLE, null, values);
        if (rowID <= 0) {
            return null;
        }

        getContext().getContentResolver().notifyChange(Columns.CONTENT_URI, null);

        return ContentUris.withAppendedId(Columns.CONTENT_URI, rowID);
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count;
        int match = sURIMatcher.match(uri);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        switch (match) {
            case MATCH_ALL:
                count = db.update(DbHelper.BOOKMARKS_TABLE, values, selection, selectionArgs);
                break;
            case MATCH_ID:
                if (selection != null || selectionArgs != null) {
                    throw new UnsupportedOperationException(
                            "Cannot update URI " + uri + " with a where clause");
                }
                count = db.update(DbHelper.BOOKMARKS_TABLE, values, Columns._ID + " = ?",
                        new String[] { uri.getLastPathSegment() });
                break;
            default:
                throw new UnsupportedOperationException("Cannot update that URI: " + uri);
        }

        if (count > 0) {
            getContext().getContentResolver().notifyChange(Columns.CONTENT_URI, null);
        }

        return count;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int match = sURIMatcher.match(uri);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        switch (match) {
            case MATCH_ALL:
                break;
            case MATCH_ID:
                if (selection != null || selectionArgs != null) {
                    throw new UnsupportedOperationException(
                            "Cannot delete URI " + uri + " with a where clause");
                }
                selection = Columns._ID + " = ?";
                selectionArgs = new String[] { uri.getLastPathSegment() };
                break;
            default:
                throw new UnsupportedOperationException("Cannot delete the URI " + uri);
        }

        int count = db.delete(DbHelper.BOOKMARKS_TABLE, selection, selectionArgs);

        if (count > 0) {
            getContext().getContentResolver().notifyChange(Columns.CONTENT_URI, null);
        }

        return count;
    }
}