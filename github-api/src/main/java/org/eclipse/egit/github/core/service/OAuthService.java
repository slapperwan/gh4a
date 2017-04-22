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

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_AUTHORIZATIONS;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Authorization;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.PagedRequest;

/**
 * Service for interacting with a user's OAUth authorizations
 *
 * @see <a href="http://developer.github.com/v3/oauth/">GitHub OAuth API
 *      documentation</a>
 */
public class OAuthService extends GitHubService {

	/**
	 * Create OAuth service
	 */
	public OAuthService() {
		super();
	}

	/**
	 * Create OAuth service
	 *
	 * @param client
	 */
	public OAuthService(GitHubClient client) {
		super(client);
	}

	/**
	 * Get all authorizations for currently authenticated user
	 *
	 * @return list of authorizations
	 * @throws IOException
	 */
	public List<Authorization> getAuthorizations() throws IOException {
		PagedRequest<Authorization> request = createPagedRequest();
		request.setUri(SEGMENT_AUTHORIZATIONS);
		request.setType(new TypeToken<List<Authorization>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Get authorization with given id
	 *
	 * @param id
	 * @return authorization
	 * @throws IOException
	 */
	public Authorization getAuthorization(int id) throws IOException {
		GitHubRequest request = createRequest();
		StringBuilder uri = new StringBuilder(SEGMENT_AUTHORIZATIONS);
		uri.append('/').append(id);
		request.setUri(uri);
		request.setType(Authorization.class);
		return (Authorization) client.get(request).getBody();
	}

	/**
	 * Delete authorization with given id
	 *
	 * @param id
	 * @throws IOException
	 */
	public void deleteAuthorization(int id) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_AUTHORIZATIONS);
		uri.append('/').append(id);
		client.delete(uri.toString());
	}

	/**
	 * Create authorization
	 *
	 * @param authorization
	 * @return authorization
	 * @throws IOException
	 */
	public Authorization createAuthorization(Authorization authorization)
			throws IOException {
		return client.post(SEGMENT_AUTHORIZATIONS, authorization,
				Authorization.class);
	}

	/**
	 * Add scopes to authorization
	 *
	 * @param id
	 * @param scopes
	 * @return authorization
	 * @throws IOException
	 */
	public Authorization addScopes(int id, Collection<String> scopes)
			throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_AUTHORIZATIONS);
		uri.append('/').append(id);
		Map<String, Collection<String>> params = Collections.singletonMap(
				"add_scopes", scopes); //$NON-NLS-1$
		return client.post(uri.toString(), params, Authorization.class);
	}

	/**
	 * Remove scopes from authorization
	 *
	 * @param id
	 * @param scopes
	 * @return authorization
	 * @throws IOException
	 */
	public Authorization removeScopes(int id, Collection<String> scopes)
			throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_AUTHORIZATIONS);
		uri.append('/').append(id);
		Map<String, Collection<String>> params = Collections.singletonMap(
				"remove_scopes", scopes); //$NON-NLS-1$
		return client.post(uri.toString(), params, Authorization.class);
	}

	/**
	 * Set scopes for authorization
	 *
	 * @param id
	 * @param scopes
	 * @return authorization
	 * @throws IOException
	 */
	public Authorization setScopes(int id, Collection<String> scopes)
			throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_AUTHORIZATIONS);
		uri.append('/').append(id);
		Map<String, Collection<String>> params = Collections.singletonMap(
				"scopes", scopes); //$NON-NLS-1$
		return client.post(uri.toString(), params, Authorization.class);
	}
}
