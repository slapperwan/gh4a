package com.gh4a.fragment;

import java.util.List;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.RxUtils;
import com.philosophicalhacker.lib.RxLoader;

import io.reactivex.Single;

public abstract class ListDataBaseFragment<T> extends LoadingListFragmentBase {
    private RootAdapter<T, ? extends RecyclerView.ViewHolder> mAdapter;
    private RxLoader mRxLoader;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRxLoader = new RxLoader(getActivity(), getLoaderManager());
        setContentShown(false);
        loadData(false);
    }

    @Override
    public void onRefresh() {
        setContentShown(false);
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
        onCreateDataSingle()
                .compose(RxUtils::doInBackground)
                .doOnError(error -> onLoaderError(error)) // FIXME consume error if result is true
                .compose(getBaseActivity()::handleError)
                .toObservable()
                .compose(mRxLoader.makeObservableTransformer(0, force))
                .subscribe(result -> {
                    mAdapter.clear();
                    onAddData(mAdapter, result);
                    setContentShown(true);
                    updateEmptyState();
                    getActivity().invalidateOptionsMenu();
                }, error -> {});
    }

    protected abstract Single<List<T>> onCreateDataSingle();
    protected abstract RootAdapter<T, ? extends RecyclerView.ViewHolder> onCreateAdapter();

    protected boolean onLoaderError(Throwable e) {
        return false;
    }
}
