package com.gh4a.loader;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.User;

import java.util.Date;

public class IssueEventHolder {
    public final Comment comment;
    public final IssueEvent event;
    public final CommitFile file;
    public final boolean isPullRequestEvent;

    public IssueEventHolder(Comment comment, boolean isPullRequestEvent) {
        this.comment = comment;
        this.event = null;
        this.file = null;
        this.isPullRequestEvent = isPullRequestEvent;
    }
    public IssueEventHolder(CommitComment comment, CommitFile file) {
        this.comment = comment;
        this.event = null;
        this.file = file;
        this.isPullRequestEvent = true;
    }
    public IssueEventHolder(IssueEvent event, boolean isPullRequestEvent) {
        this.comment = null;
        this.event = event;
        this.file = null;
        this.isPullRequestEvent = isPullRequestEvent;
    }

    public Date getCreatedAt() {
        return comment != null ? comment.getCreatedAt() : event.getCreatedAt();
    }

    public Date getUpdatedAt() {
        return comment != null ? comment.getUpdatedAt() : event.getCreatedAt();
    }

    public User getUser() {
        if (comment != null) {
            return comment.getUser();
        }
        return event.getAssigner() != null ? event.getAssigner() : event.getActor();
    }
}
