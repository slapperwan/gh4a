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
package com.gh4a.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.AttrRes;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.loader.CommitStatusLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestCommentListLoader;
import com.gh4a.loader.ReferenceLoader;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.widget.PullRequestBranchInfoView;
import com.gh4a.widget.CommitStatusBox;

import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.model.PullRequestMarker;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.model.Status;
import com.meisolsson.githubsdk.model.git.GitReference;
import com.meisolsson.githubsdk.model.request.git.CreateGitReference;
import com.meisolsson.githubsdk.service.git.GitService;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import retrofit2.Response;

public class PullRequestFragment extends IssueFragmentBase {
    private static final int ID_LOADER_STATUS = 1;
    private static final int ID_LOADER_HEAD_REF = 2;

    private PullRequest mPullRequest;
    private GitReference mHeadReference;
    private boolean mHasLoadedHeadReference;

    private final LoaderCallbacks<List<Status>> mStatusCallback =
            new LoaderCallbacks<List<Status>>(this) {
        @Override
        protected Loader<LoaderResult<List<Status>>> onCreateLoader() {
            return new CommitStatusLoader(getActivity(), mRepoOwner, mRepoName,
                    mPullRequest.head().sha());
        }

        @Override
        protected void onResultReady(List<Status> result) {
            fillStatus(result);
        }
    };

    private final LoaderCallbacks<GitReference> mHeadReferenceCallback = new LoaderCallbacks<GitReference>(this) {
        @Override
        protected Loader<LoaderResult<GitReference>> onCreateLoader() {
            return new ReferenceLoader(getActivity(), mPullRequest);
        }

        @Override
        protected void onResultReady(GitReference result) {
            mHeadReference = result;
            mHasLoadedHeadReference = true;
            getActivity().invalidateOptionsMenu();
            bindSpecialViews(mListHeaderView);
            getLoaderManager().destroyLoader(ID_LOADER_HEAD_REF);
        }
    };

    public static PullRequestFragment newInstance(PullRequest pr, Issue issue,
            boolean isCollaborator, IntentUtils.InitialCommentMarker initialComment) {
        PullRequestFragment f = new PullRequestFragment();

        Repository repo = pr.base().repo();
        Bundle args = buildArgs(repo.owner().login(), repo.name(),
                issue, isCollaborator, initialComment);
        args.putParcelable("pr", pr);
        f.setArguments(args);

        return f;
    }

