package com.gh4a.utils;

import java.io.File;

import com.gh4a.Gh4Application;
import com.gh4a.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentValues;
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

    private static void enqueueDownload(Context context, Uri uri, Uri destinationUri,
            String description, String mimeType, String mediaType, boolean wifiOnly) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(uri);

            request.setDestinationUri(destinationUri);
            request.setDescription(description);
            if (mediaType != null) {
                request.addRequestHeader("Accept", mediaType);
            }
            if (mimeType != null) {
                request.setMimeType(mimeType);
            }
            if (wifiOnly) {
                restrictDownloadToWifi(request);
            }
            request.setAllowedOverRoaming(false);

            dm.enqueue(request);
        } else {
            // HACK alert:
            // Gingerbread's DownloadManager needlessly rejected HTTPS URIs. Circumvent that
            // by building and enqueing the request to the provider by ourselves. This is safe
            // as we only rely on internal API that won't change anymore.
            ContentValues values = new ContentValues();
            values.put("uri", uri.toString());
            values.put("is_public_api", "true");
            values.put("notificationpackage", context.getPackageName());
            values.put("destination", 4);
            values.put("hint", destinationUri.toString());
            if (mediaType != null) {
                values.put("http_header_0", "Accept:" + mediaType);
            }
            values.put("description", description);
            if (mimeType != null) {
                values.put("mimetype", mimeType);
            }
            values.put("visibility", 0);
            values.put("allowed_network_types", wifiOnly ? DownloadManager.Request.NETWORK_WIFI : ~0);
            values.put("allow_roaming", false);
            values.put("is_visible_in_downloads_ui", true);

            context.getContentResolver().insert(Uri.parse("content://downloads/my_downloads"), values);
        }
    }

    private static Uri buildDownloadDestinationUri(Context context, String fileName) {
        final File file = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (file == null) {
            return null;
        }
        if (file.exists()) {
            if (!file.isDirectory()) {
                return null;
            }
        } else if (!file.mkdirs()) {
            return null;
        }
        return Uri.withAppendedPath(Uri.fromFile(file), fileName);
    }

    public static void enqueueDownload(final Context context, String url, final String mimeType,
            final String fileName, final String description, final String mediaType) {
        final Uri uri = Uri.parse(url).buildUpon()
                .appendQueryParameter("access_token", Gh4Application.get(context).getAuthToken())
                .build();
        final Uri destinationUri = buildDownloadDestinationUri(context, fileName);
        if (destinationUri == null) {
            ToastUtils.showMessage(context, R.string.download_fail_no_storage_toast);
            return;
        }

        if (!downloadNeedsWarning(context)) {
            enqueueDownload(context, uri, destinationUri, description, mimeType, mediaType, false);
            return;
        }

        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean wifiOnly = which == DialogInterface.BUTTON_NEUTRAL;
                enqueueDownload(context, uri, destinationUri, description,
                        mimeType, mediaType, wifiOnly);
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
