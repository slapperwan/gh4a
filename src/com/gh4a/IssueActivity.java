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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.adapter.CommentAdapter;
import com.gh4a.db.Bookmark;
import com.gh4a.db.BookmarkParam;
import com.gh4a.db.DbHelper;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;
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
public class IssueActivity extends BaseActivity implements OnClickListener {

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
    
    protected List<String> mLabels;

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
        Authentication auth = new LoginPasswordAuthentication(getAuthUsername(), getAuthPassword());
        issueService.setAuthentication(auth);
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
        LinearLayout mHeader = (LinearLayout) infalter.inflate(R.layout.issue_header, lvComments, false);
        
        // comment form at footer
        LinearLayout mFooter = (LinearLayout) infalter.inflate(R.layout.issue_footer, lvComments, false);

        lvComments.addHeaderView(mHeader, null, false);
        
        if (isAuthenticated()) {
            lvComments.addFooterView(mFooter, null, false);
        }

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
        Button btnCreateComment = (Button) mFooter.findViewById(R.id.btn_create);

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
        
        String body = mBundle.getString(Constants.Issue.ISSUE_BODY);
        body = body.replaceAll("\n", "<br/>");
        tvDesc.setText(Html.fromHtml(body));
        
        btnComments.setText(String.valueOf(mBundle.getInt(Constants.Issue.ISSUE_COMMENTS)));
        btnComments.setOnClickListener(this);
        
        btnCreateComment.setOnClickListener(this);
        
        LinearLayout llLabels = (LinearLayout) findViewById(R.id.ll_labels);
        ArrayList<String> labels = mBundle.getStringArrayList(Constants.Issue.ISSUE_LABELS);
        if (labels != null && !labels.isEmpty()) {
            for (String label : labels) {
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
        
        TextView tvPull = (TextView) mHeader.findViewById(R.id.tv_pull);
        if (mBundle.getString(Constants.Issue.PULL_REQUEST_URL) != null) {
            tvPull.setVisibility(View.VISIBLE);
            tvPull.setOnClickListener(this);
        }
    }

    /**
     * Fill comments into UI components.
     * 
     * @param comments the comments
     */
    protected void fillComments(List<Comment> comments) {
        mCommentAdapter.clear();
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
     * Gets the comments.
     *
     * @return the comments
     * @throws GitHubException the git hub exception
     */
    protected List<Comment> getComments() throws GitHubException {
        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
        IssueService issueService = factory.createIssueService();
        
        Authentication auth = new LoginPasswordAuthentication(getAuthUsername(), getAuthPassword());
        issueService.setAuthentication(auth);
        
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
                    activity.showError(false);
                }
                else {
                    activity.fillComments(result);
                }
            }
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isAuthenticated()) {
            MenuInflater inflater = getMenuInflater();
            menu.clear();
            if ("closed".equals(mBundle.getString(Constants.Issue.ISSUE_STATE))) {
                menu.add(Menu.FIRST, R.string.issue_reopen, 0, R.string.issue_reopen);
            }
            else {
                menu.add(Menu.FIRST, R.string.issue_close, 0, R.string.issue_close);
            }
            menu.add(Menu.FIRST, R.string.issue_edit, 0, R.string.issue_edit);
            menu.add(Menu.FIRST, R.string.issue_label_add_delete, 0, R.string.issue_label_add_delete);
            menu.add(Menu.FIRST, R.string.issue_create, 0, R.string.issue_create);
            inflater.inflate(R.menu.bookmark_menu, menu);
        }
        return true;
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.string.issue_create:
                if (isAuthenticated()) {
                    Intent intent = new Intent().setClass(this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }                
                return true;
            case R.string.issue_edit:
                if (isAuthenticated()) {
                    Intent intent = new Intent().setClass(this, IssueEditActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    intent.putExtra(Constants.Issue.ISSUE_NUMBER, mBundle.getInt(Constants.Issue.ISSUE_NUMBER));
                    intent.putExtra(Constants.Issue.ISSUE_TITLE, mBundle.getString(Constants.Issue.ISSUE_TITLE));
                    intent.putExtra(Constants.Issue.ISSUE_BODY, mBundle.getString(Constants.Issue.ISSUE_BODY));
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }     
                return true;
            case R.string.issue_close:
                if (isAuthenticated()) {
                    new CloseIssueTask(this, false).execute();
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            case R.string.issue_reopen:
                if (isAuthenticated()) {
                    new ReopenIssueTask(this, false).execute();
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            case R.string.issue_label_add_delete:
                if (isAuthenticated()) {
                    new LoadIssueLabelsTask(this).execute();
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
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
                    TextView tvState = (TextView)activity.findViewById(R.id.tv_state);
                    tvState.setBackgroundResource(R.drawable.default_red_box);
                    tvState.setText("closed");
                    activity.mBundle.putString(Constants.Issue.ISSUE_STATE, Issue.State.CLOSED.value());
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
                    TextView tvState = (TextView)activity.findViewById(R.id.tv_state);
                    tvState.setBackgroundResource(R.drawable.default_green_box);
                    tvState.setText("open");
                    activity.mBundle.putString(Constants.Issue.ISSUE_STATE, Issue.State.OPEN.value());
                }
            }
        }
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to comment issue.
     */
    private static class CommentIssueTask extends AsyncTask<Void, Void, Boolean> {

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
        public CommentIssueTask(IssueActivity activity, boolean hideMainView) {
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
                    EditText etComment = (EditText) activity.findViewById(R.id.et_desc);
                    //CheckBox cbSign = (CheckBox) activity.findViewById(R.id.cb_sign);
                    
                    String comment = etComment.getText().toString();
//                    if (cbSign.isChecked()) {
//                        comment = comment + "\n\n" + activity.getResources().getString(R.string.sign);
//                    }
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    IssueService service = factory.createIssueService();
                    Authentication auth = new LoginPasswordAuthentication(mTarget.get().getAuthUsername(),
                            mTarget.get().getAuthPassword());
                    service.setAuthentication(auth);
                    service.addComment(activity.mUserLogin, 
                            activity.mRepoName,
                            activity.mIssueNumber,
                            comment);
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
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_comment),
                            false);
                }
                else {
                    activity.showMessage(activity.getResources().getString(R.string.issue_success_comment),
                            false);
                    //reload comments
                    new LoadCommentsTask(activity).execute(false);
                    EditText etComment = (EditText) activity.findViewById(R.id.et_desc);
                    etComment.setText(null);
                    etComment.clearFocus();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_comments:
            if (!mCommentsLoaded) {
                new LoadCommentsTask(this).execute(false);
            }
            break;
        case R.id.btn_create:
            new CommentIssueTask(this, false).execute();
            break;
        case R.id.tv_pull:
            getApplicationContext().openPullRequestActivity(this,
                    mUserLogin, mRepoName, mIssueNumber);
            break;
        default:
            break;
        }
    }
    
