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
package org.eclipse.egit.github.core.service;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_COLLABORATORS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PagedRequest;

/**
 * Service for interacting with the collaborators on a GitHub repository
 *
 * @see <a href="http://developer.github.com/v3/repos/collaborators/">GitHub
 *      collaborator API documentation</a>
 */
public class CollaboratorService extends GitHubService {

	/**
	 * Create collaborator service
	 */
	public CollaboratorService() {
		super();
	}

	/**
	 * Create collaborator service
	 *
	 * @param client
	 */
	public CollaboratorService(GitHubClient client) {
		super(client);
	}

	/**
	 * Get collaborators for given repository
	 *
	 * @param repository
	 * @return non-null list of collaborators
	 * @throws IOException
	 */
	public List<User> getCollaborators(IRepositoryIdProvider repository)
			throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_COLLABORATORS);
		PagedRequest<User> request = createPagedRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Create URI for updating collaborators
	 *
	 * @param repository
	 * @param user
	 * @return URI
	 */
	protected String createUpdateUri(IRepositoryIdProvider repository,
			String user) {
		String id = getId(repository);
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_COLLABORATORS);
		uri.append('/').append(user);
		return uri.toString();
	}

	/**
	 * Is given user a collaborator on the given repository?
	 *
	 * @param repository
	 * @param user
	 * @return true if collaborator, false otherwise
	 * @throws IOException
	 */
	public boolean isCollaborator(IRepositoryIdProvider repository, String user)
			throws IOException {
		return check(createUpdateUri(repository, user));
	}

	/**
	 * Add given user as a collaborator on the given repository
	 *
	 * @param repository
	 * @param user
	 * @throws IOException
	 */
	public void addCollaborator(IRepositoryIdProvider repository, String user)
			throws IOException {
		client.put(createUpdateUri(repository, user));
	}

	/**
	 * Remove given user as a collaborator on the given repository
	 *
	 * @param repository
	 * @param user
	 * @throws IOException
	 */
	public void removeCollaborator(IRepositoryIdProvider repository, String user)
			throws IOException {
		client.delete(createUpdateUri(repository, user));
	}
}
