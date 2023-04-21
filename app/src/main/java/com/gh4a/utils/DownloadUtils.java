package com.gh4a.utils;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.meisolsson.githubsdk.model.Download;
import com.meisolsson.githubsdk.model.ReleaseAsset;

import java.io.IOException;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadUtils {
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

        new MaterialAlertDialogBuilder(context)
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
}
