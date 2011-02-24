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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.adapter.CommentAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.ImageDownloader;
import com.github.api.v2.schema.Comment;
import com.github.api.v2.schema.Issue;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.IssueService;
import com.github.api.v2.services.auth.Authentication;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;

/**
 * The IssueInfo activity.
 */
public class IssueActivity extends BaseActivity {

    /** The issue. */
    protected Issue mIssue;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The bundle. */
    protected Bundle mBundle;

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The issue number. */
    protected int mIssueNumber;

    /** The comment adapter. */
    protected CommentAdapter mCommentAdapter;

    /** The comments loaded. */
    protected boolean mCommentsLoaded;// flag to prevent more click which result
                                      // to query to the rest API

    /** The header. */
    protected LinearLayout mHeader;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.issue);
        setUpActionBar();

        mBundle = getIntent().getExtras().getBundle(Constants.DATA_BUNDLE);

        // comes from issue listing, the details already populated
        if (mBundle != null) {
            mUserLogin = mBundle.getString(Constants.Repository.REPO_OWNER);
            mRepoName = mBundle.getString(Constants.Repository.REPO_NAME);
            mIssueNumber = mBundle.getInt(Constants.Issue.ISSUE_NUMBER);
            fillData();
        }
        // comes from activity listing
        else {
            Bundle bundle = getIntent().getExtras();
            mUserLogin = bundle.getString(Constants.Repository.REPO_OWNER);
            mRepoName = bundle.getString(Constants.Repository.REPO_NAME);
            mIssueNumber = bundle.getInt(Constants.Issue.ISSUE_NUMBER);
            new LoadIssueTask(this).execute();
        }

        setBreadCrumb();

    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[3];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);
        data.put(Constants.Repository.REPO_NAME, mRepoName);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        // Repo
        b = new BreadCrumbHolder();
        b.setLabel(mRepoName);
        b.setTag(Constants.Repository.REPO_NAME);
        b.setData(data);
        breadCrumbHolders[1] = b;

        // Issues
        b = new BreadCrumbHolder();
        b.setLabel("Issues");
        b.setTag(Constants.Issue.ISSUES);
        b.setData(data);
        breadCrumbHolders[2] = b;

        createBreadcrumb("Issue #" + mIssueNumber, breadCrumbHolders);
    }

    /**
     * Gets the issue.
     *
     * @return the issue
     * @throws GitHubException the git hub exception
     */
    protected Bundle getIssue() throws GitHubException {
        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
        IssueService issueService = factory.createIssueService();
        Issue issue = issueService.getIssue(mUserLogin, mRepoName, mIssueNumber);
        mBundle = getApplicationContext().populateIssue(issue);
        return mBundle;
    }

    /**
     * Fill data into UI components.
     */
    protected void fillData() {
        ListView lvComments = (ListView) findViewById(R.id.lv_comments);

        // set details inside listview header
        LayoutInflater infalter = getLayoutInflater();
        mHeader = (LinearLayout) infalter.inflate(R.layout.issue_header, lvComments, false);

        // final Button btnAddComment = (Button)
        // findViewById(R.id.btn_add_comment);
        //        
        // EditText etComment = (EditText) findViewById(R.id.et_comment_field);
        // etComment.setOnFocusChangeListener(new OnFocusChangeListener() {
        //            
        // @Override
        // public void onFocusChange(View v, boolean hasFocus) {
        // btnAddComment.setVisibility(View.VISIBLE);
        // }
        // });

        lvComments.addHeaderView(mHeader, null, true);

        mCommentAdapter = new CommentAdapter(IssueActivity.this, new ArrayList<Comment>());
        lvComments.setAdapter(mCommentAdapter);

        ImageView ivGravatar = (ImageView) mHeader.findViewById(R.id.iv_gravatar);
        ImageDownloader imageDownloader = new ImageDownloader();
        imageDownloader.download(mBundle.getString(Constants.GRAVATAR_ID), ivGravatar);
        ivGravatar.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getApplicationContext().openUserInfoActivity(IssueActivity.this,
                        mBundle.getString(Constants.Issue.ISSUE_CREATED_BY), null);
            }
        });

        TextView tvLogin = (TextView) mHeader.findViewById(R.id.tv_login);
        TextView tvCreateAt = (TextView) mHeader.findViewById(R.id.tv_created_at);
        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        TextView tvDesc = (TextView) mHeader.findViewById(R.id.tv_desc);
        Button btnComments = (Button) mHeader.findViewById(R.id.btn_comments);

        tvLogin.setText(mBundle.getString(Constants.Issue.ISSUE_CREATED_BY));
        tvCreateAt.setText(mBundle.getString(Constants.Issue.ISSUE_CREATED_AT));
        tvState.setText(mBundle.getString(Constants.Issue.ISSUE_STATE));
        if ("closed".equals(mBundle.getString(Constants.Issue.ISSUE_STATE))) {
            tvState.setBackgroundResource(R.drawable.default_red_box);
        }
        else {
            tvState.setBackgroundResource(R.drawable.default_green_box);
        }
        tvTitle.setText(mBundle.getString(Constants.Issue.ISSUE_TITLE));
        tvDesc.setText(mBundle.getString(Constants.Issue.ISSUE_BODY));
        btnComments.setText(String.valueOf(mBundle.getInt(Constants.Issue.ISSUE_COMMENTS)));
        btnComments.setOnClickListener(new ButtonCommentsListener(this));
        
        LinearLayout llLabels = (LinearLayout) findViewById(R.id.ll_labels);
        ArrayList<CharSequence> labels = mBundle.getCharSequenceArrayList(Constants.Issue.ISSUE_LABELS);
        if (labels != null && !labels.isEmpty()) {
            for (CharSequence label : labels) {
                TextView tvLabel = new TextView(getApplicationContext());
                tvLabel.setSingleLine(true);
                tvLabel.setText(label);
                tvLabel.setTextAppearance(getApplicationContext(), R.style.default_text_small);
                tvLabel.setBackgroundResource(R.drawable.default_grey_box);
                
                llLabels.addView(tvLabel);
            }
            llLabels.setVisibility(View.VISIBLE);
        }
        else {
            llLabels.setVisibility(View.GONE);
        }
    }

    /**
     * Fill comments into UI components.
     * 
     * @param comments the comments
     */
    protected void fillComments(List<Comment> comments) {
        if (comments != null && comments.size() > 0) {
            mCommentAdapter.notifyDataSetChanged();
            for (Comment comment : comments) {
                mCommentAdapter.add(comment);
            }
        }
        mCommentAdapter.notifyDataSetChanged();
        mCommentsLoaded = true;
    }

    /**
     * An asynchronous task that runs on a background thread to load issue.
     */
    private static class LoadIssueTask extends AsyncTask<Void, Integer, Bundle> {

        /** The target. */
        private WeakReference<IssueActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load issue task.
         *
         * @param activity the activity
         */
        public LoadIssueTask(IssueActivity activity) {
            mTarget = new WeakReference<IssueActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Bundle doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    return mTarget.get().getIssue();
                }
                catch (GitHubException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Bundle result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillData();
                }
            }
        }
    }

    /**
     * Callback to be invoked when the button Comments is clicked.
     */
    private static class ButtonCommentsListener implements OnClickListener {

        /** The target. */
        private WeakReference<IssueActivity> mTarget;

        /**
         * Instantiates a new button comments listener.
         *
         * @param activity the activity
         */
        public ButtonCommentsListener(IssueActivity activity) {
            mTarget = new WeakReference<IssueActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View view) {
            if (mTarget.get() != null) {
                if (!mTarget.get().mCommentsLoaded) {
                    new LoadCommentsTask(mTarget.get()).execute(false);
                }
            }
        }
    }

    /**
     * Gets the comments.
     *
     * @return the comments
     * @throws GitHubException the git hub exception
     */
    protected List<Comment> getComments() throws GitHubException {
        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
        IssueService issueService = factory.createIssueService();
        return issueService.getIssueComments(mUserLogin, mRepoName, mIssueNumber);
    }

    /**
     * An asynchronous task that runs on a background thread to load comments.
     */
    private static class LoadCommentsTask extends AsyncTask<Boolean, Integer, List<Comment>> {

        /** The hide main view. */
        private boolean hideMainView;
        
        /** The target. */
        private WeakReference<IssueActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load comments task.
         *
         * @param activity the activity
         */
        public LoadCommentsTask(IssueActivity activity) {
            mTarget = new WeakReference<IssueActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Comment> doInBackground(Boolean... params) {
            if (mTarget.get() != null) {
                try {
                    this.hideMainView = params[0];
                    return mTarget.get().getComments();
                }
                catch (GitHubException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true,
                        hideMainView);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Comment> result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillComments(result);
                }
            }
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu.size() == 1) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.issue_menu, menu);
        }
        return true;
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_issue:
                Intent intent = new Intent().setClass(this, IssueCreateActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                startActivity(intent);
                return true;
            case R.id.edit_issue:
                intent = new Intent().setClass(this, IssueEditActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                intent.putExtra(Constants.Issue.ISSUE_NUMBER, mBundle.getInt(Constants.Issue.ISSUE_NUMBER));
                intent.putExtra(Constants.Issue.ISSUE_TITLE, mBundle.getString(Constants.Issue.ISSUE_TITLE));
                intent.putExtra(Constants.Issue.ISSUE_BODY, mBundle.getString(Constants.Issue.ISSUE_BODY));
                startActivity(intent);
                return true;
            case R.id.close_issue:
                new CloseIssueTask(this, false).execute();
                return true;
            case R.id.reopen_issue:
                new ReopenIssueTask(this, false).execute();
                return true;
            default:
                return true;
        }
    }
    
    private static class CloseIssueTask extends AsyncTask<Void, Void, Boolean> {

        /** The target. */
        private WeakReference<IssueActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /** The hide main view. */
        private boolean mHideMainView;

        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         * @param hideMainView the hide main view
         */
        public CloseIssueTask(IssueActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<IssueActivity>(activity);
            mHideMainView = hideMainView;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    IssueActivity activity = mTarget.get();
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    IssueService service = factory.createIssueService();
                    Authentication auth = new LoginPasswordAuthentication(mTarget.get().getAuthUsername(),
                            mTarget.get().getAuthPassword());
                    service.setAuthentication(auth);
                    service.closeIssue(activity.mUserLogin, 
                            activity.mRepoName,
                            activity.mIssueNumber);
                    return true;
                }
                catch (GitHubException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return false;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, mHideMainView);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_close),
                            false);
                }
                else {
                    activity.showMessage(activity.getResources().getString(R.string.issue_success_close),
                            false);
                    ((TextView)activity.findViewById(R.id.tv_state)).setBackgroundResource(R.drawable.default_red_box);
                }
            }
        }
    }
    
    private static class ReopenIssueTask extends AsyncTask<Void, Void, Boolean> {

        /** The target. */
        private WeakReference<IssueActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /** The hide main view. */
        private boolean mHideMainView;

        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         * @param hideMainView the hide main view
         */
        public ReopenIssueTask(IssueActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<IssueActivity>(activity);
            mHideMainView = hideMainView;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    IssueActivity activity = mTarget.get();
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    IssueService service = factory.createIssueService();
                    Authentication auth = new LoginPasswordAuthentication(mTarget.get().getAuthUsername(),
                            mTarget.get().getAuthPassword());
                    service.setAuthentication(auth);
                    service.reopenIssue(activity.mUserLogin, 
                            activity.mRepoName,
                            activity.mIssueNumber);
                    return true;
                }
                catch (GitHubException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return false;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, mHideMainView);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_reopen),
                            false);
                }
                else {
                    activity.showMessage(activity.getResources().getString(R.string.issue_success_reopen),
                            false);
                    ((TextView)activity.findViewById(R.id.tv_state)).setBackgroundResource(R.drawable.default_green_box);
                }
            }
        }
    }
}