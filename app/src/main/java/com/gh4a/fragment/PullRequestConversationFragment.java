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
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.model.StatusWrapper;
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.widget.CommitStatusBox;
import com.gh4a.widget.PullRequestBranchInfoView;
import com.meisolsson.githubsdk.model.CheckRun;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueEvent;
import com.meisolsson.githubsdk.model.IssueEventType;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.model.PullRequestMarker;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.model.ReviewState;
import com.meisolsson.githubsdk.model.Status;
import com.meisolsson.githubsdk.model.git.GitReference;
import com.meisolsson.githubsdk.model.request.git.CreateGitReference;
import com.meisolsson.githubsdk.service.checks.ChecksService;
import com.meisolsson.githubsdk.service.git.GitService;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;
import com.meisolsson.githubsdk.service.issues.IssueTimelineService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;
import com.meisolsson.githubsdk.service.repositories.RepositoryStatusService;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;
import androidx.collection.LongSparseArray;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

public class PullRequestConversationFragment extends IssueFragmentBase {
    private static final int ID_LOADER_STATUS = 1;
    private static final int ID_LOADER_HEAD_REF = 2;

    private PullRequest mPullRequest;
    private GitReference mHeadReference;
    private boolean mHasLoadedHeadReference;

    public static PullRequestConversationFragment newInstance(PullRequest pr, Issue issue,
            boolean isCollaborator, IntentUtils.InitialCommentMarker initialComment) {
        PullRequestConversationFragment f = new PullRequestConversationFragment();

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
        loadCommitStatusesIfOpen(false);
        reloadEvents(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mPullRequest = getArguments().getParcelable("pr");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadHeadReference(false);
        loadCommitStatusesIfOpen(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.pull_request_fragment_menu, menu);

        if (mPullRequest == null || mPullRequest.head().repo() == null
                || mPullRequest.state() == IssueState.Open) {
            menu.removeItem(R.id.delete_branch);
        } else {
            MenuItem deleteBranchItem = menu.findItem(R.id.delete_branch);
            deleteBranchItem.setVisible(mHasLoadedHeadReference);
            if (mHeadReference == null) {
                deleteBranchItem.setTitle(R.string.restore_branch);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_branch:
                showDeleteRestoreBranchConfirmDialog(mHeadReference == null);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        mHeadReference = null;
        mHasLoadedHeadReference = false;
        if (mListHeaderView != null) {
            fillStatus(new ArrayList<>());
        }
        loadHeadReference(true);
        loadCommitStatusesIfOpen(true);
        super.onRefresh();
    }

    @Override
    protected void bindSpecialViews(View headerView) {
        PullRequestBranchInfoView branchContainer = headerView.findViewById(R.id.branch_container);
        branchContainer.bind(mPullRequest.head(), mPullRequest.base(), mHeadReference);
        branchContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void assignHighlightColor() {
        if (mPullRequest.merged()) {
            setHighlightColors(R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark);
        } else if (mPullRequest.state() == IssueState.Closed) {
            setHighlightColors(R.attr.colorIssueClosed, R.attr.colorIssueClosedDark);
        } else if (mPullRequest.draft()) {
            setHighlightColors(R.attr.colorPullRequestDraft, R.attr.colorPullRequestDraftDark);
        } else {
            setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
        }
    }

   private void fillStatus(List<StatusWrapper> statuses) {
       CommitStatusBox commitStatusBox = mListHeaderView.findViewById(R.id.commit_status_box);
       commitStatusBox.fillStatus(statuses, mPullRequest.mergeableState());
   }

    @Override
    protected Single<List<TimelineItem>> onCreateDataSingle(boolean bypassCache) {
        final int issueNumber = mIssue.number();
        var timelineService = ServiceFactory.getForFullPagedLists(IssueTimelineService.class, bypassCache);
        var reviewService = ServiceFactory.getForFullPagedLists(PullRequestReviewService.class, bypassCache);
        var prCommentService = ServiceFactory.getForFullPagedLists(PullRequestReviewCommentService.class, bypassCache);

        Single<List<TimelineItem>> timelineItemsSingle = ApiHelpers.PageIterator
                .toSingle(page -> timelineService.getTimeline(mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.filter(event -> INTERESTING_EVENTS.contains(event.event())))
                .map(this::removeRedundantClosedEvent)
                .compose(RxUtils.mapList(TimelineItem::fromIssueEvent));

        Single<List<Review>> reviewsSingle = ApiHelpers.PageIterator
                .toSingle(page -> reviewService.getReviews(mRepoOwner, mRepoName, issueNumber, page))
                .cache(); // single is used multiple times -> avoid refetching data
        Single<List<ReviewComment>> prCommentsSingle = ApiHelpers.PageIterator
                .toSingle(page -> prCommentService.getPullRequestComments(mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.sortList(ApiHelpers.COMMENT_COMPARATOR))
                .cache(); // single is used multiple times -> avoid refetching data

        // For reviews with pending state we have to manually load the comments
        Single<LongSparseArray<List<ReviewComment>>> pendingReviewCommentsByIdSingle = reviewsSingle
                .compose(RxUtils.filter(r -> r.state() == ReviewState.Pending))
                .toObservable()
                .flatMap(reviews -> {
                    List<Observable<Pair<Long, List<ReviewComment>>>> obsList = new ArrayList<>();
                    for (Review r : reviews) {
                        Single<List<ReviewComment>> single = ApiHelpers.PageIterator
                                .toSingle(page -> reviewService.getReviewComments(mRepoOwner,
                                        mRepoName, issueNumber, r.id(), page));
                        obsList.add(Single.zip(Single.just(r.id()), single, Pair::create).toObservable());
                    }
                    return Observable.concat(obsList);
                })
                .toList()
                .map(list -> {
                    LongSparseArray<List<ReviewComment>> result = new LongSparseArray<>();
                    for (Pair<Long, List<ReviewComment>> pair : list) {
                        result.put(pair.first, pair.second);
                    }
                    return result;
                });

        Single<List<TimelineItem.TimelineReview>> reviewItemsSingle = Single.zip(
                reviewsSingle, prCommentsSingle, pendingReviewCommentsByIdSingle,
                (prReviews, prComments, pendingReviewCommentsById) -> {
            LongSparseArray<TimelineItem.TimelineReview> reviewsById = new LongSparseArray<>();
            List<TimelineItem.TimelineReview> reviewItems = new ArrayList<>();

            for (Review review : prReviews) {
                TimelineItem.TimelineReview timelineReview = new TimelineItem.TimelineReview(review);
                reviewsById.put(review.id(), timelineReview);
                reviewItems.add(timelineReview);

                if (review.state() == ReviewState.Pending) {
                    for (ReviewComment pendingComment : pendingReviewCommentsById.get(review.id())) {
                        timelineReview.addComment(pendingComment, null, true);
                    }
                }
            }

            Map<String, TimelineItem.TimelineReview> reviewsByDiffHunkId = new HashMap<>();
            for (ReviewComment comment : prComments) {
                if (comment.pullRequestReviewId() != null) {
                    String hunkId = TimelineItem.Diff.getDiffHunkId(comment);

                    TimelineItem.TimelineReview reviewItem = reviewsByDiffHunkId.get(hunkId);
                    if (reviewItem == null) {
                        reviewItem = reviewsById.get(comment.pullRequestReviewId());
                        reviewsByDiffHunkId.put(hunkId, reviewItem);
                    }

                    if (reviewItem != null) {
                        reviewItem.addComment(comment, null, true);
                    }
                }
            }

            return reviewItems;
        })
        // In some cases, replies to review threads are considered themselves "reviews" by GitHub.
        // We drop these "not-really reviews" since we've already added their comments to the
        // appropriate timeline review items.
        .compose(RxUtils.filter(reviewItem ->
                reviewItem.review().state() != ReviewState.Commented
                  || !TextUtils.isEmpty(reviewItem.review().body())
                  || !reviewItem.getDiffHunks().isEmpty()));

        // Before the introduction of reviews in 2016, GitHub allowed to add single
        // review comments which are not linked to a review object.
        // For now we're showing them in between the conversation, but it would be best
        // to group them in threads as GitHub does.
        Single<List<TimelineItem>> prCommentsWithoutReviewSingle = prCommentsSingle
                        .compose(RxUtils.filter(comment -> comment.pullRequestReviewId() == null))
                        .compose(RxUtils.mapList(TimelineItem.TimelineComment::new));

        return Single.zip(
                timelineItemsSingle.subscribeOn(Schedulers.io()),
                reviewItemsSingle.subscribeOn(Schedulers.io()),
                prCommentsWithoutReviewSingle.subscribeOn(Schedulers.io()),
                (timelineItems, reviewItems, commentsWithoutReview) -> {
            ArrayList<TimelineItem> result = new ArrayList<>();
            result.addAll(timelineItems);
            result.addAll(reviewItems);
            result.addAll(commentsWithoutReview);
            Collections.sort(result, TimelineItem.COMPARATOR);
            return result;
        });
    }

    // The GitHub timeline API always returns a "closed" event after a "merged" one, which we don't want
    // to display (as GH does on their website) because it doesn't make much sense from a user perspective:
    // a user either closes or merges a PR, not both at the same time.
    private List<IssueEvent> removeRedundantClosedEvent(List<IssueEvent> timelineEvents) {
        int mergedEventIndex = timelineEvents.stream()
                .map(IssueEvent::event).toList()
                .indexOf(IssueEventType.Merged);

        if (mergedEventIndex != -1) {
            timelineEvents
                .subList(mergedEventIndex, timelineEvents.size())
                .removeIf(event -> event.event() == IssueEventType.Closed);
        }

        return timelineEvents;
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
        mEditLauncher.launch(intent);
    }


    @Override
    protected Single<Response<Void>> doDeleteComment(GitHubCommentBase comment) {
        if (comment instanceof ReviewComment) {
            PullRequestReviewCommentService service =
                    ServiceFactory.get(PullRequestReviewCommentService.class, false);
            return service.deleteComment(mRepoOwner, mRepoName, comment.id());
        } else {
            IssueCommentService service = ServiceFactory.get(IssueCommentService.class, false);
            return service.deleteIssueComment(mRepoOwner, mRepoName, comment.id());
        }
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.pull_request_comment_hint;
    }

    private void showDeleteRestoreBranchConfirmDialog(final boolean restore) {
        @StringRes int message = restore
                ? R.string.restore_branch_question : R.string.delete_branch_question;
        @StringRes int buttonText = restore ? R.string.restore : R.string.delete;

        Bundle data = new Bundle();
        data.putBoolean("restore", restore);
        ConfirmationDialogFragment.show(this, message,
                buttonText, true, data, "prbranchconfirm");
    }

    @Override
    public void onConfirmed(String tag, Parcelable data) {
        if ("prbranchconfirm".equals(tag)) {
            boolean restore = ((Bundle) data).getBoolean("restore");
            if (restore) {
                restorePullRequestBranch();
            } else {
                deletePullRequestBranch();
            }
        } else {
            super.onConfirmed(tag, data);
        }
    }

    private void onHeadReferenceUpdated() {
        getActivity().invalidateOptionsMenu();
        reloadEvents(false);
    }

    private void restorePullRequestBranch() {
        PullRequestMarker head = mPullRequest.head();
        if (head.repo() == null) {
            return;
        }
        String owner = head.repo().owner().login();
        String repo = head.repo().name();

        GitService service = ServiceFactory.get(GitService.class, false);
        CreateGitReference request = CreateGitReference.builder()
                .ref("refs/heads/" + head.ref())
                .sha(head.sha())
                .build();

        service.createGitReference(owner, repo, request)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(getBaseActivity(),
                        R.string.saving_msg, R.string.restore_branch_error))
                .subscribe(result -> {
                    mHeadReference = result;
                    onHeadReferenceUpdated();
                }, error -> handleActionFailure("Restoring PR branch failed", error));
    }

    private void deletePullRequestBranch() {
        GitService service = ServiceFactory.get(GitService.class, false);

        PullRequestMarker head = mPullRequest.head();
        String owner = head.repo().owner().login();
        String repo = head.repo().name();

        service.deleteGitReference(owner, repo, "heads/" + head.ref())
                .map(ApiHelpers::mapToTrueOnSuccess)
                .compose(RxUtils.wrapForBackgroundTask(getBaseActivity(),
                        R.string.deleting_msg, R.string.delete_branch_error))
                .subscribe(result -> {
                    mHeadReference = null;
                    onHeadReferenceUpdated();
                }, error -> handleActionFailure("Deleting PR branch failed", error));
    }

    private static final Comparator<Status> STATUS_TIMESTAMP_COMPARATOR =
            (lhs, rhs) -> rhs.updatedAt().compareTo(lhs.updatedAt());
    private static final Comparator<Status> STATUS_AND_CONTEXT_COMPARATOR = new Comparator<Status>() {
        @Override
        public int compare(Status lhs, Status rhs) {
            int lhsSeverity = getStateSeverity(lhs);
            int rhsSeverity = getStateSeverity(rhs);
            if (lhsSeverity != rhsSeverity) {
                return lhsSeverity < rhsSeverity ? 1 : -1;
            } else {
                return lhs.context().compareTo(rhs.context());
            }
        }

        private int getStateSeverity(Status status) {
            switch (status.state()) {
                case Success: return 0;
                case Error:
                case Failure: return 2;
                default: return 1;
            }
        }
    };

    private void loadCommitStatusesIfOpen(boolean force) {
        if (mPullRequest.state() != IssueState.Open) {
            return;
        }

        // At this point the status box will display a loading placeholder
        CommitStatusBox commitStatusBox = mListHeaderView.findViewById(R.id.commit_status_box);
        commitStatusBox.setVisibility(View.VISIBLE);

        var repoService = ServiceFactory.getForFullPagedLists(RepositoryStatusService.class, force);
        String sha = mPullRequest.head().sha();

        Single<List<Status>> statusSingle = ApiHelpers.PageIterator
                .toSingle(page -> repoService.getStatuses(mRepoOwner, mRepoName, sha, page))
                // Sort by timestamps first, so the removal logic below keeps the newest status
                .compose(RxUtils.sortList(STATUS_TIMESTAMP_COMPARATOR))
                // Filter out outdated statuses, only keep the newest status per context
                .map(statuses -> {
                    Set<String> seenContexts = new HashSet<>();
                    Iterator<Status> iter = statuses.iterator();
                    while (iter.hasNext()) {
                        Status status = iter.next();
                        if (seenContexts.contains(status.context())) {
                            iter.remove();
                        } else {
                            seenContexts.add(status.context());
                        }
                    }
                    return statuses;
                })
                // sort by status, then context
                .compose(RxUtils.sortList(STATUS_AND_CONTEXT_COMPARATOR));

        ChecksService checksService = ServiceFactory.get(ChecksService.class, force);
        Single<List<CheckRun>> checksSingle = checksService.getCheckRunsForRef(mRepoOwner, mRepoName, sha)
                .map(ApiHelpers::throwOnFailure)
                .map(response -> response.checkRuns())
                .map(checkRuns -> {
                    HashMap<String, CheckRun> filteredRuns = new HashMap<>();
                    for (CheckRun run : checkRuns) {
                        CheckRun existing = filteredRuns.get(run.name());
                        if (existing == null
                                || run.startedAt() == null
                                || run.startedAt().after(existing.startedAt())) {
                            filteredRuns.put(run.name(), run);
                        }
                    }
                    return new ArrayList<>(filteredRuns.values());
                });

        Single<List<StatusWrapper>> allResultsSingle = Single.zip(statusSingle, checksSingle, (statuses, checks) -> {
            List<StatusWrapper> wrappers = new ArrayList<>();
            for (CheckRun check : checks) {
                wrappers.add(new StatusWrapper(getContext(), check));
            }
            for (Status status : statuses) {
                wrappers.add(new StatusWrapper(status));
            }
            return wrappers;
        });

        allResultsSingle
                .compose(makeLoaderSingle(ID_LOADER_STATUS, force))
                .subscribe(this::fillStatus, this::handleLoadFailure);
    }

    private void loadHeadReference(boolean force) {
        GitService service = ServiceFactory.get(GitService.class, force);

        PullRequestMarker head = mPullRequest.head();
        Repository repo = head.repo();
        Single<Optional<GitReference>> refSingle = repo != null && repo.owner() != null
                ? service.getGitReference(repo.owner().login(), repo.name(), head.ref())
                        .map(ApiHelpers::throwOnFailure)
                        .map(Optional::of)
                        .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, Optional.<GitReference> empty()))
                        .compose(makeLoaderSingle(ID_LOADER_HEAD_REF, force))
                : Single.just(Optional.empty());

        refSingle.subscribe(refOpt -> {
            mHeadReference = refOpt.orElse(null);
            mHasLoadedHeadReference = true;
            getActivity().invalidateOptionsMenu();
            bindSpecialViews(mListHeaderView);
        }, this::handleLoadFailure);
    }
}
