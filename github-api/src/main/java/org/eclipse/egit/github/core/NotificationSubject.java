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

public class NotificationSubject implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 5993562745647857164L;

	private String title;

	private String url;

	private String latestCommentUrl;

	private String type;

	public String getTitle() {
		return title;
	}

	public NotificationSubject setTitle(String title) {
		this.title = title;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public NotificationSubject setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getLatestCommentUrl() {
		return latestCommentUrl;
	}

	public NotificationSubject setLatestCommentUrl(String latestCommentUrl) {
		this.latestCommentUrl = latestCommentUrl;
		return this;
	}

	public String getType() {
		return type;
	}

	public NotificationSubject setType(String type) {
		this.type = type;
		return this;
	}
}
