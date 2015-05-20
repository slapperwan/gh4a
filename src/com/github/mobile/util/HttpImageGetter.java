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
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.text.Html.ImageGetter;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.UiUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private static boolean containsImages(final String html) {
        return html.contains("<img");
    }

    private final LoadingImageGetter loading;

    private final Context context;

    private final File dir;

    private final int width;

    private final Map<Object, CharSequence> rawHtmlCache = new HashMap<>();

    private final Map<Object, CharSequence> fullHtmlCache = new HashMap<>();

    private ArrayList<WeakReference<Bitmap>> loadedBitmaps;

    private boolean destroyed;

    /**
     * Create image getter for context
     *
     * @param context
     */
    public HttpImageGetter(Context context) {
        this.context = context;
        dir = context.getCacheDir();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT < 13) {
            width = fetchDisplayWidthPreHoneycomb(wm);
        } else {
            width = fetchDisplayWidth(wm);
        }

        loadedBitmaps = new ArrayList<>();
        loading = new LoadingImageGetter(context, 24);
    }

    public void destroy() {
        for (WeakReference<Bitmap> ref : loadedBitmaps) {
            Bitmap bitmap = ref.get();
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        destroyed = true;
    }

    @SuppressWarnings("deprecation")
    private int fetchDisplayWidthPreHoneycomb(WindowManager wm) {
        return wm.getDefaultDisplay().getWidth();
    }

    @TargetApi(13)
    private int fetchDisplayWidth(WindowManager wm) {
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return size.x;
    }

    private HttpImageGetter show(final TextView view, final CharSequence html) {
        if (TextUtils.isEmpty(html))
            return hide(view);

        view.setText(html);
        view.setVisibility(VISIBLE);
        view.setTag(null);
        return this;
    }

    private HttpImageGetter hide(final TextView view) {
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
        if (TextUtils.isEmpty(html))
            return hide(view);

        CharSequence encoded = fullHtmlCache.get(id);
        if (encoded != null)
            return show(view, encoded);

        encoded = rawHtmlCache.get(id);
        if (encoded == null) {
            encoded = HtmlUtils.encode(html, loading);
            if (containsImages(html))
                rawHtmlCache.put(id, encoded);
            else {
                rawHtmlCache.remove(id);
                fullHtmlCache.put(id, encoded);
                return show(view, encoded);
            }
        }

        if (TextUtils.isEmpty(encoded))
            return hide(view);

        show(view, encoded);
        view.setTag(id);
        ImageGetterAsyncTask asyncTask = new ImageGetterAsyncTask();
        AsyncTaskCompat.executeParallel(asyncTask, html, id, view);
        return this;
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

                if (id.equals(view.getTag())) {
                    show(view, result);
                }
            }
        }
    }

    private InputStream fetch(String urlString) throws IOException {
        URL url = new URL(urlString);
        return url.openStream();
    }

    public Drawable getDrawable(String source) {
        File output = null;
        if (destroyed) {
            return loading.getDrawable(source);
        }
        try {
            output = File.createTempFile("image", ".jpg", dir);
            InputStream is = fetch(source);
            if (is != null) {
                boolean success = FileUtils.save(output, is);
                if (success) {
                    Bitmap bitmap = ImageUtils.getBitmap(output, width, Integer.MAX_VALUE);
                    if (bitmap == null) {
                        return loading.getDrawable(source);
                    }
                    loadedBitmaps.add(new WeakReference<Bitmap>(bitmap));
                    BitmapDrawable drawable = new BitmapDrawable(
                            context.getResources(), bitmap);
                    drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    return drawable;
                }
                else {
                    return loading.getDrawable(source);
                }
            }
            else {
                return loading.getDrawable(source);
            }
        } catch (IOException e) {
            return loading.getDrawable(source);
        } finally {
            if (output != null)
                output.delete();
        }
    }
}