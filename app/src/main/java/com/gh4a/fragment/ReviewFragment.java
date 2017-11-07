package com.gh4a.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

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
import com.gh4a.widget.EditorBottomSheet;

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
        implements TimelineItemAdapter.OnCommentAction,
        EditorBottomSheet.Callback, EditorBottomSheet.Listener {

    private static final int REQUEST_EDIT = 1000;
    private static final String EXTRA_SELECTED_REPLY_COMMENT_ID = "selected_reply_comment_id";

    @Nullable
    private TimelineItemAdapter mAdapter;
    private EditorBottomSheet mBottomSheet;

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
    private long mSelectedReplyCommentId;

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

        if (savedInstanceState != null) {
            mSelectedReplyCommentId = savedInstanceState.getLong(EXTRA_SELECTED_REPLY_COMMENT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View listContent = super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.comment_list, container, false);

        FrameLayout listContainer = v.findViewById(R.id.list_container);
        listContainer.addView(listContent);

        mBottomSheet = v.findViewById(R.id.bottom_sheet);
        mBottomSheet.setCallback(this);
        mBottomSheet.setResizingView(listContainer);
        mBottomSheet.setListener(this);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getBaseActivity().addAppBarOffsetListener(mBottomSheet);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_SELECTED_REPLY_COMMENT_ID, mSelectedReplyCommentId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getBaseActivity().removeAppBarOffsetListener(mBottomSheet);
    }

    @Override
    public boolean onBackPressed() {
        if (mBottomSheet != null && mBottomSheet.isInAdvancedMode()) {
            mBottomSheet.setAdvancedMode(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean canChildScrollUp() {
        return (mBottomSheet != null && mBottomSheet.isExpanded()) || super.canChildScrollUp();
    }

    @Override
    protected void setHighlightColors(int colorAttrId, int statusBarColorAttrId) {
        super.setHighlightColors(colorAttrId, statusBarColorAttrId);
        mBottomSheet.setHighlightColor(colorAttrId);
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
        if (mSelectedReplyCommentId == 0) {
            selectFirstReply(data);
        } else {
            mBottomSheet.setLocked(false, 0);
        }
        if (mInitialComment != null) {
            highlightInitialComment(data);
        }
    }

    private void selectFirstReply(List<TimelineItem> data) {
        int replyItemCount = 0;
        long firstReplyId = 0;
        for (TimelineItem timelineItem : data) {
            if (timelineItem instanceof TimelineItem.Reply) {
                replyItemCount += 1;

                if (replyItemCount > 1) {
                    mBottomSheet.setLocked(true, R.string.no_reply_group_selected_hint);
                    // Do not auto-select reply group if there are more than 1 of them
                    return;
                }

                if (firstReplyId == 0) {
                    TimelineItem.Reply timelineReply = (TimelineItem.Reply) timelineItem;
                    firstReplyId = timelineReply.timelineComment.comment.getId();
                }
            }
        }

        mSelectedReplyCommentId = firstReplyId;
    }

    private void highlightInitialComment(List<TimelineItem> data) {
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
    public void onToggleAdvancedMode(boolean advancedMode) {
        getBaseActivity().collapseAppBar();
        getBaseActivity().setAppBarLocked(advancedMode);
        mBottomSheet.resetPeekHeight(0);
    }

    @Override
    public void onScrollingInBasicEditor(boolean scrolling) {
        getBaseActivity().setAppBarLocked(scrolling);
    }

    @Override
    public void quoteText(CharSequence text) {
        mBottomSheet.addQuote(text);
    }

    @Override
    public void onReplyCommentSelected(long replyToId) {
        mSelectedReplyCommentId = replyToId;
        mBottomSheet.setLocked(false, 0);
    }

    @Override
    public long getSelectedReplyCommentId() {
        return mSelectedReplyCommentId;
    }

    @Override
    public String getShareSubject(Comment comment) {
        return null;
    }

    @Override
    public void addText(CharSequence text) {
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

    @Override
    public int getCommentEditorHintResId() {
        return R.string.review_reply_hint;
    }

    @Override
    public void onSendCommentInBackground(String comment) throws IOException {
        Gh4Application app = Gh4Application.get();
        PullRequestService pullService =
                (PullRequestService) app.getService(Gh4Application.PULL_SERVICE);
        RepositoryId repositoryId = new RepositoryId(mRepoOwner, mRepoName);
        pullService.replyToComment(repositoryId, mIssueNumber, mSelectedReplyCommentId, comment);
    }

    @Override
    public void onCommentSent() {
        onRefresh();
    }

    @Override
    public CoordinatorLayout getRootLayout() {
        return getBaseActivity().getRootLayout();
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
