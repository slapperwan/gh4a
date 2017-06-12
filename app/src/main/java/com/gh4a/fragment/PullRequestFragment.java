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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.loader.CommitStatusLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestCommentListLoader;
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
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullRequestFragment extends IssueFragmentBase {
    private PullRequest mPullRequest;

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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadStatusIfOpen();
    }

    @Override
    public void onRefresh() {
        if (mListHeaderView != null) {
            fillStatus(new ArrayList<CommitStatus>());
        }
        hideContentAndRestartLoaders(1);
        super.onRefresh();
    }

    @Override
    protected void bindSpecialViews(View headerView) {
        View branchGroup = headerView.findViewById(R.id.pr_container);
        branchGroup.setVisibility(View.VISIBLE);

        StyleableTextView fromBranch = (StyleableTextView) branchGroup.findViewById(R.id.tv_pr_from);
        formatMarkerText(fromBranch, R.string.pull_request_from, mPullRequest.getHead());

        StyleableTextView toBranch = (StyleableTextView) branchGroup.findViewById(R.id.tv_pr_to);
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
        SpannableStringBuilder builder = StringUtils.applyBoldTags(getActivity(),
                getString(formatResId), view.getTypefaceValue());
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
            getLoaderManager().initLoader(1, null, mStatusCallback);
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

        ImageView statusIcon = (ImageView) mListHeaderView.findViewById(R.id.iv_merge_status_icon);
        statusIcon.setImageResource(UiUtils.resolveDrawable(getActivity(),
                statusIconDrawableAttrId));

        TextView statusLabel = (TextView) mListHeaderView.findViewById(R.id.merge_status_label);
        statusLabel.setText(statusLabelResId);

        ViewGroup statusContainer = (ViewGroup)
                mListHeaderView.findViewById(R.id.merge_commit_status_container);
        LayoutInflater inflater = getLayoutInflater(null);
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
            ImageView icon = (ImageView) statusRow.findViewById(R.id.iv_status_icon);
            icon.setImageResource(UiUtils.resolveDrawable(getActivity(), iconDrawableAttrId));

            TextView context = (TextView) statusRow.findViewById(R.id.tv_context);
            context.setText(status.getContext());

            TextView description = (TextView) statusRow.findViewById(R.id.tv_desc);
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
        Intent intent = comment instanceof CommitComment
                ? EditPullRequestCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                        mPullRequest.getNumber(), (CommitComment) comment)
                : EditIssueCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                        mIssue.getNumber(), comment);
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
    public void replyToComment(long replyToId, String text) {
        // TODO
    }
}
