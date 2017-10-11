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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.annotation.AttrRes;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.loader.CommitStatusLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestCommentListLoader;
import com.gh4a.loader.ReferenceLoader;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.IntentSpan;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.TypedResource;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullRequestFragment extends IssueFragmentBase {
    private static final int ID_LOADER_STATUS = 1;
    private static final int ID_LOADER_HEAD_REF = 2;

    private PullRequest mPullRequest;
    private Reference mHeadReference;
    private boolean mHasLoadedHeadReference;

    private final LoaderCallbacks<List<CommitStatus>> mStatusCallback =
            new LoaderCallbacks<List<CommitStatus>>(this) {
        @Override
        protected Loader<LoaderResult<List<CommitStatus>>> onCreateLoader() {
            return new CommitStatusLoader(getActivity(), mRepoOwner, mRepoName,
                    mPullRequest.getHead().getSha());
        }

        @Override
        protected void onResultReady(List<CommitStatus> result) {
            fillStatus(result);
        }
    };

    private final LoaderCallbacks<Reference> mHeadReferenceCallback = new LoaderCallbacks<Reference>(this) {
        @Override
        protected Loader<LoaderResult<Reference>> onCreateLoader() {
            return new ReferenceLoader(getActivity(), mPullRequest);
        }

        @Override
        protected void onResultReady(Reference result) {
            mHeadReference = result;
            mHasLoadedHeadReference = true;
            getActivity().invalidateOptionsMenu();
            getLoaderManager().destroyLoader(ID_LOADER_HEAD_REF);
        }
    };

    public static PullRequestFragment newInstance(PullRequest pr, Issue issue,
            boolean isCollaborator, IntentUtils.InitialCommentMarker initialComment) {
        PullRequestFragment f = new PullRequestFragment();

        Repository repo = pr.getBase().getRepo();
        Bundle args = buildArgs(repo.getOwner().getLogin(), repo.getName(),
                issue, isCollaborator, initialComment);
        args.putSerializable("pr", pr);
        f.setArguments(args);

        return f;
    }

    public void updateState(PullRequest pr) {
        mIssue.setState(pr.getState());
        mPullRequest.setState(pr.getState());
        mPullRequest.setMerged(pr.isMerged());

        assignHighlightColor();
        loadStatusIfOpen();
        reloadEvents(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mPullRequest = (PullRequest) getArguments().getSerializable("pr");
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

        if (mPullRequest == null || mPullRequest.getHead().getRepo() == null) {
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
            fillStatus(new ArrayList<CommitStatus>());
        }
        hideContentAndRestartLoaders(ID_LOADER_STATUS, ID_LOADER_HEAD_REF);
        super.onRefresh();
    }

    @Override
    protected void bindSpecialViews(View headerView) {
        View branchGroup = headerView.findViewById(R.id.pr_container);
        branchGroup.setVisibility(View.VISIBLE);

        StyleableTextView fromBranch = branchGroup.findViewById(R.id.tv_pr_from);
        formatMarkerText(fromBranch, R.string.pull_request_from, mPullRequest.getHead());

        StyleableTextView toBranch = branchGroup.findViewById(R.id.tv_pr_to);
        formatMarkerText(toBranch, R.string.pull_request_to, mPullRequest.getBase());
    }

    @Override
    protected void assignHighlightColor() {
        if (mPullRequest.isMerged()) {
            setHighlightColors(R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark);
        } else if (ApiHelpers.IssueState.CLOSED.equals(mPullRequest.getState())) {
            setHighlightColors(R.attr.colorIssueClosed, R.attr.colorIssueClosedDark);
        } else {
            setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
        }
    }

    private void formatMarkerText(StyleableTextView view,
            @StringRes int formatResId, final PullRequestMarker marker) {
        SpannableStringBuilder builder = StringUtils.applyBoldTags(getString(formatResId),
                view.getTypefaceValue());
        int pos = builder.toString().indexOf("[ref]");
        if (pos >= 0) {
            String label = TextUtils.isEmpty(marker.getLabel()) ? marker.getRef() : marker.getLabel();
            final Repository repo = marker.getRepo();
            builder.replace(pos, pos + 5, label);
            if (repo != null) {
                builder.setSpan(new IntentSpan(getActivity()) {
                    @Override
                    protected Intent getIntent() {
                        return RepositoryActivity.makeIntent(getActivity(), repo, marker.getRef());
                    }
                }, pos, pos + label.length(), 0);
            }
        }

        view.setText(builder);
        view.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
    }

    private void loadStatusIfOpen() {
        if (ApiHelpers.IssueState.OPEN.equals(mPullRequest.getState())) {
            getLoaderManager().initLoader(ID_LOADER_STATUS, null, mStatusCallback);
        }
   }

   private void fillStatus(List<CommitStatus> statuses) {
        Map<String, CommitStatus> statusByContext = new HashMap<>();
        for (CommitStatus status : statuses) {
            if (!statusByContext.containsKey(status.getContext())) {
                statusByContext.put(status.getContext(), status);
            }
        }

        final int statusIconDrawableAttrId, statusLabelResId;
        if (PullRequest.MERGEABLE_STATE_CLEAN.equals(mPullRequest.getMergeableState())) {
            statusIconDrawableAttrId = R.attr.pullRequestMergeOkIcon;
            statusLabelResId = R.string.pull_merge_status_clean;
        } else if (PullRequest.MERGEABLE_STATE_UNSTABLE.equals(mPullRequest.getMergeableState())) {
            statusIconDrawableAttrId = R.attr.pullRequestMergeUnstableIcon;
            statusLabelResId = R.string.pull_merge_status_unstable;
        } else if (PullRequest.MERGEABLE_STATE_DIRTY.equals(mPullRequest.getMergeableState())) {
            statusIconDrawableAttrId = R.attr.pullRequestMergeDirtyIcon;
            statusLabelResId = R.string.pull_merge_status_dirty;
        } else if (statusByContext.isEmpty()) {
            // unknwon status, no commit statuses -> nothing to display
            return;
        } else {
            statusIconDrawableAttrId = R.attr.pullRequestMergeUnknownIcon;
            statusLabelResId = R.string.pull_merge_status_unknown;
        }

        ImageView statusIcon = mListHeaderView.findViewById(R.id.iv_merge_status_icon);
        statusIcon.setImageResource(UiUtils.resolveDrawable(getActivity(),
                statusIconDrawableAttrId));

        TextView statusLabel = mListHeaderView.findViewById(R.id.merge_status_label);
        statusLabel.setText(statusLabelResId);

        ViewGroup statusContainer =
                mListHeaderView.findViewById(R.id.merge_commit_status_container);
        LayoutInflater inflater = getLayoutInflater();
        statusContainer.removeAllViews();
        for (CommitStatus status : statusByContext.values()) {
            View statusRow = inflater.inflate(R.layout.row_commit_status, statusContainer, false);

            String state = status.getState();
            final int iconDrawableAttrId;
            if (CommitStatus.STATE_ERROR.equals(state) || CommitStatus.STATE_FAILURE.equals(state)) {
                iconDrawableAttrId = R.attr.commitStatusFailIcon;
            } else if (CommitStatus.STATE_SUCCESS.equals(state)) {
                iconDrawableAttrId = R.attr.commitStatusOkIcon;
            } else {
                iconDrawableAttrId = R.attr.commitStatusUnknownIcon;
            }
            ImageView icon = statusRow.findViewById(R.id.iv_status_icon);
            icon.setImageResource(UiUtils.resolveDrawable(getActivity(), iconDrawableAttrId));

            TextView context = statusRow.findViewById(R.id.tv_context);
            context.setText(status.getContext());

            TextView description = statusRow.findViewById(R.id.tv_desc);
            description.setText(status.getDescription());

            statusContainer.addView(statusRow);
        }
        mListHeaderView.findViewById(R.id.merge_commit_no_status).setVisibility(
                statusByContext.isEmpty() ? View.VISIBLE : View.GONE);

        mListHeaderView.findViewById(R.id.merge_status_container).setVisibility(View.VISIBLE);
    }

    @Override
    public Loader<LoaderResult<List<TimelineItem>>> onCreateLoader() {
        return new PullRequestCommentListLoader(getActivity(),
                mRepoOwner, mRepoName, mPullRequest.getNumber());
    }

    @Override
    public void editComment(Comment comment) {
        final @AttrRes int highlightColorAttr = mPullRequest.isMerged()
                ? R.attr.colorPullRequestMerged
                : ApiHelpers.IssueState.CLOSED.equals(mPullRequest.getState())
                        ? R.attr.colorIssueClosed : R.attr.colorIssueOpen;
        Intent intent = comment instanceof CommitComment
                ? EditPullRequestCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                        mPullRequest.getNumber(), 0L, (CommitComment) comment, highlightColorAttr)
                : EditIssueCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                        mIssue.getNumber(), comment, highlightColorAttr);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    protected void deleteCommentInBackground(RepositoryId repoId, Comment comment) throws Exception {
        Gh4Application app = Gh4Application.get();

        if (comment instanceof CommitComment) {
            PullRequestService pullService =
                    (PullRequestService) app.getService(Gh4Application.PULL_SERVICE);
            pullService.deleteComment(repoId, comment.getId());
        } else {
            IssueService issueService = (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);
            issueService.deleteComment(repoId, comment.getId());
        }
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.pull_request_comment_hint;
    }

    @Override
    public void replyToComment(long replyToId) {
        // Not used in this screen
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

    private class RestoreBranchTask extends ProgressDialogTask<Reference> {
        public RestoreBranchTask() {
            super(getBaseActivity(), R.string.saving_msg);
        }

        @Override
        protected ProgressDialogTask<Reference> clone() {
            return new RestoreBranchTask();
        }

        @Override
        protected Reference run() throws Exception {
            DataService dataService =
                    (DataService) Gh4Application.get().getService(Gh4Application.DATA_SERVICE);

            PullRequestMarker head = mPullRequest.getHead();
            if (head.getRepo() == null) {
                return null;
            }
            String owner = head.getRepo().getOwner().getLogin();
            String repo = head.getRepo().getName();
            RepositoryId repoId = new RepositoryId(owner, repo);

            Reference reference = new Reference();
            reference.setRef("refs/heads/" + head.getRef());
            TypedResource object = new TypedResource();
            object.setSha(head.getSha());
            reference.setObject(object);

            return dataService.createReference(repoId, reference);
        }

        @Override
        protected void onSuccess(Reference result) {
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
            DataService dataService =
                    (DataService) Gh4Application.get().getService(Gh4Application.DATA_SERVICE);

            PullRequestMarker head = mPullRequest.getHead();
            String owner = head.getRepo().getOwner().getLogin();
            String repo = head.getRepo().getName();
            RepositoryId repoId = new RepositoryId(owner, repo);

            Reference reference = new Reference();
            reference.setRef("heads/" + head.getRef());

            dataService.deleteReference(repoId, reference);
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
