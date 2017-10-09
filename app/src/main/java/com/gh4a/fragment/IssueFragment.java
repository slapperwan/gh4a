package com.gh4a.fragment;

import android.content.Intent;
import android.support.annotation.AttrRes;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.loader.IssueCommentListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;

import java.util.List;

import io.reactivex.Single;
import retrofit2.Response;

public class IssueFragment extends IssueFragmentBase {
    public static IssueFragment newInstance(String repoOwner, String repoName, Issue issue,
            boolean isCollaborator, IntentUtils.InitialCommentMarker initialComment) {
        IssueFragment f = new IssueFragment();
        f.setArguments(buildArgs(repoOwner, repoName, issue, isCollaborator, initialComment));
        return f;
    }

    public void updateState(Issue issue) {
        mIssue = mIssue.toBuilder().state(issue.state()).build();
        assignHighlightColor();
        reloadEvents(false);
    }

    @Override
    protected void bindSpecialViews(View headerView) {
        TextView tvPull = headerView.findViewById(R.id.tv_pull);
        if (mIssue.pullRequest() != null && mIssue.pullRequest().diffUrl() != null) {
            tvPull.setVisibility(View.VISIBLE);
            tvPull.setOnClickListener(this);
        } else {
            tvPull.setVisibility(View.GONE);
        }
    }

    @Override
    protected void assignHighlightColor() {
        if (mIssue.state() == IssueState.Closed) {
            setHighlightColors(R.attr.colorIssueClosed, R.attr.colorIssueClosedDark);
        } else {
            setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_pull) {
            startActivity(PullRequestActivity.makeIntent(getActivity(),
                    mRepoOwner, mRepoName, mIssue.number()));
        } else {
            super.onClick(v);
        }
    }

    @Override
    public Loader<LoaderResult<List<TimelineItem>>> onCreateLoader() {
        return new IssueCommentListLoader(getActivity(), mRepoOwner, mRepoName, mIssue.number());
    }

    @Override
    public void editComment(GitHubCommentBase comment) {
        @AttrRes int highlightColorAttr = mIssue.state() == IssueState.Closed
                ? R.attr.colorIssueClosed : R.attr.colorIssueOpen;
        Intent intent = EditIssueCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                mIssue.number(), comment.id(), comment.body(), highlightColorAttr);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    protected Single<Response<Void>> doDeleteComment(GitHubCommentBase comment) {
        IssueCommentService service =
                Gh4Application.get().getGitHubService(IssueCommentService.class);
        return service.deleteIssueComment(mRepoOwner, mRepoName, comment.id());
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.issue_comment_hint;
    }
}
