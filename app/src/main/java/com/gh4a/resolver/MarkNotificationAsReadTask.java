package com.gh4a.resolver;

import android.content.Context;

import com.gh4a.BackgroundTask;
import com.gh4a.Gh4Application;
import com.meisolsson.githubsdk.service.activity.NotificationService;

import java.io.IOException;

public class MarkNotificationAsReadTask extends BackgroundTask<Void> {
    private final String mNotificationId;

    public MarkNotificationAsReadTask(Context context, String notificationId) {
        super(context);
        mNotificationId = notificationId;
    }

    @Override
    protected Void run() throws IOException {
        NotificationService service =
                Gh4Application.get().getGitHubService(NotificationService.class);
        service.markNotificationRead(mNotificationId).blockingGet();
        return null;
    }

    @Override
    protected void onSuccess(Void result) {
        // no-op
    }
}
