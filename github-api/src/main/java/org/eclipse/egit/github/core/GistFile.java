/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core;

import java.io.Serializable;

/**
 * Gist file class.
 */
public class GistFile implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 2067939890126207032L;

	private int size;

	private String content;

	private String filename;

	private String rawUrl;

	/**
	 * @return size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param size
	 * @return this gist file
	 */
	public GistFile setSize(int size) {
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
	 * @return this gist file
	 */
	public GistFile setContent(String content) {
		this.content = content;
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
	 * @return this gist file
	 */
	public GistFile setFilename(String filename) {
		this.filename = filename;
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
	 * @return this gist file
	 */
	public GistFile setRawUrl(String rawUrl) {
		this.rawUrl = rawUrl;
		return this;
	}
}
