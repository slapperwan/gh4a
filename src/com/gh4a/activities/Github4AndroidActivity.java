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

import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.UserService;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.AppBarLayout;
import android.support.v4.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.gh4a.BackgroundTask;
import com.gh4a.BaseActivity;
import com.gh4a.BuildConfig;
import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.CustomTabsHelper;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;

/**
 * The Github4Android activity.
 */
public class Github4AndroidActivity extends BaseActivity implements View.OnClickListener {
    private static final String OAUTH_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_CLIENT_SECRET = "client_secret";
    private static final String PARAM_CODE = "code";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_CALLBACK_URI = "redirect_uri";

    private static final Uri CALLBACK_URI = Uri.parse("gh4a://oauth");
    private static final String SCOPES = "user,repo,gist";

    private static final int REQUEST_SETTINGS = 10000;

    private View mContent;
    private View mProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Gh4Application app = Gh4Application.get();
        if (app.isAuthorized()) {
            goToToplevelActivity();
            finish();
        } else {
            setContentView(R.layout.main);

            AppBarLayout abl = (AppBarLayout) findViewById(R.id.header);
            abl.setEnabled(false);

            FrameLayout contentContainer = (FrameLayout) findViewById(R.id.content).getParent();
            contentContainer.setForeground(null);

            findViewById(R.id.login_button).setOnClickListener(this);
            mContent = findViewById(R.id.welcome_container);
            mProgress = findViewById(R.id.login_progress_container);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null
                && data.getScheme().equals(CALLBACK_URI.getScheme())
                && data.getHost().equals(CALLBACK_URI.getHost())) {
            Uri uri = Uri.parse(TOKEN_URL)
                    .buildUpon()
                    .appendQueryParameter(PARAM_CLIENT_ID, BuildConfig.CLIENT_ID)
                    .appendQueryParameter(PARAM_CLIENT_SECRET, BuildConfig.CLIENT_SECRET)
                    .appendQueryParameter(PARAM_CODE, data.getQueryParameter(PARAM_CODE))
                    .build();
            new FetchTokenTask(uri).schedule();
        } else {
            super.onNewIntent(intent);
        }
    }

    @Override
    protected int getLeftNavigationDrawerMenuResource() {
        return R.menu.anon_nav_drawer;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        super.onNavigationItemSelected(item);
        switch (item.getItemId()) {
            case R.id.settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
            case R.id.search:
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            case R.id.bookmarks:
                startActivity(new Intent(this, BookmarkListActivity.class));
                return true;
            case R.id.pub_timeline:
                startActivity(new Intent(this, TimelineActivity.class));
                return true;
            case R.id.blog:
                startActivity(new Intent(this, BlogListActivity.class));
                return true;
            case R.id.trend:
                startActivity(new Intent(this, TrendingActivity.class));
                return true;
        }
        return false;
    }

    @Override
    protected boolean canSwipeToRefresh() {
        return false;
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

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.login_button) {
            triggerLogin();
        }
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        triggerLogin();
    }

    private void triggerLogin() {
        String pkg = CustomTabsHelper.getPackageNameToUse(Github4AndroidActivity.this);
        Uri uri = Uri.parse(OAUTH_URL)
                .buildUpon()
                .appendQueryParameter(PARAM_CLIENT_ID, BuildConfig.CLIENT_ID)
                .appendQueryParameter(PARAM_SCOPE, SCOPES)
                .appendQueryParameter(PARAM_CALLBACK_URI, CALLBACK_URI.toString())
                .build();
        if (pkg != null) {
            int color = UiUtils.resolveColor(Github4AndroidActivity.this, R.attr.colorPrimary);
            CustomTabsIntent i = new CustomTabsIntent.Builder()
                    .setToolbarColor(color)
                    .build();
            i.intent.setPackage(pkg);
            i.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.launchUrl(Github4AndroidActivity.this, uri);
        } else {
            IntentUtils.launchBrowser(Github4AndroidActivity.this,
                    uri, Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        mContent.setVisibility(View.GONE);
        mProgress.setVisibility(View.VISIBLE);
    }

    private class FetchTokenTask extends BackgroundTask<Pair<String, String>> {
        private Uri mUri;
        public FetchTokenTask(Uri uri) {
            super(Github4AndroidActivity.this);
            mUri = uri;
        }

        @Override
        protected Pair<String, String> run() throws Exception {
            HttpURLConnection connection = null;
            CharArrayWriter writer = null;

            try {
                URL url = new URL(mUri.toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.addRequestProperty("Accept", "application/json");

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP failure");
                }

                InputStream in = new BufferedInputStream(connection.getInputStream());
                InputStreamReader reader = new InputStreamReader(in, "UTF-8");
                int length = connection.getContentLength();
                writer = new CharArrayWriter(Math.max(0, length));
                char[] tmp = new char[4096];

                int l;
                while ((l = reader.read(tmp)) != -1) {
                    writer.write(tmp, 0, l);
                }
                JSONObject response = new JSONObject(writer.toString());
                String token = response.getString("access_token");

                // fetch user information to get user name
                DefaultClient client = new DefaultClient();
                client.setOAuth2Token(token);
                UserService userService = new UserService(client);
                User user = userService.getUser();

                return Pair.create(user.getLogin(), token);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (writer != null) {
                    writer.close();
                }
            }
        }

        @Override
        protected void onError(Exception e) {
            super.onError(e);
            setErrorViewVisibility(true);
        }

        @Override
        protected void onSuccess(Pair<String, String> result) {
            Gh4Application.get().setLogin(result.first, result.second);
            goToToplevelActivity();
            finish();
        }
    }
}
