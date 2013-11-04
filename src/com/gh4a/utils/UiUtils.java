package com.gh4a.utils;

import com.gh4a.Gh4Application;
import com.gh4a.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class UiUtils {
    public static void hideImeForView(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static AlertDialog.Builder createDialogBuilder(Context context) {
        int dialogTheme = Gh4Application.THEME == R.style.DefaultTheme ?
                R.style.Theme_Sherlock_Dialog : R.style.Theme_Sherlock_Light_Dialog;
        return new AlertDialog.Builder(new ContextThemeWrapper(context, dialogTheme));
    }

    public static void assignTypeface(Activity parent, Typeface typeface, int[] textViewIds) {
        View decor = parent.getWindow() != null ? parent.getWindow().getDecorView() : null;
        assignTypeface(decor, typeface, textViewIds);
    }

    public static void assignTypeface(View parent, Typeface typeface, int[] textViewIds) {
        if (parent == null) {
            return;
        }
        for (int id : textViewIds) {
            TextView textView = (TextView) parent.findViewById(id);
            textView.setTypeface(typeface);
        }
    }
    
    public static int textColorForBackground(Context context, int backgroundColor) {
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);
        int min = Math.min(red, Math.min(green, blue));
        int max = Math.max(red, Math.min(green, blue));
        int luminance = (min + max) / 2;

        if (luminance >= 128) {
            return context.getResources().getColor(R.color.abs__primary_text_holo_light);
        }
        return context.getResources().getColor(R.color.abs__primary_text_holo_dark);
    }

    public static int resolveDrawable(Context context, int styledAttributeId) {
        TypedArray a = context.getTheme().obtainStyledAttributes(Gh4Application.THEME, new int[] {
            styledAttributeId
        });
        int resource = a.getResourceId(0, 0);
        a.recycle();
        return resource;
    }
}
