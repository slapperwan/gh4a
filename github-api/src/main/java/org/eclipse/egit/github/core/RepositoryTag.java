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
 * Repository tag model class
 */
public class RepositoryTag implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 1070566274663989459L;

	private String name;

	private String tarballUrl;

	private String zipballUrl;

	private TypedResource commit;

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 * @return this tag
	 */
	public RepositoryTag setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @return tarballUrl
	 */
	public String getTarballUrl() {
		return tarballUrl;
	}

	/**
	 * @param tarballUrl
	 * @return this tag
	 */
	public RepositoryTag setTarballUrl(String tarballUrl) {
		this.tarballUrl = tarballUrl;
		return this;
	}

	/**
	 * @return zipballUrl
	 */
	public String getZipballUrl() {
		return zipballUrl;
	}

	/**
	 * @param zipballUrl
	 * @return this tag
	 */
	public RepositoryTag setZipballUrl(String zipballUrl) {
		this.zipballUrl = zipballUrl;
		return this;
	}

	/**
	 * @return commit
	 */
	public TypedResource getCommit() {
		return commit;
	}

	/**
	 * @param commit
	 * @return this tag
	 */
	public RepositoryTag setCommit(TypedResource commit) {
		this.commit = commit;
		return this;
	}
}
