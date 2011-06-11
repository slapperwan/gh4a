package com.gh4a.db;

import java.io.Serializable;

public class BookmarkParam implements Serializable {

    private long id;
    private long bookmarkId;
    private String key;
    private String value;
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public long getBookmarkId() {
        return bookmarkId;
    }
    public void setBookmarkId(long bookmarkId) {
        this.bookmarkId = bookmarkId;
    }
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    
}
