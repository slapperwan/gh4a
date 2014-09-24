package com.gh4a.loader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.content.Context;

import com.gh4a.feeds.FeedHandler;
import com.gh4a.holder.Feed;

public class FeedLoader extends BaseLoader<List<Feed>> {
    private String mUrl;

    private static final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public FeedLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    public List<Feed> doLoadInBackground() throws Exception {
        BufferedInputStream bis = null;
        try {
            URL url = new URL(mUrl);
            URLConnection request = url.openConnection();

            if (request instanceof HttpsURLConnection) {
                ((HttpsURLConnection) request).setHostnameVerifier(DO_NOT_VERIFY);
            }

            bis = new BufferedInputStream(request.getInputStream());

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            FeedHandler handler = new FeedHandler();
            parser.parse(bis, handler);
            return handler.getFeeds();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }
}