package com.gh4a.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

// Based on: http://stackoverflow.com/a/18167273
public class WrapContentHeightViewPager extends ViewPager {
    public WrapContentHeightViewPager(Context context) {
        super(context);
    }

    public WrapContentHeightViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int childCount = getChildCount();
        int height = 0;

        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            if (childView != null) {
                // measure the child views with the specified measure spec
                childView.measure(widthMeasureSpec, heightMeasureSpec);
            }

            height = Math.max(height, measureHeight(heightMeasureSpec, childView));
        }

        setMeasuredDimension(getMeasuredWidth(), height);
    }

    /**
     * Determines the height of this view
     *
     * @param measureSpec A measureSpec packed into an int
     * @param view the base view with already measured height
     *
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec, View view) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            // set the height from the base view if available
            if (view != null) {
                result = view.getMeasuredHeight();
            }
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }
}
