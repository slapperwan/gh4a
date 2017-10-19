package com.gh4a.utils;

import com.gh4a.Gh4Application;
import com.gh4a.feeds.FeedHandler;
import com.gh4a.model.Feed;
import com.gh4a.model.NotificationHolder;
import com.gh4a.model.NotificationListLoadResult;
import com.gh4a.model.Trend;
import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.activity.NotificationService;
import com.meisolsson.githubsdk.service.repositories.RepositoryCollaboratorService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import io.reactivex.Single;

public class SingleFactory {
    public static Single<Boolean> isAppUserRepoCollaborator(String repoOwner, String repoName) {
        Gh4Application app = Gh4Application.get();
        RepositoryCollaboratorService service =
                app.getGitHubService(RepositoryCollaboratorService.class);

        if (!app.isAuthorized()) {
            return Single.just(false);
        }

        return service.isUserCollaborator(repoOwner, repoName, app.getAuthLogin())
                .map(ApiHelpers::throwOnFailure)
                // the API returns 403 if the user doesn't have push access,
                // which in turn means he isn't a collaborator
                .compose(RxUtils.mapFailureToValue(403, false))
                // there's no actual content, result is always null
                .map(result -> true);
    }

    public static Single<NotificationListLoadResult> getNotifications(boolean all, boolean participating) {
        final NotificationService service =
                Gh4Application.get().getGitHubService(NotificationService.class);
        final Map<String, Object> options = new HashMap<>();
        options.put("all", all);
        options.put("participating", participating);

        return ApiHelpers.PageIterator
                .toSingle(page -> service.getNotifications(options, page))
                .map(SingleFactory::notificationsToResult);
    }

    private static NotificationListLoadResult notificationsToResult(
            List<NotificationThread> notifications) {
        // group notifications by repo
        final HashMap<Repository, ArrayList<NotificationThread>> notificationsByRepo = new HashMap<>();
        for (NotificationThread n : notifications) {
            ArrayList<NotificationThread> list = notificationsByRepo.get(n.repository());
            if (list == null) {
                list = new ArrayList<>();
                notificationsByRepo.put(n.repository(), list);
            }
            list.add(n);
        }

        // sort each group by updatedAt
        for (ArrayList<NotificationThread> list : notificationsByRepo.values()) {
            Collections.sort(list, (lhs, rhs) -> rhs.updatedAt().compareTo(lhs.updatedAt()));
        }

        // sort groups by updatedAt of top notification
        ArrayList<Repository> reposByTimestamp = new ArrayList<>(notificationsByRepo.keySet());
        Collections.sort(reposByTimestamp, new Comparator<Repository>() {
            @Override
            public int compare(Repository lhs, Repository rhs) {
                NotificationThread lhsNotification = notificationsByRepo.get(lhs).get(0);
                NotificationThread rhsNotification = notificationsByRepo.get(rhs).get(0);
                return rhsNotification.updatedAt().compareTo(lhsNotification.updatedAt());
            }
        });

        // add to list
        List<NotificationHolder> result = new ArrayList<>();
        for (Repository repo : reposByTimestamp) {
            ArrayList<NotificationThread> notifsForRepo = notificationsByRepo.get(repo);
            boolean hasUnread = false;
            int count = notifsForRepo.size();

            NotificationHolder repoItem = new NotificationHolder(repo);
            result.add(repoItem);

            for (int i = 0; i < count; i++) {
                NotificationHolder item = new NotificationHolder(notifsForRepo.get(i));
                hasUnread |= item.notification.unread();
                item.setIsLastRepositoryNotification(i == count - 1);
                result.add(item);
            }

            repoItem.setIsRead(!hasUnread);
        }

        return new NotificationListLoadResult(result);
    }

    public static Single<List<Feed>> loadFeed(String url) {
        return Single.fromCallable(() -> {
            BufferedInputStream bis = null;
            try {
                URLConnection request = new URL(url).openConnection();

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
        });
    }

    public static Single<List<Trend>> loadTrends(String type) {
        return Single.fromCallable(() -> {
            String urlString = String.format(Locale.US,
                    "http://octodroid.s3.amazonaws.com/trends/trending_%s-all.json", type);
            URL url = new URL(urlString);
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
