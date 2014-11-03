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

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.ToastUtils;

/**
 * The Base activity.
 */
public abstract class BaseFragmentActivity extends ActionBarActivity {
    private boolean mHasErrorView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
    }

    /**
     * Common function when device search button pressed, then open
     * SearchActivity.
     *
     * @return true, if successful
     */
    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == android.R.id.home) {
            Intent intent = navigateUp();
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected Intent navigateUp() {
        return null;
    }

    public ProgressDialog showProgressDialog(String message, boolean cancelable) {
        return ProgressDialog.show(this, "", message, cancelable);
    }

    public void stopProgressDialog(ProgressDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isAvailable() && info.isConnected();
    }

    protected void setErrorView() {
        mHasErrorView = true;
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.error);

        findViewById(R.id.btn_home).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                goToToplevelActivity(true);
            }
        });
    }

    protected void goToToplevelActivity(boolean newTask) {
        Intent intent = getToplevelActivityIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

    protected Intent getToplevelActivityIntent() {
        Gh4Application app = Gh4Application.get(this);
        if (app.isAuthorized()) {
            Intent intent = IntentUtils.getUserActivityIntent(this, app.getAuthLogin(), null);
            intent.putExtra(UserActivity.EXTRA_TOPLEVEL_MODE, true);
            return intent;
        } else {
            return new Intent(this, Github4AndroidActivity.class);
        }
    }

    protected boolean hasErrorView() {
        return mHasErrorView;
    }

    protected void saveBookmark(String name, int type, Intent intent, String extraData) {
        ContentValues cv = new ContentValues();
        cv.put(BookmarksProvider.Columns.NAME, name);
        cv.put(BookmarksProvider.Columns.TYPE, type);
        cv.put(BookmarksProvider.Columns.URI, intent.toUri(0));
        cv.put(BookmarksProvider.Columns.EXTRA, extraData);
        if (getContentResolver().insert(BookmarksProvider.Columns.CONTENT_URI, cv) != null) {
            ToastUtils.showMessage(this, R.string.bookmark_saved);
        }
    }
}