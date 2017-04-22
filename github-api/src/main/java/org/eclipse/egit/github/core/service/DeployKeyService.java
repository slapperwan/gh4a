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

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_KEYS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Key;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.PagedRequest;

/**
 * Service for interacting with a repository's deploy keys
 *
 * @see <a href="http://developer.github.com/v3/repos/keys">GitHub deploy key
 *      API documentation</a>
 */
public class DeployKeyService extends GitHubService {

	/**
	 * Create deploy key service
	 */
	public DeployKeyService() {
		super();
	}

	/**
	 * Create deploy key service
	 *
	 * @param client
	 */
	public DeployKeyService(GitHubClient client) {
		super(client);
	}

	/**
	 * Get all deploys keys associated with the given repository
	 *
	 * @param repository
	 * @return non-null but possibly empty list of deploy keys
	 * @throws IOException
	 */
	public List<Key> getKeys(IRepositoryIdProvider repository)
			throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_KEYS);
		PagedRequest<Key> request = createPagedRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<Key>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Get deploy key with given id from given repository
	 *
	 * @param repository
	 * @param id
	 * @return deploy key
	 * @throws IOException
	 */
	public Key getKey(IRepositoryIdProvider repository, int id)
			throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_KEYS);
		uri.append('/').append(id);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(Key.class);
		return (Key) client.get(request).getBody();
	}

	/**
	 * Create deploy key to be associated with given repository
	 *
	 * @param repository
	 * @param key
	 * @return created deploy key
	 * @throws IOException
	 */
	public Key createKey(IRepositoryIdProvider repository, Key key)
			throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_KEYS);
		return client.post(uri.toString(), key, Key.class);
	}

	/**
	 * Edit given deploy key
	 *
	 * @param repository
	 * @param key
	 * @return edited deploy key
	 * @throws IOException
	 */
	public Key editKey(IRepositoryIdProvider repository, Key key)
			throws IOException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null"); //$NON-NLS-1$
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_KEYS);
		uri.append('/').append(key.getId());
		return client.post(uri.toString(), key, Key.class);
	}

	/**
	 * Deploy deploy key with given id from given repository
	 *
	 * @param repository
	 * @param id
	 * @throws IOException
	 */
	public void deleteKey(IRepositoryIdProvider repository, int id)
			throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_KEYS);
		uri.append('/').append(id);
		client.delete(uri.toString());
	}
}
