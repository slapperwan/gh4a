package com.gh4a;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EdgeEffect;
import android.widget.LinearLayout;

import java.lang.reflect.Field;

public abstract class BasePagerActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener {
    private PagerAdapter mAdapter;
    private TabLayout mTabs;
    private ViewPager mPager;
    private boolean mScrolling;
    private boolean mErrorViewVisible;
    private int mLastTabCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_pager);
        mPager = setupPager();

        updateTabVisibility();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        onPageMoved(0, 0);
    }

    protected void adjustTabsForHeaderAlignedFab(boolean fabPresent) {
        int margin = fabPresent
                ? getResources().getDimensionPixelSize(R.dimen.mini_fab_size_with_margin) : 0;

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mTabs.getLayoutParams();
        lp.rightMargin = margin;
        mTabs.setLayoutParams(lp);
    }

    protected void invalidatePages() {
        mAdapter.notifyDataSetChanged();
        updateTabVisibility();
    }

    protected void invalidateTabs() {
        invalidatePages();
        onPageMoved(0, 0);
        tryUpdatePagerColor();
    }

    protected ViewPager getPager() {
        return mPager;
    }

    protected boolean hasTabsInToolbar() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void setErrorViewVisibility(boolean visible, Throwable e) {
        mErrorViewVisible = visible;
        updateTabVisibility();
        super.setErrorViewVisibility(visible, e);
    }

    private ViewPager setupPager() {
        ViewPager pager = findViewById(R.id.pager);
        mAdapter = createAdapter(pager);
        pager.setAdapter(mAdapter);
        pager.addOnPageChangeListener(this);

        mTabs = (TabLayout) getLayoutInflater().inflate(R.layout.tab_bar, null);
        mTabs.setupWithViewPager(pager);

        if (hasTabsInToolbar()) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.addView(mTabs, new Toolbar.LayoutParams(
                    Toolbar.LayoutParams.WRAP_CONTENT,
                    Toolbar.LayoutParams.MATCH_PARENT));
        } else {
            addHeaderView(mTabs, false);
        }

        return pager;
    }

    private void updateToolbarScrollableState() {
        setToolbarScrollable(mAdapter.getCount() > 1 && !hasTabsInToolbar());
    }

    private void updateTabVisibility() {
        int count = mAdapter.getCount();

        if (count != mLastTabCount) {
            mAdapter.notifyDataSetChanged();
            mLastTabCount = count;
        }

        // We never have many pages, make sure to keep them all alive
        mPager.setOffscreenPageLimit(Math.max(1, count - 1));

        mTabs.setVisibility(count > 1 && !mErrorViewVisible ? View.VISIBLE : View.GONE);
        updateToolbarScrollableState();

        LinearLayout tabStrip = (LinearLayout) mTabs.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View tab = tabStrip.getChildAt(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tab.getLayoutParams();
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            lp.weight = 1;
            tab.setLayoutParams(lp);
        }
    }

    protected abstract PagerAdapter createAdapter(ViewGroup root);

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        onPageMoved(position, positionOffset);
    }

    @Override
    public void onPageSelected(int position) {
        if (!mScrolling) {
            onPageMoved(position, 0);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrolling = state != ViewPager.SCROLL_STATE_IDLE;
    }

    protected void onPageMoved(int position, float fraction) {}

    private void tryUpdatePagerColor() {
        ViewPagerEdgeColorHelper helper =
                (ViewPagerEdgeColorHelper) mPager.getTag(R.id.EdgeColorHelper);
        if (helper == null) {
            helper = new ViewPagerEdgeColorHelper(mPager);
            mPager.setTag(R.id.EdgeColorHelper, helper);
        }
    }

    private static class ViewPagerEdgeColorHelper {
        private final ViewPager mPager;
        private int mColor;
        private EdgeEffect mLeftEffect, mRightEffect;

        private static Field sLeftEffectField, sRightEffectField;

        public ViewPagerEdgeColorHelper(ViewPager pager) {
            mPager = pager;
            mColor = 0;
        }
        public void setColor(int color) {
            mColor = color;
            applyIfPossible();
        }

        private void applyIfPossible() {
            if (!ensureStaticFields()) {
                return;
            }
            if (mLeftEffect == null || mRightEffect == null) {
                try {
                    mLeftEffect = (EdgeEffect) sLeftEffectField.get(mPager);
                    mRightEffect = (EdgeEffect) sRightEffectField.get(mPager);
                } catch (IllegalAccessException e) {
                    mLeftEffect = mRightEffect = null;
                }
            }
            applyColor(mLeftEffect);
            applyColor(mRightEffect);
        }

        private void applyColor(EdgeEffect effect) {
            if (effect != null) {
                final int alpha = Color.alpha(effect.getColor());
                effect.setColor(Color.argb(alpha, Color.red(mColor),
                        Color.green(mColor), Color.blue(mColor)));
            }
        }

        private boolean ensureStaticFields() {
            if (sLeftEffectField != null && sRightEffectField != null) {
                return true;
            }
            try {
                sLeftEffectField = ViewPager.class.getDeclaredField("mLeftEdge");
                sLeftEffectField.setAccessible(true);
                sRightEffectField = ViewPager.class.getDeclaredField("mRightEdge");
                sRightEffectField.setAccessible(true);
                return true;
            } catch (NoSuchFieldException e) {
                // ignored
            }
            return false;
        }
    }
}
