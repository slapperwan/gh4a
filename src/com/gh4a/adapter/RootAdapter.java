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
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

/**
 * The Root adapter.
 *
 * @param <T> the generic type
 */
public abstract class RootAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements Filterable, View.OnClickListener {
    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }
    public interface OnScrolledToFooterListener {
        void onScrolledToFooter();
    }

    /**
     * The objects.
     */
    private List<T> mObjects;
    private List<T> mUnfilteredObjects;

    /**
     * The context.
     */
    protected final Context mContext;
    private final LayoutInflater mInflater;
    private OnItemClickListener<T> mItemClickListener;

    private View mHeaderView;
    private View mFooterView;
    private OnScrolledToFooterListener mFooterListener;

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_HEADER = 1;
    private static final int VIEW_TYPE_FOOTER = 2;

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
        mInflater = LayoutInflater.from(mContext);
    }

    public void setHeaderView(View headerView) {
        mHeaderView = headerView;
        notifyDataSetChanged();
    }

    public void setFooterView(View footerView, OnScrolledToFooterListener footerListener) {
        mFooterView = footerView;
        mFooterListener = footerListener;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        mItemClickListener = listener;
    }

    @Override
    public int getItemCount() {
        return mObjects.size()
                + (mHeaderView != null ? 1 : 0)
                + (mFooterView != null ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        int itemStart = mHeaderView != null ? 1 : 0;
        if (mHeaderView != null && position == 0) {
            return VIEW_TYPE_HEADER;
        } else if (mFooterView != null && position == itemStart + mObjects.size()) {
            return VIEW_TYPE_FOOTER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    public int getCount() {
        return mObjects.size();
    }

    public T getItem(int position) {
        return mObjects.get(position);
    }

    public T getItemFromAdapterPosition(int position) {
        return mObjects.get(position - (mHeaderView != null ? 1 : 0));
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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return new HeaderViewHolder(mHeaderView);
            case VIEW_TYPE_FOOTER:
                return new FooterViewHolder(mFooterView);
            default:
                RecyclerView.ViewHolder holder = onCreateViewHolder(mInflater, parent);
                holder.itemView.setOnClickListener(this);
                holder.itemView.setTag(holder);
                return holder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FooterViewHolder) {
            if (mFooterListener != null) {
                mFooterListener.onScrolledToFooter();
            }
        } else if (!(holder instanceof HeaderViewHolder)) {
            onBindViewHolder((VH) holder, getItemFromAdapterPosition(position));
        }
    }

    @Override
    public void onClick(View view) {
        if (mItemClickListener != null) {
            VH holder = (VH) view.getTag();
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                mItemClickListener.onItemClick(getItemFromAdapterPosition(position));
            }
        }
    }

    protected abstract VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent);
    protected abstract void onBindViewHolder(VH holder, T item);
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

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(View v) {
            super(v);
        }
    }
    private static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(View v) {
            super(v);
        }
    }
}