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
package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.client.PageIterator;

import java.util.ArrayList;
import java.util.Collection;

public abstract class PagedDataBaseFragment<T> extends LoadingListFragmentBase implements
        LoaderManager.LoaderCallbacks<Collection<T>>,
        RootAdapter.OnItemClickListener<T>, RootAdapter.OnScrolledToFooterListener {
    private RootAdapter<T, ? extends RecyclerView.ViewHolder> mAdapter;
    private boolean mIsLoadCompleted;
    private View mLoadingView;
    private String mCurrentFilter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(getEmptyTextResId()));
        setContentShown(false);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mAdapter = onCreateAdapter();

        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = getRecyclerView();
        LayoutInflater inflater = getLayoutInflater(savedInstanceState);
        View loadingContainer = inflater.inflate(R.layout.list_loading_view, recyclerView, false);
        mLoadingView = loadingContainer.findViewById(R.id.loading);
        mLoadingView.setVisibility(View.GONE);

        mAdapter.setFooterView(loadingContainer, this);
        mAdapter.setOnItemClickListener(this);
        if (!mAdapter.isCardStyle()) {
            recyclerView.setBackgroundResource(
                    UiUtils.resolveDrawable(getActivity(), R.attr.listBackground));
        }
        recyclerView.setAdapter(mAdapter);
        updateEmptyState();
    }

    public void refresh() {
        if (getRecyclerView() != null) {
            setContentShown(false);
            getLoaderManager().getLoader(0).onContentChanged();
        }
    }

    @Override
    protected boolean hasDividers() {
        return !mAdapter.isCardStyle();
    }

    private void fillData(PageIteratorLoader<T> loader, Collection<T> data) {
        mLoadingView.setVisibility(loader.hasMoreData() ? View.VISIBLE : View.GONE);

        if (mAdapter.getCount() > 0 && !data.isEmpty() && data.iterator().next() == mAdapter.getItem(0)) {
            // there are common items, preserve them in order to keep the scroll position
            ArrayList<T> newData = new ArrayList<>();
            int index = 0, count = mAdapter.getCount();
            for (T item : data) {
                if (index < count && item == mAdapter.getItem(index++)) {
                    // we already know about the item
                    continue;
                }
                newData.add(item);
            }
            onAddData(mAdapter, newData);
        } else {
            mAdapter.clear();
            onAddData(mAdapter, data);
        }
    }

    protected void onAddData(RootAdapter<T, ? extends RecyclerView.ViewHolder> adapter, Collection<T> data) {
        adapter.addAll(data);
    }

    @Override
    public Loader<Collection<T>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<>(getActivity(), onCreateIterator());
    }

    @Override
    public void onLoadFinished(Loader<Collection<T>> loader, Collection<T> events) {
        fillData((PageIteratorLoader<T>) loader, events);
        mIsLoadCompleted = true;
        setContentShown(true);
        getActivity().supportInvalidateOptionsMenu();
        mAdapter.notifyDataSetChanged();
        if (!TextUtils.isEmpty(mCurrentFilter)) {
            mAdapter.getFilter().filter(mCurrentFilter);
        }
        updateEmptyState();
    }

    @Override
    public void onLoaderReset(Loader<Collection<T>> loader) {
    }

    @Override
    public void onScrolledToFooter() {
        if (mIsLoadCompleted && mLoadingView.getVisibility() == View.VISIBLE) {
            mIsLoadCompleted = false;
            getLoaderManager().getLoader(0).forceLoad();
        }
    }

    protected abstract int getEmptyTextResId();
    protected abstract RootAdapter<T, ? extends RecyclerView.ViewHolder> onCreateAdapter();
    protected abstract PageIterator<T> onCreateIterator();
    public abstract void onItemClick(T item);
}
