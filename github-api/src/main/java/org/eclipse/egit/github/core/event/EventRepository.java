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
package org.eclipse.egit.github.core.event;

import org.eclipse.egit.github.core.util.ObjectUtils;

import java.io.Serializable;

/**
 * Model class for repository information contained in an {@link Event}
 */
public class EventRepository implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -8910798454171899699L;

	private long id;

	private String name;

	private String url;

	/**
	 * @return id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id
	 * @return this event repository
	 */
	public EventRepository setId(long id) {
		this.id = id;
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
	 * @return this event repository
	 */
	public EventRepository setName(String name) {
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
	 * @return this event repository
	 */
	public EventRepository setUrl(String url) {
		this.url = url;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof EventRepository) {
			return this.id == ((EventRepository) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.hashCodeForLong(this.id);
	}
}
