package com.gh4a.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.util.SparseArray;

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

    private static SparseArray<Typeface> sTypefaces = new SparseArray<>();

    public static Typeface getTypeface(Context context, int typeface, int style) {
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
        return getTypeface(context, typeface);
    }

    public static Typeface getTypeface(Context context, int typeface) {
        if (typeface < TF_REGULAR || typeface > TF_BOLDCONDENSED) {
            return null;
        }

        Typeface tf = sTypefaces.get(typeface);
        if (tf == null) {
            AssetManager assets = context.getApplicationContext().getAssets();
            tf = Typeface.createFromAsset(assets, FONT_FILENAMES[typeface]);
            sTypefaces.put(typeface, tf);
        }

        return tf;
    }
}
