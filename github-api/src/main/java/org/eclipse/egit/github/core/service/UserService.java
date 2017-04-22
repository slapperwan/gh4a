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

import static org.eclipse.egit.github.core.client.IGitHubConstants.CHARSET_UTF8;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_EMAILS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_FOLLOWERS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_FOLLOWING;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_KEYS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_LEGACY;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_SEARCH;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USER;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USERS;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.eclipse.egit.github.core.IResourceProvider;
import org.eclipse.egit.github.core.Key;
import org.eclipse.egit.github.core.SearchUser;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;

/**
 * User service class.
 *
 * @see <a href="http://developer.github.com/v3/users">GitHub users API
 *      documentation</a>
 * @see <a href="http://developer.github.com/v3/users/followers">GitHub
 *      followers API documentation</a>
 * @see <a href="http://developer.github.com/v3/users/emails">GitHub user email
 *      API documentation</a>
 * @see <a href="http://developer.github.com/v3/users/keys">GitHub user keys API
 *      documentation</a>
 */
public class UserService extends GitHubService {
	private static class UserContainer implements IResourceProvider<SearchUser> {

		private List<SearchUser> users;

		/**
		 * @see org.eclipse.egit.github.core.IResourceProvider#getResources()
		 */
		public List<SearchUser> getResources() {
			return users;
		}
	}

	/**
	 * Create user service
	 */
	public UserService() {
		super();
	}

	/**
	 * Create user service
	 *
	 * @param client
	 */
	public UserService(GitHubClient client) {
		super(client);
	}

	/**
	 * Get user with given login name
	 *
	 * @param login
	 * @return user
	 * @throws IOException
	 */
	public User getUser(String login) throws IOException {
		if (login == null)
			throw new IllegalArgumentException("Login name cannot be null"); //$NON-NLS-1$
		if (login.length() == 0)
			throw new IllegalArgumentException("Login name cannot be empty"); //$NON-NLS-1$

		GitHubRequest request = createRequest();
		StringBuilder uri = new StringBuilder(SEGMENT_USERS);
		uri.append('/').append(login);
		request.setUri(uri);
		request.setType(User.class);
		return (User) client.get(request).getBody();
	}

	/**
	 * Get currently authenticated user
	 *
	 * @return user
	 * @throws IOException
	 */
	public User getUser() throws IOException {
		GitHubRequest request = createRequest();
		request.setUri(SEGMENT_USER);
		request.setType(User.class);
		return (User) client.get(request).getBody();
	}

	/**
	 * Edit given user
	 *
	 * @param user
	 * @return edited user
	 * @throws IOException
	 */
	public User editUser(User user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$

		return client.post(SEGMENT_USER, user, User.class);
	}

	/**
	 * Create follower request
	 *
	 * @param start
	 * @param size
	 * @param user
	 * @return request
	 */
	protected PagedRequest<User> createFollowersRequest(int start, int size,
			String user) {
		PagedRequest<User> request = createPagedRequest(start, size);
		if (user == null)
			request.setUri(SEGMENT_USER + SEGMENT_FOLLOWERS);
		else {
			StringBuilder uri = new StringBuilder(SEGMENT_USERS);
			uri.append('/').append(user);
			uri.append(SEGMENT_FOLLOWERS);
			request.setUri(uri);
		}
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return request;
	}

	/**
	 * Create following request
	 *
	 * @param start
	 * @param size
	 * @param user
	 * @return request
	 */
	protected PagedRequest<User> createFollowingRequest(int start, int size,
			String user) {
		PagedRequest<User> request = createPagedRequest(start, size);
		if (user == null)
			request.setUri(SEGMENT_USER + SEGMENT_FOLLOWING);
		else {
			StringBuilder uri = new StringBuilder(SEGMENT_USERS);
			uri.append('/').append(user);
			uri.append(SEGMENT_FOLLOWING);
			request.setUri(uri);
		}
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return request;
	}

	/**
	 * Get all followers of the currently authenticated user
	 *
	 * @return list of followers
	 * @throws IOException
	 */
	public List<User> getFollowers() throws IOException {
		return getAll(pageFollowers());
	}

