package com.gh4a.job;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.home.HomeActivity;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.resolver.BrowseFilter;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.NotificationService;

import java.io.IOException;

public class NotificationHandlingService extends IntentService {
    private static final String EXTRA_REPOSITORY = "repo";

    private static final String ACTION_MARK_READ = "com.gh4a.action.MARK_AS_READ";
    private static final String ACTION_OPEN_NOTIFICATION = "com.gh4a.action.OPEN_NOTIFICATION";

    public static Intent makeMarkReposNotificationsAsReadActionIntent(Context context,
            Repository repo) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_MARK_READ)
                .putExtra(EXTRA_REPOSITORY, repo);
    }

    public static Intent makeOpenNotificationActionIntent(Context context, Uri uri,
            Repository repo) {
        return new Intent(context, NotificationHandlingService.class)
                .setAction(ACTION_OPEN_NOTIFICATION)
                .setData(uri)
                .putExtra(EXTRA_REPOSITORY, repo);
    }

    public NotificationHandlingService() {
        super("NotificationHandlingService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Repository repo = (Repository) intent.getSerializableExtra(EXTRA_REPOSITORY);
        if (repo == null) {
            return;
        }

        switch (intent.getAction()) {
            case ACTION_MARK_READ:
                markNotificationAsRead(repo);
                break;
            case ACTION_OPEN_NOTIFICATION: {
                SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREF_NAME,
                        Context.MODE_PRIVATE);
                if (prefs.getBoolean(SettingsFragment.KEY_NOTIFICATION_MARK_READ, false)) {
                    markNotificationAsRead(repo);
                }
                if (intent.getData() != null) {
                    startActivity(BrowseFilter.makeRedirectionIntent(this, intent.getData(), null));
                } else {
                    // FIXME: scroll to repo - maybe create separate activity?
                    startActivity(HomeActivity.makeIntent(this, R.id.notifications));
                }
                break;
            }
        }
    }

    private void markNotificationAsRead(Repository repo) {
        NotificationService notificationService = (NotificationService)
                Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);
        try {
            notificationService.markNotificationsAsRead(repo.getOwner().getLogin(), repo.getName());
        } catch (IOException e) {
            Log.w(Gh4Application.LOG_TAG,
                    "Could not mark repo " + repo + " as read", e);
        }
    }
}
