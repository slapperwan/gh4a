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

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.client.PageIterator;

import java.util.ArrayList;
import java.util.Collection;

public abstract class PagedDataBaseFragment<T> extends LoadingListFragmentBase implements
        LoaderManager.LoaderCallbacks<Collection<T>>, OnScrollListener {
    private RootAdapter<T> mAdapter;
    private boolean mIsLoadCompleted;
    private View mLoadingView;
    private String mCurrentFilter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LayoutInflater vi = getActivity().getLayoutInflater();
        View loadingContainer = vi.inflate(R.layout.list_loading_view, null);
        mLoadingView = loadingContainer.findViewById(R.id.loading);
        mLoadingView.setVisibility(View.GONE);

        ListView listView = getListView();
        mAdapter = onCreateAdapter();
        if (mAdapter.isCardStyle()) {
            listView.setDivider(null);
            listView.setDividerHeight(0);
            listView.setSelector(new ColorDrawable(0));

            int cardMargin = getResources().getDimensionPixelSize(R.dimen.card_margin);
            listView.setPadding(cardMargin, 0, cardMargin, 0);
        } else {
            listView.setBackgroundResource(
                    UiUtils.resolveDrawable(getActivity(), R.attr.listBackground));
        }

        listView.setOnScrollListener(this);
        listView.setTextFilterEnabled(true);
        listView.addFooterView(loadingContainer);
        setEmptyText(getString(getEmptyTextResId()));
        setListAdapter(mAdapter);
        setListShown(false);

        getLoaderManager().initLoader(0, null, this);
    }

    public void refresh() {
        if (getListView() != null) {
            setListShown(false);
            getLoaderManager().getLoader(0).onContentChanged();
        }
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

    protected void onAddData(RootAdapter<T> adapter, Collection<T> data) {
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
        setListShown(true);
        getActivity().supportInvalidateOptionsMenu();
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
        boolean loadMore = firstVisible > 0 && visibleCount > 0 &&
                getListView().getFooterViewsCount() != 0 &&
                firstVisible + visibleCount >= totalCount;

        if (loadMore && mIsLoadCompleted) {
            mIsLoadCompleted = false;
            getLoaderManager().getLoader(0).forceLoad();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if (position < mAdapter.getCount()) {
            onItemClick(mAdapter.getItem(position));
        }
    }

    protected abstract int getEmptyTextResId();
    protected abstract RootAdapter<T> onCreateAdapter();
    protected abstract PageIterator<T> onCreateIterator();
    protected abstract void onItemClick(T item);
}
