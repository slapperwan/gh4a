package com.gh4a.loader;

import android.content.Context;

import com.gh4a.holder.Trend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
        URL url = new URL(mUrl);
        List<Trend> trends = new ArrayList<>();

        HttpURLConnection connection = null;
        CharArrayWriter writer = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return trends;
            }

            InputStream in = new BufferedInputStream(connection.getInputStream());
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");
            int length = connection.getContentLength();
            writer = new CharArrayWriter(Math.max(0, length));
            char[] tmp = new char[1024];

            int l;
            while((l = reader.read(tmp)) != -1) {
                writer.write(tmp, 0, l);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (writer != null) {
                writer.close();
            }
        }

        JSONObject jsonObject = new JSONObject(writer.toString());
        JSONObject resultObject = jsonObject.getJSONObject("results");
        JSONArray repositoryArray = resultObject.getJSONArray("repositories");
        for (int i = 0; i < repositoryArray.length(); i++) {
            JSONObject repoObject = repositoryArray.getJSONObject(i);
            JSONObject titleObject = repoObject.getJSONObject("title");

            Trend trend = new Trend();
            trend.setTitle(titleObject.getString("text"));
            trend.setRepo(repoObject.optString("owner"), repoObject.optString("repo"));
            trend.setLink(titleObject.optString("href"));
            trend.setDescription(repoObject.optString("description"));

            trends.add(trend);
        }
        return trends;
    }
}
