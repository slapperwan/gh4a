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
package com.github.mobile.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.UiUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pl.droidsonroids.gif.GifDrawable;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 *
 * Original source https://github.com/github/android/blob/master/app/src/main/java/com/github/mobile/util/HttpImageGetter.java
 * Getter for an image
 */
public class HttpImageGetter implements ImageGetter {

    private static class LoadingImageGetter implements ImageGetter {

        private final Drawable image;

        private LoadingImageGetter(final Context context, final int size) {
            int imageSize = Math.round(context.getResources()
                    .getDisplayMetrics().density * size + 0.5F);
            image = ContextCompat.getDrawable(context,
                    UiUtils.resolveDrawable(context, R.attr.contentPictureIcon));

            image.setBounds(0, 0, imageSize, imageSize);
        }

        public Drawable getDrawable(String source) {
            return image;
        }
    }

    private static class GifCallback implements Drawable.Callback {
        private ArrayList<WeakReference<TextView>> mViewRefs = new ArrayList<>();
        private Handler mHandler = new Handler();

        public void addView(TextView view) {
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

        public void removeView(TextView view) {
            for (int i = 0; i < mViewRefs.size(); i++) {
                TextView existing = mViewRefs.get(i).get();
                if (existing == null || existing == view) {
                    mViewRefs.remove(i);
                }
            }
        }

        @Override
        public void invalidateDrawable(Drawable drawable) {
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
        public void scheduleDrawable(Drawable drawable, Runnable runnable, long when) {
            mHandler.postAtTime(runnable, when);
        }

        @Override
        public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
            mHandler.removeCallbacks(runnable);
        }
    }

    private static class GifInfo {
        WeakReference<GifDrawable> mDrawable;
        GifCallback mCallback;
        public GifInfo(GifDrawable d) {
            mCallback = new GifCallback();
            mDrawable = new WeakReference<>(d);
            d.setCallback(mCallback);
        }
    }

    private static boolean containsImages(final String html) {
        return html.contains("<img");
    }

    private final LoadingImageGetter loading;

    private final Context context;

    private final File dir;

    private final int width;
    private final int height;

    private final Map<Object, CharSequence> rawHtmlCache = new HashMap<>();

    private final Map<Object, CharSequence> fullHtmlCache = new HashMap<>();

    private final Map<Object, GifInfo> knownGifs = new HashMap<>();

    private ArrayList<WeakReference<Bitmap>> loadedBitmaps;

    private boolean destroyed;
    private boolean resumed;

    /**
     * Create image getter for context
     *
     * @param context
     */
    public HttpImageGetter(Context context) {
        this.context = context;
        dir = context.getCacheDir();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Point size;
        if (Build.VERSION.SDK_INT < 13) {
            size = fetchDisplaySizePreHoneycomb(wm);
        } else {
            size = fetchDisplaySize(wm);
        }

        width = size.x;
        height = size.y;

        loadedBitmaps = new ArrayList<>();
        loading = new LoadingImageGetter(context, 24);
    }

    public void pause() {
        resumed = false;
        updateGifPlayState();
    }

    public void resume() {
        resumed = true;
        updateGifPlayState();
    }

    public boolean isResumed() {
        return resumed;
    }

