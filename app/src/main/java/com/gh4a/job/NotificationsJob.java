package com.gh4a.job;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.loader.NotificationHolder;
import com.gh4a.loader.NotificationListLoader;
import com.gh4a.resolver.BrowseFilter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Notification;
import org.eclipse.egit.github.core.Repository;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NotificationsJob extends Job {
    private static final String GROUP_ID_GITHUB = "github_notifications";
    private static final int ID_SUMMARY = 1;
    public static final String TAG = "job_notifications";

    public static void scheduleJob() {
        new JobRequest.Builder(TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public static void cancelJob() {
        JobManager.instance().cancelAllForTag(NotificationsJob.TAG);
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        try {
            List<NotificationHolder> notifications =
                    NotificationListLoader.loadNotifications(false, false);
            // strip out repo items
            Iterator<NotificationHolder> iter = notifications.iterator();
            while (iter.hasNext()) {
                if (iter.next().notification == null) {
                    iter.remove();
                }
            }

            if (notifications.isEmpty()) {
                return Result.SUCCESS;
            }

            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(getContext());
            notificationManager.cancelAll();

            int accentColor = UiUtils.resolveColor(getContext(), R.attr.colorAccent);

            showSummaryNotification(notificationManager, notifications.size(), accentColor);
            for (NotificationHolder holder : notifications) {
                showSingleNotification(notificationManager, accentColor, holder.notification);
            }
        } catch (IOException e) {
            return Result.FAILURE;
        }

        return Result.SUCCESS;
    }

    private void showSingleNotification(NotificationManagerCompat notificationManager,
            int accentColor, Notification notification) {
        Repository repository = notification.getRepository();
        String title = repository.getOwner().getLogin() + "/" + repository.getName();
        long when = notification.getUpdatedAt() != null
                ? notification.getUpdatedAt().getTime()
                : System.currentTimeMillis();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(),
                Gh4Application.CHANNEL_GITHUB_NOTIFICATIONS)
                .setSmallIcon(R.drawable.octodroid)
                .setGroup(GROUP_ID_GITHUB)
                .setWhen(when)
                .setShowWhen(true)
                .setColor(accentColor)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setContentText(notification.getSubject().getTitle());

        String url = notification.getSubject().getUrl();
        if (url != null) {
            Uri uri = ApiHelpers.normalizeUri(Uri.parse(url));
            Intent intent = BrowseFilter.makeRedirectionIntent(getContext(), uri, null);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(getContext(), 0, intent, 0);
            builder.setContentIntent(pendingIntent);
        }

        int id = ID_SUMMARY + 1;
        try {
            id = Integer.parseInt(notification.getId());
        } catch (NumberFormatException e) {
            // ignored
        }
        notificationManager.notify(id, builder.build());
    }

    private void showSummaryNotification(NotificationManagerCompat notificationManager,
            int numNotifications, int accentColor) {
        String title = getContext().getString(R.string.unread_notifications_summary_title);
        String text = getContext().getResources()
                .getQuantityString(R.plurals.unread_notifications_summary_text, numNotifications,
                        numNotifications);
        notificationManager.notify(ID_SUMMARY, new NotificationCompat.Builder(
                getContext(), Gh4Application.CHANNEL_GITHUB_NOTIFICATIONS)
                .setSmallIcon(R.drawable.octodroid)
                .setGroup(GROUP_ID_GITHUB)
                .setGroupSummary(true)
                .setColor(accentColor)
                .setContentTitle(title)
                .setContentText(text)
                .setNumber(numNotifications)
                .build());
    }
}
