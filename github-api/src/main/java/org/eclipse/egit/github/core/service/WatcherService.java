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

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USER;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USERS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_SUBSCRIBERS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_SUBSCRIPTION;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_SUBSCRIPTIONS;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.util.UrlUtils;

import com.google.gson.reflect.TypeToken;

/**
 * Service class for dealing with users watching GitHub repositories.
 *
 * @see <a href="http://developer.github.com/v3/repos/watching">GitHub watcher
 *      API documentation</a>
 * @deprecated use {@link StargazerService} instead
 */
@Deprecated
public class WatcherService extends GitHubService {

	/**
	 * Subscription status field name
	 */
	public static final String FIELD_SUBSCRIBED = "subscribed"; //$NON-NLS-1$

	/**
	 * Create watcher service
	 */
	public WatcherService() {
		super();
	}

	/**
	 * Create watcher service
	 *
	 * @param client
	 */
	public WatcherService(GitHubClient client) {
		super(client);
	}

	/**
	 * Create page watcher request
	 *
	 * @param repository
	 * @param start
	 * @param size
	 * @return request
	 * @deprecated use {@link StargazerService#createStargazerRequest}
	 */
	protected PagedRequest<User> createWatcherRequest(
			IRepositoryIdProvider repository, int start, int size) {
		String id = getId(repository);
		PagedRequest<User> request = createPagedRequest(start, size);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_SUBSCRIBERS);
		request.setUri(uri);
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return request;
	}

	/**
	 * Get user watching given repository
	 *
	 * @param repository
	 * @return non-null but possibly empty list of users
	 * @throws IOException
	 * @deprecated use {@link StargazerService#getStargazers} instead
	 */
	public List<User> getWatchers(IRepositoryIdProvider repository)
			throws IOException {
		PagedRequest<User> request = createWatcherRequest(repository,
				PAGE_FIRST, PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Page watches of given repository
	 *
	 * @param repository
	 * @return page iterator
	 * @deprecated use {@link StargazerService#pageStargazers}
	 */
	public PageIterator<User> pageWatchers(IRepositoryIdProvider repository) {
		return pageWatchers(repository, PAGE_SIZE);
	}

	/**
	 * Page watches of given repository
	 *
	 * @param repository
	 * @param size
	 * @return page iterator
	 * @deprecated use {@link StargazerService#pageStargazers}
	 */
	public PageIterator<User> pageWatchers(IRepositoryIdProvider repository,
			int size) {
		return pageWatchers(repository, PAGE_FIRST, size);
	}

	/**
	 * Page watches of given repository
	 *
	 * @param repository
	 * @param start
	 * @param size
	 * @return page iterator
	 * @deprecated use {@link StargazerService#pageStargazers}
	 */
	public PageIterator<User> pageWatchers(IRepositoryIdProvider repository,
			int start, int size) {
		PagedRequest<User> request = createWatcherRequest(repository, start,
				size);
		return createPageIterator(request);
	}

	/**
	 * Create page watched request
	 *
	 * @param user
	 * @param start
	 * @param size
	 * @return request
	 * @deprecated use {@link StargazerService#createStarredRequest}
	 */
	protected PagedRequest<Repository> createWatchedRequest(String user,
			int start, int size) {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		PagedRequest<Repository> request = createPagedRequest(start, size);
		StringBuilder uri = new StringBuilder(SEGMENT_USERS);
		uri.append('/').append(UrlUtils.encode(user));
		uri.append(SEGMENT_SUBSCRIPTIONS);
		request.setUri(uri);
		request.setType(new TypeToken<List<Repository>>() {
		}.getType());
		return request;
	}

	/**
	 * Create page watched request
	 *
	 * @param start
	 * @param size
	 * @return request
	 * @deprecated use {@link StargazerService#createStarredRequest}
	 */
	protected PagedRequest<Repository> createWatchedRequest(int start, int size) {
		PagedRequest<Repository> request = createPagedRequest(start, size);
		request.setUri(SEGMENT_USER + SEGMENT_SUBSCRIPTIONS);
		request.setType(new TypeToken<List<Repository>>() {
		}.getType());
		return request;
	}

	/**
	 * Get repositories watched by the given user
	 *
	 * @param user
	 * @return non-null but possibly empty list of repositories
	 * @throws IOException
	 * @deprecated use {@link StargazerService#getStarred}
	 */
	public List<Repository> getWatched(String user) throws IOException {
		PagedRequest<Repository> request = createWatchedRequest(user,
				PAGE_FIRST, PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Page repositories being watched by given user
	 *
	 * @param user
	 * @return page iterator
	 * @throws IOException
	 * @deprecated use {@link StargazerService#pageStarred}
	 */
	public PageIterator<Repository> pageWatched(String user) {
		return pageWatched(user, PAGE_SIZE);
	}

	/**
	 * Page repositories being watched by given user
	 *
	 * @param user
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 * @deprecated use {@link StargazerService#pageStarred}
	 */
	public PageIterator<Repository> pageWatched(String user, int size) {
		return pageWatched(user, PAGE_FIRST, size);
	}

	/**
	 * Page repositories being watched by given user
	 *
	 * @param user
	 * @param start
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 * @deprecated use {@link StargazerService#pageStarred}
	 */
	public PageIterator<Repository> pageWatched(String user, int start, int size) {
		PagedRequest<Repository> request = createWatchedRequest(user, start,
				size);
		return createPageIterator(request);
	}

	/**
	 * Get repositories watched by the currently authenticated user
	 *
	 * @return non-null but possibly empty list of repositories
	 * @throws IOException
	 * @deprecated use {@link StargazerService#getStarred}
	 */
	public List<Repository> getWatched() throws IOException {
		PagedRequest<Repository> request = createWatchedRequest(PAGE_FIRST,
				PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Page repositories being watched by the currently authenticated user
	 *
	 * @return page iterator
	 * @throws IOException
	 * @deprecated use {@link StargazerService#pageStarred}
	 */
	public PageIterator<Repository> pageWatched() {
		return pageWatched(PAGE_SIZE);
	}

	/**
	 * Page repositories being watched by the currently authenticated user
	 *
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 * @deprecated use {@link StargazerService#pageStarred}
	 */
	public PageIterator<Repository> pageWatched(int size) {
		return pageWatched(PAGE_FIRST, size);
	}

	/**
	 * Page repositories being watched by the currently authenticated user
	 *
	 * @param start
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 * @deprecated use {@link StargazerService#pageStarred}
	 */
	public PageIterator<Repository> pageWatched(int start, int size) {
		PagedRequest<Repository> request = createWatchedRequest(start, size);
		return createPageIterator(request);
	}

	/**
	 * Is currently authenticated user watching given repository?
	 *
	 * @param repository
	 * @return true if watch, false otherwise
	 * @throws IOException
	 * @deprecated use {@link StargazerService#isStarring}
	 */
	public boolean isWatching(IRepositoryIdProvider repository)
			throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_SUBSCRIPTION);
		return check(uri.toString());
	}

	/**
	 * Add currently authenticated user as a watcher of the given repository
	 *
	 * @param repository
	 * @throws IOException
	 * @deprecated use {@link StargazerService#star}
	 */
	public void watch(IRepositoryIdProvider repository) throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_SUBSCRIPTION);

		Map<String, String> params = new HashMap<String, String>(1, 1);
		params.put(FIELD_SUBSCRIBED, "true");

		client.put(uri.toString(), params, null);
	}

	/**
	 * Remove currently authenticated user as a watcher of the given repository
	 *
	 * @param repository
	 * @throws IOException
	 * @deprecated use {@link StargazerService#unstar}
	 */
	public void unwatch(IRepositoryIdProvider repository) throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_SUBSCRIPTION);
		client.delete(uri.toString());
	}
}
