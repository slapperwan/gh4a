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

public class Reaction implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 6358575015023539051L;

	public static final String CONTENT_PLUS_ONE = "+1";
	public static final String CONTENT_MINUS_ONE = "-1";
	public static final String CONTENT_LAUGH = "laugh";
	public static final String CONTENT_CONFUSED = "confused";
	public static final String CONTENT_HEART = "heart";
	public static final String CONTENT_HOORAY = "hooray";

	private int id;
	private User user;
	private String content;
	private Date createdAt;

	public int getId() {
		return id;
	}

	public Reaction setId(int id) {
		this.id = id;
		return this;
	}

	public String getContent() {
		return content;
	}

	public Reaction setContent(String content) {
		this.content = content;
		return this;
	}

	public User getUser() {
		return user;
	}

	public Reaction setUser(User user) {
		this.user = user;
		return this;
	}

	public Date getCreatedAt() {
		return DateUtils.clone(createdAt);
	}

	public Reaction setCreatedAt(Date createdAt) {
		this.createdAt = DateUtils.clone(createdAt);
		return this;
	}
}
