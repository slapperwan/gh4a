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
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

/**
 * The Root adapter.
 *
 * @param <T> the generic type
 */
public abstract class RootAdapter<T> extends BaseAdapter implements Filterable {
    /** The objects. */
    private List<T> mObjects;
    private List<T> mUnfilteredObjects;

    /** The context. */
    protected Context mContext;

    private Filter mFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (TextUtils.isEmpty(constraint)) {
                results.values = mUnfilteredObjects;
                results.count = mUnfilteredObjects.size();
            } else {
                final ArrayList<T> filtered = new ArrayList<>();
                for (T object : mUnfilteredObjects) {
                    if (isFiltered(constraint, object)) {
                        filtered.add(object);
                    }
                }
                results.values = filtered;
                results.count = filtered.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mObjects = (List<T>) results.values;
            notifyDataSetChanged();
        }
    };

    /**
     * Instantiates a new root adapter.
     *
     * @param context the context
     */
    public RootAdapter(Context context) {
        mObjects = new ArrayList<>();
        mUnfilteredObjects = new ArrayList<>();
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
        if (convertView == null) {
            convertView = createView(LayoutInflater.from(mContext), parent,
                    getItemViewType(position));
        }
        bindView(convertView, getItem(position));

        return convertView;
    }

    /**
     * Adds the object.
     *
     * @param object the object
     */
    public void add(T object) {
        mUnfilteredObjects.add(object);
        mObjects.add(object);
    }

    public void addAll(Collection<T> objects) {
        if (objects != null) {
            mUnfilteredObjects.addAll(objects);
            mObjects.addAll(objects);
            notifyDataSetChanged();
        }
    }

    public void remove(T object) {
        mUnfilteredObjects.remove(object);
        mObjects.remove(object);
        notifyDataSetChanged();
    }

    public void clear() {
        mUnfilteredObjects.clear();
        mObjects.clear();
        notifyDataSetChanged();
    }

    protected boolean isFiltered(CharSequence filter, T object) {
        return true;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    public boolean isCardStyle() {
        return false;
    }

    protected abstract View createView(LayoutInflater inflater, ViewGroup parent, int viewType);
    protected abstract void bindView(View view, T object);
}