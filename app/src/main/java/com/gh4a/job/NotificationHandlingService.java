package com.gh4a.job;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gh4a.Gh4Application;
import com.gh4a.activities.home.HomeActivity;
import com.meisolsson.githubsdk.model.request.NotificationReadRequest;
import com.meisolsson.githubsdk.service.activity.NotificationService;

import java.util.Date;

public class NotificationHandlingService extends IntentService {
    private static final String EXTRA_REPO_OWNER = "owner";
    private static final String EXTRA_REPO_NAME = "repo";
    private static final String EXTRA_NOTIFICATION_ID = "notification_id";
    private static final String EXTRA_TIMESTAMP = "timestamp";

    private static final String ACTION_MARK_SEEN = "com.gh4a.action.MARK_AS_SEEN";
    private static final String ACTION_MARK_READ = "com.gh4a.action.MARK_AS_READ";
    private static final String ACTION_OPEN_NOTIFICATION = "com.gh4a.action.OPEN_NOTIFICATION";
    private static final String ACTION_HANDLE_NOTIFICATION_DISMISS =
            "com.gh4a.action.HANDLE_NOTIFICATION_DISMISS";

    public static Intent makeMarkNotificationsSeenIntent(Context context) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_MARK_SEEN);
    }

    public static Intent makeHandleDismissIntent(Context context, int notificationId) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_HANDLE_NOTIFICATION_DISMISS)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
    }

    public static Intent makeMarkReposNotificationsAsReadActionIntent(Context context,
            int notificationId, String repoOwner, String repoName) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_MARK_READ)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                .putExtra(EXTRA_REPO_OWNER, repoOwner)
                .putExtra(EXTRA_REPO_NAME, repoName)
                .putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis());
    }

    public static Intent makeOpenNotificationActionIntent(Context context,
            String repoOwner, String repoName) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_OPEN_NOTIFICATION)
                .putExtra(EXTRA_REPO_OWNER, repoOwner)
                .putExtra(EXTRA_REPO_NAME, repoName);
    }

    public NotificationHandlingService() {
        super("NotificationHandlingService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        String repoOwner = intent.getStringExtra(EXTRA_REPO_OWNER);
        String repoName = intent.getStringExtra(EXTRA_REPO_NAME);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);

        switch (intent.getAction()) {
            case ACTION_MARK_SEEN:
                NotificationsJob.markNotificationsAsSeen(this);
                break;
            case ACTION_HANDLE_NOTIFICATION_DISMISS:
                if (notificationId > 0) {
                    NotificationsJob.handleNotificationDismiss(this, notificationId);
                }
                break;
            case ACTION_MARK_READ:
                if (repoOwner != null && repoName != null) {
                    long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0);
                    markNotificationAsRead(repoOwner, repoName, timestamp);
                }
                if (notificationId > 0) {
                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationsJob.handleNotificationDismiss(this, notificationId);
                    notificationManager.cancel(notificationId);
                }
                break;
            case ACTION_OPEN_NOTIFICATION:
                if (repoOwner != null && repoName != null) {
                    startActivity(HomeActivity.makeNotificationsIntent(this, repoOwner, repoName));
                }
                break;
        }
    }

    private void markNotificationAsRead(String repoOwner, String repoName, long timestamp) {
        NotificationService service =
                Gh4Application.get().getGitHubService(NotificationService.class);
        NotificationReadRequest request = NotificationReadRequest.builder()
                .lastReadAt(new Date(timestamp))
                .build();
        try {
            service.markAllRepositoryNotificationsRead(repoOwner, repoName, request).blockingGet();
        } catch (Exception e) {
            Log.w(Gh4Application.LOG_TAG,
                    "Could not mark repo \"" + repoOwner + "/" + repoName + "\" as read", e);
        }
    }
}
