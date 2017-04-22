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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

import org.eclipse.egit.github.core.util.DateUtils;

/**
 * GitHub user model class.
 */
public class SearchUser implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -1211802439113529774L;

	/**
	 * TYPE_USER
	 */
	public static final String TYPE_USER = "user"; //$NON-NLS-1$

	/**
	 * TYPE_ORG
	 */
	public static final String TYPE_ORG = "organization"; //$NON-NLS-1$

	private String id;
	private String gravatarId;
	private String login;
	private String name;
	@SerializedName("fullname")
	private String fullName;
	@SerializedName("username")
	private String userName;

	@SerializedName("created")
	private Date createdAt;

	private String location;
	private int publicRepoCount;
	private int repos;
	private int followers;
	private String language;
	private String type;

	private double score;

	/**
	 * @return createdAt
	 */
	public Date getCreatedAt() {
		return DateUtils.clone(createdAt);
	}

	/**
	 * @return followers
	 */
	public int getFollowers() {
		return followers;
	}

	/**
	 * @return id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return gravatarId
	 */
	public String getGravatarId() {
		return gravatarId;
	}

	/**
	 * @return location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @return login
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return publicRepos
	 */
	public int getPublicRepos() {
		return publicRepoCount;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SearchUser) {
			return this.id != null && this.id.equals(((SearchUser) other).id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id != null ? this.id.hashCode() : 0;
	}
}
