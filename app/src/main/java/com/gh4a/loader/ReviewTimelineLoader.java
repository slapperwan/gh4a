package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ReviewTimelineLoader extends BaseLoader<List<TimelineItem>> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final int mPullRequestNumber;
    private final long mReviewId;

    public ReviewTimelineLoader(Context context, String repoOwner, String repoName,
            int pullRequestNumber, long reviewId) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
        mReviewId = reviewId;
    }

    @Override
    protected List<TimelineItem> doLoadInBackground() throws ApiRequestException {
        final Gh4Application app = Gh4Application.get();
        final PullRequestService prService = app.getGitHubService(PullRequestService.class);
        final PullRequestReviewService reviewService =
                app.getGitHubService(PullRequestReviewService.class);
        final PullRequestReviewCommentService commentService =
                app.getGitHubService(PullRequestReviewCommentService.class);

        Review review = ApiHelpers.throwOnFailure(reviewService.getReview(
                mRepoOwner, mRepoName, mPullRequestNumber, mReviewId).blockingGet());
        TimelineItem.TimelineReview timelineReview = new TimelineItem.TimelineReview(review);

        List<ReviewComment> reviewComments = ApiHelpers.PageIterator
                .toSingle(page -> reviewService.getReviewComments(
                        mRepoOwner, mRepoName, mPullRequestNumber, mReviewId))
                .blockingGet();

        if (!reviewComments.isEmpty()) {
            Collections.sort(reviewComments, ApiHelpers.COMMENT_COMPARATOR);

            List<GitHubFile> files = ApiHelpers.PageIterator
                    .toSingle(page -> prService.getPullRequestFiles(
                            mRepoOwner, mRepoName, mPullRequestNumber, page))
                    .blockingGet();
            HashMap<String, GitHubFile> filesByName = new HashMap<>();
            for (GitHubFile file : files) {
                filesByName.put(file.filename(), file);
            }

            // Add all of the review comments to the review item creating necessary diff hunks
            for (ReviewComment reviewComment : reviewComments) {
                GitHubFile file = filesByName.get(reviewComment.path());
                timelineReview.addComment(reviewComment, file, true);
            }

            List<ReviewComment> comments = ApiHelpers.PageIterator
                    .toSingle(page -> commentService.getPullRequestComments(
                            mRepoOwner, mRepoName, mPullRequestNumber, page))
                    .blockingGet();

            Collections.sort(comments, ApiHelpers.COMMENT_COMPARATOR);

            for (ReviewComment commitComment : comments) {
                if (reviewComments.contains(commitComment)) {
                    continue;
                }

                // Rest of the comments should be added only if they are under the same diff hunks
                // as the original review comments.
                GitHubFile file = filesByName.get(commitComment.path());
                timelineReview.addComment(commitComment, file, false);
            }
        }

        List<TimelineItem> items = new ArrayList<>();
        items.add(timelineReview);

        List<TimelineItem.Diff> diffHunks = new ArrayList<>(timelineReview.getDiffHunks());
        Collections.sort(diffHunks);

        for (TimelineItem.Diff diffHunk : diffHunks) {
            items.add(diffHunk);
            for (TimelineItem.TimelineComment comment : diffHunk.comments) {
                items.add(comment);
            }

            if (!diffHunk.isReply()) {
                items.add(new TimelineItem.Reply(diffHunk.getInitialTimelineComment()));
            }
        }

        return items;
    }
}
