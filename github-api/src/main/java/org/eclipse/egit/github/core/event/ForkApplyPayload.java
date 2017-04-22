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
 * ForkApplyEvent payload model class.
 */
public class ForkApplyPayload extends EventPayload implements Serializable {

	private static final long serialVersionUID = -7527740351672699770L;

	private String head;

	private String before;

	private String after;

	/**
	 * @return head
	 */
	public String getHead() {
		return head;
	}

	/**
	 * @param head
	 * @return this ForkApplyPayload
	 */
	public ForkApplyPayload setHead(String head) {
		this.head = head;
		return this;
	}

	/**
	 * @return before
	 */
	public String getBefore() {
		return before;
	}

	/**
	 * @param before
	 * @return this ForkApplyPayload
	 */
	public ForkApplyPayload setBefore(String before) {
		this.before = before;
		return this;
	}

	/**
	 * @return after
	 */
	public String getAfter() {
		return after;
	}

	/**
	 * @param after
	 * @return this ForkApplyPayload
	 */
	public ForkApplyPayload setAfter(String after) {
		this.after = after;
		return this;
	}
}
