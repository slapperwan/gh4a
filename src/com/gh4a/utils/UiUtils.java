package com.gh4a.utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UiUtils {
    public static final LinkMovementMethod CHECKING_LINK_METHOD = new LinkMovementMethod() {
        @Override
        public boolean onTouchEvent(@NonNull TextView widget,
                @NonNull Spannable buffer, @NonNull MotionEvent event) {
            try {
                return super.onTouchEvent(widget, buffer, event);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(widget.getContext(), R.string.link_not_openable, Toast.LENGTH_LONG)
                        .show();
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
            return ContextCompat.getColor(context, R.color.label_fg_dark);
        }
        return ContextCompat.getColor(context, R.color.label_fg_light);
    }

    public static void trySetListOverscrollColor(RecyclerView view, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RecyclerViewEdgeColorHelper helper =
                    (RecyclerViewEdgeColorHelper) view.getTag(R.id.EdgeColorHelper);
            if (helper == null) {
                helper = new RecyclerViewEdgeColorHelper(view);
                view.setTag(R.id.EdgeColorHelper, helper);
            }
            helper.setColor(color);
        }
    }

    @TargetApi(21)
    private static class RecyclerViewEdgeColorHelper implements ViewTreeObserver.OnGlobalLayoutListener {
        private RecyclerView mView;
        private int mColor;
        private EdgeEffect mTopEffect, mBottomEffect;
        private Object mLastTopEffect, mLastBottomEffect;

        private static Method sTopEnsureMethod, sBottomEnsureMethod;
        private static Field sTopEffectField, sBottomEffectField;

        public RecyclerViewEdgeColorHelper(RecyclerView view) {
            mView = view;
            mColor = 0;
            mView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
        public void setColor(int color) {
            mColor = color;
            applyIfPossible();
        }
        @Override
        public void onGlobalLayout() {
            applyIfPossible();
        }

        private void applyIfPossible() {
            if (!ensureStaticMethodsAndFields()) {
                return;
            }
            try {
                Object topEffect = sTopEffectField.get(mView);
                Object bottomEffect = sBottomEffectField.get(mView);
                if (topEffect == null || bottomEffect == null
                        || topEffect != mLastTopEffect || bottomEffect != mLastBottomEffect) {
                    sTopEnsureMethod.invoke(mView);
                    sBottomEnsureMethod.invoke(mView);
                    mLastTopEffect = sTopEffectField.get(mView);
                    mLastBottomEffect = sBottomEffectField.get(mView);

                    final Field edgeField = mLastTopEffect.getClass().getDeclaredField("mEdgeEffect");
                    edgeField.setAccessible(true);
                    mTopEffect = (EdgeEffect) edgeField.get(mLastTopEffect);
                    mBottomEffect = (EdgeEffect) edgeField.get(mLastBottomEffect);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                mTopEffect = mBottomEffect = null;
            } catch (NoSuchFieldException e) {
                mTopEffect = mBottomEffect = null;
            }
            applyColor(mTopEffect);
            applyColor(mBottomEffect);
        }

        private void applyColor(EdgeEffect effect) {
            if (effect != null) {
                final int alpha = Color.alpha(effect.getColor());
                effect.setColor(Color.argb(alpha, Color.red(mColor),
                        Color.green(mColor), Color.blue(mColor)));
            }
        }

        private boolean ensureStaticMethodsAndFields() {
            if (sTopEnsureMethod != null && sBottomEnsureMethod != null) {
                return true;
            }
            try {
                sTopEnsureMethod = RecyclerView.class.getDeclaredMethod("ensureTopGlow");
                sTopEnsureMethod.setAccessible(true);
                sBottomEnsureMethod = RecyclerView.class.getDeclaredMethod("ensureBottomGlow");
                sBottomEnsureMethod.setAccessible(true);
                sTopEffectField = RecyclerView.class.getDeclaredField("mTopGlow");
                sTopEffectField.setAccessible(true);
                sBottomEffectField = RecyclerView.class.getDeclaredField("mBottomGlow");
                sBottomEffectField.setAccessible(true);
                return true;
            } catch (NoSuchMethodException e) {
                // ignored
            } catch (NoSuchFieldException e) {
                // ignored
            }
            return false;
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

    // FIXME: Remove this and use setDestinationInExternalPublicDir() when removing GB compatibility
    //        (and re-check whether WRITE_EXTERNAL_STORAGE permission can be dropped when doing that)
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

    public static void enqueueDownloadWithPermissionCheck(final BaseActivity activity,
            final String url, final String mimeType, final String fileName,
            final String description, final String mediaType) {
        final ActivityCompat.OnRequestPermissionsResultCallback cb =
                new ActivityCompat.OnRequestPermissionsResultCallback() {
            @Override
            public void onRequestPermissionsResult(int requestCode,
                    @NonNull String[] permissions, @NonNull int[] grantResults) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enqueueDownload(activity, url, mimeType, fileName, description, mediaType);
                }
            }
        };
        activity.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, cb,
                R.string.download_permission_rationale);
    }

    private static void enqueueDownload(final Context context, String url, final String mimeType,
            final String fileName, final String description, final String mediaType) {
        if (url == null) {
            return;
        }

        final Uri uri = Uri.parse(url).buildUpon()
                .appendQueryParameter("access_token", Gh4Application.get().getAuthToken())
                .build();
        final Uri destinationUri = buildDownloadDestinationUri(fileName);
        if (destinationUri == null) {
            Toast.makeText(context, R.string.download_fail_no_storage_toast, Toast.LENGTH_LONG)
                    .show();
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
    public static boolean downloadNeedsWarning(Context context) {
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

    public static abstract class EmptinessWatchingTextWatcher implements TextWatcher {
        public EmptinessWatchingTextWatcher(EditText editor) {
            afterTextChanged(editor.getText());
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        @Override
        public void afterTextChanged(Editable s) {
            onIsEmpty(s == null || s.length() == 0);
        }
        public abstract void onIsEmpty(boolean isEmpty);
    }

    public static class ButtonEnableTextWatcher extends EmptinessWatchingTextWatcher {
        private View mView;
        private MenuItem mItem;

        public ButtonEnableTextWatcher(EditText editor, View view) {
            super(editor);
            mView = view;
            afterTextChanged(editor.getText());
        }

        public ButtonEnableTextWatcher(EditText editor, MenuItem item) {
            super(editor);
            mItem = item;
            afterTextChanged(editor.getText());
        }

        @Override
        public void onIsEmpty(boolean isEmpty) {
            if (mView != null) {
                mView.setEnabled(!isEmpty);
            } else if (mItem != null) {
                mItem.setEnabled(!isEmpty);
            }
        }
    }
}
