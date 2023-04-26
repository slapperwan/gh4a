package com.gh4a.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.timeline.TimelineItemAdapter;
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.ActivityResultHelpers;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.gh4a.widget.EditorBottomSheet;

import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.Reaction;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.model.request.ReactionRequest;
import com.meisolsson.githubsdk.model.request.pull_request.CreateReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;
import com.meisolsson.githubsdk.service.reactions.ReactionService;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

public class ReviewFragment extends ListDataBaseFragment<TimelineItem> implements
        TimelineItemAdapter.OnCommentAction, ConfirmationDialogFragment.Callback,
        EditorBottomSheet.Callback, EditorBottomSheet.Listener {

    private static final String EXTRA_SELECTED_REPLY_COMMENT_ID = "selected_reply_comment_id";

    @Nullable
    private TimelineItemAdapter mAdapter;
    private EditorBottomSheet mBottomSheet;

    private final ActivityResultLauncher<Intent> mEditLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> reloadComments(true))
    );

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
    protected Single<List<TimelineItem>> onCreateDataSingle(boolean bypassCache) {
        final PullRequestService prService = ServiceFactory.get(PullRequestService.class, bypassCache);
        final PullRequestReviewService reviewService =
                ServiceFactory.get(PullRequestReviewService.class, bypassCache);
        final PullRequestReviewCommentService commentService =
                ServiceFactory.get(PullRequestReviewCommentService.class, bypassCache);

        // The Review object passed to this fragment may be incomplete, so we re-fetch it to make
        // sure it has all the needed fields
        Single<TimelineItem.TimelineReview> reviewItemSingle =
                reviewService.getReview(mRepoOwner, mRepoName, mIssueNumber, mReview.id())
                .map(ApiHelpers::throwOnFailure)
                .map(TimelineItem.TimelineReview::new);

        Single<List<ReviewComment>> reviewCommentsSingle = ApiHelpers.PageIterator
                .toSingle(page -> reviewService.getReviewComments(
                        mRepoOwner, mRepoName, mIssueNumber, mReview.id(), page))
                .compose(RxUtils.sortList(ApiHelpers.COMMENT_COMPARATOR))
                .cache(); // single is used multiple times -> avoid refetching data

        Single<Boolean> hasCommentsSingle = reviewCommentsSingle
                .map(comments -> !comments.isEmpty());

        Single<Optional<List<GitHubFile>>> filesSingle = hasCommentsSingle
                .flatMap(hasComments -> {
                    if (!hasComments) {
                        return Single.just(Optional.absent());
                    }
                    return ApiHelpers.PageIterator
                            .toSingle(page -> prService.getPullRequestFiles(
                                    mRepoOwner, mRepoName, mIssueNumber, page))
                            .map(Optional::of);
                });

        Single<Optional<List<ReviewComment>>> commentsSingle = hasCommentsSingle
                .flatMap(hasComments -> {
                    if (!hasComments) {
                        return Single.just(Optional.absent());
                    }
                    return ApiHelpers.PageIterator
                            .toSingle(page -> commentService.getPullRequestComments(
                                    mRepoOwner, mRepoName, mIssueNumber, page))
                            .compose(RxUtils.sortList(ApiHelpers.COMMENT_COMPARATOR))
                            .map(Optional::of);
                });

        return Single.zip(
                reviewItemSingle.subscribeOn(Schedulers.io()),
                reviewCommentsSingle.subscribeOn(Schedulers.io()),
                filesSingle.subscribeOn(Schedulers.io()),
                commentsSingle.subscribeOn(Schedulers.io()),
                (reviewItem, reviewComments, filesOpt, commentsOpt) -> {
            if (!reviewComments.isEmpty()) {
                HashMap<String, GitHubFile> filesByName = new HashMap<>();
                if (filesOpt.isPresent()) {
                    for (GitHubFile file : filesOpt.get()) {
                        filesByName.put(file.filename(), file);
                    }
                }

                // Add all of the review comments to the review item creating necessary diff hunks
                for (ReviewComment reviewComment : reviewComments) {
                    GitHubFile file = filesByName.get(reviewComment.path());
                    reviewItem.addComment(reviewComment, file, true);
                }

                if (commentsOpt.isPresent()) {
                    for (ReviewComment commitComment : commentsOpt.get()) {
                        boolean alreadyPresent = false;
                        for (ReviewComment reviewComment : reviewComments) {
                            if (commitComment.id().equals(reviewComment.id())) {
                                alreadyPresent = true;
                                break;
                            }
                        }
                        if (!alreadyPresent) {
                            // Rest of the comments should be added only if they are under the same
                            // diff hunks as the original review comments.
                            GitHubFile file = filesByName.get(commitComment.path());
                            reviewItem.addComment(commitComment, file, false);
                        }
                    }
                }
            }

            List<TimelineItem> items = new ArrayList<>();
            items.add(reviewItem);

            List<TimelineItem.Diff> diffHunks = new ArrayList<>(reviewItem.getDiffHunks());
            Collections.sort(diffHunks);

            for (TimelineItem.Diff diffHunk : diffHunks) {
                items.add(diffHunk);
                items.addAll(diffHunk.comments);

                if (!diffHunk.isReply()) {
                    items.add(new TimelineItem.Reply(diffHunk.getInitialTimelineComment()));
                }
            }

            return items;
        });
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

        mEditLauncher.launch(intent);
    }

    @Override
    public void deleteComment(final GitHubCommentBase comment) {
        ConfirmationDialogFragment.show(this, R.string.delete_comment_message,
                R.string.delete, comment, "deleteconfirm");
    }

    @Override
    public void onConfirmed(String tag, Parcelable data) {
        GitHubCommentBase comment = (GitHubCommentBase) data;
        handleDeleteComment(comment);
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
    public Single<List<Reaction>> loadReactionDetails(final GitHubCommentBase comment,
            boolean bypassCache) {
        final ReactionService service = ServiceFactory.get(ReactionService.class, bypassCache);
        return ApiHelpers.PageIterator
                .toSingle(page -> comment instanceof ReviewComment
                        ? service.getPullRequestReviewCommentReactions(
                                mRepoOwner, mRepoName, comment.id(), page)
                        : service.getIssueCommentReactions(
                                mRepoOwner, mRepoName, comment.id(), page));
    }

    @Override
    public Single<Reaction> addReaction(GitHubCommentBase comment, String content) {
        final ReactionService service = ServiceFactory.get(ReactionService.class, false);
        final ReactionRequest request = ReactionRequest.builder().content(content).build();
        final Single<Response<Reaction>> responseSingle = comment instanceof ReviewComment
                ? service.createPullRequestReviewCommentReaction(mRepoOwner, mRepoName, comment.id(), request)
                : service.createIssueCommentReaction(mRepoOwner, mRepoName, comment.id(), request);
        return responseSingle.map(ApiHelpers::throwOnFailure);
    }

    @Override
    public Single<Boolean> deleteReaction(GitHubCommentBase comment, long reactionId) {
        final ReactionService service = ServiceFactory.get(ReactionService.class, false);
        final Single<Response<Void>> responseSingle = comment instanceof ReviewComment
                ? service.deletePullRequestReviewCommentReaction(mRepoOwner, mRepoName, comment.id(), reactionId)
                : service.deleteIssueCommentReaction(mRepoOwner, mRepoName, comment.id(), reactionId);
        return responseSingle.map(ApiHelpers::mapToTrueOnSuccess);
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
    public Single<?> onEditorDoSend(String comment) {
        PullRequestReviewCommentService service =
                ServiceFactory.get(PullRequestReviewCommentService.class, false);
        CreateReviewComment request = CreateReviewComment.builder()
                .body(comment)
                .inReplyTo(mSelectedReplyCommentId)
                .build();
        return service.createReviewComment(mRepoOwner, mRepoName, mIssueNumber, request)
                .map(ApiHelpers::throwOnFailure);
    }

    @Override
    public void onEditorTextSent() {
        onRefresh();
    }

    @Override
    public CoordinatorLayout getRootLayout() {
        return getBaseActivity().getRootLayout();
    }

    private void handleDeleteComment(GitHubCommentBase comment) {
        final Single<Response<Void>> responseSingle;
        if (comment instanceof ReviewComment) {
            PullRequestReviewCommentService service =
                    ServiceFactory.get(PullRequestReviewCommentService.class, false);
            responseSingle = service.deleteComment(mRepoOwner, mRepoName, comment.id());
        } else {
            IssueCommentService service = ServiceFactory.get(IssueCommentService.class, false);
            responseSingle = service.deleteIssueComment(mRepoOwner, mRepoName, comment.id());
        }

        responseSingle
                .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(getBaseActivity(),
                        R.string.deleting_msg, R.string.error_delete_comment))
                .subscribe(result -> reloadComments(false),
                        error -> handleActionFailure("Deleting comment failed", error));
    }
}
