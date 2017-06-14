package com.gh4a.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.timeline.TimelineItemAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.ReviewCommentListLoader;
import com.gh4a.loader.TimelineItem;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.List;

public class ReviewCommentsFragment extends ListDataBaseFragment<TimelineItem>
        implements TimelineItemAdapter.OnCommentAction {

    private static final int REQUEST_EDIT = 1000;

    @Nullable
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
    protected Loader<LoaderResult<List<TimelineItem>>> onCreateLoader() {
        return new ReviewCommentListLoader(getActivity(), mRepoOwner, mRepoName, mIssueNumber,
                mReview.review.getId());
    }

    @Override
    protected RootAdapter<TimelineItem, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new TimelineItemAdapter(getActivity(), mRepoOwner, mRepoName, mIssueNumber,
                mIsPullRequest, this);
        return mAdapter;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                reloadComments(true);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void reloadComments( boolean alsoClearCaches) {
        if (mAdapter != null && !alsoClearCaches) {
            // Don't clear adapter's cache, we're only interested in the new event
            mAdapter.suppressCacheClearOnNextClear();
        }

        onRefresh();
    }

    @Override
    protected int getEmptyTextResId() {
        return 0;
    }

    @Override
    public void editComment(Comment comment) {
        Intent intent;
        if (comment instanceof CommitComment) {
            intent = EditPullRequestCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                    mIssueNumber, (CommitComment) comment);
        } else {
            intent = EditIssueCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                    mIssueNumber, comment);
        }

        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    public void deleteComment(final Comment comment) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_comment_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteCommentTask(getBaseActivity(), comment).schedule();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
            reloadComments(true);
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.issue_error_comment);
        }
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private final Comment mComment;

        public DeleteCommentTask(BaseActivity activity, Comment comment) {
            super(activity, R.string.deleting_msg);
            mComment = comment;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new DeleteCommentTask(getBaseActivity(), mComment);
        }

        @Override
        protected Void run() throws Exception {
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            Gh4Application app = Gh4Application.get();

            if (mComment instanceof CommitComment) {
                PullRequestService pullService =
                        (PullRequestService) app.getService(Gh4Application.PULL_SERVICE);
                pullService.deleteComment(repoId, mComment.getId());
            } else {
                IssueService issueService =
                        (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);
                issueService.deleteComment(repoId, mComment.getId());
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            reloadComments(false);
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.error_delete_comment);
        }
    }
}
