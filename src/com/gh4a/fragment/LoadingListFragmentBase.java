package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devspark.progressfragment.ProgressFragment;
import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;
import com.gh4a.widget.SwipeRefreshLayout;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public abstract class LoadingListFragmentBase extends ProgressFragment implements
        LoaderCallbacks.ParentCallback, SwipeRefreshLayout.ChildScrollDelegate {
    private RecyclerView mRecyclerView;
    private SmoothProgressBar mProgress;
    private int[] mProgressColors = new int[2];

    public interface OnRecyclerViewCreatedListener {
        void onRecyclerViewCreated(Fragment fragment, RecyclerView recyclerView);
    }

    public LoadingListFragmentBase() {

    }

    @Override
    public BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.loading_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setContentView(R.layout.generic_list);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        if (hasDividers()) {
            mRecyclerView.addItemDecoration(new DividerItemDecoration(view.getContext()));
        }

        if (getActivity() instanceof OnRecyclerViewCreatedListener) {
            ((OnRecyclerViewCreatedListener) getActivity()).onRecyclerViewCreated(this, mRecyclerView);
        }

        mProgress = (SmoothProgressBar) view.findViewById(R.id.progress);
        mProgressColors[0] = UiUtils.resolveColor(mProgress.getContext(), R.attr.colorPrimary);
        mProgressColors[1] = UiUtils.resolveColor(mProgress.getContext(), R.attr.colorPrimaryDark);
        mProgress.setSmoothProgressDrawableColors(mProgressColors);
    }

    @Override
    public boolean canChildScrollUp() {
        return getView() != null && UiUtils.canViewScrollUp(mRecyclerView);
    }

    protected void updateEmptyState() {
        RecyclerView.Adapter<?> adapter = mRecyclerView != null
                ? mRecyclerView.getAdapter() : null;
        if (adapter instanceof RootAdapter) {
            // don't count headers and footers
            setContentEmpty(((RootAdapter) adapter).getCount() == 0);
        } else if (adapter != null) {
            setContentEmpty(adapter.getItemCount() == 0);
        }
    }

    protected RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    protected boolean hasDividers() {
        return true;
    }

    protected void setHighlightColors(int colorAttrId, int statusBarColorAttrId) {
        mProgressColors[0] = UiUtils.resolveColor(getActivity(), colorAttrId);
        mProgressColors[1] = UiUtils.resolveColor(getActivity(), statusBarColorAttrId);
        mProgress.invalidate();
        UiUtils.trySetListOverscrollColor(mRecyclerView, mProgressColors[0]);
    }

    protected void hideContentAndRestartLoaders(int... loaderIds) {
        if (getView() != null) {
            setContentShown(false);
        }
        LoaderManager lm = getLoaderManager();
        for (int id : loaderIds) {
            Loader loader = lm.getLoader(id);
            if (loader != null) {
                loader.onContentChanged();
            }
        }
    }
}
