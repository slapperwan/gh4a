package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Notification;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.NotificationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationListLoader extends BaseLoader<NotificationListLoadResult> {

    private static final Comparator<Notification> SORTER = new Comparator<Notification>() {
        @Override
        public int compare(Notification lhs, Notification rhs) {
            Repository lhsRepository = lhs.getRepository();
            Repository rhsRepository = rhs.getRepository();

            if (!lhsRepository.equals(rhsRepository)) {
                User lhsOwner = lhsRepository.getOwner();
                User rhsOwner = rhsRepository.getOwner();

                if (!lhsOwner.equals(rhsOwner)) {
                    return lhsOwner.getLogin().compareTo(rhsOwner.getLogin());
                }

                return lhsRepository.getName().compareTo(rhsRepository.getName());
            }

            return rhs.getUpdatedAt().compareTo(lhs.getUpdatedAt());
        }
    };

    private final boolean mAll;
    private final boolean mParticipating;

    public NotificationListLoader(Context context, boolean all, boolean participating) {
        super(context);
        mAll = all;
        mParticipating = participating;
    }

    @Override
    protected NotificationListLoadResult doLoadInBackground() throws Exception {
        NotificationService notificationService = (NotificationService)
                Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);
        List<Notification> notifications =
                notificationService.getNotifications(mAll, mParticipating);
        Collections.sort(notifications, SORTER);

        Repository previousRepository = null;
        List<NotificationHolder> result = new ArrayList<>();
        int unreadNotificationsCount = 0;
        NotificationHolder previousRepositoryHolder = null;

        for (Notification notification : notifications) {
            Repository repository = notification.getRepository();

            if (!repository.equals(previousRepository)) {
                markHolder(result, unreadNotificationsCount, previousRepositoryHolder);

                previousRepositoryHolder = new NotificationHolder(repository);
                result.add(previousRepositoryHolder);

                unreadNotificationsCount = 0;
            }

            if (notification.isUnread()) {
                unreadNotificationsCount += 1;
            }

            result.add(new NotificationHolder(notification));
            previousRepository = repository;
        }

        markHolder(result, unreadNotificationsCount, previousRepositoryHolder);

        return new NotificationListLoadResult(result);
    }

    private void markHolder(List<NotificationHolder> result, int unreadNotificationsCount,
            NotificationHolder previousRepositoryHolder) {
        if (previousRepositoryHolder != null && unreadNotificationsCount == 0) {
            previousRepositoryHolder.setIsRead(true);
        }

        int size = result.size();
        if (size > 0) {
            result.get(size - 1).setIsLastRepositoryNotification(true);
        }
    }
}
