package com.gh4a.job;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gh4a.Gh4Application;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.resolver.BrowseFilter;

import org.eclipse.egit.github.core.service.NotificationService;

import java.io.IOException;

public class NotificationHandlingService extends IntentService {
    private static final String EXTRA_NOTIFICATION_ID = "notification_id";

    private static final String ACTION_MARK_READ = "com.gh4a.action.MARK_AS_READ";
    private static final String ACTION_OPEN_NOTIFICATION = "com.gh4a.action.OPEN_NOTIFICATION";

    public static Intent makeMarkNotificationAsReadActionIntent(Context context,
            String notificationId) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_MARK_READ)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
    }

    public static Intent makeOpenNotificationActionIntent(Context context, Uri uri,
            String notificationId) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_OPEN_NOTIFICATION)
                .setData(uri)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
    }

    public NotificationHandlingService() {
        super("NotificationHandlingService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID);
        if (notificationId == null) {
            return;
        }

        switch (intent.getAction()) {
            case ACTION_MARK_READ:
                markNotificationAsRead(notificationId);
                break;
            case ACTION_OPEN_NOTIFICATION: {
                SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREF_NAME,
                        Context.MODE_PRIVATE);
                if (prefs.getBoolean(SettingsFragment.KEY_NOTIFICATION_MARK_READ, false)) {
                    markNotificationAsRead(notificationId);
                }

                Intent redirectIntent = BrowseFilter.makeRedirectionIntent(this,
                        intent.getData(), null);
                startActivity(redirectIntent);
                break;
            }
        }
    }

    private void markNotificationAsRead(String notificationId) {
        NotificationService notificationService = (NotificationService)
                Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);
        try {
            notificationService.markThreadAsRead(notificationId);
        } catch (IOException e) {
            Log.w(Gh4Application.LOG_TAG,
                    "Could not mark notification thread " + notificationId + " as read", e);
        }
    }
}
