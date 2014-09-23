package com.gh4a.utils;

import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

import com.gh4a.R;

public class ToastUtils {
    public static void showError(Context context) {
        showMessage(context, R.string.error_toast);

    }

    public static void showMessage(Context context, int resId) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show();
    }

    public static void notFoundMessage(Context context, int stringId) {
        notFoundMessage(context, context.getString(stringId));
    }

    public static void notFoundMessage(Context context, String object) {
        Resources res = context.getResources();
        Toast.makeText(context, res.getString(R.string.record_not_found, object),
                Toast.LENGTH_SHORT).show();
    }
}
