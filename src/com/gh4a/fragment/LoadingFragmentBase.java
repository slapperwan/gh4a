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

        SmoothProgressBar progress = (SmoothProgressBar) view.findViewById(R.id.progress);
        progress.setSmoothProgressDrawableColors(new int[] {
            UiUtils.resolveColor(progress.getContext(), R.attr.colorPrimary),
            UiUtils.resolveColor(progress.getContext(), R.attr.colorPrimaryDark)
        });
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(getContentView());
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
