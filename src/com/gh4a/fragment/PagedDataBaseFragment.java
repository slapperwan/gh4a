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

import java.util.Collection;

import org.eclipse.egit.github.core.client.PageIterator;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.PageIteratorLoader;

public abstract class PagedDataBaseFragment<T> extends ListFragment implements
        LoaderManager.LoaderCallbacks<Collection<T>>, OnScrollListener {
    private RootAdapter<T> mAdapter;
    private boolean mLoadMore;
    private boolean mIsLoadCompleted;
    private TextView mLoadingView;
    private String mCurrentFilter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LayoutInflater vi = getActivity().getLayoutInflater();
        mLoadingView = (TextView) vi.inflate(R.layout.row_simple, null);
        mLoadingView.setText(R.string.loading_msg);
        mLoadingView.setTextColor(getResources().getColor(R.color.highlight));

        mAdapter = onCreateAdapter();

        getListView().setOnScrollListener(this);
        getListView().setTextFilterEnabled(true);
        setEmptyText(getString(getEmptyTextResId()));
        setListAdapter(mAdapter);
        setListShown(false);

        getLoaderManager().initLoader(0, null, this);
    }

    public void setFilterText(String text) {
        mCurrentFilter = text;
        if (mAdapter != null) {
            mAdapter.getFilter().filter(mCurrentFilter);
        }
    }

    public void refresh() {
        mLoadMore = false;
        setListShown(false);
        getLoaderManager().getLoader(0).onContentChanged();
    }

    private void fillData(Collection<T> data) {
        ListView listView = getListView();
        if (data == null || data.isEmpty()) {
            listView.removeFooterView(mLoadingView);
            return;
        }
        if (getListView().getFooterViewsCount() == 0) {
            listView.addFooterView(mLoadingView);
            setListAdapter(mAdapter);
        }
        if (!mLoadMore) {
            mAdapter.clear();
        }
        onAddData(mAdapter, data);
        return;
    }

    protected void onAddData(RootAdapter<T> adapter, Collection<T> data) {
        adapter.addAll(data);
    }

    @Override
    public Loader<Collection<T>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<T>(getActivity(), onCreateIterator());
    }

    @Override
    public void onLoadFinished(Loader<Collection<T>> loader, Collection<T> events) {
        fillData(events);
        mIsLoadCompleted = true;
        setListShown(true);
        getActivity().invalidateOptionsMenu();
        mAdapter.notifyDataSetChanged();
        if (!TextUtils.isEmpty(mCurrentFilter)) {
            mAdapter.getFilter().filter(mCurrentFilter);
        }
    }

    @Override
    public void onLoaderReset(Loader<Collection<T>> loader) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
        boolean loadMore = firstVisible + visibleCount >= totalCount;

        if (loadMore && mIsLoadCompleted) {
            mLoadMore = true;
            mIsLoadCompleted = false;
            getLoaderManager().getLoader(0).forceLoad();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        onItemClick(mAdapter.getItem(position));
    }

    protected abstract int getEmptyTextResId();
    protected abstract RootAdapter<T> onCreateAdapter();
    protected abstract PageIterator<T> onCreateIterator();
    protected abstract void onItemClick(T item);
}