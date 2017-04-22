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
 * GitHub {@link Issue} and {@link Gist} comment class.
 */
public class Comment implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 5128896032791651031L;

	private Date createdAt;

	private Date updatedAt;

	private String body;

	private String bodyHtml;

	private String bodyText;

	private long id;

	private String url;

	private String htmlUrl;

	private User user;

	private Reactions reactions;

	/**
	 * @return createdAt
	 */
	public Date getCreatedAt() {
		return DateUtils.clone(createdAt);
	}

	/**
	 * @param createdAt
	 * @return this comment
	 */
	public Comment setCreatedAt(Date createdAt) {
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
	 * @return this comment
	 */
	public Comment setUpdatedAt(Date updatedAt) {
		this.updatedAt = DateUtils.clone(updatedAt);
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
	 * @return this comment
	 */
	public Comment setBody(String body) {
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
	 * @return this comment
	 */
	public Comment setBodyHtml(String bodyHtml) {
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
	 * @return this comment
	 */
	public Comment setBodyText(String bodyText) {
		this.bodyText = bodyText;
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
	 * @return this comment
	 */
	public Comment setId(long id) {
		this.id = id;
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
	 * @return this comment
	 */
	public Comment setUrl(String url) {
		this.url = url;
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
	 * @return this comment
	 */
	public Comment setHtmlUrl(String htmlUrl) {
		this.htmlUrl = htmlUrl;
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
	 * @return this comment
	 */
	public Comment setUser(User user) {
		this.user = user;
		return this;
	}

	public Reactions getReactions() {
		return reactions;
	}

	public Comment setReactions(Reactions reactions) {
		this.reactions = reactions;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Comment) {
			return this.id == ((Comment) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.id);
	}
}
