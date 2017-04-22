package com.gh4a.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;

public class VerticalTextView extends StyleableTextView {
    private final boolean mTopDown;

    public VerticalTextView(Context context, AttributeSet attrs) {
       super(context, attrs);

       final int gravity = getGravity();
       if (Gravity.isVertical(gravity) && (gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
          setGravity((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
          mTopDown = false;
       } else {
          mTopDown = true;
       }
    }

    @Override
    @SuppressWarnings("SuspiciousNameCombination")
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
       super.onMeasure(heightMeasureSpec, widthMeasureSpec);
       setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        Layout layout = getLayout();
        if (layout == null) {
            return;
        }

        TextPaint textPaint = getPaint();
        textPaint.setColor(getCurrentTextColor());
        textPaint.drawableState = getDrawableState();

        canvas.save();

        if (mTopDown) {
            canvas.translate(getWidth(), 0);
            canvas.rotate(90);
        } else {
            canvas.translate(0, getHeight());
            canvas.rotate(-90);
        }

        canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());
        layout.draw(canvas);

        canvas.restore();
    }
}
