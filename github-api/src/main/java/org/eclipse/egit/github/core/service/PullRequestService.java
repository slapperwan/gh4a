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

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.MergeStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_COMMENTS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_COMMITS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_EVENTS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_FILES;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_MERGE;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_PULLS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REACTIONS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REVIEWS;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

/**
 * Service class for creating, updating, getting, and listing pull requests as
 * well as getting the commits associated with a pull request and the files
 * modified by a pull request.
 *
 * @see <a href="http://developer.github.com/v3/pulls">GitHub Pull Requests API
 *      documentation</a>
 * @see <a href="http://developer.github.com/v3/pulls/comments">GitHub Pull
 *      Request comments API documentation</a>
 */
public class PullRequestService extends GitHubService {

	/**
	 * PR_TITLE
	 */
	public static final String PR_TITLE = "title"; //$NON-NLS-1$

	/**
	 * PR_BODY
	 */
	public static final String PR_BODY = "body"; //$NON-NLS-1$

	/**
	 * PR_BASE
	 */
	public static final String PR_BASE = "base"; //$NON-NLS-1$

	/**
	 * PR_HEAD
	 */
	public static final String PR_HEAD = "head"; //$NON-NLS-1$

	/**
	 * PR_STATE
	 */
	public static final String PR_STATE = "state"; //$NON-NLS-1$

	/**
	 * Merge methods for the {@link merge} method
	 */
	public static final String MERGE_METHOD_MERGE = "merge";
	public static final String MERGE_METHOD_SQUASH = "squash";
	public static final String MERGE_METHOD_REBASE = "rebase";

	/**
	 * Create pull request service
	 */
	public PullRequestService() {
		super();
	}

	/**
	 * Create pull request service
	 *
	 * @param client
	 */
	public PullRequestService(GitHubClient client) {
		super(client);
	}

	/**
	 * Create request for single pull request
	 *
	 * @param repository
	 * @param id
	 * @return request
	 * @throws IOException
	 */
	public PullRequest getPullRequest(IRepositoryIdProvider repository, int id)
			throws IOException {
		final String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(id);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(PullRequest.class);
		return (PullRequest) client.get(request).getBody();
	}

