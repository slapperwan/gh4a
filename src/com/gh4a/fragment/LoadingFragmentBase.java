package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devspark.progressfragment.ProgressFragment;
import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public abstract class LoadingFragmentBase extends ProgressFragment implements
        LoaderCallbacks.ParentCallback, SwipeRefreshLayout.ChildScrollDelegate {
    private SmoothProgressBar mProgress;
    private int[] mProgressColors = new int[2];

    public LoadingFragmentBase() {
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

        mProgress = (SmoothProgressBar) view.findViewById(R.id.progress);
        mProgressColors[0] = UiUtils.resolveColor(mProgress.getContext(), R.attr.colorPrimary);
        mProgressColors[1] = UiUtils.resolveColor(mProgress.getContext(), R.attr.colorPrimaryDark);
        mProgress.setSmoothProgressDrawableColors(mProgressColors);
    }

    @Override
    public void onDestroyView() {
        mProgress = null;
        super.onDestroyView();
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(getContentView());
    }

    protected void setProgressColors(int color, int statusBarColor) {
        mProgressColors[0] = color;
        mProgressColors[1] = statusBarColor;
        mProgress.invalidate();
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
