package com.gh4a;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class FabricApplication {
    public static void onCreate(Application app) {
        Fabric.with(app, new Crashlytics());
    }
    public static void trackVisitedUrl(String url, int position) {
        Crashlytics.setString("github-url-" + position, url);
        Crashlytics.setInt("last-url-position", position);
    }
}