package com.gh4a.utils;

import com.gh4a.BuildConfig;

public class ApiKeyHelper {
    private static final String GITHUB_FDROID_CLIENT_ID = "SOMETEXT";
    private static final String GITHUB_FDROID_SECRET = "SOMETEXT";

    public static String getClientId() {
        if (BuildConfig.FOSS) {
            return GITHUB_FDROID_CLIENT_ID;
        } else {
            return BuildConfig.CLIENT_ID;
        }
    }

    public static String getSecret() {
        if (BuildConfig.FOSS) {
            return GITHUB_FDROID_SECRET;
        } else {
            return BuildConfig.CLIENT_SECRET;
        }
    }
}