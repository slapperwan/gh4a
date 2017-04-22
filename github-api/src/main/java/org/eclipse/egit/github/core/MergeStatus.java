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
 * Pull request merge status model class.
 */
public class MergeStatus implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 2003332803236436488L;

	private boolean merged;

	private String sha;

	private String message;

	/**
	 * @return merged
	 */
	public boolean isMerged() {
		return merged;
	}

	/**
	 * @param merged
	 * @return this merge status
	 */
	public MergeStatus setMerged(boolean merged) {
		this.merged = merged;
		return this;
	}

	/**
	 * @return sha
	 */
	public String getSha() {
		return sha;
	}

	/**
	 * @param sha
	 * @return this merge status
	 */
	public MergeStatus setSha(String sha) {
		this.sha = sha;
		return this;
	}

	/**
	 * @return message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message
	 * @return this merge status
	 */
	public MergeStatus setMessage(String message) {
		this.message = message;
		return this;
	}
}
