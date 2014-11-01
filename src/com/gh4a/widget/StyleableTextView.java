package com.gh4a.widget;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.gh4a.R;

import java.util.Locale;

public class StyleableTextView extends TextView {
    private static final int[] TEXT_APPEARANCE_ATTRS = new int[] {
        android.R.attr.textAppearance
    };

    private static Typeface sTypefaceRegular;
    private static Typeface sTypefaceBold;
    private static Typeface sTypefaceItalic;
    private static Typeface sTypefaceCondensed;
    private static Typeface sTypefaceBoldCondensed;

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

    private void initAttributes(Context context, AttributeSet attrs, int defStyle) {
        initTypefacesIfNeeded(context.getApplicationContext());

        Resources.Theme theme = context.getTheme();
        TypedArray appearance = null;
        Typeface typeface = sTypefaceRegular;
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
                        typeface = typefaceForEnumValue(appearance.getInt(i, -1));
                        break;
                    case R.styleable.StyleableTextView_allCaps:
                        allCaps = appearance.getBoolean(i, false);
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
                    typeface = typefaceForEnumValue(a.getInt(i, -1));
                    break;
                case R.styleable.StyleableTextView_allCaps:
                    allCaps = a.getBoolean(i, false);
                    break;
            }
        }

        a.recycle();

        setTypeface(typeface);
        if (allCaps) {
            setTransformationMethod(new AllCapsTransformationMethod(getContext()));
        }
    }

    private Typeface typefaceForEnumValue(int value) {
        switch (value) {
            case 1: return sTypefaceBold;
            case 2: return sTypefaceItalic;
            case 3: return sTypefaceCondensed;
            case 4: return sTypefaceBoldCondensed;
        }
        return sTypefaceRegular;
    }

    private void initTypefacesIfNeeded(Context context) {
        if (sTypefaceRegular != null) {
            return;
        }

        AssetManager assets = context.getAssets();
        sTypefaceRegular = Typeface.createFromAsset(assets, "fonts/Roboto-Regular.ttf");
        sTypefaceBold = Typeface.createFromAsset(assets, "fonts/Roboto-Bold.ttf");
        sTypefaceCondensed = Typeface.createFromAsset(assets, "fonts/Roboto-Condensed.ttf");
        sTypefaceBoldCondensed = Typeface.createFromAsset(assets, "fonts/Roboto-BoldCondensed.ttf");
        sTypefaceItalic = Typeface.createFromAsset(assets, "fonts/Roboto-Italic.ttf");
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
