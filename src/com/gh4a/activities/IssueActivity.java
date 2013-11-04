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
import java.util.List;
import java.util.Locale;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.CommentAdapter;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueCommentListLoader;
import com.gh4a.loader.IssueLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.GravatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class IssueActivity extends LoadingFragmentActivity implements
        OnClickListener, CommentAdapter.OnEditComment {
    private static final int REQUEST_EDIT = 1000;

    private Issue mIssue;
    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private String mIssueState;
    private CommentAdapter mCommentAdapter;
    private boolean mIsCollaborator;

    private LoaderCallbacks<Issue> mIssueCallback = new LoaderCallbacks<Issue>() {
        @Override
        public Loader<LoaderResult<Issue>> onCreateLoader(int id, Bundle args) {
            return new IssueLoader(IssueActivity.this, mRepoOwner, mRepoName, mIssueNumber);
        }
        @Override
        public void onResultReady(LoaderResult<Issue> result) {
            if (!result.handleError(IssueActivity.this)) {
                mIssue = result.getData();
                mIssueState = mIssue.getState();
                fillData();
            } else {
                setContentEmpty(true);
            }
            setContentShown(true);
            invalidateOptionsMenu();
        }
    };

    private LoaderCallbacks<List<Comment>> mCommentCallback = new LoaderCallbacks<List<Comment>>() {
        @Override
        public Loader<LoaderResult<List<Comment>>> onCreateLoader(int id, Bundle args) {
            return new IssueCommentListLoader(IssueActivity.this, mRepoOwner, mRepoName, mIssueNumber);
        }
        @Override
        public void onResultReady(LoaderResult<List<Comment>> result) {
            if (!result.handleError(IssueActivity.this)) {
                fillComments(result.getData());
            }
        }
    };

    private LoaderCallbacks<Boolean> mCollaboratorCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsCollaboratorLoader(IssueActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            if (!result.handleError(IssueActivity.this)) {
                mIsCollaborator = result.getData();
                invalidateOptionsMenu();
            }
        }
    };
    
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
        setContentShown(false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.issue) + " #" + mIssueNumber);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        if (!Gh4Application.get(this).isAuthorized()) {
            findViewById(R.id.comment).setVisibility(View.GONE);
            findViewById(R.id.divider).setVisibility(View.GONE);
        }
        
        mCommentAdapter = new CommentAdapter(this, mRepoOwner, this);

        getSupportLoaderManager().initLoader(0, null, mIssueCallback);
        getSupportLoaderManager().initLoader(1, null, mCollaboratorCallback);
        getSupportLoaderManager().initLoader(2, null, mCommentCallback);
    }

    private void fillData() {
        // set details inside listview header
        LayoutInflater inflater = getLayoutInflater();
        ListView listView = (ListView) findViewById(R.id.list_view);
        
        LinearLayout header = (LinearLayout) inflater.inflate(R.layout.issue_header, listView, false);
        header.setClickable(false);

        UiUtils.assignTypeface(header, Gh4Application.get(this).boldCondensed, new int[] {
            R.id.comment_title, R.id.tv_title, R.id.desc_title 
        });

        listView.addHeaderView(header, null, false);
        listView.setAdapter(mCommentAdapter);
        
        if (!Gh4Application.get(this).isAuthorized()) {
            findViewById(R.id.comment).setVisibility(View.GONE);
            findViewById(R.id.divider).setVisibility(View.GONE);
        }
        
        TextView tvCommentTitle = (TextView) header.findViewById(R.id.comment_title);
        tvCommentTitle.setTextColor(getResources().getColor(R.color.highlight));
        tvCommentTitle.setText(getString(R.string.issue_comments) + " (" + mIssue.getComments() + ")");
        
        ImageView ivGravatar = (ImageView) header.findViewById(R.id.iv_gravatar);
        GravatarHandler.assignGravatar(ivGravatar, mIssue.getUser());
        ivGravatar.setOnClickListener(this);

        TextView tvExtra = (TextView) header.findViewById(R.id.tv_extra);
        tvExtra.setText(mIssue.getUser().getLogin() + "\n" + Gh4Application.pt.format(mIssue.getCreatedAt()));
        
        TextView tvState = (TextView) header.findViewById(R.id.tv_state);
        tvState.setTextColor(Color.WHITE);
        if (Constants.Issue.ISSUE_STATE_CLOSED.equals(mIssue.getState())) {
            tvState.setBackgroundResource(R.drawable.default_red_box);
            tvState.setText(getString(R.string.closed).toUpperCase(Locale.getDefault()));
        } else {
            tvState.setBackgroundResource(R.drawable.default_green_box);
            tvState.setText(getString(R.string.open).toUpperCase(Locale.getDefault()));
        }
        
        TextView tvTitle = (TextView) header.findViewById(R.id.tv_title);
        tvTitle.setText(mIssue.getTitle());
        
        TextView tvDesc = (TextView) header.findViewById(R.id.tv_desc);
        String body = mIssue.getBodyHtml();
        if (!StringUtils.isBlank(body)) {
            HttpImageGetter imageGetter = new HttpImageGetter(this);
            body = HtmlUtils.format(body).toString();
            imageGetter.bind(tvDesc, body, mIssue.getNumber());
        }
        tvDesc.setMovementMethod(LinkMovementMethod.getInstance());
        
        findViewById(R.id.iv_comment).setOnClickListener(this);

        boolean showInfoBox = false;
        TextView tvMilestone = (TextView) header.findViewById(R.id.tv_milestone);
        if (mIssue.getMilestone() != null) {
            showInfoBox = true;
            tvMilestone.setText(getString(R.string.issue_milestone, mIssue.getMilestone().getTitle()));
        } else {
            tvMilestone.setVisibility(View.GONE);
        }
        
        if (mIssue.getAssignee() != null) {
            showInfoBox = true;
            TextView tvAssignee = (TextView) header.findViewById(R.id.tv_assignee);
            tvAssignee.setText(getString(R.string.issue_assignee, mIssue.getAssignee().getLogin()));
            tvAssignee.setVisibility(View.VISIBLE);
            tvAssignee.setOnClickListener(this);
            
            ImageView ivAssignee = (ImageView) header.findViewById(R.id.iv_assignee);
            GravatarHandler.assignGravatar(ivAssignee, mIssue.getAssignee());
            ivAssignee.setVisibility(View.VISIBLE);
            ivAssignee.setOnClickListener(this);
        }
        
        
        LinearLayout llLabels = (LinearLayout) findViewById(R.id.ll_labels);
        List<Label> labels = mIssue.getLabels();
        
        if (labels != null && !labels.isEmpty()) {
            showInfoBox = true;
            for (Label label : labels) {
                TextView tvLabel = (TextView) inflater.inflate(R.layout.issue_label, null);
                int color = Color.parseColor("#" + label.getColor());

                tvLabel.setText(label.getName());
                tvLabel.setBackgroundColor(color);
                tvLabel.setTextColor(UiUtils.textColorForBackground(this, color));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.rightMargin = getResources().getDimensionPixelSize(R.dimen.label_spacing);
                llLabels.addView(tvLabel, params);
            }
        } else {
            llLabels.setVisibility(View.GONE);
        }
        
        TextView tvPull = (TextView) header.findViewById(R.id.tv_pull);
        if (mIssue.getPullRequest() != null && mIssue.getPullRequest().getDiffUrl() != null) {
            showInfoBox = true;
            tvPull.setVisibility(View.VISIBLE);
            tvPull.setOnClickListener(this);
        }
        
        if (!showInfoBox) {
            header.findViewById(R.id.info_box).setVisibility(View.GONE);
        }
    }

    protected void fillComments(List<Comment> comments) {
        mCommentAdapter.clear();
        if (comments != null) {
            mCommentAdapter.addAll(comments);
        }
        mCommentAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Gh4Application.get(this).isAuthorized()) {
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.issue_menu, menu);

            if (Constants.Issue.ISSUE_STATE_CLOSED.equals(mIssueState)) {
                menu.removeItem(R.id.issue_close);
            } else {
                menu.removeItem(R.id.issue_reopen);
            }
            
            boolean isCreator = mIssue != null &&
                    mIssue.getUser().getLogin().equals(Gh4Application.get(this).getAuthLogin());

            if (!mIsCollaborator && !isCreator) {
                menu.removeItem(R.id.issue_close);
                menu.removeItem(R.id.issue_reopen);
                menu.removeItem(R.id.issue_edit);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        Gh4Application.get(this).openIssueListActivity(this, mRepoOwner, mRepoName,
                Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.issue_create:
                if (checkForAuthOrExit()) {
                    Intent intent = new Intent().setClass(this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    startActivity(intent);
                }
                return true;
            case R.id.issue_edit:
                if (checkForAuthOrExit()) {
                    Intent intent = new Intent().setClass(this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    intent.putExtra(Constants.Issue.ISSUE_NUMBER, mIssue.getNumber());
                    startActivity(intent);
                }
                return true;
            case R.id.issue_close:
            case R.id.issue_reopen:
                if (checkForAuthOrExit()) {
                    new IssueOpenCloseTask(item.getItemId() == R.id.issue_reopen).execute();
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
        }
        return super.onOptionsItemSelected(item);
    }
    
    private boolean checkForAuthOrExit() {
        if (Gh4Application.get(this).isAuthorized()) {
            return true;
        }
        Intent intent = new Intent(this, Github4AndroidActivity.class);
        startActivity(intent);
        finish();
        return false;
    }
    
    @Override
    public void onClick(View v) {
        Gh4Application app = Gh4Application.get(this);
        switch (v.getId()) {
        case R.id.iv_gravatar:
            app.openUserInfoActivity(this, mIssue.getUser().getLogin(), null);
            break;
        case R.id.tv_assignee:
        case R.id.iv_assignee:
            app.openUserInfoActivity(this, mIssue.getAssignee().getLogin(), null);
            break;
        case R.id.iv_comment:
            EditText etComment = (EditText) findViewById(R.id.et_comment);
            String comment = etComment.getText() == null ? null : etComment.getText().toString();
            if (!StringUtils.isBlank(comment)) {
                new CommentIssueTask(comment).execute();
            }
            UiUtils.hideImeForView(getCurrentFocus());
            break;
        case R.id.tv_pull:
            app.openPullRequestActivity(this, mRepoOwner, mRepoName, mIssueNumber);
            break;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT && resultCode == Activity.RESULT_OK) {
            getSupportLoaderManager().restartLoader(2, null, mCommentCallback);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void editComment(Comment comment) {
        Intent intent = new Intent(this, EditCommentActivity.class);
        
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Comment.ID, comment.getId());
        intent.putExtra(Constants.Comment.BODY, comment.getBody());
        startActivityForResult(intent, REQUEST_EDIT);
    }

    private class IssueOpenCloseTask extends ProgressDialogTask<Issue> {
        private boolean mOpen;
        
        public IssueOpenCloseTask(boolean open) {
            super(IssueActivity.this, 0, open ? R.string.opening_msg : R.string.closing_msg);
            mOpen = open;
        }

        @Override
        protected Issue run() throws IOException {
            IssueService issueService = (IssueService)
                    Gh4Application.get(mContext).getService(Gh4Application.ISSUE_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            
            Issue issue = issueService.getIssue(repoId, mIssueNumber);
            issue.setState(mOpen ? "open" : "closed");

            return issueService.editIssue(repoId, issue);
        }

        @Override
        protected void onSuccess(Issue result) {
            mIssue = result;
            mIssueState = mOpen ? "open" : "closed";
            ToastUtils.showMessage(mContext,
                    mOpen ? R.string.issue_success_reopen : R.string.issue_success_close);
            
            TextView tvState = (TextView) findViewById(R.id.tv_state);
            tvState.setBackgroundResource(mOpen ? R.drawable.default_green_box : R.drawable.default_red_box);
            tvState.setText(getString(mOpen ? R.string.open : R.string.closed).toUpperCase(Locale.getDefault()));
            invalidateOptionsMenu();
        }
        
        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.issue_error_close);
        }
    }
    
    private class CommentIssueTask extends ProgressDialogTask<Void> {
        private String mComment;

        public CommentIssueTask(String comment) {
            super(IssueActivity.this, 0, R.string.loading_msg);
            mComment = comment;
        }

        @Override
        protected Void run() throws IOException {
            IssueService issueService = (IssueService)
                    Gh4Application.get(mContext).getService(Gh4Application.ISSUE_SERVICE);
            issueService.createComment(mRepoOwner, mRepoName, mIssueNumber, mComment);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            ToastUtils.showMessage(mContext, R.string.issue_success_comment);
            //reload comments
            getSupportLoaderManager().restartLoader(2, null, mCommentCallback);
            
            EditText etComment = (EditText) findViewById(R.id.et_comment);
            etComment.setText(null);
            etComment.clearFocus();
        }
        
        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.issue_error_comment);
        }
    }
}