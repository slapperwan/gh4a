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
package com.gh4a.activities;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.egit.github.core.Authorization;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.OAuthService;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.gh4a.BaseActivity;
import com.gh4a.ClientForAuthorization;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.TwoFactorAuthException;
import com.gh4a.adapter.DrawerAdapter;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

/**
 * The Github4Android activity.
 */
public class Github4AndroidActivity extends BaseActivity {
    private static final int REQUEST_SETTINGS = 10000;

    private static final int ITEM_SEARCH = 1;
    private static final int ITEM_BOOKMARKS = 2;
    private static final int ITEM_SETTINGS = 3;
    private static final int ITEM_TIMELINE = 4;
    private static final int ITEM_TRENDING = 5;
    private static final int ITEM_BLOG = 6;

    private static final List<DrawerAdapter.Item> DRAWER_ITEMS = Arrays.asList(
        new DrawerAdapter.SectionHeaderItem(R.string.navigation),
        new DrawerAdapter.EntryItem(R.string.search, 0, ITEM_SEARCH),
        new DrawerAdapter.EntryItem(R.string.bookmarks, 0, ITEM_BOOKMARKS),
        new DrawerAdapter.DividerItem(),
        new DrawerAdapter.SectionHeaderItem(R.string.explore),
        new DrawerAdapter.EntryItem(R.string.pub_timeline, 0, ITEM_TIMELINE),
        new DrawerAdapter.EntryItem(R.string.trend, 0, ITEM_TRENDING),
        new DrawerAdapter.EntryItem(R.string.blog, 0, ITEM_BLOG),
        new DrawerAdapter.DividerItem(),
        new DrawerAdapter.EntryItem(R.string.settings, 0, ITEM_SETTINGS)
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Gh4Application app = Gh4Application.get();
        if (app.isAuthorized()) {
            goToToplevelActivity(false);
            finish();
        } else if (!hasErrorView()) {
            setContentView(R.layout.main);
        }
    }

    @Override
    protected ListAdapter getNavigationDrawerAdapter() {
        return new DrawerAdapter(this, DRAWER_ITEMS);
    }

