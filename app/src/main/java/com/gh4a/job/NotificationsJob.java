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
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NotificationsJob extends Job {
    private static final String CHANNEL_GITHUB_NOTIFICATIONS = "channel_notifications";
    private static final String GROUP_ID_GITHUB = "github_notifications";
    public static final String TAG = "job_notifications";

    private static final String KEY_LAST_NOTIFICATION_CHECK = "last_notification_check";

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
            List<List<Notification>> notifsGroupedByRepo = new ArrayList<>();
            for (NotificationHolder holder : notifications) {
                if (holder.notification == null) {
                    notifsGroupedByRepo.add(new ArrayList<Notification>());
                } else {
                    List<Notification> list =
                            notifsGroupedByRepo.get(notifsGroupedByRepo.size() - 1);
                    list.add(holder.notification);
                }
            }

            SharedPreferences prefs = getContext().getSharedPreferences(SettingsFragment.PREF_NAME,
                    Context.MODE_PRIVATE);
            long lastCheck = prefs.getLong(KEY_LAST_NOTIFICATION_CHECK, 0);

            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(getContext());

            if (notifications.isEmpty()) {
                notificationManager.cancelAll();
            } else {
                boolean hasNewNotification = false;
                for (List<Notification> list : notifsGroupedByRepo) {
                    for (Notification n : list) {
                        hasNewNotification |= n.getUpdatedAt().getTime() > lastCheck;
                    }
                }

                showSummaryNotification(notificationManager,
                        notifsGroupedByRepo, hasNewNotification);
                for (List<Notification> list : notifsGroupedByRepo) {
                    showRepoNotification(notificationManager, list, lastCheck);
                }
            }

            prefs.edit()
                    .putLong(KEY_LAST_NOTIFICATION_CHECK, System.currentTimeMillis())
                    .apply();
        } catch (IOException e) {
            return Result.FAILURE;
        }

        return Result.SUCCESS;
    }

    private void showRepoNotification(NotificationManagerCompat notificationManager,
            List<Notification> notifications, long lastCheck) {
        Repository repository = notifications.get(0).getRepository();
        final int id = repository.hashCode();
        String title = repository.getOwner().getLogin() + "/" + repository.getName();
        // notifications are sorted by time descending
        long when = notifications.get(0).getUpdatedAt().getTime();
        String text = getContext().getResources()
                .getQuantityString(R.plurals.unread_notifications_summary_text,
                        notifications.size(), notifications.size());

        Intent markReadIntent =
                NotificationHandlingService.makeMarkReposNotificationsAsReadActionIntent(
                        getContext(), id, repository.getOwner().getLogin(), repository.getName());
        PendingIntent markReadPendingIntent = PendingIntent.getService(getContext(), id,
                markReadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action markReadAction = new NotificationCompat.Action(
                R.drawable.mark_read, getContext().getString(R.string.mark_as_read),
                markReadPendingIntent);

        NotificationCompat.Builder builder = makeBaseBuilder()
                .setLargeIcon(loadRoundUserAvatar(repository.getOwner()))
                .setGroup(GROUP_ID_GITHUB)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setWhen(when)
                .setShowWhen(true)
                .setContentTitle(title)
                .setAutoCancel(true)
                .addAction(markReadAction)
                .setContentText(text);

        String url = notifications.size() == 1 ? notifications.get(0).getSubject().getUrl() : null;
        Uri uri = url != null ? ApiHelpers.normalizeUri(Uri.parse(url)) : null;

        Intent intent = NotificationHandlingService.makeOpenNotificationActionIntent(
                getContext(), uri, repository.getOwner().getLogin(), repository.getName());
        PendingIntent pendingIntent = PendingIntent.getService(getContext(), id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        boolean hasNewNotification = false;

        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle("")
                .setConversationTitle(title);
        for (Notification n : notifications) {
            style.addMessage(n.getSubject().getTitle(),
                    n.getUpdatedAt().getTime(), determineNotificationTypeLabel(n));
            hasNewNotification = hasNewNotification ||
                    n.getUpdatedAt().getTime() > lastCheck;
        }
        builder.setStyle(style);

        if (!hasNewNotification) {
            builder.setOnlyAlertOnce(true);
        }

        notificationManager.notify(id, builder.build());
    }

    private void showSummaryNotification(NotificationManagerCompat notificationManager,
            List<List<Notification>> notificationsPerRepo, boolean hasNewNotification) {
        int totalCount = 0;
        for (List<Notification> list : notificationsPerRepo) {
            totalCount += list.size();
        }

        String title = getContext().getString(R.string.unread_notifications_summary_title);
        String text = getContext().getResources()
                .getQuantityString(R.plurals.unread_notifications_summary_text, totalCount,
                        totalCount);

        android.app.Notification publicVersion = makeBaseBuilder()
                .setContentTitle(title)
                .setContentText(text)
                .setNumber(totalCount)
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
                .setNumber(totalCount);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle(builder)
                .setBigContentTitle(text);
        for (List<Notification> list : notificationsPerRepo) {
            Repository repository = list.get(0).getRepository();
            String repoName = repository.getOwner().getLogin() + "/" + repository.getName();
            final TextAppearanceSpan notificationPrimarySpan =
                    new TextAppearanceSpan(getContext(),
                            R.style.TextAppearance_NotificationEmphasized);
            final int emphasisEnd;

            SpannableStringBuilder line = new SpannableStringBuilder(repoName).append(" ");
            if (list.size() == 1) {
                Notification n = list.get(0);
                line.append(determineNotificationTypeLabel(n));
                emphasisEnd = line.length();
                line.append(" ").append(n.getSubject().getTitle());
            } else {
                emphasisEnd = line.length();
                line.append(getContext().getResources().getQuantityString(R.plurals.notification,
                        list.size(), list.size()));
            }

            line.setSpan(notificationPrimarySpan, 0, emphasisEnd,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            inboxStyle.addLine(line);
        }
        builder.setStyle(inboxStyle);

        if (!hasNewNotification) {
            builder.setOnlyAlertOnce(true);
        }

        notificationManager.notify(0, builder.build());
    }

    private String determineNotificationTypeLabel(Notification n) {
        // FIXME
        return n.getSubject().getType();
    }

    private NotificationCompat.Builder makeBaseBuilder() {
        return new NotificationCompat.Builder(getContext(), CHANNEL_GITHUB_NOTIFICATIONS)
                .setSmallIcon(R.drawable.notification)
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
