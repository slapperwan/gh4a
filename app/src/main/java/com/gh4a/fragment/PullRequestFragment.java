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
import android.support.annotation.AttrRes;
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
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.model.PullRequestMarker;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.model.Status;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class PullRequestFragment extends IssueFragmentBase {
    private PullRequest mPullRequest;

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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadStatusIfOpen();
    }

    @Override
    public void onRefresh() {
        if (mListHeaderView != null) {
            fillStatus(new ArrayList<Status>());
        }
        hideContentAndRestartLoaders(1);
        super.onRefresh();
    }

    @Override
    protected void bindSpecialViews(View headerView) {
        View branchGroup = headerView.findViewById(R.id.pr_container);
        branchGroup.setVisibility(View.VISIBLE);

        StyleableTextView fromBranch = branchGroup.findViewById(R.id.tv_pr_from);
        formatMarkerText(fromBranch, R.string.pull_request_from, mPullRequest.head());

        StyleableTextView toBranch = branchGroup.findViewById(R.id.tv_pr_to);
        formatMarkerText(toBranch, R.string.pull_request_to, mPullRequest.base());
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

    private void formatMarkerText(StyleableTextView view,
            @StringRes int formatResId, final PullRequestMarker marker) {
        SpannableStringBuilder builder = StringUtils.applyBoldTags(getString(formatResId),
                view.getTypefaceValue());
        int pos = builder.toString().indexOf("[ref]");
        if (pos >= 0) {
            String label = TextUtils.isEmpty(marker.label()) ? marker.ref() : marker.label();
            final Repository repo = marker.repo();
            builder.replace(pos, pos + 5, label);
            if (repo != null) {
                builder.setSpan(new IntentSpan(getActivity()) {
                    @Override
                    protected Intent getIntent() {
                        return RepositoryActivity.makeIntent(getActivity(), repo, marker.ref());
                    }
                }, pos, pos + label.length(), 0);
            }
        }

        view.setText(builder);
        view.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
    }

    private void loadStatusIfOpen() {
        if (mPullRequest.state() == IssueState.Open) {
            getLoaderManager().initLoader(1, null, mStatusCallback);
        }
   }

   private void fillStatus(List<Status> statuses) {
       Map<String, Status> statusByContext = new HashMap<>();
       for (Status status : statuses) {
           if (!statusByContext.containsKey(status.context())) {
               statusByContext.put(status.context(), status);
           }
       }

       final int statusIconDrawableAttrId, statusLabelResId;
       switch (mPullRequest.mergeableState()) {
           case Clean:
               statusIconDrawableAttrId = R.attr.pullRequestMergeOkIcon;
               statusLabelResId = R.string.pull_merge_status_clean;
               break;
           case Unstable:
               statusIconDrawableAttrId = R.attr.pullRequestMergeUnstableIcon;
               statusLabelResId = R.string.pull_merge_status_unstable;
               break;
           case Dirty:
               statusIconDrawableAttrId = R.attr.pullRequestMergeDirtyIcon;
               statusLabelResId = R.string.pull_merge_status_dirty;
               break;
           default:
               if (statusByContext.isEmpty()) {
                   // unknwon status, no commit statuses -> nothing to display
                   return;
               } else {
                   statusIconDrawableAttrId = R.attr.pullRequestMergeUnknownIcon;
                   statusLabelResId = R.string.pull_merge_status_unknown;
               }
               break;
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
       for (Status status : statusByContext.values()) {
           View statusRow = inflater.inflate(R.layout.row_commit_status, statusContainer, false);

           final int iconDrawableAttrId;
           switch (status.state()) {
               case Error:
               case Failure:
                   iconDrawableAttrId = R.attr.commitStatusFailIcon;
                   break;
               case Success:
                   iconDrawableAttrId = R.attr.commitStatusOkIcon;
                   break;
               default:
                   iconDrawableAttrId = R.attr.commitStatusUnknownIcon;
           }
           ImageView icon = statusRow.findViewById(R.id.iv_status_icon);
           icon.setImageResource(UiUtils.resolveDrawable(getActivity(), iconDrawableAttrId));

           TextView context = statusRow.findViewById(R.id.tv_context);
           context.setText(status.context());

           TextView description = statusRow.findViewById(R.id.tv_desc);
           description.setText(status.description());

           statusContainer.addView(statusRow);
       }
       mListHeaderView.findViewById(R.id.merge_commit_no_status).setVisibility(
               statusByContext.isEmpty() ? View.VISIBLE : View.GONE);

       mListHeaderView.findViewById(R.id.merge_status_container).setVisibility(View.VISIBLE);
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
    protected void deleteCommentInBackground(GitHubCommentBase comment) throws Exception {
        final Response<Boolean> response;

        if (comment instanceof ReviewComment) {
            PullRequestReviewCommentService service =
                    Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
            response = service.deleteComment(mRepoOwner, mRepoName, comment.id()).blockingGet();
        } else {
            IssueCommentService service =
                    Gh4Application.get().getGitHubService(IssueCommentService.class);
            response = service.deleteIssueComment(mRepoOwner, mRepoName, comment.id()).blockingGet();
        }
        ApiHelpers.throwOnFailure(response);
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.pull_request_comment_hint;
    }

    @Override
    public void replyToComment(long replyToId) {
        // Not used in this screen
    }
}
