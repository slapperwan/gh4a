package com.gh4a.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;
import com.philosophicalhacker.lib.RxLoader;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import io.reactivex.SingleTransformer;

public abstract class LoadingFragmentBase extends Fragment implements
        BaseActivity.RefreshableChild, SwipeRefreshLayout.ChildScrollDelegate {
    private ViewGroup mContentContainer;
    private View mContentView;
    private SmoothProgressBar mProgress;
    private final int[] mProgressColors = new int[2];
    private boolean mContentShown = true;
    private RxLoader mRxLoader;

    public LoadingFragmentBase() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mRxLoader = new RxLoader(context, LoaderManager.getInstance(this));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.loading_fragment, container, false);

        mContentContainer = view.findViewById(R.id.content_container);
        mContentView = onCreateContentView(inflater, mContentContainer);
        mContentContainer.addView(mContentView);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mProgress = view.findViewById(R.id.progress);
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

    protected BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    protected <T> SingleTransformer<T, T> makeLoaderSingle(int id, boolean force) {
        return upstream -> upstream
                .compose(RxUtils::doInBackground)
                .compose(mRxLoader.makeSingleTransformer(id, force));
    }

    protected void handleLoadFailure(Throwable error) {
        BaseActivity activity = getBaseActivity();
        if (activity != null) {
            activity.handleLoadFailure(error);
        }
    }

    protected void handleActionFailure(String text, Throwable error) {
        BaseActivity activity = getBaseActivity();
        if (activity != null) {
            activity.handleActionFailure(text, error);
        }
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

    public boolean onBackPressed() {
        return false;
    }
}
