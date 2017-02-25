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

    public NotificationListLoader(Context context) {
        super(context);
    }

    @Override
    protected NotificationListLoadResult doLoadInBackground() throws Exception {
        NotificationService notificationService = (NotificationService)
                Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);
        List<Notification> notifications = notificationService.getNotifications();
        Collections.sort(notifications, SORTER);

        Repository previousRepository = null;
        List<NotificationHolder> result = new ArrayList<>();

        for (Notification notification : notifications) {
            Repository repository = notification.getRepository();
            if (!repository.equals(previousRepository)) {
                int size = result.size();
                if (size > 0) {
                    result.get(size - 1).setIsLastRepositoryNotification(true);
                }
                result.add(new NotificationHolder(repository));
            }
            result.add(new NotificationHolder(notification));
            previousRepository = repository;
        }

        int size = result.size();
        if (size > 0) {
            result.get(size - 1).setIsLastRepositoryNotification(true);
        }

        return new NotificationListLoadResult(result);
    }
}