    private void showLabelsDialog() {
        final boolean[] checkedItems = new boolean[mLabels.size()];

        final String[] availabelLabelArr = new String[mLabels.size()];
        ArrayList<String> currentLabels = mBundle.getStringArrayList(Constants.Issue.ISSUE_LABELS);

        //find which labels for this issue
        for (int i = 0; i < mLabels.size(); i++) {
            availabelLabelArr[i] = mLabels.get(i);
            if(currentLabels.contains((String) mLabels.get(i))) {
                checkedItems[i] = true;
            }
            else {
                checkedItems[i] = false;
            }
        }
        
        //final boolean[] newCheckedItems = new boolean[availableLabels.size()];
        
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(IssueActivity.this, android.R.style.Theme));
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_labels);
        builder.setMultiChoiceItems(availabelLabelArr, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                if (isChecked) {
                    checkedItems[whichButton] = true;
                }
                else {
                    checkedItems[whichButton] = false;
                }
            }
        });
        
        builder.setPositiveButton(R.string.label_it,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                new TagIssueLabelsTask(IssueActivity.this, availabelLabelArr, checkedItems).execute();
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        })
       .create();
        
        builder.show();
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to load issue labels.
     */
    private static class LoadIssueLabelsTask extends AsyncTask<Void, Void, List<String>> {

        /** The target. */
        private WeakReference<IssueActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         * @param hideMainView the hide main view
         */
        public LoadIssueLabelsTask(IssueActivity activity) {
            mTarget = new WeakReference<IssueActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    IssueService issueService = factory.createIssueService();
                    Authentication auth = new LoginPasswordAuthentication(mTarget.get().getAuthUsername(),
                            mTarget.get().getAuthPassword());
                    issueService.setAuthentication(auth);
                    
                    return issueService.getIssueLabels(mTarget.get().mUserLogin, mTarget.get().mRepoName);
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, false);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<String> result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError(false);
                }
                else {
                    activity.showIssueLabelsContextMenu(result);
                }
            }
        }
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to tag issue labels.
     */
    private static class TagIssueLabelsTask extends AsyncTask<Void, Void, Void> {

        /** The target. */
        private WeakReference<IssueActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /** The available label arr. */
        private String[] mAvailableLabelArr;
        
        /** The checked items. */
        private boolean[] mCheckedItems;
        
        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         * @param hideMainView the hide main view
         */
        public TagIssueLabelsTask(IssueActivity activity, String[] availableLabelArr, boolean[] checkedItems) {
            mTarget = new WeakReference<IssueActivity>(activity);
            mAvailableLabelArr = availableLabelArr;
            mCheckedItems = checkedItems;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Void...params) {
            if (mTarget.get() != null) {
                try {
                    IssueActivity activity = mTarget.get();
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    IssueService issueService = factory.createIssueService();
                    Authentication auth = new LoginPasswordAuthentication(mTarget.get().getAuthUsername(),
                            mTarget.get().getAuthPassword());
                    issueService.setAuthentication(auth);
                    
                    for (int i = 0; i < mCheckedItems.length; i++) {
                        String label = StringUtils.encodeUrl(mAvailableLabelArr[i]);
                        if (mCheckedItems[i]) {
                            issueService.addLabel(activity.mUserLogin, activity.mRepoName, activity.mIssueNumber, label);
                        }
                        else {
                            issueService.removeLabel(activity.mUserLogin, activity.mRepoName, activity.mIssueNumber, label);
                        }
                    }
                }
                catch (GitHubException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                }
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, false);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_label_add_delete), false);
                }
                else {
                    activity.getApplicationContext().openIssueActivity(activity,
                            activity.mUserLogin, activity.mRepoName, activity.mIssueNumber);
                }
            }
        }
    }
    
    private void showIssueLabelsContextMenu(List<String> labels) {
        if (labels != null && !labels.isEmpty()) {
            mLabels = labels;
            showLabelsDialog();
        }
        else {
            getApplicationContext().notFoundMessage(this, getResources().getString(R.string.issue_labels));
        }
    }
    
    @Override
    public void openBookmarkActivity() {
        Intent intent = new Intent().setClass(this, BookmarkListActivity.class);
        intent.putExtra(Constants.Bookmark.NAME, "Issue #" + mIssueNumber + " at " + mUserLogin + "/" + mRepoName);
        intent.putExtra(Constants.Bookmark.OBJECT_TYPE, Constants.Bookmark.OBJECT_TYPE_ISSUE);
        startActivityForResult(intent, 100);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
           if (resultCode == Constants.Bookmark.ADD) {
               DbHelper db = new DbHelper(this);
               Bookmark b = new Bookmark();
               b.setName("Issue #" + mIssueNumber + " at " + mUserLogin + "/" + mRepoName);
               b.setObjectType(Constants.Bookmark.OBJECT_TYPE_ISSUE);
               b.setObjectClass(IssueActivity.class.getName());
               long id = db.saveBookmark(b);
               
               BookmarkParam[] params = new BookmarkParam[3];
               BookmarkParam param = new BookmarkParam();
               param.setBookmarkId(id);
               param.setKey(Constants.Repository.REPO_OWNER);
               param.setValue(mUserLogin);
               params[0] = param;
               
               param = new BookmarkParam();
               param.setBookmarkId(id);
               param.setKey(Constants.Repository.REPO_NAME);
               param.setValue(mRepoName);
               params[1] = param;
               
               param = new BookmarkParam();
               param.setBookmarkId(id);
               param.setKey(Constants.Issue.ISSUE_NUMBER);
               param.setValue(String.valueOf(mIssueNumber));
               params[2] = param;
               
               db.saveBookmarkParam(params);
           }
        }
     }
}