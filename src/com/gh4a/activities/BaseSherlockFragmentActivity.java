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

import java.util.Calendar;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.utils.ToastUtils;

/**
 * The Base activity.
 */
public class BaseSherlockFragmentActivity extends SherlockFragmentActivity {
    private boolean mHasErrorView = false;

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
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!Gh4Application.get(this).isAuthorized()) {
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.anon_menu, menu);
        }
        return true;        
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            navigateUp();
            return true;
        case R.id.logout:
            Gh4Application.get(this).logout();
            return true;
        case R.id.login:
            Intent intent = new Intent(this, Github4AndroidActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP
                    |Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        case R.id.about:
            openAboutDialog();
            return true;
        case R.id.explore:
            intent = new Intent(this, ExploreActivity.class);
            startActivity(intent);
            return true;
        case R.id.search:
            intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        case R.id.bookmarks:
            intent = new Intent(this, BookmarkListActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void navigateUp() {
    }

    private void openAboutDialog() {
        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.about_dialog);
        
        TextView tvCopyright = (TextView) dialog.findViewById(R.id.copyright);
        tvCopyright.setText("Copyright " + Calendar.getInstance().get(Calendar.YEAR) + " Azwan Adli");
        
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            dialog.setTitle(getResources().getString(R.string.app_name) + " v" + versionName);
        }  catch (PackageManager.NameNotFoundException e) {
            dialog.setTitle(getResources().getString(R.string.app_name));
        }
        
        dialog.findViewById(R.id.btn_by_email).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.my_email) });
                sendIntent.setType("message/rfc822");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.send_email_title)));
            }
        });
        
        dialog.findViewById(R.id.btn_by_gh4a).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = BaseSherlockFragmentActivity.this;
                if (Gh4Application.get(context).isAuthorized()) {
                    Intent intent = new Intent(context, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, getString(R.string.my_username));
                    intent.putExtra(Constants.Repository.REPO_NAME, getString(R.string.my_repo));
                    startActivity(intent);
                } else {
                    ToastUtils.showMessage(context, R.string.login_prompt);
                }
            }
        });
        
        dialog.show();
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
    
    public void setErrorView() {
        mHasErrorView = true;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.error);
        
        findViewById(R.id.btn_home).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = BaseSherlockFragmentActivity.this;
                Gh4Application app = Gh4Application.get(context);
                if (app.isAuthorized()) {
                    app.openUserInfoActivity(context, app.getAuthLogin(), null,
                            Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                } else {
                    Intent intent = new Intent(context, Github4AndroidActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        });
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