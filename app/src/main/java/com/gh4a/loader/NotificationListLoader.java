package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.activity.NotificationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationListLoader extends BaseLoader<NotificationListLoadResult> {
    private static final Comparator<NotificationThread> TIMESTAMP_SORTER =
            new Comparator<NotificationThread>() {
        @Override
        public int compare(NotificationThread lhs, NotificationThread rhs) {
            return rhs.updatedAt().compareTo(lhs.updatedAt());
        }
    };

    private final boolean mAll;
    private final boolean mParticipating;

    public static List<NotificationHolder> loadNotifications(boolean all,
            boolean participating) throws IOException {
        final NotificationService service =
                Gh4Application.get().getGitHubService(NotificationService.class);
        final Map<String, Object> options = new HashMap<>();
        options.put("all", all);
        options.put("participating", participating);

        List<NotificationThread> notifications = ApiHelpers.Pager.fetchAllPages(
                new ApiHelpers.Pager.PageProvider<NotificationThread>() {
                    @Override
                    public Page<NotificationThread> providePage(long page) throws IOException {
                        return ApiHelpers.throwOnFailure(
                                service.getNotifications(options, page).blockingGet());
                    }
                });

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
            Collections.sort(list, TIMESTAMP_SORTER);
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

        return result;
    }

    public NotificationListLoader(Context context, boolean all, boolean participating) {
        super(context);
        mAll = all;
        mParticipating = participating;
    }

    @Override
    protected NotificationListLoadResult doLoadInBackground() throws Exception {
        return new NotificationListLoadResult(loadNotifications(mAll, mParticipating));
    }
}
