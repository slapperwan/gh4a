package com.gh4a.loader;

import android.content.Context;

import com.gh4a.holder.Trend;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

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
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet pageGet = new HttpGet(new URL(mUrl).toURI());
        HttpResponse response = httpClient.execute(pageGet);
        List<Trend> trends = new ArrayList<Trend>();
        String json = EntityUtils.toString(response.getEntity(), "UTF-8");
        if (json == null) {
            return trends;
        }

        JSONObject jsonObject = new JSONObject(json.toString());
        JSONObject resultObject = jsonObject.getJSONObject("results");
        JSONArray repositoryArray = resultObject.getJSONArray("repositories");
        for (int i = 0; i < repositoryArray.length(); i++) {
            JSONObject repositoryObject = repositoryArray.getJSONObject(i);
            JSONObject titleObject = repositoryObject.getJSONObject("title");
            String repoName = titleObject.getString("text").replaceAll("\n", "");
            String link = titleObject.getString("href");
            String description = repositoryObject.getString("description");

            Trend trend = new Trend();
            trend.setTitle(repoName);
            trend.setRepo(repoName);
            trend.setLink(link);
            trend.setDescription(description);

            trends.add(trend);
        }
        return trends;
    }
}