	/**
	 * Page followers of the currently authenticated user
	 *
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowers() {
		return pageFollowers(PAGE_SIZE);
	}

	/**
	 * Page followers of the currently authenticated user
	 *
	 * @param size
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowers(final int size) {
		return pageFollowers(PAGE_FIRST, size);
	}

	/**
	 * Page followers of the currently authenticated user
	 *
	 * @param start
	 * @param size
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowers(final int start, final int size) {
		PagedRequest<User> request = createFollowersRequest(start, size, null);
		return createPageIterator(request);
	}

	/**
	 * Get all followers of the given user
	 *
	 * @param user
	 * @return list of followers
	 * @throws IOException
	 */
	public List<User> getFollowers(final String user) throws IOException {
		return getAll(pageFollowers(user));
	}

	/**
	 * Page followers of the given user
	 *
	 * @param user
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowers(final String user) {
		return pageFollowers(user, PAGE_SIZE);
	}

	/**
	 * Page followers of the given user
	 *
	 * @param size
	 * @param user
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowers(final String user, final int size) {
		return pageFollowers(user, PAGE_FIRST, size);
	}

	/**
	 * Page followers of the given user
	 *
	 * @param start
	 * @param size
	 * @param user
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowers(final String user, final int start,
			final int size) {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		PagedRequest<User> request = createFollowersRequest(start, size, user);
		return createPageIterator(request);
	}

	/**
	 * Get all users being followed by the currently authenticated user
	 *
	 * @return list of users being followed
	 * @throws IOException
	 */
	public List<User> getFollowing() throws IOException {
		return getAll(pageFollowing());
	}

	/**
	 * Page users being followed by the currently authenticated user
	 *
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowing() {
		return pageFollowing(PAGE_SIZE);
	}

	/**
	 * Page users being followed by the currently authenticated user
	 *
	 * @param size
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowing(final int size) {
		return pageFollowing(PAGE_FIRST, size);
	}

	/**
	 * Page users being followed by the currently authenticated user
	 *
	 * @param start
	 * @param size
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowing(final int start, final int size) {
		PagedRequest<User> request = createFollowingRequest(start, size, null);
		return createPageIterator(request);
	}

	/**
	 * Get all users being followed by the given user
	 *
	 * @param user
	 * @return list of users being followed
	 * @throws IOException
	 */
	public List<User> getFollowing(final String user) throws IOException {
		return getAll(pageFollowing(user));
	}

	/**
	 * Page users being followed by the given user
	 *
	 * @param user
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowing(final String user) {
		return pageFollowing(user, PAGE_SIZE);
	}

	/**
	 * Page users being followed by the given user
	 *
	 * @param user
	 * @param size
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowing(final String user, final int size) {
		return pageFollowing(user, PAGE_FIRST, size);
	}

	/**
	 * Page users being followed by the given user
	 *
	 * @param user
	 * @param start
	 * @param size
	 * @return page iterator
	 */
	public PageIterator<User> pageFollowing(final String user, final int start,
			final int size) {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		PagedRequest<User> request = createFollowingRequest(start, size, user);
		return createPageIterator(request);
	}

