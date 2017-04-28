/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Jason Tsay (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core.event;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.util.ObjectUtils;

/**
 * Event model class.
 */
public class Event implements Serializable {

	/**
	 * Event type denoting a {@link CommitCommentPayload}
	 */
	public static final String TYPE_COMMIT_COMMENT = "CommitCommentEvent";

	/**
	 * Event type denoting a {@link CreatePayload}
	 */
	public static final String TYPE_CREATE = "CreateEvent";

	/**
	 * Event type denoting a {@link DeletePayload}
	 */
	public static final String TYPE_DELETE = "DeleteEvent";

	/**
	 * Event type denoting a {@link DownloadPayload}
	 */
	public static final String TYPE_DOWNLOAD = "DownloadEvent";

	/**
	 * Event type dneoting a {@link FollowPayload}
	 */
	public static final String TYPE_FOLLOW = "FollowEvent";

	/**
	 * Event type denoting a {@link ForkPayload}
	 */
	public static final String TYPE_FORK = "ForkEvent";

	/**
	 * Event type denoting a {@link ForkApplyPayload}
	 */
	public static final String TYPE_FORK_APPLY = "ForkApplyEvent";

	/**
	 * Event type denoting a {@link GistPayload}
	 */
	public static final String TYPE_GIST = "GistEvent";

	/**
	 * Event type denoting a {@link GollumPayload}
	 */
	public static final String TYPE_GOLLUM = "GollumEvent";

	/**
	 * Event type denoting a {@link IssueCommentPayload}
	 */
	public static final String TYPE_ISSUE_COMMENT = "IssueCommentEvent";

	/**
	 * Event type denoting a {@link IssuesPayload}
	 */
	public static final String TYPE_ISSUES = "IssuesEvent";

	/**
	 * Event type denoting a {@link MemberPayload}
	 */
	public static final String TYPE_MEMBER = "MemberEvent";

	/**
	 * Event type denoting a {@link PublicPayload}
	 */
	public static final String TYPE_PUBLIC = "PublicEvent";

	/**
	 * Event type denoting a {@link PullRequestPayload}
	 */
	public static final String TYPE_PULL_REQUEST = "PullRequestEvent";

	/**
	 * Event type denoting a {@link PullRequestReviewCommentPayload}
	 */
	public static final String TYPE_PULL_REQUEST_REVIEW_COMMENT = "PullRequestReviewCommentEvent";

	/**
	 * Event type denoting a {@link PushPayload}
	 */
	public static final String TYPE_PUSH = "PushEvent";

	/**
	 * Event type denoting a {@link ReleasePayload}
	 */
	public static final String TYPE_RELEASE = "ReleaseEvent";

	/**
	 * Event type denoting a {@link TeamAddPayload}
	 */
	public static final String TYPE_TEAM_ADD = "TeamAddEvent";

	/**
	 * Event type denoting a {@link WatchPayload}
	 */
	public static final String TYPE_WATCH = "WatchEvent";

	private static final long serialVersionUID = 3633702964380402233L;

	/**
	 * Make sure this is above payload. Payload deserialization depends on being
	 * able to read the type first.
	 */
	private String type;

	@SerializedName("public")
	private boolean isPublic;

	private EventPayload payload;

	private EventRepository repo;

	private String id;

	private User actor;

	private User org;

	private Date createdAt;

	/**
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * @return this Event
	 */
	public Event setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * @return isPublic
	 */
	public boolean isPublic() {
		return isPublic;
	}

	/**
	 * @param isPublic
	 * @return this Event
	 */
	public Event setPublic(boolean isPublic) {
		this.isPublic = isPublic;
		return this;
	}

	/**
	 * @return the repo
	 */
	public EventRepository getRepo() {
		return repo;
	}

	/**
	 * @param repo
	 * @return this Event
	 */
	public Event setRepo(EventRepository repo) {
		this.repo = repo;
		return this;
	}

	/**
	 * @return the actor
	 */
	public User getActor() {
		return actor;
	}

	/**
	 * @param actor
	 * @return this Event
	 */
	public Event setActor(User actor) {
		this.actor = actor;
		return this;
	}

	/**
	 * @return the org
	 */
	public User getOrg() {
		return org;
	}

	/**
	 * @param org
	 * @return this Event
	 */
	public Event setOrg(User org) {
		this.org = org;
		return this;
	}

	/**
	 * @return the createdAt
	 */
	public Date getCreatedAt() {
		return ObjectUtils.cloneDate(createdAt);
	}

	/**
	 * @param createdAt
	 * @return this Event
	 */
	public Event setCreatedAt(Date createdAt) {
		this.createdAt = ObjectUtils.cloneDate(createdAt);
		return this;
	}

	/**
	 * @return payload
	 */
	public EventPayload getPayload() {
		return payload;
	}

	/**
	 * @param payload
	 * @return this event
	 */
	public Event setPayload(EventPayload payload) {
		this.payload = payload;
		return this;
	}

	/**
	 * @return id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 * @return this event
	 */
	public Event setId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Event) {
			return this.id.equals(((Event) other).id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id != null ? this.id.hashCode() : 0;
	}
}
