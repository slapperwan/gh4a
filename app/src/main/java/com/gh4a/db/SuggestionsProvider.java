package com.gh4a.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.Log;

import com.gh4a.BuildConfig;

import java.util.Locale;

public class SuggestionsProvider extends ContentProvider {
    private static final String TAG = "SuggestionsProvider";

    public interface Columns extends BaseColumns {
        Uri CONTENT_URI = Uri.parse(String.format(Locale.US,
                "content://%s.SuggestionsProvider/suggestions", BuildConfig.APPLICATION_ID));

        String TYPE = "type";
        String SUGGESTION = "suggestion";
        String DATE = "date";

        int TYPE_REPO = 0;
        int TYPE_USER = 1;
        int TYPE_CODE = 2;
    }

    private static final int MATCH_ALL = 0;

    private static final UriMatcher
            sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(
                String.format(Locale.US, "%s.SuggestionsProvider", BuildConfig.APPLICATION_ID),
                "suggestions", MATCH_ALL);
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

        qb.setTables(DbHelper.SUGGESTIONS_TABLE);

        switch (match) {
            case MATCH_ALL:
                break;
            default:
                Log.e(TAG, "query: invalid request: " + uri);
                return null;
        }

        if (sortOrder == null) {
            sortOrder = Columns.DATE + " desc";
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
        long rowID = db.insert(DbHelper.SUGGESTIONS_TABLE, null, values);
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
                count = db.update(DbHelper.SUGGESTIONS_TABLE, values, selection, selectionArgs);
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
            default:
                throw new UnsupportedOperationException("Cannot delete the URI " + uri);
        }

        int count = db.delete(DbHelper.SUGGESTIONS_TABLE, selection, selectionArgs);

        if (count > 0) {
            getContext().getContentResolver().notifyChange(Columns.CONTENT_URI, null);
        }

        return count;
    }
}
