package com.gh4a.utils;

import android.graphics.Color;

import com.gh4a.R;

public class ThemeUtils {
    private static final int LIGHT_BACKGROUND_COLOR = Color.parseColor("#F7F7F9");
    private static final int DARK_BACKGROUND_COLOR = Color.parseColor("#111111");

    private static final String DARK_CSS_THEME = "dark";
    private static final String LIGHT_CSS_THEME = "light";

    public static String getCssTheme(int theme) {
        return theme == R.style.DarkTheme ? DARK_CSS_THEME : LIGHT_CSS_THEME;
    }

    public static int getWebViewBackgroundColor(int theme) {
        return theme == R.style.DarkTheme ? DARK_BACKGROUND_COLOR : LIGHT_BACKGROUND_COLOR;
    }
}
