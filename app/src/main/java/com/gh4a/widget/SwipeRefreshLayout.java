package com.gh4a.widget;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.NestedScrollingChildHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class SwipeRefreshLayout extends android.support.v4.widget.SwipeRefreshLayout
        implements AppBarLayout.OnOffsetChangedListener {
    public interface ChildScrollDelegate {
        boolean canChildScrollUp();
    }

    private final int mTouchSlop;
    private float mDownX;
    private float mDownY;
    private boolean mChildScrollableOnDown;
    private final int[] mParentOffsetInWindow = new int[2];
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private boolean mHorizontalSwipe;
    private boolean mIsOrWasUpSwipe;
    private ChildScrollDelegate mChildScrollDelegate;
    private boolean mAppBarCanScrollUp;


    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    public void setAppBarLayout(AppBarLayout abl) {
        abl.addOnOffsetChangedListener(this);
    }

    public void setChildScrollDelegate(ChildScrollDelegate delegate) {
        mChildScrollDelegate = delegate;
    }

    @Override
    public boolean canChildScrollUp() {
        if (mAppBarCanScrollUp) {
            return true;
        }
        if (mChildScrollDelegate != null) {
            return mChildScrollDelegate.canChildScrollUp();
        }
        return super.canChildScrollUp();
    }

    @Override
    public void onOffsetChanged(AppBarLayout abl, int verticalOffset) {
        mAppBarCanScrollUp = verticalOffset != 0;
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        // Leave nested scrolling up to CoordinatorLayout
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN && shouldPreventRefresh()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                mHorizontalSwipe = false;
                mIsOrWasUpSwipe = false;
                mChildScrollableOnDown = canChildScrollUp();
                break;
            case MotionEvent.ACTION_MOVE:
                final float xDiff = Math.abs(event.getX() - mDownX);
                final float yDiff = event.getY() - mDownY;

                if (yDiff < -mTouchSlop) {
                    mIsOrWasUpSwipe = true;
                }
                if (mHorizontalSwipe || xDiff > mTouchSlop) {
                    mHorizontalSwipe = true;
                    return false;
                }
                break;
        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        if (mNestedScrollingChildHelper != null) {
            mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
        }
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        if (shouldPreventRefresh()) {
            dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                    mParentOffsetInWindow);
        } else {
            super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
        }
    }

    private boolean shouldPreventRefresh() {
        return mChildScrollableOnDown || mIsOrWasUpSwipe;
    }
}