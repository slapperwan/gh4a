package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Notification;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.NotificationService;

import java.io.IOException;

public class HasNotificationsLoader extends BaseLoader<Boolean> {

    public HasNotificationsLoader(Context context) {
        super(context);
    }

    @Override
    public Boolean doLoadInBackground() throws IOException {
        NotificationService notificationService = (NotificationService)
                Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);
        PageIterator<Notification> notifications = notificationService.pageNotifications(1, false, false);
        return notifications.hasNext() && !notifications.next().isEmpty();
    }
}
