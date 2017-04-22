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
 * Reference model class
 */
public class Reference implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -4092126502387796380L;

	private String ref;

	private String url;

	private TypedResource object;

	/**
	 * @return ref
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * @param ref
	 * @return this reference
	 */
	public Reference setRef(String ref) {
		this.ref = ref;
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
	 * @return this reference
	 */
	public Reference setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * @return object
	 */
	public TypedResource getObject() {
		return object;
	}

	/**
	 * @param object
	 * @return this reference
	 */
	public Reference setObject(TypedResource object) {
		this.object = object;
		return this;
	}
}
