package com.gh4a.fragment;

import android.content.Intent;
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

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import java.util.List;

public class IssueFragment extends IssueFragmentBase {
    public static IssueFragment newInstance(String repoOwner, String repoName, Issue issue,
            boolean isCollaborator, IntentUtils.InitialCommentMarker initialComment) {
        IssueFragment f = new IssueFragment();
        f.setArguments(buildArgs(repoOwner, repoName, issue, isCollaborator, initialComment));
        return f;
    }

    public void updateState(Issue issue) {
        mIssue.setState(issue.getState());
        assignHighlightColor();
        reloadEvents(false);
    }

    @Override
    protected void bindSpecialViews(View headerView) {
        TextView tvPull = (TextView) headerView.findViewById(R.id.tv_pull);
        if (mIssue.getPullRequest() != null && mIssue.getPullRequest().getDiffUrl() != null) {
            tvPull.setVisibility(View.VISIBLE);
            tvPull.setOnClickListener(this);
        } else {
            tvPull.setVisibility(View.GONE);
        }
    }

    @Override
    protected void assignHighlightColor() {
        if (ApiHelpers.IssueState.CLOSED.equals(mIssue.getState())) {
            setHighlightColors(R.attr.colorIssueClosed, R.attr.colorIssueClosedDark);
        } else {
            setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_pull) {
            startActivity(PullRequestActivity.makeIntent(getActivity(),
                    mRepoOwner, mRepoName, mIssue.getNumber()));
        } else {
            super.onClick(v);
        }
    }

    @Override
    public Loader<LoaderResult<List<TimelineItem>>> onCreateLoader() {
        return new IssueCommentListLoader(getActivity(), mRepoOwner, mRepoName, mIssue.getNumber());
    }

    @Override
    public void editComment(Comment comment) {
        Intent intent = EditIssueCommentActivity.makeIntent(getActivity(),
                mRepoOwner, mRepoName, mIssue.getNumber(), comment);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    protected void deleteCommentInBackground(RepositoryId repoId, Comment comment) throws Exception {
        Gh4Application app = Gh4Application.get();
        IssueService issueService = (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);

        issueService.deleteComment(repoId, comment.getId());
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.issue_comment_hint;
    }

    @Override
    public void replyToComment(long replyToId, String text) {
        // TODO
    }
}
