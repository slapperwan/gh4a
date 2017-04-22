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

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_MEMBERS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_ORGS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_PUBLIC_MEMBERS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USER;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USERS;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.PagedRequest;

/**
 * Organization service class
 *
 * @see <a href="http://developer.github.com/v3/orgs">GitHub organization API
 *      documentation</a>
 * @see <a href="http://developer.github.com/v3/orgs/members">GitHub
 *      organization membership API documentation</a>
 */
public class OrganizationService extends GitHubService {

	/**
	 * Filter for roles a member can have
	 * @since 4.2
	 */
	public static enum RoleFilter {
		all, admin, member
	}

	/**
	 * Create organization service
	 */
	public OrganizationService() {
		super();
	}

	/**
	 * Create organization service
	 *
	 * @param client
	 */
	public OrganizationService(GitHubClient client) {
		super(client);
	}

	/**
	 * Create org request
	 *
	 * @param user
	 * @param start
	 * @param size
	 * @return request
	 */
	protected PagedRequest<User> createOrgRequest(String user, int start,
			int size) {
		PagedRequest<User> request = new PagedRequest<User>(start, size);
		if (user == null)
			request.setUri(SEGMENT_USER + SEGMENT_ORGS);
		else {
			StringBuilder uri = new StringBuilder(SEGMENT_USERS);
			uri.append('/').append(user);
			uri.append(SEGMENT_ORGS);
			request.setUri(uri);
		}
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return request;
	}

	/**
	 * Get organizations that the currently authenticated user is a member of
	 *
	 * @return list of organizations
	 * @throws IOException
	 */
	public List<User> getOrganizations() throws IOException {
		PagedRequest<User> request = createOrgRequest(null, PAGE_FIRST,
				PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Get organizations that the given user is a member of
	 *
	 * @param user
	 * @return list of organizations
	 * @throws IOException
	 */
	public List<User> getOrganizations(String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		PagedRequest<User> request = createOrgRequest(user, PAGE_FIRST,
				PAGE_SIZE);
		return getAll(request);
	}

	/**
	 * Get organization with the given name
	 *
	 * @param name
	 * @return organization
	 * @throws IOException
	 */
	public User getOrganization(String name) throws IOException {
		if (name == null)
			throw new IllegalArgumentException("Name cannot be null"); //$NON-NLS-1$
		if (name.length() == 0)
			throw new IllegalArgumentException("Name cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(name);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(User.class);
		return (User) client.get(request).getBody();
	}

	/**
	 * Edit given organization
	 *
	 * @param organization
	 * @return edited organization
	 * @throws IOException
	 */
	public User editOrganization(User organization) throws IOException {
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		final String name = organization.getLogin();
		if (name == null)
			throw new IllegalArgumentException(
					"Organization login cannot be null"); //$NON-NLS-1$
		if (name.length() == 0)
			throw new IllegalArgumentException(
					"Organization login cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(name);
		return client.post(uri.toString(), organization, User.class);
	}

	/**
	 * Get members of organization
	 *
	 * @param organization
	 *          the name of the organization
	 * @return list of all organization members
	 * @throws IOException
	 */
	public List<User> getMembers(String organization) throws IOException {
		return getMembers(organization, null);
	}

	/**
	 * Get members of organization
	 *
	 * @param organization
	 *          the name of the organization
	 * @param roleFilter
	 *          only return members matching the {@link RoleFilter}<br>
	 *          To use this feature it is currently required to set the
	 *          {@link org.eclipse.egit.github.core.service.GitHubService#ACCEPT_PREVIEW_IRONMAN
	 *          application/vnd.github.ironman-preview+json} Accept header in the
	 *          {@link GitHubClient#setHeaderAccept GitHubClient}
	 * @return list of all organization members whose role matches the {@code roleFilter}
	 * @throws IOException
	 * @since 4.2
	 */
	public List<User> getMembers(String organization, RoleFilter roleFilter) throws IOException
	{
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (organization.length() == 0)
			throw new IllegalArgumentException("Organization cannot be empty"); //$NON-NLS-1$

		HashMap<String, String> params = new HashMap<String, String>();
		if(roleFilter != null) params.put("role", roleFilter.toString());

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(organization);
		uri.append(SEGMENT_MEMBERS);
		PagedRequest<User> request = createPagedRequest();
		request.setParams(params);
		request.setUri(uri);
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Get public members of organization
	 *
	 * @param organization
	 * @return list of public organization members
	 * @throws IOException
	 */
	public List<User> getPublicMembers(String organization) throws IOException {
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (organization.length() == 0)
			throw new IllegalArgumentException("Organization cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(organization);
		uri.append(SEGMENT_PUBLIC_MEMBERS);
		PagedRequest<User> request = createPagedRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Check if the given user is a member of the given organization
	 *
	 * @param organization
	 * @param user
	 * @return true if member, false if not member
	 * @throws IOException
	 */
	public boolean isMember(String organization, String user)
			throws IOException {
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (organization.length() == 0)
			throw new IllegalArgumentException("Organization cannot be empty"); //$NON-NLS-1$
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(organization);
		uri.append(SEGMENT_MEMBERS);
		uri.append('/').append(user);
		return check(uri.toString());
	}

	/**
	 * Check if the given user is a public member of the given organization
	 *
	 * @param organization
	 * @param user
	 * @return true if public member, false if not public member
	 * @throws IOException
	 */
	public boolean isPublicMember(String organization, String user)
			throws IOException {
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (organization.length() == 0)
			throw new IllegalArgumentException("Organization cannot be empty"); //$NON-NLS-1$
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(organization);
		uri.append(SEGMENT_PUBLIC_MEMBERS);
		uri.append('/').append(user);
		return check(uri.toString());
	}

	/**
	 * Publicize membership of given user in given organization
	 *
	 * @param organization
	 * @param user
	 * @throws IOException
	 */
	public void showMembership(String organization, String user)
			throws IOException {
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (organization.length() == 0)
			throw new IllegalArgumentException("Organization cannot be empty"); //$NON-NLS-1$
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(organization);
		uri.append(SEGMENT_PUBLIC_MEMBERS);
		uri.append('/').append(user);
		client.put(uri.toString());
	}

	/**
	 * Conceal membership of given user in given organization
	 *
	 * @param organization
	 * @param user
	 * @throws IOException
	 */
	public void hideMembership(String organization, String user)
			throws IOException {
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (organization.length() == 0)
			throw new IllegalArgumentException("Organization cannot be empty"); //$NON-NLS-1$
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(organization);
		uri.append(SEGMENT_PUBLIC_MEMBERS);
		uri.append('/').append(user);
		client.delete(uri.toString());
	}

	/**
	 * Remove the given member from the given organization
	 *
	 * @param organization
	 * @param user
	 * @throws IOException
	 */
	public void removeMember(String organization, String user)
			throws IOException {
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (organization.length() == 0)
			throw new IllegalArgumentException("Organization cannot be empty"); //$NON-NLS-1$
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(organization);
		uri.append(SEGMENT_MEMBERS);
		uri.append('/').append(user);
		client.delete(uri.toString());
	}
}
