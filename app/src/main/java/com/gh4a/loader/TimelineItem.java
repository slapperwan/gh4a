package com.gh4a.loader;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.Review;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class TimelineItem {
    public static class TimelineComment extends TimelineItem {
        public final Comment comment;
        public final CommitFile file;

        public TimelineComment(Comment comment) {
            this.comment = comment;
            this.file = null;
        }

        public TimelineComment(Comment comment, CommitFile file) {
            this.comment = comment;
            this.file = file;
        }

        @Override
        public Date getCreatedAt() {
            return comment.getCreatedAt();
        }
    }

    public static class TimelineEvent extends TimelineItem {
        public final IssueEvent event;

        public TimelineEvent(IssueEvent event) {
            this.event = event;
        }

        @Override
        public Date getCreatedAt() {
            return event.getCreatedAt();
        }
    }

//    public static class TimelineCommitComment extends TimelineItem {
//        public final CommitComment comment;
//        public final CommitFile file;
//
//        public TimelineCommitComment(CommitComment comment, CommitFile file) {
//            this.comment = comment;
//            this.file = file;
//        }
//    }

    public static class TimelineReview extends TimelineItem {
        public final Review review;
        public final List<CommitComment> comments = new ArrayList<>();

        public TimelineReview(Review review) {
            this.review = review;
        }

        @Override
        public Date getCreatedAt() {
            return review.getSubmittedAt();
        }
    }

    public static class Diff extends TimelineItem {
        @Override
        public Date getCreatedAt() {
            return null;
        }
    }

    public static class Reply extends TimelineItem {
        @Override
        public Date getCreatedAt() {
            return null;
        }
    }

    public abstract Date getCreatedAt();
}
