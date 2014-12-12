package com.gh4a.fragment;

import java.util.List;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.UiUtils;

public abstract class ListDataBaseFragment<T> extends LoadingListFragmentBase implements
        LoaderCallbacks<LoaderResult<List<T>>> {
    private RootAdapter<T> mAdapter;
    private boolean mViewCreated;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView listView = getListView();
        mAdapter = onCreateAdapter();
        if (mAdapter.isCardStyle()) {
            listView.setDivider(null);
            listView.setDividerHeight(0);
            listView.setSelector(new ColorDrawable(android.R.color.transparent));

            int cardMargin = getResources().getDimensionPixelSize(R.dimen.card_margin);
            listView.setPadding(cardMargin, 0, cardMargin, 0);
        } else {
            listView.setBackgroundResource(
                    UiUtils.resolveDrawable(getActivity(), R.attr.listBackground));
        }

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
        getLoaderManager().getLoader(0).onContentChanged();
        setListAdapter(mAdapter);
    }

    protected void onAddData(RootAdapter<T> adapter, List<T> data) {
        adapter.addAll(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewCreated = true;
    }

    @Override
    public void onDestroyView() {
        mViewCreated = false;
        super.onDestroyView();
    }

    @Override
    public void onListItemClick(ListView listView, View view, final int position, long id) {
        // When the fragment is torn down, it can happen we get a click event after our
        // view is already destroyed. Catch that by checking whether our views are still valid.
        if (mViewCreated) {
            onItemClick(mAdapter.getItem(position));
        }
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult<List<T>>> loader, LoaderResult<List<T>> result) {
        if (!result.handleError(getActivity())) {
            mAdapter.clear();
            onAddData(mAdapter, result.getData());
        }
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<List<T>>> loader) {
    }

    protected abstract int getEmptyTextResId();
    protected abstract RootAdapter<T> onCreateAdapter();
    protected abstract void onItemClick(T item);
}
