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
 * Key model class.
 */
public class Key implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -7763033793023520265L;

	private int id;

	private String key;

	private String title;

	private String url;

	/**
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 * @return this deploy key
	 */
	public Key setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * @return key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key
	 * @return this deploy key
	 */
	public Key setKey(String key) {
		this.key = key;
		return this;
	}

	/**
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 * @return this deploy key
	 */
	public Key setTitle(String title) {
		this.title = title;
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
	 * @return this deploy key
	 */
	public Key setUrl(String url) {
		this.url = url;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Key) {
			return this.id == ((Key) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
	    return this.id;
	}
}