    @Override
    protected boolean onDrawerItemSelected(int position) {
        switch (DRAWER_ITEMS.get(position).getId()) {
            case ITEM_SETTINGS:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
            case ITEM_SEARCH:
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            case ITEM_BOOKMARKS:
                startActivity(new Intent(this, BookmarkListActivity.class));
                return true;
            case ITEM_TIMELINE:
                startActivity(new Intent(this, TimelineActivity.class));
                return true;
            case ITEM_BLOG:
                startActivity(new Intent(this, BlogListActivity.class));
                return true;
            case ITEM_TRENDING:
                startActivity(new Intent(this, TrendingActivity.class));
                return true;
        }
        return super.onDrawerItemSelected(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.login);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Menu.FIRST:
                doLogin();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS) {
            if (data.getBooleanExtra(SettingsActivity.RESULT_EXTRA_THEME_CHANGED, false)) {
                Intent intent = new Intent(getIntent());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void doLogin() {
        EditText loginView = (EditText) findViewById(R.id.et_username_main);
        EditText passwordView = (EditText) findViewById(R.id.et_password_main);

        UiUtils.hideImeForView(loginView);
        UiUtils.hideImeForView(passwordView);

        String username = loginView.getText().toString().trim();
        String password = passwordView.getText().toString();

        if (!StringUtils.checkEmail(username)) {
            AsyncTaskCompat.executeParallel(new LoginTask(username, password));
        } else {
            Toast.makeText(Github4AndroidActivity.this,
                    getString(R.string.enter_username_toast), Toast.LENGTH_LONG).show();
        }
    }

    private class LoginTask extends ProgressDialogTask<Authorization> {
        private String mUserName;
        private String mPassword;
        private String mOtpCode;

        /**
         * Instantiates a new load repository list task.
         */
        public LoginTask(String userName, String password) {
            super(Github4AndroidActivity.this, R.string.please_wait, R.string.authenticating);
            mUserName = userName;
            mPassword = password;
        }
        
        public LoginTask(String userName, String password, String otpCode) {
            super(Github4AndroidActivity.this, R.string.please_wait, R.string.authenticating);
            mUserName = userName;
            mPassword = password;
            mOtpCode = otpCode;
        }

        public LoginTask(FragmentActivity activity, int resWaitId, int resWaitMsg) {
            super(activity, resWaitId, resWaitMsg);
        }

        @Override
        protected Authorization run() throws IOException {
            GitHubClient client = new ClientForAuthorization(mOtpCode);
            client.setCredentials(mUserName, mPassword);
            client.setUserAgent("Octodroid");

            String description = "Octodroid - " + Build.MANUFACTURER + " " + Build.MODEL;
            String fingerprint = getHashedDeviceId();
            int index = 1;

            OAuthService authService = new OAuthService(client);
            for (Authorization authorization : authService.getAuthorizations()) {
                String note = authorization.getNote();
                if ("Gh4a".equals(note)) {
                    authService.deleteAuthorization(authorization.getId());
                } else if (note != null && note.startsWith("Octodroid")) {
                    if (fingerprint.equals(authorization.getFingerprint())) {
                        authService.deleteAuthorization(authorization.getId());
                    } else if (note.startsWith(description)) {
                        index++;
                    }
                }
            }

            if (index > 1) {
                description += " #" + index;
            }

            Authorization auth = new Authorization();
            auth.setNote(description);
            auth.setUrl("http://github.com/slapperwan/gh4a");
            auth.setFingerprint(fingerprint);
            auth.setScopes(Arrays.asList("user", "repo", "gist"));

            return authService.createAuthorization(auth);
        }

        @Override
        protected void onError(Exception e) {
            if (e instanceof TwoFactorAuthException) {
                if ("sms".equals(((TwoFactorAuthException) e).getTwoFactorAuthType())) {
                    AsyncTaskCompat.executeParallel(new DummyPostTask(mUserName, mPassword));
                } else {
                    open2FADialog(mUserName, mPassword);
                }
            } else {
                Toast.makeText(Github4AndroidActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onSuccess(Authorization result) {
            SharedPreferences sharedPreferences = getSharedPreferences(
                    SettingsFragment.PREF_NAME, MODE_PRIVATE);
            Editor editor = sharedPreferences.edit();
            editor.putString(Constants.User.AUTH_TOKEN, result.getToken());
            editor.putString(Constants.User.LOGIN, mUserName);
            editor.apply();

            goToToplevelActivity(false);
            finish();
        }

        private String getHashedDeviceId() {
            String androidId = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            if (androidId == null) {
                // shouldn't happen, do a lame fallback in that case
                androidId = Build.FINGERPRINT;
            }

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] result = digest.digest(androidId.getBytes("UTF-8"));
                StringBuilder sb = new StringBuilder();
                for (byte b : result) {
                    sb.append(String.format(Locale.US, "%02X", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                // won't happen
                return androidId;
            }
        }
    }

    // POST request so that GitHub trigger the SMS for OTP
    private class DummyPostTask extends LoginTask {
        private String mUserName;
        private String mPassword;

        public DummyPostTask(String userName, String password) {
            super(Github4AndroidActivity.this, R.string.please_wait, R.string.authenticating);
            mUserName = userName;
            mPassword = password;
        }

        @Override
        protected Authorization run() throws IOException {
            GitHubClient client = new ClientForAuthorization(null);
            client.setCredentials(mUserName, mPassword);
            client.setUserAgent("Octodroid");

            Authorization auth = new Authorization();
            auth.setNote("Gh4a login dummy");

            OAuthService authService = new OAuthService(client);
            return authService.createAuthorization(auth);
        }

        @Override
        protected void onError(Exception e) {
            if (e instanceof TwoFactorAuthException) {
                open2FADialog(mUserName, mPassword);
            } else {
                Toast.makeText(Github4AndroidActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void open2FADialog(final String username, final String password) {
        LayoutInflater inflater = LayoutInflater.from(Github4AndroidActivity.this);
        View authDialog = inflater.inflate(R.layout.twofactor_auth_dialog, null);
        final EditText authCode = (EditText) authDialog.findViewById(R.id.auth_code);

        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.two_factor_auth)
                .setView(authDialog)
                .setPositiveButton(R.string.verify, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LoginTask task = new LoginTask(username,
                                password, authCode.getText().toString());
                        AsyncTaskCompat.executeParallel(task);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
