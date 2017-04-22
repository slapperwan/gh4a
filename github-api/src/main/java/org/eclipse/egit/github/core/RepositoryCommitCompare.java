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
import java.util.List;

/**
 * Model class for the comparison of two commits
 */
public class RepositoryCommitCompare implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -6268028122983164123L;

	private int aheadBy;

	private int behindBy;

	private int totalCommits;

	private List<CommitFile> files;

	private List<RepositoryCommit> commits;

	private RepositoryCommit baseCommit;

	private String diffUrl;

	private String htmlUrl;

	private String patchUrl;

	private String permalinkUrl;

	private String status;

	private String url;

	/**
	 * @return aheadBy
	 */
	public int getAheadBy() {
		return aheadBy;
	}

	/**
	 * @param aheadBy
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setAheadBy(int aheadBy) {
		this.aheadBy = aheadBy;
		return this;
	}

	/**
	 * @return behindBy
	 */
	public int getBehindBy() {
		return behindBy;
	}

	/**
	 * @param behindBy
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setBehindBy(int behindBy) {
		this.behindBy = behindBy;
		return this;
	}

	/**
	 * @return totalCommits
	 */
	public int getTotalCommits() {
		return totalCommits;
	}

	/**
	 * @param totalCommits
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setTotalCommits(int totalCommits) {
		this.totalCommits = totalCommits;
		return this;
	}

	/**
	 * @return files
	 */
	public List<CommitFile> getFiles() {
		return files;
	}

	/**
	 * @param files
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setFiles(List<CommitFile> files) {
		this.files = files;
		return this;
	}

	/**
	 * @return commits
	 */
	public List<RepositoryCommit> getCommits() {
		return commits;
	}

	/**
	 * @param commits
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setCommits(List<RepositoryCommit> commits) {
		this.commits = commits;
		return this;
	}

	/**
	 * @return baseCommit
	 */
	public RepositoryCommit getBaseCommit() {
		return baseCommit;
	}

	/**
	 * @param baseCommit
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setBaseCommit(RepositoryCommit baseCommit) {
		this.baseCommit = baseCommit;
		return this;
	}

	/**
	 * @return diffUrl
	 */
	public String getDiffUrl() {
		return diffUrl;
	}

	/**
	 * @param diffUrl
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setDiffUrl(String diffUrl) {
		this.diffUrl = diffUrl;
		return this;
	}

	/**
	 * @return htmlUrl
	 */
	public String getHtmlUrl() {
		return htmlUrl;
	}

	/**
	 * @param htmlUrl
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setHtmlUrl(String htmlUrl) {
		this.htmlUrl = htmlUrl;
		return this;
	}

	/**
	 * @return patchUrl
	 */
	public String getPatchUrl() {
		return patchUrl;
	}

	/**
	 * @param patchUrl
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setPatchUrl(String patchUrl) {
		this.patchUrl = patchUrl;
		return this;
	}

	/**
	 * @return permalinkUrl
	 */
	public String getPermalinkUrl() {
		return permalinkUrl;
	}

	/**
	 * @param permalinkUrl
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setPermalinkUrl(String permalinkUrl) {
		this.permalinkUrl = permalinkUrl;
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
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setUrl(String url) {
		this.url = url;
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
	 * @return this commit compare
	 */
	public RepositoryCommitCompare setStatus(String status) {
		this.status = status;
		return this;
	}
}
