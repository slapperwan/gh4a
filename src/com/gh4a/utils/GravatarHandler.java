package com.gh4a.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.eclipse.egit.github.core.User;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;

import com.gh4a.R;

public class GravatarHandler {
    private static final String TAG = "GravatarHandler";

    private static final LinkedHashMap<String, Bitmap> sCache = new LinkedHashMap<String, Bitmap>() {
        private static final long serialVersionUID = -3926944699325959213L;
        @Override
        protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private static final int MAX_CACHE_SIZE = 100;

    private static int sNextRequestId = 1;

    private static class Request {
        String url;
        ArrayList<ImageView> views;
    }
    private static final SparseArray<Request> sRequests = new SparseArray<Request>();

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
                    sendEmptyMessageDelayed(MSG_DESTROY, 3000);
                    break;
                case MSG_DESTROY:
                    shutdownWorker();
                    break;
            }
        }

        private void processResult(int requestId, Bitmap bitmap) {
            final Request request = sRequests.get(requestId);
            if (request != null && bitmap != null) {
                sCache.put(request.url, bitmap);
                for (ImageView view : request.views) {
                    view.setImageBitmap(bitmap);
                }
            }
            sRequests.delete(requestId);
        }
    };

    
    public static void assignGravatar(ImageView view, User user) {
        if (user == null) {
            assignGravatar(view, (String) null);
            return;
        }

        String avatarUrl = user.getAvatarUrl();
        if (!TextUtils.isEmpty(avatarUrl)) {
            assignGravatar(view, avatarUrl);
            return;
        }
        String gravatarId = user.getGravatarId();
        if (TextUtils.isEmpty(gravatarId)) {
            gravatarId = StringUtils.md5Hex(user.getEmail());
        }
        assignGravatar(view, GravatarUtils.getGravatarUrl(gravatarId));
    }
    
    public static void assignGravatar(ImageView view, String url) {
        removeOldRequest(view);

        Bitmap cachedBitmap = sCache.get(url);
        if (cachedBitmap != null) {
            view.setImageBitmap(cachedBitmap);
            return;
        }

        view.setImageResource(R.drawable.default_avatar);

        if (url == null) {
            return;
        }

        Request request = getRequestForUrl(url);
        if (request != null) {
            request.views.add(view);
            return;
        }

        int requestId = sNextRequestId++;
        request = new Request();
        request.url = url;
        request.views = new ArrayList<ImageView>();
        request.views.add(view);
        sRequests.put(requestId, request);
        
        sHandler.removeMessages(MSG_DESTROY);
        if (sWorkerThread == null) {
            sWorkerThread = new HandlerThread("GravatarLoader");
            sWorkerThread.start();
            sWorkerHandler = new WorkerHandler(sWorkerThread.getLooper());
        }
        Message msg = sWorkerHandler.obtainMessage(MSG_LOAD,
                requestId, view.getWidth(), url);
        msg.sendToTarget();
    }

    private static Request getRequestForUrl(String url) {
        int count = sRequests.size();
        for (int i = 0; i < count; i++) {
            Request request = sRequests.valueAt(i);
            if (request.url.equals(url)) {
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
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    private static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int radius) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
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
                        bitmap = getRoundedCornerBitmap(bitmap, bitmap.getWidth() / 20);
                    } catch (IOException e) {
                        Log.e(TAG, "Couldn't fetch gravatar from URL " + url, e);
                    }
                    sHandler.obtainMessage(MSG_LOADED, msg.arg1, 0, bitmap).sendToTarget();
                    break;
            }
        }
    }
}