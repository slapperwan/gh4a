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
 * Repository branch model class
 */
public class RepositoryBranch implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 4927461901146433920L;

	private String name;

	private TypedResource commit;

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 * @return this branch
	 */
	public RepositoryBranch setName(String name) {
		this.name = name;
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
	 * @return this branch
	 */
	public RepositoryBranch setCommit(TypedResource commit) {
		this.commit = commit;
		return this;
	}
}
