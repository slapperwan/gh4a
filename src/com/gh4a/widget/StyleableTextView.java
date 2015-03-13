package com.gh4a.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.text.method.LinkMovementMethod;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.TypefaceCache;
import com.gh4a.utils.UiUtils;

import java.util.Locale;

public class StyleableTextView extends TextView {
    private static final int[] TEXT_APPEARANCE_ATTRS = new int[] {
        android.R.attr.textAppearance
    };

    private int mTypefaceValue = TypefaceCache.TF_REGULAR;

    public StyleableTextView(Context context) {
        super(context, null);
    }

    public StyleableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0);
    }

    public StyleableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttributes(context, attrs, defStyle);
    }

    public int getTypefaceValue() {
        return mTypefaceValue;
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyle) {
        Resources.Theme theme = context.getTheme();
        TypedArray appearance = null;
        boolean allCaps = false;

        if (attrs != null) {
            TypedArray a = theme.obtainStyledAttributes(attrs, TEXT_APPEARANCE_ATTRS, defStyle, 0);
            int ap = a.getResourceId(0, -1);
            if (ap != -1) {
                appearance = theme.obtainStyledAttributes(ap, R.styleable.StyleableTextView);
            }
            a.recycle();
        }

        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);

                switch (attr) {
                    case R.styleable.StyleableTextView_font:
                        mTypefaceValue = appearance.getInt(attr, -1);
                        break;
                    case R.styleable.StyleableTextView_allCaps:
                        allCaps = appearance.getBoolean(attr, false);
                        break;
                }
            }
        }

        TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.StyleableTextView, defStyle, 0);
        int n = a.getIndexCount();

        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);

            switch (attr) {
                case R.styleable.StyleableTextView_font:
                    mTypefaceValue = a.getInt(attr, -1);
                    break;
                case R.styleable.StyleableTextView_allCaps:
                    allCaps = a.getBoolean(attr, false);
                    break;
            }
        }

        a.recycle();

        if (!isInEditMode()) {
            setTypeface(TypefaceCache.getTypeface(getContext(), mTypefaceValue));
        }
        if (allCaps) {
            setTransformationMethod(new AllCapsTransformationMethod(getContext()));
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if (getMovementMethod() == LinkMovementMethod.getInstance()) {
            setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        }
    }

    private static class AllCapsTransformationMethod implements TransformationMethod {
        private final Locale mLocale;

        public AllCapsTransformationMethod(Context context) {
            mLocale = context.getResources().getConfiguration().locale;
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return source != null ? source.toString().toUpperCase(mLocale) : null;
        }

        @Override
        public void onFocusChanged(View view, CharSequence sourceText, boolean focused,
                int direction, Rect previouslyFocusedRect) {
        }
    }
}
