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
package org.eclipse.egit.github.core.util;

import static org.eclipse.egit.github.core.client.IGitHubConstants.CHARSET_UTF8;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SUFFIX_GIT;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.egit.github.core.IRepositoryIdProvider;

/**
 * URL utilities
 */
public abstract class UrlUtils {

	/**
	 * Create SSH URL used for repository remote configs
	 *
	 * @param repository
	 * @return URL
	 */
	public static String createRemoteSshUrl(IRepositoryIdProvider repository) {
		return createRemoteSshUrl(repository, HOST_DEFAULT);
	}

	/**
	 * Create SSH URL used for repository remote configs
	 *
	 * @param repository
	 * @param host
	 * @return URL
	 */
	public static String createRemoteSshUrl(IRepositoryIdProvider repository,
			String host) {
		return "git@" + host + ":" + repository.generateId() + SUFFIX_GIT; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Create HTTPS URL used for repository remote configs
	 *
	 * @param repository
	 * @param user
	 * @return URL
	 */
	public static String createRemoteHttpsUrl(IRepositoryIdProvider repository,
			String user) {
		return createRemoteHttpsUrl(repository, HOST_DEFAULT, user);
	}

	/**
	 * Create HTTPS URL used for repository remote configs
	 *
	 * @param repository
	 * @param host
	 * @param user
	 * @return URL
	 */
	public static String createRemoteHttpsUrl(IRepositoryIdProvider repository,
			String host, String user) {
		return "https://" + user + "@" + host + "/" + repository.generateId() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ SUFFIX_GIT;
	}

	/**
	 * Create read-only URL used for repository remote configs
	 *
	 * @param repository
	 * @return URL
	 */
	public static String createRemoteReadOnlyUrl(
			IRepositoryIdProvider repository) {
		return createRemoteReadOnlyUrl(repository, HOST_DEFAULT);
	}

	/**
	 * Create read-only URL used for repository remote configs
	 *
	 * @param repository
	 * @param host
	 * @return URL
	 */
	public static String createRemoteReadOnlyUrl(
			IRepositoryIdProvider repository, String host) {
		return "git://" + host + "/" + repository.generateId() + SUFFIX_GIT; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * URL-encode value using 'UTF-8' character set
	 *
	 * @param value
	 * @return encoded value
	 */
	public static String encode(final String value) {
		try {
			return URLEncoder.encode(value, CHARSET_UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * URL-decode value using 'UTF-8' character set
	 *
	 * @param value
	 * @return encoded value
	 */
	public static String decode(final String value) {
		try {
			return URLDecoder.decode(value, CHARSET_UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Add encoded parameter to URI
	 *
	 * @param name
	 * @param value
	 * @param uri
	 */
	public static void addParam(final String name, final String value,
			final StringBuilder uri) {
		if (uri.length() > 0)
			uri.append('&');
		uri.append(encode(name)).append('=');
		if (value != null)
			uri.append(encode(value));
	}

	/**
	 * Add request parameters to URI
	 *
	 * @param params
	 * @param uri
	 */
	public static void addParams(final Map<String, String> params,
			final StringBuilder uri) {
		if (params == null || params.isEmpty())
			return;
		for (Entry<String, String> param : params.entrySet())
			addParam(param.getKey(), param.getValue(), uri);
	}

	/**
	 * Get parameter value with name
	 *
	 * @param uri
	 * @param name
	 * @return value or null if not found in URI query
	 */
	public static String getParam(final URI uri, final String name) {
		final String query = uri.getRawQuery();
		if (query == null || query.length() == 0)
			return null;
		final String[] params = query.split("&"); //$NON-NLS-1$
		for (String param : params) {
			final String[] parts = param.split("="); //$NON-NLS-1$
			if (parts.length != 2)
				continue;
			if (!name.equals(parts[0]))
				continue;
			return decode(parts[1]);
		}
		return null;
	}
}
