package com.gh4a.job;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gh4a.Gh4Application;
import com.gh4a.activities.home.HomeActivity;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.resolver.BrowseFilter;

import org.eclipse.egit.github.core.service.NotificationService;

import java.io.IOException;

public class NotificationHandlingService extends IntentService {
    private static final String EXTRA_REPO_OWNER = "owner";
    private static final String EXTRA_REPO_NAME = "repo";
    private static final String EXTRA_NOTIFICATION_ID = "notification_id";

    private static final String ACTION_MARK_SEEN = "com.gh4a.action.MARK_AS_SEEN";
    private static final String ACTION_MARK_READ = "com.gh4a.action.MARK_AS_READ";
    private static final String ACTION_OPEN_NOTIFICATION = "com.gh4a.action.OPEN_NOTIFICATION";

    public static Intent makeMarkNotificationsSeenIntent(Context context) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_MARK_SEEN);
    }

    public static Intent makeMarkReposNotificationsAsReadActionIntent(Context context,
            int notificationId, String repoOwner, String repoName) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_MARK_READ)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                .putExtra(EXTRA_REPO_OWNER, repoOwner)
                .putExtra(EXTRA_REPO_NAME, repoName);
    }

    public static Intent makeOpenNotificationActionIntent(Context context, Uri uri,
            String repoOwner, String repoName) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_OPEN_NOTIFICATION)
                .setData(uri)
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
        if (repoOwner == null || repoName == null) {
            return;
        }

        switch (intent.getAction()) {
            case ACTION_MARK_SEEN:
                NotificationsJob.markNotificationsAsSeen(this);
                break;
            case ACTION_MARK_READ: {
                markNotificationAsRead(repoOwner, repoName);
                cancelNotification(intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1));
                break;
            }
            case ACTION_OPEN_NOTIFICATION: {
                openNotification(intent.getData(), repoOwner, repoName);
                break;
            }
        }
    }

    private void openNotification(@Nullable Uri uri, String repoOwner, String repoName) {
        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREF_NAME,
                Context.MODE_PRIVATE);
        if (prefs.getBoolean(SettingsFragment.KEY_NOTIFICATION_MARK_READ, false)) {
            markNotificationAsRead(repoOwner, repoName);
        }
        if (uri != null) {
            startActivity(BrowseFilter.makeRedirectionIntent(this, uri, null));
        } else {
            startActivity(HomeActivity.makeNotificationsIntent(this, repoOwner, repoName));
        }
    }

    private void cancelNotification(int notificationId) {
        if (notificationId >= 0) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }

    private void markNotificationAsRead(String repoOwner, String repoName) {
        NotificationService notificationService = (NotificationService)
                Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);
        try {
            notificationService.markNotificationsAsRead(repoOwner, repoName);
        } catch (IOException e) {
            Log.w(Gh4Application.LOG_TAG,
                    "Could not mark repo \"" + repoOwner + "/" + repoName + "\" as read", e);
        }
    }
}
