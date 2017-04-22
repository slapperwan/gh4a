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
 * Commit model class.
 */
public class Commit implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -1893280210470143372L;

	private CommitUser author;

	private CommitUser committer;

	private int commentCount;

	private List<Commit> parents;

	private String message;

	private String sha;

	private String url;

	private Tree tree;

	/**
	 * @return author
	 */
	public CommitUser getAuthor() {
		return author;
	}

	/**
	 * @param author
	 * @return this commit
	 */
	public Commit setAuthor(CommitUser author) {
		this.author = author;
		return this;
	}

	/**
	 * @return committer
	 */
	public CommitUser getCommitter() {
		return committer;
	}

	/**
	 * @param committer
	 * @return this commit
	 */
	public Commit setCommitter(CommitUser committer) {
		this.committer = committer;
		return this;
	}

	/**
	 * @return commentCount
	 */
	public int getCommentCount() {
		return commentCount;
	}

	/**
	 * @param commentCount
	 * @return this commit
	 */
	public Commit setCommentCount(int commentCount) {
		this.commentCount = commentCount;
		return this;
	}

	/**
	 * @return parents
	 */
	public List<Commit> getParents() {
		return parents;
	}

	/**
	 * @param parents
	 * @return this commit
	 */
	public Commit setParents(List<Commit> parents) {
		this.parents = parents;
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
	 * @return this commit
	 */
	public Commit setMessage(String message) {
		this.message = message;
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
	 * @return this commit
	 */
	public Commit setSha(String sha) {
		this.sha = sha;
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
	 * @return this commit
	 */
	public Commit setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * @return tree
	 */
	public Tree getTree() {
		return tree;
	}

	/**
	 * @param tree
	 * @return this commit
	 */
	public Commit setTree(Tree tree) {
		this.tree = tree;
		return this;
	}
}
