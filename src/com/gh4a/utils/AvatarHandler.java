package com.gh4a.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.egit.github.core.User;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.util.LruCache;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;
import android.widget.ImageView;

import com.gh4a.R;

public class AvatarHandler {
    private static final String TAG = "GravatarHandler";

    private static final int MAX_CACHED_IMAGE_SIZE = 60; /* dp - maximum gravatar view size used */

    private static Bitmap sDefaultAvatarBitmap;
    private static LruCache<Integer, Bitmap> sCache;
    private static int sNextRequestId = 1;

    private static class Request {
        int id;
        String url;
        ArrayList<ImageView> views;
    }
    private static final SparseArrayCompat<Request> sRequests = new SparseArrayCompat<>();
    private static int sMaxImageSizePx = -1;

    private static final int MSG_LOAD = 1;
    private static final int MSG_LOADED = 2;
    private static final int MSG_DESTROY = 3;

    private static HandlerThread sWorkerThread = null;
    private static Handler sWorkerHandler = null;

    private static Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOADED:
                    processResult(msg.arg1, (Bitmap) msg.obj);
                    if (sRequests.size() == 0) {
                        sendEmptyMessageDelayed(MSG_DESTROY, 3000);
                    }
                    break;
                case MSG_DESTROY:
                    shutdownWorker();
                    break;
            }
        }

        private void processResult(int requestId, Bitmap bitmap) {
            final Request request = sRequests.get(requestId);
            if (request != null && bitmap != null) {
                sCache.put(request.id, bitmap);

                for (ImageView view : request.views) {
                    applyAvatarToView(view, bitmap);
                }
            }
            sRequests.delete(requestId);
        }
    };

    public static void assignAvatar(ImageView view, User user) {
        if (user == null) {
            assignAvatar(view, 0, null);
            return;
        }

        assignAvatar(view, user.getId(), user.getAvatarUrl());
    }

    public static void assignAvatar(ImageView view, int userId, String url) {
        removeOldRequest(view);

        if (sCache == null) {
            initialize(view.getContext());
        }
        Bitmap bitmap = sCache.get(userId);
        if (bitmap != null) {
            applyAvatarToView(view, bitmap);
            return;
        }

        applyAvatarToView(view, sDefaultAvatarBitmap);
        if (userId <= 0) {
            return;
        }

        Request request = getRequestForId(userId);
        if (request != null) {
            request.views.add(view);
            return;
        }

        int requestId = sNextRequestId++;
        request = new Request();
        request.id = userId;
        request.url = makeUrl(url, userId);
        request.views = new ArrayList<>();
        request.views.add(view);
        sRequests.put(requestId, request);

        sHandler.removeMessages(MSG_DESTROY);
        if (sWorkerThread == null) {
            sWorkerThread = new HandlerThread("GravatarLoader");
            sWorkerThread.start();
            sWorkerHandler = new WorkerHandler(sWorkerThread.getLooper());
        }
        Message msg = sWorkerHandler.obtainMessage(MSG_LOAD,
                requestId, view.getWidth(), request.url);
        msg.sendToTarget();
    }

    private static void initialize(Context context) {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 10% of the available memory or 1MB for the cache, whatever is larger
        final int limit = Math.max(maxMemory / 10, 1024);

        sCache = new LruCache<Integer, Bitmap>(limit) {
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                oldValue.recycle();
            }

            @Override
            protected int sizeOf(Integer key, Bitmap value) {
                final long sizeInBytes;
                if (Build.VERSION.SDK_INT >= 19) {
                    sizeInBytes = value.getAllocationByteCount();
                } else {
                    sizeInBytes = value.getRowBytes() * value.getHeight();
                }
                return (int) (sizeInBytes / 1024);
            }
        };

        Resources res = context.getResources();
        sDefaultAvatarBitmap = BitmapFactory.decodeResource(res, R.drawable.default_avatar);
        sMaxImageSizePx = Math.round(res.getDisplayMetrics().density * MAX_CACHED_IMAGE_SIZE);
    }

    private static String makeUrl(String url, int userId) {
        if (url == null) {
            url = "https://avatars.githubusercontent.com/u/" + userId;
        }
        return Uri.parse(url).buildUpon()
                .appendQueryParameter("s", String.valueOf(sMaxImageSizePx))
                .toString();
    }

    private static void applyAvatarToView(ImageView view, Bitmap avatar) {
        RoundedBitmapDrawable d = RoundedBitmapDrawableFactory.create(view.getResources(), avatar);
        d.setCornerRadius(Math.max(avatar.getWidth() / 2, avatar.getHeight() / 2));
        d.setAntiAlias(true);
        view.setImageDrawable(d);
    }

    private static Request getRequestForId(int id) {
        int count = sRequests.size();
        for (int i = 0; i < count; i++) {
            Request request = sRequests.valueAt(i);
            if (request.id == id) {
                return request;
            }
        }
        return null;
    }

    private static void removeOldRequest(ImageView view) {
        int count = sRequests.size();
        for (int i = 0; i < count; i++) {
            Request request = sRequests.valueAt(i);
            if (request.views.contains(view)) {
                request.views.remove(view);
                if (request.views.isEmpty()) {
                    if (sWorkerHandler != null) {
                        sWorkerHandler.removeMessages(MSG_LOAD, request.url);
                    }
                    sRequests.delete(sRequests.keyAt(i));
                }
                return;
            }
        }
    }

    private static Bitmap fetchBitmap(String url) throws IOException {
        URL realUrl = new URL(url);
        InputStream input = realUrl.openStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read;

        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }

        byte[] data = output.toByteArray();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inJustDecodeBounds = false;

        final int widthRatio = options.outWidth / sMaxImageSizePx;
        final int heightRatio = options.outHeight / sMaxImageSizePx;
        options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

        Bitmap unscaled = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if (unscaled == null) {
            return null;
        }

        // We'll scale the image to the desired density
        unscaled.setDensity(0);

        float widthScale = (float) sMaxImageSizePx / (float) unscaled.getWidth();
        float heightScale = (float) sMaxImageSizePx / (float) unscaled.getHeight();
        float scaleFactor = Math.min(1, Math.min(widthScale, heightScale));

        Bitmap scaled = Bitmap.createScaledBitmap(unscaled,
                (int) (scaleFactor * unscaled.getWidth()),
                (int) (scaleFactor * unscaled.getHeight()), true);
        if (scaled != unscaled) {
            unscaled.recycle();
        }
        return scaled;
    }

    private static void shutdownWorker() {
        if (sWorkerThread != null) {
            sWorkerThread.getLooper().quit();
            sWorkerHandler = null;
            sWorkerThread = null;
        }
    }

    private static class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD:
                    String url = (String) msg.obj;
                    Bitmap bitmap = null;
                    try {
                        bitmap = fetchBitmap(url);
                    } catch (IOException e) {
                        Log.e(TAG, "Couldn't fetch gravatar from URL " + url, e);
                    }
                    sHandler.obtainMessage(MSG_LOADED, msg.arg1, 0, bitmap).sendToTarget();
                    break;
            }
        }
    }
}