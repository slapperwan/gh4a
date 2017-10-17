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

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.loader.CommitStatusLoader;
import com.gh4a.loader.IssueCommentListLoader;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
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
import com.philosophicalhacker.lib.RxLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.Response;

public class PullRequestFragment extends IssueFragmentBase {
    private static final int ID_LOADER_STATUS = 1;
    private static final int ID_LOADER_HEAD_REF = 2;

    private RxLoader mRxLoader;
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
        mRxLoader = new RxLoader(getActivity(), getLoaderManager());
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
    protected Single<List<TimelineItem>> onCreateDataSingle() {
        final Gh4Application app = Gh4Application.get();
        final int issueNumber = mIssue.number();
        final IssueEventService eventService = app.getGitHubService(IssueEventService.class);
        final IssueCommentService commentService = app.getGitHubService(IssueCommentService.class);

        final PullRequestService prService = app.getGitHubService(PullRequestService.class);
        final PullRequestReviewService reviewService =
                app.getGitHubService(PullRequestReviewService.class);
        final PullRequestReviewCommentService prCommentService =
                app.getGitHubService(PullRequestReviewCommentService.class);

        Single<List<TimelineItem>> issueCommentItemSingle = ApiHelpers.PageIterator
                .toSingle(page -> commentService.getIssueComments(mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.mapList(comment -> new TimelineItem.TimelineComment(comment)));
        Single<List<TimelineItem>> eventItemSingle = ApiHelpers.PageIterator
                .toSingle(page -> eventService.getIssueEvents(mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.filter(event -> IssueCommentListLoader.INTERESTING_EVENTS.contains(event.event())))
                .compose((RxUtils.mapList(event -> new TimelineItem.TimelineEvent(event))));
        Single<Map<String, GitHubFile>> filesByNameSingle = ApiHelpers.PageIterator
                .toSingle(page -> prService.getPullRequestFiles(mRepoOwner, mRepoName, issueNumber, page))
                .map(files -> {
                    HashMap<String, GitHubFile> filesByName = new HashMap<>();
                    for (GitHubFile file : files) {
                        filesByName.put(file.filename(), file);
                    }
                    return filesByName;
                });
        Single<List<Review>> reviewSingle = ApiHelpers.PageIterator
                .toSingle(page -> reviewService.getReviews(mRepoOwner, mRepoName, issueNumber, page));
        Single<List<ReviewComment>> prCommentSingle = ApiHelpers.PageIterator
                .toSingle(page -> prCommentService.getPullRequestComments(
                        mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.sortList(ApiHelpers.COMMENT_COMPARATOR));

        Single<LongSparseArray<List<ReviewComment>>> reviewCommentsByIdSingle = reviewSingle
                .compose(RxUtils.filter(r -> r.state() == ReviewState.Pending))
                .toObservable()
                .flatMap(reviews -> {
                    List<Observable<Pair<Long, List<ReviewComment>>>> obsList = new ArrayList<>();
                    for (Review r : reviews) {
                        Single<List<ReviewComment>> single = ApiHelpers.PageIterator
                                .toSingle(page -> reviewService.getReviewComments(mRepoOwner,
                                        mRepoName, issueNumber, r.id()));
                        obsList.add(Single.zip(Single.just(r.id()), single, (id, s) -> Pair.create(id, s))
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
            Collections.sort(result, IssueCommentListLoader.TIMELINE_ITEM_COMPARATOR);
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
                    Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
            return service.deleteComment(mRepoOwner, mRepoName, comment.id());
        } else {
            IssueCommentService service =
                    Gh4Application.get().getGitHubService(IssueCommentService.class);
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

        GitService service = Gh4Application.get().getGitHubService(GitService.class);
        CreateGitReference request = CreateGitReference.builder()
                .ref(head.ref())
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
        GitService service = Gh4Application.get().getGitHubService(GitService.class);

        PullRequestMarker head = mPullRequest.head();
        String owner = head.repo().owner().login();
        String repo = head.repo().name();

        service.deleteGitReference(owner, repo, head.ref())
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(getBaseActivity(),
                        R.string.deleting_msg, R.string.delete_branch_error))
                .subscribe(result -> {
                    mHeadReference = null;
                    onHeadReferenceUpdated();
                }, error -> {});
    }

    private void loadCommitStatusesIfOpen(boolean force) {
        if (mPullRequest.state() != IssueState.Open) {
            return;
        }

        CommitStatusLoader.load(mRepoOwner, mRepoName, mPullRequest.head().sha())
                .compose(RxUtils::doInBackground)
                .compose(getBaseActivity()::handleError)
                .toObservable()
                .compose(mRxLoader.makeObservableTransformer(ID_LOADER_STATUS, force))
                .subscribe(statuses -> fillStatus(statuses), error -> {});
    }

    private void loadHeadReference(boolean force) {
        GitService service = Gh4Application.get().getGitHubService(GitService.class);

        PullRequestMarker head = mPullRequest.head();
        Repository repo = head.repo();
        Single<GitReference> refSingle = repo == null
                ? Single.just(null)
                : service.getGitReference(repo.owner().login(), repo.name(), head.ref())
                        .map(ApiHelpers::throwOnFailure);

        refSingle.compose(RxUtils::doInBackground)
                .compose(getBaseActivity()::handleError)
                .toObservable()
                .compose(mRxLoader.makeObservableTransformer(ID_LOADER_HEAD_REF, force))
                .subscribe(ref -> {
                    mHeadReference = ref;
                    mHasLoadedHeadReference = true;
                    getActivity().invalidateOptionsMenu();
                    bindSpecialViews(mListHeaderView);
                }, error -> {});
    }
}
