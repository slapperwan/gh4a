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
package org.eclipse.egit.github.core;

import java.io.Serializable;

/**
 * Tag model class
 */
public class Tag implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = 8505182933582492676L;

	private CommitUser tagger;

	private String message;

	private String sha;

	private String tag;

	private String url;

	private TypedResource object;

	/**
	 * @return tagger
	 */
	public CommitUser getTagger() {
		return tagger;
	}

	/**
	 * @param tagger
	 * @return this tag
	 */
	public Tag setTagger(CommitUser tagger) {
		this.tagger = tagger;
		return this;
	}

	/**
	 * @return message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message
	 * @return this tag
	 */
	public Tag setMessage(String message) {
		this.message = message;
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
	 * @return this tag
	 */
	public Tag setSha(String sha) {
		this.sha = sha;
		return this;
	}

	/**
	 * @return tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @param tag
	 * @return this tag
	 */
	public Tag setTag(String tag) {
		this.tag = tag;
		return this;
	}

	/**
	 * @return url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 * @return this tag
	 */
	public Tag setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * @return object
	 */
	public TypedResource getObject() {
		return object;
	}

	/**
	 * @param object
	 * @return this tag
	 */
	public Tag setObject(TypedResource object) {
		this.object = object;
		return this;
	}
}
