package com.gh4a.utils.rx;


import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.gh4a.Gh4Application;

import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class ObservableManager {
    private Gh4Application app;
    private CompositeDisposable subscriptions;
    private HashMap<String, Observable> cache;
    private final MyActivityLifecycleCallbacks callbacks = new MyActivityLifecycleCallbacks();

    public ObservableManager(Gh4Application app) {
        this.app = app;
        this.subscriptions = new CompositeDisposable();
        this.cache = new HashMap<>();
        app.registerActivityLifecycleCallbacks(callbacks);
    }

    public boolean isCached(String key) {
        return this.cache.containsKey(key);
    }

    public synchronized void addCache(String key, Observable observable) {
        this.cache.put(key, observable);
    }

    public synchronized Observable getCached(String key) {
        if(isCached(key)) {
            return this.cache.get(key);
        } else return null;
    }

    public synchronized void addSubscription(Disposable disposable) {
        subscriptions.add(disposable);
    }

    public Observable getObservable() {
        String key = "test_key";

        return Observable.create((ObservableOnSubscribe) e -> {
            Log.d("TEST", "Subscribe called");
            e.onNext(2);
            e.onComplete();
        }).cache().compose(RxTools.handle(app, key));
    }

    public void emptyCache(String key) {
        if(key == null) { // Clear entire cache
            this.cache = new HashMap<>();
            return;
        }

        if(isCached(key)) {
            Log.d("TEST", "ObsManager emptying cache for key : " + key);
            this.cache.remove(key);
        } else Log.d("TEST", "ObsManager key not found");
    }

    public class MyActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            Log.i(activity.getClass().getSimpleName(), "LOOOOG onCreate(Bundle)");
        }

        @Override
        public void onActivityStarted(Activity activity) {
            Log.i(activity.getClass().getSimpleName(), "LOOOOG onStart()");
        }

        @Override
        public void onActivityResumed(Activity activity) {
            Log.i(activity.getClass().getSimpleName(), "LOOOOG onResume()");
        }

        @Override
        public void onActivityPaused(Activity activity) {
            Log.i(activity.getClass().getSimpleName(), "LOOOOG onPause()");
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            Log.i(activity.getClass().getSimpleName(), "LOOOOG onSaveInstanceState(Bundle)");
        }

        @Override
        public void onActivityStopped(Activity activity) {
            Log.i(activity.getClass().getSimpleName(), "LOOOOG onStop()");
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            Log.d("TEST", "NO LEAKS --> removing all subscriptions");
            if(subscriptions != null && subscriptions.isDisposed())
                subscriptions.dispose();
            Log.i(activity.getClass().getSimpleName(), "LOOOOG onDestroy()");
        }
    }
}
