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
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PageIteratorLoader;

import java.util.ArrayList;
import java.util.Collection;

public abstract class PagedDataBaseFragment<T> extends LoadingListFragmentBase implements
        RootAdapter.OnItemClickListener<T>, RootAdapter.OnScrolledToFooterListener {
    private RootAdapter<T, ? extends RecyclerView.ViewHolder> mAdapter;
    // private PageIteratorWithSaveableState<T> mIterator;
    private boolean mIsLoadCompleted;
    private View mLoadingView;

    private static final String STATE_KEY_ITERATOR_STATE = "iterator_state";

    private final LoaderCallbacks<PageIteratorLoader<T>.LoadedPage> mLoaderCallback =
            new LoaderCallbacks<PageIteratorLoader<T>.LoadedPage>(this) {
        @Override
        protected Loader<LoaderResult<PageIteratorLoader<T>.LoadedPage>> onCreateLoader() {
            return PagedDataBaseFragment.this.onCreateLoader();
        }

        @Override
        protected void onResultReady(PageIteratorLoader<T>.LoadedPage result) {
            fillData(result.results, result.hasMoreData);
            mIsLoadCompleted = true;
            setContentShown(true);
            mAdapter.notifyDataSetChanged();
            updateEmptyState();
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentShown(false);

        /*
        mIterator = onCreateIterator();
        if (savedInstanceState != null) {
            mIterator.restoreState(savedInstanceState.getBundle(STATE_KEY_ITERATOR_STATE));
        }*/

        getLoaderManager().initLoader(0, null, mLoaderCallback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        /*
        if (mIterator != null) {
            outState.putBundle(STATE_KEY_ITERATOR_STATE, mIterator.saveState());
        }*/
    }

    @Override
    public void onRefresh() {
        if (mAdapter != null) {
            mAdapter.clear();
        }
        mIsLoadCompleted = false;
        hideContentAndRestartLoaders(0);
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        mAdapter = onCreateAdapter();

        mLoadingView = inflater.inflate(R.layout.list_loading_view, view, false);
        mAdapter.setFooterView(mLoadingView, this);
        mAdapter.setOnItemClickListener(this);
        view.setAdapter(mAdapter);
        updateEmptyState();
    }

    @Override
    protected boolean hasDividers() {
        return !mAdapter.isCardStyle();
    }

    @Override
    protected boolean hasCards() {
        return mAdapter.isCardStyle();
    }

    private void fillData(Collection<T> data, boolean hasMoreData) {
        mLoadingView.setVisibility(hasMoreData ? View.VISIBLE : View.GONE);

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
    public void onScrolledToFooter() {
        if (mIsLoadCompleted && mLoadingView.getVisibility() == View.VISIBLE) {
            mIsLoadCompleted = false;
            getLoaderManager().getLoader(0).forceLoad();
        }
    }

    protected abstract RootAdapter<T, ? extends RecyclerView.ViewHolder> onCreateAdapter();
    protected abstract PageIteratorLoader<T> onCreateLoader();
    public abstract void onItemClick(T item);
}
