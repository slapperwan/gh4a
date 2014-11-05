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
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.IssueEventAdapter;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueCommentListLoader;
import com.gh4a.loader.IssueEventHolder;
import com.gh4a.loader.IssueLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;
import com.shamanland.fab.FloatingActionButton;

public class IssueActivity extends BaseActivity implements
        View.OnClickListener, IssueEventAdapter.OnEditComment,
        SwipeRefreshLayout.ChildScrollDelegate {
    private static final int REQUEST_EDIT = 1000;

    private Issue mIssue;
    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private ViewGroup mHeader;
    private TextView mDescriptionView;
    private boolean mEventsLoaded;
    private IssueEventAdapter mEventAdapter;
    private ListView mListView;
    private boolean mIsCollaborator;
    private FloatingActionButton mEditFab;

    private LoaderCallbacks<Issue> mIssueCallback = new LoaderCallbacks<Issue>() {
        @Override
        public Loader<LoaderResult<Issue>> onCreateLoader(int id, Bundle args) {
            return new IssueLoader(IssueActivity.this, mRepoOwner, mRepoName, mIssueNumber);
        }
        @Override
        public void onResultReady(LoaderResult<Issue> result) {
            if (!result.handleError(IssueActivity.this)) {
                mIssue = result.getData();
                fillDataIfDone();
                invalidateOptionsMenu();
            } else {
                setContentEmpty(true);
                setContentShown(true);
            }
        }
    };

    private LoaderCallbacks<List<IssueEventHolder>> mEventCallback =
            new LoaderCallbacks<List<IssueEventHolder>>() {
        @Override
        public Loader<LoaderResult<List<IssueEventHolder>>> onCreateLoader(int id, Bundle args) {
            return new IssueCommentListLoader(IssueActivity.this, mRepoOwner, mRepoName, mIssueNumber);
        }
        @Override
        public void onResultReady(LoaderResult<List<IssueEventHolder>> result) {
            if (!result.handleError(IssueActivity.this)) {
                List<IssueEventHolder> events = result.getData();
                mEventAdapter.clear();
                if (events != null) {
                    mEventAdapter.addAll(events);
                }
                mEventAdapter.notifyDataSetChanged();
                mEventsLoaded = true;
                fillDataIfDone();
            } else {
                setContentEmpty(true);
                setContentShown(true);
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
                if (mIsCollaborator) {
                    mEditFab.setVisibility(View.VISIBLE);
                }
                invalidateOptionsMenu();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mIssueNumber = data.getInt(Constants.Issue.NUMBER);

        if (hasErrorView()) {
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

        mListView = (ListView) findViewById(android.R.id.list);

        LinearLayout header = (LinearLayout) findViewById(R.id.header);
        LayoutInflater inflater = getLayoutInflater();

        mHeader = (ViewGroup) inflater.inflate(R.layout.issue_header, header, false);
        mHeader.setClickable(false);
        mHeader.setVisibility(View.GONE);
        header.addView(mHeader);

        mDescriptionView = (TextView) inflater.inflate(R.layout.issue_description, mListView, false);
        mListView.addHeaderView(mDescriptionView);

        mEventAdapter = new IssueEventAdapter(this, mRepoOwner, mRepoName, this);
        mListView.setAdapter(mEventAdapter);

        setChildScrollDelegate(this);

        if (!Gh4Application.get(this).isAuthorized()) {
            findViewById(R.id.comment).setVisibility(View.GONE);
            findViewById(R.id.divider).setVisibility(View.GONE);
        }

        mEditFab = (FloatingActionButton) inflater.inflate(R.layout.issue_edit_fab, null);
        mEditFab.setOnClickListener(this);
        mEditFab.setVisibility(View.GONE);
        setHeaderAlignedActionButton(mEditFab);

        getSupportLoaderManager().initLoader(0, null, mIssueCallback);
        getSupportLoaderManager().initLoader(1, null, mCollaboratorCallback);
        getSupportLoaderManager().initLoader(2, null, mEventCallback);
    }

    @Override
    public boolean canChildScrollUp() {
        return UiUtils.canViewScrollUp(mListView);
    }

    private void fillDataIfDone() {
        if (mIssue == null || !mEventsLoaded) {
            return;
        }
        fillData();
        setContentShown(true);
    }

    private void fillData() {
        // set details inside listview header
        ImageView ivGravatar = (ImageView) mHeader.findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(ivGravatar, mIssue.getUser());
        ivGravatar.setOnClickListener(this);

        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        boolean closed = Constants.Issue.STATE_CLOSED.equals(mIssue.getState());
        int stateTextResId = closed ? R.string.closed : R.string.open;
        int stateColorAttributeId = closed ? R.attr.colorIssueClosed : R.attr.colorIssueOpen;

        tvState.setText(getString(stateTextResId).toUpperCase(Locale.getDefault()));
        transitionHeaderToColor(UiUtils.resolveColor(this, stateColorAttributeId));
        mEditFab.setSelected(closed);

        TextView tvExtra = (TextView) mHeader.findViewById(R.id.tv_extra);
        tvExtra.setText(mIssue.getUser().getLogin());

        TextView tvTimestamp = (TextView) mHeader.findViewById(R.id.tv_timestamp);
        tvTimestamp.setText(StringUtils.formatRelativeTime(this, mIssue.getCreatedAt(), true));

        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        tvTitle.setText(mIssue.getTitle());

        String body = mIssue.getBodyHtml();
        if (!StringUtils.isBlank(body)) {
            HttpImageGetter imageGetter = new HttpImageGetter(this);
            body = HtmlUtils.format(body).toString();
            imageGetter.bind(mDescriptionView, body, mIssue.getNumber());
        }
        mDescriptionView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);

        findViewById(R.id.iv_comment).setOnClickListener(this);

        TextView tvMilestone = (TextView) mHeader.findViewById(R.id.tv_milestone);
        if (mIssue.getMilestone() != null) {
            tvMilestone.setText(getString(R.string.issue_milestone, mIssue.getMilestone().getTitle()));
            tvMilestone.setVisibility(View.VISIBLE);
        } else {
            tvMilestone.setVisibility(View.GONE);
        }

        TextView tvAssignee = (TextView) mHeader.findViewById(R.id.tv_assignee);
        ImageView ivAssignee = (ImageView) mHeader.findViewById(R.id.iv_assignee);
        if (mIssue.getAssignee() != null) {
            tvAssignee.setText(getString(R.string.issue_assignee, mIssue.getAssignee().getLogin()));
            tvAssignee.setVisibility(View.VISIBLE);
            tvAssignee.setOnClickListener(this);

            AvatarHandler.assignAvatar(ivAssignee, mIssue.getAssignee());
            ivAssignee.setVisibility(View.VISIBLE);
            ivAssignee.setOnClickListener(this);
        } else {
            tvAssignee.setVisibility(View.GONE);
            ivAssignee.setVisibility(View.GONE);
        }


        LinearLayout llLabels = (LinearLayout) findViewById(R.id.ll_labels);
        List<Label> labels = mIssue.getLabels();

        if (labels != null && !labels.isEmpty()) {
            llLabels.removeAllViews();
            llLabels.setVisibility(View.VISIBLE);

            for (Label label : labels) {
                TextView tvLabel = (TextView) getLayoutInflater().inflate(R.layout.issue_label, null);
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

        TextView tvPull = (TextView) mHeader.findViewById(R.id.tv_pull);
        if (mIssue.getPullRequest() != null && mIssue.getPullRequest().getDiffUrl() != null) {
            tvPull.setVisibility(View.VISIBLE);
            tvPull.setOnClickListener(this);
        } else {
            tvPull.setVisibility(View.GONE);
        }

        mHeader.setVisibility(View.VISIBLE);
        mEditFab.setVisibility(View.VISIBLE);

        refreshDone();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Gh4Application.get(this).isAuthorized()) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.issue_menu, menu);

            if (mIssue == null) {
                menu.removeItem(R.id.issue_close);
                menu.removeItem(R.id.issue_reopen);
            } else if (Constants.Issue.STATE_CLOSED.equals(mIssue.getState())) {
                menu.removeItem(R.id.issue_close);
            } else {
                menu.removeItem(R.id.issue_reopen);
            }

            boolean isCreator = mIssue != null &&
                    mIssue.getUser().getLogin().equals(Gh4Application.get(this).getAuthLogin());

            if (!mIsCollaborator && !isCreator) {
                menu.removeItem(R.id.issue_close);
                menu.removeItem(R.id.issue_reopen);
            }
            if (mIssue == null) {
                menu.removeItem(R.id.share);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getIssueListActivityIntent(this,
                mRepoOwner, mRepoName, Constants.Issue.STATE_OPEN);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.issue_close:
            case R.id.issue_reopen:
                if (checkForAuthOrExit()) {
                    new IssueOpenCloseTask(itemId == R.id.issue_reopen).execute();
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

    @Override
    protected boolean canSwipeToRefresh() {
        return true;
    }

    @Override
    public void onRefresh() {
        mIssue = null;
        mEventsLoaded = false;
        setContentShown(false);
        getSupportLoaderManager().restartLoader(0, null, mIssueCallback);
        getSupportLoaderManager().restartLoader(1, null, mCollaboratorCallback);
        getSupportLoaderManager().restartLoader(2, null, mEventCallback);
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
        Intent intent = null;
        switch (v.getId()) {
        case R.id.edit_fab:
            if (checkForAuthOrExit()) {
                intent = new Intent(this, IssueEditActivity.class);
                intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.NAME, mRepoName);
                intent.putExtra(IssueEditActivity.EXTRA_ISSUE, mIssue);
            }
            break;
        case R.id.iv_gravatar:
            intent = IntentUtils.getUserActivityIntent(this, mIssue.getUser());
            break;
        case R.id.tv_assignee:
        case R.id.iv_assignee:
            intent = IntentUtils.getUserActivityIntent(this, mIssue.getAssignee());
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
            intent = IntentUtils.getPullRequestActivityIntent(this,
                    mRepoOwner, mRepoName, mIssueNumber);
            break;
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                getSupportLoaderManager().getLoader(2).onContentChanged();
                setResult(RESULT_OK);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void editComment(Comment comment) {
        Intent intent = new Intent(this, EditIssueCommentActivity.class);

        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
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
            issue.setState(mOpen ? Constants.Issue.STATE_OPEN : Constants.Issue.STATE_CLOSED);

            return issueService.editIssue(repoId, issue);
        }

        @Override
        protected void onSuccess(Issue result) {
            mIssue = result;
            ToastUtils.showMessage(mContext,
                    mOpen ? R.string.issue_success_reopen : R.string.issue_success_close);

            // reload issue state
            fillDataIfDone();
            // reload events, the action will have triggered an additional one
            getSupportLoaderManager().getLoader(2).onContentChanged();
            setResult(RESULT_OK);
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
            getSupportLoaderManager().getLoader(2).onContentChanged();
            setResult(RESULT_OK);

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
