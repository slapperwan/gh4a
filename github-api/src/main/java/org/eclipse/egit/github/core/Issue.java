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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.egit.github.core.util.DateUtils;

/**
 * GitHub issue model class.
 */
public class Issue implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 6358575015023539051L;

	private long id;

	private Date closedAt;

	private Date createdAt;

	private Date updatedAt;

	private int comments;

	private int number;

	private List<Label> labels;

	private Milestone milestone;

	private PullRequest pullRequest;

	private String body;

	private String bodyHtml;

	private String bodyText;

	private String htmlUrl;

	private String state;

	private String title;

	private String url;

	private List<User> assignees;

	private User user;

	private User closedBy;

	private boolean locked;

	private Reactions reactions;

	/**
	 * @return closedAt
	 */
	public Date getClosedAt() {
		return DateUtils.clone(closedAt);
	}

	/**
	 * @param closedAt
	 * @return this issue
	 */
	public Issue setClosedAt(Date closedAt) {
		this.closedAt = DateUtils.clone(closedAt);
		return this;
	}

	/**
	 * @return createdAt
	 */
	public Date getCreatedAt() {
		return DateUtils.clone(createdAt);
	}

	/**
	 * @param createdAt
	 * @return this issue
	 */
	public Issue setCreatedAt(Date createdAt) {
		this.createdAt = DateUtils.clone(createdAt);
		return this;
	}

	/**
	 * @return updatedAt
	 */
	public Date getUpdatedAt() {
		return DateUtils.clone(updatedAt);
	}

	/**
	 * @param updatedAt
	 * @return this issue
	 */
	public Issue setUpdatedAt(Date updatedAt) {
		this.updatedAt = DateUtils.clone(updatedAt);
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
	 * @return this issue
	 */
	public Issue setComments(int comments) {
		this.comments = comments;
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
	 * @return this issue
	 */
	public Issue setNumber(int number) {
		this.number = number;
		return this;
	}

	/**
	 * @return labels
	 */
	public List<Label> getLabels() {
		return labels;
	}

	/**
	 * @param labels
	 * @return this issue
	 */
	public Issue setLabels(List<Label> labels) {
		this.labels = labels != null ? new ArrayList<Label>(labels) : null;
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
	 * @return this issue
	 */
	public Issue setMilestone(Milestone milestone) {
		this.milestone = milestone;
		return this;
	}

	/**
	 * @return pullRequest
	 */
	public PullRequest getPullRequest() {
		return pullRequest;
	}

	/**
	 * @param pullRequest
	 * @return this issue
	 */
	public Issue setPullRequest(PullRequest pullRequest) {
		this.pullRequest = pullRequest;
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
	 * @return this issue
	 */
	public Issue setBody(String body) {
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
	 * @return this issue
	 */
	public Issue setBodyHtml(String bodyHtml) {
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
	 * @return this issue
	 */
	public Issue setBodyText(String bodyText) {
		this.bodyText = bodyText;
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
	 * @return this issue
	 */
	public Issue setHtmlUrl(String htmlUrl) {
		this.htmlUrl = htmlUrl;
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
	 * @return this issue
	 */
	public Issue setState(String state) {
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
	 * @return this issue
	 */
	public Issue setTitle(String title) {
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
	 * @return this issue
	 */
	public Issue setUrl(String url) {
		this.url = url;
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
	 * @return this issue
	 */
	public Issue setAssignees(List<User> assignees) {
		this.assignees = assignees;
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
	 * @return this issue
	 */
	public Issue setUser(User user) {
		this.user = user;
		return this;
	}

	/**
	 * @return closedBy
	 */
	public User getClosedBy() {
		return closedBy;
	}

	/**
	 * @param closedBy
	 * @return this issue
	 */
	public Issue setClosedBy(User closedBy) {
		this.closedBy = closedBy;
		return this;
	}

	/**
	 * @return locked
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * @param id
	 * @return this issue
	 */
	public Issue setLocked(boolean locked) {
		this.locked = locked;
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
	 * @return this issue
	 */
	public Issue setId(long id) {
		this.id = id;
		return this;
	}

	public Reactions getReactions() {
		return reactions;
	}

	public Issue setReactions(Reactions reactions) {
		this.reactions = reactions;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Issue) {
			return this.id == ((Issue) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.id);
	}

	@Override
	public String toString() {
		return "Issue " + number; //$NON-NLS-1$
	}
}
