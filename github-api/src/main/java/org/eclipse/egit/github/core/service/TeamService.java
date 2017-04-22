/******************************************************************************
 *  Copyright (c) 2011, 2014 GitHub Inc. and others
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Michael Mathews (Arizona Board of Regents) - (Bug: 447419)
 *    			 Team Membership API implementation
 *****************************************************************************/
package org.eclipse.egit.github.core.service;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_MEMBERS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_MEMBERSHIPS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_ORGS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_TEAMS;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.Team;
import org.eclipse.egit.github.core.TeamMembership;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.PagedRequest;

/**
 * Service class for working with organization teams
 *
 * @see <a href="http://developer.github.com/v3/orgs/teams">GitHub team API
 *      documentation</a>
 */
public class TeamService extends GitHubService {

	/**
	 * Create team service
	 */
	public TeamService() {
		super();
	}

	/**
	 * Create team service
	 *
	 * @param client
	 */
	public TeamService(GitHubClient client) {
		super(client);
	}

	/**
	 * Get team with given id
	 *
	 * @param id
	 * @return team
	 * @throws IOException
	 */
	public Team getTeam(int id) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(Team.class);
		return (Team) client.get(request).getBody();
	}

	/**
	 * Get all teams in the given organization
	 *
	 * @param organization
	 * @return list of teams
	 * @throws IOException
	 */
	public List<Team> getTeams(String organization) throws IOException {
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (organization.length() == 0)
			throw new IllegalArgumentException("Organization cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(organization);
		uri.append(SEGMENT_TEAMS);
		PagedRequest<Team> request = createPagedRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<Team>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Create the given team
	 *
	 * @param organization
	 * @param team
	 * @return created team
	 * @throws IOException
	 */
	public Team createTeam(String organization, Team team) throws IOException {
		return createTeam(organization, team, null);
	}

	/**
	 * Create the given team
	 *
	 * @param organization
	 * @param team
	 * @param repoNames
	 * @return created team
	 * @throws IOException
	 */
	public Team createTeam(String organization, Team team,
			List<String> repoNames) throws IOException {
		if (organization == null)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (organization.length() == 0)
			throw new IllegalArgumentException("Organization cannot be null"); //$NON-NLS-1$
		if (team == null)
			throw new IllegalArgumentException("Team cannot be null"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(organization);
		uri.append(SEGMENT_TEAMS);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", team.getName()); //$NON-NLS-1$
		params.put("permission", team.getPermission()); //$NON-NLS-1$
		if (repoNames != null)
			params.put("repo_names", repoNames); //$NON-NLS-1$
		return client.post(uri.toString(), params, Team.class);
	}

	/**
	 * Edit the given team
	 *
	 * @param team
	 * @return edited team
	 * @throws IOException
	 */
	public Team editTeam(Team team) throws IOException {
		if (team == null)
			throw new IllegalArgumentException("Team cannot be null"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(team.getId());
		return client.post(uri.toString(), team, Team.class);
	}

	/**
	 * Delete the team with the given id
	 *
	 * @param id
	 * @throws IOException
	 */
	public void deleteTeam(int id) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		client.delete(uri.toString());
	}

	/**
	 * Get members of team with given id
	 *
	 * @param id
	 * @return team members
	 * @throws IOException
	 */
	public List<User> getMembers(int id) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_MEMBERS);
		PagedRequest<User> request = createPagedRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<User>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Is the given user a member of the team with the given id
	 *
	 * @param id
	 * @param user
	 * @return true if member, false if not member
	 * @throws IOException
	 */
	public boolean isMember(int id, String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_MEMBERS);
		uri.append('/').append(user);
		return check(uri.toString());
	}

	/**
	 * Add given user to team with given id
	 *
	 * @param id
	 * @param user
	 * @throws IOException
	 */
	public void addMember(int id, String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_MEMBERS);
		uri.append('/').append(user);
		client.put(uri.toString());
	}

	/**
	 * Remove given user from team with given id
	 *
	 * @param id
	 * @param user
	 * @throws IOException
	 */
	public void removeMember(int id, String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_MEMBERS);
		uri.append('/').append(user);
		client.delete(uri.toString());
	}

	public TeamMembership getMembership(int id, String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_MEMBERSHIPS);
		uri.append('/').append(user);

		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(TeamMembership.class);
		return (TeamMembership) client.get(request).getBody();
	}

	public TeamMembership addMembership(int id, String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_MEMBERSHIPS);
		uri.append('/').append(user);
		return client.put(uri.toString(), null, TeamMembership.class);
	}

	public void removeMembership(int id, String user) throws IOException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null"); //$NON-NLS-1$
		if (user.length() == 0)
			throw new IllegalArgumentException("User cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_MEMBERSHIPS);
		uri.append('/').append(user);
		client.delete(uri.toString());
	}

	/**
	 * Get all repositories for given team
	 *
	 * @param id
	 * @return non-null list of repositories
	 * @throws IOException
	 */
	public List<Repository> getRepositories(int id) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_REPOS);
		PagedRequest<Repository> request = createPagedRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<Repository>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Is given repository managed by given team
	 *
	 * @param id
	 * @param repository
	 * @return true if managed by team, false otherwise
	 * @throws IOException
	 */
	public boolean isTeamRepository(int id, IRepositoryIdProvider repository)
			throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		return check(uri.toString());
	}

	/**
	 * Add repository to team
	 *
	 * @param id
	 * @param repository
	 * @throws IOException
	 */
	public void addRepository(int id, IRepositoryIdProvider repository)
			throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		client.put(uri.toString());
	}

	/**
	 * Remove repository from team
	 *
	 * @param id
	 * @param repository
	 * @throws IOException
	 */
	public void removeRepository(int id, IRepositoryIdProvider repository)
			throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_TEAMS);
		uri.append('/').append(id);
		uri.append(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		client.delete(uri.toString());
	}

	/**
	 * Get teams associated with given repository
	 *
	 * @param repository
	 * @return list of teams
	 * @throws IOException
	 */
	public List<Team> getTeams(IRepositoryIdProvider repository)
			throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_TEAMS);
		PagedRequest<Team> request = createPagedRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<Team>>() {
		}.getType());
		return getAll(request);
	}
}
