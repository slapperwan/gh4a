package com.gh4a.fragment;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderResult;

public abstract class ListDataBaseFragment<T> extends ListFragment implements
        LoaderCallbacks<LoaderResult<List<T>>> {
    private RootAdapter<T> mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = onCreateAdapter();

        int emptyResId = getEmptyTextResId();
        if (emptyResId != 0) {
            setEmptyText(getString(emptyResId));
        }

        setListAdapter(mAdapter);
        setListShown(false);
        
        getLoaderManager().initLoader(0, null, this);
    }

    public void refresh() {
        setListShown(false);
        mAdapter.clear();
        getLoaderManager().restartLoader(0, null, this);
    }

    protected void onAddData(RootAdapter<T> adapter, List<T> data) {
        adapter.addAll(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView listView, View view, final int position, long id) {
        onItemClick(mAdapter.getItem(position));
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult<List<T>>> loader, LoaderResult<List<T>> result) {
        if (!result.handleError(getActivity())) {
            onAddData(mAdapter, result.getData());
        }
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<List<T>>> loader) {
    }

    protected abstract int getEmptyTextResId();
    protected abstract RootAdapter<T> onCreateAdapter();
    protected abstract void onItemClick(T item);
}
