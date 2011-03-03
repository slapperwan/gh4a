package com.gh4a.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.holder.YourActionFeed;
import com.github.api.v2.services.constant.ApplicationConstants;
import com.github.api.v2.services.util.Base64;

public class RssParser {
    
    final URL feedUrl;

    public RssParser(String feedUrl){
        try {
            this.feedUrl = new URL(feedUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getInputStream() {
        try {
            HttpURLConnection request = (HttpURLConnection) feedUrl.openConnection();
            
            request.setRequestMethod("GET");
            request.setDoOutput(true);
            
            if (ApplicationConstants.CONNECT_TIMEOUT > -1) {
                request.setConnectTimeout(ApplicationConstants.CONNECT_TIMEOUT);
            }
    
            if (ApplicationConstants.READ_TIMEOUT > -1) {
                request.setReadTimeout(ApplicationConstants.READ_TIMEOUT);
            }
            String credentials = "slapperwan:marwan-2002git";
            request.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(credentials.getBytes()));
            request.connect();
            return new BufferedInputStream(request.getInputStream());
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    
    public List<YourActionFeed> parse() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            RssHandler handler = new RssHandler();
            parser.parse(this.getInputStream(), handler);
            return handler.getFeeds();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 
    }

}
