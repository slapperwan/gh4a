package com.gh4a;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.widget.TextView;

public class HtmlImageGetter implements ImageGetter {

    Context c;
    TextView textView;

    public HtmlImageGetter(TextView t, Context c) {
        this.c = c;
        this.textView = t;
    }
    
    @Override
    public Drawable getDrawable(String source) {
        URLDrawable urlDrawable = new URLDrawable();

        // get the actual source
        ImageGetterAsyncTask asyncTask = new ImageGetterAsyncTask(urlDrawable);

        asyncTask.execute(source);

        // return reference to URLDrawable where I will change with actual image
        // from
        // the src tag
        return urlDrawable;
    }

    public class ImageGetterAsyncTask extends AsyncTask<String, Void, Drawable> {
        URLDrawable urlDrawable;

        public ImageGetterAsyncTask(URLDrawable d) {
            this.urlDrawable = d;
        }

        @Override
        protected Drawable doInBackground(String... params) {
            String source = params[0];
            return fetchDrawable(source);
        }

        @Override
        protected void onPostExecute(Drawable result) {
            if (result != null) {
                // set the correct bound according to the result from HTTP call
                Log.d("height", "" + result.getIntrinsicHeight());
                Log.d("width", "" + result.getIntrinsicWidth());
                urlDrawable.setBounds(0, 0, 0 + result.getIntrinsicWidth(),
                        0 + result.getIntrinsicHeight());
    
                // change the reference of the current drawable to the result
                // from the HTTP call
                urlDrawable.drawable = result;
    
                // redraw the image by invalidating the container
                HtmlImageGetter.this.textView.invalidate();
    
                // For ICS
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    HtmlImageGetter.this.textView
                            .setHeight((HtmlImageGetter.this.textView.getHeight() + result
                                    .getIntrinsicHeight()));
                }
                else {
                    // Pre ICS
                    HtmlImageGetter.this.textView.setEllipsize(null);
                }
            }
        }

        /***
         * Get the Drawable from URL
         * 
         * @param urlString
         * @return
         */
        public Drawable fetchDrawable(String urlString) {
            try {
                InputStream is = fetch(urlString);
                Drawable drawable = Drawable.createFromStream(is, "src");
                if (drawable != null) {
                    drawable.setBounds(0, 0, 0 + drawable.getIntrinsicWidth(),
                            0 + drawable.getIntrinsicHeight());
                    return drawable;
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
    }

    private InputStream fetch(String urlString) throws MalformedURLException,
            IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet(urlString);
        HttpResponse response = httpClient.execute(request);
        return response.getEntity().getContent();
    }

    public class URLDrawable extends BitmapDrawable {
        // the drawable that you need to set, you could set the initial drawing
        // with the loading image if you need to
        protected Drawable drawable;

        @Override
        public void draw(Canvas canvas) {
            // override the draw to facilitate refresh function later
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }
    }
    
    static class FlushedInputStream extends FilterInputStream {

        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

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
}
