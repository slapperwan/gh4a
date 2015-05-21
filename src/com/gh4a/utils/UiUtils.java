package com.gh4a.utils;

import java.io.File;
import java.lang.reflect.Field;

import com.gh4a.Gh4Application;
import com.gh4a.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EdgeEffect;
import android.widget.ListView;
import android.widget.TextView;

public class UiUtils {
    public static final LinkMovementMethod CHECKING_LINK_METHOD = new LinkMovementMethod() {
        @Override
        public boolean onTouchEvent(@NonNull TextView widget,
                @NonNull Spannable buffer, @NonNull MotionEvent event) {
            try {
                return super.onTouchEvent(widget, buffer, event);
            } catch (ActivityNotFoundException e) {
                ToastUtils.showMessage(widget.getContext(), R.string.link_not_openable);
                return true;
            }
        }
    };

    public static void hideImeForView(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static int textColorForBackground(Context context, int backgroundColor) {
        int red = Color.red(backgroundColor);
        int green = Color.green(backgroundColor);
        int blue = Color.blue(backgroundColor);
        int luminance = Math.round(0.213F * red + 0.715F * green + 0.072F * blue);

        if (luminance >= 128) {
            return context.getResources().getColor(R.color.label_fg_dark);
        }
        return context.getResources().getColor(R.color.label_fg_light);
    }

    public static void trySetListOverscrollColor(ListView view, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            trySetEdgeEffectColor(view, "mEdgeGlowTop", color);
            trySetEdgeEffectColor(view, "mEdgeGlowBottom", color);
        }
    }

    @TargetApi(21)
    private static void trySetEdgeEffectColor(ListView view, String fieldName, int color) {
        try {
            Field effectField = AbsListView.class.getDeclaredField(fieldName);
            effectField.setAccessible(true);
            EdgeEffect effect = (EdgeEffect) effectField.get(view);
            final int alpha = Color.alpha(effect.getColor());
            effect.setColor(Color.argb(alpha, Color.red(color),
                    Color.green(color), Color.blue(color)));
        } catch (NoSuchFieldException e) {
            // ignored
        } catch (IllegalAccessException e) {
            // ignored
        }
    }

    public static int mixColors(int startColor, int endColor, float fraction) {
        // taken from ArgbEvaluator.evaluate
        int startA = (startColor >> 24) & 0xff;
        int startR = (startColor >> 16) & 0xff;
        int startG = (startColor >> 8) & 0xff;
        int startB = startColor & 0xff;

        int endA = (endColor >> 24) & 0xff;
        int endR = (endColor >> 16) & 0xff;
        int endG = (endColor >> 8) & 0xff;
        int endB = endColor & 0xff;

        return ((startA + (int)(fraction * (endA - startA))) << 24) |
                ((startR + (int)(fraction * (endR - startR))) << 16) |
                ((startG + (int)(fraction * (endG - startG))) << 8) |
                ((startB + (int)(fraction * (endB - startB))));
    }

    public static boolean canViewScrollUp(View view) {
        if (view == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;
                if (absListView.getChildCount() == 0) {
                    return false;
                }
                return absListView.getFirstVisiblePosition() > 0
                        || absListView.getChildAt(0).getTop() < absListView.getPaddingTop();
            } else {
                return view.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(view, -1);
        }
    }

    public static AlertDialog.Builder createDialogBuilderWithAlertIcon(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder.setIconAttribute(android.R.attr.alertDialogIcon);
        } else {
            builder.setIcon(android.R.drawable.ic_dialog_alert);
        }
        return builder;
    }

    public static Context makeHeaderThemedContext(Context context) {
        int themeResId = resolveDrawable(context, R.attr.headerTheme);
        if (themeResId != 0) {
            return new ContextThemeWrapper(context, themeResId);
        }
        return context;
    }

    public static int resolveDrawable(Context context, int styledAttributeId) {
        TypedArray a = context.obtainStyledAttributes(new int[] {
            styledAttributeId
        });
        int resource = a.getResourceId(0, 0);
        a.recycle();
        return resource;
    }

    public static int resolveColor(Context context, int styledAttributeId) {
        TypedArray a = context.obtainStyledAttributes(new int[] {
            styledAttributeId
        });
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
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
            values.put("is_public_api", true);
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

    private static Uri buildDownloadDestinationUri(String fileName) {
        final File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
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
        if (url == null) {
            return;
        }

        final Uri uri = Uri.parse(url).buildUpon()
                .appendQueryParameter("access_token", Gh4Application.get().getAuthToken())
                .build();
        final Uri destinationUri = buildDownloadDestinationUri(fileName);
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

        new AlertDialog.Builder(context)
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
