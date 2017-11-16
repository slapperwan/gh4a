package com.gh4a.utils;

import android.app.Application;

import com.tspoon.traceur.Traceur;

public class DebuggingHelper {
    public static void onCreate(Application app) {
        Traceur.enableLogging();
    }
}
