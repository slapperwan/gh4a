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
 * Team model class.
 */
public class Team implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -1844276044857264413L;

	private int id;

	private int membersCount;

	private int reposCount;

	private String name;

	private String permission;

	private String url;

	/**
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 * @return this team
	 */
	public Team setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * @return membersCount
	 */
	public int getMembersCount() {
		return membersCount;
	}

	/**
	 * @param membersCount
	 * @return this team
	 */
	public Team setMembersCount(int membersCount) {
		this.membersCount = membersCount;
		return this;
	}

	/**
	 * @return reposCount
	 */
	public int getReposCount() {
		return reposCount;
	}

	/**
	 * @param reposCount
	 * @return this team
	 */
	public Team setReposCount(int reposCount) {
		this.reposCount = reposCount;
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
	 * @return this team
	 */
	public Team setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @return permission
	 */
	public String getPermission() {
		return permission;
	}

	/**
	 * @param permission
	 * @return this team
	 */
	public Team setPermission(String permission) {
		this.permission = permission;
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
	 * @return this team
	 */
	public Team setUrl(String url) {
		this.url = url;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Team) {
			return this.id == ((Team) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id;
	}
}
