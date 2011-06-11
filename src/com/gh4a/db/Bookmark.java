package com.gh4a.db;

import java.io.Serializable;

public class Bookmark implements Serializable {

    private long id;
    private String name;
    private String objectType;
    private String objectClass;
    
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getObjectType() {
        return objectType;
    }
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }
    public String getObjectClass() {
        return objectClass;
    }
    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }
}
