package com.gh4a.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.style.ReplacementSpan;

import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.Label;

public class IssueLabelSpan extends ReplacementSpan {
    private final RectF mTmpRect = new RectF();
    private final int mFgColor;
    private final int mBgColor;
    private final int mPadding;
    private final int mRightAndBottomMargin;
    private final float mTextSize;

    private int mAscent;
    private int mDescent;

    public IssueLabelSpan(Context context, Label label, boolean withMargin) {
        super();
        mBgColor = ApiHelpers.colorForLabel(label);
        mFgColor = UiUtils.textColorForBackground(context, mBgColor);

        Resources res = context.getResources();
        mPadding = res.getDimensionPixelSize(R.dimen.issue_label_padding);
        mRightAndBottomMargin =
                withMargin ? res.getDimensionPixelSize(R.dimen.issue_label_margin) : 0;
        mTextSize = res.getDimension(R.dimen.issue_label_text_size);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text,
            int start, int end, Paint.FontMetricsInt fm) {
        paint.setTextSize(mTextSize);
        if (fm != null) {
            paint.getFontMetricsInt(fm);
            mAscent = -fm.ascent;
            mDescent = fm.descent;
            fm.top -= mPadding;
            fm.ascent -= mPadding;
            fm.bottom += mPadding + mRightAndBottomMargin;
            fm.descent += mPadding + mRightAndBottomMargin;
        }

        float textSize = paint.measureText(text, start, end);
        return (int) Math.ceil(textSize) + 2 * mPadding + mRightAndBottomMargin;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
            float x, int top, int y, int bottom, @NonNull Paint paint) {
        paint.setTextSize(mTextSize);
        float textSize = paint.measureText(text, start, end);

        final float bgLeft = x;
        final float bgRight = bgLeft + textSize + 2 * mPadding;
        final float bgTop = y - mAscent - mPadding;
        final float bgBottom = y + mDescent + mPadding;
        final float cornerRadius = mPadding;

        paint.setColor(mBgColor);
        mTmpRect.set(bgLeft, bgTop, bgRight, bgBottom);
        canvas.drawRoundRect(mTmpRect, cornerRadius, cornerRadius, paint);

        paint.setColor(mFgColor);
        canvas.drawText(text, start, end, x + mPadding, y, paint);
    }
}
