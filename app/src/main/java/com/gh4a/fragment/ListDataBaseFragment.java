package com.gh4a.fragment;

import java.util.List;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;

public abstract class ListDataBaseFragment<T> extends LoadingListFragmentBase {
    private RootAdapter<T, ? extends RecyclerView.ViewHolder> mAdapter;

    private final LoaderCallbacks<List<T>> mLoaderCallback = new LoaderCallbacks<List<T>>(this) {
        @Override
        protected Loader<LoaderResult<List<T>>> onCreateLoader() {
            return ListDataBaseFragment.this.onCreateLoader();
        }

        @Override
        protected void onResultReady(List<T> result) {
            mAdapter.clear();
            onAddData(mAdapter, result);
            setContentShown(true);
            updateEmptyState();
            getActivity().invalidateOptionsMenu();
        }

        @Override
        protected boolean onError(Exception e) {
            return onLoaderError(e);
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentShown(false);
        getLoaderManager().initLoader(0, null, mLoaderCallback);
    }

    @Override
    public void onRefresh() {
        hideContentAndRestartLoaders(0);
        if (mAdapter != null) {
            mAdapter.clear();
        }
    }

    @Override
    protected boolean hasDividers() {
        return !mAdapter.isCardStyle();
    }

    protected void onAddData(RootAdapter<T, ?> adapter, List<T> data) {
        adapter.addAll(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        mAdapter = onCreateAdapter();
        view.setAdapter(mAdapter);
        updateEmptyState();
    }

    @Override
    protected boolean hasCards() {
        return mAdapter.isCardStyle();
    }

    protected abstract Loader<LoaderResult<List<T>>> onCreateLoader();
    protected abstract RootAdapter<T, ? extends RecyclerView.ViewHolder> onCreateAdapter();

    protected boolean onLoaderError(Exception e) {
        return false;
    }
}
