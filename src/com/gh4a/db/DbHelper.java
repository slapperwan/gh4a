package com.gh4a.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gh4adb";
    private static final int DATABASE_VERSION = 1;
    private static final String TBL_BOOKMARK = "bookmark";
    private static final String TBL_BOOKMARK_PARAM = "bookmark_param";
    
    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TBL_BOOKMARK + " (_id integer primary key autoincrement, "
            + "name text not null, object_type, object_class text not null);");
        db.execSQL("create table " + TBL_BOOKMARK_PARAM + " (_id integer primary key autoincrement, "
                + "bookmark_id integer not null, key text not null, value text not null);");
        db.execSQL("create index bookmark_id_idx on " + TBL_BOOKMARK_PARAM + "(bookmark_id)");
        db.execSQL("create index object_type_idx on " + TBL_BOOKMARK + "(object_type)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public List<Bookmark> findAllBookmark() {
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor c = db.query(TBL_BOOKMARK, 
                            new String[] {"_id", "name", "object_type", "object_class"},
                            null, 
                            null,
                            null, 
                            null, 
                            "object_type desc");
            
            List<Bookmark> bookmarks = new ArrayList<Bookmark>();
            
            int numRows = 0;
            if (c != null) {
                numRows = c.getCount();
                c.moveToFirst();
                for (int i = 0; i < numRows; ++i) {
                    Bookmark b = new Bookmark();
                    
                    b.setId(c.getInt(0));
                    b.setName(c.getString(1));
                    b.setObjectType(c.getString(2));
                    b.setObjectClass(c.getString(3));
                    
                    bookmarks.add(b);
                    c.moveToNext();
                }
            }
            return bookmarks;
        }
        finally {
            db.close();
        }
    }
    
    public long saveBookmark(Bookmark b) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues initialValues = new ContentValues();
            initialValues.put("name", b.getName());
            initialValues.put("object_type", b.getObjectType());
            initialValues.put("object_class", b.getObjectClass());
            return db.insert(TBL_BOOKMARK, null, initialValues);
        }
        finally {
            db.close();
        }
    }
    
    public void updateBookmark(Bookmark b) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", b.getName());
            contentValues.put("object_type", b.getObjectType());
            contentValues.put("object_class", b.getObjectClass());
            db.update(TBL_BOOKMARK, contentValues, "_id = " + b.getId(), null);
        }
        finally {
            db.close();
        }
    }
    
    public void deleteBookmark(long id) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.delete(TBL_BOOKMARK, "_id = " + id, null);
        }
        finally {
            db.close();
        }
    }
    
    public List<BookmarkParam> findBookmarkParams(long bookmarkId) {
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor c = db.query(TBL_BOOKMARK_PARAM, 
                            new String[] {"_id", "bookmark_id", "key", "value"},
                            "bookmark_id = " + bookmarkId, 
                            null,
                            null, 
                            null, 
                            "_id asc");
            
            List<BookmarkParam> params = new ArrayList<BookmarkParam>();
            
            int numRows = 0;
            if (c != null) {
                numRows = c.getCount();
                c.moveToFirst();
                for (int i = 0; i < numRows; ++i) {
                    BookmarkParam b = new BookmarkParam();
                    
                    b.setId(c.getInt(0));
                    b.setBookmarkId(c.getInt(1));
                    b.setKey(c.getString(2));
                    b.setValue(c.getString(3));
                    
                    params.add(b);
                    c.moveToNext();
                }
            }
            return params;
        }
        finally {
            db.close();
        }
    }
    
    public void saveBookmarkParam(BookmarkParam[] params) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            for (BookmarkParam b : params) {
                ContentValues initialValues = new ContentValues();
                initialValues.put("bookmark_id", b.getBookmarkId());
                initialValues.put("key", b.getKey());
                initialValues.put("value", b.getValue());
                db.insert(TBL_BOOKMARK_PARAM, null, initialValues);
            }
        }
        finally {
            db.close();
        }
    }
    
    public void deleteBookmarkParam(long bookmarkId) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.delete(TBL_BOOKMARK_PARAM, "bookmark_id = " + bookmarkId, null);
        }
        finally {
            db.close();
        }
    }
}
