package com.gh4a.loader;

import android.content.Context;

import com.gh4a.holder.Trend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TrendLoader extends BaseLoader<List<Trend>> {
    private String mUrl;
    private String mQueryTarget;

    public TrendLoader(Context context, String url, String queryTarget) {
        super(context);
        mUrl = url;
        mQueryTarget = queryTarget;
    }

    @Override
    public List<Trend> doLoadInBackground() throws Exception {
        URL url = new URL(mUrl);
        List<Trend> trends = new ArrayList<>();

        HttpURLConnection connection = null;
        CharArrayWriter writer = null;

        JSONObject input = new JSONObject().put("input",
                new JSONObject().put("webpage/url", mQueryTarget));

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            try {
                dos.write(input.toString().getBytes());
                dos.flush();
            } finally {
                dos.close();
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return trends;
            }

            InputStream in = new BufferedInputStream(connection.getInputStream());
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");
            int length = connection.getContentLength();
            writer = new CharArrayWriter(Math.max(0, length));
            char[] tmp = new char[1024];

            int l;
            while ((l = reader.read(tmp)) != -1) {
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
        JSONArray resultArray = jsonObject.getJSONArray("results");
        for (int i = 0; i < resultArray.length(); i++) {
            JSONObject repoObject = resultArray.getJSONObject(i);

            Trend trend = new Trend();
            trend.setRepo(repoObject.getString("owner"), repoObject.getString("repo"));
            trend.setDescription(repoObject.optString("description"));

            trends.add(trend);
        }
        return trends;
    }
}
