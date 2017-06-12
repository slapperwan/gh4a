package com.gh4a.loader;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.Review;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TimelineItem implements Serializable {
    public static class TimelineComment extends TimelineItem {
        private static final Pattern PULL_REQUEST_PATTERN =
                Pattern.compile(".+/repos/(.+)/(.+)/pulls/(\\d+)");

        @NonNull
        public final Comment comment;

        @Nullable
        public final CommitFile file;

        public TimelineComment(@NonNull Comment comment) {
            this.comment = comment;
            this.file = null;
        }

        public TimelineComment(@NonNull CommitComment commitComment, @NonNull CommitFile file) {
            this.comment = commitComment;
            this.file = file;
        }

        @Nullable
        public CommitComment getCommitComment() {
            return comment instanceof CommitComment ? (CommitComment) comment : null;
        }

        @Override
        public Date getCreatedAt() {
            return comment.getCreatedAt();
        }

        @Nullable
        public Intent makeDiffIntent(Context context) {
            CommitComment commitComment = getCommitComment();

            if (file == null || commitComment == null) {
                return null;
            }

            Matcher matcher = PULL_REQUEST_PATTERN.matcher(commitComment.getPullRequestUrl());
            if (matcher.matches()) {
                String repoOwner = matcher.group(1);
                String repoName = matcher.group(2);
                int pullRequestNumber = Integer.parseInt(matcher.group(3));

                return PullRequestDiffViewerActivity.makeIntent(context, repoOwner,
                        repoName, pullRequestNumber, commitComment.getCommitId(),
                        commitComment.getPath(), file.getPatch(), null, commitComment.getPosition(),
                        -1, -1, false, new IntentUtils.InitialCommentMarker(commitComment.getId()));
            }

            return null;
        }
    }

    public static class TimelineEvent extends TimelineItem {
        @NonNull
        public final IssueEvent event;

        public TimelineEvent(@NonNull IssueEvent event) {
            this.event = event;
        }

        @Override
        public Date getCreatedAt() {
            return event.getCreatedAt();
        }
    }

    public static class TimelineReview extends TimelineItem {
        @NonNull
        public final Review review;

        @NonNull
        public final Map<String, Diff> chunks = new HashMap<>();

        public TimelineReview(@NonNull Review review) {
            this.review = review;
        }

        @Override
        public Date getCreatedAt() {
            return review.getSubmittedAt();
        }
    }

    public static class Diff extends TimelineItem implements Comparable<Diff> {
        @NonNull
        public final List<TimelineComment> comments = new ArrayList<>();

        @NonNull
        public TimelineComment getInitialTimelineComment() {
            TimelineComment timelineComment = comments.get(0);

            if (timelineComment == null) {
                throw new AssertionError("Missing required initial comment.");
            }

            return timelineComment;
        }

        @NonNull
        public CommitComment getInitialComment() {
            TimelineComment comment = getInitialTimelineComment();

            if (comment.getCommitComment() == null) {
                throw new AssertionError("Missing required initial commit comment.");
            }

            return comment.getCommitComment();
        }

        @Override
        public Date getCreatedAt() {
            return getInitialComment().getCreatedAt();
        }

        @Override
        public int compareTo(@NonNull Diff other) {
            CommitComment comment = getInitialComment();
            CommitComment otherComment = other.getInitialComment();

            // First sort by filename
            int byPath = comment.getPath().compareTo(otherComment.getPath());
            if (byPath != 0) {
                return byPath;
            }

            // Then by line numbers
            if (comment.getOriginalPosition() < otherComment.getOriginalPosition()) {
                return -1;
            }
            if (comment.getOriginalPosition() > otherComment.getOriginalPosition()) {
                return 1;
            }

            Date createdAt = getCreatedAt();
            Date otherCreatedAt = other.getCreatedAt();

            if (createdAt == null && otherCreatedAt == null) {
                // TODO: Figure out how to sort hunks if both are for pending reviews.
                return 0;
            }

            // Null created date means that the diff is for a pending review, place these after
            // other diffs.
            if (createdAt == null) {
                return 1;
            }
            if (otherCreatedAt == null) {
                return -1;
            }

            return createdAt.compareTo(otherCreatedAt);
        }
    }

    public static class Reply extends TimelineItem {
        @Override
        public Date getCreatedAt() {
            return null;
        }
    }

    @Nullable
    public abstract Date getCreatedAt();
}
