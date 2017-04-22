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

import org.eclipse.egit.github.core.service.IssueService;

/**
 * Extension of {@link Issue} that includes the {@link Repository} that the
 * issue is in.
 * <p>
 * This type of issue is returned from {@link IssueService} calls that don't
 * require an {@link IRepositoryIdProvider} to be specified and therefore the
 * repository information is needed to correlate which issues occur in which
 * repositories.
 */
public class RepositoryIssue extends Issue {

	private static final long serialVersionUID = 6219926097588214812L;

	private Repository repository;

	/**
	 * @return repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @param repository
	 * @return this issue
	 */
	public RepositoryIssue setRepository(Repository repository) {
		this.repository = repository;
		return this;
	}
}
