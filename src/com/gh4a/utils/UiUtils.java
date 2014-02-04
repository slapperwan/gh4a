package com.gh4a.utils;

import com.gh4a.Gh4Application;
import com.gh4a.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
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

    public static void enqueueDownload(Context context, String url, String mimeType,
            String fileName, String description) {
        Uri uri = Uri.parse(url).buildUpon()
                .appendQueryParameter("access_token", Gh4Application.get(context).getAuthToken())
                .build();
        final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final DownloadManager.Request request = new DownloadManager.Request(uri);

        request.addRequestHeader("Accept", "application/octet-stream")
                .setDescription(description)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverRoaming(false);

        if (mimeType != null) {
            request.setMimeType(mimeType);
        }

        if (!downloadNeedsWarning(context)) {
            dm.enqueue(request);
            return;
        }

        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_NEUTRAL) {
                    restrictDownloadToWifi(request);
                }
                dm.enqueue(request);
            }
        };

        createDialogBuilder(context)
                .setTitle(R.string.download_mobile_warning_title)
                .setMessage(R.string.download_mobile_warning_message)
                .setPositiveButton(R.string.download_now_button, buttonListener)
                .setNeutralButton(R.string.download_wifi_button, buttonListener)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @SuppressLint("NewApi")
    private static boolean downloadNeedsWarning(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return cm.isActiveNetworkMetered();
        }

        NetworkInfo info = cm.getActiveNetworkInfo();
        return info == null || info.getType() != ConnectivityManager.TYPE_WIFI;
    }

    @SuppressLint("NewApi")
    private static void restrictDownloadToWifi(DownloadManager.Request request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            request.setAllowedOverMetered(false);
        } else {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        }
    }
}
