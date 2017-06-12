package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.timeline.TimelineItemAdapter;
import com.gh4a.loader.TimelineItem;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewCommentsFragment extends LoadingListFragmentBase
        implements TimelineItemAdapter.OnCommentAction {

    private TimelineItemAdapter mAdapter;

    public static ReviewCommentsFragment newInstance(String repoOwner, String repoName,
            int issueNumber, boolean isPullRequest, TimelineItem.TimelineReview review) {
        ReviewCommentsFragment f = new ReviewCommentsFragment();
        Bundle args = new Bundle();
        args.putString("repo_owner", repoOwner);
        args.putString("repo_name", repoName);
        args.putInt("issue_number", issueNumber);
        args.putBoolean("is_pr", isPullRequest);
        args.putSerializable("review", review);
        f.setArguments(args);
        return f;
    }

    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private boolean mIsPullRequest;
    private TimelineItem.TimelineReview mReview;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRepoOwner = args.getString("repo_owner");
        mRepoName = args.getString("repo_name");
        mIssueNumber = args.getInt("issue_number");
        mIsPullRequest = args.getBoolean("is_pr");
        mReview = (TimelineItem.TimelineReview) args.getSerializable("review");
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);

        mAdapter = new TimelineItemAdapter(getActivity(), mRepoOwner, mRepoName,
                mIssueNumber, mIsPullRequest, this);

        List<TimelineItem.Diff> chunks = new ArrayList<>(mReview.chunks.values());
        Collections.sort(chunks);

        for (TimelineItem.Diff chunk : chunks) {
            mAdapter.add(chunk);
            for (TimelineItem.TimelineComment comment : chunk.comments) {
                mAdapter.add(comment);
            }

            mAdapter.add(new TimelineItem.Reply(chunk.getInitialTimelineComment()));
        }
        view.setAdapter(mAdapter);
    }

    @Override
    public void onRefresh() {
    }

    @Override
    protected int getEmptyTextResId() {
        return 0;
    }

    @Override
    public void editComment(Comment comment) {

    }

    @Override
    public void deleteComment(Comment comment) {

    }

    @Override
    public void quoteText(CharSequence text) {

    }

    @Override
    public void replyToComment(long replyToId, String text) {
        new ReplyTask(getBaseActivity(), replyToId, text).schedule();
    }

    @Override
    public String getShareSubject(Comment comment) {
        return null;
    }

    private class ReplyTask extends ProgressDialogTask<CommitComment> {
        private final BaseActivity mBaseActivity;
        private final String mBody;
        private final long mReplyToId;

        public ReplyTask(BaseActivity baseActivity, long replyToId, String body) {
            super(baseActivity, R.string.saving_msg);
            mBaseActivity = baseActivity;
            mReplyToId = replyToId;
            mBody = body;
        }

        @Override
        protected ProgressDialogTask<CommitComment> clone() {
            return new ReplyTask(mBaseActivity, mReplyToId, mBody);
        }

        @Override
        protected CommitComment run() throws IOException {
            PullRequestService pullRequestService =
                    (PullRequestService) Gh4Application.get()
                            .getService(Gh4Application.PULL_SERVICE);

            return pullRequestService.replyToComment(new RepositoryId(mRepoOwner, mRepoName),
                    mIssueNumber, mReplyToId, mBody);
        }

        @Override
        protected void onSuccess(CommitComment result) {
            // TODO: Refresh comments
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.issue_error_comment);
        }
    }
}
