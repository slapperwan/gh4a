package com.gh4a;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.gh4a.activities.BaseFragmentActivity;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public abstract class LoadingFragmentActivity extends BaseFragmentActivity implements
        SwipeRefreshLayout.OnRefreshListener {
    private View mContentContainer;
    private View mContentView;
    private View mEmptyView;
    private boolean mContentShown;
    private SmoothProgressBar mProgress;
    protected SwipeRefreshLayout mSwipeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isOnline()) {
            super.setContentView(R.layout.loading_activity);
            setupSwipeToRefresh();
        } else {
            setErrorView();
        }
    }

    @Override
    protected void onStart() {
        if (!hasErrorView()) {
            ensureContent();
            if (mContentView == null) {
                throw new IllegalStateException("Content view must be initialized before");
            }
        }
        super.onStart();
    }

    protected boolean canSwipeToRefresh() {
        return false;
    }

    @Override
    public void onRefresh() {
        mSwipeLayout.setRefreshing(true);
    }

    protected void refreshDone() {
        mSwipeLayout.setRefreshing(false);
    }

    private void setupSwipeToRefresh() {
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        if (canSwipeToRefresh()) {
            mSwipeLayout.setOnRefreshListener(this);
            mSwipeLayout.setColorSchemeColors(
                    UiUtils.resolveColor(this, R.attr.colorPrimary), 0,
                    UiUtils.resolveColor(this, R.attr.colorPrimaryDark), 0
            );
        } else {
            mSwipeLayout.setEnabled(false);
        }
    }

    @Override
    public void setContentView(int layoutResId) {
        if (layoutResId == R.layout.error) {
            super.setContentView(layoutResId);
        } else {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            View contentView = layoutInflater.inflate(layoutResId, null);
            setContentView(contentView);
        }
    }

    @Override
    public void setContentView(View view) {
        ensureContent();
        if (view == null) {
            throw new IllegalArgumentException("Content view can't be null");
        }
        if (mContentContainer instanceof ViewGroup) {
            ViewGroup contentContainer = (ViewGroup) mContentContainer;
            if (mContentView == null) {
                contentContainer.addView(view);
            } else {
                int index = contentContainer.indexOfChild(mContentView);
                // replace content view
                contentContainer.removeView(mContentView);
                contentContainer.addView(view, index);
            }
            mContentView = view;
        } else {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
    }

    protected void setEmptyText(int resId) {
        setEmptyText(getString(resId));
    }

    public void setEmptyText(CharSequence text) {
        ensureContent();
        if (mEmptyView != null && mEmptyView instanceof TextView) {
            TextView emptyView = (TextView) mEmptyView;
            emptyView.setText(text);
        } else {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
    }

    protected void setContentShown(boolean shown) {
        setContentShown(shown, true);
    }

    private void setContentShown(boolean shown, boolean animate) {
        ensureContent();
        if (mContentShown == shown) {
            return;
        }
        mContentShown = shown;
        if (shown) {
            if (animate) {
                mProgress.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mContentContainer.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            } else {
                mProgress.clearAnimation();
                mContentContainer.clearAnimation();
            }
            mProgress.setVisibility(View.GONE);
            mContentContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgress.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
                mContentContainer.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            } else {
                mProgress.clearAnimation();
                mContentContainer.clearAnimation();
            }
            mProgress.setVisibility(View.VISIBLE);
            mContentContainer.setVisibility(View.GONE);
        }
    }

    protected void setContentEmpty(boolean isEmpty) {
        ensureContent();
        if (mContentView == null) {
            throw new IllegalStateException("Content view must be initialized before");
        }
        if (isEmpty) {
            mEmptyView.setVisibility(View.VISIBLE);
            mContentView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mContentView.setVisibility(View.VISIBLE);
        }
    }

    private void ensureContent() {
        if (mContentContainer != null && mProgress != null) {
            return;
        }
        mProgress = (SmoothProgressBar) findViewById(R.id.progress);
        if (mProgress == null) {
            throw new RuntimeException("Your content must have a ViewGroup whose id attribute is 'R.id.progress'");
        }
        mProgress.setSmoothProgressDrawableColors(new int[] {
            UiUtils.resolveColor(this, R.attr.colorPrimary),
            UiUtils.resolveColor(this, R.attr.colorPrimaryDark)
        });

        mContentContainer = findViewById(R.id.content_container);
        if (mContentContainer == null) {
            throw new RuntimeException("Your content must have a ViewGroup whose id attribute is 'R.id.content_container'");
        }
        mEmptyView = findViewById(android.R.id.empty);
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
        mContentShown = true;
    }
}
