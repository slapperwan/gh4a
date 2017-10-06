package com.gh4a.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Label;

import java.util.List;

public class LabelBadgeView extends View {
    private int[] mColors;
    private final int mBadgeSize;
    private final int mBadgeSpacing;
    private int mBadgesPerRow;
    private final Paint mBadgePaint;

    public LabelBadgeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LabelBadgeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWillNotDraw(false);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LabelBadgeView, defStyle, 0);
        mBadgeSize = a.getDimensionPixelSize(R.styleable.LabelBadgeView_badgeSize,
                getResources().getDimensionPixelSize(R.dimen.default_label_badge_size));
        mBadgeSpacing = a.getDimensionPixelSize(R.styleable.LabelBadgeView_badgeSpacing,
                getResources().getDimensionPixelSize(R.dimen.default_label_badge_spacing));

        a.recycle();

        mBadgePaint = new Paint();
        mBadgePaint.setAntiAlias(true);
    }

    public void setLabels(List<Label> labels) {
        int count = labels != null ? labels.size() : 0;
        mColors = new int[count];
        for (int i = 0; i < count; i++) {
            mColors[i] = ApiHelpers.colorForLabel(labels.get(i));
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxWidth = Integer.MAX_VALUE;

        switch (MeasureSpec.getMode(widthMeasureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                maxWidth = specWidth;
                break;
        }

        maxWidth -= getPaddingLeft() + getPaddingRight();
        mBadgesPerRow = Math.max(1, Math.min(getBadgeCount(), maxWidth / (mBadgeSize + mBadgeSpacing)));
        int rows = getBadgeRows();

        int desiredWidth = mBadgesPerRow * mBadgeSize + (mBadgesPerRow - 1) * mBadgeSpacing
                + getPaddingLeft() + getPaddingRight();
        int desiredHeight = rows * mBadgeSize + (rows - 1) * mBadgeSpacing
                + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(View.resolveSizeAndState(desiredWidth, widthMeasureSpec, 0),
                View.resolveSizeAndState(desiredHeight, heightMeasureSpec, 0));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mColors == null) {
            return;
        }

        int pl = getPaddingLeft();
        int pr = getPaddingRight();
        int pt = getPaddingTop();
        int pb = getPaddingBottom();

        int horizontalSpacing = (mBadgesPerRow - 1) * mBadgeSpacing;
        int verticalSpacing = (getBadgeRows()- 1) * mBadgeSpacing;
        float availableHorizontal = canvas.getWidth() - pl - pr - horizontalSpacing;
        float availableVertical = canvas.getHeight() - pt - pb - verticalSpacing;
        float badgeSize = Math.min(availableHorizontal / mBadgesPerRow,
                availableVertical / getBadgeRows());

        for (int row = 0, col = 0, i = 0; i < mColors.length; i++) {
            float centerX = pl + col * (mBadgeSpacing + badgeSize) + (badgeSize / 2);
            float centerY = pt + row * (mBadgeSpacing + badgeSize) + (badgeSize / 2);

            mBadgePaint.setColor(mColors[i]);
            canvas.drawCircle(centerX, centerY, badgeSize / 2, mBadgePaint);

            col++;
            if (col == mBadgesPerRow) {
                col = 0;
                row++;
            }
        }
    }

    private int getBadgeCount() {
        return mColors != null ? mColors.length : 0;
    }

    private int getBadgeRows() {
        int badges = getBadgeCount();
        if (badges == 0) {
            return 1;
        }
        return (int) Math.ceil((double) badges / mBadgesPerRow);
    }
}
