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

    public IssueEventHolder(Comment comment) {
        this.comment = comment;
        this.event = null;
        this.file = null;
    }
    public IssueEventHolder(CommitComment comment, CommitFile file) {
        this.comment = comment;
        this.event = null;
        this.file = file;
    }
    public IssueEventHolder(IssueEvent event) {
        this.comment = null;
        this.event = event;
        this.file = null;
    }

    public Date getCreatedAt() {
        return comment != null ? comment.getCreatedAt() : event.getCreatedAt();
    }

    public User getUser() {
        if (comment != null) {
            return comment.getUser();
        }
        return event.getAssigner() != null ? event.getAssigner() : event.getActor();
    }
}
