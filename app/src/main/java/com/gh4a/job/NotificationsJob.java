package com.gh4a.job;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
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
import com.gh4a.adapter.NotificationAdapter;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.loader.NotificationHolder;
import com.gh4a.loader.NotificationListLoader;
import com.gh4a.utils.AvatarHandler;

import org.eclipse.egit.github.core.Notification;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NotificationsJob extends Job {
    private static final String CHANNEL_GITHUB_NOTIFICATIONS = "channel_notifications";
    private static final String GROUP_ID_GITHUB = "github_notifications";
    public static final String TAG = "job_notifications";

    private static final String KEY_LAST_NOTIFICATION_CHECK = "last_notification_check";
    private static final String KEY_LAST_NOTIFICATION_SEEN = "last_notification_seen";
    private static final String KEY_LAST_SHOWN_REPO_IDS = "last_notification_repo_ids";

    private static final Object sPrefsLock = new Object();

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

    public static void markNotificationsAsSeen(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        context.getSharedPreferences(SettingsFragment.PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_NOTIFICATION_SEEN, System.currentTimeMillis())
                .putStringSet(KEY_LAST_SHOWN_REPO_IDS, null)
                .apply();
    }

    public static void handleNotificationDismiss(Context context, int id) {
        SharedPreferences prefs =
                context.getSharedPreferences(SettingsFragment.PREF_NAME, Context.MODE_PRIVATE);
        String idString = String.valueOf(id);

        synchronized (sPrefsLock) {
            Set<String> lastShownRepoIds = prefs.getStringSet(KEY_LAST_SHOWN_REPO_IDS, null);
            if (lastShownRepoIds != null && lastShownRepoIds.contains(idString)) {
                lastShownRepoIds.remove(idString);
                if (lastShownRepoIds.isEmpty()) {
                    // last notification was cleared, so cancel summary notification
                    NotificationManagerCompat nm =
                            NotificationManagerCompat.from(context);
                    nm.cancel(0);
                    lastShownRepoIds = null;
                }
                prefs.edit()
                        .putStringSet(KEY_LAST_SHOWN_REPO_IDS, lastShownRepoIds)
                        .apply();
            }
        }
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        List<List<Notification>> notifsGroupedByRepo = new ArrayList<>();
        try {
            List<NotificationHolder> notifications =
                    NotificationListLoader.loadNotifications(false, false);
            for (NotificationHolder holder : notifications) {
                if (holder.notification == null) {
                    notifsGroupedByRepo.add(new ArrayList<Notification>());
                } else {
                    List<Notification> list =
                            notifsGroupedByRepo.get(notifsGroupedByRepo.size() - 1);
                    list.add(holder.notification);
                }
            }
        } catch (IOException e) {
            return Result.FAILURE;
        }

        synchronized (sPrefsLock) {
            SharedPreferences prefs = getContext().getSharedPreferences(SettingsFragment.PREF_NAME,
                    Context.MODE_PRIVATE);
            long lastCheck = prefs.getLong(KEY_LAST_NOTIFICATION_CHECK, 0);
            long lastSeen = prefs.getLong(KEY_LAST_NOTIFICATION_SEEN, 0);
            Set<String> lastShownRepoIds = prefs.getStringSet(KEY_LAST_SHOWN_REPO_IDS, null);
            Set<String> newShownRepoIds = new HashSet<>();
            boolean hasUnseenNotification = false, hasNewNotification = false;

            for (List<Notification> list : notifsGroupedByRepo) {
                for (Notification n : list) {
                    long timestamp = n.getUpdatedAt().getTime();
                    hasNewNotification |= timestamp > lastCheck;
                    hasUnseenNotification |= timestamp > lastSeen;
                }
            }

            if (!hasUnseenNotification) {
                // seen timestamp is only updated when notifications are canceled by us,
                // so everything is canceled at this point and we have nothing to notify of
                return Result.SUCCESS;
            }

            NotificationManagerCompat nm =
                    NotificationManagerCompat.from(getContext());

            showSummaryNotification(nm, notifsGroupedByRepo, hasNewNotification);
            for (List<Notification> list : notifsGroupedByRepo) {
                showRepoNotification(nm, list, lastCheck);
                String repoId = String.valueOf(list.get(0).getRepository().getId());
                if (lastShownRepoIds != null) {
                    lastShownRepoIds.remove(repoId);
                }
                newShownRepoIds.add(repoId);
            }

            // cancel sub-notifications for repos that no longer have notifications
            if (lastShownRepoIds != null) {
                for (String repoId : lastShownRepoIds) {
                    nm.cancel(Integer.parseInt(repoId));
                }
            }

            prefs.edit()
                    .putLong(KEY_LAST_NOTIFICATION_CHECK, System.currentTimeMillis())
                    .putStringSet(KEY_LAST_SHOWN_REPO_IDS, newShownRepoIds)
                    .apply();
        }

        return Result.SUCCESS;
    }

    private void showRepoNotification(NotificationManagerCompat nm,
            List<Notification> notifications, long lastCheck) {
        Repository repository = notifications.get(0).getRepository();
        final int id = (int) repository.getId();
        String title = repository.getOwner().getLogin() + "/" + repository.getName();
        // notifications are sorted by time descending
        long when = notifications.get(0).getUpdatedAt().getTime();
        String text = getContext().getResources()
                .getQuantityString(R.plurals.unread_notifications_summary_text,
                        notifications.size(), notifications.size());

        Intent intent = NotificationHandlingService.makeOpenNotificationActionIntent(
                getContext(), repository.getOwner().getLogin(), repository.getName());
        PendingIntent contentIntent = PendingIntent.getService(getContext(), id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent deleteIntent = PendingIntent.getService(getContext(), id,
                NotificationHandlingService.makeHandleDismissIntent(getContext(), id),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent markReadIntent =
                NotificationHandlingService.makeMarkReposNotificationsAsReadActionIntent(
                        getContext(), id, repository.getOwner().getLogin(), repository.getName());
        PendingIntent markReadPendingIntent = PendingIntent.getService(getContext(), id,
                markReadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action markReadAction = new NotificationCompat.Action(
                R.drawable.mark_read, getContext().getString(R.string.mark_as_read),
                markReadPendingIntent);

        android.app.Notification publicVersion = makeBaseBuilder()
                .setContentTitle(getContext().getString(R.string.unread_notifications_summary_title))
                .setContentText(text)
                .setNumber(notifications.size())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        NotificationCompat.Builder builder = makeBaseBuilder()
                .setLargeIcon(loadRoundUserAvatar(repository.getOwner()))
                .setGroup(GROUP_ID_GITHUB)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setWhen(when)
                .setShowWhen(true)
                .setNumber(notifications.size())
                .setPublicVersion(publicVersion)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentTitle(title)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setAutoCancel(true)
                .addAction(markReadAction)
                .setContentText(text);

        boolean hasNewNotification = false;

        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle("")
                .setConversationTitle(title);
        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification n = notifications.get(i);
            style.addMessage(n.getSubject().getTitle(),
                    n.getUpdatedAt().getTime(), determineNotificationTypeLabel(n));
            hasNewNotification = hasNewNotification ||
                    n.getUpdatedAt().getTime() > lastCheck;
        }
        builder.setStyle(style);

        if (!hasNewNotification) {
            builder.setOnlyAlertOnce(true);
        }

        nm.notify(id, builder.build());
    }

    private void showSummaryNotification(NotificationManagerCompat nm,
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
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        PendingIntent contentIntent = PendingIntent.getActivity(getContext(), 0,
                HomeActivity.makeIntent(getContext(), R.id.notifications), 0);
        PendingIntent deleteIntent = PendingIntent.getService(getContext(), 0,
                NotificationHandlingService.makeMarkNotificationsSeenIntent(getContext()), 0);
        NotificationCompat.Builder builder = makeBaseBuilder()
                .setGroup(GROUP_ID_GITHUB)
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setContentTitle(title)
                .setContentText(text)
                .setPublicVersion(publicVersion)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
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

        nm.notify(0, builder.build());
    }

    private String determineNotificationTypeLabel(Notification n) {
        final Resources res = getContext().getResources();
        switch (n.getSubject().getType()) {
            case NotificationAdapter.SUBJECT_COMMIT:
                return res.getString(R.string.notification_subject_commit);
            case NotificationAdapter.SUBJECT_ISSUE:
                return res.getString(R.string.notification_subject_issue);
            case NotificationAdapter.SUBJECT_PULL_REQUEST:
                return res.getString(R.string.notification_subject_pr);
            case NotificationAdapter.SUBJECT_RELEASE:
                return res.getString(R.string.notification_subject_release);
        }
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
