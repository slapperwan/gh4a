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
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.gh4a.R;
import com.gh4a.activities.home.HomeActivity;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.loader.NotificationHolder;
import com.gh4a.loader.NotificationListLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;

import org.eclipse.egit.github.core.Notification;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NotificationsJob extends Job {
    private static final String CHANNEL_GITHUB_NOTIFICATIONS = "channel_notifications";
    private static final String GROUP_ID_GITHUB = "github_notifications";
    public static final String TAG = "job_notifications";

    private static final String KEY_LAST_NOTIFICATION_CHECK = "last_notification_check";
    private static final String KEY_LAST_NOTIFICATION_IDS = "last_notification_ids";

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
            long lastCheck = prefs.getLong(KEY_LAST_NOTIFICATION_CHECK, 0);
            Set<String> newPostedIds = null;

            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(getContext());

            if (notifications.isEmpty()) {
                notificationManager.cancelAll();
            } else {
                ArrayList<Notification> added = new ArrayList<>();
                Set<String> lastIds = prefs.getStringSet(KEY_LAST_NOTIFICATION_IDS, null);

                for (NotificationHolder n : notifications) {
                    if (lastIds != null && lastIds.contains(n.notification.getId())) {
                        lastIds.remove(n.notification.getId());
                    } else {
                        added.add(n.notification);
                    }
                }
                // What's in lastIds now are the removed notifications, so cancel those
                if (lastIds != null) {
                    for (String id : lastIds) {
                        notificationManager.cancel(id.hashCode());
                    }
                }

                if ((lastIds != null && !lastIds.isEmpty()) || !added.isEmpty()) {
                    showSummaryNotification(notificationManager,
                            notifications, lastCheck, !added.isEmpty());
                }
                for (int i = 0; i < added.size(); i++) {
                    showSingleNotification(notificationManager, added.get(i), lastCheck);
                }
                newPostedIds = new HashSet<>();
                for (NotificationHolder n : notifications) {
                    newPostedIds.add(n.notification.getId());
                }
            }

            prefs.edit()
                    .putLong(KEY_LAST_NOTIFICATION_CHECK, System.currentTimeMillis())
                    .putStringSet(KEY_LAST_NOTIFICATION_IDS, newPostedIds)
                    .apply();
        } catch (IOException e) {
            return Result.FAILURE;
        }

        return Result.SUCCESS;
    }

    private void showSingleNotification(NotificationManagerCompat notificationManager,
            Notification notification, long lastCheck) {
        int id = notification.getId().hashCode();
        Repository repository = notification.getRepository();
        User owner = repository.getOwner();
        String title = owner.getLogin() + "/" + repository.getName();
        long when = notification.getUpdatedAt().getTime();

        Intent markReadIntent = NotificationHandlingService.makeMarkNotificationAsReadActionIntent(
                getContext(), notification.getId());
        PendingIntent markReadPendingIntent = PendingIntent.getService(getContext(), id,
                markReadIntent, 0);
        NotificationCompat.Action markReadAction = new NotificationCompat.Action(
                R.drawable.mark_read, getContext().getString(R.string.mark_as_read),
                markReadPendingIntent);

        NotificationCompat.Builder builder = makeBaseBuilder()
                .setLargeIcon(loadRoundUserAvatar(owner))
                .setGroup(GROUP_ID_GITHUB)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setWhen(when)
                .setShowWhen(true)
                .setContentTitle(title)
                .setAutoCancel(true)
                .addAction(markReadAction)
                .setContentText(notification.getSubject().getTitle());

        String url = notification.getSubject().getUrl();
        if (url != null) {
            Uri uri = ApiHelpers.normalizeUri(Uri.parse(url));
            Intent intent = NotificationHandlingService.makeOpenNotificationActionIntent(
                    getContext(), uri, notification.getId());
            PendingIntent pendingIntent = PendingIntent.getService(getContext(), id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);
        }

        notificationManager.notify(id, builder.build());
    }

    private void showSummaryNotification(NotificationManagerCompat notificationManager,
            List<NotificationHolder> notifications, long lastCheck, boolean hasNewNotification) {
        String title = getContext().getString(R.string.unread_notifications_summary_title);
        String text = getContext().getResources()
                .getQuantityString(R.plurals.unread_notifications_summary_text,
                        notifications.size(), notifications.size());

        android.app.Notification publicVersion = makeBaseBuilder()
                .setContentTitle(title)
                .setContentText(text)
                .setNumber(notifications.size())
                .build();

        PendingIntent contentIntent = PendingIntent.getActivity(getContext(), 0,
                HomeActivity.makeIntent(getContext(), R.id.notifications), 0);
        NotificationCompat.Builder builder = makeBaseBuilder()
                .setGroup(GROUP_ID_GITHUB)
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(text)
                .setPublicVersion(publicVersion)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setNumber(notifications.size());

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle(builder)
                .setBigContentTitle(title)
                .setSummaryText(text);
        for (int i = 0; i < notifications.size() && i < 10; i++) {
            Notification n = notifications.get(i).notification;
            Repository repository = n.getRepository();
            String repoName = repository.getOwner().getLogin() + "/" + repository.getName();
            final TextAppearanceSpan notificationPrimarySpan =
                    new TextAppearanceSpan(getContext(), R.style.TextAppearance_NotificationEmphasized);
            SpannableStringBuilder line = new SpannableStringBuilder(repoName)
                    .append(" ")
                    .append(n.getSubject().getTitle());
            line.setSpan(notificationPrimarySpan, 0, repoName.length(),
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            inboxStyle.addLine(line);
        }
        builder.setStyle(inboxStyle);

        if (!hasNewNotification) {
            builder.setOnlyAlertOnce(true);
        }

        notificationManager.notify(0, builder.build());
    }

    private NotificationCompat.Builder makeBaseBuilder() {
        return new NotificationCompat.Builder(getContext(), CHANNEL_GITHUB_NOTIFICATIONS)
                .setSmallIcon(R.drawable.octodroid_bg)
                .setColor(ContextCompat.getColor(getContext(), R.color.octodroid));
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
