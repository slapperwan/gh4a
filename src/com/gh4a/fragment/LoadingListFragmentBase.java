package com.gh4a.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devspark.progressfragment.ProgressListFragment;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class LoadingListFragmentBase extends ProgressListFragment implements
        SwipeRefreshLayout.ChildScrollDelegate {
    public LoadingListFragmentBase() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.loading_fragment, container, false);
        ViewGroup contentContainer = (ViewGroup) view.findViewById(R.id.content_container);
        contentContainer.removeView(contentContainer.findViewById(android.R.id.empty));
        inflater.inflate(R.layout.list_fragment_list, contentContainer);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SmoothProgressBar progress = (SmoothProgressBar) view.findViewById(R.id.progress);
        progress.setSmoothProgressDrawableColors(new int[] {
                UiUtils.resolveColor(view.getContext(), R.attr.colorPrimary),
                UiUtils.resolveColor(view.getContext(), R.attr.colorPrimaryDark)
        });
    }


    @Override
    public boolean canChildScrollUp() {
        return getView() != null && UiUtils.canViewScrollUp(getListView());
    }
}
