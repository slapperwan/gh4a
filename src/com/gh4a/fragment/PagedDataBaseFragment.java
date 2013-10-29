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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.PageIteratorLoader;

public abstract class PagedDataBaseFragment<T> extends BaseFragment implements
        LoaderManager.LoaderCallbacks<Collection<T>>, OnItemClickListener, OnScrollListener {
    private ListView mListView;
    private RootAdapter<T> mAdapter;
    private boolean mLoadMore;
    private boolean mIsLoadCompleted;
    private TextView mLoadingView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        LayoutInflater vi = getSherlockActivity().getLayoutInflater();
        mLoadingView = (TextView) vi.inflate(R.layout.row_simple, null);
        mLoadingView.setText(R.string.loading_msg);
        mLoadingView.setTextColor(getResources().getColor(R.color.highlight));
        
        mAdapter = onCreateAdapter();
        mListView.setAdapter(mAdapter);
        
        getLoaderManager().initLoader(0, null, this);
    }

    public void refresh() {
        mLoadMore = false;
        getLoaderManager().restartLoader(0, null, this);
    }
    
    protected void fillData(Collection<T> data) {
        if (data == null || data.isEmpty()) {
            mListView.removeFooterView(mLoadingView);
            return;
        }
        if (mListView.getFooterViewsCount() == 0) {
            mListView.addFooterView(mLoadingView);
            mListView.setAdapter(mAdapter);
        }
        if (!mLoadMore) {
            mAdapter.clear();
        }
        onAddData(mAdapter, data);
        mAdapter.notifyDataSetChanged();
        if (!mLoadMore) {
            mListView.setSelection(0);
        }
    }

    protected void onAddData(RootAdapter<T> adapter, Collection<T> data) {
        adapter.addAll(data);
    }

    @Override
    public Loader<Collection<T>> onCreateLoader(int id, Bundle args) {
        return new PageIteratorLoader<T>(getSherlockActivity(), onCreateIterator()); 
    }

    @Override
    public void onLoadFinished(Loader<Collection<T>> loader, Collection<T> events) {
        mIsLoadCompleted = true;
        hideLoading();
        fillData(events);
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
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        onItemClick(mAdapter.getItem(position));
    }
    
    protected abstract RootAdapter<T> onCreateAdapter();
    protected abstract PageIterator<T> onCreateIterator();
    protected abstract void onItemClick(T item);
}