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
 * Commit comment model class.
 */
public class CommitComment extends Comment implements Comparable<CommitComment> {

	/** serialVersionUID */
	private static final long serialVersionUID = 5932088388457362322L;

	private int line;

	private int position = -1;

	private int originalPosition;

	private String commitId;

	private String originalCommitId;

	private String path;

	private String diffHunk;

	private long pullRequestReviewId;

	private String pullRequestUrl;

	/**
	 * @return line
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @param line
	 * @return this commit comment
	 */
	public CommitComment setLine(int line) {
		this.line = line;
		return this;
	}

	/**
	 * @return position
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @param position
	 * @return this commit comment
	 */
	public CommitComment setPosition(int position) {
		this.position = position;
		return this;
	}

	/**
	 * @return originalPosition
	 */
	public int getOriginalPosition() {
		return originalPosition;
	}

	/**
	 * @param originalPosition
	 * @return this commit comment
	 */
	public CommitComment setOriginalPosition(int originalPosition) {
		this.originalPosition = originalPosition;
		return this;
	}

	/**
	 * @return commitId
	 */
	public String getCommitId() {
		return commitId;
	}

	/**
	 * @param commitId
	 * @return this commit comment
	 */
	public CommitComment setCommitId(String commitId) {
		this.commitId = commitId;
		return this;
	}

	/**
	 * @return originalCommitId
	 */
	public String getOriginalCommitId() {
		return originalCommitId;
	}

	/**
	 * @param originalCommitId
	 * @return this commit comment
	 */
	public CommitComment setOriginalCommitId(String originalCommitId) {
		this.originalCommitId = originalCommitId;
		return this;
	}

	/**
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path
	 * @return this commit comment
	 */
	public CommitComment setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * @return diffHunk
	 */
	public String getDiffHunk() {
		return diffHunk;
	}

	/**
	 * @param diffHunk
	 * @return this commit comment
	 */
	public CommitComment setDiffHunk(String diffHunk) {
		this.diffHunk = diffHunk;
		return this;
	}

	public long getPullRequestReviewId() {
		return pullRequestReviewId;
	}

	public CommitComment setPullRequestReviewId(long pullRequestReviewId) {
		this.pullRequestReviewId = pullRequestReviewId;
		return this;
	}

	public String getPullRequestUrl() {
		return pullRequestUrl;
	}

	public CommitComment setPullRequestUrl(String pullRequestUrl) {
		this.pullRequestUrl = pullRequestUrl;
		return this;
	}

	@Override
	public int compareTo(CommitComment other) {
		if (getCreatedAt() == null) {
			return 1;
		}
		if (other.getCreatedAt() == null) {
			return -1;
		}
		return getCreatedAt().compareTo(other.getCreatedAt());
	}
}
