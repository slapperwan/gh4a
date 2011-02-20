/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gh4a.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.gh4a.Constants;

/**
 * Based on blog Multithreading For Performance 
 * ({@link http://android-developers.blogspot.com/2010/07/multithreading-for-performance.html})
 * 
 * Some of the parts are modified to suit with gh4a application
 */
public class ImageDownloader {

    /** The instance. */
    private static ImageDownloader instance;

    /**
     * Gets the single instance of ImageDownloader.
     *
     * @return single instance of ImageDownloader
     */
    public static ImageDownloader getInstance() {
        if (instance == null) {
            instance = new ImageDownloader();
        }
        return instance;
    }

    /**
     * Download.
     * 
     * @param gravatarId the gravatar id
     * @param ivImage the iv image
     */
    public void download(String gravatarId, ImageView ivImage) {
        String url = "http://www.gravatar.com/avatar.php?gravatar_id=" + gravatarId + "&size=60";

        resetPurgeTimer();
        Bitmap bitmap = getBitmapFromCache(url);

        if (bitmap == null) {
            if (cancelPotentialDownload(url, ivImage)) {
                BitmapDownloaderTask task = new BitmapDownloaderTask(ivImage);
                DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                ivImage.setImageDrawable(downloadedDrawable);
                task.execute(url);
            }
        }
        else {
            cancelPotentialDownload(url, ivImage);
            ivImage.setImageBitmap(bitmap);
        }
    }
    
    public void download(String gravatarId, ImageView ivImage, int size) {
        String url = "http://www.gravatar.com/avatar.php?gravatar_id=" + gravatarId + "&size=" + size;

        resetPurgeTimer();
        Bitmap bitmap = getBitmapFromCache(url);

        if (bitmap == null) {
            if (cancelPotentialDownload(url, ivImage)) {
                BitmapDownloaderTask task = new BitmapDownloaderTask(ivImage);
                DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                ivImage.setImageDrawable(downloadedDrawable);
                task.execute(url);
            }
        }
        else {
            cancelPotentialDownload(url, ivImage);
            ivImage.setImageBitmap(bitmap);
        }
    }

