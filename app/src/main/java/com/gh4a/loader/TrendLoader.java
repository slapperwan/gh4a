package com.gh4a.loader;

import android.content.Context;

import com.gh4a.holder.Trend;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Single;

public class TrendLoader extends BaseLoader<List<Trend>> {
    private static final String URL_TEMPLATE =
            "http://octodroid.s3.amazonaws.com/trends/trending_%s-all.json";

    private final String mType;

    public TrendLoader(Context context, String type) {
        super(context);
        mType = type;
    }

    @Override
    public List<Trend> doLoadInBackground() throws IOException, JSONException {
        return loadTrends(mType).blockingGet();
    }

    public static Single<List<Trend>> loadTrends(String type) {
        return Single.fromCallable(() -> {
            URL url = new URL(String.format(Locale.US, URL_TEMPLATE, type));
            List<Trend> trends = new ArrayList<>();

            HttpURLConnection connection = null;
            CharArrayWriter writer = null;

            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return trends;
                }

                InputStream in = new BufferedInputStream(connection.getInputStream());
                InputStreamReader reader = new InputStreamReader(in, "UTF-8");
                int length = connection.getContentLength();
                writer = new CharArrayWriter(Math.max(0, length));
                char[] tmp = new char[4096];

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

            JSONArray resultArray = new JSONArray(writer.toString());
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject repoObject = resultArray.getJSONObject(i);

                trends.add(new Trend(
                        repoObject.getString("owner"),
                        repoObject.getString("repo"),
                        repoObject.isNull("description") ? null : repoObject.optString("description"),
                        (int) repoObject.getDouble("stars"),
                        (int) repoObject.getDouble("new_stars"),
                        (int) repoObject.getDouble("forks")));
            }
            return trends;

        });
    }
}
