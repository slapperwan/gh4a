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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.gh4a.BasePagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.fragment.CommitCompareFragment;
import com.gh4a.fragment.PullRequestFilesFragment;
import com.gh4a.fragment.PullRequestFragment;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.widget.IssueStateTrackingFloatingActionButton;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.MergeStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.Locale;

public class PullRequestActivity extends BasePagerActivity implements
        View.OnClickListener, PullRequestFilesFragment.CommentUpdateListener {
    public static Intent makeIntent(Context context, String repoOwner, String repoName, int number) {
        return makeIntent(context, repoOwner, repoName, number, -1);
    }
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int number, long initialCommentId) {
        return new Intent(context, PullRequestActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("number", number)
                .putExtra("initial_comment", initialCommentId);
    }

    private static final int REQUEST_EDIT_ISSUE = 1001;

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private long mInitialCommentId;
    private Boolean mIsCollaborator;

    private Issue mIssue;
    private PullRequest mPullRequest;
    private PullRequestFragment mPullRequestFragment;
    private IssueStateTrackingFloatingActionButton mEditFab;

    private ViewGroup mHeader;
    private int[] mHeaderColorAttrs;

    private static final int[] TITLES = new int[]{
            R.string.pull_request_conversation, R.string.commits, R.string.pull_request_files
    };

    private final LoaderCallbacks<PullRequest> mPullRequestCallback = new LoaderCallbacks<PullRequest>(this) {
        @Override
        protected Loader<LoaderResult<PullRequest>> onCreateLoader() {
            return new PullRequestLoader(PullRequestActivity.this,
                    mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        protected void onResultReady(PullRequest result) {
            mPullRequest = result;
            fillHeader();
            showContentIfReady();
            supportInvalidateOptionsMenu();
        }
    };

    private final LoaderCallbacks<Issue> mIssueCallback = new LoaderCallbacks<Issue>(this) {
        @Override
        protected Loader<LoaderResult<Issue>> onCreateLoader() {
            return new IssueLoader(PullRequestActivity.this,
                    mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        protected void onResultReady(Issue result) {
            mIssue = result;
            showContentIfReady();
        }
    };

    private final LoaderCallbacks<Boolean> mCollaboratorCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new IsCollaboratorLoader(PullRequestActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        protected void onResultReady(Boolean result) {
            mIsCollaborator = result;
            showContentIfReady();
            supportInvalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();

        mHeader = (ViewGroup) inflater.inflate(R.layout.issue_header, null);
        mHeader.setClickable(false);
        mHeader.setVisibility(View.GONE);
        addHeaderView(mHeader, true);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.pull_request_title) + " #" + mPullRequestNumber);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentShown(false);

        getSupportLoaderManager().initLoader(0, null, mPullRequestCallback);
        getSupportLoaderManager().initLoader(1, null, mIssueCallback);
        getSupportLoaderManager().initLoader(2, null, mCollaboratorCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pullrequest_menu, menu);

        Gh4Application app = Gh4Application.get();
        boolean authorized = app.isAuthorized();

        boolean isCreator = mPullRequest != null
                && ApiHelpers.loginEquals(mPullRequest.getUser(), app.getAuthLogin());
        boolean isCollaborator = mIsCollaborator != null && mIsCollaborator;
        boolean canOpenOrClose = authorized && (isCreator || isCollaborator);
        boolean canMerge = authorized && isCollaborator;

        if (!canOpenOrClose || mPullRequest == null) {
            menu.removeItem(R.id.pull_close);
            menu.removeItem(R.id.pull_reopen);
        } else if (ApiHelpers.IssueState.CLOSED.equals(mPullRequest.getState())) {
            menu.removeItem(R.id.pull_close);
            if (mPullRequest.isMerged()) {
                menu.findItem(R.id.pull_reopen).setEnabled(false);
            }
        } else {
            menu.removeItem(R.id.pull_reopen);
        }

        if (!canMerge || mPullRequest == null) {
            menu.removeItem(R.id.pull_merge);
        } else if (mPullRequest.isMerged() || !mPullRequest.isMergeable()) {
            MenuItem mergeItem = menu.findItem(R.id.pull_merge);
            mergeItem.setEnabled(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pull_merge:
                showMergeDialog();
                break;
            case R.id.pull_close:
            case R.id.pull_reopen:
                new PullRequestOpenCloseTask(item.getItemId() == R.id.pull_reopen).schedule();
                break;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_pull_subject,
                        mPullRequest.getNumber(), mPullRequest.getTitle(),
                        mRepoOwner + "/" + mRepoName));
                shareIntent.putExtra(Intent.EXTRA_TEXT, mPullRequest.getHtmlUrl());
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                break;
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(mPullRequest.getHtmlUrl()));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT_ISSUE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK);
                onRefresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mPullRequestNumber = extras.getInt("number");
        mInitialCommentId = extras.getLong("initial_comment", -1);
    }

    @Override
    public void onRefresh() {
        mIssue = null;
        mPullRequest = null;
        mIsCollaborator = null;
        setContentShown(false);
        if (mEditFab != null) {
            mEditFab.post(new Runnable() {
                @Override
                public void run() {
                    updateFabVisibility();
                }
            });
        }
        mHeader.setVisibility(View.GONE);
        mHeaderColorAttrs = null;
        forceLoaderReload(0, 1, 2);
        invalidateTabs();
        super.onRefresh();
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mPullRequest != null && mIssue != null ? TITLES : null;
    }

    @Override
    protected int[] getHeaderColors() {
        return mHeaderColorAttrs;
    }

    @Override
    protected Fragment getFragment(int position) {
        if (position == 1) {
            return CommitCompareFragment.newInstance(mRepoOwner, mRepoName,
                    mPullRequest.getBase().getSha(), mPullRequest.getHead().getSha());
        } else if (position == 2) {
            return PullRequestFilesFragment.newInstance(mRepoOwner, mRepoName,
                    mPullRequestNumber, mPullRequest.getHead().getSha());
        } else {
            mPullRequestFragment = PullRequestFragment.newInstance(mPullRequest,
                    mIssue, mIsCollaborator, mInitialCommentId);
            mInitialCommentId = -1;
            return mPullRequestFragment;
        }
    }

    @Override
    protected Intent navigateUp() {
        return PullRequestListActivity.makeIntent(this, mRepoOwner, mRepoName);
    }

    @Override
    public void onCommentsUpdated() {
        if (mPullRequestFragment != null) {
            mPullRequestFragment.refreshComments();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mEditFab) {
            Intent editIntent = IssueEditActivity.makeEditIntent(this, mRepoOwner, mRepoName, mIssue);
            startActivityForResult(editIntent, REQUEST_EDIT_ISSUE);
        } else if (v.getId() == R.id.iv_gravatar) {
            Intent intent = UserActivity.makeIntent(this, (User) v.getTag());
            if (intent != null) {
                startActivity(intent);
            }
        }
    }

    private void showContentIfReady() {
        if (mPullRequest != null && mIssue != null && mIsCollaborator != null) {
            setContentShown(true);
            invalidateTabs();
            updateFabVisibility();
        }
    }

    private void showMergeDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        String title = getString(R.string.pull_message_dialog_title, mPullRequest.getNumber());
        View view = inflater.inflate(R.layout.pull_merge_message_dialog, null);

        final EditText editor = (EditText) view.findViewById(R.id.et_commit_message);
        editor.setText(mPullRequest.getTitle());

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.pull_request_merge, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editor.getText() == null ? null : editor.getText().toString();
                        new PullRequestMergeTask(text).schedule();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void updateTabRightMargin(int dimensionResId) {
        int margin = dimensionResId != 0
                ? getResources().getDimensionPixelSize(dimensionResId) : 0;

        View tabs = findViewById(R.id.tabs);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) tabs.getLayoutParams();
        lp.rightMargin = margin;
        tabs.setLayoutParams(lp);
    }

    private void updateFabVisibility() {
        boolean isIssueOwner = mIssue != null
                && ApiHelpers.loginEquals(mIssue.getUser(), Gh4Application.get().getAuthLogin());
        boolean isCollaborator = mIsCollaborator != null && mIsCollaborator;
        boolean shouldHaveFab = (isIssueOwner || isCollaborator)
                && mPullRequest != null && mIssue != null;
        CoordinatorLayout rootLayout = getRootLayout();

        if (shouldHaveFab && mEditFab == null) {
            mEditFab = (IssueStateTrackingFloatingActionButton)
                    getLayoutInflater().inflate(R.layout.issue_edit_fab, rootLayout, false);
            mEditFab.setOnClickListener(this);
            rootLayout.addView(mEditFab);
            updateTabRightMargin(R.dimen.mini_fab_size_with_margin);
        } else if (!shouldHaveFab && mEditFab != null) {
            rootLayout.removeView(mEditFab);
            updateTabRightMargin(0);
            mEditFab = null;
        }
        if (mEditFab != null) {
            mEditFab.setState(mPullRequest.getState());
            mEditFab.setMerged(mPullRequest.isMerged());
        }
    }

    private void fillHeader() {
        final int stateTextResId;

        if (mPullRequest.isMerged()) {
            stateTextResId = R.string.pull_request_merged;
            mHeaderColorAttrs = new int[]{
                    R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark
            };
        } else if (ApiHelpers.IssueState.CLOSED.equals(mPullRequest.getState())) {
            stateTextResId = R.string.closed;
            mHeaderColorAttrs = new int[]{
                    R.attr.colorIssueClosed, R.attr.colorIssueClosedDark
            };
        } else {
            stateTextResId = R.string.open;
            mHeaderColorAttrs = new int[]{
                    R.attr.colorIssueOpen, R.attr.colorIssueOpenDark
            };
        }

        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        tvState.setText(getString(stateTextResId).toUpperCase(Locale.getDefault()));

        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        tvTitle.setText(mPullRequest.getTitle());

        mHeader.setVisibility(View.VISIBLE);
    }

    private void handlePullRequestUpdate() {
        if (mPullRequestFragment != null) {
            mPullRequestFragment.update(mPullRequest);
        }

        fillHeader();
        updateFabVisibility();
        transitionHeaderToColor(mHeaderColorAttrs[0], mHeaderColorAttrs[1]);
        supportInvalidateOptionsMenu();
    }

    private class PullRequestOpenCloseTask extends ProgressDialogTask<PullRequest> {
        private final boolean mOpen;

        public PullRequestOpenCloseTask(boolean open) {
            super(getBaseActivity(), 0, open ? R.string.opening_msg : R.string.closing_msg);
            mOpen = open;
        }

        @Override
        protected ProgressDialogTask<PullRequest> clone() {
            return new PullRequestOpenCloseTask(mOpen);
        }

        @Override
        protected PullRequest run() throws IOException {
            PullRequestService pullService = (PullRequestService)
                    Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

            PullRequest pullRequest = new PullRequest();
            pullRequest.setNumber(mPullRequest.getNumber());
            pullRequest.setState(mOpen ? ApiHelpers.IssueState.OPEN : ApiHelpers.IssueState.CLOSED);

            return pullService.editPullRequest(repoId, pullRequest);
        }

        @Override
        protected void onSuccess(PullRequest result) {
            mPullRequest = result;
            handlePullRequestUpdate();
        }

        @Override
        protected String getErrorMessage() {
            int errorMessageResId =
                    mOpen ? R.string.issue_error_reopen : R.string.issue_error_close;
            return getContext().getString(errorMessageResId, mPullRequest.getNumber());
        }
    }

    private class PullRequestMergeTask extends ProgressDialogTask<MergeStatus> {
        private final String mCommitMessage;

        public PullRequestMergeTask(String commitMessage) {
            super(getBaseActivity(), 0, R.string.merging_msg);
            mCommitMessage = commitMessage;
        }

        @Override
        protected ProgressDialogTask<MergeStatus> clone() {
            return new PullRequestMergeTask(mCommitMessage);
        }

        @Override
        protected MergeStatus run() throws Exception {
            PullRequestService pullService = (PullRequestService)
                    Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

            return pullService.merge(repoId, mPullRequest.getNumber(), mCommitMessage);
        }

        @Override
        protected void onSuccess(MergeStatus result) {
            if (result.isMerged()) {
                mPullRequest.setMerged(true);
                mPullRequest.setState(ApiHelpers.IssueState.CLOSED);
            }
            handlePullRequestUpdate();
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.pull_error_merge, mPullRequest.getNumber());
        }
    }
}
