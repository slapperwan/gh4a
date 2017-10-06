package com.gh4a.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.widget.EditorBottomSheet;

import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Reaction;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.model.request.ReactionRequest;
import com.meisolsson.githubsdk.model.request.pull_request.CreateReviewComment;
import com.meisolsson.githubsdk.service.reactions.ReactionService;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

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
        args.putParcelable("review", review);
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
    private @StringRes int mCommentEditorHintResId = R.string.review_reply_hint;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRepoOwner = args.getString("repo_owner");
        mRepoName = args.getString("repo_name");
        mIssueNumber = args.getInt("issue_number");
        mReview = args.getParcelable("review");
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
                mReview.id());
    }

    @Override
    protected RootAdapter<TimelineItem, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new TimelineItemAdapter(getActivity(), mRepoOwner, mRepoName, mIssueNumber,
                true, false, this);
        return mAdapter;
    }

    @Override
    protected void onAddData(RootAdapter<TimelineItem, ?> adapter, List<TimelineItem> data) {
        selectAndRemoveFirstReply(data);

        // Lock the bottom sheet if there is no selected reply group
        mBottomSheet.setLocked(mSelectedReplyCommentId <= 0,
                R.string.no_reply_group_selected_hint);

        mCommentEditorHintResId = R.string.reply;
        for (TimelineItem item : data) {
            if (item instanceof TimelineItem.Reply) {
                mCommentEditorHintResId = R.string.review_reply_hint;
                break;
            }
        }
        mBottomSheet.updateHint();

        super.onAddData(adapter, data);
        if (mInitialComment != null) {
            highlightInitialComment(data);
        }
    }

    private void selectAndRemoveFirstReply(List<TimelineItem> data) {
        int groupCount = 0;
        TimelineItem.Reply firstReplyItem = null;
        TimelineItem.Diff firstDiffItem = null;
        for (TimelineItem timelineItem : data) {
            if (timelineItem instanceof TimelineItem.Diff) {
                groupCount += 1;
                if (groupCount > 1) {
                    return;
                }

                if (firstDiffItem == null) {
                    firstDiffItem = (TimelineItem.Diff) timelineItem;
                }
            } else if (firstDiffItem != null && timelineItem instanceof TimelineItem.Reply) {
                TimelineItem.Reply replyItem = (TimelineItem.Reply) timelineItem;
                if (replyItem.timelineComment.getParentDiff().equals(firstDiffItem)) {
                    firstReplyItem = replyItem;
                }
            }
        }

        if (firstReplyItem != null) {
            mSelectedReplyCommentId = firstReplyItem.timelineComment.comment().id();
            // When there is only one reply item we don't need to display it
            data.remove(firstReplyItem);
        }
    }

    private void highlightInitialComment(List<TimelineItem> data) {
        for (int i = 0; i < data.size(); i++) {
            TimelineItem item = data.get(i);

            if (item instanceof TimelineItem.TimelineComment) {
                TimelineItem.TimelineComment comment = (TimelineItem.TimelineComment) item;
                if (mInitialComment.matches(comment.comment().id(), comment.getCreatedAt())) {
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
    public void editComment(GitHubCommentBase comment) {
        Intent intent;
        if (comment instanceof ReviewComment) {
            intent = EditPullRequestCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                    mIssueNumber, comment.id(), 0L, comment.body(), 0);
        } else {
            intent = EditIssueCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                    mIssueNumber, comment.id(), comment.body(), 0);
        }

        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    public void deleteComment(final GitHubCommentBase comment) {
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
    public String getShareSubject(GitHubCommentBase comment) {
        return null;
    }

    @Override
    public void addText(CharSequence text) {
    }

    @Override
    public List<Reaction> loadReactionDetailsInBackground(final GitHubCommentBase comment)
            throws IOException {
        final ReactionService service = Gh4Application.get().getGitHubService(ReactionService.class);
        final ApiHelpers.Pager.PageProvider<Reaction> pageProvider;
        if (comment instanceof ReviewComment) {
            pageProvider = new ApiHelpers.Pager.PageProvider<Reaction>() {
                @Override
                public Page<Reaction> providePage(long page) throws IOException {
                    return ApiHelpers.throwOnFailure(service.getPullRequestReviewCommentReactions(
                            mRepoOwner, mRepoName, comment.id(), page).blockingGet());
                }
            };
        } else {
            pageProvider = new ApiHelpers.Pager.PageProvider<Reaction>() {
                @Override
                public Page<Reaction> providePage(long page) throws IOException {
                    return ApiHelpers.throwOnFailure(service.getIssueCommentReactions(
                            mRepoOwner, mRepoName, comment.id(), page).blockingGet());
                }
            };
        }
        return ApiHelpers.Pager.fetchAllPages(pageProvider);
    }

    @Override
    public Reaction addReactionInBackground(GitHubCommentBase comment, String content)
            throws IOException {
        final ReactionService service = Gh4Application.get().getGitHubService(ReactionService.class);
        final ReactionRequest request = ReactionRequest.builder().content(content).build();
        final Response<Reaction> response;
        if (comment instanceof ReviewComment) {
            response = service.createPullRequestReviewCommentReaction(
                    mRepoOwner, mRepoName, comment.id(), request).blockingGet();
        } else {
            response = service.createIssueCommentReaction(
                    mRepoOwner, mRepoName, comment.id(), request).blockingGet();
        }
        return ApiHelpers.throwOnFailure(response);
    }

    @Override
    public int getCommentEditorHintResId() {
        return mCommentEditorHintResId;
    }

    @Override
    public int getEditorErrorMessageResId() {
        return R.string.issue_error_comment;
    }

    @Override
    public void onEditorSendInBackground(String comment) throws IOException {
        PullRequestReviewCommentService service =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
        CreateReviewComment request = CreateReviewComment.builder()
                .body(comment)
                .inReplyTo(mSelectedReplyCommentId)
                .build();
        ApiHelpers.throwOnFailure(service.createReviewComment(
                mRepoOwner, mRepoName, mIssueNumber, request).blockingGet());
    }

    @Override
    public void onEditorTextSent() {
        onRefresh();
    }

    @Override
    public CoordinatorLayout getRootLayout() {
        return getBaseActivity().getRootLayout();
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private final GitHubCommentBase mComment;

        public DeleteCommentTask(BaseActivity activity, GitHubCommentBase comment) {
            super(activity, R.string.deleting_msg);
            mComment = comment;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new DeleteCommentTask(getBaseActivity(), mComment);
        }

        @Override
        protected Void run() throws Exception {
            final Response<Void> response;
            if (mComment instanceof ReviewComment) {
                PullRequestReviewCommentService service =
                        Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
                response = service.deleteComment(mRepoOwner, mRepoName, mComment.id()).blockingGet();
            } else {
                IssueCommentService service =
                        Gh4Application.get().getGitHubService(IssueCommentService.class);
                response = service.deleteIssueComment(mRepoOwner, mRepoName, mComment.id()).blockingGet();
            }
            ApiHelpers.throwOnFailure(response);
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
