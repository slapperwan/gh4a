package org.eclipse.egit.github.core;

import org.eclipse.egit.github.core.util.ObjectUtils;

import java.io.Serializable;
import java.util.Date;

public class Review implements Serializable {

    public static final String STATE_COMMENTED = "COMMENTED";
    public static final String STATE_APPROVED = "APPROVED";
    public static final String STATE_PENDING = "PENDING";
    public static final String STATE_DISMISSED = "DISMISSED";

    private long id;

    private User user;

    private String body;

    private String state;

    private String htmlUrl;

    private String pullRequestUrl;

    private Date submittedAt;

    private String commitId;

    public long getId() {
        return id;
    }

    public Review setId(long id) {
        this.id = id;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Review setUser(User user) {
        this.user = user;
        return this;
    }

    public String getBody() {
        return body;
    }

    public Review setBody(String body) {
        this.body = body;
        return this;
    }

    public String getState() {
        return state;
    }

    public Review setState(String state) {
        this.state = state;
        return this;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public Review setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
        return this;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public Review setPullRequestUrl(String pullRequestUrl) {
        this.pullRequestUrl = pullRequestUrl;
        return this;
    }

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public Review setSubmittedAt(Date submittedAt) {
        this.submittedAt = submittedAt;
        return this;
    }

    public String getCommitId() {
        return commitId;
    }

    public Review setCommitId(String commitId) {
        this.commitId = commitId;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Review && this.id == ((Review) other).id;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCodeForLong(this.id);
    }
}
