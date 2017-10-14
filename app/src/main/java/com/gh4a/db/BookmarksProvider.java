package com.gh4a.db;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.gh4a.R;
import com.gh4a.BuildConfig;

import java.util.Locale;

public class BookmarksProvider extends ContentProvider {
    private static final String TAG = "BookmarksProvider";

    public interface Columns extends BaseColumns {
        Uri CONTENT_URI = Uri.parse(String.format(Locale.US,
                "content://%s/bookmarks", BuildConfig.APPLICATION_ID));

        String NAME = "name";
        String TYPE = "type";
        String URI = "uri";
        String EXTRA = "extra_data";
        String ORDER_ID = "order_id";

        int TYPE_USER = 0;
        int TYPE_REPO = 1;
    }

    private static final int MATCH_ALL = 0;
    private static final int MATCH_ID = 1;

    private static final UriMatcher
            sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(BuildConfig.APPLICATION_ID, "bookmarks", MATCH_ALL);
        sURIMatcher.addURI(BuildConfig.APPLICATION_ID, "bookmarks/#", MATCH_ID);
    }

    private DbHelper mDbHelper;

    // url must be resolvable by BrowseFilter!
    public static void saveBookmark(Context context, String name, int type, String url,
            String extraData, boolean showToast) {
        ContentResolver cr = context.getContentResolver();

        ContentValues cv = new ContentValues();
        cv.put(BookmarksProvider.Columns.NAME, name);
        cv.put(BookmarksProvider.Columns.TYPE, type);
        cv.put(BookmarksProvider.Columns.URI, url);
        cv.put(BookmarksProvider.Columns.EXTRA, extraData);
        cv.put(BookmarksProvider.Columns.ORDER_ID, getNextOrderId(cr));

        if (cr.insert(BookmarksProvider.Columns.CONTENT_URI, cv) != null && showToast) {
            Toast.makeText(context, R.string.bookmark_saved, Toast.LENGTH_LONG).show();
        }
    }

    private static int getNextOrderId(ContentResolver cr) {
        Cursor query = cr.query(Columns.CONTENT_URI,
                new String[] { "COUNT(*)" },
                null, null, null);

        int orderId = 0;
        if (query != null) {
            if (query.moveToFirst()) {
                orderId = query.getInt(0);
            }
            query.close();
        }
        return orderId;
    }

    public static void removeBookmark(Context context, String url) {
        int removedRows = context.getContentResolver().delete(Columns.CONTENT_URI,
                Columns.URI + " = ?",
                new String[] { url });
        if (removedRows > 0) {
            Toast.makeText(context, R.string.bookmark_removed, Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean hasBookmarked(Context context, String url) {
        Cursor cursor = context.getContentResolver().query(Columns.CONTENT_URI,
                new String[] { Columns._ID },
                Columns.URI + " = ?",
                new String[] { url },
                null);

        boolean hasBookmarked = false;
        if (cursor != null) {
            hasBookmarked = cursor.getCount() > 0;
            cursor.close();
        }
        return hasBookmarked;
    }

    public static void reorderBookmark(Context context, long id, int orderId) {
        ContentValues cv = new ContentValues();
        cv.put(Columns.ORDER_ID, orderId);
        Uri uri = ContentUris.withAppendedId(Columns.CONTENT_URI, id);
        context.getContentResolver().update(uri, cv, null, null);
    }

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
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
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
