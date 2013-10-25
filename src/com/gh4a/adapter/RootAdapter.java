/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.ocpsoft.pretty.time.PrettyTime;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * The Root adapter.
 * 
 * @param <T> the generic type
 */
public abstract class RootAdapter<T> extends BaseAdapter {

    /** The Constant pt. */
    protected static final PrettyTime pt = new PrettyTime(new Locale(""));

    /** The objects. */
    protected List<T> mObjects;

    /** The context. */
    protected Context mContext;

    /**
     * Instantiates a new root adapter.
     * 
     * @param context the context
     * @param objects the objects
     */
    public RootAdapter(Context context) {
        mObjects = new ArrayList<T>();
        mContext = context;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getCount()
     */
    @Override
    public int getCount() {
        return mObjects.size();
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public T getItem(int position) {
        return mObjects.get(position);
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View,
     * android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return doGetView(position, convertView, parent);
    }

    /**
     * Adds the object.
     * 
     * @param object the object
     */
    public void add(T object) {
        mObjects.add(object);
    }
    
    public void addAll(List<T> objects) {
        if (objects != null) {
            mObjects.addAll(objects);
        }
    }
    
    public void addAll(int position, List<T> objects) {
        if (objects != null) {
            mObjects.addAll(position, objects);
        }
    }

    public void remove(T object) {
        mObjects.remove(object);
    }

    public List<T> getObjects() {
        return mObjects;
    }
    
    public void clear() {
        mObjects.clear();
    }
    /**
     * Do get view.
     * 
     * @param position the position
     * @param convertView the convert view
     * @param parent the parent
     * @return the view
     */
    public abstract View doGetView(int position, View convertView, ViewGroup parent);
}
