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
import java.util.Date;
import java.util.Map;

import org.eclipse.egit.github.core.util.ObjectUtils;

/**
 * Repository hook model class
 */
public class RepositoryHook implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -9023469643749604324L;

	private boolean active;

	private Date createdAt;

	private Date updatedAt;

	private long id;

	private RepositoryHookResponse lastResponse;

	private String name;

	private String url;

	private Map<String, String> config;

	/**
	 * @return active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active
	 * @return this hook
	 */
	public RepositoryHook setActive(boolean active) {
		this.active = active;
		return this;
	}

	/**
	 * @return createdAt
	 */
	public Date getCreatedAt() {
		return ObjectUtils.cloneDate(createdAt);
	}

	/**
	 * @param createdAt
	 * @return this hook
	 */
	public RepositoryHook setCreatedAt(Date createdAt) {
		this.createdAt = ObjectUtils.cloneDate(createdAt);
		return this;
	}

	/**
	 * @return updatedAt
	 */
	public Date getUpdatedAt() {
		return ObjectUtils.cloneDate(updatedAt);
	}

	/**
	 * @param updatedAt
	 * @return this hook
	 */
	public RepositoryHook setUpdatedAt(Date updatedAt) {
		this.updatedAt = ObjectUtils.cloneDate(updatedAt);
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
	 * @return this hook
	 */
	public RepositoryHook setId(long id) {
		this.id = id;
		return this;
	}

	/**
	 * @return lastResponse
	 */
	public RepositoryHookResponse getLastResponse() {
		return lastResponse;
	}

	/**
	 * @param lastResponse
	 * @return this hook
	 */
	public RepositoryHook setLastResponse(RepositoryHookResponse lastResponse) {
		this.lastResponse = lastResponse;
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
	 * @return this hook
	 */
	public RepositoryHook setName(String name) {
		this.name = name;
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
	 * @return this hook
	 */
	public RepositoryHook setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * @return config
	 */
	public Map<String, String> getConfig() {
		return config;
	}

	/**
	 * @param config
	 * @return this hook
	 */
	public RepositoryHook setConfig(Map<String, String> config) {
		this.config = config;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RepositoryHook) {
			return this.id == ((RepositoryHook) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.hashCodeForLong(this.id);
	}
}
