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
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.gh4a.utils.ActivityResultHelpers;
import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
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
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.fragment.CommitCompareFragment;
import com.gh4a.fragment.ConfirmationDialogFragment;
import com.gh4a.fragment.PullRequestFilesFragment;
import com.gh4a.fragment.PullRequestFragment;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.SingleFactory;
import com.gh4a.utils.Triplet;
import com.gh4a.widget.BottomSheetCompatibleScrollingViewBehavior;
import com.gh4a.widget.IssueStateTrackingFloatingActionButton;

import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.model.PullRequestMarker;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewState;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.request.pull_request.EditPullRequest;
import com.meisolsson.githubsdk.model.request.pull_request.MergeRequest;
import com.meisolsson.githubsdk.service.issues.IssueService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import java.util.Locale;

import io.reactivex.Single;

public class PullRequestActivity extends BaseFragmentPagerActivity implements
        View.OnClickListener, ConfirmationDialogFragment.Callback,
        PullRequestFilesFragment.CommentUpdateListener {
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

    private final ActivityResultLauncher<Intent> mEditIssueLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> {
                setResult(Activity.RESULT_OK);
                onRefresh();
            })
    );

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

    private final ActivityResultLauncher<Intent> mCreateReviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> {
                if (mPullRequestFragment != null) {
                    mPullRequestFragment.reloadEvents(false);
                }
                loadPendingReview(true);
            })
    );

    private static final int[] TITLES = new int[]{
            R.string.pull_request_conversation, R.string.commits, R.string.pull_request_files
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater =
                LayoutInflater.from(new ContextThemeWrapper(this, R.style.HeaderTheme));
        mHeader = (ViewGroup) inflater.inflate(R.layout.issue_header, null);
        mHeader.setClickable(false);
        mHeader.setVisibility(View.GONE);
        addHeaderView(mHeader, !hasTabsInToolbar());

        setContentShown(false);
        load(false);
        loadPendingReview(false);
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
                && ApiHelpers.loginEquals(mPullRequest.user(), app.getAuthLogin());
        boolean isClosed = mPullRequest != null && mPullRequest.state() == IssueState.Closed;
        boolean isCollaborator = mIsCollaborator != null && mIsCollaborator;
        boolean closerIsCreator = mIssue != null
                && ApiHelpers.userEquals(mIssue.user(), mIssue.closedBy());
        boolean canClose = mPullRequest != null && authorized && (isCreator || isCollaborator);
        boolean canOpen = canClose && (isCollaborator || closerIsCreator);
        boolean canMerge = canClose && isCollaborator;

        if (!canClose || isClosed) {
            menu.removeItem(R.id.pull_close);
        }
        if (!canOpen || !isClosed) {
            menu.removeItem(R.id.pull_reopen);
        } else if (isClosed && mPullRequest.merged()) {
            menu.findItem(R.id.pull_reopen).setEnabled(false);
        }
        if (!canMerge) {
            menu.removeItem(R.id.pull_merge);
        } else if (mPullRequest.merged()
                || mPullRequest.mergeable() == null || !mPullRequest.mergeable()) {
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
                MergeDialogFragment.newInstance(mPullRequest)
                        .show(getSupportFragmentManager(), "mergemethod");
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
                        mPullRequest.number(), mPullRequest.title(),
                        mRepoOwner + "/" + mRepoName), Uri.parse(mPullRequest.htmlUrl()));
                break;
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(mPullRequest.htmlUrl()));
                break;
            case R.id.copy_number:
                IntentUtils.copyToClipboard(this, "Pull Request #" + mPullRequest.number(),
                        String.valueOf(mPullRequest.number()));
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        setContentShown(false);
        if (mEditFab != null) {
            mEditFab.post(this::updateFabVisibility);
        }
        mHeader.setVisibility(View.GONE);
        mHeaderColorAttrs = null;
        load(true);
        loadPendingReview(true);
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
            PullRequestMarker base = mPullRequest.base();
            PullRequestMarker head = mPullRequest.head();
            return CommitCompareFragment.newInstance(mRepoOwner, mRepoName, mPullRequestNumber,
                    base.label(), base.sha(), head.label(), head.sha());
        } else if (position == 2) {
            return PullRequestFilesFragment.newInstance(mRepoOwner, mRepoName,
                    mPullRequestNumber, mPullRequest.head().sha());
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
            mEditIssueLauncher.launch(editIntent);
        } else if (v.getId() == R.id.iv_gravatar) {
            Intent intent = UserActivity.makeIntent(this, (User) v.getTag());
            if (intent != null) {
                startActivity(intent);
            }
        }
    }

    @Override
    public void onConfirmed(String tag, Parcelable data) {
        boolean reopen = ((Bundle) data).getBoolean("reopen");
        updatePullRequestState(reopen);
    }

    private void showOpenCloseConfirmDialog(final boolean reopen) {
        @StringRes int messageResId = reopen
                ? R.string.reopen_pull_request_confirm : R.string.close_pull_request_confirm;
        @StringRes int buttonResId = reopen
                ? R.string.pull_request_reopen : R.string.pull_request_close;
        Bundle data = new Bundle();
        data.putBoolean("reopen", reopen);

        ConfirmationDialogFragment.show(this, messageResId, buttonResId, true, data, "opencloseconfirm");
    }

    private void showReviewDialog() {
        Intent intent = CreateReviewActivity.makeIntent(this, mRepoOwner, mRepoName,
                mPullRequestNumber, mPendingReview);
        mCreateReviewLauncher.launch(intent);
    }

    private void updateFabVisibility() {
        boolean isIssueOwner = mIssue != null
                && ApiHelpers.loginEquals(mIssue.user(), Gh4Application.get().getAuthLogin());
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
            mEditFab.setState(mPullRequest.state());
            mEditFab.setMerged(mPullRequest.merged());
        }
    }

    private void fillHeader() {
        final int stateTextResId;

        if (mPullRequest.merged()) {
            stateTextResId = R.string.pull_request_merged;
            mHeaderColorAttrs = new int[] {
                R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark
            };
        } else if (mPullRequest.state() == IssueState.Closed) {
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
        tvTitle.setText(mPullRequest.title());

        mHeader.setVisibility(View.VISIBLE);
    }

    private void handlePullRequestUpdate() {
        if (mPullRequestFragment != null) {
            mPullRequestFragment.updateState(mPullRequest);
        }
        if (mIssue != null) {
            Issue.Builder builder = mIssue.toBuilder().state(mPullRequest.state());
            if (mPullRequest.state() == IssueState.Closed) {
                // if we came here, we either closed or merged the PR ourselves,
                // so set the 'closed by' field accordingly
                builder.closedBy(Gh4Application.get().getCurrentAccountInfoForAvatar());
            }
            mIssue = builder.build();
        }
        fillHeader();
        updateFabVisibility();
        transitionHeaderToColor(mHeaderColorAttrs[0], mHeaderColorAttrs[1]);
        supportInvalidateOptionsMenu();
    }

    private void updatePullRequestState(boolean open) {
        @StringRes int dialogMessageResId = open ? R.string.opening_msg : R.string.closing_msg;
        @StringRes int errorMessageResId = open ? R.string.issue_error_reopen : R.string.issue_error_close;
        String errorMessage = getString(errorMessageResId, mPullRequest.number());

        PullRequestService service = ServiceFactory.get(PullRequestService.class, false);
        EditPullRequest request = EditPullRequest.builder()
                .state(open ? ApiHelpers.IssueState.OPEN : ApiHelpers.IssueState.CLOSED)
                .build();

        service.editPullRequest(mRepoOwner, mRepoName, mPullRequestNumber, request)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, dialogMessageResId, errorMessage))
                .subscribe(result -> {
                    mPullRequest = result;
                    handlePullRequestUpdate();
                }, error -> handleActionFailure("Updating pull request failed", error));
    }

    private void mergePullRequest(String commitMessage, MergeRequest.Method mergeMethod) {
        String errorMessage = getString(R.string.pull_error_merge, mPullRequest.number());
        PullRequestService service = ServiceFactory.get(PullRequestService.class, false);
        MergeRequest request = MergeRequest.builder()
                .commitMessage(commitMessage)
                .method(mergeMethod)
                .build();

        service.mergePullRequest(mRepoOwner, mRepoName, mPullRequestNumber, request)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(this, R.string.merging_msg, errorMessage))
                .subscribe(result -> {
                    if (result.merged()) {
                        mPullRequest = mPullRequest.toBuilder()
                                .merged(true)
                                .state(IssueState.Closed)
                                .build();
                    }
                    handlePullRequestUpdate();
                }, error -> handleActionFailure("Merging pull request failed", error));
    }

    private void load(boolean force) {
        PullRequestService prService = ServiceFactory.get(PullRequestService.class, force);
        IssueService issueService = ServiceFactory.get(IssueService.class, force);

        Single<PullRequest> prSingle = prService.getPullRequest(mRepoOwner, mRepoName, mPullRequestNumber)
                .map(ApiHelpers::throwOnFailure);
        Single<Issue> issueSingle = issueService.getIssue(mRepoOwner, mRepoName, mPullRequestNumber)
                .map(ApiHelpers::throwOnFailure);
        Single<Boolean> isCollaboratorSingle =
                SingleFactory.isAppUserRepoCollaborator(mRepoOwner, mRepoName, force);

        Single.zip(issueSingle, prSingle, isCollaboratorSingle, Triplet::create)
                .compose(makeLoaderSingle(0, force))
                .subscribe(result -> {
                    mIssue = result.first;
                    mPullRequest = result.second;
                    mIsCollaborator = result.third;
                    fillHeader();
                    setContentShown(true);
                    invalidateTabs();
                    updateFabVisibility();
                    supportInvalidateOptionsMenu();

                    if (mInitialPage >= 0 && mInitialPage < TITLES.length) {
                        getPager().setCurrentItem(mInitialPage);
                        mInitialPage = -1;
                    }
                }, this::handleLoadFailure);
    }

    private void loadPendingReview(boolean force) {
        String ownLogin = Gh4Application.get().getAuthLogin();
        PullRequestReviewService service = ServiceFactory.get(PullRequestReviewService.class, force);

        ApiHelpers.PageIterator
                .toSingle(page -> service.getReviews(mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(RxUtils.filterAndMapToFirst(r -> {
                    return r.state() == ReviewState.Pending
                            && ApiHelpers.loginEquals(r.user(), ownLogin);
                }))
                .compose(makeLoaderSingle(1, force))
                .doOnSubscribe(disposable -> {
                    mPendingReviewLoaded = false;
                    supportInvalidateOptionsMenu();
                })
                .subscribe(result -> {
                    mPendingReview = result.orNull();
                    mPendingReviewLoaded = true;
                    supportInvalidateOptionsMenu();
                }, this::handleLoadFailure);
    }

    private static class MergeMethodDesc {
        final String text;
        final MergeRequest.Method action;

        public MergeMethodDesc(String text, MergeRequest.Method action) {
            this.text = text;
            this.action = action;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public static class MergeDialogFragment extends DialogFragment {
        public static MergeDialogFragment newInstance(PullRequest pr) {
            MergeDialogFragment f = new MergeDialogFragment();
            Bundle args = new Bundle();
            args.putParcelable("pr", pr);
            f.setArguments(args);
            return f;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            final PullRequest pr = getArguments().getParcelable("pr");
            String title = getString(R.string.pull_message_dialog_title, pr.number());
            View view = inflater.inflate(R.layout.pull_merge_message_dialog, null);

            final View editorNotice = view.findViewById(R.id.notice);
            final EditText editor = view.findViewById(R.id.et_commit_message);
            editor.setText(pr.title());

            final ArrayAdapter<MergeMethodDesc> adapter =
                    new ArrayAdapter<>(getContext(), R.layout.spinner_item);
            adapter.add(new MergeMethodDesc(
                    getString(R.string.pull_merge_method_merge), MergeRequest.Method.Merge));
            adapter.add(new MergeMethodDesc(
                    getString(R.string.pull_merge_method_squash), MergeRequest.Method.Squash));
            adapter.add(new MergeMethodDesc(
                    getString(R.string.pull_merge_method_rebase), MergeRequest.Method.Rebase));

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

            final PullRequestActivity activity = (PullRequestActivity) getContext();
            return new AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setView(view)
                    .setPositiveButton(R.string.pull_request_merge, (dialog, which) -> {
                        String text = editor.getText() == null ? null : editor.getText().toString();
                        int methodIndex = mergeMethod.getSelectedItemPosition();

                        activity.mergePullRequest(text, adapter.getItem(methodIndex).action);
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .create();
        }
    }
}
