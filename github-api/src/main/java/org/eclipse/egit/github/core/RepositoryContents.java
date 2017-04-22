/******************************************************************************
 *  Copyright (c) 2012 GitHub Inc.
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
 * Contents of a path in a repository
 */
public class RepositoryContents implements Serializable {

	private static final long serialVersionUID = -70974727412738287L;

	/** ENCODING_BASE64 */
	public static final String ENCODING_BASE64 = "base64"; //$NON-NLS-1$

	/** TYPE_FILE */
	public static final String TYPE_FILE = "file"; //$NON-NLS-1$

	/** TYPE_DIR */
	public static final String TYPE_DIR = "dir"; //$NON-NLS-1$

	private long size;

	private String content;

	private String encoding;

	private String name;

	private String path;

	private String sha;

	private String type;

	/**
	 * @return size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @param size
	 * @return this contents
	 */
	public RepositoryContents setSize(long size) {
		this.size = size;
		return this;
	}

	/**
	 * @return content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content
	 * @return this contents
	 */
	public RepositoryContents setContent(String content) {
		this.content = content;
		return this;
	}

	/**
	 * @return encoding
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * @param encoding
	 * @return this contents
	 */
	public RepositoryContents setEncoding(String encoding) {
		this.encoding = encoding;
		return this;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 * @return this contents
	 */
	public RepositoryContents setName(String name) {
		this.name = name;
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
	 * @return this contents
	 */
	public RepositoryContents setPath(String path) {
		this.path = path;
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
	 * @return this contents
	 */
	public RepositoryContents setType(String type) {
		this.type = type;
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
	 * @return this contents
	 */
	public RepositoryContents setSha(String sha) {
		this.sha = sha;
		return this;
	}
}
