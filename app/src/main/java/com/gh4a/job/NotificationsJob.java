package com.gh4a.job;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.gh4a.R;
import com.gh4a.activities.home.HomeActivity;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.loader.NotificationHolder;
import com.gh4a.loader.NotificationListLoader;
import com.gh4a.resolver.BrowseFilter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;

import org.eclipse.egit.github.core.Notification;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NotificationsJob extends Job {
    private static final String CHANNEL_GITHUB_NOTIFICATIONS = "channel_notifications";
    private static final String GROUP_ID_GITHUB = "github_notifications";
    private static final int NOTIFICATION_ID_BASE = 10000;
    public static final String TAG = "job_notifications";

    public static void scheduleJob(int intervalMinutes) {
        new JobRequest.Builder(TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(intervalMinutes),
                        TimeUnit.MINUTES.toMillis(5))
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public static void cancelJob() {
        JobManager.instance().cancelAllForTag(NotificationsJob.TAG);
    }

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(CHANNEL_GITHUB_NOTIFICATIONS,
                context.getString(R.string.channel_notifications_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.channel_notifications_description));

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
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

            SharedPreferences prefs = getContext().getSharedPreferences(SettingsFragment.PREF_NAME,
                    Context.MODE_PRIVATE);
            long lastCheck = prefs.getLong(SettingsFragment.KEY_LAST_NOTIFICATION_CHECK, 0);
            int lastCount = prefs.getInt(SettingsFragment.KEY_LAST_NOTIFICATION_COUNT, 0);

            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(getContext());

            if (notifications.isEmpty()) {
                notificationManager.cancelAll();
            } else {
                for (int i = 0; i < lastCount; i++) {
                    // Do not cancel summary notification
                    notificationManager.cancel(NOTIFICATION_ID_BASE + 1 + i);
                }

                int color = ContextCompat.getColor(getContext(), R.color.octodroid);

                showSummaryNotification(notificationManager, notifications, color, lastCheck);
                for (int i = 0; i < notifications.size(); i++) {
                    showSingleNotification(notificationManager, color,
                            notifications.get(i).notification, i, lastCheck);
                }
            }

            prefs.edit()
                    .putLong(SettingsFragment.KEY_LAST_NOTIFICATION_CHECK,
                            System.currentTimeMillis())
                    .putInt(SettingsFragment.KEY_LAST_NOTIFICATION_COUNT, notifications.size())
                    .apply();
        } catch (IOException e) {
            return Result.FAILURE;
        }

        return Result.SUCCESS;
    }

    private void showSingleNotification(NotificationManagerCompat notificationManager,
            int color, Notification notification, int index, long lastCheck) {
        int id = NOTIFICATION_ID_BASE + 1 + index;
        Repository repository = notification.getRepository();
        User owner = repository.getOwner();
        String title = owner.getLogin() + "/" + repository.getName();
        long when = notification.getUpdatedAt().getTime();

        Intent markReadIntent = BrowseFilter.makeMarkNotificationAsReadActionIntent(getContext(),
                notification.getId());
        PendingIntent markReadPendingIntent = PendingIntent.getActivity(getContext(), id,
                markReadIntent, 0);
        NotificationCompat.Action markReadAction = new NotificationCompat.Action(
                R.drawable.mark_read, getContext().getString(R.string.mark_as_read),
                markReadPendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(),
                CHANNEL_GITHUB_NOTIFICATIONS)
                .setSmallIcon(R.drawable.octodroid_bg)
                .setLargeIcon(loadRoundUserAvatar(owner))
                .setGroup(GROUP_ID_GITHUB)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setWhen(when)
                .setShowWhen(true)
                .setLocalOnly(when <= lastCheck)
                .setColor(color)
                .setContentTitle(title)
                .setAutoCancel(true)
                .addAction(markReadAction)
                .setContentText(notification.getSubject().getTitle());

        String url = notification.getSubject().getUrl();
        if (url != null) {
            Uri uri = ApiHelpers.normalizeUri(Uri.parse(url));
            Intent intent = BrowseFilter.makeOpenNotificationActionIntent(getContext(), uri,
                    notification.getId());
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(getContext(), 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
        }

        notificationManager.notify(id, builder.build());
    }

    private void showSummaryNotification(NotificationManagerCompat notificationManager,
            List<NotificationHolder> notifications, int color, long lastCheck) {
        String title = getContext().getString(R.string.unread_notifications_summary_title);
        String text = getContext().getResources()
                .getQuantityString(R.plurals.unread_notifications_summary_text,
                        notifications.size(), notifications.size());
        PendingIntent contentIntent = PendingIntent.getActivity(getContext(), 0,
                HomeActivity.makeIntent(getContext(), R.id.notifications), 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getContext(), CHANNEL_GITHUB_NOTIFICATIONS)
                .setSmallIcon(R.drawable.octodroid_bg)
                .setGroup(GROUP_ID_GITHUB)
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setColor(color)
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(text)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setNumber(notifications.size());

        boolean hasNewNotification = false;
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle(builder)
                .setBigContentTitle(title)
                .setSummaryText(text);
        for (NotificationHolder notification : notifications) {
            inboxStyle.addLine(notification.notification.getSubject().getTitle());

            if (notification.notification.getUpdatedAt().getTime() > lastCheck) {
                hasNewNotification = true;
            }
        }
        builder.setStyle(inboxStyle);

        if (!hasNewNotification) {
            builder.setOnlyAlertOnce(true);
            builder.setLocalOnly(true);
        }

        notificationManager.notify(NOTIFICATION_ID_BASE, builder.build());
    }

    private Bitmap loadRoundUserAvatar(User user) {
        Bitmap avatar = AvatarHandler.loadUserAvatarSynchronously(getContext(), user);
        if (avatar == null) {
            return null;
        }

        final Bitmap output = Bitmap.createBitmap(avatar.getWidth(), avatar.getHeight(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, avatar.getWidth(), avatar.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.BLACK);
        canvas.drawOval(new RectF(rect), paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(avatar, rect, rect, paint);

        return output;
    }
}
