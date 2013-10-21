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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.androidquery.AQuery;
import com.gh4a.Constants.LoaderResult;
import com.gh4a.adapter.CommentAdapter;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueLoader;
import com.gh4a.utils.GravatarUtils;
import com.gh4a.utils.StringUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class IssueActivity extends BaseSherlockFragmentActivity implements 
    OnClickListener, LoaderManager.LoaderCallbacks<HashMap<Integer, Object>> {

    private Issue mIssue;
    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private String mIssueState;
    private CommentAdapter mCommentAdapter;
    private boolean isCollaborator;
    private boolean isCreator;
    private ProgressDialog mProgressDialog;
    private AQuery aq;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mIssueNumber = data.getInt(Constants.Issue.ISSUE_NUMBER);
        mIssueState = data.getString(Constants.Issue.ISSUE_STATE);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.issue);

        aq = new AQuery(this);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.issue) + " #" + mIssueNumber);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        RelativeLayout tlComment = (RelativeLayout) findViewById(R.id.rl_comment);
        if (!isAuthorized()) {
            tlComment.setVisibility(View.GONE);
        }
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().getLoader(0).forceLoad();
        
        getSupportLoaderManager().initLoader(1, null, this);
    }

    private void fillData() {
        new LoadCommentsTask(this).execute();
        
        Typeface boldCondensed = getApplicationContext().boldCondensed;
        
        ListView lvComments = (ListView) findViewById(R.id.list_view);
        // set details inside listview header
        LayoutInflater infalter = getLayoutInflater();
        LinearLayout mHeader = (LinearLayout) infalter.inflate(R.layout.issue_header, lvComments, false);
        mHeader.setClickable(false);
        
        lvComments.addHeaderView(mHeader, null, false);
        
        RelativeLayout rlComment = (RelativeLayout) findViewById(R.id.rl_comment);
        if (!isAuthorized()) {
            rlComment.setVisibility(View.GONE);
        }

        TextView tvCommentTitle = (TextView) mHeader.findViewById(R.id.comment_title);
        mCommentAdapter = new CommentAdapter(IssueActivity.this, new ArrayList<Comment>(), 
                mIssue.getNumber(), mIssue.getState(),
                mRepoOwner, mRepoName);
        lvComments.setAdapter(mCommentAdapter);

        ImageView ivGravatar = (ImageView) mHeader.findViewById(R.id.iv_gravatar);
        aq.id(R.id.iv_gravatar).image(GravatarUtils.getGravatarUrl(mIssue.getUser().getGravatarId()),
                true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), AQuery.FADE_IN);
        
        ivGravatar.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getApplicationContext().openUserInfoActivity(IssueActivity.this,
                        mIssue.getUser().getLogin(), null);
            }
        });

        TextView tvExtra = (TextView) mHeader.findViewById(R.id.tv_extra);
        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        TextView tvDescTitle = (TextView) mHeader.findViewById(R.id.desc_title);
        tvDescTitle.setTypeface(getApplicationContext().boldCondensed);
        tvDescTitle.setTextColor(getResources().getColor(R.color.highlight));
        
        tvCommentTitle.setTypeface(getApplicationContext().boldCondensed);
        tvCommentTitle.setTextColor(getResources().getColor(R.color.highlight));
        tvCommentTitle.setText(getResources().getString(R.string.issue_comments) + " (" + mIssue.getComments() + ")");
        
        TextView tvDesc = (TextView) mHeader.findViewById(R.id.tv_desc);
        tvDesc.setMovementMethod(LinkMovementMethod.getInstance());
        
        TextView tvMilestone = (TextView) mHeader.findViewById(R.id.tv_milestone);
        
        ImageView ivComment = (ImageView) findViewById(R.id.iv_comment);
        if (Gh4Application.THEME == R.style.DefaultTheme) {
            ivComment.setImageResource(R.drawable.social_send_now_dark);
        }
        ivComment.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
        ivComment.setPadding(5, 2, 5, 2);
        ivComment.setOnClickListener(this);

        tvExtra.setText(mIssue.getUser().getLogin() + "\n" + pt.format(mIssue.getCreatedAt()));
        tvState.setTextColor(Color.WHITE);
        if ("closed".equals(mIssue.getState())) {
            tvState.setBackgroundResource(R.drawable.default_red_box);
            tvState.setText(getString(R.string.closed).toUpperCase(Locale.getDefault()));
        }
        else {
            tvState.setBackgroundResource(R.drawable.default_green_box);
            tvState.setText(getString(R.string.open).toUpperCase(Locale.getDefault()));
        }
        tvTitle.setText(mIssue.getTitle());
        tvTitle.setTypeface(boldCondensed);
        
        boolean showInfoBox = false;
        if (mIssue.getAssignee() != null) {
            showInfoBox = true;
            TextView tvAssignee = (TextView) mHeader.findViewById(R.id.tv_assignee);
            tvAssignee.setText(getString(R.string.issue_assignee, mIssue.getAssignee().getLogin()));
            tvAssignee.setVisibility(View.VISIBLE);
            tvAssignee.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    getApplicationContext().openUserInfoActivity(IssueActivity.this,
                            mIssue.getAssignee().getLogin(), null);
                }
            });
            
            ImageView ivAssignee = (ImageView) mHeader.findViewById(R.id.iv_assignee);
            
            aq.id(R.id.iv_assignee).image(GravatarUtils.getGravatarUrl(mIssue.getAssignee().getGravatarId()),
                    true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), AQuery.FADE_IN);
            
            ivAssignee.setVisibility(View.VISIBLE);
            ivAssignee.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    getApplicationContext().openUserInfoActivity(IssueActivity.this,
                            mIssue.getAssignee().getLogin(), null);
                }
            });
        }
        
        if (mIssue.getMilestone() != null) {
            showInfoBox = true;
            tvMilestone.setText(getString(R.string.issue_milestone, mIssue.getMilestone().getTitle()));
        }
        else {
            tvMilestone.setVisibility(View.GONE);
        }
        
        String body = mIssue.getBodyHtml();
        if (!StringUtils.isBlank(body)) {
            HttpImageGetter imageGetter = new HttpImageGetter(this);
            body = HtmlUtils.format(body).toString();
            imageGetter.bind(tvDesc, body, mIssue.getNumber());
        }
        
        LinearLayout llLabels = (LinearLayout) findViewById(R.id.ll_labels);
        List<Label> labels = mIssue.getLabels();
        
        if (labels != null && !labels.isEmpty()) {
            showInfoBox = true;
            for (Label label : labels) {
                TextView tvLabel = new TextView(this);
                tvLabel.setSingleLine(true);
                tvLabel.setText(label.getName());
                tvLabel.setTextAppearance(this, R.style.default_text_small);
                tvLabel.setBackgroundColor(Color.parseColor("#" + label.getColor()));
                tvLabel.setPadding(5, 2, 5, 2);
                int r = Color.red(Color.parseColor("#" + label.getColor()));
                int g = Color.green(Color.parseColor("#" + label.getColor()));
                int b = Color.blue(Color.parseColor("#" + label.getColor()));
                if (r + g + b < 383) {
                    tvLabel.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
                }
                else {
                    tvLabel.setTextColor(getResources().getColor(android.R.color.primary_text_light));
                }
                llLabels.addView(tvLabel);
                
                View v = new View(this);
                v.setLayoutParams(new LayoutParams(5, LayoutParams.WRAP_CONTENT));
                llLabels.addView(v);
            }
        }
        else {
            llLabels.setVisibility(View.GONE);
        }
        
        TextView tvPull = (TextView) mHeader.findViewById(R.id.tv_pull);
        if (mIssue.getPullRequest() != null
                && mIssue.getPullRequest().getDiffUrl() != null) {
            showInfoBox = true;
            tvPull.setVisibility(View.VISIBLE);
            tvPull.setOnClickListener(this);
        }
        
        if (!showInfoBox) {
            RelativeLayout rl = (RelativeLayout) mHeader.findViewById(R.id.info_box);
            rl.setVisibility(View.GONE);
        }
    }

    protected void fillComments(List<Comment> comments) {
        stopProgressDialog(mProgressDialog);
        mCommentAdapter.clear();
        if (comments != null && comments.size() > 0) {
            mCommentAdapter.notifyDataSetChanged();
            for (Comment comment : comments) {
                mCommentAdapter.add(comment);
            }
        }
        mCommentAdapter.notifyDataSetChanged();
    }

    private List<Comment> getComments() throws IOException {
        GitHubClient client = new DefaultClient();
        client.setOAuth2Token(getAuthToken());
        IssueService issueService = new IssueService(client);
        return issueService.getComments(mRepoOwner, mRepoName, mIssueNumber);
    }

    private static class LoadCommentsTask extends AsyncTask<Boolean, Integer, List<Comment>> {

        private WeakReference<IssueActivity> mTarget;
        private boolean mException;

        public LoadCommentsTask(IssueActivity activity) {
            mTarget = new WeakReference<IssueActivity>(activity);
        }

        @Override
        protected List<Comment> doInBackground(Boolean... params) {
            if (mTarget.get() != null) {
                try {
                    return mTarget.get().getComments();
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(List<Comment> result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
    
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
        if (isAuthorized()) {
            menu.clear();
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.issue_menu, menu);

            if ("closed".equals(mIssueState)) {
                menu.removeItem(R.id.issue_close);
            }
            else {
                menu.removeItem(R.id.issue_reopen);
            }
            
            if (!isCollaborator && !isCreator) {
                menu.removeItem(R.id.issue_close);
                menu.removeItem(R.id.issue_reopen);
                menu.removeItem(R.id.issue_edit);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName, 
                        Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;     
            case R.id.issue_create:
                if (isAuthorized()) {
                    Intent intent = new Intent().setClass(this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }                
                return true;
            case R.id.issue_edit:
                if (isAuthorized()) {
                    Intent intent = new Intent().setClass(this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    intent.putExtra(Constants.Issue.ISSUE_NUMBER, mIssue.getNumber());
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }     
                return true;
            case R.id.issue_close:
                if (isAuthorized()) {
                    new CloseIssueTask(this, false).execute();
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            case R.id.issue_reopen:
                if (isAuthorized()) {
                    new ReopenIssueTask(this, false).execute();
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_issue_subject,
                        mIssueNumber, mIssue.getTitle(), mRepoOwner + "/" + mRepoName));
                shareIntent.putExtra(Intent.EXTRA_TEXT,  mIssue.getHtmlUrl());
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
            default:
                return true;
        }
    }
    
    private static class CloseIssueTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<IssueActivity> mTarget;
        private boolean mException;

        public CloseIssueTask(IssueActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<IssueActivity>(activity);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    IssueActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    IssueService issueService = new IssueService(client);
                    
                    Issue issue = issueService.getIssue(new RepositoryId(activity.mRepoOwner,
                            activity.mRepoName), activity.mIssueNumber);
                    
                    issue.setState("closed");
                    
                    activity.mIssue = issueService.editIssue(new RepositoryId(activity.mRepoOwner,
                            activity.mRepoName), issue);
                    return true;
                }
                catch (Exception e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.mProgressDialog = activity.showProgressDialog(activity.getString(R.string.closing_msg), false);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.stopProgressDialog(activity.mProgressDialog);
                
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_close),
                            false);
                }
                else {
                    activity.mIssueState = "closed";
                    activity.showMessage(activity.getResources().getString(R.string.issue_success_close),
                            false);
                    TextView tvState = (TextView)activity.findViewById(R.id.tv_state);
                    tvState.setBackgroundResource(R.drawable.default_red_box);
                    tvState.setText(activity.getString(R.string.closed).toUpperCase(Locale.getDefault()));
                    activity.invalidateOptionsMenu();
                }
            }
        }
    }
    
    private static class ReopenIssueTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<IssueActivity> mTarget;
        private boolean mException;

        public ReopenIssueTask(IssueActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<IssueActivity>(activity);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    IssueActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    IssueService issueService = new IssueService(client);
                    
                    Issue issue = issueService.getIssue(new RepositoryId(activity.mRepoOwner,
                            activity.mRepoName), activity.mIssueNumber);
                    
                    issue.setState("open");
                    
                    activity.mIssue = issueService.editIssue(new RepositoryId(activity.mRepoOwner,
                            activity.mRepoName), issue);
                    return true;
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.mProgressDialog = activity.showProgressDialog(activity.getString(R.string.opening_msg), false);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.stopProgressDialog(activity.mProgressDialog);
                
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_reopen),
                            false);
                }
                else {
                    activity.mIssueState = "open";
                    activity.showMessage(activity.getResources().getString(R.string.issue_success_reopen),
                            false);
                    TextView tvState = (TextView)activity.findViewById(R.id.tv_state);
                    tvState.setBackgroundResource(R.drawable.default_green_box);
                    tvState.setText(activity.getString(R.string.open).toUpperCase(Locale.getDefault()));
                    activity.invalidateOptionsMenu();
                }
            }
        }
    }
    
    private static class CommentIssueTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<IssueActivity> mTarget;
        private boolean mException;

        public CommentIssueTask(IssueActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<IssueActivity>(activity);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    IssueActivity activity = mTarget.get();
                    EditText etComment = (EditText) activity.findViewById(R.id.et_comment);
                    
                    if (etComment.getText() != null && !StringUtils.isBlank(etComment.getText().toString())) {  
                        String comment = etComment.getText().toString();
                        GitHubClient client = new GitHubClient();
                        client.setOAuth2Token(mTarget.get().getAuthToken());
                        IssueService issueService = new IssueService(client);
                        issueService.createComment(activity.mRepoOwner, 
                                activity.mRepoName,
                                activity.mIssueNumber,
                                comment);
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                activity.mProgressDialog = activity.showProgressDialog(activity.getString(R.string.loading_msg), false);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mTarget.get() != null) {
                IssueActivity activity = mTarget.get();
                
                if (mException) {
                    activity.stopProgressDialog(activity.mProgressDialog);
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_comment),
                            false);
                }
                else {
                    if (result) {
                        activity.showMessage(activity.getResources().getString(R.string.issue_success_comment),
                                false);
                        //reload comments
                        new LoadCommentsTask(activity).execute(false);
                    }
                    EditText etComment = (EditText) activity.findViewById(R.id.et_comment);
                    etComment.setText(null);
                    etComment.clearFocus();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.iv_comment:
            EditText etComment = (EditText) findViewById(R.id.et_comment);
            if (etComment.getText() != null && !StringUtils.isBlank(etComment.getText().toString())) {
                new CommentIssueTask(this, false).execute();
            }
            if (getCurrentFocus() != null) {
                hideKeyboard(getCurrentFocus().getWindowToken());
            }
            break;
        case R.id.tv_pull:
            getApplicationContext().openPullRequestActivity(this,
                    mRepoOwner, mRepoName, mIssueNumber);
            break;
        default:
            break;
        }
    }
    
    @Override
    public Loader<HashMap<Integer, Object>> onCreateLoader(int id, Bundle arg1) {
        if (id == 0) {
            return new IssueLoader(this, mRepoOwner, mRepoName, mIssueNumber);
        }
        else {
            return new IsCollaboratorLoader(this, mRepoOwner, mRepoName);
        }
    }

    @Override
    public void onLoadFinished(Loader<HashMap<Integer, Object>> loader, HashMap<Integer, Object> result) {
        if (!isLoaderError(result)) {
            Object data = result.get(LoaderResult.DATA); 
            
            if (loader.getId() == 0) {
                hideLoading();
                mIssue = (Issue) data;
                mIssueState = mIssue.getState();
                getSupportLoaderManager().getLoader(1).forceLoad();
                fillData();
            }
            else {
                isCollaborator = (Boolean) data;
                isCreator = mIssue.getUser().getLogin().equals(getApplicationContext().getAuthLogin());
                invalidateOptionsMenu();
            }
        }
        else {
            hideLoading();
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onLoaderReset(Loader<HashMap<Integer, Object>> loader) {
    }
    
}