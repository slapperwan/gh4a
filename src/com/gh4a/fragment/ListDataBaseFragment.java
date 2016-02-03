package com.gh4a.fragment;

import java.util.List;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.UiUtils;

public abstract class ListDataBaseFragment<T> extends LoadingListFragmentBase implements
        LoaderCallbacks<LoaderResult<List<T>>>, RootAdapter.OnItemClickListener<T> {
    private RootAdapter<T, ? extends RecyclerView.ViewHolder> mAdapter;
    private boolean mViewCreated;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    public void refresh() {
        setContentShown(false);
        mAdapter.clear();
        getLoaderManager().getLoader(0).onContentChanged();
        getRecyclerView().setAdapter(mAdapter);
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

        mViewCreated = true;
        updateEmptyState();
    }

    @Override
    public void onDestroyView() {
        mViewCreated = false;
        super.onDestroyView();
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult<List<T>>> loader, LoaderResult<List<T>> result) {
        if (!result.handleError(getActivity())) {
            mAdapter.clear();
            onAddData(mAdapter, result.getData());
        }
        if (isResumed()) {
            setContentShown(true);
        } else {
            setContentShownNoAnimation(true);
        }
        updateEmptyState();
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<List<T>>> loader) {
    }

    protected abstract int getEmptyTextResId();
    protected abstract RootAdapter<T, ? extends RecyclerView.ViewHolder> onCreateAdapter();
    public abstract void onItemClick(T item);
}