    public void updateState(PullRequest pr) {
        mIssue = mIssue.toBuilder().state(pr.state()).build();
        mPullRequest = mPullRequest.toBuilder()
                .state(pr.state())
                .merged(pr.merged())
                .build();

        assignHighlightColor();
        loadStatusIfOpen();
        reloadEvents(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mPullRequest = getArguments().getParcelable("pr");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getLoaderManager().initLoader(ID_LOADER_HEAD_REF, null, mHeadReferenceCallback);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadStatusIfOpen();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.pull_request_fragment_menu, menu);

        if (mPullRequest == null || mPullRequest.head().repo() == null
                || mPullRequest.state() == IssueState.Open) {
            menu.removeItem(R.id.delete_branch);
        } else {
            MenuItem deleteBranchItem = menu.findItem(R.id.delete_branch);
            deleteBranchItem.setVisible(mHasLoadedHeadReference);
            if (mHeadReference == null) {
                deleteBranchItem.setTitle(R.string.restore_branch);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_branch:
                showDeleteRestoreBranchConfirmDialog(mHeadReference == null);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        mHeadReference = null;
        mHasLoadedHeadReference = false;
        if (mListHeaderView != null) {
            fillStatus(new ArrayList<Status>());
        }
        hideContentAndRestartLoaders(ID_LOADER_STATUS, ID_LOADER_HEAD_REF);
        super.onRefresh();
    }

    @Override
    protected void bindSpecialViews(View headerView) {
        if (!mHasLoadedHeadReference) {
            return;
        }

        PullRequestBranchInfoView branchContainer = headerView.findViewById(R.id.branch_container);
        branchContainer.bind(mPullRequest.head(), mPullRequest.base(), mHeadReference);
        branchContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void assignHighlightColor() {
        if (mPullRequest.merged()) {
            setHighlightColors(R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark);
        } else if (mPullRequest.state() == IssueState.Closed) {
            setHighlightColors(R.attr.colorIssueClosed, R.attr.colorIssueClosedDark);
        } else {
            setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
        }
    }

    private void loadStatusIfOpen() {
        if (mPullRequest.state() == IssueState.Open) {
            getLoaderManager().initLoader(ID_LOADER_STATUS, null, mStatusCallback);
        }
   }

   private void fillStatus(List<Status> statuses) {
       CommitStatusBox commitStatusBox = mListHeaderView.findViewById(R.id.commit_status_box);
       commitStatusBox.fillStatus(statuses, mPullRequest.mergeableState());
   }

    @Override
    public Loader<LoaderResult<List<TimelineItem>>> onCreateLoader() {
        return new PullRequestCommentListLoader(getActivity(),
                mRepoOwner, mRepoName, mPullRequest.number());
    }

    @Override
    public void editComment(GitHubCommentBase comment) {
        final @AttrRes int highlightColorAttr = mPullRequest.merged()
                ? R.attr.colorPullRequestMerged
                : mPullRequest.state() == IssueState.Closed
                        ? R.attr.colorIssueClosed : R.attr.colorIssueOpen;
        Intent intent = comment instanceof ReviewComment
                ? EditPullRequestCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                mPullRequest.number(), comment.id(), 0L, comment.body(), highlightColorAttr)
                : EditIssueCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                        mIssue.number(), comment.id(), comment.body(), highlightColorAttr);
        startActivityForResult(intent, REQUEST_EDIT);
    }


    @Override
    protected Single<Response<Void>> doDeleteComment(GitHubCommentBase comment) {
        if (comment instanceof ReviewComment) {
            PullRequestReviewCommentService service =
                    Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
            return service.deleteComment(mRepoOwner, mRepoName, comment.id());
        } else {
            IssueCommentService service =
                    Gh4Application.get().getGitHubService(IssueCommentService.class);
            return service.deleteIssueComment(mRepoOwner, mRepoName, comment.id());
        }
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.pull_request_comment_hint;
    }

    private void showDeleteRestoreBranchConfirmDialog(final boolean restore) {
        int message = restore ? R.string.restore_branch_question : R.string.delete_branch_question;
        int buttonText = restore ? R.string.restore : R.string.delete;
        new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (restore) {
                            new RestoreBranchTask().schedule();
                        } else {
                            new DeleteBranchTask().schedule();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private class RestoreBranchTask extends ProgressDialogTask<GitReference> {
        public RestoreBranchTask() {
            super(getBaseActivity(), R.string.saving_msg);
        }

        @Override
        protected ProgressDialogTask<GitReference> clone() {
            return new RestoreBranchTask();
        }

        @Override
        protected GitReference run() throws Exception {
            GitService service = Gh4Application.get().getGitHubService(GitService.class);

            PullRequestMarker head = mPullRequest.head();
            if (head.repo() == null) {
                return null;
            }
            String owner = head.repo().owner().login();
            String repo = head.repo().name();

            CreateGitReference request = CreateGitReference.builder()
                    .ref(head.ref())
                    .sha(head.sha())
                    .build();

            return ApiHelpers.throwOnFailure(
                    service.createGitReference(owner, repo, request).blockingGet());
        }

        @Override
        protected void onSuccess(GitReference result) {
            mHeadReference = result;
            onHeadReferenceUpdated();
        }

        @Override
        protected String getErrorMessage() {
            return getString(R.string.restore_branch_error);
        }
    }

    private void onHeadReferenceUpdated() {
        getActivity().invalidateOptionsMenu();
        reloadEvents(false);
    }

    private class DeleteBranchTask extends ProgressDialogTask<Void> {
        public DeleteBranchTask() {
            super(getBaseActivity(), R.string.deleting_msg);
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new DeleteBranchTask();
        }

        @Override
        protected Void run() throws Exception {
            GitService service = Gh4Application.get().getGitHubService(GitService.class);

            PullRequestMarker head = mPullRequest.head();
            String owner = head.repo().owner().login();
            String repo = head.repo().name();

            ApiHelpers.throwOnFailure(
                    service.deleteGitReference(owner, repo, head.ref()).blockingGet());
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mHeadReference = null;
            onHeadReferenceUpdated();
        }

        @Override
        protected String getErrorMessage() {
            return getString(R.string.delete_branch_error);
        }
    }
}
