/******************************************************************************
 *  Copyright (c) 2015 Jon Ander Peñalba
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Jon Ander Peñalba - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.github.core.service;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_STARGAZERS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_STARRED;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USER;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USERS;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;

import com.google.gson.reflect.TypeToken;

/**
 * Service class for dealing with users starring GitHub repositories.
 *
 * @see <a href="https://developer.github.com/v3/activity/starring/">GitHub stargazer
 *      API documentation</a>
 * @since 4.2
 */
public class StargazerService extends GitHubService {

	/**
	 * Create stargazer service
	 */
	public StargazerService() {
		super();
	}

	/**
	 * Create stargazer service
	 *
	 * @param client
	 */
	public StargazerService(GitHubClient client) {
		super(client);
	}

	/**
	 * Create page stargazer request
	 *
	 * @param repository
	 * @param start
	 * @param size
	 * @return request
	 */
	protected PagedRequest<User> createStargazerRequest(
			IRepositoryIdProvider repository, int start, int size) {
		String id = getId(repository);
		PagedRequest<User> request = createPagedRequest(start, size);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_STARGAZERS);
		request.setUri(uri);
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return request;
	}

	/**
	 * Get users starring the given repository
	 *
	 * @param repository
	 * @return non-null but possibly empty list of users
	 * @throws IOException
	 */
	public List<User> getStargazers(IRepositoryIdProvider repository)
			throws IOException {
		PagedRequest<User> request = createStargazerRequest(repository,
				PAGE_FIRST, PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Page stargazers of given repository
	 *
	 * @param repository
	 * @return page iterator
	 */
	public PageIterator<User> pageStargazers(IRepositoryIdProvider repository) {
		return pageStargazers(repository, PAGE_SIZE);
	}

	/**
	 * Page stargazers of given repository
	 *
	 * @param repository
	 * @param size
	 * @return page iterator
	 */
	public PageIterator<User> pageStargazers(IRepositoryIdProvider repository,
			int size) {
		return pageStargazers(repository, PAGE_FIRST, size);
	}

	/**
	 * Page stargazers of given repository
	 *
	 * @param repository
	 * @param start
	 * @param size
	 * @return page iterator
	 */
	public PageIterator<User> pageStargazers(IRepositoryIdProvider repository,
			int start, int size) {
		PagedRequest<User> request = createStargazerRequest(repository, start,
				size);
		return createPageIterator(request);
	}

	/**
	 * Create page starred request
	 *
	 * @param user
	 * @param start
	 * @param size
	 * @return request
	 */
	protected PagedRequest<Repository> createStarredRequest(String user,
			int start, int size) {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		PagedRequest<Repository> request = createPagedRequest(start, size);
		StringBuilder uri = new StringBuilder(SEGMENT_USERS);
		uri.append('/').append(user);
		uri.append(SEGMENT_STARRED);
		request.setUri(uri);
		request.setType(new TypeToken<List<Repository>>() {
		}.getType());
		return request;
	}

	/**
	 * Create page starred request
	 *
	 * @param start
	 * @param size
	 * @return request
	 */
	protected PagedRequest<Repository> createStarredRequest(int start, int size) {
		PagedRequest<Repository> request = createPagedRequest(start, size);
		request.setUri(SEGMENT_USER + SEGMENT_STARRED);
		request.setType(new TypeToken<List<Repository>>() {
		}.getType());
		return request;
	}

	/**
	 * Get repositories starred by the given user
	 *
	 * @param user
	 * @return non-null but possibly empty list of repositories
	 * @throws IOException
	 */
	public List<Repository> getStarred(String user) throws IOException {
		PagedRequest<Repository> request = createStarredRequest(user,
				PAGE_FIRST, PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Page repositories starred by given user
	 *
	 * @param user
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(String user) throws IOException {
		return pageStarred(user, PAGE_SIZE);
	}

	/**
	 * Page repositories starred by given user
	 *
	 * @param user
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(String user, int size)
			throws IOException {
		return pageStarred(user, PAGE_FIRST, size);
	}

	/**
	 * Page repositories starred by given user
	 *
	 * @param user
	 * @param start
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(String user, int start, int size)
			throws IOException {
		PagedRequest<Repository> request = createStarredRequest(user, start,
				size);
		return createPageIterator(request);
	}

	/**
	 * Get repositories starred by the currently authenticated user
	 *
	 * @return non-null but possibly empty list of repositories
	 * @throws IOException
	 */
	public List<Repository> getStarred() throws IOException {
		PagedRequest<Repository> request = createStarredRequest(PAGE_FIRST,
				PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Page repositories starred by the currently authenticated user
	 *
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred() throws IOException {
		return pageStarred(PAGE_SIZE);
	}

	/**
	 * Page repositories starred by the currently authenticated user
	 *
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(int size) throws IOException {
		return pageStarred(PAGE_FIRST, size);
	}

	/**
	 * Page repositories starred by the currently authenticated user
	 *
	 * @param start
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(int start, int size)
			throws IOException {
		PagedRequest<Repository> request = createStarredRequest(start, size);
		return createPageIterator(request);
	}

	/**
	 * Is currently authenticated user starring given repository?
	 *
	 * @param repository
	 * @return {@code true} if starred, {@code false} otherwise
	 * @throws IOException
	 */
	public boolean isStarring(IRepositoryIdProvider repository)
			throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_USER);
		uri.append(SEGMENT_STARRED);
		uri.append('/').append(id);
		return check(uri.toString());
	}

	/**
	 * Add currently authenticated user as a stargazer of the given repository
	 *
	 * @param repository
	 * @throws IOException
	 */
	public void star(IRepositoryIdProvider repository) throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_USER);
		uri.append(SEGMENT_STARRED);
		uri.append('/').append(id);
		client.put(uri.toString());
	}

	/**
	 * Remove currently authenticated user as a stargazer of the given repository
	 *
	 * @param repository
	 * @throws IOException
	 */
	public void unstar(IRepositoryIdProvider repository) throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_USER);
		uri.append(SEGMENT_STARRED);
		uri.append('/').append(id);
		client.delete(uri.toString());
	}
}
