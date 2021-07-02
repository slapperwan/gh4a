/*
 * Copyright 2012 GitHub Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.graphics.drawable.DrawableWrapper;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.fragment.SettingsFragment;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pl.droidsonroids.gif.GifDrawable;

public class HttpImageGetter {
    private static class GifCallback implements Drawable.Callback {
        private final List<WeakReference<TextView>> mViewRefs;
        private final Handler mHandler = new Handler();

        public GifCallback(List<WeakReference<TextView>> viewRefs) {
            mViewRefs = viewRefs;
        }

        @Override
        public void invalidateDrawable(@NonNull Drawable drawable) {
            for (WeakReference<TextView> ref : mViewRefs) {
                TextView view = ref.get();
                if (view != null) {
                    view.invalidate();
                    // make sure the TextView's display list is regenerated
                    boolean enabled = view.isEnabled();
                    view.setEnabled(!enabled);
                    view.setEnabled(enabled);
                }
            }
        }

        @Override
        public void scheduleDrawable(@NonNull Drawable drawable,
                                     @NonNull Runnable runnable, long when) {
            mHandler.postAtTime(runnable, when);
        }

        @Override
        public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
            mHandler.removeCallbacks(runnable);
        }
    }

    private static class GifInfo {
        final WeakReference<GifDrawable> mDrawable;
        final GifCallback mCallback;

        public GifInfo(GifDrawable d, List<WeakReference<TextView>> viewRefs) {
            mCallback = new GifCallback(viewRefs);
            mDrawable = new WeakReference<>(d);
            d.setCallback(mCallback);
        }
        public void destroy() {
            GifDrawable drawable = mDrawable.get();
            if (drawable != null) {
                drawable.setCallback(null);
                drawable.stop();
                drawable.recycle();
            }
        }
    }

    // interface just used for tracking purposes
    private static class LoadedBitmapDrawable extends BitmapDrawable {
        public LoadedBitmapDrawable(Resources res, Bitmap bitmap) {
            super(res, bitmap);
        }
    }

    private static class PlaceholderDrawable extends DrawableWrapper implements Runnable {
        private final String mUrl;
        private final ObjectInfo mInfo;
        private Drawable mLoadedImage;

        public PlaceholderDrawable(String url, ObjectInfo info, Drawable placeholder) {
            super(placeholder);
            setBounds(0, 0, placeholder.getIntrinsicWidth(), placeholder.getIntrinsicHeight());
            mUrl = url;
            mInfo = info;
        }

        public String getUrl() {
            return mUrl;
        }

        public void addLoadedImage(Drawable image, Handler handler) {
            synchronized (this) {
                mLoadedImage = image;
                handler.post(this);
            }
        }

        @Override
        public void run() {
            setWrappedDrawable(mLoadedImage);
            setBounds(0, 0, mLoadedImage.getIntrinsicWidth(), mLoadedImage.getIntrinsicHeight());
            mInfo.invalidateViewsForNewDrawable();
        }
    }

    private class ObjectInfo implements ImageGetter {
        private final ArrayList<WeakReference<TextView>> mViewRefs = new ArrayList<>();
        private final List<GifInfo> mGifs = new ArrayList<>();
        private final List<WeakReference<Bitmap>> mBitmaps = new ArrayList<>();

        private CharSequence mHtml;
        private ImageGetterAsyncTask mTask;
        private boolean mHasStartedImageLoad;
        private boolean mResumed = true;

        void bind(TextView view, String html) {
            addView(view);

            if (mHtml == null) {
                encode(view.getContext(), html);
            }

            apply(mHtml);

            if (!mHasStartedImageLoad) {
                ImageSpan[] spans = getImageSpans();
                if (spans.length > 0) {
                    ArrayList<PlaceholderDrawable> imagesToLoad = new ArrayList<>();
                    for (ImageSpan span : spans) {
                        Drawable d = span.getDrawable();
                        if (d instanceof PlaceholderDrawable) {
                            imagesToLoad.add((PlaceholderDrawable) d);
                        }
                    }
                    mTask = new ImageGetterAsyncTask(HttpImageGetter.this, this);
                    mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            imagesToLoad.toArray(new PlaceholderDrawable[0]));
                }
                mHasStartedImageLoad = true;
            }
        }
        void unbind(TextView view) {
            removeView(view);
        }

        void encode(Context context, String html) {
            CharSequence encoded = HtmlUtils.encode(context, html, this);
            synchronized (this) {
                mHtml = encoded;
            }
        }

        void onImageLoadDone() {
            discardLoadedImages();

            for (ImageSpan span : getImageSpans()) {
                Drawable d = span.getDrawable();
                if (d instanceof PlaceholderDrawable) {
                    PlaceholderDrawable phd = (PlaceholderDrawable) d;
                    d = phd.getWrappedDrawable();
                }
                if (d instanceof GifDrawable) {
                    GifDrawable gd = (GifDrawable) d;
                    if (mResumed) {
                        gd.start();
                    }
                    mGifs.add(new GifInfo(gd, mViewRefs));
                } else if (d instanceof LoadedBitmapDrawable) {
                    BitmapDrawable bd = (BitmapDrawable) d;
                    mBitmaps.add(new WeakReference<>(bd.getBitmap()));
                }
            }
        }

        void invalidateViewsForNewDrawable() {
            for (int i = 0; i < mViewRefs.size(); i++) {
                TextView view = mViewRefs.get(i).get();
                if (view != null) {
                    view.setText(view.getText());
                }
            }
        }

        void setResumed(boolean resumed) {
            mResumed = resumed;
            for (GifInfo info : mGifs) {
                GifDrawable drawable = info.mDrawable.get();
                if (drawable == null) {
                    continue;
                }
                if (resumed) {
                    drawable.start();
                } else {
                    drawable.stop();
                }
            }
        }

        @NonNull
        private ImageSpan[] getImageSpans() {
            if (TextUtils.isEmpty(mHtml)) {
                return new ImageSpan[0];
            }
            Spanned spanned = (Spanned) mHtml;
            return spanned.getSpans(0, spanned.length(), ImageSpan.class);
        }

        private void discardLoadedImages() {
            for (WeakReference<Bitmap> ref : mBitmaps) {
                Bitmap bitmap = ref.get();
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
            mBitmaps.clear();
            for (GifInfo info : mGifs) {
                info.destroy();
            }
            mGifs.clear();
            mHasStartedImageLoad = false;
        }

        void clearHtmlCache() {
            if (mTask != null) {
                mTask.cancel(true);
                mTask = null;
            }
            mHtml = null;
            mHasStartedImageLoad = false;
        }

        private void apply(CharSequence text) {
            int visibility = TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE;
            for (int i = 0; i < mViewRefs.size(); i++) {
                TextView view = mViewRefs.get(i).get();
                if (view != null) {
                    view.setText(text);
                    view.setVisibility(visibility);
                }
            }
        }

        private void addView(TextView view) {
            boolean alreadyPresent = false;
            for (int i = 0; i < mViewRefs.size(); i++) {
                TextView existing = mViewRefs.get(i).get();
                if (existing == null) {
                    mViewRefs.remove(i);
                } else if (existing == view) {
                    alreadyPresent = true;
                }
            }
            if (!alreadyPresent) {
                mViewRefs.add(new WeakReference<>(view));
            }
        }

        private void removeView(TextView view) {
            for (int i = 0; i < mViewRefs.size(); i++) {
                TextView existing = mViewRefs.get(i).get();
                if (existing == null || existing == view) {
                    mViewRefs.remove(i);
                }
            }
        }

        @Override
        public Drawable getDrawable(String source) {
            return new PlaceholderDrawable(source, this, mLoadingDrawable);
        }
    }

    private final Handler mHandler = new Handler();
    private final Map<Object, ObjectInfo> mObjectInfos = new HashMap<>();
    private final Drawable mLoadingDrawable;
    private final Drawable mErrorDrawable;
    private final OkHttpClient mClient;

    private final Context mContext;

    private final File mCacheDir;

    private final int mWidth;
    private final int mHeight;

    private boolean mDestroyed;

    public HttpImageGetter(Context context) {
        mContext = context;
        mCacheDir = context.getCacheDir();
        mClient = ServiceFactory.getImageHttpClient();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Point size = new Point();

        wm.getDefaultDisplay().getSize(size);
        mWidth = size.x;
        mHeight = size.y;

        mLoadingDrawable = ContextCompat.getDrawable(context, R.drawable.image_loading);
        mLoadingDrawable.setBounds(0, 0,
                mLoadingDrawable.getIntrinsicWidth(), mLoadingDrawable.getIntrinsicHeight());

        mErrorDrawable = ContextCompat.getDrawable(context, R.drawable.content_picture);
        mErrorDrawable.setBounds(0, 0,
                mErrorDrawable.getIntrinsicWidth(), mErrorDrawable.getIntrinsicHeight());
    }

    public void pause() {
        for (ObjectInfo info : mObjectInfos.values()) {
            info.setResumed(false);
        }
    }

    public void resume() {
        for (ObjectInfo info : mObjectInfos.values()) {
            info.setResumed(true);
        }
    }

    public void clearHtmlCache() {
        for (ObjectInfo info : mObjectInfos.values()) {
            info.clearHtmlCache();
        }
    }

    public void destroy() {
        for (ObjectInfo info : mObjectInfos.values()) {
            info.discardLoadedImages();
        }
        mObjectInfos.clear();
        mDestroyed = true;
    }

    public void encode(final Context context, final Object id, final String html) {
        findOrCreateInfo(id).encode(context, html);
    }

    public void bind(final TextView view, final String html, final Object id) {
        unbind(view);
        findOrCreateInfo(id).bind(view, html);
    }

    private void unbind(final TextView view) {
        for (ObjectInfo info : mObjectInfos.values()) {
            info.unbind(view);
        }
    }

    private ObjectInfo findOrCreateInfo(Object id) {
        ObjectInfo info = mObjectInfos.get(id);
        if (info == null) {
            info = new ObjectInfo();
            mObjectInfos.put(id, info);
        }
        return info;
    }

    private static class ImageGetterAsyncTask extends AsyncTask<PlaceholderDrawable, Void, Void> {
        private final HttpImageGetter mImageGetter;
        private final ObjectInfo mInfo;

        public ImageGetterAsyncTask(HttpImageGetter getter, ObjectInfo info) {
            mImageGetter = getter;
            mInfo = info;
        }

        @Override
        protected Void doInBackground(PlaceholderDrawable... params) {
            for (PlaceholderDrawable placeholder : params) {
                Drawable drawable = mImageGetter.loadImageForUrl(placeholder.getUrl());
                if (drawable != null) {
                    placeholder.addLoadedImage(drawable, mImageGetter.mHandler);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mInfo.onImageLoadDone();
            }
        }
    }

    private Drawable loadImageForUrl(String source) {
        HttpUrl url = source != null ? HttpUrl.parse(source) : null;
        Bitmap bitmap = null;

        if (!mDestroyed && url != null) {
            File output = null;
            InputStream is = null;
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try (Response response = mClient.newCall(request).execute()) {
                is = response.body().byteStream();
                if (is != null) {
                    MediaType mediaType = response.body().contentType();
                    String mime = mediaType != null ? mediaType.toString() : null;
                    if (mime == null) {
                        mime = URLConnection.guessContentTypeFromName(source);
                    }
                    if (mime == null) {
                        mime = URLConnection.guessContentTypeFromStream(is);
                    }
                    if (mime != null && mime.startsWith("image/svg")) {
                        bitmap = renderSvgToBitmap(mContext.getResources(), is, mWidth, mHeight);
                    } else {
                        boolean isGif = mime != null && mime.startsWith("image/gif");
                        if (!isGif || canLoadGif()) {
                            output = File.createTempFile("image", ".tmp", mCacheDir);
                            if (FileUtils.save(output, is)) {
                                if (isGif) {
                                    GifDrawable d = new GifDrawable(output);
                                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                                    return d;
                                } else {
                                    bitmap = getBitmap(output, mWidth, mHeight);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // fall through to showing the error bitmap
            } finally {
                if (output != null) {
                    output.delete();
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignored
                    }
                }
            }
        }

        synchronized (this) {
            if (mDestroyed && bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
        }

        if (bitmap == null) {
            return mErrorDrawable;
        }

        BitmapDrawable drawable = new LoadedBitmapDrawable(mContext.getResources(), bitmap);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        return drawable;
    }

    private boolean canLoadGif() {
        SharedPreferences prefs = mContext.getSharedPreferences(SettingsFragment.PREF_NAME,
                Context.MODE_PRIVATE);
        int mode = prefs.getInt(SettingsFragment.KEY_GIF_LOADING, 1);
        switch (mode) {
            case 1: // load via Wifi
                return !UiUtils.downloadNeedsWarning(mContext);
            case 2: // always load
                return true;
            default:
                return false;
        }
    }

    private static Bitmap getBitmap(final File image, int width, int height) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        RandomAccessFile file = null;

        try {
            file = new RandomAccessFile(image.getAbsolutePath(), "r");
            FileDescriptor fd = file.getFD();

            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fd, null, options);

            int scale = 1;
            while (options.outWidth >= width || options.outHeight >= height) {
                options.outWidth /= 2;
                options.outHeight /= 2;
                scale *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inSampleSize = scale;

            return BitmapFactory.decodeFileDescriptor(fd, null, options);
        } catch (IOException e) {
            return null;
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

    private static Bitmap renderSvgToBitmap(Resources res, InputStream is,
            int maxWidth, int maxHeight) {
        try {
            SVG svg = SVG.getFromInputStream(is);
            if (svg != null) {
                svg.setRenderDPI(DisplayMetrics.DENSITY_DEFAULT);
                Float density = res.getDisplayMetrics().density;
                int docWidth = (int) (svg.getDocumentWidth() * density);
                int docHeight = (int) (svg.getDocumentHeight() * density);
                if (docWidth < 0 || docHeight < 0) {
                    float aspectRatio = svg.getDocumentAspectRatio();
                    if (aspectRatio > 0) {
                        float heightForAspect = (float) maxWidth / aspectRatio;
                        float widthForAspect = (float) maxHeight * aspectRatio;
                        if (widthForAspect < heightForAspect) {
                            docWidth = Math.round(widthForAspect);
                            docHeight = maxHeight;
                        } else {
                            docWidth = maxWidth;
                            docHeight = Math.round(heightForAspect);
                        }
                    } else {
                        docWidth = maxWidth;
                        docHeight = maxHeight;
                    }

                    // we didn't take density into account anymore when calculating docWidth
                    // and docHeight, so don't scale with it and just let the renderer
                    // figure out the scaling
                    density = null;
                }

                while (docWidth >= maxWidth || docHeight >= maxHeight) {
                    docWidth /= 2;
                    docHeight /= 2;
                    if (density != null) {
                        density /= 2;
                    }
                }

                Bitmap bitmap = Bitmap.createBitmap(docWidth, docHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                if (density != null) {
                    canvas.scale(density, density);
                }
                svg.renderToCanvas(canvas);
                return bitmap;
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }
}
