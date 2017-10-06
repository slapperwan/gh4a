package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.activity.NotificationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationListLoader extends BaseLoader<NotificationListLoadResult> {

    private static final Comparator<NotificationThread> SORTER = new Comparator<NotificationThread>() {
        @Override
        public int compare(NotificationThread lhs, NotificationThread rhs) {
            Repository lhsRepository = lhs.repository();
            Repository rhsRepository = rhs.repository();

            if (!lhsRepository.equals(rhsRepository)) {
                User lhsOwner = lhsRepository.owner();
                User rhsOwner = rhsRepository.owner();

                if (!lhsOwner.equals(rhsOwner)) {
                    return lhsOwner.login().compareTo(rhsOwner.location());
                }

                return lhsRepository.name().compareTo(rhsRepository.name());
            }

            return rhs.updatedAt().compareTo(lhs.updatedAt());
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
        final NotificationService service =
                Gh4Application.get().getGitHubService(NotificationService.class);
        final Map<String, Object> options = new HashMap<>();
        options.put("all", mAll);
        options.put("participating", mParticipating);

        List<NotificationThread> notifications = ApiHelpers.Pager.fetchAllPages(
                new ApiHelpers.Pager.PageProvider<NotificationThread>() {
            @Override
            public Page<NotificationThread> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(service.getNotifications(options, page).blockingGet());
            }
        });
        Collections.sort(notifications, SORTER);

        Repository previousRepository = null;
        List<NotificationHolder> result = new ArrayList<>();
        int unreadNotificationsCount = 0;
        NotificationHolder previousRepositoryHolder = null;

        for (NotificationThread notification : notifications) {
            Repository repository = notification.repository();

            if (!repository.equals(previousRepository)) {
                markHolder(result, unreadNotificationsCount, previousRepositoryHolder);

                previousRepositoryHolder = new NotificationHolder(repository);
                result.add(previousRepositoryHolder);

                unreadNotificationsCount = 0;
            }

            if (notification.unread()) {
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
