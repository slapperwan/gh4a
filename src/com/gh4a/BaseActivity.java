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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gh4a.db.Bookmark;
import com.gh4a.db.BookmarkParam;
import com.gh4a.holder.BreadCrumbHolder;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ScrollingTextView;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.ocpsoft.pretty.time.PrettyTime;

/**
 * The Base activity.
 */
public class BaseActivity extends Activity {

    /** The Constant pt. */
    protected static final PrettyTime pt = new PrettyTime();

    /* (non-Javadoc)
     * @see android.content.ContextWrapper#getApplicationContext()
     */
    @Override
    public Gh4Application getApplicationContext() {
        return (Gh4Application) super.getApplicationContext();
    }

    /**
     * Common function when device search button pressed, then open
     * SearchActivity.
     *
     * @return true, if successful
     */
    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent().setClass(getApplication(), SearchActivity.class);
        startActivity(intent);
        return true;
    }

    /**
     * Hide keyboard.
     *
     * @param binder the binder
     */
    public void hideKeyboard(IBinder binder) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binder, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu = setupOptionMenu(menu);
//        if (isAuthenticated()
//                && this instanceof UserActivity) {
//            MenuInflater inflater = getMenuInflater();
//            inflater.inflate(R.menu.authenticated_menu, menu);
//        }
        if (!isAuthenticated()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.anon_menu, menu);
        }
        return true;        
    }
    
    public Menu setupOptionMenu(Menu menu) {
        return menu;
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.logout:
            SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                    Constants.PREF_NAME, MODE_PRIVATE);
            
            if (sharedPreferences != null) {
                if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
                        && sharedPreferences.getString(Constants.User.USER_PASSWORD, null) != null){
                    Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.commit();
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    Toast.makeText(this, getResources().getString(R.string.success_logout), Toast.LENGTH_SHORT).show();
                    this.finish();
                }
            }
            return true;
        case R.id.login:
            Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
            startActivity(intent);
            return true;
        case R.id.about:
            openAboutDialog();
            return true;
        case R.id.feedback:
            openFeedbackDialog();
            return true;
        case R.id.bookmarks:
            openBookmarkActivity();
            return true;
        default:
            return setMenuOptionItemSelected(item);
        }
    }

    public void openBookmarkActivity() {
        //should be override at sub class
    }
    
    public boolean setMenuOptionItemSelected(MenuItem item) {
        return true;
    }
    
    public void openAboutDialog() {
        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.about_dialog);
        
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            dialog.setTitle(getResources().getString(R.string.app_name) + " v" + versionName);
        } 
        catch (PackageManager.NameNotFoundException e) {
            dialog.setTitle(getResources().getString(R.string.app_name));
        }
        
        dialog.show();
    }
    
    public void openFeedbackDialog() {
        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.feedback_dialog);
        dialog.setTitle(getResources().getString(R.string.feedback));
        
        Button btnByEmail = (Button) dialog.findViewById(R.id.btn_by_email);
        btnByEmail.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.my_email)});
                sendIntent.setType("message/rfc822");
                startActivity(Intent.createChooser(sendIntent, "Select email application."));
            }
        });
        
        Button btnByGh4a = (Button) dialog.findViewById(R.id.btn_by_gh4a);
        btnByGh4a.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                if (isAuthenticated()) {
                    Intent intent = new Intent().setClass(BaseActivity.this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, getResources().getString(R.string.my_username));
                    intent.putExtra(Constants.Repository.REPO_NAME, getResources().getString(R.string.my_repo));
                    startActivity(intent);
                }
                else {
                    showMessage("Please login", false);
                }
            }
        });
        
        dialog.show();
    }
    
    /**
     * Creates the breadcrumb.
     *
     * @param subTitle the sub title
     * @param breadCrumbHolders the bread crumb holders
     */
    public void createBreadcrumb(String subTitle, BreadCrumbHolder... breadCrumbHolders) {
        if (breadCrumbHolders != null) {
            LinearLayout llPart = (LinearLayout) this.findViewById(R.id.ll_part);
            for (int i = 0; i < breadCrumbHolders.length; i++) {
                TextView tvBreadCrumb = new TextView(getApplication());
                SpannableString part = new SpannableString(breadCrumbHolders[i].getLabel());
                part.setSpan(new UnderlineSpan(), 0, part.length(), 0);
                tvBreadCrumb.append(part);
                tvBreadCrumb.setTag(breadCrumbHolders[i]);
                tvBreadCrumb.setBackgroundResource(R.drawable.default_link);
                tvBreadCrumb.setTextAppearance(getApplication(), R.style.default_text_small);
                tvBreadCrumb.setSingleLine(true);
                tvBreadCrumb.setOnClickListener(new OnClickBreadCrumb(this));
    
                llPart.addView(tvBreadCrumb);
    
                if (i < breadCrumbHolders.length - 1) {
                    TextView slash = new TextView(getApplication());
                    slash.setText(" / ");
                    slash.setTextAppearance(getApplication(), R.style.default_text_small);
                    llPart.addView(slash);
                }
            }
        }

        ScrollingTextView tvSubtitle = (ScrollingTextView) this.findViewById(R.id.tv_subtitle);
        tvSubtitle.setText(subTitle);
    }

    /**
     * Sets the up action bar.
     */
    public void setUpActionBar() {
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        if (isAuthenticated()) {
            Intent intent = new Intent().setClass(getApplicationContext(), UserActivity.class);
            intent.putExtra(Constants.User.USER_LOGIN, getAuthUsername());
            actionBar.setHomeAction(new IntentAction(this, intent, R.drawable.ic_home));
        }
        actionBar.addAction(new IntentAction(this, new Intent(getApplication(),
                ExploreActivity.class), R.drawable.ic_explore));
        actionBar.addAction(new IntentAction(this, new Intent(getApplication(),
                SearchActivity.class), R.drawable.ic_search));
    }

    /**
     * Checks if is authenticated.
     *
     * @return true, if is authenticated
     */
    public boolean isAuthenticated() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        
        if (sharedPreferences != null) {
            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
                    && sharedPreferences.getString(Constants.User.USER_PASSWORD, null) != null){
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
    
    /**
     * Gets the auth username.
     *
     * @return the auth username
     */
    public String getAuthUsername() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        
        if (sharedPreferences != null) {
            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
                    && sharedPreferences.getString(Constants.User.USER_PASSWORD, null) != null){
                return sharedPreferences.getString(Constants.User.USER_LOGIN, null);
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }
    
    /**
     * Gets the auth password.
     *
     * @return the auth password
     */
    public String getAuthPassword() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        
        if (sharedPreferences != null) {
            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
                    && sharedPreferences.getString(Constants.User.USER_PASSWORD, null) != null){
                return sharedPreferences.getString(Constants.User.USER_PASSWORD, null);
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }
    
    /**
     * The Class OnClickBreadCrumb.
     */
    private static class OnClickBreadCrumb implements OnClickListener {

        /** The target. */
        private WeakReference<BaseActivity> mTarget;

        /**
         * Instantiates a new on click bread crumb.
         *
         * @param activity the activity
         */
        public OnClickBreadCrumb(BaseActivity activity) {
            mTarget = new WeakReference<BaseActivity>(activity);
        }

        /* (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View view) {
            TextView breadCrumb = (TextView) view;
            BreadCrumbHolder b = (BreadCrumbHolder) breadCrumb.getTag();
            String tag = b.getTag();
            HashMap<String, String> data = b.getData();

            BaseActivity baseActivity = mTarget.get();

            if (Constants.User.USER_LOGIN.equals(tag)) {
                mTarget.get().getApplicationContext().openUserInfoActivity(baseActivity,
                        data.get(Constants.User.USER_LOGIN), null);
            }
            else if (Constants.Repository.REPO_NAME.equals(tag)) {
                mTarget.get().getApplicationContext().openRepositoryInfoActivity(baseActivity,
                        data.get(Constants.User.USER_LOGIN),
                        data.get(Constants.Repository.REPO_NAME));
            }
            else if (Constants.Issue.ISSUES.equals(tag)) {
                mTarget.get().getApplicationContext().openIssueListActivity(baseActivity,
                        data.get(Constants.User.USER_LOGIN),
                        data.get(Constants.Repository.REPO_NAME), Constants.Issue.ISSUE_STATE_OPEN);
            }
            else if (Constants.Issue.ISSUE.equals(tag)) {
                mTarget.get().getApplicationContext().openIssueActivity(baseActivity,
                        data.get(Constants.User.USER_LOGIN),
                        data.get(Constants.Repository.REPO_NAME),
                        Integer.parseInt(data.get(Constants.Issue.ISSUE_NUMBER)));
            }
            else if (Constants.Commit.COMMITS.equals(tag)) {
                mTarget.get().getApplicationContext().openBranchListActivity(baseActivity,
                        data.get(Constants.User.USER_LOGIN),
                        data.get(Constants.Repository.REPO_NAME), R.id.btn_commits);
            }
            else if (Constants.PullRequest.PULL_REQUESTS.equals(tag)) {
                mTarget.get().getApplicationContext().openPullRequestListActivity(baseActivity,
                        data.get(Constants.User.USER_LOGIN),
                        data.get(Constants.Repository.REPO_NAME),
                        Constants.Issue.ISSUE_STATE_OPEN);
            }
            else if (Constants.Object.TREE.equals(tag)) {
                mTarget.get().getApplicationContext().openBranchListActivity(baseActivity,
                        data.get(Constants.User.USER_LOGIN),
                        data.get(Constants.Repository.REPO_NAME), R.id.btn_branches);
            }
            else if (Constants.Commit.COMMIT.equals(tag)) {
                mTarget.get().getApplicationContext().openCommitInfoActivity(baseActivity,
                        data.get(Constants.User.USER_LOGIN),
                        data.get(Constants.Repository.REPO_NAME),
                        data.get(Constants.Object.OBJECT_SHA));
            }
            else if (Constants.Repository.REPO_BRANCH.equals(tag)) {
                Intent intent = new Intent().setClass(mTarget.get(), FileManagerActivity.class);
                if (mTarget.get() instanceof CommitListActivity) {
                    intent = new Intent().setClass(mTarget.get(), CommitListActivity.class);
                }
                intent.putExtra(Constants.Repository.REPO_OWNER, data
                        .get(Constants.User.USER_LOGIN));
                intent.putExtra(Constants.Repository.REPO_NAME, data
                        .get(Constants.Repository.REPO_NAME));
                intent.putExtra(Constants.Object.TREE_SHA, data.get(Constants.Object.TREE_SHA));
                intent.putExtra(Constants.Object.OBJECT_SHA, data.get(Constants.Object.TREE_SHA));
                intent.putExtra(Constants.Object.PATH, "Tree");
                intent.putExtra(Constants.Repository.REPO_BRANCH, data
                        .get(Constants.Repository.REPO_BRANCH));
                intent.putExtra(Constants.VIEW_ID, Integer.parseInt(data.get(Constants.VIEW_ID)));
                mTarget.get().startActivity(intent);
            }
            else if (Constants.Object.BRANCHES.equals(tag)) {
                mTarget.get().getApplicationContext().openBranchListActivity(mTarget.get(),
                        data.get(Constants.User.USER_LOGIN),
                        data.get(Constants.Repository.REPO_NAME), R.id.btn_branches);
            }
            else if (Constants.Object.TAGS.equals(tag)) {
                mTarget.get().getApplicationContext().openTagListActivity(mTarget.get(),
                        data.get(Constants.User.USER_LOGIN),
                        data.get(Constants.Repository.REPO_NAME), R.id.btn_branches);
            }
            else if (Constants.EXPLORE.equals(tag)) {
                Intent intent = new Intent().setClass(mTarget.get(), ExploreActivity.class);
                mTarget.get().startActivity(intent);
            }
            else if (Constants.Blog.BLOG.equals(tag)) {
                Intent intent = new Intent().setClass(mTarget.get(), BlogListActivity.class);
                mTarget.get().startActivity(intent);
            }
            else if (Constants.Wiki.WIKI.equals(tag)) {
                Intent intent = new Intent().setClass(mTarget.get(), WikiListActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, data.get(Constants.User.USER_LOGIN));
                intent.putExtra(Constants.Repository.REPO_NAME, data.get(Constants.Repository.REPO_NAME));
                mTarget.get().startActivity(intent);
            }
            else if (Constants.Discussion.CATEGORY.equals(tag)) {
                Intent intent = new Intent().setClass(mTarget.get(), DiscussionCategoryListActivity.class);
                mTarget.get().startActivity(intent);
            }
            else if (Constants.Discussion.DISCUSSIONS.equals(tag)) {
                Intent intent = new Intent().setClass(mTarget.get(), DiscussionListActivity.class);
                intent.putExtra(Constants.Discussion.URL, data.get(Constants.Discussion.URL));
                intent.putExtra(Constants.Discussion.TITLE, data.get(Constants.Discussion.TITLE));
                mTarget.get().startActivity(intent);
            }
            else if (Constants.Job.JOB.equals(tag)) {
                Intent intent = new Intent().setClass(mTarget.get(), JobListActivity.class);
                mTarget.get().startActivity(intent);
            }
        }
    };

    /**
     * Show error.
     */
    public void showError() {
        Toast
                .makeText(getApplication(), "An error occured while fetching data",
                        Toast.LENGTH_SHORT).show();
        super.finish();
    }

    /**
     * Show error.
     *
     * @param finishThisActivity the finish this activity
     */
    public void showError(boolean finishThisActivity) {
        Toast
                .makeText(getApplication(), "An error occured while fetching data",
                        Toast.LENGTH_SHORT).show();
        if (finishThisActivity) {
            super.finish();
        }
    }
    
    public void showMessage(String message, boolean finishThisActivity) {
        Toast
                .makeText(getApplication(), message,
                        Toast.LENGTH_SHORT).show();
        if (finishThisActivity) {
            super.finish();
        }
    }
    
    public boolean isSettingEnabled(String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(key, false);
    }
    
    public String getSettingStringValue(String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(key, null);
    }
}