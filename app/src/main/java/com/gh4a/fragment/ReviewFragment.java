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
import com.gh4a.loader.ReviewTimelineLoader;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.List;

public class ReviewFragment extends ListDataBaseFragment<TimelineItem>
        implements TimelineItemAdapter.OnCommentAction {

    private static final int REQUEST_EDIT = 1000;

    @Nullable
    private TimelineItemAdapter mAdapter;

    public static ReviewFragment newInstance(String repoOwner, String repoName, int issueNumber,
            Review review, IntentUtils.InitialCommentMarker mInitialComment) {
        ReviewFragment f = new ReviewFragment();
        Bundle args = new Bundle();
        args.putString("repo_owner", repoOwner);
        args.putString("repo_name", repoName);
        args.putInt("issue_number", issueNumber);
        args.putSerializable("review", review);
        args.putParcelable("initial_comment", mInitialComment);
        f.setArguments(args);
        return f;
    }

    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private Review mReview;
    private IntentUtils.InitialCommentMarker mInitialComment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRepoOwner = args.getString("repo_owner");
        mRepoName = args.getString("repo_name");
        mIssueNumber = args.getInt("issue_number");
        mReview = (Review) args.getSerializable("review");
        mInitialComment = args.getParcelable("initial_comment");
        args.remove("initial_comment");
    }

    @Override
    protected Loader<LoaderResult<List<TimelineItem>>> onCreateLoader() {
        return new ReviewTimelineLoader(getActivity(), mRepoOwner, mRepoName, mIssueNumber,
                mReview.getId());
    }

    @Override
    protected RootAdapter<TimelineItem, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new TimelineItemAdapter(getActivity(), mRepoOwner, mRepoName, mIssueNumber,
                true, false, this);
        return mAdapter;
    }

    @Override
    protected void onAddData(RootAdapter<TimelineItem, ?> adapter, List<TimelineItem> data) {
        super.onAddData(adapter, data);

        if (mInitialComment != null) {
            for (int i = 0; i < data.size(); i++) {
                TimelineItem item = data.get(i);

                if (item instanceof TimelineItem.TimelineComment) {
                    TimelineItem.TimelineComment comment = (TimelineItem.TimelineComment) item;
                    if (mInitialComment.matches(comment.comment.getId(), comment.getCreatedAt())) {
                        scrollToAndHighlightPosition(i);
                        break;
                    }
                }
            }
            mInitialComment = null;
        }
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
                    mIssueNumber, 0L, (CommitComment) comment, 0);
        } else {
            intent = EditIssueCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                    mIssueNumber, comment, 0);
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
    public void replyToComment(long replyToId) {
        Intent intent = EditPullRequestCommentActivity.makeIntent(getActivity(),
                mRepoOwner, mRepoName, mIssueNumber, replyToId, new CommitComment(), 0);
        startActivity(intent);
    }

    @Override
    public String getShareSubject(Comment comment) {
        return null;
    }

    @Override
    public List<Reaction> loadReactionDetailsInBackground(Comment comment) throws IOException {
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
        Gh4Application app = Gh4Application.get();
        if (comment instanceof CommitComment) {
            PullRequestService pullService =
                    (PullRequestService) app.getService(Gh4Application.PULL_SERVICE);
            return pullService.getCommentReactions(repoId, comment.getId());
        } else {
            IssueService issueService =
                    (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);
            return issueService.getCommentReactions(repoId, comment.getId());
        }
    }

    @Override
    public Reaction addReactionInBackground(Comment comment, String content) throws IOException {
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
        Gh4Application app = Gh4Application.get();
        if (comment instanceof CommitComment) {
            PullRequestService pullService =
                    (PullRequestService) app.getService(Gh4Application.PULL_SERVICE);
            return pullService.addCommentReaction(repoId, comment.getId(), content);
        } else {
            IssueService issueService =
                    (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);
            return issueService.addCommentReaction(repoId, comment.getId(), content);
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
