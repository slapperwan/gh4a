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
 * Commit file model class.
 */
public class CommitFile implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -4607637532042868579L;

	private int additions;

	private int changes;

	private int deletions;

	private String blobUrl;

	private String filename;

	private String patch;

	private String rawUrl;

	private String sha;

	private String status;

	/**
	 * @return additions
	 */
	public int getAdditions() {
		return additions;
	}

	/**
	 * @param additions
	 * @return this commit file
	 */
	public CommitFile setAdditions(int additions) {
		this.additions = additions;
		return this;
	}

	/**
	 * @return changes
	 */
	public int getChanges() {
		return changes;
	}

	/**
	 * @param changes
	 * @return this commit file
	 */
	public CommitFile setChanges(int changes) {
		this.changes = changes;
		return this;
	}

	/**
	 * @return deletions
	 */
	public int getDeletions() {
		return deletions;
	}

	/**
	 * @param deletions
	 * @return this commit file
	 */
	public CommitFile setDeletions(int deletions) {
		this.deletions = deletions;
		return this;
	}

	/**
	 * @return blobUrl
	 */
	public String getBlobUrl() {
		return blobUrl;
	}

	/**
	 * @param blobUrl
	 * @return this commit file
	 */
	public CommitFile setBlobUrl(String blobUrl) {
		this.blobUrl = blobUrl;
		return this;
	}

	/**
	 * @return filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename
	 * @return this commit file
	 */
	public CommitFile setFilename(String filename) {
		this.filename = filename;
		return this;
	}

	/**
	 * @return patch
	 */
	public String getPatch() {
		return patch;
	}

	/**
	 * @param patch
	 * @return this commit file
	 */
	public CommitFile setPatch(String patch) {
		this.patch = patch;
		return this;
	}

	/**
	 * @return rawUrl
	 */
	public String getRawUrl() {
		return rawUrl;
	}

	/**
	 * @param rawUrl
	 * @return this commit file
	 */
	public CommitFile setRawUrl(String rawUrl) {
		this.rawUrl = rawUrl;
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
	 * @return this commit file
	 */
	public CommitFile setSha(String sha) {
		this.sha = sha;
		return this;
	}

	/**
	 * @return status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status
	 * @return this commit file
	 */
	public CommitFile setStatus(String status) {
		this.status = status;
		return this;
	}
}
