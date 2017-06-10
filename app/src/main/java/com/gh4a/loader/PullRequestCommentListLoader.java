package com.gh4a.loader;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullRequestCommentListLoader extends IssueCommentListLoader {

    public PullRequestCommentListLoader(Context context, String repoOwner,
            String repoName, int issueNumber) {
        super(context, repoOwner, repoName, issueNumber, true);
    }

    @Override
    public List<TimelineItem> doLoadInBackground() throws IOException {
        // combine issue comments and pull request comments (to get comments on diff)
        List<TimelineItem> events = super.doLoadInBackground();

        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
        List<CommitComment> commitComments = pullRequestService.getComments(repoId, mIssueNumber);

//        HashMap<String, CommitFile> filesByName = new HashMap<>();
//        for (CommitFile file : pullRequestService.getFiles(repoId, mIssueNumber)) {
//            filesByName.put(file.getFilename(), file);
//        }
        Map<String, TimelineItem.TimelineReview> reviewsBySpecialId = new HashMap<>();
        LongSparseArray<TimelineItem.TimelineReview> reviewsById = new LongSparseArray<>();
        List<TimelineItem.TimelineReview> reviews = new ArrayList<>();
        for (Review review : pullRequestService.getReviews(repoId, mIssueNumber)) {
            TimelineItem.TimelineReview timelineReview = new TimelineItem.TimelineReview(review);
            reviewsById.put(review.getId(), timelineReview);
            reviews.add(timelineReview);
        }

        for (CommitComment commitComment : commitComments) {
            String id = commitComment.getOriginalCommitId() + commitComment.getPath() +
                    commitComment.getOriginalPosition();
            TimelineItem.TimelineReview review = reviewsBySpecialId.get(id);
            if (review == null) {
                review = reviewsById.get(commitComment.getPullRequestReviewId());
                reviewsBySpecialId.put(id, review);
            }

            List<CommitComment> idComments = review.comments.get(id);
            if (idComments == null) {
                idComments = new ArrayList<>();
                review.comments.put(id, idComments);
            }
            idComments.add(commitComment);
        }

        for (TimelineItem.TimelineReview review : reviews) {
            if (!review.review.getState().equals(Review.STATE_COMMENTED) ||
                    !TextUtils.isEmpty(review.review.getBody()) ||
                    review.comments.size() > 0) {
                events.add(review);
            }
        }

//        final Map<String, CommitComment> data = new HashMap<>();
//        for (CommitComment commitComment : commitComments) {
//            CommitFile file = filesByName.get(commitComment.getPath());
//            events.add(new TimelineItem.TimelineComment(commitComment, file));
//
//            String id = commitComment.getOriginalCommitId() + commitComment.getOriginalPosition();
//            if (data.containsKey(id)) {
//                if (data.get(id).getCreatedAt().after(commitComment.getCreatedAt())) {
//                    data.put(id, commitComment);
//                }
//            } else {
//                data.put(id, commitComment);
//            }
//        }
//
//        for (CommitComment commitComment : data.values()) {
//            for (Review review : reviews) {
//                if (commitComment.getPullRequestReviewId() == review.getId()) {
//                    events.add(new TimelineItem.TimelineReview(review));
//                    reviews.remove(review);
//                    break;
//                }
//            }
//        }
//
//        for (Review review : reviews) {
//            if (!TextUtils.isEmpty(review.getBody()) ||
//                    !review.getState().equals(Review.STATE_COMMENTED)) {
//                events.add(new TimelineItem.TimelineReview(review));
//            }
//        }

        Collections.sort(events, new Comparator<TimelineItem>() {
            @Override
            public int compare(TimelineItem lhs, TimelineItem rhs) {
                Comment leftComment = lhs instanceof TimelineItem.TimelineComment
                        ? ((TimelineItem.TimelineComment) lhs).comment
                        : null;
                Comment rightComment = rhs instanceof TimelineItem.TimelineComment
                        ? ((TimelineItem.TimelineComment) rhs).comment
                        : null;

                CommitComment leftCommitComment =
                        leftComment instanceof CommitComment ? (CommitComment) leftComment : null;
                CommitComment rightCommitComment =
                        rightComment instanceof CommitComment ? (CommitComment) rightComment : null;

//                if (leftCommitComment != null && rightCommitComment != null) {
//                    String leftId = leftCommitComment.getOriginalCommitId() +
//                            leftCommitComment.getOriginalPosition();
//                    String rightId = rightCommitComment.getOriginalCommitId() +
//                            rightCommitComment.getOriginalPosition();
//                    if (leftId.equals(rightId)) {
//                        return compareByTime(lhs, rhs);
//                    }
//
//                    return data.get(leftId).getCreatedAt()
//                            .compareTo(data.get(rightId).getCreatedAt());
//                }

                if (leftCommitComment != null && rhs instanceof TimelineItem.TimelineReview ||
                        lhs instanceof TimelineItem.TimelineReview && rightCommitComment != null) {
                    CommitComment comment =
                            leftCommitComment != null ? leftCommitComment : rightCommitComment;
                    Review review = ((TimelineItem.TimelineReview) (
                            lhs instanceof TimelineItem.TimelineReview ? lhs : rhs)).review;

                    String id = comment.getOriginalCommitId() + comment.getOriginalPosition();

//                    if (data.get(id).getPullRequestReviewId() == review.getId()) {
//                        return lhs instanceof TimelineItem.TimelineReview ? -1 : 1;
//                    }
//
//                    if (lhs instanceof TimelineItem.TimelineReview) {
//                        return Long.valueOf(review.getId())
//                                .compareTo(data.get(id).getPullRequestReviewId());
//                    }
//                    return Long.valueOf(data.get(id).getPullRequestReviewId())
//                            .compareTo(review.getId());
                }

                return compareByTime(lhs, rhs);
            }
        });

        List<String> addedDiffIds = new ArrayList<>();
        boolean previousWasDiffComment = false;

        int i = 0;
        while (i < events.size()) {
            TimelineItem item = events.get(i);
            if (item instanceof TimelineItem.TimelineComment) {
                TimelineItem.TimelineComment comment = (TimelineItem.TimelineComment) item;
                if (comment.comment instanceof CommitComment) {
                    CommitComment commitComment = (CommitComment) comment.comment;
                    String id = commitComment.getOriginalCommitId() +
                            commitComment.getOriginalPosition();

                    if (!addedDiffIds.contains(id)) {
                        addedDiffIds.add(id);
                        events.add(i, new TimelineItem.Diff());
                        if (previousWasDiffComment) {
                            events.add(i, new TimelineItem.Reply());
                            previousWasDiffComment = false;
                        }
                    } else {
                        previousWasDiffComment = true;
                    }
                } else if (previousWasDiffComment) {
                    events.add(i, new TimelineItem.Reply());
                    previousWasDiffComment = false;
                }
            } else if (previousWasDiffComment) {
                events.add(i, new TimelineItem.Reply());
                previousWasDiffComment = false;
            }

            i += 1;
        }

        if (previousWasDiffComment) {
            events.add(i, new TimelineItem.Reply());
        }

        return events;
    }

    private int compareByTime(TimelineItem lhs, TimelineItem rhs) {
        return lhs.getCreatedAt().compareTo(rhs.getCreatedAt());
    }
}
