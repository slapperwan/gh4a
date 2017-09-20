package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Notification;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.NotificationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class NotificationListLoader extends BaseLoader<NotificationListLoadResult> {
    private static final Comparator<Notification> TIMESTAMP_SORTER = new Comparator<Notification>() {
        @Override
        public int compare(Notification lhs, Notification rhs) {
            return rhs.getUpdatedAt().compareTo(lhs.getUpdatedAt());
        }
    };
    private final boolean mAll;
    private final boolean mParticipating;

    public static List<NotificationHolder> loadNotifications(boolean all,
            boolean participating) throws IOException {
        NotificationService notificationService = (NotificationService)
                Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);
        List<Notification> notifications =
                notificationService.getNotifications(all, participating);

        // group notifications by repo
        final HashMap<Repository, ArrayList<Notification>> notificationsByRepo = new HashMap<>();
        for (Notification n : notifications) {
            ArrayList<Notification> list = notificationsByRepo.get(n.getRepository());
            if (list == null) {
                list = new ArrayList<>();
                notificationsByRepo.put(n.getRepository(), list);
            }
            list.add(n);
        }

        // sort each group by updatedAt
        for (ArrayList<Notification> list : notificationsByRepo.values()) {
            Collections.sort(list, TIMESTAMP_SORTER);
        }

        // sort groups by updatedAt of top notification
        ArrayList<Repository> reposByTimestamp = new ArrayList<>(notificationsByRepo.keySet());
        Collections.sort(reposByTimestamp, new Comparator<Repository>() {
            @Override
            public int compare(Repository lhs, Repository rhs) {
                Notification lhsNotification = notificationsByRepo.get(lhs).get(0);
                Notification rhsNotification = notificationsByRepo.get(rhs).get(0);
                return rhsNotification.getUpdatedAt().compareTo(lhsNotification.getUpdatedAt());
            }
        });

        // add to list
        List<NotificationHolder> result = new ArrayList<>();
        for (Repository repo : reposByTimestamp) {
            ArrayList<Notification> notifsForRepo = notificationsByRepo.get(repo);
            boolean hasUnread = false;
            int count = notifsForRepo.size();

            NotificationHolder repoItem = new NotificationHolder(repo);
            result.add(repoItem);

            for (int i = 0; i < count; i++) {
                NotificationHolder item = new NotificationHolder(notifsForRepo.get(i));
                hasUnread |= item.notification.isUnread();
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
