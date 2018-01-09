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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.gh4a.BaseFragmentPagerActivity;
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
import com.gh4a.loader.PendingReviewLoader;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.BottomSheetCompatibleScrollingViewBehavior;
import com.gh4a.widget.IssueStateTrackingFloatingActionButton;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.MergeStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PullRequestActivity extends BaseFragmentPagerActivity implements
        View.OnClickListener, PullRequestFilesFragment.CommentUpdateListener {
    public static Intent makeIntent(Context context, String repoOwner, String repoName, int number) {
        return makeIntent(context, repoOwner, repoName, number, -1, null);
    }
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int number, int initialPage, IntentUtils.InitialCommentMarker initialComment) {
        return new Intent(context, PullRequestActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("number", number)
                .putExtra("initial_page", initialPage)
                .putExtra("initial_comment", initialComment);
    }

    public static final int PAGE_CONVERSATION = 0;
    public static final int PAGE_COMMITS = 1;
    public static final int PAGE_FILES = 2;

    private static final int REQUEST_EDIT_ISSUE = 1001;
    private static final int REQUEST_CREATE_REVIEW = 1002;

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private int mInitialPage;
    private IntentUtils.InitialCommentMarker mInitialComment;
    private Boolean mIsCollaborator;

    private Issue mIssue;
    private PullRequest mPullRequest;
    private PullRequestFragment mPullRequestFragment;
    private IssueStateTrackingFloatingActionButton mEditFab;
    private Review mPendingReview;
    private boolean mPendingReviewLoaded;

    private ViewGroup mHeader;
    private int[] mHeaderColorAttrs;

    private static final int[] TITLES = new int[]{
            R.string.pull_request_conversation, R.string.commits, R.string.pull_request_files
    };

    private class MergeMethodDesc {
        final @StringRes int textResId;
        final String action;

        public MergeMethodDesc(@StringRes int textResId, String action) {
            this.textResId = textResId;
            this.action = action;
        }

        @Override
        public String toString() {
            return getString(textResId);
        }
    }

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

    private final LoaderCallbacks<List<Review>> mPendingReviewCallback =
            new LoaderCallbacks<List<Review>>(this) {
        @Override
        protected Loader<LoaderResult<List<Review>>> onCreateLoader() {
            return new PendingReviewLoader(PullRequestActivity.this,
                    mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        protected void onResultReady(List<Review> result) {
            String ownLogin = Gh4Application.get().getAuthLogin();
            mPendingReview = null;
            for (Review review : result) {
                if (ApiHelpers.loginEquals(review.getUser(), ownLogin)) {
                    mPendingReview = review;
                    break;
                }
            }
            mPendingReviewLoaded = true;
            supportInvalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(UiUtils.makeHeaderThemedContext(this));
        mHeader = (ViewGroup) inflater.inflate(R.layout.issue_header, null);
        mHeader.setClickable(false);
        mHeader.setVisibility(View.GONE);
        addHeaderView(mHeader, !hasTabsInToolbar());

        setContentShown(false);

        getSupportLoaderManager().initLoader(0, null, mPullRequestCallback);
        getSupportLoaderManager().initLoader(1, null, mIssueCallback);
        getSupportLoaderManager().initLoader(2, null, mCollaboratorCallback);
        getSupportLoaderManager().initLoader(3, null, mPendingReviewCallback);
    }

    @NonNull
    protected String getActionBarTitle() {
        return getString(R.string.pull_request_title) + " #" + mPullRequestNumber;
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected AppBarLayout.ScrollingViewBehavior onCreateSwipeLayoutBehavior() {
        return new BottomSheetCompatibleScrollingViewBehavior();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pullrequest_menu, menu);

        Gh4Application app = Gh4Application.get();
        boolean authorized = app.isAuthorized();

        boolean isCreator = mPullRequest != null
                && ApiHelpers.loginEquals(mPullRequest.getUser(), app.getAuthLogin());
        boolean isClosed = mPullRequest != null
                && ApiHelpers.IssueState.CLOSED.equals(mPullRequest.getState());
        boolean isCollaborator = mIsCollaborator != null && mIsCollaborator;
        boolean closerIsCreator = mIssue != null
                && ApiHelpers.userEquals(mIssue.getUser(), mIssue.getClosedBy());
        boolean canClose = mPullRequest != null && authorized && (isCreator || isCollaborator);
        boolean canOpen = canClose && (isCollaborator || closerIsCreator);
        boolean canMerge = canClose && isCollaborator;

        if (!canClose || isClosed) {
            menu.removeItem(R.id.pull_close);
        }
        if (!canOpen || !isClosed) {
            menu.removeItem(R.id.pull_reopen);
        } else if (isClosed && mPullRequest.isMerged()) {
            menu.findItem(R.id.pull_reopen).setEnabled(false);
        }
        if (!canMerge) {
            menu.removeItem(R.id.pull_merge);
        } else if (mPullRequest.isMerged() || !mPullRequest.isMergeable()) {
            MenuItem mergeItem = menu.findItem(R.id.pull_merge);
            mergeItem.setEnabled(false);
        }

        if (mPullRequest == null) {
            menu.removeItem(R.id.share);
            menu.removeItem(R.id.browser);
            menu.removeItem(R.id.copy_number);
        }
        if (!mPendingReviewLoaded || mPullRequest == null || isClosed) {
            menu.removeItem(R.id.pull_review);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean displayDetachAction() {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pull_merge:
                showMergeDialog();
                break;
            case R.id.pull_review:
                showReviewDialog();
                break;
            case R.id.pull_close:
            case R.id.pull_reopen:
                showOpenCloseConfirmDialog(item.getItemId() == R.id.pull_reopen);
                break;
            case R.id.share:
                IntentUtils.share(this, getString(R.string.share_pull_subject,
                        mPullRequest.getNumber(), mPullRequest.getTitle(),
                        mRepoOwner + "/" + mRepoName), mPullRequest.getHtmlUrl());
                break;
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(mPullRequest.getHtmlUrl()));
                break;
            case R.id.copy_number:
                IntentUtils.copyToClipboard(this, "Pull Request #" + mPullRequest.getNumber(),
                        String.valueOf(mPullRequest.getNumber()));
                return true;
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
        } else if (requestCode == REQUEST_CREATE_REVIEW) {
            if (resultCode == Activity.RESULT_OK) {
                if (mPullRequestFragment != null) {
                    mPullRequestFragment.reloadEvents(false);
                }
                // reload pending reviews
                getSupportLoaderManager().getLoader(3).onContentChanged();
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
        mInitialComment = extras.getParcelable("initial_comment");
        mInitialPage = extras.getInt("initial_page", -1);
        extras.remove("initial_comment");
        extras.remove("initial_page");
    }

    @Override
    public void onRefresh() {
        mIssue = null;
        mPullRequest = null;
        mIsCollaborator = null;
        mPendingReview = null;
        mPendingReviewLoaded = false;
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
        forceLoaderReload(0, 1, 2, 3);
        invalidateTabs();
        supportInvalidateOptionsMenu();
        super.onRefresh();
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mPullRequest != null && mIssue != null && mIsCollaborator != null ? TITLES : null;
    }

    @Override
    protected int[] getHeaderColorAttrs() {
        return mHeaderColorAttrs;
    }

    @Override
    protected Fragment makeFragment(int position) {
        if (position == 1) {
            PullRequestMarker base = mPullRequest.getBase();
            PullRequestMarker head = mPullRequest.getHead();
            return CommitCompareFragment.newInstance(mRepoOwner, mRepoName, mPullRequestNumber,
                    base.getLabel(), base.getSha(), head.getLabel(), head.getSha());
        } else if (position == 2) {
            return PullRequestFilesFragment.newInstance(mRepoOwner, mRepoName,
                    mPullRequestNumber, mPullRequest.getHead().getSha());
        } else {
            Fragment f = PullRequestFragment.newInstance(mPullRequest,
                    mIssue, mIsCollaborator, mInitialComment);
            mInitialComment = null;
            return f;
        }
    }

    @Override
    protected void onFragmentInstantiated(Fragment f, int position) {
        if (position == 0) {
            mPullRequestFragment = (PullRequestFragment) f;
        }
    }

    @Override
    protected void onFragmentDestroyed(Fragment f) {
        if (f == mPullRequestFragment) {
            mPullRequestFragment = null;
        }
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment object) {
        return true;
    }

    @Override
    protected Intent navigateUp() {
        return IssueListActivity.makeIntent(this, mRepoOwner, mRepoName, true);
    }

    @Override
    public void onCommentsUpdated() {
        if (mPullRequestFragment != null) {
            mPullRequestFragment.reloadEvents(true);
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

            if (mInitialPage >= 0 && mInitialPage < TITLES.length) {
                getPager().setCurrentItem(mInitialPage);
                mInitialPage = -1;
            }
        }
    }

    private void showOpenCloseConfirmDialog(final boolean reopen) {
        @StringRes int messageResId = reopen
                ? R.string.reopen_pull_request_confirm : R.string.close_pull_request_confirm;
        @StringRes int buttonResId = reopen
                ? R.string.pull_request_reopen : R.string.pull_request_close;
        new AlertDialog.Builder(this)
                .setMessage(messageResId)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(false)
                .setPositiveButton(buttonResId, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new PullRequestOpenCloseTask(reopen).schedule();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showMergeDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        String title = getString(R.string.pull_message_dialog_title, mPullRequest.getNumber());
        View view = inflater.inflate(R.layout.pull_merge_message_dialog, null);

        final View editorNotice = view.findViewById(R.id.notice);
        final EditText editor = view.findViewById(R.id.et_commit_message);
        editor.setText(mPullRequest.getTitle());

        final ArrayAdapter<MergeMethodDesc> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item);
        adapter.add(new MergeMethodDesc(R.string.pull_merge_method_merge,
                PullRequestService.MERGE_METHOD_MERGE));
        adapter.add(new MergeMethodDesc(R.string.pull_merge_method_squash,
                PullRequestService.MERGE_METHOD_SQUASH));
        adapter.add(new MergeMethodDesc(R.string.pull_merge_method_rebase,
                PullRequestService.MERGE_METHOD_REBASE));

        final Spinner mergeMethod = view.findViewById(R.id.merge_method);
        mergeMethod.setAdapter(adapter);
        mergeMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int editorVisibility = position == 2 ? View.GONE : View.VISIBLE;
                editorNotice.setVisibility(editorVisibility);
                editor.setVisibility(editorVisibility);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.pull_request_merge, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editor.getText() == null ? null : editor.getText().toString();
                        int methodIndex = mergeMethod.getSelectedItemPosition();
                        String method = adapter.getItem(methodIndex).action;
                        new PullRequestMergeTask(text, method).schedule();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showReviewDialog() {
        Intent intent = CreateReviewActivity.makeIntent(this, mRepoOwner, mRepoName,
                mPullRequestNumber, mPendingReview);
        startActivityForResult(intent, REQUEST_CREATE_REVIEW);
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
            adjustTabsForHeaderAlignedFab(true);
        } else if (!shouldHaveFab && mEditFab != null) {
            rootLayout.removeView(mEditFab);
            adjustTabsForHeaderAlignedFab(false);
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
            mHeaderColorAttrs = new int[] {
                R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark
            };
        } else if (ApiHelpers.IssueState.CLOSED.equals(mPullRequest.getState())) {
            stateTextResId = R.string.closed;
            mHeaderColorAttrs = new int[] {
                R.attr.colorIssueClosed, R.attr.colorIssueClosedDark
            };
        } else {
            stateTextResId = R.string.open;
            mHeaderColorAttrs = new int[] {
                R.attr.colorIssueOpen, R.attr.colorIssueOpenDark
            };
        }

        TextView tvState = mHeader.findViewById(R.id.tv_state);
        tvState.setText(getString(stateTextResId).toUpperCase(Locale.getDefault()));

        TextView tvTitle = mHeader.findViewById(R.id.tv_title);
        tvTitle.setText(mPullRequest.getTitle());

        mHeader.setVisibility(View.VISIBLE);
    }

    private void handlePullRequestUpdate() {
        if (mPullRequestFragment != null) {
            mPullRequestFragment.updateState(mPullRequest);
        }
        if (mIssue != null) {
            mIssue.setState(mPullRequest.getState());
            if (ApiHelpers.IssueState.CLOSED.equals(mIssue.getState())) {
                // if we came here, we either closed or merged the PR ourselves,
                // so set the 'closed by' field accordingly
                mIssue.setClosedBy(new User().setLogin(Gh4Application.get().getAuthLogin()));
            }
        }
        fillHeader();
        updateFabVisibility();
        transitionHeaderToColor(mHeaderColorAttrs[0], mHeaderColorAttrs[1]);
        supportInvalidateOptionsMenu();
    }

    private class PullRequestOpenCloseTask extends ProgressDialogTask<PullRequest> {
        private final boolean mOpen;

        public PullRequestOpenCloseTask(boolean open) {
            super(getBaseActivity(), open ? R.string.opening_msg : R.string.closing_msg);
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
        private final String mMergeMethod;

        public PullRequestMergeTask(String commitMessage, String mergeMethod) {
            super(getBaseActivity(), R.string.merging_msg);
            mCommitMessage = commitMessage;
            mMergeMethod = mergeMethod;
        }

        @Override
        protected ProgressDialogTask<MergeStatus> clone() {
            return new PullRequestMergeTask(mCommitMessage, mMergeMethod);
        }

        @Override
        protected MergeStatus run() throws Exception {
            PullRequestService pullService = (PullRequestService)
                    Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

            return pullService.merge(repoId, mPullRequest.getNumber(), mCommitMessage, mMergeMethod);
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
