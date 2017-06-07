/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.github.core.service;

import com.google.gson.reflect.TypeToken;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Notification;
import org.eclipse.egit.github.core.ThreadSubscription;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_NOTIFICATIONS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_SUBSCRIPTION;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_THREADS;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

public class NotificationService extends GitHubService {

	/**
	 * Last notifications check point field name
	 */
	public static final String FIELD_LAST_READ_AT = "last_read_at";

	/**
	 * Conversation subscription status field name
	 */
	public static final String FIELD_SUBSCRIBED = "subscribed";

	/**
	 * Conversation blocking status field name
	 */
	public static final String FIELD_IGNORED = "ignored";

	public static final String FIELD_ALL = "all";

	public  static final String FIELD_PARTICIPATING = "participating";

	/**
	 * Notification reason for being assigned to the issue
	 */
	public static final String REASON_ASSIGN = "assign";

	/**
	 * Notification reason for being author of the thread
	 */
	public static final String REASON_AUTHOR = "author";

	/**
	 * Notification reason for commenting on the thread
	 */
	public static final String REASON_COMMENT = "comment";

	/**
	 * Notification reason for accepting an invitation to contribute to the
	 * repository
	 */
	public static final String REASON_INVITATION = "invitation";

	/**
	 * Notification reason for manual subscription to the thread
	 */
	public static final String REASON_MANUAL = "manual";

	/**
	 * Notification reason for being @mentioned in the thread
	 */
	public static final String REASON_MENTION = "mention";

	/**
	 * Notification reason for changing the thread state (e.g. closing issue,
	 * merging pull request)
	 */
	public static final String REASON_STATE_CHANGE = "state_change";

	/**
	 * Notification reason for watching the repository
	 */
	public static final String REASON_SUBSCRIBED = "subscribed";

	/**
	 * Notification reason for being on a mentioned team
	 */
	public static final String REASON_TEAM_MENTION = "team_mention";

	/**
	 * Create notification service
	 */
	public NotificationService() {
		super();
	}

	/**
	 * Create notification service
	 *
	 * @param client cannot be null
	 */
	public NotificationService(GitHubClient client) {
		super(client);
	}

	/**
	 * Get notifications for currently authenticated user
	 *
	 * @return non-null but possibly empty list of notifications
	 * @throws IOException if something went wrong
	 */
	public List<Notification> getNotifications(boolean all,
			boolean participating) throws IOException {
		return getAll(pageNotifications(all, participating));
	}

	/**
	 * Page notifications for currently authenticated user
	 *
	 * @return iterator over pages of notifications
	 ** @param all
	 *** @param participating
	 */
	public PageIterator<Notification> pageNotifications(boolean all, boolean participating) {
		return pageNotifications(PAGE_SIZE, all, participating);
	}

	/**
	 * Page notifications for currently authenticated user
	 *
	 * @param size the number of pages
	 * @param all
	 **@param participating @return iterator over pages of notifications
	 */
	public PageIterator<Notification> pageNotifications(int size, boolean all,
			boolean participating) {
		return pageNotifications(PAGE_FIRST, size, all, participating);
	}

	/**
	 * Page notifications for currently authenticated user
	 *
	 * @param start the number of first page to load
	 * @param size  the number of pages
	 * @param all
	 *@param participating @return iterator over pages of notifications
	 */
	public PageIterator<Notification> pageNotifications(int start, int size, boolean all,
			boolean participating) {
		PagedRequest<Notification> request = createPagedRequest(start, size);
		request.setUri(SEGMENT_NOTIFICATIONS);
		request.setType(new TypeToken<List<Notification>>() {
		}.getType());

		Map<String, String> params = new HashMap<>();
		params.put(FIELD_ALL, String.valueOf(all));
		params.put(FIELD_PARTICIPATING, String.valueOf(participating));
		request.setParams(params);

		return createPageIterator(request);
	}

	/**
	 * Get bulk notifications request
	 *
	 * @param repoId the id of the repository
	 * @param start  the number of first page to load
	 * @param size   the number of pages
	 * @return iterator over pages of notifications
	 */
	protected PagedRequest<Notification> createNotificationsRequest(
			String repoId, int start, int size) {
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_NOTIFICATIONS);

