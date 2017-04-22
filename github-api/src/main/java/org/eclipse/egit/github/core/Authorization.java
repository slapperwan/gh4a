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
import java.util.Date;
import java.util.List;

import org.eclipse.egit.github.core.util.DateUtils;

/**
 * Authorization model class
 */
public class Authorization implements Serializable {

	/** serialVersionUID */
	private static final long serialVersionUID = -5564926246696914047L;

	private Application app;

	private Date createdAt;

	private Date updatedAt;

	private int id;

	private List<String> scopes;

	private String note;

	private String noteUrl;

	private String token;

	private String tokenLastEight;

	private String hashedToken;

	private String fingerprint;

	private String url;

	/**
	 * @return app
	 */
	public Application getApp() {
		return app;
	}

	/**
	 * @param app
	 * @return this authorization
	 */
	public Authorization setApp(Application app) {
		this.app = app;
		return this;
	}

	/**
	 * @return createdAt
	 */
	public Date getCreatedAt() {
		return DateUtils.clone(createdAt);
	}

	/**
	 * @param createdAt
	 * @return this authorization
	 */
	public Authorization setCreatedAt(Date createdAt) {
		this.createdAt = DateUtils.clone(createdAt);
		return this;
	}

	/**
	 * @return updatedAt
	 */
	public Date getUpdatedAt() {
		return DateUtils.clone(updatedAt);
	}

	/**
	 * @param updatedAt
	 * @return this authorization
	 */
	public Authorization setUpdatedAt(Date updatedAt) {
		this.updatedAt = DateUtils.clone(updatedAt);
		return this;
	}

	/**
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 * @return this authorization
	 */
	public Authorization setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * @return note
	 */
	public String getNote() {
		return note;
	}

	/**
	 * @param note
	 * @return this authorization
	 */
	public Authorization setNote(String note) {
		this.note = note;
		return this;
	}

	/**
	 * @return noteUrl
	 */
	public String getNoteUrl() {
		return noteUrl;
	}

	/**
	 * @param noteUrl
	 * @return this authorization
	 */
	public Authorization setNoteUrl(String noteUrl) {
		this.noteUrl = noteUrl;
		return this;
	}

	/**
	 * @return scopes
	 */
	public List<String> getScopes() {
		return scopes;
	}

	/**
	 * @param scopes
	 * @return this authorization
	 */
	public Authorization setScopes(List<String> scopes) {
		this.scopes = scopes;
		return this;
	}

	/**
	 * @return token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @param token
	 * @return this authorization
	 */
	public Authorization setToken(String token) {
		this.token = token;
		return this;
	}

	/**
	 * @return tokenLastEight
	 */
	public String getTokenLastEight() {
		return tokenLastEight;
	}

	/**
	 * @return hashedToken
	 */
	public String getHashedToken() {
		return hashedToken;
	}

	/**
	 * @return fingerprint
	 */
	public String getFingerprint() {
		return fingerprint;
	}

	/**
	 * @param fingerprint
	 * @return this authorization
	 */
	public Authorization setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
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
	 * @return this authorization
	 */
	public Authorization setUrl(String url) {
		this.url = url;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Authorization) {
			return this.id == ((Authorization) other).id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id;
	}
}
