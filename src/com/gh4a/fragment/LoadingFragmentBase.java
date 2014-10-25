package com.gh4a.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devspark.progressfragment.ProgressFragment;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class LoadingFragmentBase extends ProgressFragment implements
        SwipeRefreshLayout.ChildScrollDelegate {
    private SmoothProgressBar mProgress;

    public LoadingFragmentBase() {
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
        mProgress.setSmoothProgressDrawableColors(new int[] {
                UiUtils.resolveColor(mProgress.getContext(), R.attr.colorPrimary),
                UiUtils.resolveColor(mProgress.getContext(), R.attr.colorPrimaryDark)
        });
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
}