	/**
	 * Create paged request for fetching pull requests
	 *
	 * @param provider
	 * @param state
	 * @param start
	 * @param size
	 * @return paged request
	 */
	protected PagedRequest<PullRequest> createPullsRequest(
			IRepositoryIdProvider provider, String state, int start, int size) {
		final String id = getId(provider);

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_PULLS);
		PagedRequest<PullRequest> request = createPagedRequest(start, size);
		request.setUri(uri);
		if (state != null)
			request.setParams(Collections.singletonMap(
					IssueService.FILTER_STATE, state));
		request.setType(new TypeToken<List<PullRequest>>() {
		}.getType());
		return request;
	}

	/**
	 * Get pull requests from repository matching state
	 *
	 * @param repository
	 * @param state
	 * @return list of pull requests
	 * @throws IOException
	 */
	public List<PullRequest> getPullRequests(IRepositoryIdProvider repository,
			String state) throws IOException {
		return getAll(pagePullRequests(repository, state));
	}

	/**
	 * Page pull requests with given state
	 *
	 * @param repository
	 * @param state
	 * @return iterator over pages of pull requests
	 */
	public PageIterator<PullRequest> pagePullRequests(
			IRepositoryIdProvider repository, String state) {
		return pagePullRequests(repository, state, PAGE_SIZE);
	}

	/**
	 * Page pull requests with given state
	 *
	 * @param repository
	 * @param state
	 * @param size
	 * @return iterator over pages of pull requests
	 */
	public PageIterator<PullRequest> pagePullRequests(
			IRepositoryIdProvider repository, String state, int size) {
		return pagePullRequests(repository, state, PAGE_FIRST, size);
	}

	/**
	 * Page pull requests with given state
	 *
	 * @param repository
	 * @param state
	 * @param start
	 * @param size
	 * @return iterator over pages of pull requests
	 */
	public PageIterator<PullRequest> pagePullRequests(
			IRepositoryIdProvider repository, String state, int start, int size) {
		PagedRequest<PullRequest> request = createPullsRequest(repository,
				state, start, size);
		return createPageIterator(request);
	}

	private Map<String, String> createPrMap(PullRequest request) {
		Map<String, String> params = new HashMap<String, String>();
		if (request != null) {
			String title = request.getTitle();
			if (title != null)
				params.put(PR_TITLE, title);
			String body = request.getBody();
			if (body != null)
				params.put(PR_BODY, body);
			PullRequestMarker baseMarker = request.getBase();
			if (baseMarker != null) {
				String base = baseMarker.getLabel();
				if (base != null)
					params.put(PR_BASE, base);
			}
			PullRequestMarker headMarker = request.getHead();
			if (headMarker != null) {
				String head = headMarker.getLabel();
				if (head != null)
					params.put(PR_HEAD, head);
			}
		}
		return params;
	}

	private Map<String, String> editPrMap(PullRequest request) {
		Map<String, String> params = new HashMap<String, String>();
		String title = request.getTitle();
		if (title != null)
			params.put(PR_TITLE, title);
		String body = request.getBody();
		if (body != null)
			params.put(PR_BODY, body);
		String state = request.getState();
		if (state != null)
			params.put(PR_STATE, state);
		return params;
	}

	/**
	 * Create pull request
	 *
	 * @param repository
	 * @param request
	 * @return created pull request
	 * @throws IOException
	 */
	public PullRequest createPullRequest(IRepositoryIdProvider repository,
			PullRequest request) throws IOException {
		String id = getId(repository);

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_PULLS);
		Map<String, String> params = createPrMap(request);
		return client.post(uri.toString(), params, PullRequest.class);
	}

	/**
	 * Create pull request by attaching branch information to an existing issue
	 *
	 * @param repository
	 * @param issueId
	 * @param head
	 * @param base
	 * @return created pull request
	 * @throws IOException
	 */
	public PullRequest createPullRequest(IRepositoryIdProvider repository,
			int issueId, String head, String base) throws IOException {
		String id = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_PULLS);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("issue", issueId); //$NON-NLS-1$
		params.put("head", head); //$NON-NLS-1$
		params.put("base", base); //$NON-NLS-1$
		return client.post(uri.toString(), params, PullRequest.class);
	}

	/**
	 * Edit pull request
	 *
	 * @param repository
	 * @param request
	 * @return edited pull request
	 * @throws IOException
	 */
	public PullRequest editPullRequest(IRepositoryIdProvider repository,
			PullRequest request) throws IOException {
		String id = getId(repository);
		if (request == null)
			throw new IllegalArgumentException("Request cannot be null"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(request.getNumber());
		Map<String, String> params = editPrMap(request);
		return client.post(uri.toString(), params, PullRequest.class);
	}

	/**
	 * Get all commits associated with given pull request id
	 *
	 * @param repository
	 * @param id
	 * @return list of commits
	 * @throws IOException
	 */
	public List<RepositoryCommit> getCommits(IRepositoryIdProvider repository,
			int id) throws IOException {
		final String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(id);
		uri.append(SEGMENT_COMMITS);
		PagedRequest<RepositoryCommit> request = createPagedRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<RepositoryCommit>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Get all changed files associated with given pull request id
	 *
	 * @param repository
	 * @param id
	 * @return list of commit files
	 * @throws IOException
	 */
	public List<CommitFile> getFiles(IRepositoryIdProvider repository, int id)
			throws IOException {
		final String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(id);
		uri.append(SEGMENT_FILES);
		PagedRequest<CommitFile> request = createPagedRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<CommitFile>>() {
		}.getType());
		return getAll(request);
	}

	/**
	 * Is the given pull request id merged?
	 *
	 * @param repository
	 * @param id
	 * @return true if merge, false otherwise
	 * @throws IOException
	 */
	public boolean isMerged(IRepositoryIdProvider repository, int id)
			throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(id);
		uri.append(SEGMENT_MERGE);
		return check(uri.toString());
	}

	/**
	 * Merge given pull request
	 *
	 * @param repository
	 * @param id
	 * @param commitMessage
	 * @return status of merge
	 * @throws IOException
	 */
	public MergeStatus merge(IRepositoryIdProvider repository, int id,
			String commitMessage) throws IOException {
		return merge(repository, id, commitMessage, null);
	}

	/**
	 * Merge given pull request
	 *
	 * @param repository
	 * @param id
	 * @param commitMessage
	 * @param method
	 * @return status of merge
	 * @throws IOException
	 */
	public MergeStatus merge(IRepositoryIdProvider repository, int id,
			String commitMessage, String method) throws IOException {
		String repoId = getId(repository);
		Map<Object, Object> params = new HashMap<Object, Object>();
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(id);
		uri.append(SEGMENT_MERGE);

		params.put("commit_message", commitMessage); //$NON-NLS-1$
		if (method != null) {
			params.put("merge_method", method); //$NON-NLS-1$
		}
		return client.put(uri.toString(), params, MergeStatus.class);
	}

	/**
	 * Get all comments on commits in given pull request
	 *
	 * @param repository
	 * @param pullRequestNumber
	 * @return non-null list of comments
	 * @throws IOException
	 */
	public List<CommitComment> getComments(IRepositoryIdProvider repository,
			int pullRequestNumber) throws IOException {
		return getAll(pageComments(repository, pullRequestNumber));
	}

	/**
	 * Page pull request commit comments
	 *
	 * @param repository
	 * @param pullRequestNumber
	 * @return iterator over pages of commit comments
	 */
	public PageIterator<CommitComment> pageComments(
			IRepositoryIdProvider repository, int pullRequestNumber) {
		return pageComments(repository, pullRequestNumber, PAGE_SIZE);
	}

	/**
	 * Page pull request commit comments
	 *
	 * @param repository
	 * @param pullRequestNumber
	 * @param size
	 * @return iterator over pages of commit comments
	 */
	public PageIterator<CommitComment> pageComments(
			IRepositoryIdProvider repository, int pullRequestNumber, int size) {
		return pageComments(repository, pullRequestNumber, PAGE_FIRST, size);
	}

	/**
	 * Page pull request commit comments
	 *
	 * @param repository
	 * @param pullRequestNumber
	 * @param start
	 * @param size
	 * @return iterator over pages of commit comments
	 */
	public PageIterator<CommitComment> pageComments(
			IRepositoryIdProvider repository, int pullRequestNumber, int start, int size) {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(pullRequestNumber);
		uri.append(SEGMENT_COMMENTS);
		PagedRequest<CommitComment> request = createPagedRequest(start, size);
		request.setUri(uri);
		request.setType(new TypeToken<List<CommitComment>>() {
		}.getType());
		return createPageIterator(request);
	}

	/**
	 * Get all comments on commits in given pull request
	 *
	 * @param repository
	 * @param id
	 * @return non-null list of comments
	 * @throws IOException
	 */
	public List<Review> getReviews(IRepositoryIdProvider repository,
			int id) throws IOException {
		return getAll(pageReviews(repository, id));
	}

	/**
	 * Page pull request commit comments
	 *
	 * @param repository
	 * @param id
	 * @return iterator over pages of commit comments
	 */
	public PageIterator<Review> pageReviews(
			IRepositoryIdProvider repository, int id) {
		return pageReviews(repository, id, PAGE_SIZE);
	}

	/**
	 * Page pull request commit comments
	 *
	 * @param repository
	 * @param id
	 * @param size
	 * @return iterator over pages of commit comments
	 */
	public PageIterator<Review> pageReviews(
			IRepositoryIdProvider repository, int id, int size) {
		return pageReviews(repository, id, PAGE_FIRST, size);
	}

	/**
	 * Page pull request commit comments
	 *
	 * @param repository
	 * @param id
	 * @param start
	 * @param size
	 * @return iterator over pages of commit comments
	 */
	public PageIterator<Review> pageReviews(
			IRepositoryIdProvider repository, int id, int start, int size) {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(id);
		uri.append(SEGMENT_REVIEWS);
		PagedRequest<Review> request = createPagedRequest(start, size);
		request.setUri(uri);
		request.setType(new TypeToken<List<Review>>() {
		}.getType());
		return createPageIterator(request);
	}

	/**
	 * Get pull request review with the given id.
     *
	 * @param repository
	 * @param pullRequestNumber
	 * @param reviewId
	 * @return review
	 * @throws IOException
	 */
	public Review getReview(IRepositoryIdProvider repository, int pullRequestNumber,
			long reviewId) throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(pullRequestNumber);
		uri.append(SEGMENT_REVIEWS);
		uri.append('/').append(reviewId);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(Review.class);
		return (Review) client.get(request).getBody();
	}

	/**
	 * Get all comments on a review in given pull request.
	 *
	 * @param repository
	 * @param pullRequestNumber
	 * @param reviewId
	 * @return non-null list of comments
	 * @throws IOException
	 */
	public List<CommitComment> getReviewComments(IRepositoryIdProvider repository,
			int pullRequestNumber, long reviewId) throws IOException {
		return getAll(pageReviewComments(repository, pullRequestNumber, reviewId));
	}

	/**
	 * Page pull request review comments
	 *
	 * @param repository
	 * @param pullRequestNumber
	 * @param reviewId
	 * @return iterator over pages of review comments
	 */
	public PageIterator<CommitComment> pageReviewComments(IRepositoryIdProvider repository,
			int pullRequestNumber, long reviewId) {
		return pageReviewComments(repository, pullRequestNumber, reviewId, PAGE_SIZE);
	}

	/**
	 * Page pull request review comments
	 *
	 * @param repository
	 * @param pullRequestNumber
	 * @param reviewId
	 * @param size
	 *  @return iterator over pages of review comments
	 */
	public PageIterator<CommitComment> pageReviewComments(IRepositoryIdProvider repository,
			int pullRequestNumber, long reviewId, int size) {
		return pageReviewComments(repository, pullRequestNumber, reviewId, PAGE_FIRST, size);
	}

	/**
	 * Page pull request review comments
	 *
	 * @param repository
	 * @param pullRequestNumber
	 * @param reviewId
	 * @param start
	 * @param size
	 * @return iterator over pages of review comments
	 */
	public PageIterator<CommitComment> pageReviewComments(IRepositoryIdProvider repository,
			int pullRequestNumber, long reviewId, int start, int size) {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(pullRequestNumber);
		uri.append(SEGMENT_REVIEWS);
		uri.append('/').append(reviewId);
		uri.append(SEGMENT_COMMENTS);
		PagedRequest<CommitComment> request = createPagedRequest(start, size);
		request.setUri(uri);
		request.setType(new TypeToken<List<CommitComment>>() {
		}.getType());
		return createPageIterator(request);
	}

	/**
	 * Get commit comment with given id
	 *
	 * @param repository
	 * @param commentId
	 * @return commit comment
	 * @throws IOException
	 */
	public CommitComment getComment(IRepositoryIdProvider repository,
			long commentId) throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append(SEGMENT_COMMENTS);
		uri.append('/').append(commentId);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(CommitComment.class);
		return (CommitComment) client.get(request).getBody();
	}

	public List<Reaction> getCommentReactions(IRepositoryIdProvider repository,
			long commentId) throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append(SEGMENT_COMMENTS);
		uri.append('/').append(commentId);
		uri.append(SEGMENT_REACTIONS);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(new TypeToken<List<Reaction>>() {
		}.getType());
		return (List<Reaction>) client.get(request).getBody();
	}

	/**
	 * Create comment on given pull request
	 *
	 * @param repository
	 * @param id
	 * @param comment
	 * @return created commit comment
	 * @throws IOException
	 */
	public CommitComment createComment(IRepositoryIdProvider repository,
			int id, CommitComment comment) throws IOException {
		String repoId = getId(repository);

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(id);
		uri.append(SEGMENT_COMMENTS);
		return client.post(uri.toString(), comment, CommitComment.class);
	}

	/**
	 * Reply to given comment
	 *
	 * @param repository
	 * @param pullRequestId
	 * @param commentId
	 * @param body
	 * @return created comment
	 * @throws IOException
	 */
	public CommitComment replyToComment(IRepositoryIdProvider repository,
			int pullRequestId, long commentId, String body) throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append('/').append(pullRequestId);
		uri.append(SEGMENT_COMMENTS);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("in_reply_to", commentId); //$NON-NLS-1$
		params.put("body", body); //$NON-NLS-1$
		return client.post(uri.toString(), params, CommitComment.class);
	}

	/**
	 * Edit pull request comment
	 *
	 * @param repository
	 * @param comment
	 * @return edited comment
	 * @throws IOException
	 */
	public CommitComment editComment(IRepositoryIdProvider repository,
			CommitComment comment) throws IOException {
		String repoId = getId(repository);
		if (comment == null)
			throw new IllegalArgumentException("Comment cannot be null"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append(SEGMENT_COMMENTS);
		uri.append('/').append(comment.getId());
		return client.post(uri.toString(), comment, CommitComment.class);
	}

	/**
	 * Delete commit comment with given id
	 *
	 * @param repository
	 * @param commentId
	 * @throws IOException
	 */
	public void deleteComment(IRepositoryIdProvider repository, long commentId)
			throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_PULLS);
		uri.append(SEGMENT_COMMENTS);
		uri.append('/').append(commentId);
		client.delete(uri.toString());
	}

	public Reaction addCommentReaction(IRepositoryIdProvider repository,
			long commentId, String content) throws IOException {
		String id = getId(repository);
		if (content == null)
			throw new IllegalArgumentException("Content cannot be null"); //$NON-NLS-1$
		if (content.length() == 0)
			throw new IllegalArgumentException("Content cannot be empty"); //$NON-NLS-1$

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(id);
		uri.append(SEGMENT_PULLS);
		uri.append(SEGMENT_COMMENTS);
		uri.append('/').append(commentId);
		uri.append(SEGMENT_REACTIONS);

		return client.post(uri.toString(), Collections.singletonMap("content", content),
				Reaction.class);
	}

	public Review createReview(IRepositoryIdProvider repository, int pullRequestNumber,
			String body) throws IOException {
		String id = getId(repository);

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS)
				.append('/').append(id)
				.append(SEGMENT_PULLS)
				.append('/').append(pullRequestNumber)
				.append(SEGMENT_REVIEWS);

		Map<String, Object> params = new HashMap<>();
		params.put("body", body); //$NON-NLS-1$

		return client.post(uri.toString(), params, Review.class);
	}

	public Review submitReview(IRepositoryIdProvider repository, int pullRequestNumber,
			long reviewId, String body, String event) throws IOException {
		String id = getId(repository);

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS)
				.append('/').append(id)
				.append(SEGMENT_PULLS)
				.append('/').append(pullRequestNumber)
				.append(SEGMENT_REVIEWS)
				.append('/').append(reviewId)
				.append(SEGMENT_EVENTS);

		Map<String, Object> params = new HashMap<>();
		params.put("body", body); //$NON-NLS-1$
		params.put("event", event); //$NON-NLS-1$

		return client.post(uri.toString(), params, Review.class);
	}

	public void deleteReview(IRepositoryIdProvider repository, int pullRequestNumber,
			long reviewId) throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS)
				.append('/').append(repoId)
				.append(SEGMENT_PULLS)
				.append('/').append(pullRequestNumber)
				.append(SEGMENT_REVIEWS)
				.append('/').append(reviewId);
		client.delete(uri.toString());
	}

	public Review createReview(IRepositoryIdProvider repository, int pullRequestNumber,
			Map<String, Object> params) throws IOException {
		String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS)
				.append('/').append(repoId)
				.append(SEGMENT_PULLS)
				.append('/').append(pullRequestNumber)
				.append(SEGMENT_REVIEWS);
		return client.post(uri.toString(), params, Review.class);
	}
}
