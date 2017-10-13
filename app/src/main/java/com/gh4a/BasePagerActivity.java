package com.gh4a;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EdgeEffect;
import android.widget.LinearLayout;

import com.gh4a.utils.UiUtils;

import java.lang.reflect.Field;

public abstract class BasePagerActivity extends BaseActivity implements
        ViewPager.OnPageChangeListener {
    private PagerAdapter mAdapter;
    private TabLayout mTabs;
    private ViewPager mPager;
    private int[][] mTabHeaderColors;
    private boolean mScrolling;
    private boolean mErrorViewVisible;
    private int mCurrentHeaderColor;
    private int mLastTabCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_pager);
        mPager = setupPager();

        mCurrentHeaderColor = UiUtils.resolveColor(this, R.attr.colorPrimary);
        updateTabHeaderColors();
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
        updateTabHeaderColors();
        if (mTabHeaderColors != null) {
            onPageMoved(0, 0);
        } else {
            int[] colorAttrs = getHeaderColorAttrs();
            if (colorAttrs != null) {
                transitionHeaderToColor(colorAttrs[0], colorAttrs[1]);
            } else {
                transitionHeaderToColor(R.attr.colorPrimary, R.attr.colorPrimaryDark);
            }
        }
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

    private void updateTabHeaderColors() {
        int[][] colorAttrs = getTabHeaderColorAttrs();
        if (colorAttrs == null) {
            mTabHeaderColors = null;
            return;
        }

        mTabHeaderColors = new int[colorAttrs.length][2];
        for (int i = 0; i < mTabHeaderColors.length; i++) {
            mTabHeaderColors[i][0] = UiUtils.resolveColor(this, colorAttrs[i][0]);
            mTabHeaderColors[i][1] = UiUtils.resolveColor(this, colorAttrs[i][1]);
        }
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

    @Override
    protected void setHeaderColor(int color, int statusBarColor) {
        super.setHeaderColor(color, statusBarColor);
        mCurrentHeaderColor = color;
    }

    @Override
    protected void transitionHeaderToColor(int colorAttrId, int statusBarColorAttrId) {
        super.transitionHeaderToColor(colorAttrId, statusBarColorAttrId);
        mCurrentHeaderColor = UiUtils.resolveColor(this, colorAttrId);
    }

    protected abstract PagerAdapter createAdapter(ViewGroup root);

    /* expected format: int[tabCount][2] - 0 is header, 1 is status bar */
    protected int[][] getTabHeaderColorAttrs() {
        return null;
    }

    /* expected format: int[2] - 0 is header, 1 is status bar */
    protected int[] getHeaderColorAttrs() {
        return null;
    }

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
        if (!mScrolling) {
            tryUpdatePagerColor();
        }
    }

    protected void onPageMoved(int position, float fraction) {
        if (mTabHeaderColors != null) {
            int nextIndex = Math.max(0, Math.min(position + 1, mTabHeaderColors.length - 1));
            int headerColor = UiUtils.mixColors(mTabHeaderColors[position][0],
                    mTabHeaderColors[nextIndex][0], fraction);
            int statusBarColor = UiUtils.mixColors(mTabHeaderColors[position][1],
                    mTabHeaderColors[nextIndex][1], fraction);
            setHeaderColor(headerColor, statusBarColor);
        }
    }

    private void tryUpdatePagerColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewPagerEdgeColorHelper helper =
                    (ViewPagerEdgeColorHelper) mPager.getTag(R.id.EdgeColorHelper);
            if (helper == null) {
                helper = new ViewPagerEdgeColorHelper(mPager);
                mPager.setTag(R.id.EdgeColorHelper, helper);
            }
            helper.setColor(mCurrentHeaderColor);
        }
    }

    @TargetApi(21)
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
                    Object leftEffect = sLeftEffectField.get(mPager);
                    Object rightEffect = sRightEffectField.get(mPager);

                    final Field edgeField = leftEffect.getClass().getDeclaredField("mEdgeEffect");
                    edgeField.setAccessible(true);

                    mLeftEffect = (EdgeEffect) edgeField.get(leftEffect);
                    mRightEffect = (EdgeEffect) edgeField.get(rightEffect);
                } catch (IllegalAccessException | NoSuchFieldException e) {
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
