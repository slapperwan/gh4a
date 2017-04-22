/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
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

/**
 * Repository contributor model class
 */
public class Contributor implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -8434028880839230626L;

	/**
	 * Anonymous contributor type value
	 */
	public static final String TYPE_ANONYMOUS = "Anonymous"; //$NON-NLS-1$

	private int contributions;

	private int id;

	private String gravatarId;

	private String avatarUrl;

	private String login;

	private String name;

	private String type;

	private String url;

	/**
	 * @return contributions
	 */
	public int getContributions() {
		return contributions;
	}

	/**
	 * @param contributions
	 * @return this contributor
	 */
	public Contributor setContributions(int contributions) {
		this.contributions = contributions;
		return this;
	}

	/**
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 * @return this contributor
	 */
	public Contributor setId(int id) {
		this.id = id;
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
	 * @return this contributor
	 */
	public Contributor setGravatarId(String gravatarId) {
		this.gravatarId = gravatarId;
		return this;
	}

	/**
	 * @return avatarUrl
	 */
	public String getAvatarUrl() {
		return avatarUrl;
	}

	/**
	 * @param avatarUrl
	 * @return this contributor
	 */
	public Contributor setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
		return this;
	}

	/**
	 * @return login
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * @param login
	 * @return this contributor
	 */
	public Contributor setLogin(String login) {
		this.login = login;
		return this;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 * @return this contributor
	 */
	public Contributor setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * @return this contributor
	 */
	public Contributor setType(String type) {
		this.type = type;
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
	 * @return this contributor
	 */
	public Contributor setUrl(String url) {
		this.url = url;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Contributor) {
			return this.id == ((Contributor) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id;
	}
}
