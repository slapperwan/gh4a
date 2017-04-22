package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public abstract class LoadingFragmentBase extends Fragment implements
        LoaderCallbacks.ParentCallback, SwipeRefreshLayout.ChildScrollDelegate {
    private ViewGroup mContentContainer;
    private View mContentView;
    private SmoothProgressBar mProgress;
    private final int[] mProgressColors = new int[2];
    private boolean mContentShown = true;

    public LoadingFragmentBase() {
    }

    @Override
    public BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.loading_fragment, container, false);

        mContentContainer = (ViewGroup) view.findViewById(R.id.content_container);
        mContentView = onCreateContentView(inflater, mContentContainer);
        mContentContainer.addView(mContentView);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mProgress = (SmoothProgressBar) view.findViewById(R.id.progress);
        mProgressColors[0] = UiUtils.resolveColor(mProgress.getContext(), R.attr.colorPrimary);
        mProgressColors[1] = UiUtils.resolveColor(mProgress.getContext(), R.attr.colorPrimaryDark);
        mProgress.setSmoothProgressDrawableColors(mProgressColors);
        updateContentVisibility();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContentContainer = null;
        mProgress = null;
        mProgress = null;
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(mContentView);
    }

    protected void setHighlightColors(int colorAttrId, int statusBarColorAttrId) {
        mProgressColors[0] = UiUtils.resolveColor(getActivity(), colorAttrId);
        mProgressColors[1] = UiUtils.resolveColor(getActivity(), statusBarColorAttrId);
        if (mProgress != null) {
            mProgress.invalidate();
        }
    }

    protected int getHighlightColor() {
        return mProgressColors[0];
    }

    protected void hideContentAndRestartLoaders(int... loaderIds) {
        setContentShown(false);
        LoaderManager lm = getLoaderManager();
        for (int id : loaderIds) {
            Loader loader = lm.getLoader(id);
            if (loader != null) {
                loader.onContentChanged();
            }
        }
    }

    protected boolean isContentShown() {
        return mContentShown;
    }

    protected void setContentShown(boolean shown) {
        if (mContentShown != shown) {
            mContentShown = shown;
            if (mContentContainer != null) {
                updateContentVisibility();
            }
        }
    }

    private void updateContentVisibility() {
        View out = mContentShown ? mProgress : mContentContainer;
        View in = mContentShown ? mContentContainer : mProgress;
        if (isResumed()) {
            out.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
            in.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
        } else {
            in.clearAnimation();
            out.clearAnimation();
        }
        out.setVisibility(View.GONE);
        in.setVisibility(View.VISIBLE);
    }

    protected abstract View onCreateContentView(LayoutInflater inflater, ViewGroup parent);
}
