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

/**
 * Resource that has type and URL fields
 */
public class TypedResource extends ShaResource {

	/** serialVersionUID */
	private static final long serialVersionUID = -7285665432528832240L;

	/**
	 * TYPE_COMMIT
	 */
	public static final String TYPE_COMMIT = "commit"; //$NON-NLS-1$

	/**
	 * TYPE_TAG
	 */
	public static final String TYPE_TAG = "tag"; //$NON-NLS-1$

	/**
	 * TYPE_BLOB
	 */
	public static final String TYPE_BLOB = "blob"; //$NON-NLS-1$

	private String type;

	private String url;

	/**
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * @return this resource
	 */
	public TypedResource setType(String type) {
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
	 * @return this resource
	 */
	public TypedResource setUrl(String url) {
		this.url = url;
		return this;
	}
}