    /**
     * Download bitmap.
     * 
     * @param urlStr the url str
     * @return the bitmap
     */
    static Bitmap downloadBitmap(String urlStr) {
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            conn.connect();
            is = conn.getInputStream();
            // bis = new BufferedInputStream(is);
            final Bitmap bitmap = BitmapFactory.decodeStream(new FlushedInputStream(is));
            return bitmap;
        }
        catch (Exception e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                }
            }
        }
        return null;
    }

    /**
     * Download bitmap2.
     * 
     * @param url the url
     * @return the bitmap
     */
    static Bitmap downloadBitmap2(String url) {
        final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from "
                        + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    final Bitmap bitmap = BitmapFactory.decodeStream(new FlushedInputStream(
                            inputStream));
                    return bitmap;
                }
                finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        }
        catch (Exception e) {
            // Could provide a more explicit error message for IOException or
            // IllegalStateException
            getRequest.abort();
            Log.w("ImageDownloader", "Error while retrieving bitmap from " + url + e.toString());
        }
        finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }

    /**
     * The Class FlushedInputStream.
     */
    static class FlushedInputStream extends FilterInputStream {

        /**
         * Instantiates a new flushed input stream.
         * 
         * @param inputStream the input stream
         */
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        /*
         * (non-Javadoc)
         * @see java.io.FilterInputStream#skip(long)
         */
        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int byt = read();
                    if (byt < 0) {
                        break; // we reached EOF
                    }
                    else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    /**
     * An asynchronous task that runs on a background thread to bitmap
     * downloader.
     */
    public class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {

        /** The url. */
        private String url;

        /** The image view reference. */
        private final WeakReference<ImageView> imageViewReference;

        /**
         * Instantiates a new bitmap downloader task.
         * 
         * @param imageView the image view
         */
        public BitmapDownloaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        // Actual download method, run in the task thread
        protected Bitmap doInBackground(String... params) {
            // params comes from the execute() call: params[0] is the url.
            url = params[0];
            return downloadBitmap(url);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            addBitmapToCache(url, bitmap);

            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
                // Change bitmap only if this process is still associated with
                // it
                if (this == bitmapDownloaderTask) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    /**
     * The Class DownloadedDrawable.
     */
    static class DownloadedDrawable extends ColorDrawable {

        /** The bitmap downloader task reference. */
        private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

        /**
         * Instantiates a new downloaded drawable.
         * 
         * @param bitmapDownloaderTask the bitmap downloader task
         */
        public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
            super(Color.GRAY);
            bitmapDownloaderTaskReference = new WeakReference<BitmapDownloaderTask>(
                    bitmapDownloaderTask);
        }

        /**
         * Gets the bitmap downloader task.
         * 
         * @return the bitmap downloader task
         */
        public BitmapDownloaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

    /**
     * Cancel potential download.
     * 
     * @param url the url
     * @param imageView the image view
     * @return true, if successful
     */
    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                bitmapDownloaderTask.cancel(true);
            }
            else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the bitmap downloader task.
     * 
     * @param imageView the image view
     * @return the bitmap downloader task
     */
    private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    /*
     * Cache-related fields and methods. We use a hard and a soft cache. A soft
     * reference cache is too aggressively cleared by the Garbage Collector.
     */

    /** The Constant HARD_CACHE_CAPACITY. */
    private static final int HARD_CACHE_CAPACITY = 10;
    
    /** The Constant DELAY_BEFORE_PURGE. */
    private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds

    // Hard cache, with a fixed maximum capacity and a life duration
    /** The s hard bitmap cache. */
    private final HashMap<String, Bitmap> sHardBitmapCache = new LinkedHashMap<String, Bitmap>(
            HARD_CACHE_CAPACITY / 2, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
            if (size() > HARD_CACHE_CAPACITY) {
                // Entries push-out of hard reference cache are transferred to
                // soft reference cache
                sSoftBitmapCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
                return true;
            }
            else return false;
        }
    };

    // Soft cache for bitmaps kicked out of hard cache
    /** The Constant sSoftBitmapCache. */
    private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
            HARD_CACHE_CAPACITY / 2);

    /** The purge handler. */
    private final Handler purgeHandler = new Handler();

    /** The purger. */
    private final Runnable purger = new Runnable() {
        public void run() {
            clearCache();
        }
    };

    /**
     * Adds this bitmap to the cache.
     *
     * @param url the url
     * @param bitmap The newly downloaded bitmap.
     */
    private void addBitmapToCache(String url, Bitmap bitmap) {
        if (bitmap != null) {
            synchronized (sHardBitmapCache) {
                sHardBitmapCache.put(url, bitmap);
            }
        }
    }

    /**
     * Gets the bitmap from cache.
     *
     * @param url The URL of the image that will be retrieved from the cache.
     * @return The cached bitmap or null if it was not found.
     */
    private Bitmap getBitmapFromCache(String url) {
        // First try the hard reference cache
        synchronized (sHardBitmapCache) {
            final Bitmap bitmap = sHardBitmapCache.get(url);
            if (bitmap != null) {
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
                sHardBitmapCache.remove(url);
                sHardBitmapCache.put(url, bitmap);
                return bitmap;
            }
        }

        // Then try the soft reference cache
        SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
        if (bitmapReference != null) {
            final Bitmap bitmap = bitmapReference.get();
            if (bitmap != null) {
                // Bitmap found in soft cache
                return bitmap;
            }
            else {
                // Soft reference has been Garbage Collected
                sSoftBitmapCache.remove(url);
            }
        }

        return null;
    }

    /**
     * Clears the image cache used internally to improve performance. Note that
     * for memory efficiency reasons, the cache will automatically be cleared
     * after a certain inactivity delay.
     */
    public void clearCache() {
        sHardBitmapCache.clear();
        sSoftBitmapCache.clear();
    }

    /**
     * Allow a new delay before the automatic cache clear is done.
     */
    private void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }

}
