package com.gh4a.utils;

import android.content.Context;
import android.widget.Toast;

import com.gh4a.R;

public class ToastUtils {
    public static void showError(Context context) {
        showMessage(context, R.string.error_toast);

    }

    public static void showMessage(Context context, int resId) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show();
    }
}
