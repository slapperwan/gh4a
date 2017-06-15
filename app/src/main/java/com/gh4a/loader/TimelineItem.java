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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TimelineItem implements Serializable {
    public static class TimelineComment extends TimelineItem {
        private static final Pattern PULL_REQUEST_PATTERN =
                Pattern.compile(".*github\\.com/repos/([^/]+)/([^/]+)/pulls/(\\d+)");

        @NonNull
        public final Comment comment;

        @Nullable
        public final CommitFile file;

        public TimelineComment(@NonNull Comment comment) {
            this.comment = comment;
            this.file = null;
        }

        public TimelineComment(@NonNull CommitComment commitComment, @Nullable CommitFile file) {
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
                        commitComment.getPath(), file.getPatch(), null, -1, -1, -1, false,
                        new IntentUtils.InitialCommentMarker(commitComment.getId()));
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
        private final Map<String, Diff> diffHunksBySpecialId = new HashMap<>();

        public TimelineReview(@NonNull Review review) {
            this.review = review;
        }

        @Override
        public Date getCreatedAt() {
            return review.getSubmittedAt();
        }

        public Collection<Diff> getDiffHunks() {
            return diffHunksBySpecialId.values();
        }

        /**
         * Add the specified commend and it's matching commit file to correct diff hunk inside of
         * this review.
         * NOTE: This method expects to be called first with comments from this review in a sorted
         * order.
         *
         * @param comment The comment to add to this review.
         * @param file The commit file in which the comment was created.
         * @param addNewDiffHunk {@code true} if new diff hunk should be created if it's not found.
         */
        public void addComment(@NonNull CommitComment comment, @Nullable CommitFile file,
                boolean addNewDiffHunk) {
            // Comments are grouped by a special diff hunk id which is a combination of these 3
            // fields. By using this id we can display comments and their replies together under
            // a single diff hunk.
            // NOTE: Using this id is not correct in all of the possible cases (Comments created
            // with "Start new conversation" are incorrect). Sadly the GitHub API doesn't provide
            // better information than that so this is all that we can rely on.
            String id = comment.getOriginalCommitId() + comment.getPath() +
                    comment.getOriginalPosition();

            Diff diffHunk = diffHunksBySpecialId.get(id);
            if (diffHunk == null) {
                if (addNewDiffHunk) {
                    diffHunk = new Diff(new TimelineComment(comment, file));
                    diffHunksBySpecialId.put(id, diffHunk);
                }
            } else {
                if (diffHunk.isReply()) {
                    // To match how GitHub web interface works diff hunks with this property set to
                    // true should display only the initial comments.
                    return;
                }

                if (diffHunk.getCreatedAt() != null &&
                        diffHunk.getCreatedAt().after(comment.getCreatedAt())) {
                    // Because we are adding all of the comments in order based on their creation
                    // date and also first add only comments from the initial review we know that
                    // if something comes out of order then our initial comment was a reply.
                    diffHunk.setIsReply(true);
                } else {
                    diffHunk.comments.add(new TimelineComment(comment, file));
                }
            }
        }
    }

    public static class Diff extends TimelineItem implements Comparable<Diff> {
        @NonNull
        public final List<TimelineComment> comments = new ArrayList<>();

        private boolean mIsReply;

        public Diff(TimelineComment timelineComment) {
            comments.add(timelineComment);
        }

        /**
         * Indicates whether this diff hunk corresponds to an initial review comment which was
         * created as a reply to different review comment.
         * <p>
         * This property is used to match how GitHub web interface works. These comments are special
         * because for them reply button is not displayed and they are the only comments that should
         * be visible in the corresponding diff hunk.
         */
        public boolean isReply() {
            return mIsReply;
        }

        /**
         * Mark that this diff hunk corresponds to an initial review comment that was created as
         * reply to different review comment.
         */
        public void setIsReply(boolean isReply) {
            mIsReply = isReply;
        }

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
        @NonNull
        public final TimelineComment timelineComment;

        public Reply(@NonNull TimelineComment timelineComment) {
            this.timelineComment = timelineComment;
        }

        @Override
        public Date getCreatedAt() {
            return null;
        }
    }

    @Nullable
    public abstract Date getCreatedAt();
}