		PagedRequest<Notification> request = createPagedRequest(start, size);
		request.setUri(uri);
		request.setType(new TypeToken<List<Notification>>() {
		}.getType());
		return request;
	}

	/**
	 * Get notifications for currently authenticated user
	 *
	 * @param user       the repository owner
	 * @param repository the repository name
	 * @return non-null but possibly empty list of notifications
	 * @throws IOException if something went wrong
	 */
	public List<Notification> getNotifications(String user, String repository)
			throws IOException {
		return getAll(pageNotifications(user, repository));
	}

	/**
	 * Get notifications for currently authenticated user
	 *
	 * @param repository interface to provide an id for a repository
	 * @return non-null but possibly empty list of notifications
	 * @throws IOException if something went wrong
	 */
	public List<Notification> getNotifications(IRepositoryIdProvider repository)
			throws IOException {
		return getAll(pageNotifications(repository));
	}

	/**
	 * Page notifications for currently authenticated user
	 *
	 * @param user       the repository owner
	 * @param repository the repository name
	 * @return iterator over pages of notifications
	 */
	public PageIterator<Notification> pageNotifications(String user,
			String repository) {
		return pageNotifications(user, repository, PAGE_SIZE);
	}

	/**
	 * Page notifications for currently authenticated user
	 *
	 * @param user       the repository owner
	 * @param repository the repository name
	 * @param size       the number of pages
	 * @return iterator over pages of notifications
	 */
	public PageIterator<Notification> pageNotifications(String user,
			String repository, int size) {
		return pageNotifications(user, repository, PAGE_FIRST, size);
	}

	/**
	 * Page notifications for currently authenticated user
	 *
	 * @param user       the repository owner
	 * @param repository the repository name
	 * @param start      the number of first page to load
	 * @param size       the number of pages
	 * @return iterator over pages of notifications
	 */
	public PageIterator<Notification> pageNotifications(String user,
			String repository, int start, int size) {
		verifyRepository(user, repository);
		String repoId = user + '/' + repository;
		PagedRequest<Notification> request = createNotificationsRequest(repoId,
				start, size);
		return createPageIterator(request);
	}

	/**
	 * Page notifications for currently authenticated user
	 *
	 * @param repository interface to provide an id for a repository
	 * @return iterator over pages of notifications
	 */
	public PageIterator<Notification> pageNotifications(
			IRepositoryIdProvider repository) {
		return pageNotifications(repository, PAGE_SIZE);
	}

	/**
	 * Page notifications for currently authenticated user
	 *
	 * @param repository interface to provide an id for a repository
	 * @param size       the number of pages
	 * @return iterator over pages of notifications
	 */
	public PageIterator<Notification> pageNotifications(
			IRepositoryIdProvider repository, int size) {
		return pageNotifications(repository, PAGE_FIRST, size);
	}

	/**
	 * Page notifications for currently authenticated user
	 *
	 * @param repository interface to provide an id for a repository
	 * @param start      the number of first page to load
	 * @param size       the number of pages
	 * @return iterator over pages of notifications
	 */
	public PageIterator<Notification> pageNotifications(
			IRepositoryIdProvider repository, int start, int size) {
		String repoId = getId(repository);
		PagedRequest<Notification> request = createNotificationsRequest(repoId,
				start, size);
		return createPageIterator(request);
	}

	/**
	 * Get a single notification
	 *
	 * @param threadId the id of the thread subscription
	 * @return the notification
	 * @throws IOException if something went wrong
	 */
	public Notification getNotification(String threadId) throws IOException {
		GitHubRequest request = createRequest();

		StringBuilder uri = new StringBuilder(SEGMENT_NOTIFICATIONS);
		uri.append(SEGMENT_THREADS);
		uri.append('/').append(threadId);

		request.setUri(uri);
		request.setType(Notification.class);

		return (Notification) client.get(request).getBody();
	}

	/**
	 * Mark a thread as read
	 *
	 * @param threadId the id of the thread subscription
	 * @throws IOException if something went wrong
	 */
	public void markThreadAsRead(String threadId) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_NOTIFICATIONS);
		uri.append(SEGMENT_THREADS);
		uri.append('/').append(threadId);

		client.post(uri.toString());
	}

	/**
	 * Mark all notifications as read
	 *
	 * @throws IOException if something went wrong
	 */
	public void markNotificationsAsRead() throws IOException {
		markNotificationsAsRead((Date) null);
	}

	/**
	 * Mark all notifications as read
	 *
	 * @param lastReadAt the last point that notifications were checked.
	 *                   Anything updated since this time will not be updated.
	 * @throws IOException if something went wrong
	 */
	public void markNotificationsAsRead(Date lastReadAt) throws IOException {
		Map<Object, Object> params = new HashMap<>();
		if (lastReadAt != null) {
			params.put(FIELD_LAST_READ_AT, lastReadAt);
		}

		client.put(SEGMENT_NOTIFICATIONS, params);
	}

	/**
	 * Mark all notifications in a repository as read
	 *
	 * @param user       the repository owner
	 * @param repository the repository name
	 * @throws IOException if something went wrong
	 */
	public void markNotificationsAsRead(String user, String repository)
			throws IOException {
		markNotificationsAsRead(user, repository, null);
	}

	/**
	 * Mark all notifications in a repository as read
	 *
	 * @param user       the repository owner
	 * @param repository the repository name
	 * @param lastReadAt the last point that notifications were checked.
	 *                   Anything updated since this time will not be updated.
	 * @throws IOException if something went wrong
	 */
	public void markNotificationsAsRead(String user, String repository,
			Date lastReadAt) throws IOException {
		verifyRepository(user, repository);

		String repoId = user + '/' + repository;
		markNotificationsAsRead(repoId, lastReadAt);
	}

	/**
	 * Mark all notifications in a repository as read
	 *
	 * @param repository interface to provide an id for a repository
	 * @throws IOException if something went wrong
	 */
	public void markNotificationsAsRead(IRepositoryIdProvider repository)
			throws IOException {
		markNotificationsAsRead(repository, null);
	}

	/**
	 * Mark all notifications in a repository as read
	 *
	 * @param repository interface to provide an id for a repository
	 * @param lastReadAt the last point that notifications were checked.
	 *                   Anything updated since this time will not be updated.
	 * @throws IOException if something went wrong
	 */
	public void markNotificationsAsRead(IRepositoryIdProvider repository,
			Date lastReadAt) throws IOException {
		String repoId = getId(repository);
		markNotificationsAsRead(repoId, lastReadAt);
	}

	/**
	 * Mark all notifications in a repository as read
	 *
	 * @param repoId the id of the repository
	 * @throws IOException if something went wrong
	 */
	public void markNotificationsAsRead(String repoId) throws IOException {
		markNotificationsAsRead(repoId, (Date) null);
	}

	/**
	 * Mark all notifications in a repository as read
	 *
	 * @param repoId     the id of the repository
	 * @param lastReadAt the last point that notifications were checked.
	 *                   Anything updated since this time will not be updated.
	 * @throws IOException if something went wrong
	 */
	public void markNotificationsAsRead(String repoId, Date lastReadAt)
			throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_NOTIFICATIONS);

		Map<Object, Object> params = new HashMap<>();
		if (lastReadAt != null) {
			params.put(FIELD_LAST_READ_AT, lastReadAt);
		}

		client.put(uri.toString(), params);
	}

	/**
	 * Check to see if the user is subscribed to a thread
	 *
	 * @param threadId the id of the thread subscription
	 * @return the thread subscription
	 * @throws IOException if something went wrong
	 */
	public ThreadSubscription getThreadSubscription(String threadId)
			throws IOException {
		GitHubRequest request = createRequest();

		StringBuilder uri = new StringBuilder(SEGMENT_NOTIFICATIONS);
		uri.append(SEGMENT_THREADS);
		uri.append('/').append(threadId);
		uri.append(SEGMENT_SUBSCRIPTION);

		request.setUri(uri);
		request.setType(ThreadSubscription.class);

		return (ThreadSubscription) client.get(request).getBody();
	}

	/**
	 * Subscribe or unsubscribe from a conversation. Unsubscribing from
	 * a conversation mutes all future notifications (until you comment or
	 * get @mentioned once more).
	 *
	 * @param threadId     the id of the thread subscription
	 * @param subscription the thread subscription from which to retrieve
	 *                     values for receiving and blocking thread
	 *                     notifications
	 * @return the thread subscription
	 * @throws IOException if something went wrong
	 */
	public ThreadSubscription setThreadSubscription(String threadId,
			ThreadSubscription subscription) throws IOException {
		return setThreadSubscription(threadId, subscription.isSubscribed(),
				subscription.isIgnored());
	}

	/**
	 * Subscribe or unsubscribe from a conversation. Unsubscribing from
	 * a conversation mutes all future notifications (until you comment or
	 * get @mentioned once more).
	 *
	 * @param threadId   the id of the thread subscription
	 * @param subscribed determines if notifications should be received from
	 *                   this thread (null keeps this setting unchanged)
	 * @param ignored    determines if all notifications should be blocked from
	 *                   this thread (null keeps this setting unchanged)
	 * @return the thread subscription
	 * @throws IOException if something went wrong
	 */
	public ThreadSubscription setThreadSubscription(String threadId,
			Boolean subscribed, Boolean ignored) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_NOTIFICATIONS);
		uri.append(SEGMENT_THREADS);
		uri.append('/').append(threadId);
		uri.append(SEGMENT_SUBSCRIPTION);

		Map<Object, Object> params = new HashMap<>();
		if (subscribed != null) {
			params.put(FIELD_SUBSCRIBED, subscribed);
		}
		if (ignored != null) {
			params.put(FIELD_IGNORED, ignored);
		}

		return client.put(uri.toString(), params, ThreadSubscription.class);
	}

	/**
	 * Delete a thread subscription
	 *
	 * @param threadId the id of the thread subscription
	 * @throws IOException if something went wrong
	 */
	public void deleteThreadSubscription(String threadId) throws IOException {
		StringBuilder uri = new StringBuilder(SEGMENT_NOTIFICATIONS);
		uri.append(SEGMENT_THREADS);
		uri.append('/').append(threadId);
		uri.append(SEGMENT_SUBSCRIPTION);

		client.delete(uri.toString());
	}
}