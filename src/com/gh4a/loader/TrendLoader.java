package com.gh4a.loader;

import android.content.Context;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.holder.Trend;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TrendLoader extends BaseLoader<List<Trend>> {
    private String mUrl;

    public TrendLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    public List<Trend> doLoadInBackground() throws Exception {
        BufferedReader reader = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet pageGet = new HttpGet(new URL(mUrl).toURI());
            HttpResponse response = httpClient.execute(pageGet);

            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            StringBuilder json = new StringBuilder();
            String inputStr;
            while ((inputStr = reader.readLine()) != null) {
                json.append(inputStr);
            }

            JSONObject jsonObject = new JSONObject(json.toString());
            JSONObject resultObject = jsonObject.getJSONObject("results");
            JSONArray repositoryArray = resultObject.getJSONArray("repositories");
            List<Trend> trends = new ArrayList<Trend>();
            for(int i = 0; i < repositoryArray.length(); i++){
                JSONObject repositoryObject = repositoryArray.getJSONObject(i);
                String repoName = repositoryObject.getJSONObject("title").getString("text").replaceAll("\n", "");
                String title = repositoryObject.getJSONObject("title").getString("href");
                String description = repositoryObject.getString("description");

                Trend trend = new Trend();
                trend.setTitle(repoName);
                trend.setRepo(repoName);
                trend.setLink(title);
                trend.setDescription(description);

                trends.add(trend);
            }
            return trends;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                }
            }
        }
    }
}
