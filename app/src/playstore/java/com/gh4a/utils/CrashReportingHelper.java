package com.gh4a.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class CrashReportingHelper {
    private static final int MAX_TRACKED_URLS = 5;
    private static int sNextUrlTrackingPosition = 0;
    private static boolean sHasCrashlytics;

    public static void onCreate(Context context) {
        boolean isDebuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        sHasCrashlytics = !isDebuggable && !isEmulator();

        if (sHasCrashlytics) {
            Fabric.with(context, new Crashlytics());
        }
    }
    public static void trackVisitedUrl(String url) {
        if (sHasCrashlytics) {
            synchronized (CrashReportingHelper.class) {
                Crashlytics.setString("github-url-" + sNextUrlTrackingPosition, url);
                Crashlytics.setInt("last-url-position", sNextUrlTrackingPosition);
                if (++sNextUrlTrackingPosition >= MAX_TRACKED_URLS) {
                    sNextUrlTrackingPosition = 0;
                }
            }
        }
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("unknown")
                || Build.FINGERPRINT.startsWith("unknown")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"));
    }
}