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
import android.support.annotation.AttrRes;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AlertDialog;
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
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.gh4a.widget.PullRequestBranchInfoView;
import com.gh4a.widget.CommitStatusBox;

import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.Issue;
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
import com.meisolsson.githubsdk.service.git.GitService;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;
import com.meisolsson.githubsdk.service.issues.IssueEventService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;
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
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.Response;

public class PullRequestFragment extends IssueFragmentBase {
    private static final int ID_LOADER_STATUS = 1;
    private static final int ID_LOADER_HEAD_REF = 2;

    private PullRequest mPullRequest;
    private GitReference mHeadReference;
    private boolean mHasLoadedHeadReference;

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        if (!mHasLoadedHeadReference) {
            return;
        }

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
        } else {
            setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
        }
    }

   private void fillStatus(List<Status> statuses) {
       CommitStatusBox commitStatusBox = mListHeaderView.findViewById(R.id.commit_status_box);
       commitStatusBox.fillStatus(statuses, mPullRequest.mergeableState());
   }

    @Override
    protected Single<List<TimelineItem>> onCreateDataSingle(boolean bypassCache) {
        final int issueNumber = mIssue.number();
        final IssueEventService eventService =
                ServiceFactory.get(IssueEventService.class, bypassCache);
        final IssueCommentService commentService =
                ServiceFactory.get(IssueCommentService.class, bypassCache);

        final PullRequestService prService = ServiceFactory.get(PullRequestService.class, bypassCache);
        final PullRequestReviewService reviewService =
                ServiceFactory.get(PullRequestReviewService.class, bypassCache);
        final PullRequestReviewCommentService prCommentService =
                ServiceFactory.get(PullRequestReviewCommentService.class, bypassCache);

        Single<List<TimelineItem>> issueCommentItemSingle = ApiHelpers.PageIterator
                .toSingle(page -> commentService.getIssueComments(mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.mapList(TimelineItem.TimelineComment::new));
        Single<List<TimelineItem>> eventItemSingle = ApiHelpers.PageIterator
                .toSingle(page -> eventService.getIssueEvents(mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.filter(event -> INTERESTING_EVENTS.contains(event.event())))
                .compose((RxUtils.mapList(TimelineItem.TimelineEvent::new)));
        Single<Map<String, GitHubFile>> filesByNameSingle = ApiHelpers.PageIterator
                .toSingle(page -> prService.getPullRequestFiles(mRepoOwner, mRepoName, issueNumber, page))
                .map(files -> {
                    Map<String, GitHubFile> filesByName = new HashMap<>();
                    for (GitHubFile file : files) {
                        filesByName.put(file.filename(), file);
                    }
                    return filesByName;
                })
                .cache(); // single is used multiple times -> avoid refetching data

        Single<List<Review>> reviewSingle = ApiHelpers.PageIterator
                .toSingle(page -> reviewService.getReviews(mRepoOwner, mRepoName, issueNumber, page))
                .cache(); // single is used multiple times -> avoid refetching data
        Single<List<ReviewComment>> prCommentSingle = ApiHelpers.PageIterator
                .toSingle(page -> prCommentService.getPullRequestComments(
                        mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.sortList(ApiHelpers.COMMENT_COMPARATOR))
                .cache(); // single is used multiple times -> avoid refetching data

        Single<LongSparseArray<List<ReviewComment>>> reviewCommentsByIdSingle = reviewSingle
                .compose(RxUtils.filter(r -> r.state() == ReviewState.Pending))
                .toObservable()
                .flatMap(reviews -> {
                    List<Observable<Pair<Long, List<ReviewComment>>>> obsList = new ArrayList<>();
                    for (Review r : reviews) {
                        Single<List<ReviewComment>> single = ApiHelpers.PageIterator
                                .toSingle(page -> reviewService.getReviewComments(mRepoOwner,
                                        mRepoName, issueNumber, r.id()));
                        obsList.add(Single.zip(Single.just(r.id()), single, Pair::create)
                                .toObservable());
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

        Single<List<TimelineItem.TimelineReview>> reviewTimelineSingle = Single.zip(
                reviewSingle, filesByNameSingle, prCommentSingle, reviewCommentsByIdSingle,
                (prReviews, filesByName, commitComments, pendingCommentsById) -> {
            LongSparseArray<TimelineItem.TimelineReview> reviewsById = new LongSparseArray<>();
            List<TimelineItem.TimelineReview> reviews = new ArrayList<>();

            for (Review review : prReviews) {
                TimelineItem.TimelineReview timelineReview = new TimelineItem.TimelineReview(review);
                reviewsById.put(review.id(), timelineReview);
                reviews.add(timelineReview);

                if (review.state() == ReviewState.Pending) {
                    for (ReviewComment pendingComment : pendingCommentsById.get(review.id())) {
                        GitHubFile commitFile = filesByName.get(pendingComment.path());
                        timelineReview.addComment(pendingComment, commitFile, true);
                    }
                }
            }

            Map<String, TimelineItem.TimelineReview> reviewsBySpecialId = new HashMap<>();

            for (ReviewComment commitComment : commitComments) {
                GitHubFile file = filesByName.get(commitComment.path());
                if (commitComment.pullRequestReviewId() != 0) {
                    String id = TimelineItem.Diff.getDiffHunkId(commitComment);

                    TimelineItem.TimelineReview review = reviewsBySpecialId.get(id);
                    if (review == null) {
                        review = reviewsById.get(commitComment.pullRequestReviewId());
                        reviewsBySpecialId.put(id, review);
                    }

                    review.addComment(commitComment, file, true);
                }
            }

            return reviews;
        })
        .compose(RxUtils.filter(item -> {
            //noinspection CodeBlock2Expr
            return item.review().state() != ReviewState.Commented
                    || !TextUtils.isEmpty(item.review().body())
                    || !item.getDiffHunks().isEmpty();
        }));

        Single<List<TimelineItem.TimelineComment>> commitCommentWithoutReviewSingle = Single.zip(
                prCommentSingle.compose(RxUtils.filter(comment -> comment.pullRequestReviewId() == 0)),
                filesByNameSingle,
                (comments, files) -> {
                    List<TimelineItem.TimelineComment> items = new ArrayList<>();
                    for (ReviewComment comment : comments) {
                        items.add(new TimelineItem.TimelineComment(comment, files.get(comment.path())));
                    }
                    return items;
                });

        return Single.zip(
                issueCommentItemSingle,
                eventItemSingle,
                reviewTimelineSingle,
                commitCommentWithoutReviewSingle,
                (comments, events, reviewItems, commentsWithoutReview) -> {
            ArrayList<TimelineItem> result = new ArrayList<>();
            result.addAll(comments);
            result.addAll(events);
            result.addAll(reviewItems);
            result.addAll(commentsWithoutReview);
            Collections.sort(result, TimelineItem.COMPARATOR);
            return result;
        });
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
        int message = restore ? R.string.restore_branch_question : R.string.delete_branch_question;
        int buttonText = restore ? R.string.restore : R.string.delete;
        new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(buttonText, (dialog, which) -> {
                    if (restore) {
                        restorePullRequestBranch();
                    } else {
                        deletePullRequestBranch();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
                }, error -> {});
    }

    private void deletePullRequestBranch() {
        GitService service = ServiceFactory.get(GitService.class, false);

        PullRequestMarker head = mPullRequest.head();
        String owner = head.repo().owner().login();
        String repo = head.repo().name();

        service.deleteGitReference(owner, repo, "heads/" + head.ref())
                .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(getBaseActivity(),
                        R.string.deleting_msg, R.string.delete_branch_error))
                .subscribe(result -> {
                    mHeadReference = null;
                    onHeadReferenceUpdated();
                }, error -> {});
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

        RepositoryStatusService service = ServiceFactory.get(RepositoryStatusService.class, force);
        String sha = mPullRequest.head().sha();

        ApiHelpers.PageIterator
                    .toSingle(page -> service.getStatuses(mRepoOwner, mRepoName, sha, page))
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
                    .compose(RxUtils.sortList(STATUS_AND_CONTEXT_COMPARATOR))
                    .compose(makeLoaderSingle(ID_LOADER_STATUS, force))
                    .subscribe(this::fillStatus, error -> {});
    }

    private void loadHeadReference(boolean force) {
        GitService service = ServiceFactory.get(GitService.class, force);

        PullRequestMarker head = mPullRequest.head();
        Repository repo = head.repo();
        Single<Optional<GitReference>> refSingle = repo == null
                ? Single.just(Optional.absent())
                : service.getGitReference(repo.owner().login(), repo.name(), head.ref())
                        .map(ApiHelpers::throwOnFailure)
                        .map(Optional::of)
                        .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, Optional.<GitReference>absent()))
                        .compose(makeLoaderSingle(ID_LOADER_HEAD_REF, force));

        refSingle.subscribe(refOpt -> {
            mHeadReference = refOpt.orNull();
            mHasLoadedHeadReference = true;
            getActivity().invalidateOptionsMenu();
            bindSpecialViews(mListHeaderView);
        }, error -> {});
    }
}
