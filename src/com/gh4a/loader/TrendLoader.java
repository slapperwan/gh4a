package com.gh4a.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.feeds.TrendHandler;
import com.gh4a.holder.Trend;

public class TrendLoader extends AsyncTaskLoader<List<Trend>> {

    private String mUrl;
    
    public TrendLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }
    
    @Override
    public List<Trend> loadInBackground() {
        InputStream bis = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet pageGet = new HttpGet(new URL(mUrl).toURI());
            HttpResponse response = httpClient.execute(pageGet);

            bis = response.getEntity().getContent();
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            TrendHandler handler = new TrendHandler();
            parser.parse(bis, handler);
            return handler.getTrends();
        }
        catch (MalformedURLException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
        catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
        catch (ParserConfigurationException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
        catch (SAXException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        } catch (URISyntaxException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
        finally {
            if (bis != null) {
                try {
                    bis.close();
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                }
            }
        }
    }
}
