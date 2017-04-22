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

import static java.net.HttpURLConnection.HTTP_CREATED;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_DOWNLOADS;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_FIRST;
import static org.eclipse.egit.github.core.client.PagedRequest.PAGE_SIZE;

import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Download;
import org.eclipse.egit.github.core.DownloadResource;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.util.MultiPartUtils;

/**
 * Service for accessing, creating, and deleting repositories downloads.
 *
 * @see <a href="http://developer.github.com/v3/repos/downloads">GitHub
 *      downloads API documentation</a>
 */
public class DownloadService extends GitHubService {

	/**
	 * UPLOAD_KEY
	 */
	public static final String UPLOAD_KEY = "key"; //$NON-NLS-1$

	/**
	 * UPLOAD_ACL
	 */
	public static final String UPLOAD_ACL = "acl"; //$NON-NLS-1$

	/**
	 * UPLOAD_SUCCESS_ACTION_STATUS
	 */
	public static final String UPLOAD_SUCCESS_ACTION_STATUS = "success_action_status"; //$NON-NLS-1$

	/**
	 * UPLOAD_FILENAME
	 */
	public static final String UPLOAD_FILENAME = "Filename"; //$NON-NLS-1$

	/**
	 * UPLOAD_AWS_ACCESS_KEY_ID
	 */
	public static final String UPLOAD_AWS_ACCESS_KEY_ID = "AWSAccessKeyId"; //$NON-NLS-1$

	/**
	 * UPLOAD_POLICY
	 */
	public static final String UPLOAD_POLICY = "Policy"; //$NON-NLS-1$

	/**
	 * UPLOAD_SIGNATURE
	 */
	public static final String UPLOAD_SIGNATURE = "Signature"; //$NON-NLS-1$

	/**
	 * UPLOAD_FILE
	 */
	public static final String UPLOAD_FILE = "file"; //$NON-NLS-1$

	/**
	 * UPLOAD_CONTENT_TYPE
	 */
	public static final String UPLOAD_CONTENT_TYPE = "Content-Type"; //$NON-NLS-1$

	/**
	 * Create download service
	 */
	public DownloadService() {
		super();
	}

	/**
	 * Create download service
	 *
	 * @param client
	 */
	public DownloadService(GitHubClient client) {
		super(client);
	}

