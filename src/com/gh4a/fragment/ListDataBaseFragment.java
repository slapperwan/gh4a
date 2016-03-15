package com.gh4a.fragment;

import java.util.List;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.UiUtils;

public abstract class ListDataBaseFragment<T> extends LoadingListFragmentBase implements
        RootAdapter.OnItemClickListener<T> {
    private RootAdapter<T, ? extends RecyclerView.ViewHolder> mAdapter;

    private LoaderCallbacks<List<T>> mLoaderCallback = new LoaderCallbacks<List<T>>(this) {
        @Override
        protected Loader<LoaderResult<List<T>>> onCreateLoader() {
            return ListDataBaseFragment.this.onCreateLoader();
        }

        @Override
        protected void onResultReady(List<T> result) {
            mAdapter.clear();
            onAddData(mAdapter, result);
            if (isResumed()) {
                setContentShown(true);
            } else {
                setContentShownNoAnimation(true);
            }
            updateEmptyState();
            getActivity().supportInvalidateOptionsMenu();
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mAdapter = onCreateAdapter();
        mAdapter.setOnItemClickListener(this);

        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = getRecyclerView();
        if (!mAdapter.isCardStyle()) {
            recyclerView.setBackgroundResource(
                    UiUtils.resolveDrawable(getActivity(), R.attr.listBackground));
        }
        recyclerView.setAdapter(mAdapter);

        int emptyResId = getEmptyTextResId();
        if (emptyResId != 0) {
            setEmptyText(getString(emptyResId));
        }

        updateEmptyState();
    }

    protected abstract Loader<LoaderResult<List<T>>> onCreateLoader();
    protected abstract int getEmptyTextResId();
    protected abstract RootAdapter<T, ? extends RecyclerView.ViewHolder> onCreateAdapter();
    public abstract void onItemClick(T item);
}
