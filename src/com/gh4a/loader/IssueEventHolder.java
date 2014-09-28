package com.gh4a.loader;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.User;

import java.util.Date;

public class IssueEventHolder {
    public final Comment comment;
    public final IssueEvent event;

    public IssueEventHolder(Comment comment) {
        this.comment = comment;
        this.event = null;
    }
    public IssueEventHolder(IssueEvent event) {
        this.comment = null;
        this.event = event;
    }

    public Date getCreatedAt() {
        return comment != null ? comment.getCreatedAt() : event.getCreatedAt();
    }

    public User getUser() {
        return comment != null ? comment.getUser() : event.getActor();
    }
}
