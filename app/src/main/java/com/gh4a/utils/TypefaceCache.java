package com.gh4a.utils;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Build;
import android.util.SparseArray;

import com.gh4a.Gh4Application;

public class TypefaceCache {
    public static final int TF_REGULAR = 0;
    public static final int TF_MEDIUM = 1;
    public static final int TF_BOLD = 2;
    public static final int TF_ITALIC = 3;
    public static final int TF_CONDENSED = 4;
    public static final int TF_BOLDCONDENSED = 5;

    private static final String[] FONT_FILENAMES = new String[] {
        "fonts/Roboto-Regular.ttf",
        "fonts/Roboto-Medium.ttf",
        "fonts/Roboto-Bold.ttf",
        "fonts/Roboto-Italic.ttf",
        "fonts/Roboto-Condensed.ttf",
        "fonts/Roboto-BoldCondensed.ttf"
    };

    private static final String[] FONT_FAMILIES = new String[] {
        "sans-serif",
        "sans-serif-medium",
        "sans-serif",
        "sans-serif",
        "sans-serif-condensed",
        "sans-serif-condensed"
    };
    private static final int[] FONT_STYLES = new int[] {
        Typeface.NORMAL,
        Typeface.NORMAL,
        Typeface.BOLD,
        Typeface.ITALIC,
        Typeface.NORMAL,
        Typeface.BOLD
    };

    private static final SparseArray<Typeface> sTypefaces = new SparseArray<>();

    public static Typeface getTypeface(int typeface, int style) {
        switch (style) {
            case Typeface.BOLD:
                switch (typeface) {
                    case TF_REGULAR: typeface = TF_BOLD; break;
                    case TF_CONDENSED: typeface = TF_BOLDCONDENSED; break;
                }
                break;
            case Typeface.ITALIC:
                switch (typeface) {
                    case TF_REGULAR: typeface = TF_ITALIC; break;
                }
                break;
        }
        return getTypeface(typeface);
    }

    public static Typeface getTypeface(int typeface) {
        if (typeface < TF_REGULAR || typeface > TF_BOLDCONDENSED) {
            return null;
        }

        Typeface tf = sTypefaces.get(typeface);
        if (tf == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // L has all typefaces we need, use system fonts
                tf = Typeface.create(FONT_FAMILIES[typeface], FONT_STYLES[typeface]);
            } else {
                // use our fonts
                AssetManager assets = Gh4Application.get().getAssets();
                tf = Typeface.createFromAsset(assets, FONT_FILENAMES[typeface]);
            }
            sTypefaces.put(typeface, tf);
        }

        return tf;
    }
}
