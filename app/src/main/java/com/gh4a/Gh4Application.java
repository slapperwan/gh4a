/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gh4a;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Build;
import android.util.SparseArray;

import com.evernote.android.job.JobManager;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.job.Gh4JobCreator;
import com.gh4a.job.NotificationsJob;
import com.gh4a.utils.CrashReportingHelper;
import com.meisolsson.githubsdk.model.User;

import net.danlew.android.joda.JodaTimeAndroid;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * The Class Gh4Application.
 */
public class Gh4Application extends Application implements OnSharedPreferenceChangeListener {
    public static final String LOG_TAG = "Gh4a";
    public static int THEME = R.style.LightTheme;

    private static Gh4Application sInstance;
    private HashMap<Class<?>, Object> mServiceCache = new HashMap<>();
    private PrettyTime mPt;

    private static final int THEME_DARK = 0;
    private static final int THEME_LIGHT = 1;

    private static final String KEY_VERSION = "version";
    private static final String KEY_ACTIVE_LOGIN = "active_login";
    private static final String KEY_ALL_LOGINS = "logins";
    private static final String KEY_PREFIX_TOKEN = "token_";
    private static final String KEY_PREFIX_USER_ID = "user_id_";

    /*
     * (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        SharedPreferences prefs = getPrefs();
        selectTheme(prefs.getInt(SettingsFragment.KEY_THEME, THEME_LIGHT));

        if (prefs.getInt(KEY_VERSION, 0) < 2) {
            // convert old-style login/token pref to new-style login list
            String login = prefs.getString("USER_LOGIN", null);
            String token = prefs.getString("Token", null);
            HashSet<String> loginSet = new HashSet<>();
            if (login != null && token != null) {
                loginSet.add(login);
            }

            SharedPreferences.Editor editor = prefs.edit()
                    .putString(KEY_ACTIVE_LOGIN, login)
                    .putStringSet(KEY_ALL_LOGINS, loginSet)
                    .putInt(KEY_VERSION, 2)
                    .remove("USER_LOGIN")
                    .remove("Token");
            if (login != null && token != null) {
                editor.putString(KEY_PREFIX_TOKEN + login, token);
            }
            editor.apply();
        }

        prefs.registerOnSharedPreferenceChangeListener(this);

        CrashReportingHelper.onCreate(this);

        mPt = new PrettyTime();
        JodaTimeAndroid.init(this);

        JobManager.create(this).addJobCreator(new Gh4JobCreator());
        updateNotificationJob(prefs);
    }

    private void updateNotificationJob(SharedPreferences prefs) {
        if (isAuthorized() && prefs.getBoolean(SettingsFragment.KEY_NOTIFICATIONS, false)) {
            int intervalMinutes = prefs.getInt(SettingsFragment.KEY_NOTIFICATION_INTERVAL, 15);
            NotificationsJob.scheduleJob(intervalMinutes);
        } else {
            NotificationsJob.cancelJob();
        }
    }

    public <S> S getGitHubService(Class<S> serviceClass) {
        S service = (S) mServiceCache.get(serviceClass);
        if (service == null) {
            service = ServiceFactory.createService(serviceClass, null, null, null);
            mServiceCache.put(serviceClass, service);
        }
        return service;
    }

    private void selectTheme(int theme) {
        switch (theme) {
            case THEME_DARK:
                THEME = R.style.DarkTheme;
                break;
            case THEME_LIGHT:
            case 2: /* for backwards compat with old settings, was light-dark theme */
                THEME = R.style.LightTheme;
                break;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mPt = new PrettyTime(newConfig.getLocales().get(0));
        } else {
            mPt = new PrettyTime(newConfig.locale);
        }
    }

    /* package */ static void trackVisitedUrl(String url) {
        synchronized (Gh4Application.class) {
            CrashReportingHelper.trackVisitedUrl(get(), url);
        }
    }

    public PrettyTime getPrettyTimeInstance() {
        return mPt;
    }

    public void setActiveLogin(String login) {
        if (getPrefs().getStringSet(KEY_ALL_LOGINS, null).contains(login)) {
            getPrefs().edit()
                    .putString(KEY_ACTIVE_LOGIN, login)
                    .apply();
        }
    }

    public String getAuthLogin() {
        return getPrefs().getString(KEY_ACTIVE_LOGIN, null);
    }

    public SparseArray<String> getAccounts() {
        SparseArray<String> accounts = new SparseArray<>();
        for (String login : getPrefs().getStringSet(KEY_ALL_LOGINS, null)) {
            int id = getPrefs().getInt(KEY_PREFIX_USER_ID + login, -1);
            if (id > 0) {
                accounts.put(id, login);
            }
        }
        return accounts;
    }

    public String getAuthToken() {
        String login = getAuthLogin();
        return login != null ? getPrefs().getString(KEY_PREFIX_TOKEN + login, null) : null;
    }

    public void addAccount(User user, String token) {
        SharedPreferences prefs = getPrefs();
        String login = user.login();
        Set<String> logins = prefs.getStringSet(KEY_ALL_LOGINS, null);
        logins.add(login);

        prefs.edit()
                .putString(KEY_ACTIVE_LOGIN, login)
                .putStringSet(KEY_ALL_LOGINS, logins)
                .putString(KEY_PREFIX_TOKEN + login, token)
                .putLong(KEY_PREFIX_USER_ID + login, user.id())
                .apply();

        updateNotificationJob(prefs);
    }

    public User getCurrentAccountInfoForAvatar() {
        String login = getAuthLogin();
        if (login != null) {
            long userId = getPrefs().getLong(KEY_PREFIX_USER_ID + login, -1);
            if (userId >= 0) {
                return User.builder().login(login).id(userId).build();
            }
        }
        return null;
    }

    public void setCurrentAccountInfo(User user) {
        getPrefs().edit()
                .putLong(KEY_PREFIX_USER_ID + user.login(), user.id())
                .apply();
    }

    public void logout() {
        String login = getAuthLogin();
        if (login == null) {
            return;
        }

        Set<String> logins = getPrefs().getStringSet(KEY_ALL_LOGINS, null);
        logins.remove(login);

        getPrefs().edit()
                .putString(KEY_ACTIVE_LOGIN, logins.size() > 0 ? logins.iterator().next() : null)
                .putStringSet(KEY_ALL_LOGINS, logins)
                .remove(KEY_PREFIX_TOKEN + login)
                .remove(KEY_PREFIX_USER_ID + login)
                .apply();

        NotificationsJob.cancelJob();
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(SettingsFragment.PREF_NAME, MODE_PRIVATE);
    }

    public static Gh4Application get() {
        return sInstance;
    }

    public boolean isAuthorized() {
        return getAuthLogin() != null && getAuthToken() != null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsFragment.KEY_THEME)) {
            selectTheme(sharedPreferences.getInt(key, THEME_LIGHT));
        }
    }
}
