/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.github.core;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.eclipse.egit.github.core.util.DateUtils;

/**
 * GitHub v2 issue model class.
 */
public class SearchIssue implements Serializable {

	private static final long serialVersionUID = 4853048031771824016L;

	private Date createdAt;

	private Date updatedAt;

	private int comments;

	private int number;

	private int position;

	private int votes;

	private List<String> labels;

	private String body;

	private String gravatarId;

	private String htmlUrl;

	private String state;

	private String title;

	private String user;

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
	public SearchIssue setCreatedAt(Date createdAt) {
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
	public SearchIssue setUpdatedAt(Date updatedAt) {
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
	public SearchIssue setComments(int comments) {
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
	public SearchIssue setNumber(int number) {
		this.number = number;
		return this;
	}

	/**
	 * @return position
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @param position
	 * @return this issue
	 */
	public SearchIssue setPosition(int position) {
		this.position = position;
		return this;
	}

	/**
	 * @return votes
	 */
	public int getVotes() {
		return votes;
	}

	/**
	 * @param votes
	 * @return this issue
	 */
	public SearchIssue setVotes(int votes) {
		this.votes = votes;
		return this;
	}

	/**
	 * @return labels
	 */
	public List<String> getLabels() {
		return labels;
	}

	/**
	 * @param labels
	 * @return this issue
	 */
	public SearchIssue setLabels(List<String> labels) {
		this.labels = labels;
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
	public SearchIssue setBody(String body) {
		this.body = body;
		return this;
	}

	/**
	 * @return gravatarId
	 */
	public String getGravatarId() {
		return gravatarId;
	}

	/**
	 * @param gravatarId
	 * @return this issue
	 */
	public SearchIssue setGravatarId(String gravatarId) {
		this.gravatarId = gravatarId;
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
	public SearchIssue setHtmlUrl(String htmlUrl) {
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
	public SearchIssue setState(String state) {
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
	public SearchIssue setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * @return user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user
	 * @return this issue
	 */
	public SearchIssue setUser(String user) {
		this.user = user;
		return this;
	}
}
