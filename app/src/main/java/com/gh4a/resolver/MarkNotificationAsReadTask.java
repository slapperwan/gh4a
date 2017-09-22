package com.gh4a.resolver;

import android.content.Context;

import com.gh4a.BackgroundTask;
import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.service.NotificationService;

import java.io.IOException;

public class MarkNotificationAsReadTask extends BackgroundTask<Void> {
    private final String mNotificationId;

    public MarkNotificationAsReadTask(Context context, String notificationId) {
        super(context);
        mNotificationId = notificationId;
    }

    @Override
    protected Void run() throws IOException {
        NotificationService notificationService = (NotificationService)
                Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);
        notificationService.markThreadAsRead(mNotificationId);
        return null;
    }

    @Override
    protected void onSuccess(Void result) {
        // no-op
    }
}
