package com.gh4a.utils;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.meisolsson.githubsdk.model.Download;
import com.meisolsson.githubsdk.model.ReleaseAsset;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadUtils {
    // Legacy Android DownloadManager cannot reliably fetch GitHub raw files.
    // Reuse the content already provided by the GitHub API when possible.
    public static void downloadRepositoryFileWithPermissionCheck(final BaseActivity activity,
            final String base64Content, final String rawUrl, final String path) {
        final String mimeType = FileUtils.getMimeTypeFor(path);
        final String fileName = FileUtils.getFileName(path);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            enqueueDownloadWithPermissionCheck(activity, rawUrl, mimeType, fileName, null);
            return;
        }
        handleDownloadPermissionCheck(activity, () -> {
            if (!TextUtils.isEmpty(base64Content)) {
                new Thread(() -> {
                    try {
                        byte[] data = Base64.decode(base64Content, Base64.DEFAULT);
                        saveBytesToDownloads(activity.getApplicationContext(), data, fileName, mimeType);
                    } catch (IllegalArgumentException e) {
                        showDownloadToast(activity.getApplicationContext(), "Download failed: invalid file content");
                    }
                }).start();
            } else {
                enqueueLegacyHttpDownload(activity, rawUrl, fileName, mimeType, null, true);
            }
        });
    }

    public static void enqueueDownloadWithPermissionCheck(final BaseActivity activity,
            final Download download) {
        enqueueDownloadWithPermissionCheck(activity, download.htmlUrl(), download.contentType(),
                download.name(), download.description());
    }

    public static void enqueueDownloadWithPermissionCheck(final BaseActivity activity,
            final ReleaseAsset asset) {
        handleDownloadPermissionCheck(activity, () -> enqueueDownload(activity, asset));
    }

    public static void enqueueDownloadWithPermissionCheck(final BaseActivity activity,
            final String url, final String mimeType, final String fileName, final String description) {
        handleDownloadPermissionCheck(activity,
                () -> enqueueDownload(activity, url, fileName, description, mimeType, null, false));
    }

    private static void handleDownloadPermissionCheck(final BaseActivity activity, Runnable func) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+ doesn't require WRITE_EXTERNAL_STORAGE permission anymore,
            // since we're writing to a pre-defined public directory only
            func.run();
            return;
        }
        final ActivityCompat.OnRequestPermissionsResultCallback cb =
                (requestCode, permissions, grantResults) -> {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        func.run();
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

    private static void enqueueLegacyHttpDownload(Context context, String url, String fileName,
            String mimeType, String mediaType, boolean addAuthHeader) {
        final Context appContext = context.getApplicationContext();
        final Request.Builder requestBuilder = new Request.Builder().url(url);

        if (mediaType != null) {
            requestBuilder.header("Accept", mediaType);
        }
        final String token = Gh4Application.get().getAuthToken();
        if (addAuthHeader && token != null) {
            requestBuilder.header("Authorization", "Token " + token);
        }

        final OkHttpClient client = ServiceFactory.getHttpClientBuilder().build();
        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                showDownloadToast(appContext, "Download failed: " + describeException(e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response closeableResponse = response) {
                    if (!closeableResponse.isSuccessful()) {
                        showDownloadToast(appContext,
                                "Download failed: HTTP " + closeableResponse.code() + " "
                                        + closeableResponse.message());
                        return;
                    }

                    ResponseBody body = closeableResponse.body();
                    if (body == null) {
                        showDownloadToast(appContext, "Download failed: empty response body");
                        return;
                    }

                    try (InputStream in = body.byteStream()) {
                        saveStreamToDownloads(appContext, in, fileName, mimeType);
                    }
                } catch (IOException | RuntimeException e) {
                    showDownloadToast(appContext, "Download failed: " + describeException(e));
                }
            }
        });
    }

    private static void saveBytesToDownloads(Context context, byte[] data, String fileName,
            String mimeType) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                showDownloadToast(context, "Download failed: cannot open Downloads");
                return;
            }

            File destination = new File(downloadsDir, fileName);
            File temporary = new File(downloadsDir, fileName + ".part");
            if (temporary.exists() && !temporary.delete()) {
                showDownloadToast(context, "Download failed: cannot remove stale .part file");
                return;
            }

            try (FileOutputStream out = new FileOutputStream(temporary)) {
                out.write(data);
                out.flush();
            }

            finishSavedFile(context, temporary, destination, fileName, mimeType);
        } catch (IOException | RuntimeException e) {
            showDownloadToast(context, "Download failed: " + describeException(e));
        }
    }

    private static void saveStreamToDownloads(Context context, InputStream in, String fileName,
            String mimeType) throws IOException {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
            showDownloadToast(context, "Download failed: cannot open Downloads");
            return;
        }

        File destination = new File(downloadsDir, fileName);
        File temporary = new File(downloadsDir, fileName + ".part");
        if (temporary.exists() && !temporary.delete()) {
            showDownloadToast(context, "Download failed: cannot remove stale .part file");
            return;
        }

        try (FileOutputStream out = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
        }

        finishSavedFile(context, temporary, destination, fileName, mimeType);
    }

    private static void finishSavedFile(Context context, File temporary, File destination,
            String fileName, String mimeType) {
        if (destination.exists() && !destination.delete()) {
            temporary.delete();
            showDownloadToast(context, "Download failed: cannot replace existing file");
            return;
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete();
            showDownloadToast(context, "Download failed: rename .part failed");
            return;
        }

        registerCompletedDownload(context, destination, fileName, mimeType);
        showDownloadToast(context, "Downloaded to Downloads/" + fileName);
    }

    @SuppressWarnings("deprecation")
    private static void registerCompletedDownload(Context context, File file, String fileName,
            String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return;
        }
        try {
            DownloadManager dm =
                    (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            long id = dm.addCompletedDownload(fileName, fileName, true,
                    mimeType != null ? mimeType : "application/octet-stream",
                    file.getAbsolutePath(), file.length(), true);
            if (id < 0) {
                showDownloadToast(context, "Saved, but Downloads registration failed");
            }
        } catch (RuntimeException e) {
            showDownloadToast(context,
                    "Saved, but Downloads registration failed: " + describeException(e));
        }
    }

    private static String describeException(Throwable error) {
        String name = error.getClass().getSimpleName();
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return name;
        }
        return name + ": " + message;
    }

    private static void showDownloadToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(
                () -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
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
}