	/**
	 * Check if the currently authenticated user is following the given user
	 *
	 * @param user
	 * @return true if following, false if not following
	 * @throws IOException
	 */
	public boolean isFollowing(final String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_USER + SEGMENT_FOLLOWING);
		uri.append('/').append(user);
		return check(uri.toString());
	}

	/**
	 * Follow the given user
	 *
	 * @param user
	 * @throws IOException
	 */
	public void follow(final String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_USER + SEGMENT_FOLLOWING);
		uri.append('/').append(user);
		client.put(uri.toString());
	}

	/**
	 * Unfollow the given user
	 *
	 * @param user
	 * @throws IOException
	 */
	public void unfollow(final String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_USER + SEGMENT_FOLLOWING);
		uri.append('/').append(user);
		client.delete(uri.toString());
	}

	/**
	 * Get all e-mail addresses for the currently authenticated user
	 *
	 * @return list of e-mail address
	 * @throws IOException
	 */
	public List<String> getEmails() throws IOException {
		PagedRequest<String> request = createPagedRequest();
		request.setUri(SEGMENT_USER + SEGMENT_EMAILS);
		request.setType(new TypeToken<List<String>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Add one or more e-mail addresses to the currently authenticated user's
	 * account
	 *
	 * @param emails
	 * @throws IOException
	 */
	public void addEmail(String... emails) throws IOException {
		if (emails == null)
			throw new IllegalArgumentException("Emails cannot be null"); //$NON-NLS-1$
		if (emails.length == 0)
			throw new IllegalArgumentException("Emails cannot be empty"); //$NON-NLS-1$

		client.post(SEGMENT_USER + SEGMENT_EMAILS, emails, null);
	}

	/**
	 * Remove one or more e-mail addresses from the currently authenticated
	 * user's account
	 *
	 * @param emails
	 * @throws IOException
	 */
	public void removeEmail(String... emails) throws IOException {
		if (emails == null)
			throw new IllegalArgumentException("Emails cannot be null"); //$NON-NLS-1$
		if (emails.length == 0)
			throw new IllegalArgumentException("Emails cannot be empty"); //$NON-NLS-1$

		client.delete(SEGMENT_USER + SEGMENT_EMAILS, emails);
	}

	/**
	 * Get all public keys for currently authenticated user
	 *
	 * @return non-null list of public keys
	 * @throws IOException
	 */
	public List<Key> getKeys() throws IOException {
		PagedRequest<Key> request = createPagedRequest();
		request.setUri(SEGMENT_USER + SEGMENT_KEYS);
		request.setType(new TypeToken<List<Key>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Get key with given id
	 *
	 * @param id
	 * @return key
	 * @throws IOException
	 */
	public Key getKey(int id) throws IOException {
		GitHubRequest request = createRequest();
		StringBuilder uri = new StringBuilder(SEGMENT_USER + SEGMENT_KEYS);
		uri.append('/').append(id);
		request.setUri(uri);
		request.setType(Key.class);
		return (Key) client.get(request).getBody();
	}

	/**
	 * Create key for currently authenticated user
	 *
	 * @param key
	 * @return created key
	 * @throws IOException
	 */
	public Key createKey(Key key) throws IOException {
		return client.post(SEGMENT_USER + SEGMENT_KEYS, key, Key.class);
	}

	/**
	 * Edit key for currently authenticated user
	 *
	 * @param key
	 * @return edited key
	 * @throws IOException
	 */
	public Key editKey(Key key) throws IOException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_USER + SEGMENT_KEYS);
		uri.append('/').append(key.getId());
		return client.post(uri.toString(), key, Key.class);
	}

	/**
	 * Delete key with given id
	 *
	 * @param id
	 * @throws IOException
	 */
	public void deleteKey(int id) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_USER + SEGMENT_KEYS);
		uri.append('/').append(id);
		client.delete(uri.toString());
	}

	/**
	 * Search for users matching query.
	 *
	 * @param query
	 * @return list of users
	 * @throws IOException
	 */
	public List<SearchUser> searchUsers(final String query) throws IOException {
		return searchUsers(query, -1);
	}

	/**
	 * Search for users matching query.
	 *
	 * @param query
	 * @param startPage
	 * @return list of users
	 * @throws IOException
	 */
	public List<SearchUser> searchUsers(final String query, final int startPage)
			throws IOException {
		if (query == null)
			throw new IllegalArgumentException("Query cannot be null"); //$NON-NLS-1$
		if (query.length() == 0)
			throw new IllegalArgumentException("Query cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_LEGACY + SEGMENT_USER
				+ SEGMENT_SEARCH);
		final String encodedQuery = URLEncoder.encode(query, CHARSET_UTF8)
				.replace("+", "%20") //$NON-NLS-1$ //$NON-NLS-2$
				.replace(".", "%2E"); //$NON-NLS-1$ //$NON-NLS-2$
		uri.append('/').append(encodedQuery);

		PagedRequest<SearchUser> request = createPagedRequest();

		request.setUri(uri);
		request.setType(UserContainer.class);
		return getAll(request);
	}
}
