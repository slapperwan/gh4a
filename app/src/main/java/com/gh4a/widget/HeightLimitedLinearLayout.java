package com.gh4a.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.gh4a.R;

public class HeightLimitedLinearLayout extends LinearLayout {
    private int mMaxHeight = -1;

    public HeightLimitedLinearLayout(Context context) {
        super(context);
    }

    public HeightLimitedLinearLayout(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeightLimitedLinearLayout(Context context,
            @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.HeightLimitedLinearLayout, defStyleAttr, 0);
            int n = a.getIndexCount();

            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);

                switch (attr) {
                    case R.styleable.HeightLimitedLinearLayout_maxHeight:
                        mMaxHeight = a.getDimensionPixelSize(attr, -1);
                        break;
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMaxHeight > 0) {
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);

            switch (heightMode) {
                case MeasureSpec.AT_MOST:
                case MeasureSpec.EXACTLY:
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            Math.min(heightSize, mMaxHeight), heightMode);
                    break;
                case MeasureSpec.UNSPECIFIED:
                    heightMeasureSpec =
                            MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
                    break;
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
