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
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_CONTENTS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_README;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;

import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;

/**
 * Service for accessing repository contents
 *
 * @see <a href="http://developer.github.com/v3/repos/contents">GitHub contents
 *      API documentation</a>
 */
public class ContentsService extends GitHubService {

	/**
	 * Create contents service
	 */
	public ContentsService() {
		super();
	}

	/**
	 * Create contents service
	 *
	 * @param client
	 */
	public ContentsService(final GitHubClient client) {
		super(client);
	}

	/**
	 * Get repository README
	 *
	 * @param repository
	 * @return README
	 * @throws Exception
	 */
	public RepositoryContents getReadme(IRepositoryIdProvider repository)
			throws Exception {
		return getReadme(repository, null);
	}

	/**
	 * Get repository README
	 *
	 * @param repository
	 * @param ref
	 * @return README
	 * @throws IOException
	 */
	public RepositoryContents getReadme(IRepositoryIdProvider repository,
			String ref) throws IOException {
		String id = getId(repository);

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_README);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		if (ref != null && ref.length() > 0)
			request.setParams(Collections.singletonMap("ref", ref));
		request.setType(RepositoryContents.class);
		return (RepositoryContents) client.get(request).getBody();
	}

	/**
	 * Get HTML for the repository's README
	 *
	 * @param repository
	 * @return HTML
	 * @throws IOException
	 */
	public String getReadmeHtml(IRepositoryIdProvider repository)
			throws IOException {
		return getReadmeHtml(repository, null);
	}

	/**
	 * Get HTML for the repository's README
	 *
	 * @param repository
	 * @param ref
	 * @return HTML
	 * @throws IOException
	 */
	public String getReadmeHtml(IRepositoryIdProvider repository,
			String ref) throws IOException {
		String id = getId(repository);

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_README);

		GitHubRequest request = createRequest();
		request.setUri(uri);
		if (ref != null && ref.length() > 0)
			request.setParams(Collections.singletonMap("ref", ref));

		return client.getStreamContents(request);
	}

	/**
	 * Get contents at the root of the given repository on master branch
	 *
	 * @param repository
	 * @return list of contents at root
	 * @throws IOException
	 */
	public List<RepositoryContents> getContents(IRepositoryIdProvider repository)
			throws IOException {
		return getContents(repository, null);
	}

	/**
	 * Get contents at path in the given repository on master branch
	 *
	 * @param repository
	 * @param path
	 * @return list of contents at path
	 * @throws IOException
	 */
	public List<RepositoryContents> getContents(
			IRepositoryIdProvider repository, String path) throws IOException {
		return getContents(repository, path, null);
	}

	/**
	 * Get contents of path at reference in given repository
	 * <p>
	 * For file paths this will return a list with one entry corresponding to
	 * the file contents at the given path
	 *
	 * @param repository
	 * @param path
	 * @param ref
	 * @return list of contents at path
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public List<RepositoryContents> getContents(
			IRepositoryIdProvider repository, String path, String ref)
			throws IOException {
		String id = getId(repository);

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_CONTENTS);
		if (path != null && path.length() > 0) {
			final String encodedPath = URLEncoder.encode(path, CHARSET_UTF8)
				.replace("+", "%20") //$NON-NLS-1$ //$NON-NLS-2$
				.replace(".", "%2E"); //$NON-NLS-1$ //$NON-NLS-2$
			if (path.charAt(0) != '/')
				uri.append('/');
			uri.append(encodedPath);
		}
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(RepositoryContents.class);
		request.setArrayType(new TypeToken<List<RepositoryContents>>() {
		}.getType());
		if (ref != null && ref.length() > 0)
			request.setParams(Collections.singletonMap("ref", ref));

		Object body = client.get(request).getBody();
		if (body instanceof RepositoryContents)
			return Collections.singletonList((RepositoryContents) body);
		else
			return (List<RepositoryContents>) body;
	}
}
