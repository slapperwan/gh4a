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
package org.eclipse.egit.github.core.client;

import static org.eclipse.egit.github.core.client.IGitHubConstants.PARAM_PAGE;
import static org.eclipse.egit.github.core.client.IGitHubConstants.PARAM_PER_PAGE;

import org.eclipse.egit.github.core.util.UrlUtils;

/**
 * Paged request class that contains the initial page size and page number of
 * the request.
 *
 * @param <V>
 */
public class PagedRequest<V> extends GitHubRequest {

	/**
	 * First page
	 */
	public static final int PAGE_FIRST = 1;

	/**
	 * Default page size
	 */
	public static final int PAGE_SIZE = 100;

	private final int pageSize;

	private final int page;

	/**
	 * Create paged request with default size
	 */
	public PagedRequest() {
		this(PAGE_FIRST, PAGE_SIZE);
	}

	/**
	 * Create paged request with given starting page and page size.
	 *
	 * @param start
	 * @param size
	 */
	public PagedRequest(int start, int size) {
		page = start;
		pageSize = size;
	}

	/**
	 * Get initial page size
	 *
	 * @return pageSize
	 */
	public int getPageSize() {
		return pageSize;
	}

	@Override
	protected void addParams(final StringBuilder uri) {
		super.addParams(uri);
		final int size = getPageSize();
		if (size > 0)
			UrlUtils.addParam(PARAM_PER_PAGE, Integer.toString(size), uri);
		final int number = getPage();
		if (number > 0)
			UrlUtils.addParam(PARAM_PAGE, Integer.toString(number), uri);
	}

	/**
	 * Get initial page number
	 *
	 * @return page
	 */
	public int getPage() {
		return page;
	}
}
