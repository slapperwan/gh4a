package com.gh4a.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

public class TaskUtils {
    private static final String EXTRA_NEW_TASK = "TaskUtils.new_task";

    private TaskUtils() {
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void startNewTask(@NonNull Context context, @NonNull Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }
        intent.putExtra(EXTRA_NEW_TASK, true);
        context.startActivity(intent);
    }

    public static boolean isNewTaskIntent(@Nullable Intent intent) {
        return intent != null && intent.getBooleanExtra(EXTRA_NEW_TASK, false);
    }
}
