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

import android.content.Context;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    private final List<T> mUnfilteredObjects;

    /**
     * The context.
     */
    protected final Context mContext;
    private final LayoutInflater mInflater;
    private OnItemClickListener<T> mItemClickListener;
    private boolean mContextMenuSupported;

    private View mHeaderView;
    private View mFooterView;
    private OnScrolledToFooterListener mFooterListener;
    private int mHighlightPosition = -1;
    private boolean mHolderCreated = false;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_FOOTER = 1;
    private static final int VIEW_TYPE_ITEM = 2;

    public static final int CUSTOM_VIEW_TYPE_START = VIEW_TYPE_ITEM;

    private final Filter mFilter = new Filter() {
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
        if (mHolderCreated) {
            throw new IllegalStateException("Must not set item click listener after views are bound");
        }
        mItemClickListener = listener;
    }

    public void setContextMenuSupported(boolean supported) {
        if (mHolderCreated) {
            throw new IllegalStateException("Must not set context menu state after views are bound");
        }
        mContextMenuSupported = supported;
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
            int viewType = getItemViewType(getItem(position - itemStart));
            assert viewType >= CUSTOM_VIEW_TYPE_START;
            return viewType;
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

    public void highlight(int position) {
        mHighlightPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mHolderCreated = true;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return new HeaderViewHolder(mHeaderView);
            case VIEW_TYPE_FOOTER:
                return new FooterViewHolder(mFooterView);
            default:
                RecyclerView.ViewHolder holder = onCreateViewHolder(mInflater, parent, viewType);
                if (mItemClickListener != null) {
                    holder.itemView.setOnClickListener(this);
                    holder.itemView.setTag(holder);
                }
                if (mContextMenuSupported) {
                    holder.itemView.setLongClickable(true);
                }
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
            if (position == mHighlightPosition) {
                final View v = holder.itemView;
                v.post(() -> {
                    if (v.getBackground() != null) {
                        final int centerX = v.getWidth() / 2;
                        final int centerY = v.getHeight() / 2;
                        DrawableCompat.setHotspot(v.getBackground(), centerX, centerY);
                    }
                    v.setPressed(true);
                    v.setPressed(false);
                    mHighlightPosition = -1;
                });
            }
        }
    }

    @Override
    public void onClick(View view) {
        VH holder = (VH) view.getTag();
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            mItemClickListener.onItemClick(getItemFromAdapterPosition(position));
        }
    }

    protected abstract VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent,
            int viewType);
    protected abstract void onBindViewHolder(VH holder, T item);
    protected boolean isFiltered(CharSequence filter, T object) {
        return true;
    }
    protected int getItemViewType(T item) {
        return VIEW_TYPE_ITEM;
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