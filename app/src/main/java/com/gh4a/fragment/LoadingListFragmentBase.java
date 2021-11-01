package com.gh4a.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;
import com.gh4a.widget.SwipeRefreshLayout;
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller;

public abstract class LoadingListFragmentBase extends LoadingFragmentBase implements
        BaseActivity.RefreshableChild, SwipeRefreshLayout.ChildScrollDelegate {
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private NestedScrollView mEmptyViewContainer;
    private RecyclerFastScroller mFastScroller;

    public interface OnRecyclerViewCreatedListener {
        void onRecyclerViewCreated(Fragment fragment, RecyclerView recyclerView);
    }

    public LoadingListFragmentBase() {

    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, ViewGroup parent) {
        View view = inflater.inflate(R.layout.list_fragment_content, parent, false);

        mEmptyViewContainer = view.findViewById(R.id.empty_view_container);
        TextView emptyView = view.findViewById(android.R.id.empty);
        int emptyTextResId = getEmptyTextResId();
        if (emptyTextResId != 0) {
            emptyView.setText(emptyTextResId);
        }

        mLayoutManager = new LinearLayoutManager(view.getContext());
        mRecyclerView = view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(mLayoutManager);
        onRecyclerViewInflated(mRecyclerView, inflater);
        if (hasDividers()) {
            mRecyclerView.addItemDecoration(new DividerItemDecoration(view.getContext()));
        }
        if (!hasCards()) {
            mRecyclerView.setBackgroundResource(R.color.list_background);
        }

        mFastScroller = view.findViewById(R.id.fast_scroller);
        mFastScroller.attachRecyclerView(mRecyclerView);
        mFastScroller.setVisibility(View.VISIBLE);

        BaseActivity activity = getBaseActivity();
        if (activity.hasRightDrawer()) {
            // Prevent the right drawer from being accidentally opened while fast scrolling
            mFastScroller.setOnHandleTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        activity.setRightDrawerLockedClosed(true);
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        activity.setRightDrawerLockedClosed(false);
                        break;
                }

                return false;
            });
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof OnRecyclerViewCreatedListener) {
            ((OnRecyclerViewCreatedListener) getActivity()).onRecyclerViewCreated(this, mRecyclerView);
        }
    }

    @Override
    public boolean canChildScrollUp() {
        return getView() != null && UiUtils.canViewScrollUp(mRecyclerView);
    }

    protected void updateEmptyState() {
        if (mRecyclerView == null) {
            return;
        }

        boolean empty = false;
        RecyclerView.Adapter<?> adapter = mRecyclerView.getAdapter();
        if (adapter instanceof RootAdapter) {
            // don't count headers and footers
            empty = ((RootAdapter) adapter).getCount() == 0;
        } else if (adapter != null) {
            empty = adapter.getItemCount() == 0;
        }

        mRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        mEmptyViewContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
    }

    protected void scrollToAndHighlightPosition(final int position) {
        getBaseActivity().collapseAppBar();
        mLayoutManager.scrollToPositionWithOffset(position, 0);
        final RecyclerView.Adapter<?> adapter = mRecyclerView.getAdapter();
        if (adapter instanceof RootAdapter) {
            mRecyclerView.postDelayed(() -> ((RootAdapter) adapter).highlight(position), 600);
        }
    }

    protected boolean hasDividers() {
        return true;
    }
    protected boolean hasCards() { return false; }

    @Override
    protected void setHighlightColors(int colorAttrId, int statusBarColorAttrId) {
        super.setHighlightColors(colorAttrId, statusBarColorAttrId);
        UiUtils.trySetListOverscrollColor(mRecyclerView, getHighlightColor());
        mFastScroller.setHandlePressedColor(getHighlightColor());
    }

    protected abstract int getEmptyTextResId();
}
