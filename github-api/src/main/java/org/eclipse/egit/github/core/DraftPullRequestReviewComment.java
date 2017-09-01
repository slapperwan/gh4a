package org.eclipse.egit.github.core;

import java.io.Serializable;

public class DraftPullRequestReviewComment implements Serializable {
    private static final long serialVersionUID = -2754992759480082133L;

    private String path;
    private int position;
    private String body;

    public String getPath() {
        return path;
    }

    public DraftPullRequestReviewComment setPath(String path) {
        this.path = path;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public DraftPullRequestReviewComment setPosition(int position) {
        this.position = position;
        return this;
    }

    public String getBody() {
        return body;
    }

    public DraftPullRequestReviewComment setBody(String body) {
        this.body = body;
        return this;
    }
}
