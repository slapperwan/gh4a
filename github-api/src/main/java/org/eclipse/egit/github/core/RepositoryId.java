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
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Repository id
 */
public class RepositoryId implements IRepositoryIdProvider, Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -57313931704393200L;

	/**
	 * Create repository from url.
	 *
	 * @see #createFromId(String)
	 * @param url
	 * @return repository or null if parsing fails
	 */
	public static RepositoryId createFromUrl(URL url) {
		return url != null ? createFromId(url.getPath()) : null;
	}

	/**
	 * Create repository from id. The id is split on the '/' character and the
	 * first two non-empty segments are interpreted to be the repository owner
	 * and name.
	 *
	 * @param id
	 * @return repository
	 */
	public static RepositoryId createFromId(String id) {
		if (id == null || id.length() == 0)
			return null;
		String owner = null;
		String name = null;
		for (String segment : id.split("/")) //$NON-NLS-1$
			if (segment.length() > 0)
				if (owner == null)
					owner = segment;
				else if (name == null)
					name = segment;
				else
					break;

		return owner != null && owner.length() > 0 && name != null
				&& name.length() > 0 ? new RepositoryId(owner, name) : null;
	}

	/**
	 * Create from string URL
	 *
	 * @see #createFromUrl(URL)
	 * @param url
	 * @return repository or null if it could not be parsed from URL path
	 */
	public static RepositoryId createFromUrl(String url) {
		if (url == null || url.length() == 0)
			return null;
		try {
			return createFromUrl(new URL(url));
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Create repository id from given owner and name.
	 *
	 * @param owner
	 * @param name
	 * @return repository id
	 */
	public static RepositoryId create(String owner, String name) {
		return new RepositoryId(owner, name);
	}

	private final String owner;

	private final String name;

	/**
	 * Create repository id with given owner and name. This constructor
	 * validates the parameters and throws an {@link IllegalArgumentException}
	 * if either is null or empty.
	 *
	 * @param owner
	 *            must be non-null and non-empty
	 * @param name
	 *            must be non-null and non-empty
	 */
	public RepositoryId(final String owner, final String name) {
		if (owner == null)
			throw new IllegalArgumentException("Owner cannot be null"); //$NON-NLS-1$
		if (owner.length() == 0)
			throw new IllegalArgumentException("Owner cannot be empty"); //$NON-NLS-1$
		if (name == null)
			throw new IllegalArgumentException("Name cannot be null"); //$NON-NLS-1$
		if (name.length() == 0)
			throw new IllegalArgumentException("Name cannot be empty"); //$NON-NLS-1$

		this.owner = owner;
		this.name = name;
	}

	/**
	 * @return owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	public String generateId() {
		return owner + "/" + name; //$NON-NLS-1$
	}

	@Override
	public int hashCode() {
		return generateId().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof RepositoryId))
			return false;
		RepositoryId other = (RepositoryId) obj;
		return name.equals(other.name) && owner.equals(other.owner);
	}

	@Override
	public String toString() {
		return generateId();
	}
}
