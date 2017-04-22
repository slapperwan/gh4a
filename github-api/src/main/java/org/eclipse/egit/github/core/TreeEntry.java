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
 * Tree entry model class
 */
public class TreeEntry implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -6181332657279059683L;

	/**
	 * TYPE_BLOB
	 */
	public static final String TYPE_BLOB = "blob"; //$NON-NLS-1$

	/**
	 * TYPE_TREE
	 */
	public static final String TYPE_TREE = "tree"; //$NON-NLS-1$

	/**
	 * MODE_BLOB
	 */
	public static final String MODE_BLOB = "100644"; //$NON-NLS-1$

	/**
	 * MODE_BLOB_EXECUTABLE
	 */
	public static final String MODE_BLOB_EXECUTABLE = "100755"; //$NON-NLS-1$

	/**
	 * MODE_BLOB_SYMLINK
	 */
	public static final String MODE_BLOB_SYMLINK = "120000"; //$NON-NLS-1$

	/**
	 * MODE_DIRECTORY
	 */
	public static final String MODE_DIRECTORY = "040000"; //$NON-NLS-1$

	/**
	 * MODE_SUBMODULE
	 */
	public static final String MODE_SUBMODULE = "160000"; //$NON-NLS-1$

	private long size;

	private String mode;

	private String path;

	private String sha;

	private String type;

	private String url;

	/**
	 * @return size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @param size
	 * @return this tree entry
	 */
	public TreeEntry setSize(long size) {
		this.size = size;
		return this;
	}

	/**
	 * @return mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * @param mode
	 * @return this tree entry
	 */
	public TreeEntry setMode(String mode) {
		this.mode = mode;
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
	 * @return this tree entry
	 */
	public TreeEntry setPath(String path) {
		this.path = path;
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
	 * @return this tree entry
	 */
	public TreeEntry setSha(String sha) {
		this.sha = sha;
		return this;
	}

	/**
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * @return this tree entry
	 */
	public TreeEntry setType(String type) {
		this.type = type;
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
	 * @return this tree entry
	 */
	public TreeEntry setUrl(String url) {
		this.url = url;
		return this;
	}
}
