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

public class Notification implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 989467705847081308L;

	private String id;

	private Repository repository;

	private NotificationSubject subject;

	private String reason;

	private boolean unread;

	private Date updatedAt;

	private Date lastReadAt;

	private String url;

	public String getId() {
		return id;
	}

	public Notification setId(String id) {
		this.id = id;
		return this;
	}

	public Repository getRepository() {
		return repository;
	}

	public Notification setRepository(Repository repository) {
		this.repository = repository;
		return this;
	}

	public NotificationSubject getSubject() {
		return subject;
	}

	public Notification setSubject(NotificationSubject subject) {
		this.subject = subject;
		return this;
	}

	public String getReason() {
		return reason;
	}

	public Notification setReason(String reason) {
		this.reason = reason;
		return this;
	}

	public boolean isUnread() {
		return unread;
	}

	public Notification setUnread(boolean unread) {
		this.unread = unread;
		return this;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public Notification setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

	public Date getLastReadAt() {
		return lastReadAt;
	}

	public Notification setLastReadAt(Date lastReadAt) {
		this.lastReadAt = lastReadAt;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public Notification setUrl(String url) {
		this.url = url;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Notification) {
			return this.id.equals(((Notification) other).id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id != null ? this.id.hashCode() : 0;
	}
}
