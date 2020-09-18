package com.gh4a.worker;

import android.app.Notification;
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
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.gh4a.R;
import com.gh4a.activities.home.HomeActivity;
import com.gh4a.adapter.NotificationAdapter;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.model.NotificationHolder;
import com.gh4a.model.NotificationListLoadResult;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.SingleFactory;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NotificationsWorker extends Worker {
    private static final String TAG = "NotificationsWorker";

    private static final String CHANNEL_GITHUB_NOTIFICATIONS = "channel_notifications";
    private static final String GROUP_ID_GITHUB = "github_notifications";
    private static final String WORK_TAG = "job_notifications";

    private static final String KEY_LAST_NOTIFICATION_CHECK = "last_notification_check";
    private static final String KEY_LAST_NOTIFICATION_SEEN = "last_notification_seen";
    private static final String KEY_LAST_SHOWN_REPO_IDS = "last_notification_repo_ids";

    private static final Object sPrefsLock = new Object();

    public static void schedule(Context context, int intervalMinutes) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(NotificationsWorker.class, intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .build();
        Log.d(TAG, "Scheduling notification fetch to happen every " + intervalMinutes + " min");
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE, request);
    }

    public static void cancel(Context context) {
        Log.d(TAG, "Canceling notification fetch");
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG);
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

        synchronized (sPrefsLock) {
            getPrefs(context)
                    .edit()
                    .putLong(KEY_LAST_NOTIFICATION_SEEN, System.currentTimeMillis())
                    .putStringSet(KEY_LAST_SHOWN_REPO_IDS, null)
                    .apply();
        }
    }

    public static long getLastCheckTimestamp(Context context) {
        SharedPreferences prefs = getPrefs(context);
        if (!prefs.getBoolean(SettingsFragment.KEY_NOTIFICATIONS, false)) {
            return 0;
        }
        return prefs.getLong(KEY_LAST_NOTIFICATION_CHECK, 0);
    }

    public static void handleNotificationDismiss(Context context, int id) {
        SharedPreferences prefs = getPrefs(context);
        String idString = String.valueOf(id);

        synchronized (sPrefsLock) {
            Set<String> lastShownRepoIds = StringUtils.getEditableStringSetFromPrefs(prefs, KEY_LAST_SHOWN_REPO_IDS);
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

    public NotificationsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<List<NotificationThread>> notifsGroupedByRepo = new ArrayList<>();
        try {
            Log.d(TAG, "Starting notification fetch in background");
            NotificationListLoadResult result =
                    SingleFactory.getNotifications(false, false, false).blockingGet();
            for (NotificationHolder holder : result.notifications) {
                if (holder.notification == null) {
                    notifsGroupedByRepo.add(new ArrayList<>());
                } else {
                    List<NotificationThread> list =
                            notifsGroupedByRepo.get(notifsGroupedByRepo.size() - 1);
                    list.add(holder.notification);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed fetching notifications", e);
            return Result.failure();
        }

        synchronized (sPrefsLock) {
            SharedPreferences prefs = getPrefs(getApplicationContext());
            long lastCheck = prefs.getLong(KEY_LAST_NOTIFICATION_CHECK, 0);
            long lastSeen = prefs.getLong(KEY_LAST_NOTIFICATION_SEEN, 0);
            Set<String> lastShownRepoIds =
                    StringUtils.getEditableStringSetFromPrefs(prefs, KEY_LAST_SHOWN_REPO_IDS);
            Set<String> newShownRepoIds = new HashSet<>();
            boolean hasUnseenNotification = false, hasNewNotification = false;

            for (List<NotificationThread> list : notifsGroupedByRepo) {
                for (NotificationThread n : list) {
                    long timestamp = n.updatedAt().getTime();
                    hasNewNotification |= timestamp > lastCheck;
                    hasUnseenNotification |= timestamp > lastSeen;
                }
            }

            Log.d(TAG, "Last check was " + new Date(lastCheck) + ", last seen " + new Date(lastSeen)
                    + " -> has new " + hasNewNotification + ", has unseen " + hasUnseenNotification);

            if (!hasUnseenNotification) {
                // Seen timestamp is only updated when notifications are canceled by us,
                // so everything is canceled at this point and we have nothing to notify of.
                // We don't update the last check timestamp here, since it's fine for the UI
                // to _not_ discard the already seen notifications in the list.
                return Result.success();
            }

            NotificationManagerCompat nm =
                    NotificationManagerCompat.from(getApplicationContext());

            showSummaryNotification(nm, notifsGroupedByRepo, hasNewNotification);
            for (List<NotificationThread> list : notifsGroupedByRepo) {
                showRepoNotification(nm, list, lastCheck);
                String repoId = String.valueOf(list.get(0).repository().id());
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

        return Result.success();
    }

    private void showRepoNotification(NotificationManagerCompat nm,
            List<NotificationThread> notifications, long lastCheck) {
        final Context context = getApplicationContext();
        Repository repository = notifications.get(0).repository();
        final int id = repository.id().intValue();
        String title = ApiHelpers.formatRepoName(getApplicationContext(), repository);
        // notifications are sorted by time descending
        long when = notifications.get(0).updatedAt().getTime();
        String text = context.getResources().getQuantityString(
                R.plurals.unread_notifications_summary_text,
                notifications.size(), notifications.size());

        Intent intent = NotificationHandlingService.makeOpenNotificationActionIntent(
                context, repository.owner().login(), repository.name());
        PendingIntent contentIntent = PendingIntent.getService(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent deleteIntent = PendingIntent.getService(context, id,
                NotificationHandlingService.makeHandleDismissIntent(context, id),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent markReadIntent =
                NotificationHandlingService.makeMarkReposNotificationsAsReadActionIntent(
                        context, id, repository.owner().login(), repository.name());
        PendingIntent markReadPendingIntent = PendingIntent.getService(context, id,
                markReadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action markReadAction = new NotificationCompat.Action(
                R.drawable.mark_read, context.getString(R.string.mark_as_read),
                markReadPendingIntent);

        android.app.Notification publicVersion = makeBaseBuilder()
                .setContentTitle(context.getString(R.string.unread_notifications_summary_title))
                .setContentText(text)
                .setNumber(notifications.size())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        NotificationCompat.Builder builder = makeBaseBuilder()
                .setLargeIcon(loadRoundUserAvatar(repository.owner()))
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
            NotificationThread n = notifications.get(i);
            style.addMessage(n.subject().title(),
                    n.updatedAt().getTime(), determineNotificationTypeLabel(n));
            hasNewNotification = hasNewNotification ||
                    n.updatedAt().getTime() > lastCheck;
        }
        builder.setStyle(style);

        if (!hasNewNotification) {
            builder.setOnlyAlertOnce(true);
        }

        nm.notify(id, builder.build());
    }

    private void showSummaryNotification(NotificationManagerCompat nm,
            List<List<NotificationThread>> notificationsPerRepo, boolean hasNewNotification) {
        final Context context = getApplicationContext();
        int totalCount = 0;
        for (List<NotificationThread> list : notificationsPerRepo) {
            totalCount += list.size();
        }

        String title = context.getString(R.string.unread_notifications_summary_title);
        String text = context.getResources().getQuantityString(
                R.plurals.unread_notifications_summary_text, totalCount, totalCount);

        Notification publicVersion = makeBaseBuilder()
                .setContentTitle(title)
                .setContentText(text)
                .setNumber(totalCount)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                HomeActivity.makeIntent(context, R.id.notifications), 0);
        PendingIntent deleteIntent = PendingIntent.getService(context, 0,
                NotificationHandlingService.makeMarkNotificationsSeenIntent(context), 0);
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
        for (List<NotificationThread> list : notificationsPerRepo) {
            String repoName = ApiHelpers.formatRepoName(getApplicationContext(), list.get(0).repository());
            final TextAppearanceSpan notificationPrimarySpan =
                    new TextAppearanceSpan(context, R.style.TextAppearance_NotificationEmphasized);
            final int emphasisEnd;

            SpannableStringBuilder line = new SpannableStringBuilder(repoName).append(" ");
            if (list.size() == 1) {
                NotificationThread n = list.get(0);
                line.append(determineNotificationTypeLabel(n));
                emphasisEnd = line.length();
                line.append(" ").append(n.subject().title());
            } else {
                emphasisEnd = line.length();
                line.append(context.getResources().getQuantityString(R.plurals.notification,
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

    private String determineNotificationTypeLabel(NotificationThread n) {
        final Resources res = getApplicationContext().getResources();
        switch (n.subject().type()) {
            case NotificationAdapter.SUBJECT_COMMIT:
                return res.getString(R.string.notification_subject_commit);
            case NotificationAdapter.SUBJECT_ISSUE:
                return res.getString(R.string.notification_subject_issue);
            case NotificationAdapter.SUBJECT_PULL_REQUEST:
                return res.getString(R.string.notification_subject_pr);
            case NotificationAdapter.SUBJECT_RELEASE:
                return res.getString(R.string.notification_subject_release);
        }
        return n.subject().type();
    }

    private NotificationCompat.Builder makeBaseBuilder() {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_GITHUB_NOTIFICATIONS)
                .setSmallIcon(R.drawable.notification)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.octodroid));
    }

    private Bitmap loadRoundUserAvatar(User user) {
        Bitmap avatar = AvatarHandler.loadUserAvatarSynchronously(getApplicationContext(), user);
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

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(SettingsFragment.PREF_NAME, Context.MODE_PRIVATE);
    }
}
