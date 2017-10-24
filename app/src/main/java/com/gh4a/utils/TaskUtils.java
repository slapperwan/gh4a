package com.gh4a.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

public class TaskUtils {
    private TaskUtils() {
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void startNewTask(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }
        context.startActivity(intent);
    }
}
