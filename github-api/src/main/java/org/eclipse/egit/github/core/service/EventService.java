/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Jason Tsay (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core.service;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_EVENTS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_NETWORKS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_ORGS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_PUBLIC;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_RECEIVED_EVENTS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_USERS;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

import com.google.gson.reflect.TypeToken;

import java.util.List;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.event.Event;

/**
 * Service class for interacting with Events
 *
 * @see <a href="http://developer.github.com/v3/events">GitHub Event API
 *      documentation</a>
 * @see <a href="http://developer.github.com/v3/events/types">GitHub Event types
 *      API documentation</a>
 */
public class EventService extends GitHubService {

	/**
	 * Create event service
	 */
	public EventService() {
		super();
	}

	/**
	 * Create event service
	 *
	 * @param client
	 */
	public EventService(GitHubClient client) {
		super(client);
	}

	/**
	 * Create page iterator for all public events
	 *
	 * @return events page iterator
	 */
	public PageIterator<Event> pagePublicEvents() {
		return pagePublicEvents(PAGE_SIZE);
	}

	/**
	 * Create page iterator for all public events
	 *
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pagePublicEvents(final int size) {
		return pagePublicEvents(PAGE_FIRST, size);
	}

	/**
	 * Create page iterator for all public events
	 *
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pagePublicEvents(final int start, final int size) {
		PagedRequest<Event> request = createPagedRequest(start, size);
		request.setUri(SEGMENT_EVENTS);
		request.setType(new TypeToken<List<Event>>() {
		}.getType());
		return createPageIterator(request);
	}

	/**
	 * Create page iterator for a given repo's events
	 *
	 * @param repository
	 * @return events page iterator
	 */
	public PageIterator<Event> pageEvents(IRepositoryIdProvider repository) {
		return pageEvents(repository, PAGE_SIZE);
	}

	/**
	 * Create page iterator for a given repo's events
	 *
	 * @param repository
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageEvents(IRepositoryIdProvider repository,
			final int size) {
		return pageEvents(repository, PAGE_FIRST, size);
	}

	/**
	 * Create page iterator for a given repo's events
	 *
	 * @param repository
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageEvents(IRepositoryIdProvider repository,
			final int start, final int size) {
		PagedRequest<Event> request = createRepoEventRequest(repository, start,
				size);
		return createPageIterator(request);
	}

	/**
	 * Create page iterator for a given repo network's events
	 *
	 * @param repository
	 * @return events page iterator
	 */
	public PageIterator<Event> pageNetworkEvents(
			IRepositoryIdProvider repository) {
		return pageNetworkEvents(repository, PAGE_SIZE);
	}

	/**
	 * Create page iterator for a given repo network's events
	 *
	 * @param repository
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageNetworkEvents(
			IRepositoryIdProvider repository, final int size) {
		return pageNetworkEvents(repository, PAGE_FIRST, size);
	}

	/**
	 * Create page iterator for a given repo network's events
	 *
	 * @param repository
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageNetworkEvents(
			IRepositoryIdProvider repository, final int start, final int size) {
		PagedRequest<Event> request = createNetworkRepoEventRequest(repository,
				start, size);
		return createPageIterator(request);
	}

	/**
	 * Create page iterator for a given org's events
	 *
	 * @param org
	 * @return events page iterator
	 */
	public PageIterator<Event> pageOrgEvents(String org) {
		return pageOrgEvents(org, PAGE_SIZE);
	}

	/**
	 * Create page iterator for a given org's events
	 *
	 * @param org
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageOrgEvents(String org, final int size) {
		return pageOrgEvents(org, PAGE_FIRST, size);
	}

	/**
	 * Create page iterator for a given org's events
	 *
	 * @param org
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageOrgEvents(String org, final int start,
			final int size) {
		PagedRequest<Event> request = createOrgEventRequest(org, start, size);
		return createPageIterator(request);
	}

	/**
	 * Create page iterator for a given user's received events Returns private
	 * events if authenticated as the user
	 *
	 * @param user
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserReceivedEvents(String user) {
		return pageUserReceivedEvents(user, false);
	}

	/**
	 * Create page iterator for a given user's received events
	 *
	 * @param user
	 * @param isPublic
	 *            only include public events?
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserReceivedEvents(String user,
			boolean isPublic) {
		return pageUserReceivedEvents(user, isPublic, PAGE_SIZE);
	}

	/**
	 * Create page iterator for a given user's received events
	 *
	 * @param user
	 * @param isPublic
	 *            only include public events?
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserReceivedEvents(String user,
			boolean isPublic, final int size) {
		return pageUserReceivedEvents(user, isPublic, PAGE_FIRST, size);
	}

	/**
	 * Create page iterator for a given user's received events
	 *
	 * @param user
	 * @param isPublic
	 *            only include public events?
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserReceivedEvents(String user,
			boolean isPublic, final int start, final int size) {
		PagedRequest<Event> request = createUserReceivedEventRequest(user,
				isPublic, start, size);
		return createPageIterator(request);
	}

	/**
	 * Create page iterator for a given user's events Returns private events if
	 * authenticated as the user
	 *
	 * @param user
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserEvents(String user) {
		return pageUserEvents(user, false);
	}

	/**
	 * Create page iterator for a given user's events
	 *
	 * @param user
	 * @param isPublic
	 *            only include public events?
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserEvents(String user, boolean isPublic) {
		return pageUserEvents(user, isPublic, PAGE_SIZE);
	}

	/**
	 * Create page iterator for a given user's events
	 *
	 * @param user
	 * @param isPublic
	 *            only include public events?
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserEvents(String user, boolean isPublic,
			final int size) {
		return pageUserEvents(user, isPublic, PAGE_FIRST, size);
	}

	/**
	 * Create page iterator for a given user's events
	 *
	 * @param user
	 * @param isPublic
	 *            only include public events?
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserEvents(String user, boolean isPublic,
			final int start, final int size) {
		PagedRequest<Event> request = createUserEventRequest(user, isPublic,
				start, size);
		return createPageIterator(request);
	}

	/**
	 * Create page iterator for a given user's orgs events
	 *
	 * @param user
	 * @param org
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserOrgEvents(String user, String org) {
		return pageUserOrgEvents(user, org, PAGE_SIZE);
	}

	/**
	 * Create page iterator for a given user's orgs events
	 *
	 * @param user
	 * @param org
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserOrgEvents(String user, String org,
			final int size) {
		return pageUserOrgEvents(user, org, PAGE_FIRST, size);
	}

	/**
	 * Create page iterator for a given user's orgs events
	 *
	 * @param user
	 * @param org
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	public PageIterator<Event> pageUserOrgEvents(String user, String org,
			final int start, final int size) {
		PagedRequest<Event> request = createUserOrgEventRequest(user, org,
				start, size);
		return createPageIterator(request);
	}

	/**
	 * Create repo event page request
	 *
	 * @param repository
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	protected PagedRequest<Event> createRepoEventRequest(
			IRepositoryIdProvider repository, int start, int size) {
		final String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_EVENTS);
		PagedRequest<Event> request = createPagedRequest(start, size);
		request.setUri(uri).setType(new TypeToken<List<Event>>() {
		}.getType());
		return request;
	}

	/**
	 * Create network repo event page request
	 *
	 * @param repository
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	protected PagedRequest<Event> createNetworkRepoEventRequest(
			IRepositoryIdProvider repository, int start, int size) {
		final String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_NETWORKS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_EVENTS);
		PagedRequest<Event> request = createPagedRequest(start, size);
		request.setUri(uri).setType(new TypeToken<List<Event>>() {
		}.getType());
		return request;
	}

	/**
	 * Create org event page request
	 *
	 * @param org
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	protected PagedRequest<Event> createOrgEventRequest(String org, int start,
			int size) {
		StringBuilder uri = new StringBuilder(SEGMENT_ORGS);
		uri.append('/').append(org);
		uri.append(SEGMENT_EVENTS);
		PagedRequest<Event> request = createPagedRequest(start, size);
		request.setUri(uri).setType(new TypeToken<List<Event>>() {
		}.getType());
		return request;
	}

	/**
	 * Create user received event page request
	 *
	 * @param user
	 * @param isPublic
	 *            only include public events?
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	protected PagedRequest<Event> createUserReceivedEventRequest(String user,
			boolean isPublic, int start, int size) {
		StringBuilder uri = new StringBuilder(SEGMENT_USERS);
		uri.append('/').append(user);
		uri.append(SEGMENT_RECEIVED_EVENTS);
		if (isPublic)
			uri.append(SEGMENT_PUBLIC);
		PagedRequest<Event> request = createPagedRequest(start, size);
		request.setUri(uri).setType(new TypeToken<List<Event>>() {
		}.getType());
		return request;
	}

	/**
	 * Create user event page request
	 *
	 * @param user
	 * @param isPublic
	 *            only include public events?
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	protected PagedRequest<Event> createUserEventRequest(String user,
			boolean isPublic, int start, int size) {
		StringBuilder uri = new StringBuilder(SEGMENT_USERS);
		uri.append('/').append(user);
		uri.append(SEGMENT_EVENTS);
		if (isPublic)
			uri.append(SEGMENT_PUBLIC);
		PagedRequest<Event> request = createPagedRequest(start, size);
		request.setUri(uri).setType(new TypeToken<List<Event>>() {
		}.getType());
		return request;
	}

	/**
	 * Create user's organization event page request
	 *
	 * @param user
	 * @param org
	 * @param start
	 *            starting page number
	 * @param size
	 *            size of page
	 * @return events page iterator
	 */
	protected PagedRequest<Event> createUserOrgEventRequest(String user,
			String org, int start, int size) {
		StringBuilder uri = new StringBuilder(SEGMENT_USERS);
		uri.append('/').append(user);
		uri.append(SEGMENT_EVENTS).append(SEGMENT_ORGS);
		uri.append('/').append(org);
		PagedRequest<Event> request = createPagedRequest(start, size);
		request.setUri(uri).setType(new TypeToken<List<Event>>() {
		}.getType());
		return request;
	}
}
