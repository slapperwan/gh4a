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
import java.util.List;

import org.eclipse.egit.github.core.Commit;

/**
 * PushEvent payload model class.
 */
public class PushPayload extends EventPayload implements Serializable {

	private static final long serialVersionUID = -1542484898531583478L;

	private String before;

	private String head;

	private String ref;

	private int size;

	private List<Commit> commits;

	/**
	 * @return before
	 */
	public String getBefore() {
		return before;
	}

	/**
	 * @param before
	 * @return this payload
	 */
	public PushPayload setBefore(String before) {
		this.before = before;
		return this;
	}

	/**
	 * @return head
	 */
	public String getHead() {
		return head;
	}

	/**
	 * @param head
	 * @return this PushEvent
	 */
	public PushPayload setHead(String head) {
		this.head = head;
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
	 * @return this PushEvent
	 */
	public PushPayload setRef(String ref) {
		this.ref = ref;
		return this;
	}

	/**
	 * @return size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param size
	 * @return this PushEvent
	 */
	public PushPayload setSize(int size) {
		this.size = size;
		return this;
	}

	/**
	 * @return commits
	 */
	public List<Commit> getCommits() {
		return commits;
	}

	/**
	 * @param commits
	 * @return this PushEvent
	 */
	public PushPayload setCommits(List<Commit> commits) {
		this.commits = commits;
		return this;
	}
}
