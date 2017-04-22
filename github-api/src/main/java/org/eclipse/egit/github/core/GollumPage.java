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
package org.eclipse.egit.github.core;

import java.io.Serializable;

/**
 * Gollum Page model class.
 */
public class GollumPage implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -5841603600916978606L;

	public static final String ACTION_CREATE = "created";
	public static final String ACTION_EDIT = "edited";

	private String pageName;

	private String title;

	private String action;

	private String sha;

	private String htmlUrl;

	/**
	 * @return pageName
	 */
	public String getPageName() {
		return pageName;
	}

	/**
	 * @param pageName
	 * @return this page
	 */
	public GollumPage setPageName(String pageName) {
		this.pageName = pageName;
		return this;
	}

	/**
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 * @return this page
	 */
	public GollumPage setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * @return action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action
	 * @return this page
	 */
	public GollumPage setAction(String action) {
		this.action = action;
		return this;
	}

	/**
	 * @return sha
	 */
	public String getSha() {
		return sha;
	}

	/**
	 * @param sha
	 * @return this page
	 */
	public GollumPage setSha(String sha) {
		this.sha = sha;
		return this;
	}

	/**
	 * @return htmlUrl
	 */
	public String getHtmlUrl() {
		return htmlUrl;
	}

	/**
	 * @param htmlUrl
	 * @return this page
	 */
	public GollumPage setHtmlUrl(String htmlUrl) {
		this.htmlUrl = htmlUrl;
		return this;
	}
}
