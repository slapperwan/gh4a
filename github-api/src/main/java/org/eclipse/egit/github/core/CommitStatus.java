/******************************************************************************
 *  Copyright (c) 2012, 2015 GitHub Inc. and others.
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
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.egit.github.core.util.ObjectUtils;

/**
 * Status of a commit in a repository
 */
public class CommitStatus implements Serializable {

	private static final long serialVersionUID = -7701789812780758070L;

	/**
	 * Error state
	 */
	public static final String STATE_ERROR = "error"; //$NON-NLS-1$

	/**
	 * Failure state
	 */
	public static final String STATE_FAILURE = "failure"; //$NON-NLS-1$

	/**
	 * Pending state
	 */
	public static final String STATE_PENDING = "pending"; //$NON-NLS-1$

	/**
	 * Success state
	 */
	public static final String STATE_SUCCESS = "success"; //$NON-NLS-1$

	private Date createdAt;

	private Date updatedAt;

	private long id;

	private String context;

	private String description;

	private String state;

	private String targetUrl;

	private String url;

	private User creator;

	/**
	 * @return createdAt
	 */
	public Date getCreatedAt() {
		return ObjectUtils.cloneDate(createdAt);
	}

	/**
	 * @param createdAt
	 * @return this status
	 */
	public CommitStatus setCreatedAt(final Date createdAt) {
		this.createdAt = ObjectUtils.cloneDate(createdAt);
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
	 * @return this status
	 */
	public CommitStatus setUpdatedAt(final Date updatedAt) {
		this.updatedAt = ObjectUtils.cloneDate(updatedAt);
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
	 * @return this status
	 */
	public CommitStatus setId(final long id) {
		this.id = id;
		return this;
	}

	/**
	 * @return context
	 */
	public String getContext() {
		return context;
	}

	/**
	 * @param context
	 * @return this status
	 */
	public CommitStatus setContext(final String context) {
		this.context = context;
		return this;
	}

	/**
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 * @return this status
	 */
	public CommitStatus setDescription(final String description) {
		this.description = description;
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
	 * @return this status
	 * throws {@link IllegalArgumentException} if state is invalid
	 */
	public CommitStatus setState(final String state) {
		if (STATE_ERROR.equals(state) || STATE_FAILURE.equals(state) || STATE_PENDING.equals(state)
				|| STATE_SUCCESS.equals(state)) {
			this.state = state;
			return this;
		}
		throw new IllegalArgumentException(MessageFormat.format("Invalid state {0}", state));
	}

	/**
	 * @return targetUrl
	 */
	public String getTargetUrl() {
		return targetUrl;
	}

	/**
	 * @param targetUrl
	 * @return this status
	 */
	public CommitStatus setTargetUrl(final String targetUrl) {
		this.targetUrl = targetUrl;
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
	 * @return this status
	 */
	public CommitStatus setUrl(final String url) {
		this.url = url;
		return this;
	}

	/**
	 * @return creator
	 */
	public User getCreator() {
		return creator;
	}

	/**
	 * @param creator
	 * @return this status
	 */
	public CommitStatus setCreator(final User creator) {
		this.creator = creator;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof CommitStatus) {
			return this.id == ((CommitStatus) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.hashCodeForLong(this.id);
	}
}
