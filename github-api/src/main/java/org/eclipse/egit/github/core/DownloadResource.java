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

import java.util.Date;

import org.eclipse.egit.github.core.util.DateUtils;

/**
 * Extension of {@link Download} to represent the initiation of a download with
 * metadata about where to put the actual content when uploading.
 */
public class DownloadResource extends Download {

	/** serialVersionUID */
	private static final long serialVersionUID = 4522843864589481490L;

	private boolean redirect;

	private Date expirationdate;

	private String accesskeyid;

	private String acl;

	private String mimeType;

	private String path;

	private String policy;

	private String prefix;

	private String s3Url;

	private String signature;

	/**
	 * @return redirect
	 */
	public boolean isRedirect() {
		return redirect;
	}

	/**
	 * @param redirect
	 * @return this download resource
	 */
	public DownloadResource setRedirect(boolean redirect) {
		this.redirect = redirect;
		return this;
	}

	/**
	 * @return expirationdate
	 */
	public Date getExpirationdate() {
		return DateUtils.clone(expirationdate);
	}

	/**
	 * @param expirationdate
	 * @return this download resource
	 */
	public DownloadResource setExpirationdate(Date expirationdate) {
		this.expirationdate = DateUtils.clone(expirationdate);
		return this;
	}

	/**
	 * @return accesskeyid
	 */
	public String getAccesskeyid() {
		return accesskeyid;
	}

	/**
	 * @param accesskeyid
	 * @return this download resource
	 */
	public DownloadResource setAccesskeyid(String accesskeyid) {
		this.accesskeyid = accesskeyid;
		return this;
	}

	/**
	 * @return acl
	 */
	public String getAcl() {
		return acl;
	}

	/**
	 * @param acl
	 * @return this download resource
	 */
	public DownloadResource setAcl(String acl) {
		this.acl = acl;
		return this;
	}

	/**
	 * @return mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * @param mimeType
	 * @return this download resource
	 */
	public DownloadResource setMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	/**
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path
	 * @return this download resource
	 */
	public DownloadResource setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * @return policy
	 */
	public String getPolicy() {
		return policy;
	}

	/**
	 * @param policy
	 * @return this download resource
	 */
	public DownloadResource setPolicy(String policy) {
		this.policy = policy;
		return this;
	}

	/**
	 * @return prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @param prefix
	 * @return this download resource
	 */
	public DownloadResource setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	/**
	 * @return s3Url
	 */
	public String getS3Url() {
		return s3Url;
	}

	/**
	 * @param s3Url
	 * @return this download resource
	 */
	public DownloadResource setS3Url(String s3Url) {
		this.s3Url = s3Url;
		return this;
	}

	/**
	 * @return signature
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * @param signature
	 * @return this download resource
	 */
	public DownloadResource setSignature(String signature) {
		this.signature = signature;
		return this;
	}
}
