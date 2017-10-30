package com.gh4a.fragment;

import java.util.List;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import com.gh4a.adapter.RootAdapter;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

public abstract class ListDataBaseFragment<T> extends LoadingListFragmentBase {
    private RootAdapter<T, ? extends RecyclerView.ViewHolder> mAdapter;
    private Disposable mSubscription;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentShown(false);
        loadData(false);
    }

    @Override
    public void onRefresh() {
        setContentShown(false);
        if (mSubscription != null) {
            mSubscription.dispose();
        }
        loadData(true);
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

    private void loadData(boolean force) {
        mSubscription = onCreateDataSingle()
                .compose(makeLoaderSingle(0, force))
                .subscribe(result -> {
                    mAdapter.clear();
                    onAddData(mAdapter, result);
                    setContentShown(true);
                    updateEmptyState();
                }, error -> {});
    }

    protected abstract Single<List<T>> onCreateDataSingle();
    protected abstract RootAdapter<T, ? extends RecyclerView.ViewHolder> onCreateAdapter();
}