    public void destroy() {
        synchronized (this) {
            for (WeakReference<Bitmap> ref : loadedBitmaps) {
                Bitmap bitmap = ref.get();
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
            loadedBitmaps.clear();
            for (GifInfo info : knownGifs.values()) {
                GifDrawable drawable = info.mDrawable.get();
                if (drawable != null) {
                    drawable.setCallback(null);
                    drawable.stop();
                    drawable.recycle();
                }
            }
            knownGifs.clear();
            destroyed = true;
        }
    }

    private synchronized void updateGifPlayState() {
        for (GifInfo info : knownGifs.values()) {
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

    @SuppressWarnings("deprecation")
    private Point fetchDisplaySizePreHoneycomb(WindowManager wm) {
        Display display = wm.getDefaultDisplay();
        return new Point(display.getWidth(), display.getHeight());
    }

    @TargetApi(13)
    private Point fetchDisplaySize(WindowManager wm) {
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return size;
    }

    private HttpImageGetter show(final TextView view, final CharSequence html, final Object id) {
        if (TextUtils.isEmpty(html))
            return hide(view, id);

        view.setText(html);
        view.setVisibility(VISIBLE);
        view.setTag(null);
        return this;
    }

    private HttpImageGetter hide(final TextView view, final Object id) {
        view.setText(null);
        view.setVisibility(GONE);
        view.setTag(null);
        return this;
    }

    /**
     * Encode given HTML string and map it to the given id
     *
     * @param id
     * @param html
     * @return this image getter
     */
    public void encode(final Object id, final String html) {
        if (TextUtils.isEmpty(html))
            return;

        CharSequence encoded = HtmlUtils.encode(html, loading);
        // Use default encoding if no img tags
        if (containsImages(html))
            rawHtmlCache.put(id, encoded);
        else {
            rawHtmlCache.remove(id);
            fullHtmlCache.put(id, encoded);
        }
    }

    /**
     * Bind text view to HTML string
     *
     * @param view
     * @param html
     * @param id
     * @return this image getter
     */
    public HttpImageGetter bind(final TextView view, final String html,
            final Object id) {
        unbindViewFromGifs(view);
        if (TextUtils.isEmpty(html))
            return hide(view, id);

        CharSequence encoded = fullHtmlCache.get(id);
        if (encoded != null) {
            addViewToGifCb(view, id);
            return show(view, encoded, id);
        }

        encoded = rawHtmlCache.get(id);
        if (encoded == null) {
            encoded = HtmlUtils.encode(html, loading);
            if (containsImages(html))
                rawHtmlCache.put(id, encoded);
            else {
                rawHtmlCache.remove(id);
                fullHtmlCache.put(id, encoded);
                return show(view, encoded, id);
            }
        }

        if (TextUtils.isEmpty(encoded))
            return hide(view, id);

        show(view, encoded, id);
        view.setTag(id);
        ImageGetterAsyncTask asyncTask = new ImageGetterAsyncTask();
        AsyncTaskCompat.executeParallel(asyncTask, html, id, view);
        return this;
    }

    private synchronized void unbindViewFromGifs(TextView view) {
        for (GifInfo info : knownGifs.values()) {
            info.mCallback.removeView(view);
        }
    }

    private synchronized void addViewToGifCb(TextView view, Object id) {
        GifInfo info = knownGifs.get(id);
        if (info != null) {
            info.mCallback.addView(view);
        }
    }

    public class ImageGetterAsyncTask extends AsyncTask<Object, Void, CharSequence> {
        String html;
        Object id;
        TextView view;

        @Override
        protected CharSequence doInBackground(Object... params) {
            html = (String) params[0];
            id = params[1];
            view = (TextView) params[2];
            return HtmlUtils.encode(html, HttpImageGetter.this);
        }

        protected void onPostExecute(CharSequence result) {
            if (result != null) {
                rawHtmlCache.remove(id);
                fullHtmlCache.put(id, result);

                Spanned spanned = (Spanned) result;
                ImageSpan[] spans = spanned.getSpans(0, result.length(), ImageSpan.class);
                for (ImageSpan span : spans) {
                    Drawable d = span.getDrawable();
                    if (d instanceof GifDrawable) {
                        GifDrawable gd = (GifDrawable) d;
                        synchronized (this) {
                            if (resumed) {
                                gd.start();
                            }
                            knownGifs.put(id, new GifInfo(gd));
                        }
                    }
                }


                if (id.equals(view.getTag())) {
                    addViewToGifCb(view, id);
                    show(view, result, id);
                }
            }
        }
    }

    @Override
    public Drawable getDrawable(String source) {
        Bitmap bitmap = null;

        if (!destroyed) {
            File output = null;
            InputStream is = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(source).openConnection();
                is = connection.getInputStream();
                if (is != null) {
                    String mime = connection.getContentType();
                    if (mime == null) {
                        mime = URLConnection.guessContentTypeFromName(source);
                    }
                    if (mime == null) {
                        mime = URLConnection.guessContentTypeFromStream(is);
                    }
                    if (mime != null && mime.startsWith("image/svg")) {
                        bitmap = ImageUtils.renderSvgToBitmap(context.getResources(),
                                is, width, height);
                    } else {
                        boolean isGif = mime != null && mime.startsWith("image/gif");
                        if (!isGif || canLoadGif()) {
                            output = File.createTempFile("image", ".tmp", dir);
                            if (FileUtils.save(output, is)) {
                                if (isGif) {
                                    GifDrawable d = new GifDrawable(output);
                                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                                    return d;
                                } else {
                                    bitmap = ImageUtils.getBitmap(output, width, height);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // fall through to showing the loading bitmap
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
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        synchronized (this) {
            if (destroyed && bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            } else if (bitmap != null) {
                loadedBitmaps.add(new WeakReference<>(bitmap));
            }
        }

        if (bitmap == null) {
            return loading.getDrawable(source);
        }

        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        return drawable;
    }

    private boolean canLoadGif() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // animated GIFs cause more memory pressure than GB can handle
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(SettingsFragment.PREF_NAME,
                Context.MODE_PRIVATE);
        int mode = prefs.getInt(SettingsFragment.KEY_GIF_LOADING, 1);
        switch (mode) {
            case 1: // load via Wifi
                return !UiUtils.downloadNeedsWarning(context);
            case 2: // always load
                return true;
            default:
                return false;
        }
    }
}
