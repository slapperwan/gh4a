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
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.IssueEventAdapter;
import com.gh4a.fragment.CommentBoxFragment;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueCommentListLoader;
import com.gh4a.loader.IssueEventHolder;
import com.gh4a.loader.IssueLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;
import com.gh4a.widget.IssueLabelSpan;
import com.gh4a.widget.IssueStateTrackingFloatingActionButton;
import com.gh4a.widget.SwipeRefreshLayout;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class IssueActivity extends BaseActivity implements
        View.OnClickListener, IssueEventAdapter.OnEditComment,
        SwipeRefreshLayout.ChildScrollDelegate, CommentBoxFragment.Callback {
    private static final int REQUEST_EDIT = 1000;
    private static final int REQUEST_EDIT_ISSUE = 1001;

    private Issue mIssue;
    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private ViewGroup mHeader;
    private View mListHeaderView;
    private boolean mEventsLoaded;
    private IssueEventAdapter mEventAdapter;
    private RecyclerView mRecyclerView;
    private boolean mIsCollaborator;
    private IssueStateTrackingFloatingActionButton mEditFab;
    private CommentBoxFragment mCommentFragment;
    private HttpImageGetter mImageGetter;
    private Handler mHandler = new Handler();

    private LoaderCallbacks<Issue> mIssueCallback = new LoaderCallbacks<Issue>(this) {
        @Override
        protected Loader<LoaderResult<Issue>> onCreateLoader() {
            return new IssueLoader(IssueActivity.this, mRepoOwner, mRepoName, mIssueNumber);
        }
        @Override
        protected void onResultReady(Issue result) {
            mIssue = result;
            fillDataIfDone();
            supportInvalidateOptionsMenu();
        }
    };

    private LoaderCallbacks<List<IssueEventHolder>> mEventCallback =
            new LoaderCallbacks<List<IssueEventHolder>>(this) {
        @Override
        protected Loader<LoaderResult<List<IssueEventHolder>>> onCreateLoader() {
            return new IssueCommentListLoader(IssueActivity.this, mRepoOwner, mRepoName, mIssueNumber);
        }
        @Override
        protected void onResultReady(List<IssueEventHolder> result) {
            mEventAdapter.clear();
            if (result != null) {
                mEventAdapter.addAll(result);
            }
            mEventAdapter.notifyDataSetChanged();
            mEventsLoaded = true;
            fillDataIfDone();
        }
    };

    private LoaderCallbacks<Boolean> mCollaboratorCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new IsCollaboratorLoader(IssueActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        protected void onResultReady(Boolean result) {
            mIsCollaborator = result;
            updateFabVisibility();
            updateCommentLockState();
            supportInvalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.issue);
        setContentShown(false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.issue) + " #" + mIssueNumber);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this));
        mImageGetter = new HttpImageGetter(this);

        LayoutInflater inflater = getLayoutInflater();

        mHeader = (ViewGroup) inflater.inflate(R.layout.issue_header, null);
        mHeader.setClickable(false);
        mHeader.setVisibility(View.GONE);
        addHeaderView(mHeader, false);

        mListHeaderView = inflater.inflate(R.layout.issue_comment_list_header, mRecyclerView, false);

        mEventAdapter = new IssueEventAdapter(this, mRepoOwner, mRepoName, this);
        mEventAdapter.setHeaderView(mListHeaderView);
        mRecyclerView.setAdapter(mEventAdapter);

        setChildScrollDelegate(this);

        if (!Gh4Application.get().isAuthorized()) {
            findViewById(R.id.comment_box).setVisibility(View.GONE);
        }

        FragmentManager fm = getSupportFragmentManager();
        mCommentFragment = (CommentBoxFragment) fm.findFragmentById(R.id.comment_box);

        setToolbarScrollable(true);

        getSupportLoaderManager().initLoader(0, null, mIssueCallback);
        getSupportLoaderManager().initLoader(1, null, mCollaboratorCallback);
        getSupportLoaderManager().initLoader(2, null, mEventCallback);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
        mIssueNumber = extras.getInt(Constants.Issue.NUMBER);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageGetter.destroy();
        if (mEventAdapter != null) {
            mEventAdapter.destroy();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEventAdapter.resume();
        mImageGetter.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mEventAdapter.pause();
        mImageGetter.pause();
    }

    @Override
    public boolean canChildScrollUp() {
        if (mCommentFragment != null && mCommentFragment.canChildScrollUp()) {
            return true;
        }
        return UiUtils.canViewScrollUp(mRecyclerView);
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
        ImageView ivGravatar = (ImageView) mListHeaderView.findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(ivGravatar, mIssue.getUser());
        ivGravatar.setOnClickListener(this);

        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        boolean closed = Constants.Issue.STATE_CLOSED.equals(mIssue.getState());
        int stateTextResId = closed ? R.string.closed : R.string.open;
        int stateColorAttributeId = closed ? R.attr.colorIssueClosed : R.attr.colorIssueOpen;

        tvState.setText(getString(stateTextResId).toUpperCase(Locale.getDefault()));
        transitionHeaderToColor(stateColorAttributeId,
                closed ? R.attr.colorIssueClosedDark : R.attr.colorIssueOpenDark);
        UiUtils.trySetListOverscrollColor(mRecyclerView,
                UiUtils.resolveColor(this, stateColorAttributeId));

        TextView tvExtra = (TextView) mListHeaderView.findViewById(R.id.tv_extra);
        tvExtra.setText(ApiHelpers.getUserLogin(this, mIssue.getUser()));

        TextView tvTimestamp = (TextView) mListHeaderView.findViewById(R.id.tv_timestamp);
        tvTimestamp.setText(StringUtils.formatRelativeTime(this, mIssue.getCreatedAt(), true));

        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        tvTitle.setText(mIssue.getTitle());

        String body = mIssue.getBodyHtml();
        TextView descriptionView = (TextView) mListHeaderView.findViewById(R.id.tv_desc);
        if (!StringUtils.isBlank(body)) {
            body = HtmlUtils.format(body).toString();
            mImageGetter.bind(descriptionView, body, mIssue.getNumber());
        }
        descriptionView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);

        View milestoneGroup = mListHeaderView.findViewById(R.id.milestone_container);
        if (mIssue.getMilestone() != null) {
            TextView tvMilestone = (TextView) mListHeaderView.findViewById(R.id.tv_milestone);
            tvMilestone.setText(mIssue.getMilestone().getTitle());
            milestoneGroup.setVisibility(View.VISIBLE);
        } else {
            milestoneGroup.setVisibility(View.GONE);
        }

        View assigneeGroup = mListHeaderView.findViewById(R.id.assignee_container);
        if (mIssue.getAssignee() != null) {
            TextView tvAssignee = (TextView) mListHeaderView.findViewById(R.id.tv_assignee);
            tvAssignee.setText(mIssue.getAssignee().getLogin());

            ImageView ivAssignee = (ImageView) mListHeaderView.findViewById(R.id.iv_assignee);
            AvatarHandler.assignAvatar(ivAssignee, mIssue.getAssignee());
            ivAssignee.setOnClickListener(this);
            assigneeGroup.setVisibility(View.VISIBLE);
        } else {
            assigneeGroup.setVisibility(View.GONE);
        }

        List<Label> labels = mIssue.getLabels();
        View labelGroup = mListHeaderView.findViewById(R.id.label_container);
        if (labels != null && !labels.isEmpty()) {
            TextView labelView = (TextView) mListHeaderView.findViewById(R.id.labels);
            SpannableStringBuilder builder = new SpannableStringBuilder();

            for (Label label : labels) {
                int pos = builder.length();
                IssueLabelSpan span = new IssueLabelSpan(this, label, true);
                builder.append(label.getName());
                builder.setSpan(span, pos, pos + label.getName().length(), 0);
            }
            labelView.setText(builder);
            labelGroup.setVisibility(View.VISIBLE);
        } else {
            labelGroup.setVisibility(View.GONE);
        }

        TextView tvPull = (TextView) mListHeaderView.findViewById(R.id.tv_pull);
        if (mIssue.getPullRequest() != null && mIssue.getPullRequest().getDiffUrl() != null) {
            tvPull.setVisibility(View.VISIBLE);
            tvPull.setOnClickListener(this);
        } else {
            tvPull.setVisibility(View.GONE);
        }

        mHeader.setVisibility(View.VISIBLE);
        updateFabVisibility();
        updateCommentLockState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.issue_menu, menu);

        boolean authorized = Gh4Application.get().isAuthorized();
        boolean isCreator = mIssue != null && authorized &&
                ApiHelpers.loginEquals(mIssue.getUser(), Gh4Application.get().getAuthLogin());
        boolean canOpenOrClose = mIssue != null && authorized && (isCreator || mIsCollaborator);

        if (!canOpenOrClose) {
            menu.removeItem(R.id.issue_close);
            menu.removeItem(R.id.issue_reopen);
        } else if (Constants.Issue.STATE_CLOSED.equals(mIssue.getState())) {
            menu.removeItem(R.id.issue_close);
        } else {
            menu.removeItem(R.id.issue_reopen);
        }

        if (mIssue == null) {
            menu.removeItem(R.id.share);
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
                    new IssueOpenCloseTask(itemId == R.id.issue_reopen).schedule();
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
    public void onRefresh() {
        mIssue = null;
        mEventsLoaded = false;
        mIsCollaborator = false;
        setContentShown(false);
        transitionHeaderToColor(R.attr.colorPrimary, R.attr.colorPrimaryDark);
        mHeader.setVisibility(View.GONE);

        // onRefresh() can be triggered in the draw loop, and CoordinatorLayout doesn't
        // like its child list being changed while drawing
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateFabVisibility();
                updateCommentLockState();
            }
        });

        LoaderManager lm = getSupportLoaderManager();
        for (int i = 0; i < 3; i++) {
            lm.getLoader(i).onContentChanged();
        }
        super.onRefresh();
    }

    private void updateCommentLockState() {
        boolean locked = mIssue != null && mIssue.isLocked() && !mIsCollaborator;
        mCommentFragment.setLocked(locked);
    }

    private void updateFabVisibility() {
        boolean shouldHaveFab = mIsCollaborator && mIssue != null && mEventsLoaded;
        CoordinatorLayout rootLayout = getRootLayout();

        if (shouldHaveFab && mEditFab == null) {
            mEditFab = (IssueStateTrackingFloatingActionButton)
                    getLayoutInflater().inflate(R.layout.issue_edit_fab, rootLayout, false);
            mEditFab.setOnClickListener(this);
            rootLayout.addView(mEditFab);
        } else if (!shouldHaveFab && mEditFab != null) {
            rootLayout.removeView(mEditFab);
            mEditFab = null;
        }
        if (mEditFab != null) {
            mEditFab.setState(mIssue.getState());
        }
    }

    private boolean checkForAuthOrExit() {
        if (Gh4Application.get().isAuthorized()) {
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
                Intent editIntent = new Intent(this, IssueEditActivity.class);
                editIntent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                editIntent.putExtra(Constants.Repository.NAME, mRepoName);
                editIntent.putExtra(IssueEditActivity.EXTRA_ISSUE, mIssue);
                startActivityForResult(editIntent, REQUEST_EDIT_ISSUE);
            }
            break;
        case R.id.iv_gravatar:
            intent = IntentUtils.getUserActivityIntent(this, mIssue.getUser());
            break;
        case R.id.tv_assignee:
        case R.id.iv_assignee:
            intent = IntentUtils.getUserActivityIntent(this, mIssue.getAssignee());
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
        } else if (requestCode == REQUEST_EDIT_ISSUE) {
            if (resultCode == Activity.RESULT_OK) {
                getSupportLoaderManager().getLoader(0).onContentChanged();
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

    @Override
    public int getCommentEditorHintResId() {
        return R.string.issue_comment_hint;
    }

    @Override
    public void onSendCommentInBackground(String comment) throws IOException {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        issueService.createComment(mRepoOwner, mRepoName, mIssueNumber, comment);
    }

    @Override
    public void onCommentSent() {
        //reload comments
        getSupportLoaderManager().getLoader(2).onContentChanged();
        setResult(RESULT_OK);
    }

    private class IssueOpenCloseTask extends ProgressDialogTask<Issue> {
        private boolean mOpen;

        public IssueOpenCloseTask(boolean open) {
            super(IssueActivity.this, 0, open ? R.string.opening_msg : R.string.closing_msg);
            mOpen = open;
        }

        @Override
        protected ProgressDialogTask<Issue> clone() {
            return new IssueOpenCloseTask(mOpen);
        }

        @Override
        protected Issue run() throws IOException {
            IssueService issueService = (IssueService)
                    Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

            Issue issue = issueService.getIssue(repoId, mIssueNumber);
            issue.setState(mOpen ? Constants.Issue.STATE_OPEN : Constants.Issue.STATE_CLOSED);

            return issueService.editIssue(repoId, issue);
        }

        @Override
        protected void onSuccess(Issue result) {
            mIssue = result;

            // reload issue state
            fillDataIfDone();
            // reload events, the action will have triggered an additional one
            getSupportLoaderManager().getLoader(2).onContentChanged();
            setResult(RESULT_OK);
            supportInvalidateOptionsMenu();
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.issue_error_close, mIssueNumber);
        }
    }
}
