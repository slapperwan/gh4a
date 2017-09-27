/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import org.eclipse.egit.github.core.util.ObjectUtils;

/**
 * Pull request model class.
 */
public class PullRequest implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 7858604768525096763L;

	public static final String MERGEABLE_STATE_BEHIND = "behind";
	public static final String MERGEABLE_STATE_BLOCKED = "blocked";
	public static final String MERGEABLE_STATE_CLEAN = "clean";
	public static final String MERGEABLE_STATE_DIRTY = "dirty";
	public static final String MERGEABLE_STATE_UNSTABLE = "unstable";
	public static final String MERGEABLE_STATE_UNKNOWN = "unknown";

	private boolean mergeable;

	@SerializedName("mergeable_state")
	private String mergeableState;

	private boolean merged;

	private Date closedAt;

	private Date mergedAt;

	private Date updatedAt;

	private Date createdAt;

	private long id;

	private int additions;

	private int changedFiles;

	private int comments;

	private int reviewComments;

	private int commits;

	private int deletions;

	private int number;

	private Milestone milestone;

	private PullRequestMarker base;

	private PullRequestMarker head;

	private String body;

	private String bodyHtml;

	private String bodyText;

	private String diffUrl;

	private String htmlUrl;

	private String issueUrl;

	private String patchUrl;

	private String state;

	private String title;

	private String url;

	private List<User> assignees;

	private User mergedBy;

	private User user;

	private boolean locked;

	/**
	 * @return mergeable
	 */
	public boolean isMergeable() {
		return mergeable;
	}

	/**
	 * @param mergeable
	 * @return this pull request
	 */
	public PullRequest setMergeable(boolean mergeable) {
		this.mergeable = mergeable;
		return this;
	}

	/**
	 * @return mergeableState
	 */
	public String getMergeableState() {
		return mergeableState;
	}

	/**
	 * @param mergeableState
	 * @return this pull request
	 */
	public PullRequest setMergeableState(String mergeableState) {
		this.mergeableState = mergeableState;
		return this;
	}

	/**
	 * @return merged
	 */
	public boolean isMerged() {
		return merged;
	}

	/**
	 * @param merged
	 * @return this pull request
	 */
	public PullRequest setMerged(boolean merged) {
		this.merged = merged;
		return this;
	}

	/**
	 * @return closedAt
	 */
	public Date getClosedAt() {
		return ObjectUtils.cloneDate(closedAt);
	}

	/**
	 * @param closedAt
	 * @return this pull request
	 */
	public PullRequest setClosedAt(Date closedAt) {
		this.closedAt = ObjectUtils.cloneDate(closedAt);
		return this;
	}

	/**
	 * @return mergedAt
	 */
	public Date getMergedAt() {
		return ObjectUtils.cloneDate(mergedAt);
	}

	/**
	 * @param mergedAt
	 * @return this pull request
	 */
	public PullRequest setMergedAt(Date mergedAt) {
		this.mergedAt = ObjectUtils.cloneDate(mergedAt);
		return this;
	}

	/**
	 * @return updatedAt
	 */
	public Date getUpdatedAt() {
		return ObjectUtils.cloneDate(updatedAt);
	}

	/**
	 * @param updatedAt
	 * @return this pull request
	 */
	public PullRequest setUpdatedAt(Date updatedAt) {
		this.updatedAt = ObjectUtils.cloneDate(updatedAt);
		return this;
	}

	/**
	 * @return createdAt
	 */
	public Date getCreatedAt() {
		return ObjectUtils.cloneDate(createdAt);
	}

	/**
	 * @param createdAt
	 * @return this pull request
	 */
	public PullRequest setCreatedAt(Date createdAt) {
		this.createdAt = ObjectUtils.cloneDate(createdAt);
		return this;
	}

	/**
	 * @return additions
	 */
	public int getAdditions() {
		return additions;
	}

	/**
	 * @param additions
	 * @return this pull request
	 */
	public PullRequest setAdditions(int additions) {
		this.additions = additions;
		return this;
	}

	/**
	 * @return changedFiles
	 */
	public int getChangedFiles() {
		return changedFiles;
	}

	/**
	 * @param changedFiles
	 * @return this pull request
	 */
	public PullRequest setChangedFiles(int changedFiles) {
		this.changedFiles = changedFiles;
		return this;
	}

	/**
	 * @return comments
	 */
	public int getComments() {
		return comments;
	}

	/**
	 * @param comments
	 * @return this pull request
	 */
	public PullRequest setComments(int comments) {
		this.comments = comments;
		return this;
	}

	/**
	 * @return number of inline comments on the diff in the pull request
	 */
	public int getReviewComments()
	{
		return reviewComments;
	}

	/**
	 * @param reviewComments {@link #getReviewComments()}
	 * @return this pull request
	 */
	public PullRequest setReviewComments(int reviewComments)
	{
		this.reviewComments = reviewComments;
		return this;
	}

	/**
	 * @return commits
	 */
	public int getCommits() {
		return commits;
	}

	/**
	 * @param commits
	 * @return this pull request
	 */
	public PullRequest setCommits(int commits) {
		this.commits = commits;
		return this;
	}

	/**
	 * @return deletions
	 */
	public int getDeletions() {
		return deletions;
	}

	/**
	 * @param deletions
	 * @return this pull request
	 */
	public PullRequest setDeletions(int deletions) {
		this.deletions = deletions;
		return this;
	}

	/**
	 * @return number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @param number
	 * @return this pull request
	 */
	public PullRequest setNumber(int number) {
		this.number = number;
		return this;
	}

	/**
	 * @return base
	 */
	public PullRequestMarker getBase() {
		return base;
	}

	/**
	 * @param base
	 * @return this pull request
	 */
	public PullRequest setBase(PullRequestMarker base) {
		this.base = base;
		return this;
	}

	/**
	 * @return head
	 */
	public PullRequestMarker getHead() {
		return head;
	}

	/**
	 * @param head
	 * @return this pull request
	 */
	public PullRequest setHead(PullRequestMarker head) {
		this.head = head;
		return this;
	}

	/**
	 * @return body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * @param body
	 * @return this pull request
	 */
	public PullRequest setBody(String body) {
		this.body = body;
		return this;
	}

	/**
	 * @return bodyHtml
	 */
	public String getBodyHtml() {
		return bodyHtml;
	}

	/**
	 * @param bodyHtml
	 * @return this pull request
	 */
	public PullRequest setBodyHtml(String bodyHtml) {
		this.bodyHtml = bodyHtml;
		return this;
	}

	/**
	 * @return bodyText
	 */
	public String getBodyText() {
		return bodyText;
	}

	/**
	 * @param bodyText
	 * @return this pull request
	 */
	public PullRequest setBodyText(String bodyText) {
		this.bodyText = bodyText;
		return this;
	}

	/**
	 * @return diffUrl
	 */
	public String getDiffUrl() {
		return diffUrl;
	}

	/**
	 * @param diffUrl
	 * @return this pull request
	 */
	public PullRequest setDiffUrl(String diffUrl) {
		this.diffUrl = diffUrl;
		return this;
	}

	/**
	 * @return htmlUrl
	 */
	public String getHtmlUrl() {
		return htmlUrl;
	}

	/**
	 * @param htmlUrl
	 * @return this pull request
	 */
	public PullRequest setHtmlUrl(String htmlUrl) {
		this.htmlUrl = htmlUrl;
		return this;
	}

	/**
	 * @return issueUrl
	 */
	public String getIssueUrl() {
		return issueUrl;
	}

	/**
	 * @param issueUrl
	 * @return this pull request
	 */
	public PullRequest setIssueUrl(String issueUrl) {
		this.issueUrl = issueUrl;
		return this;
	}

	/**
	 * @return patchUrl
	 */
	public String getPatchUrl() {
		return patchUrl;
	}

	/**
	 * @param patchUrl
	 * @return this pull request
	 */
	public PullRequest setPatchUrl(String patchUrl) {
		this.patchUrl = patchUrl;
		return this;
	}

	/**
	 * @return state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state
	 * @return this pull request
	 */
	public PullRequest setState(String state) {
		this.state = state;
		return this;
	}

	/**
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 * @return this pull request
	 */
	public PullRequest setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * @return url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 * @return this pull request
	 */
	public PullRequest setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * @return mergedBy
	 */
	public User getMergedBy() {
		return mergedBy;
	}

	/**
	 * @param mergedBy
	 * @return this pull request
	 */
	public PullRequest setMergedBy(User mergedBy) {
		this.mergedBy = mergedBy;
		return this;
	}

	/**
	 * @return user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * @param user
	 * @return this pull request
	 */
	public PullRequest setUser(User user) {
		this.user = user;
		return this;
	}

	/**
	 * @return id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id
	 * @return this pull request
	 */
	public PullRequest setId(long id) {
		this.id = id;
		return this;
	}

	/**
	 * @return milestone
	 */
	public Milestone getMilestone() {
		return milestone;
	}

	/**
	 * @param milestone
	 * @return this pull request
	 */
	public PullRequest setMilestone(Milestone milestone) {
		this.milestone = milestone;
		return this;
	}

	/**
	 * @return assignees
	 */
	public List<User> getAssignees() {
		return assignees;
	}

	/**
	 * @param assignees
	 * @return this pull request
	 */
	public PullRequest setAssignees(List<User> assignees) {
		this.assignees = assignees;
		return this;
	}


	/**
	 * @return locked
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * @param locked
	 * @return this issue
	 */
	public PullRequest setLocked(boolean locked) {
		this.locked = locked;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof PullRequest) {
			return this.id == ((PullRequest) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.hashCodeForLong(this.id);
	}

	@Override
	public String toString() {
		return "Pull Request " + number; //$NON-NLS-1$
	}
}
