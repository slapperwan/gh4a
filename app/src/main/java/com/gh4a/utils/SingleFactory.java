package com.gh4a.utils;

import com.gh4a.Gh4Application;
import com.gh4a.ServiceFactory;
import com.gh4a.model.Feed;
import com.gh4a.model.GitHubFeedService;
import com.gh4a.model.NotificationHolder;
import com.gh4a.model.NotificationListLoadResult;
import com.gh4a.model.Trend;
import com.gh4a.model.TrendService;
import com.meisolsson.githubsdk.core.ServiceGenerator;
import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.activity.NotificationService;
import com.meisolsson.githubsdk.service.repositories.RepositoryCollaboratorService;

import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.RegistryMatcher;
import org.simpleframework.xml.transform.Transform;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class SingleFactory {
    public static Single<Boolean> isAppUserRepoCollaborator(String repoOwner, String repoName,
            boolean bypassCache) {
        Gh4Application app = Gh4Application.get();
        RepositoryCollaboratorService service =
                ServiceFactory.get(RepositoryCollaboratorService.class, bypassCache);

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

    public static Single<NotificationListLoadResult> getNotifications(boolean all,
            boolean participating, boolean bypassCache) {
        final NotificationService service =
                ServiceFactory.get(NotificationService.class, bypassCache);
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
        Collections.sort(reposByTimestamp, (lhs, rhs) -> {
            NotificationThread lhsNotification = notificationsByRepo.get(lhs).get(0);
            NotificationThread rhsNotification = notificationsByRepo.get(rhs).get(0);
            return rhsNotification.updatedAt().compareTo(lhsNotification.updatedAt());
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

    public static Single<List<Feed>> loadFeed(String relativeUrl) {
        return RetrofitHelper.feedService()
                .getFeed(relativeUrl)
                .map(ApiHelpers::throwOnFailure)
                .map(feed -> feed.feed);
    }

    public static Single<List<Trend>> loadTrends(String type) {
        return RetrofitHelper.trendService()
                .getTrends(type)
                .map(ApiHelpers::throwOnFailure);
    }

    private static class RetrofitHelper {
        private static GitHubFeedService sFeedService;
        private static TrendService sTrendService;

        static GitHubFeedService feedService() {
            if (sFeedService == null) {
                initialize();
            }
            return sFeedService;
        }

        static TrendService trendService() {
            if (sTrendService == null) {
                initialize();
            }
            return sTrendService;
        }

        private static void initialize() {
            RegistryMatcher matcher = new RegistryMatcher();
            matcher.bind(Date.class, new Transform<Date>() {
                private final DateFormat mFormat =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

                @Override
                public Date read(String value) throws Exception {
                    return mFormat.parse(value);
                }

                @Override
                public String write(Date value) throws Exception {
                    return mFormat.format(value);
                }
            });

            OkHttpClient client = ServiceFactory.getHttpClientBuilder()
                    .followRedirects(false)
                    .build();

            sFeedService = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(SimpleXmlConverterFactory.create(new Persister(matcher)))
                    .baseUrl("https://github.com/")
                    .client(client)
                    .build()
                    .create(GitHubFeedService.class);
            sTrendService = new Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(MoshiConverterFactory.create(ServiceGenerator.moshi))
                    .baseUrl("http://octodroid.s3.amazonaws.com/")
                    .build()
                    .create(TrendService.class);
        }
    }


}
