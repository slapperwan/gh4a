package com.gh4a.utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

public class TaskUtils {
    private TaskUtils() {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void startMatchingOrNewTask(Context context, Intent intent,
            IntentMatcher matcher) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            context.startActivity(intent);
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            startNewTask(context, intent);
            return;
        }

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.AppTask task : activityManager.getAppTasks()) {
            Intent taskIntent = task.getTaskInfo().baseIntent;
            Bundle taskExtras = taskIntent.getExtras();
            if (!taskIntent.getComponent().equals(intent.getComponent()) || taskExtras == null) {
                continue;
            }

            if (matcher.matches(extras, taskExtras)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                task.startActivity(context, intent, null);
                return;
            }
        }

        startNewTask(context, intent);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void startNewTask(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    public interface IntentMatcher {
        boolean matches(@NonNull Bundle a, @NonNull Bundle b);
    }
}
