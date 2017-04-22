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

public class ThreadSubscription implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -7100194315306040133L;

	private boolean subscribed;

	private boolean ignored;

	private String reason;

	private Date createdAt;

	private String url;

	private String threadUrl;

	public boolean isSubscribed() {
		return subscribed;
	}

	public ThreadSubscription setSubscribed(boolean subscribed) {
		this.subscribed = subscribed;
		return this;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public ThreadSubscription setIgnored(boolean ignored) {
		this.ignored = ignored;
		return this;
	}

	public String getReason() {
		return reason;
	}

	public ThreadSubscription setReason(String reason) {
		this.reason = reason;
		return this;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public ThreadSubscription setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public ThreadSubscription setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getThreadUrl() {
		return threadUrl;
	}

	public ThreadSubscription setThreadUrl(String threadUrl) {
		this.threadUrl = threadUrl;
		return this;
	}
}