	/**
	 * Get download metadata for given repository and id
	 *
	 * @param repository
	 * @param id
	 * @return download
	 * @throws IOException
	 */
	public Download getDownload(IRepositoryIdProvider repository, int id)
			throws IOException {
		final String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_DOWNLOADS);
		uri.append('/').append(id);
		GitHubRequest request = createRequest();
		request.setUri(uri);
		request.setType(Download.class);
		return (Download) client.get(request).getBody();
	}

	/**
	 * Create paged downloads request
	 *
	 * @param repository
	 * @param start
	 * @param size
	 * @return request
	 */
	protected PagedRequest<Download> createDownloadsRequest(
			IRepositoryIdProvider repository, int start, int size) {
		final String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_DOWNLOADS);
		PagedRequest<Download> request = createPagedRequest(start, size);
		request.setType(new TypeToken<List<Download>>() {
		}.getType());
		request.setUri(uri);
		return request;
	}

	/**
	 * Get metadata for all downloads for given repository
	 *
	 * @param repository
	 * @return non-null but possibly empty list of download metadata
	 * @throws IOException
	 */
	public List<Download> getDownloads(IRepositoryIdProvider repository)
			throws IOException {
		return getAll(pageDownloads(repository));
	}

	/**
	 * Page metadata for downloads for given repository
	 *
	 * @param repository
	 * @return iterator over pages of downloads
	 */
	public PageIterator<Download> pageDownloads(IRepositoryIdProvider repository) {
		return pageDownloads(repository, PAGE_SIZE);
	}

	/**
	 * Page downloads for given repository
	 *
	 * @param repository
	 * @param size
	 * @return iterator over pages of downloads
	 */
	public PageIterator<Download> pageDownloads(
			IRepositoryIdProvider repository, int size) {
		return pageDownloads(repository, PAGE_FIRST, size);
	}

	/**
	 * Page downloads for given repository
	 *
	 * @param repository
	 * @param start
	 * @param size
	 * @return iterator over pages of downloads
	 */
	public PageIterator<Download> pageDownloads(
			IRepositoryIdProvider repository, int start, int size) {
		PagedRequest<Download> request = createDownloadsRequest(repository,
				start, size);
		return createPageIterator(request);
	}

	/**
	 * Delete download with given id from given repository
	 *
	 * @param repository
	 * @param id
	 * @throws IOException
	 */
	public void deleteDownload(IRepositoryIdProvider repository, int id)
			throws IOException {
		final String repoId = getId(repository);
		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_DOWNLOADS);
		uri.append('/').append(id);
		client.delete(uri.toString());
	}

	/**
	 * Create a new resource for download associated with the given repository
	 *
	 * @param repository
	 * @param download
	 * @return download resource
	 * @throws IOException
	 */
	public DownloadResource createResource(IRepositoryIdProvider repository,
			Download download) throws IOException {
		final String repoId = getId(repository);

		StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
		uri.append('/').append(repoId);
		uri.append(SEGMENT_DOWNLOADS);
		return client.post(uri.toString(), download, DownloadResource.class);
	}

	/**
	 * Upload a resource to be available as the download described by the given
	 * resource.
	 *
	 * @param resource
	 * @param content
	 * @param size
	 * @throws IOException
	 */
	public void uploadResource(DownloadResource resource, InputStream content,
			long size) throws IOException {
		if (resource == null)
			throw new IllegalArgumentException(
					"Download resource cannot be null"); //$NON-NLS-1$
		if (content == null)
			throw new IllegalArgumentException(
					"Content input stream cannot be null"); //$NON-NLS-1$

		Map<String, Object> parts = new LinkedHashMap<String, Object>();
		parts.put(UPLOAD_KEY, resource.getPath());
		parts.put(UPLOAD_ACL, resource.getAcl());
		parts.put(UPLOAD_SUCCESS_ACTION_STATUS, Integer.toString(HTTP_CREATED));
		parts.put(UPLOAD_FILENAME, resource.getName());
		parts.put(UPLOAD_AWS_ACCESS_KEY_ID, resource.getAccesskeyid());
		parts.put(UPLOAD_POLICY, resource.getPolicy());
		parts.put(UPLOAD_SIGNATURE, resource.getSignature());
		parts.put(UPLOAD_CONTENT_TYPE, resource.getMimeType());
		parts.put(UPLOAD_FILE, content);

		HttpURLConnection connection = MultiPartUtils.post(resource.getS3Url(),
				parts);
		int status = connection.getResponseCode();
		if (status != HTTP_CREATED)
			throw new IOException("Unexpected response status of " + status); //$NON-NLS-1$
	}

	/**
	 * Create download and set the content to be the content of given input
	 * stream. This is a convenience method that performs a
	 * {@link #createResource(IRepositoryIdProvider, Download)} followed by a
	 * {@link #uploadResource(DownloadResource, InputStream, long)} with the
	 * results.
	 *
	 * @param repository
	 * @param download
	 *            metadata about the download
	 * @param content
	 *            raw content of the download
	 * @param size
	 *            size of content in the input stream
	 * @return created resource
	 * @throws IOException
	 */
	public DownloadResource createDownload(IRepositoryIdProvider repository,
			Download download, InputStream content, long size)
			throws IOException {
		DownloadResource resource = createResource(repository, download);
		uploadResource(resource, content, size);
		return resource;
	}

	/**
	 * Create download from content of given file.
	 *
	 * @see #createDownload(IRepositoryIdProvider, Download, InputStream, long)
	 * @param repository
	 * @param download
	 *            metadata about the download
	 * @param file
	 *            must be non-null
	 * @return created resource
	 * @throws IOException
	 */
	public DownloadResource createDownload(IRepositoryIdProvider repository,
			Download download, File file) throws IOException {
		if (file == null)
			throw new IllegalArgumentException("File cannot be null"); //$NON-NLS-1$

		return createDownload(repository, download, new FileInputStream(file),
				file.length());
	}
}
