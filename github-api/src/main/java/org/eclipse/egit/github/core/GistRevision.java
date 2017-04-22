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

import org.eclipse.egit.github.core.util.DateUtils;

/**
 * Gist revision class.
 */
public class GistRevision implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -7863453407918499259L;

	private Date committedAt;

	private GistChangeStatus changeStatus;

	private String url;

	private String version;

	private User user;

	/**
	 * @return committedAt
	 */
	public Date getCommittedAt() {
		return DateUtils.clone(committedAt);
	}

	/**
	 * @param committedAt
	 * @return this gist revision
	 */
	public GistRevision setCommittedAt(Date committedAt) {
		this.committedAt = DateUtils.clone(committedAt);
		return this;
	}

	/**
	 * @return changeStatus
	 */
	public GistChangeStatus getChangeStatus() {
		return changeStatus;
	}

	/**
	 * @param changeStatus
	 * @return this gist revision
	 */
	public GistRevision setChangeStatus(GistChangeStatus changeStatus) {
		this.changeStatus = changeStatus;
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
	 * @return this gist revision
	 */
	public GistRevision setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * @return version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version
	 * @return this gist revision
	 */
	public GistRevision setVersion(String version) {
		this.version = version;
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
	 * @return this gist revision
	 */
	public GistRevision setUser(User user) {
		this.user = user;
		return this;
	}
}
