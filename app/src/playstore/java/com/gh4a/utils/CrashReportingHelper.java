package com.gh4a.utils;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.gh4a.BuildConfig;

import io.fabric.sdk.android.Fabric;

public class CrashReportingHelper {
    private static final int MAX_TRACKED_URLS = 5;
    private static int position = 0;
    private static boolean hasCrashlytics;

    public static void onCreate(Application app) {
        boolean isDebuggable = (app.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        hasCrashlytics = (!isDebuggable && !TextUtils.equals(Build.DEVICE, "sdk") && !BuildConfig.FOSS);

        if (hasCrashlytics) {
            Fabric.with(app, new Crashlytics());
        }
    }
    public static void trackVisitedUrl(Application app, String url) {
        if (hasCrashlytics) {
            Crashlytics.setString("github-url-" + position, url);
            Crashlytics.setInt("last-url-position", position);
            if (++position >= MAX_TRACKED_URLS) {
                position = 0;
            }
        }
    }
}