package com.gh4a;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class FabricApplication {
    public static void onCreate(Application app) {
        Fabric.with(app, new Crashlytics());
    }
    public static void trackVisitedUrl(String url, int sNextUrlTrackingPosition) {
        Crashlytics.setString("github-url-" + sNextUrlTrackingPosition, url);
        Crashlytics.setInt("last-url-position", sNextUrlTrackingPosition);
    }
}