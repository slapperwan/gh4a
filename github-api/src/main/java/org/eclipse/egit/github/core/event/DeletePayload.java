/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Jason Tsay (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core.event;

import java.io.Serializable;

/**
 * DeleteEvent payload model class.
 */
public class DeletePayload extends EventPayload implements Serializable {

	private static final long serialVersionUID = -7571623946339106873L;

	public static final String REF_TYPE_BRANCH = "branch";
	public static final String REF_TYPE_TAG = "tag";

	private String refType;

	private String ref;

	/**
	 * @return refType
	 */
	public String getRefType() {
		return refType;
	}

	/**
	 * @param refType
	 * @return this DeletePayload
	 */
	public DeletePayload setRefType(String refType) {
		this.refType = refType;
		return this;
	}

	/**
	 * @return ref
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * @param ref
	 * @return this DeletePayload
	 */
	public DeletePayload setRef(String ref) {
		this.ref = ref;
		return this;
	}
}
