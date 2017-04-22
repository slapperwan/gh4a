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

import java.io.Serializable;

/**
 * Id model class.
 */
public class Id implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -1074145490136786429L;

	private String id;

	/**
	 * Get id
	 *
	 * @return id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 * @return this id
	 */
	public Id setId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Id) {
			return this.id != null && this.id.equals(((Id) other).id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id != null ? this.id.hashCode() : 0;
	}
}
