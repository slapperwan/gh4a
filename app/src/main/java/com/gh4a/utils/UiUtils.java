package com.gh4a.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.widget.IssueLabelSpan;
import com.meisolsson.githubsdk.model.Download;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.ReleaseAsset;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UiUtils {
    public static void hideImeForView(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static void showImeForView(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
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
        private final RecyclerView mView;
        private int mColor;
        private EdgeEffect mTopEffect, mBottomEffect;

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
                if (topEffect == null || bottomEffect == null) {
                    sTopEnsureMethod.invoke(mView);
                    sBottomEnsureMethod.invoke(mView);

                    mTopEffect = (EdgeEffect) sTopEffectField.get(mView);
                    mBottomEffect = (EdgeEffect) sBottomEffectField.get(mView);
                } else {
                    mTopEffect = (EdgeEffect) topEffect;
                    mBottomEffect = (EdgeEffect) bottomEffect;
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
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
            } catch (NoSuchMethodException | NoSuchFieldException e) {
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
        return view.canScrollVertically(-1);
    }

    public static Dialog createProgressDialog(Context context, @StringRes int messageResId) {
        View content = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null);
        TextView message = content.findViewById(R.id.message);

        message.setText(messageResId);
        return new AlertDialog.Builder(context)
                .setView(content)
                .create();
    }

    public static @ColorInt int resolveColor(Context context, @AttrRes int styledAttributeId) {
        TypedArray a = context.obtainStyledAttributes(new int[] {
            styledAttributeId
        });
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    private static void enqueueDownload(Context context, Uri uri, String fileName,
            String description, String mimeType, String mediaType,
            boolean wifiOnly, boolean addAuthHeader) {
        final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setDescription(description)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverRoaming(false);

        if (mediaType != null) {
            request.addRequestHeader("Accept", mediaType);
        }
        final String token = Gh4Application.get().getAuthToken();
        if (addAuthHeader && token != null) {
            request.addRequestHeader("Authorization", "Token " + token);
        }
        if (mimeType != null) {
            request.setMimeType(mimeType);
        }
        if (wifiOnly) {
            request.setAllowedOverMetered(false);
        }

        dm.enqueue(request);
    }

    public static void enqueueDownloadWithPermissionCheck(final BaseActivity activity,
            final Download download) {
        enqueueDownloadWithPermissionCheck(activity, download.htmlUrl(), download.contentType(),
                download.name(), download.description());
    }

    public static void enqueueDownloadWithPermissionCheck(final BaseActivity activity,
            final ReleaseAsset asset) {
        final ActivityCompat.OnRequestPermissionsResultCallback cb =
                (requestCode, permissions, grantResults) -> {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        enqueueDownload(activity, asset);
                    }
                };
        activity.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, cb,
                R.string.download_permission_rationale);
    }

    public static void enqueueDownloadWithPermissionCheck(final BaseActivity activity,
            final String url, final String mimeType, final String fileName, final String description) {
        final ActivityCompat.OnRequestPermissionsResultCallback cb =
                (requestCode, permissions, grantResults) -> {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        enqueueDownload(activity, url, fileName, description, mimeType, null, false);
                    }
                };
        activity.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, cb,
                R.string.download_permission_rationale);
    }

    private static void enqueueDownload(final Context context, final ReleaseAsset asset) {
        // Ugly workaround for #972 (see #976 for analysis), suggested by GH support:
        // "First, you make an API call to the endpoint for fetching an asset and you pass in the
        //  token via the Authorization header. You make this call using an HTTP library (not via
        //  the Android Download Manager) and you disable automatic following of redirects when you
        //  make that call (in case that's enabled by default). The result of that call will be a
        //  redirect response with a Location header.
        //  Second, you use the Android Download Manager to download the asset from the URL that's
        //  provided in the Location header from the response of the first step. You would not
        //  provide an Authorization header here since the required authorization is already a
        //  part of the URL."
        final OkHttpClient client = ServiceFactory.getHttpClientBuilder()
                .followRedirects(false)
                .build();
        final Request.Builder requestBuilder = new Request.Builder()
                .url(asset.url())
                .header("Accept", "application/octet-stream");
        final String token = Gh4Application.get().getAuthToken();
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Token " + token);
        }

        final Handler handler = new Handler(Looper.getMainLooper());

        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            private void completeDownload(final String url) {
                handler.post(() -> {
                    enqueueDownload(context, url, asset.name(), asset.label(), asset.contentType(),
                            "application/octet-stream", false);
                });
            }
            @Override
            public void onFailure(Call call, IOException e) {
                completeDownload(asset.url());
            }

            @Override
            public void onResponse(Call call, Response response) {
                completeDownload(response.isRedirect()
                        ? response.header("Location")
                        : call.request().url().toString());
            }
        });
    }

    private static void enqueueDownload(final Context context, String url, final String fileName,
            final String description, final String mimeType,
            final String mediaType, final boolean addAuthHeader) {
        if (url == null) {
            return;
        }

        final Uri uri = Uri.parse(url);
        if (!downloadNeedsWarning(context)) {
            enqueueDownload(context, uri, fileName, description, mimeType, mediaType, false, addAuthHeader);
            return;
        }

        DialogInterface.OnClickListener buttonListener = (dialog, which) -> {
            boolean wifiOnly = which == DialogInterface.BUTTON_NEUTRAL;
            enqueueDownload(context, uri, fileName, description, mimeType, mediaType, wifiOnly, addAuthHeader);
        };

        new AlertDialog.Builder(context)
                .setTitle(R.string.download_mobile_warning_title)
                .setMessage(R.string.download_mobile_warning_message)
                .setPositiveButton(R.string.download_now_button, buttonListener)
                .setNeutralButton(R.string.download_wifi_button, buttonListener)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean downloadNeedsWarning(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.isActiveNetworkMetered();
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

    public static CharSequence getSelectedText(TextView view) {
        int min = 0;
        int max = view.length();

        if (view.isFocused()) {
            int selectionStart = view.getSelectionStart();
            int selectionEnd = view.getSelectionEnd();

            min = Math.max(0, Math.min(selectionStart, selectionEnd));
            max = Math.max(0, Math.max(selectionStart, selectionEnd));
        }

        return view.getText().subSequence(min, max);
    }

    public static abstract class QuoteActionModeCallback implements ActionMode.Callback {
        private final TextView mView;

        public QuoteActionModeCallback(TextView view) {
            mView = view;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.comment_selection_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() != R.id.quote) {
                return false;
            }

            onTextQuoted(UiUtils.getSelectedText(mView));
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        public abstract void onTextQuoted(CharSequence text);
    }

    public static class WhitespaceTokenizer implements MultiAutoCompleteTextView.Tokenizer {
        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            while (cursor > 0 && !Character.isWhitespace(text.charAt(cursor - 1))) {
                cursor--;
            }

            return cursor;
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int len = text.length();

            while (cursor < len) {
                if (Character.isWhitespace(text.charAt(cursor))) {
                    return cursor;
                } else {
                    cursor++;
                }
            }

            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            return text;
        }
    }

    @NonNull
    public static SpannableStringBuilder formatLabelList(Context context, List<Label> labels) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (Label label : labels) {
            int pos = builder.length();
            IssueLabelSpan span = new IssueLabelSpan(context, label, true);
            builder.append(label.name());
            builder.setSpan(span, pos, pos + label.name().length(), 0);
        }
        return builder;
    }

    public static int limitViewHeight(int heightMeasureSpec, int maxHeight) {
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);

        switch (heightMode) {
            case View.MeasureSpec.AT_MOST:
            case View.MeasureSpec.EXACTLY:
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                        Math.min(heightSize, maxHeight), heightMode);
                break;
            case View.MeasureSpec.UNSPECIFIED:
                heightMeasureSpec =
                        View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST);
                break;
        }

        return heightMeasureSpec;
    }

    public static void setMenuItemText(Context context, MenuItem item, String title,
            String subtitle) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(title).append("\n");

        int start = builder.length();
        builder.append(subtitle);

        int secondaryTextColor = UiUtils.resolveColor(context, android.R.attr.textColorSecondary);
        builder.setSpan(new ForegroundColorSpan(secondaryTextColor), start, builder.length(), 0);

        item.setTitle(builder);
    }
}
