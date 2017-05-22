package org.eclipse.egit.github.core.service;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USER;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USERS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_STARRED;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_STARGAZERS;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.util.UrlUtils;

public class StarService extends GitHubService {

	/**
	 * Create starring service
	 */
	public StarService() {
		super();
	}

	/**
	 * Create starring service
	 *
	 * @param client
	 */
        public StarService(GitHubClient client) {
		super(client);
	}

	/**
	 * Create page starring request
	 *
	 * @param repository
	 * @param start
	 * @param size
	 * @return request
	 */
	protected PagedRequest<User> createStargazerRequest(
			IRepositoryIdProvider repository, int start, int size) {
		String id = getId(repository);
		PagedRequest request = createPagedRequest(start, size);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_STARGAZERS);
		request.setUri(uri);
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return request;
	}

	/**
	 * Get user who have starred the given repository
	 *
	 * @param repository
	 * @return non-null but possibly empty list of users
	 * @throws IOException
	 */
	public List<User> getStargazers(IRepositoryIdProvider repository)
			throws IOException {
		PagedRequest request = createStargazerRequest(repository,
				PAGE_FIRST, PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Page users who have starred the given repository
	 *
	 * @param repository
	 * @return page iterator
	 */
	public PageIterator<User> pageStargazers(IRepositoryIdProvider repository) {
		return pageStargazers(repository, PAGE_SIZE);
	}

	/**
	 * Page users who have starred the given repository
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
	 * Page users who have starred the given repository
	 *
	 * @param repository
	 * @param start
	 * @param size
	 * @return page iterator
	 */
	public PageIterator<User> pageStargazers(IRepositoryIdProvider repository,
			int start, int size) {
		PagedRequest request = createStargazerRequest(repository, start, size);
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
			Map<String, String> filterData, int start, int size) {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null");
		if (user.length() == 0) {
			throw new IllegalArgumentException("User cannot be empty");
		}
		PagedRequest request = createPagedRequest(start, size);
		StringBuilder uri = new StringBuilder(SEGMENT_USERS);
		uri.append('/').append(UrlUtils.encode(user));
		uri.append(SEGMENT_STARRED);
		request.setParams(filterData);
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
		PagedRequest request = createPagedRequest(start, size);
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
		return getStarred(user, null);
	}

	/**
	 * Get repositories starred by the given user
	 *
	 * @param user
	 * @param filterData
	 * @return non-null but possibly empty list of repositories
	 * @throws IOException
	 */
	public List<Repository> getStarred(String user, Map<String, String> filterData)
			throws IOException {
		PagedRequest request = createStarredRequest(user, filterData,
				PAGE_FIRST, PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Page repositories being starred by given user
	 *
	 * @param user
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(String user) {
		return pageStarred(user, null, PAGE_SIZE);
	}

	/**
	 * Page repositories being starred by given user
	 *
	 * @param user
	 * @param filterData
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(String user, Map<String, String> filterData) {
		return pageStarred(user, filterData, PAGE_SIZE);
	}

	/**
	 * Page repositories being starred by given user
	 *
	 * @param user
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(String user, int size) {
		return pageStarred(user, null, PAGE_FIRST, size);
	}

	/**
	 * Page repositories being starred by given user
	 *
	 * @param user
	 * @param filterData
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(String user, Map<String, String> filterData,
			int size) {
		return pageStarred(user, filterData, PAGE_FIRST, size);
	}

	/**
	 * Page repositories being starred by given user
	 *
	 * @param user
	 * @param start
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(String user, int start, int size) {
		return pageStarred(user, null, start, size);
	}

	/**
	 * Page repositories being starred by given user
	 *
	 * @param user
	 * @param filterData
	 * @param start
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(String user, Map<String, String> filterData,
			int start, int size) {
		PagedRequest request = createStarredRequest(user, filterData, start, size);
		return createPageIterator(request);
	}

	/**
	 * Get repositories starred by the currently authenticated user
	 *
	 * @return non-null but possibly empty list of repositories
	 * @throws IOException
	 */
	public List<Repository> getStarred() throws IOException {
		PagedRequest request = createStarredRequest(PAGE_FIRST,
			PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Page repositories being starred by the currently authenticated user
	 *
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred() {
		return pageStarred(PAGE_SIZE);
	}

	/**
	 * Page repositories being starred by the currently authenticated user
	 *
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(int size) {
		return pageStarred(PAGE_FIRST, size);
	}

	/**
	 * Page repositories being starred by the currently authenticated user
	 *
	 * @param start
	 * @param size
	 * @return page iterator
	 * @throws IOException
	 */
	public PageIterator<Repository> pageStarred(int start, int size) {
		PagedRequest request = createStarredRequest(start, size);
		return createPageIterator(request);
	}

	/**
	 * Is currently authenticated user starring given repository?
	 *
	 * @param repository
	 * @return true if watch, false otherwise
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
	 * Make currently authenticated user star the given repository
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
	 * Make currently authenticated unstar the given repository
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
