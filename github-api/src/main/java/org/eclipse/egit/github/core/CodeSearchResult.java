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
package org.eclipse.egit.github.core;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.eclipse.egit.github.core.util.DateUtils;

/**
 * GitHub code search result class.
 */
public class CodeSearchResult implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -1211802439113529774L;

	private String name;
	private String path;
	private String sha;
	private String url;
	private String gitUrl;
	private String htmlUrl;
	private Repository repository;
	private double score;
	private List<TextMatch> textMatches;

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return sha
	 */
	public String getSha() {
		return sha;
	}

	/**
	 * @return url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return gitUrl
	 */
	public String getGitUrl() {
		return gitUrl;
	}

	/**
	 * @return htmlUrl
	 */
	public String getHtmlUrl() {
		return htmlUrl;
	}

	/**
	 * @return repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @return textMatches
	 */
	public List<TextMatch> getTextMatches() {
		return textMatches;
	}
}
